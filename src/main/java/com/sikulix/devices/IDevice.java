/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.devices;

import com.sikulix.api.Element;
import com.sikulix.api.Picture;
import com.sikulix.core.Content;
import com.sikulix.core.SX;

import java.awt.*;

public abstract class IDevice {

  public enum Action {
    LEFT, LEFTDOWN, LEFTUP, LEFTDOUBLE,
    RIGHT, RIGHTDOWN, RIGHTUP, RIGHTDOUBLE,
    MIDDLE, MIDDLEDOWN, MIDDLEUP, MIDDLEDOUBLE,
    UP, DOWN, DOWNUP
  }

  public enum KeyMode {
    PRESS_ONLY, RELEASE_ONLY, PRESS_RELEASE
  }

  ;

  public static boolean load(Class clazz) {
    if (SX.isSet(Content.whereIs(clazz))) {
      return true;
    }
    return false;
  }

  public abstract IDevice start(Object... args);

  public abstract void stop();

  public abstract boolean isValid();

  public abstract int getNumberOfMonitors();

  public abstract Rectangle getMonitor(int... id);

  public abstract Rectangle getAllMonitors();

  public abstract int getMonitorID();

  public abstract int getMonitorID(int id);

  public abstract void resetMonitors();

  public abstract Rectangle[] getMonitors();

  public abstract int getContainingMonitorID(Element element);

  public abstract Element getContainingMonitor(Element element);

  public abstract Element click(Element loc);

  public abstract Element doubleClick(Element loc);

  public abstract Element rightClick(Element loc);

  public abstract Element click(Action action);

  public abstract Element click(Element loc, Action action);

  public abstract Element dragDrop(Element from, Element to, Object... times);

  public abstract void keyStart();

  public abstract void keyStop();

  public abstract void key(Action action, Object key);

  /**
   * move the mouse from the current position to the offset given by the parameters
   *
   * @param xoff horizontal offset (&lt; 0 left, &gt; 0 right)
   * @param yoff vertical offset (&lt; 0 up, &gt; 0 down)
   * @return the new mouseposition as Element (might be invalid)
   */
  public abstract Element move(int xoff, int yoff);

  /**
   * move the mouse to the target of given Element (default center)
   *
   * @param loc
   * @return the new mouseposition as Element (might be invalid)
   */
  public abstract Element move(Element loc);

  /**
   * @return the current mouseposition as Element (might be invalid)
   */
  public abstract Element at();

  public abstract void button(Action action);

  public abstract void wheel(Action action, int steps);

  public abstract Picture capture(Object... args);
}
