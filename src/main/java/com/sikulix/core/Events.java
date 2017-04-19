/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.Element;
import com.sikulix.api.Event;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Events {
  private static SXLog log = SX.getSXLog("SX.Events");

  //<editor-fold desc="housekeeping">
  private static Events instance = null;
  private ConcurrentHashMap<Element, ConcurrentHashMap<Long, Event>> elements = new ConcurrentHashMap<>();

  private boolean running = false;

  EventLoop eventLoop = null;

  private static boolean processEvents = true;

  public static void shouldProcessEvents(boolean state) {
    processEvents = state;
  }

  private Events() {
  }

  private static Events get() {
    if (SX.isNull(instance)) {
      instance = new Events();
    }
    return instance;
  }

  public static void reset() {
    processEvents = true;
    instance = null;
  }
  //</editor-fold>

  //<editor-fold desc="start/stop">
  public static void startObserving() {
    if (!get().running) {
      if (SX.isNull(get().eventLoop)) {
        get().eventLoop = new EventLoop();
        new Thread(get().eventLoop).start();
      }
    }
    get().running = true;
    log.trace("startObserving");
  }

  public static void pause() {
    if (isRunning()) {
      get().running = false;
    }
  }

  public static void stopObserving() {
    log.trace("stop requested");
    if (isRunning()) {
      get().running = false;
    }
    waitForEventLoopToFinish();
  }

  private static boolean isRunning() {
    return get().running;
  }
  //</editor-fold>

  //<editor-fold desc="elements (where)">
  private static ConcurrentHashMap<Long, Event> put(Element where) {
    get().elements.put(where, new ConcurrentHashMap<Long, Event>());
    return get().elements.get(where);
  }

  public static void add(Element where, Collection<Event> evts) {
    ConcurrentHashMap<Long, Event> events = put(where);
    for (Event evt : evts) {
      evt.reset();
      events.put(evt.getKey(), evt);
    }
  }

  public static void remove(Element where) {
    get().elements.put(where, new ConcurrentHashMap<Long, Event>());
  }

  private static ConcurrentHashMap<Long, Event> getEventList(Element where) {
    ConcurrentHashMap<Long, Event> events = get().elements.get(where);
    return events;
  }
  //</editor-fold>

  //<editor-fold desc="events (what)">
  public static boolean hasEvents(Element where) {
    return SX.isNotNull(get().elements.get(where));
  }

  public static int getEventCount(Element where) {
    if (SX.isNotNull(getEventList(where))) {
      return getEventList(where).size();
    }
    return 0;
  }

  private static List<Event> getEvents(Element where) {
    List<Event> events = new ArrayList<>();
    events.addAll(getEventList(where).values());
    Collections.sort(events);
    return events;
  }

  public static boolean hasEvent(Element what, Element where) {
    if (SX.isNotNull(getEventList(where))) {
      for (Event evt : getEventList(where).values()) {
        if (evt.isFor(what)) {
          return true;
        }
      }
    }
    return false;
  }

  public static Event getEvent(Element what, Element where) {
    Event evt = null;
    if (SX.isNotNull(getEventList(where))) {
      for (Event e : getEventList(where).values()) {
        if (e.isFor(what)) {
          evt = e;
          break;
        }
      }
    }
    return evt;
  }

  public static boolean hasHappened(Element where) {
    return countHappened(where) > 0;
  }

  private static int countHappened(Element where) {
    int count = 0;
    if (hasEvents(where)) {
      for (Event e : getEvents(where)) {
        if (e.getWhen() > where.getObserveCount()) {
          count++;
        }
      }
    }
    return count;
  }

  public static Event nextHappened(Element where) {
    Event evt = null;
    if (SX.isNotNull(getEventList(where))) {
      if (getEventList(where).size() > 0) {
        evt = getEvents(where).get(0);
        getEventList(where).remove(evt.getKey());
      }
    }
    return evt;
  }
  //</editor-fold>

  //<editor-fold desc="EventLoop">
  private static void waitForEventLoopToFinish() {
    if (SX.isNull(get().eventLoop)) {
      return;
    }
    log.trace("waitForEventLoopToFinish: start");
    while (get().eventLoop.isLooping()) {
      SX.pause(0.5);
    }
    log.trace("waitForEventLoopToFinish: end");
  }

  public static void waitUntilFinished() {
    waitForEventLoopToFinish();
  }

  private static class EventLoop implements Runnable {

    boolean running = true;

    public boolean isLooping() {
      return running;
    }

    @Override
    public void run()
    {
      log.trace("EventLoop: started for %d elements", get().elements.size());
      int numWhere = 1;
      Element[] wheres = get().elements.keySet().toArray(new Element[0]);
      while (true) {
        for (Element where : wheres) {
          if (!where.isObserving()) {
            int numEvents = getEventList(where).size();
            if (numEvents > 0) {
              log.trace("EventLoop: starting observe for (%d) %s with %d events",
                      numWhere, where, numEvents);
              for (Event evt : getEvents(where)) {
                if (processEvents) {
                  new Thread(new Observe(evt, numWhere)).start();
                } else {
                  log.trace("Observe skipped: %s", evt);
                  evt.setWhen(new Date().getTime());
                }
              }
            }
          }
        }
        SX.pause(1);
        if (!isRunning()) {
          for (Element where : wheres) {
            where.observeStop();
          }
        }
        boolean someObserving = false;
        while (true) {
          for (Element where : wheres) {
            someObserving |= where.isObserving();
          }
          if (!isRunning() && someObserving) {
            continue;
          }
          break;
        }
        if (!someObserving) {
          break;
        }
      }
      running = false;
      log.trace("EventLoop: stopped");
    }
  }
  //</editor-fold>

  //<editor-fold desc="Observe">
  private static class Observe implements Runnable {

    Event event = null;
    int nWhere = 0;

    public Observe(Event event, int nWhere) {
      this.event = event;
      this.nWhere = nWhere;
    }

    boolean shouldRepeat = true;

    public void stop() {
      shouldRepeat = false;
    }

    @Override
    public void run() {
      event.getWhere().incrementObserveCount();
      String cType = event.getTypeShort();
      log.trace("Observe start: %s%d in %d", cType, event.getKey(), nWhere);
      while (true) {
        if (event.shouldRepeat()) {
          SX.pause(event.getRepeat());
        }
        event.pause();
        boolean success = false;
        Finder.PossibleMatch possibleMatch = new Finder.PossibleMatch();
        Element where = possibleMatch.get(event.getWhat(), event.getWhere(), nWhere);
        if (event.isAppear()) {
          if (!where.hasMatch()) {
            while (where.isObserving() && shouldRepeat) {
              log.trace("Observe repeat: %s%d in %d", cType, event.getKey(), nWhere);
              possibleMatch.repeat();
              if (where.hasMatch()) {
                success = true;
                break;
              }
            }
          } else {
            success = true;
          }
          if (success) {
            event.setMatch(where.getLastMatch());
            success = true;
          }
        } else if (event.isVanish()) {
          where.setLastVanish(null);
          if (where.hasMatch()) {
            Element match = where.getLastMatch();
            where.setLastVanish(match);
            while (where.isObserving() && shouldRepeat) {
              log.trace("Observe repeat: %s%d in %d", cType, event.getKey(), nWhere);
              possibleMatch.repeat();
              if (where.hasMatch()) {
                match = where.getLastMatch();
                where.setLastVanish(match);
                event.setVanish(match);
              } else {
                success = true;
                break;
              }
            }
          }
        } else if (event.isChange()) {
          log.error("Observe: onChange not implemented: %s", event);
        } else {
          log.error("Observe: Event invalid: %s", event);
        }
        if (success) {
          event.setWhen(new Date().getTime());
          event.incrementCount();
          log.trace("Observe success: %s%d in %d %s", cType, event.getKey(), nWhere,
                  (Event.TYPE.ONAPPEAR.equals(event.isAppear()) ? event.getMatch() : event.getVanish()));
          if (event.hasHandler()) {
            log.trace("Observe handler: %s%d in %d", cType, event.getKey(), nWhere);
            event.getHandler().run(event);
          }
        } else {
          log.trace("Observe stopped: %s%d in %d", cType, event.getKey(), nWhere);
        }
        if (!event.shouldRepeat()) {
          event.getWhere().decrementObserveCount();
          break;
        }
      }
    }
  }
  //</editor-fold>
}