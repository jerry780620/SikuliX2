/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.devices.vnc;

import com.sikulix.api.Element;
import com.sikulix.api.Picture;
import com.sikulix.core.*;
import com.sikulix.devices.IDevice;
import com.sikulix.vnc.*;

import java.awt.Rectangle;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VNCDevice extends IDevice implements Closeable {

  static SXLog log;

  static {
    log = SX.getSXLog("SX.VNCDEVICE");
    log.isSX();
    log.on(SXLog.TRACE);
  }

  private static String parameterNames = "ip,port,password,user,connectionTimeout,timeout";
  private static String parameterClass = "s,i,h,s,i,i";
  private static Object[] parameterDefaults = new Object[]{"127.0.0.1", 5900, null, null, 10, 1000};
  private static Object[] parameterNotSet = new Object[]{"", 0, null, null, 0, 0};

  private static Parameters parameters = new Parameters(parameterNames, parameterClass, parameterDefaults, parameterNotSet);

  //<editor-fold desc="parameter: getter, setter">
  public String getIp() {
    return ip;
  }

  public void setIp(Object ip) {
    this.ip = (String) ip;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Object port) {
    this.port = (Integer) port;
  }

  public String getUser() {
    return user;
  }

  public void setUser(Object user) {
    this.user = (String) user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(Object password) {
    this.password = (String) password;
  }

  public Integer getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(Object connectionTimeout) {
    this.connectionTimeout = (Integer) connectionTimeout;
  }

  public Integer getTimeout() {
    return timeout;
  }

  public void setTimeout(Object timeout) {
    this.timeout = (Integer) timeout;
  }
  //</editor-fold>

  private static List<IDevice> devices = new ArrayList<>();

  private String ip = null;
  private Integer port = null;
  private String user = null;
  private String password = null;
  private Integer connectionTimeout = null;
  private Integer timeout = null;

  private VNCClient client = null;
  private volatile boolean closed;
  private Picture lastScreenImage;

  @Override
  public IDevice start(Object... args) {
    boolean available = false;
    try {
      Class.forName("com.sikulix.vnc.VNCCLient");
      available = true;
    } catch (ClassNotFoundException e) {
    }
    if (available) {
      parameters.initParameters(this, args);
      log.trace("start(): %s", parameters);
      try {
        client = VNCClient.connect(ip, port, password, true);
        new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              client.processMessages();
            } catch (RuntimeException e) {
              if (!closed) {
                throw e;
              }
            }
          }
        }).start();
        capture();
        devices.add(this);
        return this;
      } catch (Exception e) {
        log.error("VNCClient.connect: did not work: %s", e.getMessage());
      }
    } else {
      log.error("VNC not working: sikulix2tigervnc not on classpath");
    }
    return null;
  }

  @Override
  public void stop() {
    log.trace("stop(): %s", getIp());
    devices.remove(this);
    close();
  }

  @Override
  public void close() {
    closed = true;
    client.close();
    client = null;
  }

  public static void stopAll() {
    if (devices.size() > 0) {
      for (IDevice device : devices.toArray(new IDevice[]{})) {
        device.stop();
        devices.remove(device);
      }
    }
  }

  @Override
  public boolean isValid() {
    return SX.isNotNull(client);
  }

  @Override
  public int getNumberOfMonitors() {
    return 0;
  }

  @Override
  public Rectangle getMonitor(int... id) {
    Rectangle bounds = client.getBounds();
    return null;
  }

  @Override
  public Rectangle getAllMonitors() {
    return null;
  }

  @Override
  public int getMonitorID() {
    return 0;
  }

  @Override
  public int getMonitorID(int id) {
    return 0;
  }

  @Override
  public void resetMonitors() {

  }

  @Override
  public Rectangle[] getMonitors() {
    return new Rectangle[0];
  }

  @Override
  public int getContainingMonitorID(Element element) {
    return 0;
  }

  @Override
  public Element getContainingMonitor(Element element) {
    return null;
  }

  @Override
  public Element click(Element loc) {
    return null;
  }

  @Override
  public Element doubleClick(Element loc) {
    return null;
  }

  @Override
  public Element rightClick(Element loc) {
    return null;
  }

  @Override
  public Element click(Action action) {
    return null;
  }

  @Override
  public Element click(Element loc, Action action) {
    return null;
  }

  @Override
  public Element dragDrop(Element from, Element to, Object... times) {
    return null;
  }

  @Override
  public void keyStart() {

  }

  @Override
  public void keyStop() {

  }

  @Override
  public void key(Action action, Object key) {

  }

  /**
   * move the mouse from the current position to the offset given by the parameterTypes
   *
   * @param xoff horizontal offset (&lt; 0 left, &gt; 0 right)
   * @param yoff vertical offset (&lt; 0 up, &gt; 0 down)
   * @return the new mouseposition as Element (might be invalid)
   */
  @Override
  public Element move(int xoff, int yoff) {
    return null;
  }

  /**
   * move the mouse to the target of given Element (default center)
   *
   * @param loc
   * @return the new mouseposition as Element (might be invalid)
   */
  @Override
  public Element move(Element loc) {
    return null;
  }

  /**
   * @return the current mouseposition as Element (might be invalid)
   */
  @Override
  public Element at() {
    return null;
  }

  @Override
  public void button(Action action) {

  }

  @Override
  public void wheel(Action action, int steps) {

  }

  int maxChecks = 100;

  @Override
  public Picture capture(Object... args) {
    Element what = new Element(client.getBounds());
    if (args.length > 0) {
      if (args[0] instanceof Element) {
        what = (Element) args[0];
      }
    }
    if (maxChecks > 0) {
      client.refreshFramebuffer(what.x, what.y, what.w, what.h, false);
    }
    Picture picture2;
    Picture picture1 = new Picture(client.getFrameBuffer(what.x, what.y, what.w, what.h));
    if (maxChecks > 0) {
      SX.pause(0.15);
      picture2 = new Picture(client.getFrameBuffer(what.x, what.y, what.w, what.h));
      List<Element> rectangles = Finder.detectChanges(picture1.getContent(), picture2.getContent());
      while (rectangles.size() == 0) {
        picture1 = new Picture(picture2);
        picture2 = new Picture(client.getFrameBuffer(what.x, what.y, what.w, what.h));
        rectangles = Finder.detectChanges(picture1.getContent(), picture2.getContent());
        maxChecks--;
        if (maxChecks < 0) {
          break;
        }
      }
      while (rectangles.size() > 0) {
        picture1 = new Picture(picture2);
        picture2 = new Picture(client.getFrameBuffer(what.x, what.y, what.w, what.h));
        rectangles = Finder.detectChanges(picture1.getContent(), picture2.getContent());
      }
      maxChecks = 0;
    } else {
      return picture1;
    }
    return picture2;
  }
}
