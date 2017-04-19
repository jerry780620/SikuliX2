/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package org.sikuli.script;

import com.sikulix.api.Target;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class Pattern extends Target {
  private static SXLog log = SX.getSXLog("API.PATTERN");

  public Pattern(String name) {
    super(name);
  }

  @Override
  public String toString() {
    String sOffset = (getTarget().x != 0 || getTarget().y != 0) ? "T: " + getTarget().x + "," + getTarget().y : "";
    return String.format("Pattern(%s) S: %d%% %s", getName(), (int) (getSimilar() * 100), sOffset);
  }

}

