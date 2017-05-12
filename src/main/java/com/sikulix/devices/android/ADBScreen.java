/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.devices.android;

import com.sikulix.api.Picture;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import org.sikuli.script.FindFailed;
import org.sikuli.script.IScreen;
import org.sikuli.script.Region;

import java.awt.*;

/**
 * Created by Törcsi on 2016. 06. 26.
 * Revised by RaiMan
 */
public class ADBScreen extends Region implements IScreen {

  static SXLog log = SX.getSXLog("SX.ADBScreen");

  private static String me = "ADBScreen: ";

  private static boolean isFake = false;
  protected ADBRobot robot = null;
  private static int logLvl = 3;
  private Picture lastPicture = null;
  private Rectangle bounds;

  private boolean waitPrompt = false;
//  protected OverlayCapturePrompt prompt;
  private String promptMsg = "Select a region on the screen";
  private static int waitForScreenshot = 300;

  public boolean needsUnLock = false;
  public int waitAfterAction = 1;

//---------------------------Inits
  private ADBGadget device = null;
  private static ADBScreen screen = null;

  public static ADBScreen start() {
    if (screen == null) {
      try {
        screen = new ADBScreen();
      } catch (Exception e) {
        log.error("start: No devices attached");
        screen = null;
      }
    }
    return screen;
  }

  public static void stop() {
      ADBGadget.reset();
      screen = null;
  }

  public ADBScreen() {
    super();
    setOtherScreen(this);

    device = ADBGadget.init();
    if (device != null) {
      robot = device.getRobot(this);
      robot.setAutoDelay(10);
      bounds = device.getBounds();
      w = bounds.width;
      h = bounds.height;
    }
  }

  private void setOtherScreen(ADBScreen adbScreen) {
  }

  public boolean isValid() {
    return null != device;
  }

  public ADBGadget getADBDevice() {
    return device;
  }

  public String toString() {
    if (null == device) {
      return "ADBScreen: No Android device attached";
    } else {
      return String.format("ADBScreen: Android device: %s", getDeviceDescription());
    }
  }

  public String getDeviceDescription() {
    return String.format("%s (%d x %d)", device.getDeviceSerial(), bounds.width, bounds.height);
  }

  public void wakeUp(int seconds) {
    if (null == device) {
      return;
    }
    if (null == device.isDisplayOn()) {
      log.error("wakeUp: not possible - see log");
      return;
    }
    if (!device.isDisplayOn()) {
      device.wakeUp(seconds);
      if (needsUnLock) {
        //TODO aSwipeUp();
      }
    }
  }

  public String exec(String command, String... args) {
    if (device == null) {
      return null;
    }
    return device.exec(command, args);
  }

  //-----------------------------Overrides
  @Override
  public IScreen getScreen() {
    return this;
  }

//  public void update(EventSubject s) {
//    waitPrompt = false;
//  }

//  public IRobot getRobot() {
//    return robot;
//  }

  @Override
  public Rectangle getBounds() {
    return bounds;
  }

  public Picture capture() {
    return capture(x, y, w, h);
  }

  public Picture capture(int x, int y, int w, int h) {
//    Picture simg = null;
//    if (device != null) {
//      log(logLvl, "ADBScreen.capture: (%d,%d) %dx%d", x, y, w, h);
//      simg = device.captureScreen(new Rectangle(x, y, w, h));
//    } else {
//      log(-1, "capture: no ADBRobot available");
//    }
//    lastPicture = simg;
    return new Picture();
  }

  public Picture capture(Region reg) {
    return capture(reg.x, reg.y, reg.w, reg.h);
  }

  public Picture capture(Rectangle rect) {
    return capture(rect.x, rect.y, rect.width, rect.height);
  }

//  public void showTarget(Location loc) {
//    showTarget(loc, Settings.SlowMotionDelay);
//  }
//
//  protected void showTarget(Location loc, double secs) {
//    if (Settings.isShowActions()) {
//      ScreenHighlighter overlay = new ScreenHighlighter(this, null);
//      overlay.showTarget(loc, (float) secs);
//    }
//  }

  @Override
  public boolean isOtherScreen() {
    return false;
  }

  @Override
  public int getID() {
    return 0;
  }

  public String getIDString() {
    return "Android";
  }

  public Picture getLastPictureFromScreen() {
    return lastPicture;
  }

//  private EventObserver captureObserver = null;

  public Picture userCapture(final String msg) {
//TODO userCapture
//    if (robot == null) {
//      return null;
//    }
//    waitPrompt = true;
//    Thread th = new Thread() {
//      @Override
//      public void run() {
//        prompt = new OverlayCapturePrompt(ADBScreen.this);
//        prompt.prompt(msg);
//      }
//    };
//
//    th.start();
//
//    boolean hasShot = false;
//    Picture simg = null;
//    int count = 0;
//    while (!hasShot) {
//      this.wait(0.1f);
//      if (count++ > waitForScreenshot) {
//        break;
//      }
//      if (prompt == null) {
//        continue;
//      }
//      if (prompt.isComplete()) {
//        simg = prompt.getSelection();
//        if (simg != null) {
//          lastPicture = simg;
//          hasShot = true;
//        }
//        prompt.close();
//      }
//    }
//    prompt.close();
//    prompt = null;
    return new Picture();
  }

  public int getIdFromPoint(int srcx, int srcy) {
    return 0;
  }

  public void aKey(int keyHome) {

  }

  public void aSwipeLeft() {

  }

  public void aSwipeRight() {

  }

  public void aTap(Picture sIMg) throws FindFailed {

  }

//  public Region newRegion(Location loc, int width, int height) {
//    return new Region(loc.x, loc.y, width, height, this);
//  }
//
//  public Region newRegion(int _x, int _y, int width, int height) {
//    return new Region(_x, _y, width, height, this);
//  }
//
//TODO  public Location newLocation(int _x, int _y) {
//    return new Location(_x, _y).setOtherScreen(this);
//  }
}
