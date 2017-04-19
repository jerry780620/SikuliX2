/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import java.awt.Color;

public class Symbol extends Element {

  private static eType eClazz = eType.SYMBOL;
  public eType getType() {
    return eClazz;
  }
  public String getTypeFirstLetter() {
    return "Y";
  }



  private static SXLog log = SX.getSXLog("SX." + eClazz.toString());

  //<editor-fold desc="*** construction">
  private void copyPlus(Element elem) {
    copy(elem);
    if (elem.hasContent()) {
      setContent(elem.getContent().clone());
    }
    setComponent(elem.getComponent());
  }

  public boolean isActive() {
    return Component.BUTTON.equals(getComponent());
  }

  public Symbol() {}

  public Symbol(int w, int h) {
    this();
    init(0, 0, w, h);
  }

  public Symbol(Element element) {
    copy(element);
  }

  public static Symbol button(int w, int h) {
    return new Symbol(w, h).setComponent(Component.BUTTON).setName();
  }

  public static Symbol rectangle(int w, int h) {
    return new Symbol(w, h).setName();
  }

  public static Symbol square(int w) {
    return new Symbol(w, w).setName();
  }

  public static Symbol circle(int diameter) {
    return new Symbol(diameter, diameter).setComponent(Component.CIRCLE).setName();
  }

  public static Symbol ellipse(int w, int h) {
    return new Symbol(w, h).setComponent(Component.CIRCLE).setName();
  }

  public Symbol setName() {
     setName(getComponent().toString());
     return this;
  }

  @Override
  public boolean isRectangle() {
    return Component.RECTANGLE.equals(getComponent()) || Component.BUTTON.equals(getComponent());
  }

  public boolean isButton() {
    return Component.BUTTON.equals(getComponent());
  }

  public boolean isCircle() {
    return Component.CIRCLE.equals(getComponent());
  }

  private Color fillColor = null;

  public Color getFillColor() {
    return fillColor;
  }

  public Symbol fill() {
    fillColor = getLineColor();
    return this;
  }

  public Symbol fill(Color color) {
    fillColor = color;
    return this;
  }

  private int line = getHighLightLine();

  public int getLine() {
    return line;
  }

  public Symbol setLine(int line) {
    this.line = line;
    return this;
  }

  @Override
  public Color getColor() {
    return color;
  }

  public Symbol setColor(Color color) {
    this.color = color;
    return  this;
  }

  private Color color = Color.lightGray;

  public Symbol at(int x, int y) {
    super.at(x, y);
    return this;
  }
}
