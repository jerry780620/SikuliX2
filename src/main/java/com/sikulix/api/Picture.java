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
import java.net.MalformedURLException;
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

  //<editor-fold desc="*** get content">
  private long timeToLoad = -1;

  public long getTimeToLoad() {
    return timeToLoad;
  }

  private void setContent(String fpImg) {
    URL url = Picture.searchOnImagePath(fpImg);
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
          log.debug("get: loaded: (%dx%s) %s", getContent().width(), getContent().height(), urlImg);
        } else {
          log.error("get: not loaded: %s", urlImg);
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

  //<editor-fold desc="*** path">
  private static final List<URL> imagePath = Collections.synchronizedList(new ArrayList<URL>());

  private static boolean bundlePathIsFile = true;

  private static void initPath() {
    if (imagePath.isEmpty()) {
      imagePath.add(Content.asFileURL(SX.getSXIMAGES()));
      bundlePathIsFile = true;
    }
  }

  public static void clearPath() {
    imagePath.clear();
  }

  public static boolean setBundlePath(Object... args) {
    initPath();
    if (args.length == 0) {
      imagePath.set(0, Content.asFileURL(SX.getSXIMAGES()));
      bundlePathIsFile = true;
      return true;
    }
    URL urlPath = Content.makeURL(args);
    if (SX.isSet(urlPath)) {
      if ("file".equals(urlPath.getProtocol()) && urlPath.getPath().contains("test-classes")) {
        try {
          urlPath = new URL("file", null, 0, urlPath.getPath().replace("test-", ""));
        } catch (MalformedURLException e) {
          log.error("setBundlePath: hack(test-classes -> classes) did not work");
        }
      }
      bundlePathIsFile = false;
      if ("file".equals(urlPath.getProtocol())) {
        if (!urlPath.getPath().contains(".jar!/")) {
          bundlePathIsFile = true;
        }
      }
      imagePath.set(0, urlPath);
      return true;
    }
    return false;
  }

  public static boolean resetPath(Object... args) {
    imagePath.clear();
    if (args.length == 0) {
      initPath();
      return true;
    }
    return setBundlePath(args);
  }

  public static String getBundlePath() {
    initPath();
    return Content.asPath(imagePath.get(0));
  }

  public static boolean isBundlePathFile() {
    getBundlePath();
    return bundlePathIsFile;
  }

  public static String[] getPath(String filter) {
    String[] sPaths = new String[imagePath.size()];
    int n = 0;
    String sPath;
    for (URL uPath : imagePath) {
      sPath = uPath.toString();
      if (SX.isSet(filter) && !sPath.contains(filter)) {
        continue;
      }
      sPaths[n++] = sPath;
    }
    return sPaths;
  }

  public static String[] getPath() {
    return getPath("");
  }

  public static int hasPath(String filter) {
    if (SX.isNotSet(filter)) return -1;
    int n = 0;
    String sPath;
    for (URL uPath : imagePath) {
      sPath = Content.asPath(uPath);
      if (sPath.contains(filter)) {
        return n;
      }
      n++;
    }
    return -1;
  }

  public static String getPath(int n) {
    if (n < 0 || n > imagePath.size() - 1) {
      n = 0;
    }
    return Content.asPath(imagePath.get(n));
  }

  public static boolean setPath(int n, String fpPath) {
    if (n < 0 || n > imagePath.size() - 1) {
      return false;
    }
    URL urlPath = Content.makeURL(fpPath);
    if (SX.isSet(urlPath)) {
      imagePath.set(n, urlPath);
      return true;
    }
    return false;
  }

  public static String removePath(int n) {
    if (n < 0 || n > imagePath.size() - 1) {
      n = 0;
    }
    return Content.asPath(imagePath.get(n));
  }

  public static int addPath(Object... args) {
    initPath();
    if (args.length == 0) {
      return -1;
    }
    URL urlPath = Content.makeURL(args);
    if (SX.isSet(urlPath)) {
      imagePath.add(urlPath);
      return imagePath.size() - 1;
    }
    return -1;
  }

  public static int removePath(Object... args) {
    //TODO implment removePath()
    URL url = null;
    return -1;
  }

  public static int insertPath(Object... args) {
    //TODO implment insertPath()
    URL url = null;
    return -1;
  }

  public static int changePath(Object... args) {
    //TODO implment changePath()
    URL url = null;
    return -1;
  }

  /**
   * try to find the given relative image file name on the image path<br>
   * starting from entry 0, the first found existence is taken<br>
   * absolute file names are checked for existence
   *
   * @param fname relative or absolute filename
   * @return a valid URL or null if not found/exists
   */
  public static URL searchOnImagePath(String fname) {
    fname = getValidName(fname);
    URL fURL = null;
    String proto = "";
    fname = Content.normalize(fname);
    if (new File(fname).isAbsolute()) {
      if (new File(fname).exists()) {
        fURL = Content.makeURL(fname);
      }
    } else {
      initPath();
      for (URL path : imagePath) {
        if (path == null) {
          continue;
        }
        proto = path.getProtocol();
        fURL = Content.makeURL(path, fname);
        if ("file".equals(proto)) {
          if (new File(fURL.getPath()).exists()) {
            break;
          }
        } else if ("jar".equals(proto) || proto.startsWith("http")) {
          if (fURL != null) {
            break;
          }
        } else {
          log.error("searchOnImagePath: URL not supported: " + path);
          return fURL;
        }
        fURL = null;
      }
    }
    if (fURL == null) {
      log.error("searchOnImagePath: does not exist: " + fname);
    }
    return fURL;
  }

  /**
   * image file types supported <br>
   * Windows bitmaps - *.bmp <br>
   * JPEG files - *.jpeg, *.jpg, *.jpe<br>
   * Portable Network Graphics - *.png <br>
   * TIFF files - *.tiff, *.tif (see the *Notes* section)
   *
   * @param name an image file name
   * @return the name optionally .png added if no ending
   */
  public static String getValidName(String name) {
//    String validEndings = ".bmp.dib.jpeg.jpg.jpe.jp2.png.pbm.pgm.ppm.sr.ras.tiff.tif";
//    String validName = name;
//    String[] parts = validName.split("\\.");
//    if (parts.length == 1) {
//      log.trace("getValidName: supposing PNG: %s", name);
//      validName += ".png";
//    } else {
//      String ending = "." + parts[parts.length - 1];
//      if (validEndings.indexOf(ending) == -1) {
//        log.error("getValidName: image file ending %s not supported: %s", ending, name);
//      }
//    }
    return Content.asValidImageFilename(name);
  }

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
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="*** helpers">

  /**
   * get a new resized Picture
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
