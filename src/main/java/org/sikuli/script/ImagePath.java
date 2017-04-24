/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package org.sikuli.script;

import com.sikulix.core.Content;
import com.sikulix.core.SX;

public class ImagePath {

  public static String[] get() {
    return Content.getImagePath().getAll();
  }

  private static boolean bundleEquals(Object path) {
    return Content.equalsPath(path.toString(), Content.getBundlePath());
  }

  public static boolean add(String mainPath) {
    return -1 < Content.getImagePath().add(mainPath);
  }

  public static boolean add(String mainPath, String altPath) {
    return -1 < Content.getImagePath().add(mainPath, altPath);
  }

  public static boolean remove(String path) {
    return SX.isSet(Content.getImagePath().remove(Content.getImagePath().get(path)));
  }

  public static boolean reset(String path) {
    return SX.isSet(Content.resetImagePath(path));
  }

  public static boolean reset() {
    return reset(Content.getBundlePath());
  }

  public static boolean setBundlePath(String path) {
    if (Content.setBundlePath(path)) {
      return true;
    }
    return false;
  }

  public static String getBundlePath() {
    return Content.getBundlePath();
  }
}
