package org.kornicameister.jsocket.common;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;
import java.util.Properties;

public abstract class JatApplication
    implements Application {
  private static final Logger     LOGGER     = LogManager.getLogger(JatApplication.class);
  protected            Properties properties = null;

  @Override
  public void stop() {
    LOGGER.entry("Entering stop()");
    try {
      this.doStop();
    } catch (Throwable throwable) {
      LOGGER.error(MarkerManager.getMarker("error"), throwable.getMessage());
    }
    LOGGER.exit("Exiting stop()");
  }

  @Override
  public void start() {
    LOGGER.entry("Entering start()");
    try {
      this.loadProperties();
      this.doStart();
    } catch (Throwable throwable) {
      LOGGER.error(MarkerManager.getMarker("error"), throwable.getMessage());
    }
    LOGGER.exit("Exiting start()");
  }

  protected abstract void doStop();

  @Override
  public final void run() {
    this.start();
  }

  private void loadProperties() throws IOException {
    final Properties properties = new Properties();
    try {
      properties.load(ClassLoader.getSystemResourceAsStream("application.properties"));
    } catch (IOException exception) {
      LOGGER.error(MarkerManager.getMarker("properties"), "Failed to load properties", exception);
      throw exception;
    }
    this.properties = properties;
  }

  protected abstract void doStart();

}
