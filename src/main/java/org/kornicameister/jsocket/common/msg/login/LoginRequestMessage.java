package org.kornicameister.jsocket.common.msg.login;


import java.io.Serializable;

public class LoginRequestMessage
    implements Serializable {
  private String login;

  public String getLogin() {
    return login;
  }

  public LoginRequestMessage setLogin(final String login) {
    this.login = login;
    return this;
  }
}
