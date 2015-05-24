package org.kornicameister.jsocket.common.jms;


import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.transport.TransportListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.jms.Connection;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import java.io.IOException;

public enum JMSUtils {
  INSTANCE;

  public Connection newConection(final ConnectionInformation info) throws Exception {
    final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
    this.configureFactory(info, factory);
    return factory.createConnection();
  }

  private void configureFactory(final ConnectionInformation info, final ActiveMQConnectionFactory factory) {
    factory.setBrokerURL(info.getHost());
    factory.setClientID(info.getClientId());
    factory.setExceptionListener(new ConnectionExceptionListener(info.getClientId()));
    factory.setUseCompression(true);
    factory.setTransportListener(new TransportListener() {
      private Marker marker = MarkerManager.getMarker("TransportListener");
      private Logger logger = LogManager.getLogger(info.getClientId());

      @Override
      public void onCommand(final Object command) {
        this.logger.debug(this.marker, String.format("onCommand(command=%s)", command));
      }

      @Override
      public void onException(final IOException error) {
        this.logger.error(this.marker, String.format("onException(error=%s)", error));
      }

      @Override
      public void transportInterupted() {
        this.logger.warn(this.marker, "transportInterupted()");
      }

      @Override
      public void transportResumed() {
        this.logger.debug(this.marker, "transportResumed()");
      }
    });
  }

  public Connection newQueueConection(final ConnectionInformation info) throws Exception {
    final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
    this.configureFactory(info, factory);
    return factory.createQueueConnection();
  }

  public Connection newTopicConection(final ConnectionInformation info) throws Exception {
    final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
    this.configureFactory(info, factory);
    return factory.createTopicConnection();
  }

  private static class ConnectionExceptionListener
      implements ExceptionListener {
    private final Logger logger;

    public ConnectionExceptionListener(final String clientId) {
      this.logger = LogManager.getLogger(clientId);
    }

    @Override
    public void onException(final JMSException exception) {
      this.logger.error(MarkerManager.getMarker("JMS:Error"), "Exception caught in jms connection", exception);
    }
  }
}
