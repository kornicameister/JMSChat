package org.kornicameister.jsocket.app.client.ctrl;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.util.Callback;
import org.joda.time.DateTime;
import org.kornicameister.jsocket.app.client.ClientApplication;
import org.kornicameister.jsocket.app.client.JatClient;
import org.kornicameister.jsocket.app.client.util.DialogsFactory;
import org.kornicameister.jsocket.common.msg.cm.ChatMessage;

import java.net.URL;
import java.util.Iterator;
import java.util.ResourceBundle;

import static org.kornicameister.jsocket.app.client.JatClient.CallbackType.*;

public class MainViewController
    implements Initializable {
  @FXML
  private ListView<String>      userList;
  @FXML
  private ListView<ChatMessage> chatListView;
  @FXML
  private TextArea              messageTextArea;
  @FXML
  private Button                sendButton;
  @FXML
  private Button                logoutButton;
  @FXML
  private MenuItem              exitItem;

  // main app
  private ClientApplication mainApp;

  // local for controller
  private ObservableSet<ChatMessage> chatHistory;
  private JatClient                  jat;

  public void setJat(final JatClient jat) {
    this.jat = jat;
    this.registerCallbacks();
  }

  private void registerCallbacks() {
    this.onUserListCallback();
    this.onChatMessageCallback();
    this.onErrorCallback();
  }

  private void onUserListCallback() {
    this.jat.registerCallback(USER_LIST, listOfUsers -> {
      Platform.runLater(() -> {
        final Iterable<?> tmp = (Iterable<?>) listOfUsers;
        final Iterator<?> iterator = tmp.iterator();
        final ObservableList<String> users = FXCollections.observableArrayList();
        final ObservableList<String> items = this.userList.getItems();

        String userName;

        while (iterator.hasNext()) {
          userName = (String) iterator.next();
          users.add(userName);
        }

        if (!items.isEmpty()) {
          users.forEach(nick -> {
            if (!items.contains(nick)) {
              this.chatHistory.add(
                  new ChatMessage()
                      .setSent(DateTime.now())
                      .setText(String.format("%s just %s", nick, users.size() > items.size() ? "joined" : "left"))
                      .setUser("SYSTEM")
              );
            }
          });
          items.forEach(nick -> {
            if (!users.contains(nick)) {
              this.chatHistory.add(
                  new ChatMessage()
                      .setSent(DateTime.now())
                      .setText(String.format("%s just %s", nick, users.size() > items.size() ? "joined" : "left"))
                      .setUser("SYSTEM")
              );
            }
          });
        }

        this.userList.setItems(users);
      });
    });
  }

  private void onChatMessageCallback() {
    this.jat.registerCallback(CHAT_MESSAGE, cm -> {
      final ChatMessage chatMessage = (ChatMessage) cm;
      Platform.runLater(() -> this.chatHistory.add(chatMessage));
    });
  }

  private void onErrorCallback() {
    this.jat.registerCallback(ERROR, error -> DialogsFactory.INSTANCE.showErrorDialog((Throwable) error));
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.initSendMessageBtn();
    this.initLogoutBtn();
    this.initExitMenuItem();
    this.initChatListView();
  }

  private void initSendMessageBtn() {
    this.sendButton.setOnMouseClicked((event) -> this.sendMessage());
    this.messageTextArea.setOnKeyReleased((event -> {
      final KeyCode code = event.getCode();
      if (code.equals(KeyCode.ENTER)) {
        if (!event.isShiftDown()) {
          if (this.messageTextArea.getText().trim().isEmpty()) {
            this.messageTextArea.clear();
          } else {
            this.sendMessage();
          }
        } else {
          this.messageTextArea.nextWord();
        }
      }
    }));
  }

  private void initLogoutBtn() {
    this.logoutButton.setOnAction(e -> this.mainApp.exit());
  }

  private void initExitMenuItem() {
    this.exitItem.setOnAction(e -> this.mainApp.exit());
  }

  private void initChatListView() {
    this.chatListView.setPadding(new Insets(5.0));
    this.chatListView.setCellFactory(
        new Callback<ListView<ChatMessage>, ListCell<ChatMessage>>() {
          @Override
          public ListCell<ChatMessage> call(final ListView<ChatMessage> param) {
            return new ListCell<ChatMessage>() {
              @Override
              protected void updateItem(final ChatMessage item, final boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {

                  this.setWrapText(true);
                  this.setMaxWidth(150);

                  if (item.getUser().equals("SYSTEM")) {
                    this.setText(
                        String.format("%s >> %s", item.getSent().toString("hh:MM"), item.getText())
                    );
                  } else {
                    this.setText(
                        String.format("[%s]:%s >> %s", item.getUser(), item.getSent().toString("hh:MM"), item.getText())
                    );
                  }
                }
              }
            };
          }
        }
    );
    this.chatHistory = FXCollections.observableSet();
    this.chatHistory.addListener((SetChangeListener<ChatMessage>) change -> {
      final ObservableList<ChatMessage> messages = this.chatListView.getItems();
      messages.add(change.getElementAdded());
      messages.remove(change.getElementRemoved());
      messages.sorted((o1, o2) -> o1.getSent().compareTo(o2.getSent()));
    });
  }

  private void sendMessage() {
    final String text = this.messageTextArea.getText();
    if ((text == null || text.isEmpty()) || (text.trim().isEmpty())) {
      return;
    }
    try {
      this.mainApp.sendMessage(text);
    } catch (Exception exception) {
      DialogsFactory.INSTANCE.showErrorDialog(exception);
    }
    this.messageTextArea.requestFocus();
    this.messageTextArea.clear();
  }

  public MainViewController setMainApp(final ClientApplication mainApp) {
    this.mainApp = mainApp;
    return this;
  }

}
