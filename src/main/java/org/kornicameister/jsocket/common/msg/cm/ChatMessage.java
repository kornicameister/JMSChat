package org.kornicameister.jsocket.common.msg.cm;


import org.joda.time.DateTime;

import java.io.Serializable;

public class ChatMessage
    implements Serializable {
  private static final long serialVersionUID = -2662402683643893989L;
  private DateTime sent;
  private String   text;
  private String   user;
  private String room = null;

  public String getRoom() {
    return room;
  }

  public ChatMessage setRoom(final String room) {
    this.room = room;
    return this;
  }

  public DateTime getSent() {
    return sent;
  }

  public ChatMessage setSent(final DateTime sent) {
    this.sent = sent;
    return this;
  }

  public String getText() {
    return text;
  }

  public ChatMessage setText(final String text) {
    this.text = text;
    return this;
  }

  public String getUser() {
    return user;
  }

  public ChatMessage setUser(final String user) {
    this.user = user;
    return this;
  }
}
