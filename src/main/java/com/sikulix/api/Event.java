/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Event implements Comparable {
  private static SXLog log = SX.getSXLog("SX.Event");

  @Override
  public int compareTo(Object evt) {
    long diff = this.when - ((Event) evt).getWhen();
    return  diff == 0 ? 0 : (diff < 0 ? -1 : 1);
  }

  public enum TYPE {
    FINDFAILED, IMAGEMISSING, ONAPPEAR, ONVANISH, ONCHANGE, GENERIC
  }

  public String getTypeShort() {
    return type.toString().substring(2,3);
  }

  public boolean isAppear() {
    return TYPE.ONAPPEAR.equals(type);
  }

  public boolean isVanish() {
    return TYPE.ONVANISH.equals(type);
  }

  public boolean isChange() {
    return TYPE.ONCHANGE.equals(type);
  }

  public enum RESPONSE {
    ABORT, SKIP, RETRY, PROMPT, CAPTURE, HANDLE
  }

  private Event() {
  }

  public Event(TYPE type, Element what, Element where) {
    this.type = type;
    this.what = what;
    this.where = where;
  }

  public Event(TYPE type, Element what, Element where, Handler handler) {
    this(type, what, where);
    this.handler = handler;
  }

  public Event(Handler handler) {
    this(TYPE.GENERIC, null, null);
    this.handler = handler;
  }

  TYPE type = TYPE.GENERIC;

  public Element getWhat() {
    return what;
  }

  Element what = null;

  public Element getWhere() {
    return where;
  }

  Element where = null;

  public boolean hasHandler() {
    return SX.isNotNull(handler);
  }

  public Handler getHandler() {
    return handler;
  }

  public void setHandler(Handler handler) {
    this.handler = handler;
  }

  Handler handler = null;

  public boolean hasMatch() {
    return SX.isNotNull(match);
  }

  public void setMatch(Element match) {
    this.match = match;
  }

  public Element getMatch() {
    return match;
  }

  Element match = null;

  public boolean hasVanish() {
    return SX.isNotNull(vanish);
  }

  public Element getVanish() {
    return vanish;
  }

  public void setVanish(Element vanish) {
    this.vanish = vanish;
  }

  Element vanish = null;

  public List<Element> getChanges() {
    return changes;
  }

  public void setChanges(List<Element> changes) {
    this.changes = changes;
  }

  List<Element> changes = new ArrayList<>();

  public long getWhen() {
    return when;
  }

  public long setWhen() {
    when = new Date().getTime();
    return when;
  }

  public long setWhen(long newWhen) {
    when = newWhen;
    return when;
  }

  long when = 0;

  public long getKey() {
    return key;
  }

  public Long setKey(long key) {
    this.key = key;
    return key;
  }

  long key = 0;

  public boolean isFor(Element what) {
    return this.what.equals(what);
  }

  private int repeatPauseDefault = 1;
  private int repeatPause = -1;

  public void repeat() {
    repeatPause = repeatPauseDefault;
  }

  public void repeat(int pause) {
    repeatPause = pause;
  }

  public void pause() {
    repeatPause = -1;
  }

  public boolean shouldRepeat() {
    return repeatPause > -1;
  }

  public int getRepeat() {
    return repeatPause;
  }

  public int getCount() {
    return count;
  }

  public void incrementCount() {
    this.count++;
  }

  private int count = 0;

  public void stop() {

  }

  public void reset() {
    count = 0;
    repeatPause = -1;
    match = null;
    vanish = null;
    changes = new ArrayList<>();
  }

  public void handle() {
    log.trace("handling: %s", this);
    this.getHandler().run(this);
  }

  public String toString() {
    return String.format("%s what: %s where: %s", type, what, where);
  }
}
