/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package org.sikuli.script;

import com.sikulix.api.Do;
import com.sikulix.api.Element;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Region extends Element {
  private static SXLog log = SX.getSXLog("API.REGION");

  //<editor-fold desc="housekeeping">
  private static eType eClazz = eType.REGION;
  public eType getType() {
    return eClazz;
  }

  public Element asElement() {
    return new Element(this);
  }

  public boolean isValid() {
    boolean valid = true;
    valid &= SX.isNotNull(getScreen());
    valid &= w > 1 && h > 1;
    valid &= getScreen().asElement().contains(this.asElement());
    return valid;
  }

  public String toStringPlus() {
    return " #" + getScreen().getID();
  }
  //</editor-fold>

  //<editor-fold desc="Constructors">
  public Region() {}

  public Region(int X, int Y, int W, int H, Screen parentScreen) {
    initRegion(X, Y, W, H);
    initScreen(parentScreen);
  }

  public Region(int X, int Y, int W, int H) {
    initRegion(X, Y, W, H);
    initScreen(null);
  }

  public Region(Rectangle rectangle) {
    initRegion(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    initScreen(null);
  }

  public Region(Region region) {
    initRegion(region.x, region.y, region.w, region.h);
    initScreen(null);
  }

  public Region(Element element) {
    initRegion(element.x, element.y, element.w, element.h);
    initMore(element);
    initScreen(null);
  }

  protected void initRegion(int x, int y, int w, int h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }

  protected void initRegion(Rectangle rectangle) {
    this.x = rectangle.x;
    this.y = rectangle.y;
    this.w = rectangle.width;
    this.h = rectangle.height;
  }

  private void initMore(Element element) {
  }
  //</editor-fold>

  //<editor-fold desc="create">
  private static Region create(int X, int Y, int W, int H, Screen scr) {
    return new Region(X, Y, W, H, scr);
  }

  public static Region create(int X, int Y, int W, int H) {
    return Region.create(X, Y, W, H, null);
  }

  public static Region create(Location loc, int w, int h) {
    int _x = loc.x;
    int _y = loc.y;
    Screen s = (Screen) loc.getScreen();
    if (s == null) {
      _x = _y = 0;
      s = Screen.getPrimaryScreen();
    }
    return Region.create(_x, _y, w, h, s);
  }

  public final static int CREATE_X_DIRECTION_LEFT = 0;
  public final static int CREATE_X_DIRECTION_RIGHT = 1;
  public final static int CREATE_Y_DIRECTION_TOP = 0;
  public final static int CREATE_Y_DIRECTION_BOTTOM = 1;

  public static Region create(Location loc, int create_x_direction, int create_y_direction, int w, int h) {
    int _x = loc.x;
    int _y = loc.y;
    Screen s = (Screen) loc.getScreen();
    if (s == null) {
      _x = _y = 0;
      s = Screen.getPrimaryScreen();
    }
    int X;
    int Y;
    int W = w;
    int H = h;
    if (create_x_direction == CREATE_X_DIRECTION_LEFT) {
      if (create_y_direction == CREATE_Y_DIRECTION_TOP) {
        X = _x;
        Y = _y;
      } else {
        X = _x;
        Y = _y - h;
      }
    } else {
      if (create_y_direction == CREATE_Y_DIRECTION_TOP) {
        X = _x - w;
        Y = _y;
      } else {
        X = _x - w;
        Y = _y - h;
      }
    }
    return Region.create(X, Y, W, H, s);
  }

  public static Region create(Rectangle r) {
    return Region.create(r.x, r.y, r.width, r.height);
  }

  protected static Region create(Rectangle r, Screen parentScreen) {
    return Region.create(r.x, r.y, r.width, r.height, parentScreen);
  }

  public static Region create(Region region) {
    return Region.create(region.x, region.y, region.w, region.h);
  }
  //</editor-fold>

  //<editor-fold desc="access attributes">
  public Location getCenter() {
    Element element = super.getCenter();
    return new Location(element);
  }

  public Rectangle getRect() {
    return super.getRectangle();
  }

  public Rectangle getBounds() {
    return getRect();
  }
  //</editor-fold>
  
  //<editor-fold desc="obsolete?">
//  /**
//   * Setting, how to react if an image is not found {@link FindFailed}
//   */
//  private FindFailedResponse findFailedResponse = FindFailed.defaultFindFailedResponse;
//  private Object findFailedHandler = FindFailed.getFindFailedHandler();
//  private Object imageMissingHandler = FindFailed.getImageMissingHandler();

//
//  /**
//   * The last found {@link Match} in the Region
//   */
//  private Match lastMatch = null;
//  /**
//   * The last found {@link Match}es in the Region
//   */
//  private Iterator<Match> lastMatches = null;
//  private long lastSearchTime = -1;
//  private long lastFindTime = -1;
//  private boolean isScreenUnion = false;
//  private boolean isVirtual = false;
//  private long lastSearchTimeRepeat = -1;
//
//  public double getAutoWaitTimeout() {
//    return autoWaitTimeout;
//  }
//
//  public void setAutoWaitTimeout(double autoWaitTimeout) {
//    this.autoWaitTimeout = autoWaitTimeout;
//  }
//
  //</editor-fold>

  //<editor-fold desc="offset">
  public Location asOffset() {
    return new Location(w, h);
  }

  public Region offset(Location loc) {
    return (Region) Region.create(x + loc.x, y + loc.y, w, h, getScreen());
  }

  public Region offset(int x, int y) {
    return (Region) Region.create(this.x + x, this.y + y, w, h, getScreen());
  }
  //</editor-fold>

  //<editor-fold desc="grow">
  @Deprecated
  public Region nearby() {
    return grow();
  }

  @Deprecated
  public Region nearby(int range) {
    return (Region) grow(range);
  }

  public Region grow() {
    return (Region) super.grow();
  }

  public Region grow(int range) {
    return (Region) super.grow(range);
  }

  public Region grow(int w, int h) {
    return (Region) super.grow(w, h);
  }

  public Region grow(int l, int r, int t, int b) {
    return (Region) Region.create(x - l, y - t, w + l + r, h + t + b, getScreen());
  }

  public static Region grow(Location loc, int x, int y, int w, int h) {
    return Region.create(loc, x, y, w, h);
  }

  public static Region grow(Location loc, int w, int h) {
    int _x = loc.x;
    int _y = loc.y;
    Screen s = (Screen) loc.getScreen();
    if (s == null) {
      _x = _y = 0;
      s = Screen.getPrimaryScreen();
    }
    int X = _x - (int) w / 2;
    int Y = _y - (int) h / 2;
    return Region.create(X, Y, w, h, s);
  }

  public static Region grow(Location loc) {
    int _x = loc.x;
    int _y = loc.y;
    Screen s = (Screen) loc.getScreen();
    if (s == null) {
      _x = _y = 0;
      s = Screen.getPrimaryScreen();
    }
    return Region.create(_x, _y, 1, 1, s);
  }
  //</editor-fold>

  //<editor-fold desc="right">
  public Location rightAt() {
    return rightAt(0);
  }

  public Location rightAt(int offset) {
    return new Location(super.right(offset));
  }

  public Region right() {
    return new Region(super.right());
  }

  public Region right(int width) {
    return new Region(super.right(width));
  }
  //</editor-fold>

  //<editor-fold desc="left">
  public Location leftAt() {
    return leftAt(0);
  }

  public Location leftAt(int offset) {
    return new Location(super.left(offset));
  }

  public Region left() {
    return new Region(super.left());
  }

  public Region left(int width) {
    return new Region(super.left(width));
  }
  //</editor-fold>

  //<editor-fold desc="above">
  public Location aboveAt() {
    return aboveAt(0);
  }

  public Location aboveAt(int offset) {
    return new Location(super.above(offset));
  }

  public Region above() {
    return new Region(super.above());
  }

  public Region above(int height) {
    return new Region(super.above(height));
  }
  //</editor-fold>

  //<editor-fold desc="below">
  public Location belowAt() {
    return belowAt(0);
  }

  public Location belowAt(int offset) {
    return new Location(super.below(offset));
  }

  public Region below() {
    return new Region(super.below());
  }

  public Region below(int height) {
    return new Region(super.below(height));
  }
  //</editor-fold>
  
  //<editor-fold defaultstate="collapsed" desc="new regions">
  public Region union(Region region) {
    return new Region(super.union(region));
  }

  public Region intersection(Region region) {
    return new Region(super.intersection(region));
  }
  //</editor-fold>

  //<editor-fold desc="find">
  public <PSI> Match find(PSI target) throws FindFailed {
    if (getAutoWaitTimeout() > 0) {
      return wait(target, getAutoWaitTimeout());
    }
    Do.find(toFindTarget(target), this);
    if (hasMatch()) {
      setLastMatch(getLastMatch());
      return (Match) getLastMatch();
    }
    throw new FindFailed(String.format("%s in %s", target, this));
  }

  public void wait(double timeout) {
    try {
      Thread.sleep((long) (timeout * 1000L));
    } catch (InterruptedException e) {
    }
  }

  public <PSI> Match wait(PSI target) throws FindFailed {
    if (target instanceof Float || target instanceof Double) {
      wait(0.0 + ((Double) target));
      return null;
    }
    return wait(target, getAutoWaitTimeout());
  }

  public <PSI> Match wait(PSI target, double timeout) throws FindFailed {
    Do.wait(toFindTarget(target), this, timeout);
    if (hasMatch()) {
      setLastMatch(getLastMatch());
      return (Match) getLastMatch();
    }
    throw new FindFailed(String.format("%s in %s", target, this));
  }

  public <PSI> Match exists(PSI target) {
    return exists(target, getAutoWaitTimeout());
  }

  public <PSI> Match exists(PSI target, double timeout) {
    Do.wait(toFindTarget(target), this, timeout);
    if (hasMatch()) {
      setLastMatch(getLastMatch());
      return (Match) getLastMatch();
    }
    return null;
  }

  public <PSI> boolean waitVanish(PSI target) {
    return waitVanish(target, getAutoWaitTimeout());
  }

  public <PSI> boolean waitVanish(PSI target, double timeout) {
    Do.wait(toFindTarget(target), this, timeout);
    return hasVanish();
  }

  private static <PSI> Object toFindTarget(PSI target) {
    if (target instanceof String) {
      return target;
    }
    if (target instanceof Image) {
      ((Image) target).getName();
    }
    if (target instanceof Pattern) {
      return ((Pattern) target).getName();
    }
    return new Element();
  }

  public <PSI> Iterator<Match> findAll(PSI target) throws FindFailed {
    List<Match> matches = new ArrayList<>();
    List<Element> matchElements = Do.findAll(toFindTarget(target), this);
    if (matchElements.size() == 0) {
      throw new FindFailed(String.format("%s in %s", target, this));
    }
    for (Element elem : matchElements) {
      matches.add(new Match(elem));
    }
    return new IteratorMatch(matches);
  }

  private class IteratorMatch implements Iterator<Match> {

    List<Match> matches = null;

    public IteratorMatch(List<Match> matches) {
      this.matches = matches;
    }

    @Override
    public boolean hasNext() {
      return matches.size() > 0;
    }

    @Override
    public Match next() {
      if (hasNext()) {
        return (Match) matches.remove(0);
      }
      return null;
    }

    @Override
    public void remove() {
    }
  }

//  public boolean hasMatch() {
//    return SX.isNotNull(super.getLastMatch());
//  }
//
//  public Match getLastMatch() {
//    return new Match(super.getLastMatch());
//  }
  //</editor-fold>
}
