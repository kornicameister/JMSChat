package org.kornicameister.jsocket.common.user;


import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.io.Serializable;

public class JatUser
    implements Serializable {
  private static final long   serialVersionUID = -182938398752147162L;
  private              String nick             = null;

  public String getNick() {
    return nick;
  }

  public JatUser setNick(final String nick) {
    this.nick = nick;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(serialVersionUID, nick);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    JatUser that = (JatUser) o;

    return Objects.equal(this.nick, that.nick);
  }


  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("nick", nick)
        .toString();
  }
}
