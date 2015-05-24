package org.kornicameister.jsocket.common.msg.logout;


import java.io.Serializable;

public class LogoutRequestMessage
    implements Serializable {
  private static final long serialVersionUID = -8487418881930714142L;
  private String login;

  public String getLogin() {
    return login;
  }

  public LogoutRequestMessage setLogin(final String login) {
    this.login = login;
    return this;
  }
}
