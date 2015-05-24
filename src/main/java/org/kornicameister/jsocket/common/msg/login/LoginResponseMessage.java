package org.kornicameister.jsocket.common.msg.login;

import java.io.Serializable;
import java.util.Collection;

public class LoginResponseMessage
    implements Serializable {
  private static final long serialVersionUID = 6479942969904906699L;
  private Collection<String> connectedUsers;
  private String             mainRoomName;

  public Collection<String> getConnectedUsers() {
    return connectedUsers;
  }

  public LoginResponseMessage setConnectedUsers(final Collection<String> connectedUsers) {
    this.connectedUsers = connectedUsers;
    return this;
  }

  public String getMainRoomName() {
    return mainRoomName;
  }

  public LoginResponseMessage setMainRoomName(final String mainRoomName) {
    this.mainRoomName = mainRoomName;
    return this;
  }
}
