package org.kornicameister.jsocket.app.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.kornicameister.jsocket.app.client.ctrl.MainViewController;

import javax.jms.JMSException;
import java.io.IOException;

public class ClientApplication
    extends Application {
  private static final Logger             LOGGER     = LogManager.getLogger(ClientApplication.class);
  private static final Marker             JMS_MARKER = MarkerManager.getMarker("client:gui");
  private              Stage              stage      = null;
  private              JatClientImpl      app        = null;
  private              MainViewController controller = null;

  public void sendMessage(final String roomId, final String text) throws JMSException {
    this.app.sendMessage(roomId, text);
  }

  @Override
  public void init() throws Exception {
    super.init();
    this.initJatClient();
  }

  private void initJatClient() {
    final JatClientImpl app = new JatClientImpl();

    final Thread appThread = new Thread(app, "ServerApplicationThread");
    appThread.setUncaughtExceptionHandler((thread, error) -> LOGGER.error(String.format("%s threw an error with msg %s", thread.getName(), error.getMessage())));
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

    this.app = app;
  }

  @Override
  public void start(final Stage primaryStage) throws Exception {
    this.stage = primaryStage;
    this.stage.setTitle("Chat Application");

    this.initRootLayout();
    this.initUiController();
    this.showLoginDialog();
  }

  private void initRootLayout() {
    try {
      // Load root layout from fxml file.
      final FXMLLoader loader = new FXMLLoader();
      loader.setLocation(ClassLoader.getSystemResource("ui/client.fxml"));

      // Show the scene containing the root layout.
      Scene scene = new Scene(loader.load());
      stage.setScene(scene);
      stage.setResizable(false);
      stage.show();

      stage.setOnCloseRequest(e -> this.exit());

      final MainViewController controller = loader.getController();
      this.controller = controller.setMainApp(this);

    } catch (IOException exception) {
      LOGGER.error(JMS_MARKER, "Error occured when loading main view", exception);
    }
  }

  private void initUiController() {
    this.controller.setJat(this.app);
  }

  private void showLoginDialog() {
    final TextInputDialog dialog = new TextInputDialog("...");

    dialog.setTitle("Log in to chat");
    dialog.setHeaderText("In order to log in into application, enter your nick and click Ok");
    dialog.setContentText("Please enter your nick: ");
    dialog.setOnCloseRequest(e -> {
      if (dialog.getResult() == null) {
        this.exit();
      }
    });

    dialog.showAndWait().ifPresent(name -> {
      try {
        this.app.connect(name);
        this.stage.setTitle(name);
      } catch (Exception exception) {
        LOGGER.error(JMS_MARKER, "Error occured when connecting to server", exception);
      }
    });
  }

  public void exit() {
    this.app.stop();
    Platform.exit();
  }
}
