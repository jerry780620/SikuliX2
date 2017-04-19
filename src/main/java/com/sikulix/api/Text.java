/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

/**
 * implements the API for text search and OCR features<br>
 * holds all settings for a text feature processing<br>
 * and the results of this processing<br>
 * uses Core.TextFinder which implements the text features using Tesseract (via Tess4J)
 *
 */
public class Text extends Element {

  private static final SXLog log = SX.getSXLog("SX.Text");

  private String searchText = null;
  private String ocrText = null;

  /**
   * create a Text object with specific settings to be used later with read
   *
   * @param settings
   */
  public Text(Object... settings) {
    init(settings);
  }

  /**
   * create a Text object with specific settings to be used later with find/findAll
   *
   * @param text
   * @param settings
   */
  public Text(String text, Object... settings) {
    init(settings);
    searchText = text;
  }

  private void init(Object... settings) {
    //TODO what settings and how to process/store/access
  }

  /**
   * OCR in the given Element according to the settings of this Text object
   *
   * @param where
   * @return
   */
  public Text read(Element where) {
    //TODO read the text according to settings and fill ocrText
    return this;
  }

  /**
   * convenience: OCR in the given Element according to the standard settings
   *
   * @param where
   * @param settings should be omitted (only to have a valid signature)
   * @return
   */
  public static Text read(Element where, Object... settings) {
    return new Text().read(where);
  }

  /**
   * find the searchText in the given Element
   * according to the settings of this Text object
   *
   * @param where
   * @return
   */
  public Text find(Element where) {
    //TODO search the text and fill lastMatch
    return this;
  }

  /**
   * convenience: find the given text in the given Element
   * according to the standard settings
   *
   * @param text
   * @param where
   * @return
   */
  public static Text find(String text, Element where) {
    return new Text(text).find(where);
  }


  /**
   * find all occurences of the searchText in the given Element
   * according to the settings of this Text object
   *
   * @param where
   * @return
   */
  public Text findAll(Element where) {
    //TODO search the text and fill lastMatches
    return this;
  }

  /**
   * convenience: find all occurences of the given text in the given Element
   * according to the standard settings
   *
   * @param text
   * @param where
   * @return
   */
  public static Text findAll(String text, Element where) {
    return new Text(text).findAll(where);
  }
}
