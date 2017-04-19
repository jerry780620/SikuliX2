/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.core;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.NativeInputEvent;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseInputListener;
import org.jnativehook.mouse.NativeMouseWheelEvent;
import org.jnativehook.mouse.NativeMouseWheelListener;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NativeHook implements NativeKeyListener, NativeMouseInputListener, NativeMouseWheelListener {

  //<editor-fold desc="housekeeping">
  static SXLog log;

  static {
    log = SX.getLogger("SX.NativeHook");
    log.isSX();
    log.on(SXLog.INFO);
  }

  static NativeHook listener = null;
  static ExecutorService dispatcher = null;
  static Map<Integer, NativeHookCallback> toBeConsumed = new HashMap<Integer, NativeHookCallback>();

  private ConcurrentLinkedQueue<NativeInputEvent> eventQueue = new ConcurrentLinkedQueue<NativeInputEvent>();

//  public Rectangle getMainMonitor() {
//    return mainMonitor;
//  }
//
//  public void setMainMonitor(Rectangle mainMonitor) {
//    this.mainMonitor = mainMonitor;
//  }
//
//  public double getDistMPos() {
//    return distMPos;
//  }
//
//  public void setDistMPos(double distMPos) {
//    this.distMPos = distMPos;
//  }

  private Rectangle mainMonitor;
  double distMPos = 0;

  private int eventQueueSize = 0;

  private Integer keyStop = NativeKeyEvent.VC_ESCAPE;

  public void setStopKey(Integer stopKey) {
    if (keyStop != null) {
      toBeConsumed.remove(keyStop);
    }
    keyStop = stopKey;
    if (keyStop != null) {
      toBeConsumed.put(keyStop, null);
    }
  }

  private boolean runningGesture = false;
  private boolean hookLocked = false;
  private int defPoints = 100;
  //</editor-fold>

  //<editor-fold desc="mouse position and info">
  Point lastMousePosition = new Point(0, 0);
  NativeInputEvent currentEvent = null;

  public Point getMousePosition() {
    return lastMousePosition;
  }

  //</editor-fold>

  //<editor-fold desc="hook start / stop">
  private NativeHook() {
  }

  private void init() {
    if (eventQueueSize == 0) {
      mainMonitor = SX.getSXLOCALDEVICE().getMonitor(SX.getSXLOCALDEVICE().getMonitorID());
      distMPos = Math.sqrt(mainMonitor.getWidth() * mainMonitor.getWidth() +
              mainMonitor.getHeight() * mainMonitor.getHeight()) / 150;
      eventQueueSize = (int) ((mainMonitor.width + mainMonitor.height) / distMPos * 1.2);
    }
  }

  public static NativeHook start() {
    if (SX.isNull(listener)) {
      listener = new NativeHook();
      Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
      logger.setLevel(Level.INFO);
      logger.setUseParentHandlers(false);

      try {
        dispatcher = new VoidDispatchService();
        if (dispatcher != null) {
          GlobalScreen.setEventDispatcher(dispatcher);
        }
        GlobalScreen.registerNativeHook();
        log.trace("registerNativeHook: OK");
      } catch (NativeHookException ex) {
        log.error("registerNativeHook: %s", ex.getMessage());
        System.exit(1);
      }

      GlobalScreen.addNativeKeyListener(listener);
      GlobalScreen.addNativeMouseListener(listener);
      GlobalScreen.addNativeMouseMotionListener(listener);
    }
    return listener;
  }

  public void stop() {
    if (listener == null) {
      return;
    }
    Runnable stopit = new Runnable() {
      public void run() {
        try {
          GlobalScreen.removeNativeKeyListener(listener);
          GlobalScreen.removeNativeMouseListener(listener);
          GlobalScreen.unregisterNativeHook();
          log.trace("unregisterNativeHook: OK");
          listener.clearQueue(0);
          listener = null;
          dispatcher = null;
        } catch (NativeHookException e) {
          log.error("unregisterNativeHook: %s", e.getMessage());
          System.exit(1);
        }
      }
    };
    SwingUtilities.invokeLater(stopit);
  }
  //</editor-fold>

  //<editor-fold desc="consume keys">
  public String consumeKey(NativeKeyEvent e) {
    String msg = "";
    if (dispatcher != null) {
      try {
        Field f = NativeInputEvent.class.getDeclaredField("reserved");
        f.setAccessible(true);
        f.setShort(e, (short) 0x01);
      } catch (Exception ex) {
        msg = ex.getMessage();
      }
    }
    return msg;
  }

  private static class VoidDispatchService extends AbstractExecutorService {
    private boolean running = false;

    public VoidDispatchService() {
      running = true;
    }

    public void shutdown() {
      running = false;
    }

    public List<Runnable> shutdownNow() {
      running = false;
      return new ArrayList<Runnable>(0);
    }

    public boolean isShutdown() {
      return !running;
    }

    public boolean isTerminated() {
      return !running;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      return true;
    }

    public void execute(Runnable r) {
      r.run();
    }
  }

  public void addCallback(int key, NativeHookCallback cb) {
    toBeConsumed.put(key, cb);
  }
  //</editor-fold>

  //<editor-fold desc="keys">
  public void nativeKeyPressed(NativeKeyEvent evt) {
    if (hookLocked) {
      return;
    }
    if (dispatcher != null && toBeConsumed.containsKey(evt.getKeyCode())) {
      log.trace("consumeKeyPressed: %s %s", NativeKeyEvent.getKeyText(evt.getKeyCode()), consumeKey(evt));
    } else {
      log.trace("Key Pressed: %s", NativeKeyEvent.getKeyText(evt.getKeyCode()));
    }
  }

  public void nativeKeyReleased(NativeKeyEvent evt) {
    if (hookLocked) {
      return;
    }
    if (dispatcher != null && toBeConsumed.containsKey(evt.getKeyCode())) {
      log.trace("consumeKeyReleased: %s %s", NativeKeyEvent.getKeyText(evt.getKeyCode()), consumeKey(evt));
      if (toBeConsumed.get(evt.getKeyCode()) != null) {
        toBeConsumed.get(evt.getKeyCode()).callback(evt);
      } else if (keyStop != null && evt.getKeyCode() == keyStop) {
        log.info("should stop requested (key)");
        stop();
      }
    } else {
      log.trace("Key Released: %s", NativeKeyEvent.getKeyText(evt.getKeyCode()));
      return;
    }
  }

  public void nativeKeyTyped(NativeKeyEvent evt) {
    if (hookLocked) {
      return;
    }
    if (evt.getKeyCode() == 0) {
      return;
    }
    log.trace("Key Typed: %s", NativeKeyEvent.getKeyText(evt.getKeyCode()));
  }
  //</editor-fold>

  //<editor-fold desc="mouse">
  public void nativeMouseClicked(NativeMouseEvent evt) {
    if (hookLocked) {
      return;
    }
    collectEvent(evt);
    log.trace("Mouse Clicked: %s, %s", evt.getButton(), evt.getPoint());
  }

  public void nativeMousePressed(NativeMouseEvent evt) {
    if (hookLocked) {
      return;
    }
    log.trace("Mouse Pressed: %s, %s", evt.getButton(), evt.getPoint());
  }

  public void nativeMouseReleased(NativeMouseEvent evt) {
    if (hookLocked) {
      return;
    }
    log.trace("Mouse Released: %s, %s", evt.getButton(), evt.getPoint());
  }

  public void nativeMouseMoved(NativeMouseEvent evt) {
    if (hookLocked) {
      return;
    }
    Point current = evt.getPoint();
    currentEvent = evt;
    if (runningGesture) {
      if( current.distance(lastMousePosition) > distMPos) {
        log.trace("nativeMouseMoved: %s", evt.paramString());
        collectEvent(evt);
        lastMousePosition = current;
      }
    } else {
      lastMousePosition = current;
    }
  }

  public void nativeMouseDragged(NativeMouseEvent evt) {
    if (hookLocked) {
      return;
    }
  }

  public void nativeMouseWheelMoved(NativeMouseWheelEvent evt) {
    if (hookLocked) {
      return;
    }
  }
  //</editor-fold>

  //<editor-fold desc="gesture">
  public boolean startGesture() {
    return startGesture(defPoints);
  }

  public boolean startGesture(int points) {
    if (runningGesture) {
      return false;
    }
    clearQueue(points);
    runningGesture = true;
    return true;
  }

  public boolean stopGesture() {
    if (hookLocked || !runningGesture) {
      return false;
    }
    hookLocked = true;
    return true;
  }

  public void allowGesture() {
    runningGesture = false;
    hookLocked = false;
  }

  private NativeInputEvent collectEvent(NativeInputEvent evt) {
    init();
    while (eventQueue.size() >= eventQueueSize) {
      eventQueue.poll();
    }
    eventQueue.add(evt);
    return evt;
  }

  public void clearQueue(int max) {

    eventQueue.clear();
    currentEvent = null;
    if (max > 0) {
      eventQueueSize = max;
    }
  }

  public List<Object> getQueue() {
    List<Object> content = new ArrayList<Object>();
    NativeInputEvent evt = eventQueue.poll();
    if (!SX.isNull(evt)) {
      while (!SX.isNull(evt = eventQueue.poll())) {
        content.add(evt);
      }
    }
    content.add(currentEvent);
    return content;
  }

  public NativeInputEvent getQueueLast() {
    return currentEvent;
  }
  //</editor-fold>
}
