/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.Picture;
import net.sourceforge.tess4j.Tesseract1;
import net.sourceforge.tess4j.TesseractException;

import java.io.File;

/**
 * Implements all text search and OCR features using Tesseract via Tess4J
 */
public class TextFinder {

  private static final SXLog log = SX.getLogger("SX.TextFinder");

  private Tesseract1 tess = null;
  private String datapath = SX.getSXSYSAPPDATA() + "/Sikulix/SikulixTesseract";
  private boolean valid = false;

  public TextFinder() {
    init();
  }

  public TextFinder(String datapath) {
    this.datapath = datapath;
    init();
  }

  private void init() {
    tess = new Tesseract1();
    valid = setTessdata();
    if (valid) {
      log.trace("init: tessdata = %s", datapath);
    }
  }

  private boolean setTessdata() {
    File tessdata = new File(datapath, "tessdata");
    if (tessdata.exists()) {
      tess.setDatapath(datapath);
      return true;
    } else  {
      log.error("init: tessdata not found: %s", tessdata);
    }
    return false;
  }

  public boolean isValid() {
    return valid;
  }

  public String read(Picture picture) {
    if (valid) {
      try {
        return tess.doOCR(picture.get());
      } catch (TesseractException e) {
        log.error("read: %s", e.getMessage());
      }
    } else {
      log.error("read: TextFinder not valid");
    }
    return "did not work";
  }

}
