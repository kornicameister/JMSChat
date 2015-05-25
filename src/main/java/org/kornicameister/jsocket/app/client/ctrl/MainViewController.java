package org.kornicameister.jsocket.app.client.ctrl;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import org.kornicameister.jsocket.app.client.ClientApplication;
import org.kornicameister.jsocket.app.client.JatClient;
import org.kornicameister.jsocket.app.client.util.DialogsFactory;
import org.kornicameister.jsocket.common.msg.cm.ChatMessage;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.ResourceBundle;

import static org.kornicameister.jsocket.app.client.JatClient.CallbackType.*;

public class MainViewController
    implements Initializable {
  private static final String NEW_MESSAGE = "newMessage";
  @FXML
  private ListView<String> userList;
  @FXML
  private TextArea         messageTextArea;
  @FXML
  private TabPane          roomTabPane;
  @FXML
  private Button           sendButton;
  @FXML
  private Button logoutButton;

  // main app
  private ClientApplication mainApp;

  // local for controller
  private String    activeTabId;
  private JatClient jat;

  public void setJat(final JatClient jat) {
    this.jat = jat;
    this.registerCallbacks();
  }

  private void registerCallbacks() {
    this.onUserListCallback();
    this.onChatListCallback();
    this.onChatMessageCallback();
    this.onErrorCallback();
  }

  private void onUserListCallback() {
    this.jat.registerCallback(USER_LIST, listOfUsers -> {
      final Iterable<?> tmp = (Iterable<?>) listOfUsers;
      final Iterator<?> iterator = tmp.iterator();
      final ObservableList<String> users = FXCollections.observableArrayList();
      String userName;

      while (iterator.hasNext()) {
        userName = (String) iterator.next();
        users.add(userName);
      }

      Platform.runLater(() -> this.userList.setItems(users));
    });
  }

  private void onChatListCallback() {
    this.jat.registerCallback(CHAT_LIST, listOfChats -> {
      final Iterable<?> tmp = (Iterable<?>) listOfChats;
      final Iterator<?> iterator = tmp.iterator();

      final ObservableList<String> chats = FXCollections.observableArrayList();
      String chatName;

      while (iterator.hasNext()) {
        chatName = (String) iterator.next();
        chats.add(chatName);
      }

      Platform.runLater(() -> this.setChatList(chats));
    });
  }

  private void onChatMessageCallback() {
    this.jat.registerCallback(CHAT_MESSAGE, cm -> {
      final ChatMessage chatMessage = (ChatMessage) cm;
      Platform.runLater(() -> this.displayChatMessage(chatMessage));
    });
  }

  private void onErrorCallback() {
    this.jat.registerCallback(ERROR, error -> {
      DialogsFactory.INSTANCE.showErrorDialog((Throwable) error);
    });
  }

  private void setChatList(final Iterable<String> chatList) {
    final ObservableList<Tab> tabs = this.roomTabPane.getTabs();
    chatList.forEach(chatName -> {
      try {
        final Tab tab = new Tab();

        final FXMLLoader loader = new FXMLLoader();
        loader.setLocation(ClassLoader.getSystemResource("ui/chat.fxml"));

        tab.setContent(loader.load());
        tab.setId(chatName);
        tab.setText(chatName);

        final ChatController controller = loader.getController();
        this.jat.registerCallback(CHAT_MESSAGE, cm -> {
          final ChatMessage chatMessage = (ChatMessage) cm;
          Platform.runLater(() -> controller.displayChatMessage(chatMessage));
        });

        tabs.add(tab);
      } catch (IOException e) {
        DialogsFactory.INSTANCE.showErrorDialog(e);
      }
    });
  }

  private void displayChatMessage(final ChatMessage chatMessage) {
    final Tab targetTab = this
        .roomTabPane
        .getTabs()
        .filtered(tab -> tab.getId().equals(chatMessage.getRoom())).get(0);
    if (!targetTab.isSelected()) {
      // add asterisk to tab title
      targetTab.getProperties().put("newMessage", true);
      targetTab.setText(String.format("%s*", targetTab.getText()));
    }
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.initTabs();
    this.initSendMessageBtn();
    this.initLogoutBtn();
  }

  private void initTabs() {
    final ObservableList<Tab> tabs = this.roomTabPane.getTabs();
    tabs.addListener((ListChangeListener<Tab>) tab -> {
      tab.getList().forEach((tt -> {
        if (tt.isSelected()) {
          this.activeTabId = tt.getId();
          final String text = tt.getText();
          final Boolean newMesage = (Boolean) tt.getProperties().getOrDefault(NEW_MESSAGE, false);
          if (newMesage) {
            tt.setText(text.substring(0, text.indexOf("*") - 1));
          }
        }
      }));
    });
    tabs.forEach(tab -> {
      if (tab.isSelected()) {
        this.activeTabId = tab.getId();
      }
    });
  }

  private void initSendMessageBtn() {
    this.sendButton.setOnMouseClicked((event) -> this.sendMessage());
    this.messageTextArea.setOnKeyReleased((event -> {
      if (event.getCode().compareTo(KeyCode.ENTER) == 0) {
        this.sendMessage();
      }
    }));
  }

  private void initLogoutBtn() {
    this.logoutButton.setOnAction(e -> {
      this.mainApp.exit();
    });
  }

  private void sendMessage() {
    final String text = this.messageTextArea.getText();
    if (text == null || text.isEmpty()) {
      return;
    }
    try {
      this.mainApp.sendMessage(this.activeTabId, text);
    } catch (Exception exception) {
      DialogsFactory.INSTANCE.showErrorDialog(exception);
    }
    this.messageTextArea.clear();
  }

  public MainViewController setMainApp(final ClientApplication mainApp) {
    this.mainApp = mainApp;
    return this;
  }

}
