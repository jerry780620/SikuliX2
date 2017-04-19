/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package org.sikuli.script;

import com.sikulix.api.Element;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class Location extends Element {
  private static SXLog log = SX.getSXLog("API.LOCATION");

  private static eType eClazz = eType.LOCATION;
  public eType getType() {
    return eClazz;
  }

  public String toStringPlus() {
    return " #" + getScreen().getID();
  }

  public Location(int x, int y) {
    this.x = x;
    this.y = y;
    initScreen(null);
  }

  public Location(Element element) {
    x = element.x;
    y = element.y;
    initScreen(null);
  }

}
