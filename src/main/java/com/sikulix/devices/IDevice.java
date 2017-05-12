/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.devices;

import com.sikulix.api.Element;
import com.sikulix.api.Picture;

import java.awt.*;

public interface IDevice {

  enum Action {
    LEFT, LEFTDOWN, LEFTUP, LEFTDOUBLE,
    RIGHT, RIGHTDOWN, RIGHTUP, RIGHTDOUBLE,
    MIDDLE, MIDDLEDOWN, MIDDLEUP, MIDDLEDOUBLE,
    UP, DOWN, DOWNUP
  }

  enum KeyMode {
    PRESS_ONLY, RELEASE_ONLY, PRESS_RELEASE
  };

  IDevice start(Object... args);
  void stop();
  boolean isValid();

  int getNumberOfMonitors();
  Rectangle getMonitor(int... id);
  Rectangle getAllMonitors();
  int getMonitorID();
  int getMonitorID(int id);
  void resetMonitors();
  Rectangle[] getMonitors();
  int getContainingMonitorID(Element element);
  Element getContainingMonitor(Element element);
  Element click(Element loc);
  Element doubleClick(Element loc);
  Element rightClick(Element loc);
  Element click(Action action);
  Element click(Element loc, Action action);
  Element dragDrop(Element from, Element to, Object... times);
  void keyStart();
  void keyStop();
  void key(Action action, Object key);

  /**
   * move the mouse from the current position to the offset given by the parameters
   *
   * @param xoff horizontal offset (&lt; 0 left, &gt; 0 right)
   * @param yoff vertical offset (&lt; 0 up, &gt; 0 down)
   * @return the new mouseposition as Element (might be invalid)
   */
  Element move(int xoff, int yoff);

  /**
   * move the mouse to the target of given Element (default center)
   *
   * @param loc
   * @return the new mouseposition as Element (might be invalid)
   */
  Element move(Element loc);

  /**
   * @return the current mouseposition as Element (might be invalid)
   */
  Element at();

  void button(Action action);

  void wheel(Action action, int steps);

  Picture capture(Object... args);
  }
