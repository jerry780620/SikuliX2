/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.run.Runner;

import java.awt.*;
import java.net.URL;

public class Window extends Element {

  private static eType eClazz = eType.WINDOW;
  public eType getType() {
    return eClazz;
  }

  private static SXLog log = SX.getSXLog("SX." + eClazz.toString());

  private String application = "";

  public Window(String application) {
    this.application = application;
  }

  public boolean toFront() {
    if (SX.isMac()) {
      String script = String.format("tell app \"%s\" to activate", application);
      Object run = Runner.run(Runner.ScriptType.APPLESCRIPT, script);
      return true;
    }
    return false;
  }

  /**
   * open the given url in the standard browser
   *
   * @param url string representing a valid url
   * @return false on error, true otherwise
   */
  public static boolean openURL(String url) {
    try {
      URL u = new URL(url);
      Desktop.getDesktop().browse(u.toURI());
    } catch (Exception ex) {
      log.error("show in browser: bad URL: " + url);
      return false;
    }
    return true;
  }
}
