/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.run.Runner;

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
}
