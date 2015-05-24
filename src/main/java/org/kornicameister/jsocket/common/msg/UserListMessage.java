package org.kornicameister.jsocket.common.msg;


import java.io.Serializable;
import java.util.Set;

public class UserListMessage
    implements Serializable {
  private static final long        serialVersionUID = -9129465574308173483L;
  private              Set<String> userList         = null;

  public Set<String> getUserList() {
    return userList;
  }

  public UserListMessage setUserList(final Set<String> userList) {
    this.userList = userList;
    return this;
  }
}
