package org.kornicameister.jsocket;

import javafx.application.Application;
import org.kornicameister.jsocket.app.client.ClientApplication;
import org.kornicameister.jsocket.app.server.ServerApplication;
import org.kornicameister.jsocket.common.FunctionalSite;

public class Main {
  private static final FunctionalSite DEFAULT_MODE = FunctionalSite.SERVER;

  public static void main(String[] args) {
    final FunctionalSite functionalSite = Main.getCommSite(args);
    System.setProperty("org.apache.activemq.default.directory.prefix", String.format("%s-", functionalSite.toString().toLowerCase()));

    switch (functionalSite) {
      case CLIENT:
        Application.launch(ClientApplication.class, args);
        break;
      case SERVER:
        ServerApplication.launch();
        break;
    }

  }

  private static FunctionalSite getCommSite(final String[] args) {
    if (args.length == 0) {
      return DEFAULT_MODE;
    }
    final String commSite = args[0].toUpperCase();
    FunctionalSite site = null;

    try {
      site = FunctionalSite.valueOf(commSite);
    } catch (Exception ignored) {
    }

    return site != null ? site : DEFAULT_MODE;
  }

}
