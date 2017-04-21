/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.Content;
import com.sikulix.core.Finder;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;

public class Picture extends Element {

  eType eClazz = eType.PICTURE;

  public eType getType() {
    return eClazz;
  }

  public String getTypeFirstLetter() {
    return "I";
  }

  private static SXLog log = SX.getSXLog("SX.PICTURE");

  //<editor-fold desc="*** construction">
  public Picture() {
  }

  private void copyPlus(Element elem) {
    copy(elem);
    if (elem.hasContent()) {
      setContent(elem.getContent().clone());
    } else {
      setContent();
    }
    urlImg = elem.urlImg;
    setAttributes();
  }

  public static Picture create(Object... args) {
    Picture picture = new Picture();
    int aLen = args.length;
    if (aLen > 0) {
      Object args0 = args[0];
      if (args0 instanceof BufferedImage) {
        picture = new Picture((BufferedImage) args0);
      } else if (args0 instanceof Mat) {
        picture = new Picture((Mat) args0);
      } else if (args0 instanceof String) {
        picture = new Picture((String) args0);
      } else if (args0 instanceof URL) {
        picture = new Picture((URL) args0);
      } else if (args0 instanceof Element) {
        if (aLen == 1) {
          picture = new Picture((Element) args0);
        } else {
          Object args1 = args[1];
          Object args2 = (aLen == 3 ? args[2] : null);
          if (aLen == 3) {
            if (args2 instanceof Element && args1 instanceof Double) {
              picture = new Picture((Element) args0, (Double) args1, (Element) args2);
            }
          } else {
            if (args1 instanceof Element) {
              picture = new Picture((Element) args0, (Element) args1);
            }
          }
        }
      }
    }
    return picture;
  }

  public Picture(BufferedImage bimg) {
    long start = new Date().getTime();
    setContent(makeMat(bimg));
    timeToLoad = new Date().getTime() - start;
    init(0, 0, getContent().width(), getContent().height());
    setAttributes();
  }

  public Picture(Mat mat) {
    if (SX.isNull(mat)) {
      setContent();
    } else {
      long start = new Date().getTime();
      setContent(mat.clone());
      timeToLoad = new Date().getTime() - start;
    }
    init(0, 0, getContent().width(), getContent().height());
    setAttributes();
  }

  public Picture(String fpImg) {
    setContent(fpImg);
    init(0, 0, getContent().width(), getContent().height());
  }

  public Picture(URL url) {
    setContent(url);
    init(0, 0, getContent().width(), getContent().height());
  }

  public Picture(Element elem) {
    copyPlus(elem);
  }

  public Picture(Element elem, double score) {
    copyPlus(elem);
    setScore(score);
  }

  public Picture(Element elem, double score, Element offset) {
    copyPlus(elem);
    setScore(score);
    setTarget(offset);
  }

  public Picture(Element elem, Element offset) {
    copyPlus(elem);
    setTarget(offset);
  }

  /**
   * @return true if the Element is useable and/or has valid content
   */
  @Override
  public boolean isValid() {
    if (SX.isSet(getContent())) {
      return !getContent().empty();
    }
    return false;
  }

  public String getURL() {
    return urlImg.toString();
  }
  //</editor-fold>

  //<editor-fold desc="*** getAll content">
  private long timeToLoad = -1;

  public long getTimeToLoad() {
    return timeToLoad;
  }

  private void setContent(String fpImg) {
    URL url = Content.searchOnImagePath(fpImg);
    if (SX.isSet(url)) {
      setContent(url);
    } else {
      setContent();
      setName(getNameFromFileL(new File(fpImg)));
    }
  }

  private void setContent(URL url) {
    setContent();
    if (SX.isSet(url)) {
      urlImg = url;
      setName(getNameFromURL(urlImg));
      if (urlImg != null) {
        long start = new Date().getTime();
        String urlProto = urlImg.getProtocol();
        if (urlProto.equals("file")) {
          File imgFile = new File(urlImg.getPath());
          setContent(Imgcodecs.imread(imgFile.getAbsolutePath(), Imgcodecs.IMREAD_UNCHANGED));
        } else {
          try {
            setContent(makeMat(ImageIO.read(urlImg)));
          } catch (IOException e) {
            log.error("load(): %s for %s", e.getMessage(), urlImg);
          }
        }
        timeToLoad = new Date().getTime() - start;
        if (isValid()) {
          setAttributes();
          log.debug("getAll: loaded: (%dx%s) %s", getContent().width(), getContent().height(), urlImg);
        } else {
          log.error("getAll: not loaded: %s", urlImg);
        }
      }
    }
  }

