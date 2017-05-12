/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.devices.android;

import com.sikulix.api.Picture;
import com.sikulix.core.SX;
import org.sikuli.script.IScreen;
import org.sikuli.script.Location;

import java.awt.*;

/**
 * Created by Törcsi on 2016. 06. 26.
 * Revised by RaiMan
 */
public class ADBRobot {

  private int mouse_X1 = -1;
  private int mouse_Y1 = -1;

  private int mouse_X2 = -1;
  private int mouse_Y2 = -1;


  private boolean mouseDown = false;

  private int autodelay = 0;
  private boolean waitForIdle = false;
  final static int MAX_DELAY = 60000;

  private ADBScreen screen;
  private ADBDevice device;

  public ADBRobot(ADBScreen screen, ADBDevice device) {
    this.screen = screen;
    this.device = device;
  }

  private void notSupported(String feature) {
    //Debug.error("ADBRobot: %s: not supported yet", feature);

  }

  public boolean isRemote() {
    return true;
  }

  public IScreen getScreen() {
    return screen;
  }

  public void cleanup() {
    notSupported("feature");
  }

  //<editor-fold desc="key actions not supported yet">
  public void keyDown(String keys) {
    notSupported("keyDown");
  }

  public void keyUp(String keys) {
    notSupported("keyUp");
  }

  public void keyDown(int code) {
    notSupported("keyDown");
  }

  public void keyUp(int code) {
    notSupported("keyUp");
  }

  public void keyUp() {
    notSupported("keyUp");
  }

  public void pressModifiers(int modifiers) {
    if (modifiers != 0) {
      notSupported("pressModifiers");
    }
  }

  public void releaseModifiers(int modifiers) {
    if (modifiers != 0) {
      notSupported("releaseModifiers");
    }
  }

  public void typeChar(char character, int mode) {
    if (device == null) {
      return;
    }
    device.typeChar(character);
  }

  public void typeKey(int key) {
    notSupported("typeKey");
  }

  public void typeStarts() {
    if (device == null) {
      return;
    }
    while (!device.typeStarts()) {
      SX.pause(1);
    }
  }

  public void typeEnds() {
    if (device == null) {
      return;
    }
    device.typeEnds();
  }

  //</editor-fold>

  public void mouseMove(int x, int y) {
    if (!mouseDown) {
      mouse_X1 = x;
      mouse_Y1 = y;
    } else {
      mouse_X2 = x;
      mouse_Y2 = y;
    }
  }

  public void mouseDown(int buttons) {
    clickStarts();
  }

  public int mouseUp(int buttons) {
    clickEnds();
    return 0;
  }

  public void mouseReset() {
    mouseDown = false;
  }

  public void clickStarts() {
    mouseDown = true;
    mouse_X2 = mouse_X1;
    mouse_Y2 = mouse_Y1;
  }

  public void clickEnds() {
    if (device == null) {
      return;
    }
    if (mouseDown) {
      mouseDown = false;
      if (mouse_X1 == mouse_X2 && mouse_Y1 == mouse_Y2) {
        device.tap(mouse_X1, mouse_Y1);
      } else {
        device.swipe(mouse_X1, mouse_Y1, mouse_X2, mouse_Y2);
      }
    }
  }

  //<editor-fold desc="mouse actions not supported yet">
  public void smoothMove(Location dest) {
    mouseMove(dest.x, dest.y);
  }

  public void smoothMove(Location src, Location dest, long ms) {
    notSupported("smoothMove");
  }

  public void mouseWheel(int wheelAmt) {
    notSupported("mouseWheel");
  }
  //</editor-fold>

  public Picture captureScreen(Rectangle screenRect) {
    if (device == null) {
      return null;
    }
    return device.captureScreen(screenRect);
  }

  public Color getColorAt(int x, int y) {
    notSupported("getColorAt");
    return null;
  }

  public void waitForIdle() {
    try {
      new java.awt.Robot().waitForIdle();
    } catch (AWTException e) {
//      Debug.log(-1, "Error-could non instantiate robot: " + e);
    }
  }

  public void delay(int ms) {
    if (ms < 0) {
      ms = 0;
    }
    if (ms > MAX_DELAY) {
      ms = MAX_DELAY;
    }
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
//      Debug.log(-1, "Thread Interrupted: " + e);
    }
  }

  public void setAutoDelay(int ms) {
    if (ms < 0) {
      ms = 0;
    }
    if (ms > MAX_DELAY) {
      ms = MAX_DELAY;
    }
    autodelay = ms;
  }
}

