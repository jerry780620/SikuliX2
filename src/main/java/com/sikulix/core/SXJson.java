/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.core;

import org.json.JSONArray;
import org.json.JSONObject;

public class SXJson {

  static SXLog log;

  static {
    log = SX.getSXLog("SXJson");
    log.isSX();
    log.on(SXLog.TRACE);
  }

  JSONObject theJsonObject = null;
  JSONArray theJsonArray = null;

  public SXJson() {
    theJsonObject = new JSONObject(JSONObject.NULL);
  }

  public SXJson(String strJson) {
    try {
      theJsonObject = new JSONObject(strJson);
    } catch (Exception e) {
      log.error("SXJson(String): not possible: %s", e.getMessage());
    }
  }

  public SXJson(JSONObject objJson) {
    theJsonObject = objJson;
  }

  public SXJson(Object bean) {
    try {
      theJsonObject = new JSONObject(bean);
    } catch (Exception e) {
      log.error("SXJson(bean): not possible: %s", e.getMessage());
    }
  }

  public static JSONObject makeBean(Object bean) {
    return new SXJson(bean).theJsonObject;
  }

  public static JSONObject makeObject(String strJson) {
    return new SXJson(strJson).theJsonObject;
  }

  public static SXJson asArray(Object toArray) {
    SXJson theJson = new SXJson();
    try {
      if (toArray instanceof String) {
        theJson.setArray(new JSONArray((String) toArray));
      } else {
        JSONArray anArray = new JSONArray(toArray);
        theJson.setArray(anArray);
      }
    } catch (Exception e) {
      log.error("JSONArray: not possible: %s", e.getMessage());
    }
    return theJson;
  }

  private SXJson setArray(JSONArray theArray) {
    theJsonArray = theArray;
    return this;
  }

  public JSONArray getArray() {
    return theJsonArray;
  }

  public boolean isArray() {
    return SX.isNotNull(theJsonArray);
  }

//TODO Base64
//  private String toBase64(Mat image) {
//    MatOfByte mobImage = new MatOfByte();
//    Highgui.imencode("png", image, mobImage);
//    byte[] bImage = mobImage.toArray();
//    String b64Image = Base64.getEncoder().encodeToString(bImage);
//    return b64Image;
//  }
//
//  private Mat fromBase64(String image) {
//    byte[] bImage = Base64.getDecoder().decode(image);
//    Mat mImage = new Mat();
//    mImage.put(0,0,bImage);
//    return Highgui.imdecode(mImage, CV_LOAD_IMAGE_COLOR);
//  }
}
