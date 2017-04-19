/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package org.sikuli.script;

import com.sikulix.api.Element;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class Match extends Region {
  private static SXLog log = SX.getSXLog("API.MATCH");

  public  Match(Element elem) {
    x = elem.x;
    y = elem.y;
    w = elem.w;
    h = elem.h;
    setScore(elem.getScore());
    setTarget(elem.getTarget());
  }

  public boolean isMatch() {
    return true;
  }

}
