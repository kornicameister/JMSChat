package org.kornicameister.jsocket.app.server;

import com.google.common.base.Objects;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.kornicameister.jsocket.common.JatApplication;
import org.kornicameister.jsocket.common.jms.ConnectionInformation;
import org.kornicameister.jsocket.common.jms.JMSUtils;
import org.kornicameister.jsocket.common.msg.UserListMessage;
import org.kornicameister.jsocket.common.msg.login.LoginRequestMessage;
import org.kornicameister.jsocket.common.msg.login.LoginResponseMessage;
import org.kornicameister.jsocket.common.msg.logout.LogoutRequestMessage;

import javax.jms.*;
import java.io.Serializable;
import java.util.Set;

public class ServerApplication
    extends JatApplication {
  private static final Logger             LOGGER                   = LogManager.getLogger(ServerApplication.class);
  private static final Marker             JMS_MARKER               = MarkerManager.getMarker("jms:server");
  private              Connection         connection               = null;
  private              MessageConsumer    consumer                 = null;
  private              MessageProducer    responseToClientProducer = null;
  private              Set<ConnectedUser> connectedUsers           = null;
  private              ActiveMQTopic      mainRoomTopic            = null;
  private              ActiveMQTopic      serverMessageTopic       = null;

  public ServerApplication() {
    this.connectedUsers = Sets.newHashSet();
  }

  public static void launch(final String... args) {
    final ServerApplication app = new ServerApplication();

    final Thread appThread = new Thread(app, "ServerApplicationThread");
    appThread.setUncaughtExceptionHandler((thread, error) -> {
      LOGGER.error(String.format("%s threw an error with msg %s", thread.getName(), error.getMessage()));
    });
    appThread.start();

    final Thread mainThread = Thread.currentThread();
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        try {
          app.stop();
          mainThread.join();
        } catch (InterruptedException ignore) {
        }
      }
    });
  }

  @Override
  protected void doStart() {
    try {
      this.connection = JMSUtils.INSTANCE.newConection(
          new ConnectionInformation() {
            @Override
            public String getHost() {
              return properties.getProperty("activemq.broker-url");
            }

            @Override
            public String getClientId() {
              return ServerApplication.class.getSimpleName();
            }
          }
      );
      this.connection.start();

      final Session session = this.connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      this.initConsumer(session);
      this.initResponseToClientProducer(session);
      this.initMainRoomTopic(session);
      this.initServerMessageTopic(session);

    } catch (Exception exception) {
      LOGGER.error(MarkerManager.getMarker("doStart"), "Failed to start server", exception);
      throw new RuntimeException(exception);
    }
  }

  private void initConsumer(Session session) throws JMSException {
    this.consumer = session.createConsumer((Queue) () -> this.properties.getProperty("queue.name"));
    this.consumer.setMessageListener(message -> {
      try {
        final ObjectMessage om = (ObjectMessage) message;
        final Serializable object = om.getObject();
        if (object instanceof LoginRequestMessage) {
          this.handleLogin(((LoginRequestMessage) object).getLogin(), message);
        } else if (object instanceof LogoutRequestMessage) {
          this.handleLogout(((LogoutRequestMessage) object).getLogin());
        }
      } catch (JMSException exception) {
        LOGGER.error(JMS_MARKER, "Exception in message listener", exception);
      }
    });
  }

  private void initResponseToClientProducer(Session session) throws JMSException {
    final MessageProducer producer = this.responseToClientProducer = session.createProducer(null);
    producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    producer.setTimeToLive(10000);
  }

  private void initMainRoomTopic(final Session session) throws JMSException {
    this.mainRoomTopic = (ActiveMQTopic) session.createTopic(this.properties.getProperty("topic.mainRoom"));
  }

  private void initServerMessageTopic(final Session session) throws JMSException {
    this.serverMessageTopic = (ActiveMQTopic) session.createTopic(this.properties.getProperty("topic.serverMessage"));
  }

  private void handleLogin(final String nick, final Message message) throws JMSException {
    this.connectedUsers.add(new ConnectedUser().setNick(nick));
    final Session session = this.connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

    final LoginResponseMessage lrm = new LoginResponseMessage()
        .setConnectedUsers(FluentIterable.from(this.connectedUsers).transform(ConnectedUser::getNick).toSet())
        .setMainRoomName(this.mainRoomTopic.getTopicName());

    this.responseToClientProducer.send(message.getJMSReplyTo(), session.createObjectMessage(lrm));

    this.connectedUsers
        .stream()
        .filter(user -> !user.getNick().equals(nick))
        .forEach(user -> {
          final UserListMessage ulm = new UserListMessage()
              .setUserList(FluentIterable.from(this.connectedUsers).transform(ConnectedUser::getNick).toSet());
          try {
            this.responseToClientProducer.send(this.serverMessageTopic, session.createObjectMessage(ulm));
          } catch (JMSException exception) {
            LOGGER.error(JMS_MARKER, "Exception when sending user list", exception);
          }
        });

    session.close();
  }

  private void handleLogout(final String login) throws JMSException {
    final Session session = this.connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
    this.connectedUsers.removeIf(user -> user.getNick().equals(login));
    this.connectedUsers.stream().forEach(user -> {
      final UserListMessage ulm = new UserListMessage()
          .setUserList(FluentIterable.from(this.connectedUsers).transform(ConnectedUser::getNick).toSet());
      try {
        this.responseToClientProducer.send(this.serverMessageTopic, session.createObjectMessage(ulm));
      } catch (JMSException exception) {
        LOGGER.error(JMS_MARKER, "Exception when sending user list", exception);
      }
    });
    session.close();
  }

  @Override
  protected void doStop() {
    if (this.connection != null) {
      try {
        this.responseToClientProducer = null;
        this.consumer = null;
        this.connection.close();
      } catch (JMSException exception) {
        LOGGER.error(JMS_MARKER, "Exception when shutting down", exception);
      } finally {
        this.connection = null;
      }
    }
  }

  private static class ConnectedUser {
    private String nick = null;

    public String getNick() {
      return nick;
    }

    public ConnectedUser setNick(final String nick) {
      this.nick = nick;
      return this;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(nick);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ConnectedUser that = (ConnectedUser) o;

      return Objects.equal(this.nick, that.nick);
    }
  }
}
