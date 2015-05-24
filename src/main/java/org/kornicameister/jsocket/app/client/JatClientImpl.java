package org.kornicameister.jsocket.app.client;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.activemq.ActiveMQSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.joda.time.DateTime;
import org.kornicameister.jsocket.common.JatApplication;
import org.kornicameister.jsocket.common.callback.Callback;
import org.kornicameister.jsocket.common.jms.ConnectionInformation;
import org.kornicameister.jsocket.common.jms.JMSUtils;
import org.kornicameister.jsocket.common.msg.UserListMessage;
import org.kornicameister.jsocket.common.msg.cm.ChatMessage;
import org.kornicameister.jsocket.common.msg.login.LoginRequestMessage;
import org.kornicameister.jsocket.common.msg.login.LoginResponseMessage;
import org.kornicameister.jsocket.common.msg.logout.LogoutRequestMessage;
import org.kornicameister.jsocket.common.user.JatUser;

import javax.jms.*;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

class JatClientImpl
    extends JatApplication
    implements JatClient {
  private static final Logger                            LOGGER      = LogManager.getLogger(JatClientImpl.class);
  private static final Marker                            JMS_MARKER  = MarkerManager.getMarker("jms:client");
  private              Connection                        connection  = null;
  private              JatUser                           user        = null;
  private              MessageProducer                   producer    = null;
  private              Queue                             serverQueue = null;
  private              ActiveMQSession                   session     = null;
  private              Set<ChatModel>                    chats       = Sets.newHashSet();
  private              Map<CallbackType, List<Callback>> callbackMap = Maps.newHashMapWithExpectedSize(CallbackType.values().length);

  @Override
  public void registerCallback(final CallbackType ct, final Callback callback) {
    List<Callback> list = this.callbackMap.get(ct);
    if (list == null) {
      list = Lists.newArrayList();
      this.callbackMap.put(ct, list);
    }
    list.add(callback);
  }

  @Override
  protected void doStart() {
  }

  @Override
  protected void doStop() {
    if (this.connection != null) {
      try {
        this.disconnect();
        this.connection.close();
      } catch (JMSException exception) {
        LOGGER.error(JMS_MARKER, "Exception when shutting down", exception);
        this.callCallbacks(CallbackType.ERROR, exception);
      } finally {
        this.connection = null;
      }
    }
  }

  private void disconnect() throws JMSException {
    this.producer.send(this.session.createObjectMessage(new LogoutRequestMessage().setLogin(this.user.getNick())));
  }

  private void callCallbacks(final CallbackType ct, final Object data) {
    final List<Callback> callbacks = this.callbackMap.get(ct);
    if (callbacks != null) {
      callbacks.stream().forEach(callback -> callback.call(data));
    }
  }

  public void connect(final String username) throws Exception {
    this.user = new JatUser().setNick(username);

    this.connection = JMSUtils.INSTANCE.newConection(new ConnectionInformation() {
      @Override
      public String getHost() {
        return properties.getProperty("activemq.broker-url");
      }

      @Override
      public String getClientId() {
        return user.getNick();
      }
    });
    this.connection.start();

    this.session = (ActiveMQSession) this.connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

    this.initClient();
    this.doConnect();
  }

  private void initClient() throws JMSException {
    // set up queues
    {
      this.initProducer();
      this.initMessageListener();
      this.initServerMessageTopicConsumer();
    }
    // set up queues
  }

  private void doConnect() throws JMSException {
    final ObjectMessage objectMessage = this.session.createObjectMessage(new LoginRequestMessage().setLogin(this.user.getNick()));
    objectMessage.setJMSReplyTo(this.serverQueue);
    objectMessage.setJMSCorrelationID(objectMessage.getJMSMessageID());
    this.producer.send(objectMessage);
  }

  private void initProducer() throws JMSException {
    this.producer = this.session.createProducer((Queue) () -> this.properties.getProperty("queue.name"));
  }

  private void initMessageListener() throws JMSException {
    final TemporaryQueue queue = this.session.createTemporaryQueue();
    final MessageConsumer consumer = this.session.createConsumer(queue);
    consumer.setMessageListener(message -> {
      try {
        final ObjectMessage om = (ObjectMessage) message;
        final Serializable object = om.getObject();

        if (object instanceof LoginResponseMessage) {
          this.handleLogin((LoginResponseMessage) object);
          message.acknowledge();
        }

      } catch (Exception exception) {
        LOGGER.error(JMS_MARKER, "Exception in message listener", exception);
        this.callCallbacks(CallbackType.ERROR, exception);
      }
    });

    this.serverQueue = queue;
  }

  private void initServerMessageTopicConsumer() throws JMSException {
    this.session
        .createConsumer(this.session.createTopic(this.properties.getProperty("topic.serverMessage")))
        .setMessageListener(msg -> {
          final ObjectMessage om = (ObjectMessage) msg;
          try {
            final Serializable object = om.getObject();
            if (object instanceof UserListMessage) {
              final UserListMessage userListMessage = (UserListMessage) object;
              this.callCallbacks(CallbackType.USER_LIST, userListMessage.getUserList());
            }
            msg.acknowledge();
          } catch (JMSException exception) {
            LOGGER.error(JMS_MARKER, "Exception in message listener", exception);
            this.callCallbacks(CallbackType.ERROR, exception);
          }
        });
  }

  private void handleLogin(final LoginResponseMessage object) throws Exception {
    this.createChat(object.getMainRoomName());
    this.callCallbacks(CallbackType.USER_LIST, object.getConnectedUsers());
    this.callCallbacks(CallbackType.CHAT_LIST, FluentIterable.from(this.chats).transform(ChatModel::getChatName));
  }

  private void createChat(final String mainRoomName) throws JMSException {
    final Topic topic = this.session.createTopic(mainRoomName);
    this.chats.add(
        new ChatModel()
            .setChatName(mainRoomName)
            .setPublisher(this.session.createPublisher(topic))
            .setSubscriber(this.session.createSubscriber(topic))
    );
  }

  public void sendMessage(final String roomId, final String text) throws JMSException {
    final ChatModel chatModel = FluentIterable
        .from(this.chats)
        .firstMatch(chat -> chat.getChatName().equalsIgnoreCase(roomId))
        .get();

    chatModel
        .publisher
        .publish(
            this.session.createObjectMessage(
                new ChatMessage()
                    .setSent(DateTime.now())
                    .setText(text)
                    .setUser(this.user.getNick())
                    .setRoom(roomId)
            )
        );
  }

  private class ChatModel {
    private String         chatName  = null;
    private TopicPublisher publisher = null;

    public ChatModel setPublisher(final TopicPublisher publisher) {
      this.publisher = publisher;
      return this;
    }

    public ChatModel setSubscriber(final TopicSubscriber subscriber) throws JMSException {
      subscriber.setMessageListener(message -> {
        final ObjectMessage om = (ObjectMessage) message;
        final ChatMessage cm;
        try {
          cm = (ChatMessage) om.getObject();
          JatClientImpl.this.callCallbacks(CallbackType.CHAT_MESSAGE, cm);
        } catch (JMSException exception) {
          LOGGER.error(JMS_MARKER, String.format("Error in topic %s subscription", this.getChatName()), exception);
          JatClientImpl.this.callCallbacks(CallbackType.ERROR, exception);
        }
      });
      return this;
    }

    public String getChatName() {
      return chatName;
    }

    public ChatModel setChatName(final String chatName) {
      this.chatName = chatName;
      return this;
    }
  }

}
