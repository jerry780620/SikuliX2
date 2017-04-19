/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.util;

import com.sikulix.api.Do;
import com.sikulix.api.Element;
import com.sikulix.api.Picture;
import com.sikulix.core.NativeHook;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    log = SX.getLogger("SX.Sikulix");
    options.addAll(Arrays.asList(args));
    if (options.isEmpty()) {
      log.p("SikuliX2::util.Sikulix::main: no args - nothing to do :-)");
    }
    if (options.contains("trace")) {
      System.setProperty("sikulix.logging", "trace");
    }
    log.trace("main: start: %s", "parameter");

    //<editor-fold desc="tests">
    if (options.contains("test")) {

      traceBlock("testing: native libraries");
      SX.setSXBASECLASS();

      traceBlock("testing: NativeHook");
      if (!SX.isHeadless()) {
        NativeHook hook = NativeHook.start();
        SX.pause(1);
        hook.stop();
        SX.pause(1);
        log.trace("NativeHook works");
      } else {
        log.trace("headless: NativeHook not tested");
      }

      traceBlock("testing: bundlePath in jar");
      Do.setBundlePath("./Images");
      log.trace("bundlePath: %s", Do.getBundlePath());

      traceBlock("testing: load image from jar");
      Picture img = new Picture("sikulix2");
      boolean imgOK = img.hasContent();
      img.show(3);

      traceBlock("testing: find image in other image");
      Picture base = new Picture("shot-tile");
      boolean baseOK = base.hasContent();
      if (baseOK && imgOK) {
        Element element = new Element();
        element = Do.find(img, base);
        if (element.isMatch()) {
          base.showMatch();
        }
      }

      traceBlock("testing: find image on primary monitor");
      if (baseOK && imgOK) {
        Element element = new Element();
        base.show();
        element = Do.find(img);
        if (element.isMatch()) {
          Do.onMain().showMatch();
        }
      }
    }
    //</editor-fold>

    if (options.contains("tool")) {
      new Tool();
    }
  }
}
