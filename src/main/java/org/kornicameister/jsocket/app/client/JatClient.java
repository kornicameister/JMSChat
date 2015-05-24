package org.kornicameister.jsocket.app.client;


import org.kornicameister.jsocket.common.callback.Callback;

public interface JatClient {

  void registerCallback(final CallbackType ct, final Callback callback);

  enum CallbackType {
    CHAT_LIST,
    CHAT_MESSAGE,
    USER_LIST,
    ERROR
  }
}

