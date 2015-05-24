package org.kornicameister.jsocket.common;


interface Application
    extends Runnable {
  /**
   * Logic to be executed when terminating the application
   */
  void stop();

  /**
   * Logic to be executed when starting the application
   */
  void start();
}
