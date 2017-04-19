/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package org.sikuli.script;

import com.sikulix.api.Picture;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class Image extends Picture {
  private static SXLog log = SX.getSXLog("API.IMAGE");

  public String toString() {
    return String.format("Image(%s)", getName());
  }

  public Image(String name) {
    super(name);
  }
}