  private String getNameFromURL(URL url) {
    String name = getName();
    if (SX.isNotNull(url)) {
      name = url.getPath().replace("file:", "");
      name = new File(name).getName();
      int iDot = name.indexOf(".");
      if (iDot > -1) {
        name = name.substring(0, iDot);
      }
    }
    return name;
  }

  private String getNameFromFileL(File image) {
    String name = getName();
    if (SX.isNotNull(image)) {
      name = image.getName();
      int iDot = name.indexOf(".");
      if (iDot > -1) {
        name = name.substring(0, iDot);
      }
    }
    return name;
  }

  public Picture reset() {
    if (isValid()) {
      setContent(urlImg);
    }
    return this;
  }

  private final int resizeMinDownSample = 12;
  private int[] meanColor = null;
  private double minThreshhold = 1.0E-5;

  public Color getMeanColor() {
    return new Color(meanColor[2], meanColor[1], meanColor[0]);
  }

  public boolean isMeanColorEqual(Color otherMeanColor) {
    Color col = getMeanColor();
    int r = (col.getRed() - otherMeanColor.getRed()) * (col.getRed() - otherMeanColor.getRed());
    int g = (col.getGreen() - otherMeanColor.getGreen()) * (col.getGreen() - otherMeanColor.getGreen());
    int b = (col.getBlue() - otherMeanColor.getBlue()) * (col.getBlue() - otherMeanColor.getBlue());
    return Math.sqrt(r + g + b) < minThreshhold;
  }

  private void setAttributes() {
    if (!hasContent()) {
      return;
    }
    plainColor = false;
    blackColor = false;
    resizeFactor = Math.min(((double) getContent().width()) / resizeMinDownSample,
            ((double) getContent().height()) / resizeMinDownSample);
    resizeFactor = Math.max(1.0, resizeFactor);
    MatOfDouble pMean = new MatOfDouble();
    MatOfDouble pStdDev = new MatOfDouble();
    if (hasMask()) {
      Core.meanStdDev(getContentBGR(), pMean, pStdDev, getMask());
    } else {
      Core.meanStdDev(getContentBGR(), pMean, pStdDev);
    }
    double sum = 0.0;
    double[] arr = pStdDev.toArray();
    for (int i = 0; i < arr.length; i++) {
      sum += arr[i];
    }
    if (sum < minThreshhold) {
      plainColor = true;
    }
    sum = 0.0;
    arr = pMean.toArray();
    meanColor = new int[arr.length];
    for (int i = 0; i < arr.length; i++) {
      meanColor[i] = (int) arr[i];
      sum += arr[i];
    }
    if (sum < minThreshhold && plainColor) {
      blackColor = true;
    }
    if (meanColor.length > 1) {
      whiteColor = isMeanColorEqual(Color.WHITE);
    }
  }
  //</editor-fold>

  //</editor-fold>

  public static boolean handleImageMissing(String type, Finder.PossibleMatch possibleMatch) {
    if (possibleMatch.isImageMissingWhat()) {
      log.trace("%s: handling image missing: what: %s", type, possibleMatch.getWhat());
      return handleImageMissing(possibleMatch.getWhat());
    } else {
      log.trace("%s: handling image missing: where: %s", type, possibleMatch.getWhere());
      return handleImageMissing(possibleMatch.getWhere());
    }
  }

  static boolean handleImageMissing(Element image) {
    //TODO image missing handler
    return false;
  }

  public static boolean handleFindFailed(String type, Finder.PossibleMatch possibleMatch) {
    //TODO find failed handler
    log.trace("%s: handling not found: %s", type, possibleMatch);
    return false;
  }

  //<editor-fold defaultstate="collapsed" desc="*** helpers">

  /**
   * getAll a new resized Picture
   *
   * @param factor resize factor
   * @return a new inMemory Picture
   */
  public Picture getResized(double factor) {
    return new Picture(getResizedMat(factor));
  }

  /**
   * resize the Picture by factor
   *
   * @param factor resize factor
   * @return the Picture
   */
  public Picture resize(double factor) {
    setContent(getResizedMat(factor));
    return this;
  }

  /**
   * create a sub image from this image
   *
   * @param x pixel column
   * @param y pixel row
   * @param w width
   * @param h height
   * @return the new image
   */
  public Picture getSub(int x, int y, int w, int h) {
    Picture img = new Picture();
    if (isValid()) {
      img = new Picture(getContent().submat(new Rect(x, y, w, h)));
    }
    return img;
  }

  public Picture getSub(Element elem) {
    return getSub(elem.x, elem.y, elem.w, elem.h);
  }
//</editor-fold>
}
