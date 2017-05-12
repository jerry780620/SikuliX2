/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */
package com.sikulix.devices.vnc;

import com.sikulix.api.Picture;
import org.sikuli.script.IScreen;
import org.sikuli.script.Region;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;

public class VNCScreen extends Region implements IScreen, Closeable {
  private final VNCClient client;
  private volatile boolean closed;
  //private final IRobot robot;
  private Picture lastScreenImage;

  public static VNCScreen start(String theIP, int thePort, String password, int cTimeout, int timeout) throws IOException {
    return new VNCScreen(VNCClient.connect(theIP, thePort, password, true));
  }

  public static VNCScreen start(String theIP, int thePort, int cTimeout, int timeout) throws IOException {
    return new VNCScreen(VNCClient.connect(theIP, thePort, null, true));
  }

  public void stop() throws IOException {
    close();
  }

  public static void stopAll() {
  }

  private VNCScreen(final VNCClient client) {
    this.client = client;
    //this.robot = new VNCRobot(this);
//    setOtherScreen(this);
//    setRect(getBounds());
    initScreen(this);
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
    client.refreshFramebuffer();
  }

  @Override
  public void close() throws IOException {
    closed = true;
    client.close();
  }

  @Override
  public Rectangle getBounds() {
    return client.getBounds();
  }

  @Override
  public Picture capture() {
    return capture(getBounds());
  }

  public Picture capture(Region reg) {
    return capture(reg.x, reg.y, reg.w, reg.h);
  }

  public Picture capture(Rectangle rect) {
    return capture(rect.x, rect.y, rect.width, rect.height);
  }

  public Picture capture(int x, int y, int w, int h) {
    BufferedImage image = client.getFrameBuffer(x, y, w, h);
    Picture img = new Picture(image);
    lastScreenImage = img;
    return img;
  }

  @Override
  public boolean isOtherScreen() {
    return false;
  }

  @Override
  public int getID() {
    return 0;
  }

  public Picture userCapture(final String msg) {
//TODO VNCScreen: implement userCapture()
//    final OverlayCapturePrompt prompt = new OverlayCapturePrompt(this);
//
//    Thread th = new Thread() {
//      @Override
//      public void run() {
//        prompt.prompt(msg);
//      }
//    };
//
//    th.start();
//
//    boolean hasShot = false;
//    ScreenImage simg = null;
//    int count = 0;
//    while (!hasShot) {
//      this.wait(0.1f);
//      if (count++ > 300) {
//        break;
//      }
//      if (prompt == null) {
//        continue;
//      }
//      if (prompt.isComplete()) {
//        simg = prompt.getSelection();
//        if (simg != null) {
//          lastScreenImage = simg;
//          hasShot = true;
//        }
//        prompt.close();
//      }
//    }
//    prompt.close();
//
    return new Picture();
  }

  public VNCClient getClient() {
    return client;
  }
}
