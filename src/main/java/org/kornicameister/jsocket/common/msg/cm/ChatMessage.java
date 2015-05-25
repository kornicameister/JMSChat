package org.kornicameister.jsocket.common.msg.cm;


import com.google.common.base.Objects;
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

  @Override
  public int hashCode() {
    return Objects.hashCode(serialVersionUID, sent, user, room);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ChatMessage that = (ChatMessage) o;

    return Objects.equal(this.sent, that.sent) &&
        Objects.equal(this.user, that.user) &&
        Objects.equal(this.room, that.room);
  }
}
