package org.kornicameister.jsocket.app.client.ctrl;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.util.Callback;
import org.kornicameister.jsocket.common.msg.cm.ChatMessage;

import java.net.URL;
import java.util.ResourceBundle;

public class ChatController
    implements Initializable {
  @FXML
  private ListView<ChatMessage> messageList;

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.messageList.setCellFactory(
        new Callback<ListView<ChatMessage>, ListCell<ChatMessage>>() {
          @Override
          public ListCell<ChatMessage> call(final ListView<ChatMessage> param) {
            return new ListCell<ChatMessage>() {
              @Override
              protected void updateItem(final ChatMessage item, final boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {

                  this.setWrapText(true);
                  this.setTextOverrun(OverrunStyle.LEADING_WORD_ELLIPSIS);

                  this.setText(
                      String.format("[%s]:%s >> %s", item.getUser(), item.getSent().toString("hh:MM"), item.getText())
                  );
                }
              }
            };
          }
        }
    );
  }

  void displayChatMessage(final ChatMessage chatMessage) {
    Platform.runLater(() -> this.messageList.getItems().add(chatMessage));
  }
}
