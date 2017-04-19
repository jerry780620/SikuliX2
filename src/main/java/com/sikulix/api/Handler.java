/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class Handler {
  private static SXLog log = SX.getSXLog("SX.Handler");

  public Handler() {

  }

  public Handler(Object handler, Event.TYPE type) {
    this.handler = (Handler) handler;
    setType(type);
  }

  public void setType(Event.TYPE type) {
    this.type = type;
  }

  private Handler handler = null;
  private Event.TYPE type = Event.TYPE.GENERIC;

  public void run(Event e) {
    log.error("Event handler not implemented");
    showEvent(e);
  }

  public void showEvent(Event e) {
    String msg = "---------- Event:\n" +
            "key = %d\n" +
            "when = %d\n" +
            "what = %s\n" +
            "where = %s\n" +
            "match = %s\n" +
            "vanish = %s\n" +
            "count = %s\n" +
            "---------";
    log.p(msg, e.getKey(), e.getWhen(), e.getWhat(), e.getWhere(),
            e.getWhere().getLastMatch(), e.getWhere().getLastVanish(), e.getCount());
  }
}
