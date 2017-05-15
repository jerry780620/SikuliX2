/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.util;

import com.sikulix.core.Content;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.devices.IDevice;
import com.sikulix.devices.vnc.VNCDevice;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Sikulix {

  //<editor-fold desc="housekeeping">
  static SXLog log;

  static String stars = repeat("*", 50);

  public static String repeat(String str, int count) {
    return String.format("%0" + count + "d", 0).replace("0", str);
  }

  private static void traceBlock(String message) {
    log.trace(stars);
    log.trace("*****   %s", message);
    log.trace(stars);
  }

  static List<String> options = new ArrayList<>();
  //</editor-fold>

  public static void main(String[] args) {
    log = SX.getSXLog("SX_Sikulix");
    options.addAll(Arrays.asList(args));
    if (options.isEmpty()) {
      log.p("SikuliX2::util.Sikulix::main: no args - nothing to do :-)");
      return;
    }
    if (options.contains("trace")) {
      options.remove("trace");
      log.globalOn(SXLog.TRACE);
    }
    String sargs = "";
    for (String option : options) {
      sargs += " " + option;
    }
    log.trace("main: start: %s", sargs);

    if (options.get(0).contains("methods")) {
      Map<String, String> methods = SX.listPublicMethods(Content.class);
      for (String method : methods.keySet().stream().sorted().collect(Collectors.toList())) {
        log.p("%-40s %s", method, methods.get(method));
      }
      return;
    }

    if (options.contains("tool")) {
      new Tool();
    }

    if (options.contains("play")) {
//********** play start
      IDevice vnc = new VNCDevice().start();
      if (SX.isSet(vnc)) {
        SX.pause(3);
        vnc.stop();
      }
//********** play end
    }
  }
}
