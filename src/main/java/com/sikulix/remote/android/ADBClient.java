/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.remote.android;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import se.vidstige.jadb.AdbServerLauncher;
import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbDevice;

import java.util.List;

/**
 * Created by tg44 on 2016. 06. 26..
 * Modified by RaiMan
 */
public class ADBClient {

  static SXLog log = SX.getSXLog("SX.ADBClient");

  private static JadbConnection jadb = null;
  private static boolean shouldStopServer = false;
  private static JadbDevice device = null;
  public static boolean isAdbAvailable = true;

  private static void init() {
    getConnection(true);
    if (jadb == null) {
      try {
        new AdbServerLauncher(null, null).launch();
        log.trace("ADBClient: ADBServer started");
        getConnection(false);
        if (jadb != null) {
          shouldStopServer = true;
        }
      } catch (Exception e) {
        //Cannot run program "adb": error=2, No such file or directory
        if (e.getMessage().startsWith("Cannot run program")) {
          isAdbAvailable = false;
          log.error("ADBClient: package adb not available. need to be installed");
        } else {
          log.error("ADBClient: ADBServer problem: %s", e.getMessage());
        }
      }
    }
    String serial = null;
    if (jadb != null) {
      List<JadbDevice> devices = null;
      try {
        devices = jadb.getDevices();
      } catch (Exception e) {
      }
      if (devices != null && devices.size() > 0) {
        device = devices.get(0);
        serial = device.getSerial();
      } else {
        device = null;
        log.error("ADBClient: init: no devices attached");
      }
    }
    if (device != null) {
      log.trace("ADBClient: init: attached device: serial(%s)", serial);
    }
  }

  public static void reset() {
    device = null;
    jadb = null;
    Process p = null;
    if (!shouldStopServer) {
      return;
    }
    try {
      p = Runtime.getRuntime().exec(new String[] {"adb", "kill-server"});
      p.waitFor();
    } catch (Exception e) {
      log.error("ADBClient: reset: kill-server did not work");
    }
  }

  private static void getConnection(boolean quiet) {
    if (jadb == null) {
      try {
        jadb = new JadbConnection();
        jadb.getHostVersion();
        log.trace("ADBClient: ADBServer connection established");
      } catch (Exception e) {
        if (!quiet) {
          log.error("ADBClient: ADBServer connection not possible: %s", e.getMessage());
        }
        jadb = null;
      }
    }
  }

  public static JadbDevice getDevice() {
    init();
    return device;
  }

  //TODO: get device by id

  public boolean isValid() {
    return jadb != null;
  }

  public boolean hasDevices() {
    return device != null;
  }
}
