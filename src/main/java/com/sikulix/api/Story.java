/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.devices.LocalDevice;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.*;

public class Story {

  static SXLog log;

  public static Font myFont;

  static {
    log = SX.getSXLog("SX.STORY");
    log.isSX();
    log.on(SXLog.INFO);
    myFont = new Font(Font.DIALOG, Font.BOLD, 16);
    BufferedImage bImg = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
    Graphics2D gImg = (Graphics2D) bImg.getGraphics().create();
    myFont.getLineMetrics("", gImg.getFontRenderContext());
  }

  //<editor-fold desc="housekeeping">
  private static float darker_factor = 0.85f;
  private static int showTime = (int) SX.getOptionNumber("SXShow.showTime", 2);

  private java.util.List<Element> elements = new ArrayList<>();
  private java.util.Map<Element, Object[]> options = new HashMap<>();
  private java.util.List<Symbol> activeElements = new ArrayList<>();
  private Symbol activeElement = null;
  private boolean canDrag = false;
  private boolean isDragging = false;
  private Element dragStart = null;
  private Element currentDrag = null;
  private Element storyTopLeft = null;
  private LocalDevice localDevice;
  private int onMonitor = -1;
  private boolean isFullScreenCapture = false;

  private Element storyBackground = new Element();
  private BufferedImage storyImg = null;
  private BufferedImage showImg = null;

  public Story() {
    log.terminate(1, "transparent story: not implemented");
  }

  public Story(int monitor, int... times) {
    init(monitor, null, false, times);
  }

  public Story(Element background, int... times) {
    init(-1, background, false, times);
  }

  public Story(int monitor, Element background, int... times) {
    init(monitor, background, false, times);
  }

  public Story(Element background, boolean internal) {
    init(-1, background, internal);
  }

  private void init(int monitor, Element background, boolean internal, int... times) {
    if (SX.isNull(background)) {
      storyBackground = new Element(Do.getLocalDevice().getMonitor(monitor));
      onMonitor = storyBackground.getDevice().getContainingMonitorID(storyBackground);
    } else {
      if (monitor > -1) {
        onMonitor = Do.getLocalDevice().getMonitorID(monitor);
      }
      storyBackground = background;
    }
    if (times.length > 0) {
      pauseAfter = times[0];
    }
    if (times.length > 1) {
      pauseBefore = times[1];
    }
    waitForFrame = 0;
    String contentLoaded = "with content";
    if (storyBackground.isSymbol()) {
      add(storyBackground);
      storyImg = plainBackground(storyBackground);
      canDrag = true;
    } else {
      Element screen = new Element(Do.getLocalDevice().getMonitor(monitor));
      int addBorder = 0;
      int screenHeight = screen.h;
      if (!storyBackground.equals(screen) && !internal) {
        withBorder = true;
        addBorder = 2 * borderThickness;
        screenHeight = screen.h - (SX.isMac() ? 21 : 0);
      }
      if (storyBackground.hasContent()) {
        showImg = storyBackground.get();
      } else {
        showImg = storyBackground.capture().get();
        contentLoaded = "captured";
      }
      double storyW = storyBackground.w + addBorder;
      double storyH = storyBackground.h + addBorder;
      if (storyW > screen.w || storyH > screenHeight) {
        double toResize = Math.min(screen.w/storyW, (screenHeight)/storyH);
        showImg = new Picture(showImg).resize(((int) (toResize * 100))/100.0).get();
        withBorder = true;
      }
      new Picture(showImg);
      storyImg = plainBackground(Element.create(showImg));
    }
    storyTopLeft = Element.create(storyImg).getCentered();
    if (withBorder && SX.isMac()) {
      storyTopLeft.y += 11;
    }
    localDevice = (LocalDevice) Do.getLocalDevice();
    log.trace("init: %s: %s", contentLoaded, storyBackground);
  }
  //</editor-fold>

  //<editor-fold desc="attributes">
  private boolean withBorder = false;

  public void setBorder() {
    withBorder = true;
  }

  private boolean shouldAddBorder = false;

  public void addBorder() {
    shouldAddBorder = true;
  }

  private static Color defaultBorderColor = Color.green;
  private Color borderColor = defaultBorderColor;

  public Color getBorderColor() {
    return borderColor;
  }

  public void setBorderColor(Color borderColor) {
    this.borderColor = borderColor;
  }

  private static int defaultBorderThickness = 6;
  private int borderThickness = defaultBorderThickness;
  private int margin = 0;
  private int corner = 20;


  public int getBorderThickness() {
    return borderThickness;
  }

  public void setBorderThickness(int borderThickness) {
    this.borderThickness = borderThickness;
  }

  protected static Color defaultlineColor = Color.red;
  protected static int defaultLineThickness = 3;

  private Color labelColor = new Color(200, 200, 200);

  public Color getLabelColor() {
    return labelColor;
  }

  public void setLabelColor(Color labelColor) {
    this.labelColor = labelColor;
  }

  protected static int defaultShowTime = (int) SX.getOptionNumber("SXShow.showTime", 3);

  int pauseAfter = 0;
  int pauseBefore = 0;
  //</editor-fold>

  //<editor-fold desc="showing">
  public Story start() {
    log.trace("starting");
    showThread = new ShowIt(pauseBefore);
    showThread.running = true;
    SwingUtilities.invokeLater(showThread);
    if (pauseBefore == 0) {
      while (!showThread.isVisible()) {
        SX.pause(0.3);
      }
    }
    if (pauseAfter > 0) {
      new Thread(new ShowStop(showThread, pauseAfter)).start();
    }
    return this;
  }

  public void stop() {
    showThread.stop();
    log.trace("stopping");
  }

  public void show() {
    show(showTime);
  }

  public void show(long time) {
    waitForFrame = 0;
    start();
    showThread.running = true;
    long end = new Date().getTime() + time * 1000;
    while (new Date().getTime() < end) {
      SX.pause(0.5);
      if (!showThread.running) {
        return;
      }
    }
    stop();
  }

  public Picture get() {
    setStoryDim();
    makeStoryImage(storyW, storyH);
    return new Picture(storyImg);
  }

  private void makeStoryImage(int storyW, int storyH) {
    Graphics2D gStory = (Graphics2D) storyImg.getGraphics().create();
    if (SX.isNotNull(showImg)) {
      if (withBorder) {
        gStory.drawImage(showImg, borderThickness, borderThickness, null);
      } else {
        gStory.drawImage(showImg, 0, 0, null);
      }
    }
    if (withBorder) {
      drawRect(INSIDE, gStory, 0, 0, storyW, storyH, borderThickness, borderColor);
    }
    for (Element element : elements) {
      drawElement(gStory, storyBackground, element);
    }
  }

  private void drawElement(Graphics2D g2d, Element currentBase, Element what) {
    Element topLeft;
    Element where = null;
    Object[] whatOptions = options.get(what);
    if (SX.isNotNull(whatOptions)) {
      where = (Element) whatOptions[0];
    }
    if (what.isSymbol()) {
      Symbol symbol = (Symbol) what;
      if (symbol.isRectangle() || symbol.isCircle() || symbol.isButton()) {
        if (SX.isNull(where)) {
          topLeft = what.getCentered(currentBase);
        } else {
          topLeft = where;
        }
        int stroke = symbol.getLine();
        if (symbol.isButton()) {
          if (SX.isNotNull(symbol.getFillColor())) {
            g2d.setColor(symbol.getFillColor());
            g2d.fillRoundRect(topLeft.x, topLeft.y, symbol.w, symbol.h, corner, corner);
          }
          drawRect(ROUNDED, g2d, topLeft.x, topLeft.y, symbol.w, symbol.h, stroke, symbol.getColor());
        } else if (symbol.isRectangle()) {
          drawRect(INSIDE, g2d, topLeft.x, topLeft.y, symbol.w, symbol.h, stroke, symbol.getColor());
          if (SX.isNotNull(symbol.getFillColor())) {
            g2d.setColor(symbol.getFillColor());
            g2d.fillRect(topLeft.x + stroke, topLeft.y + stroke,
                    symbol.w - 2 * (stroke), symbol.h - 2 * (stroke));
          }
        } else if (symbol.isCircle()) {
          g2d.setStroke(new BasicStroke(stroke));
          g2d.setColor(symbol.getColor());
          g2d.drawArc(topLeft.x + stroke / 2, topLeft.y + stroke / 2,
                  symbol.w - stroke, symbol.h - stroke, 0, 360);
          if (SX.isNotNull(symbol.getFillColor())) {
            g2d.setColor(symbol.getFillColor());
            g2d.fillArc(topLeft.x + stroke, topLeft.y + stroke,
                    symbol.w - 2 * stroke, symbol.h - 2 * stroke, 0, 360);
          }
        }
        symbol.x = topLeft.x;
        symbol.y = topLeft.y;
      }
    } else {
      int locX = what.x - currentBase.x + margin;
      int locY = what.y - currentBase.y + margin;
      if (what.isMatch()) {
        int rectW = 50;
        int rectH = 20;
        int rectX = locX;
        int rectY = locY - what.getHighLightLine() - rectH;
        if (rectY < 30) {
          rectY += what.h + rectH + 3 * what.getHighLightLine();
        }
        int margin = 2;
        int fontSize = rectH - margin * 2;
        g2d.setColor(labelColor);
        g2d.fillRect(rectX, rectY, rectW, rectH);
        g2d.setColor(Color.black);
        g2d.setFont(myFont);
        double hlScore = what.isMatch() ? Math.min(what.getScore(), 0.9999) : 0;
        g2d.drawString(String.format("%05.2f", 100 * hlScore), rectX + margin, rectY + fontSize);
      }
      drawRect(AROUND, g2d, locX, locY, what.w, what.h,
              what.getHighLightLine(), what.getLineColor());
    }
  }

  private int INSIDE = 0;
  private int AROUND = 1;
  private int ROUNDED = 2;
  private int storyW, storyH;

  private void setStoryDim() {
    if (SX.isNotNull(showImg)) {
      storyW = showImg.getWidth() + margin * 2;
      storyH = showImg.getHeight() + margin * 2;
    } else {
      storyW = storyBackground.w;
      storyH = storyBackground.h;
    }
  }

  private void drawRect(int type, Graphics2D g2d, int x, int y, int w, int h, int stroke, Color color) {
    g2d.setColor(color);
    int offset = stroke / 2;
    int margin = -stroke;
    if (type == AROUND) {
      stroke = ((stroke + 1) / 2) * 2;
      offset = -stroke / 2;
      margin = stroke;
    }
    g2d.setStroke(new BasicStroke(stroke));
    if (type == ROUNDED) {
      g2d.drawRoundRect(x + offset, y + offset, w + margin, h + margin, corner, corner);
    } else {
      g2d.drawRect(x + offset, y + offset, w + margin, h + margin);
    }
  }

  public void waitForEnd() {
    while (isRunning()) SX.pause(0.3);
  }

  public boolean isRunning() {
    return showThread.running;
  }
  //</editor-fold>

  //<editor-fold desc="story frame">
  private ShowIt showThread = null;

  private class ShowIt implements Runnable {
    public boolean running = false;
    private JFrame frame = new JFrame();
    private int waitBefore = 0;

    public ShowIt() {
    }

    public ShowIt(int waitBefore) {
      this.waitBefore = waitBefore;
    }

    private String logStoryFrame() {
      return String.format("[%d,%d %dx%d]", storyTopLeft.x, storyTopLeft.y, storyW, storyH);
    }

    @Override
    public void run() {
      frame.setUndecorated(true);
      frame.setAlwaysOnTop(true);

      frame.addMouseListener(new MouseInputAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          super.mousePressed(e);
          if (canDrag) {
            Element loc = new Element(e.getPoint());
            log.trace("pressed at: (%d,%d) story: %s", loc.x, loc.y, logStoryFrame());
            dragStart = loc;
          }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
          super.mouseReleased(e);
          Element loc = new Element(e.getPoint());
          log.trace("released at: (%d,%d) story: %s", loc.x, loc.y, logStoryFrame());
          activeElement = null;
          if (!isDragging) {
            if (activeElements.size() > 0) {
              for (Symbol symbol : activeElements) {
                if (symbol.contains(loc)) {
                  activeElement = symbol;
                }
                log.trace("clicked active symbol: %s story: %s", getClickedSymbol(), logStoryFrame());
              }
            }
            stop();
          } else {
            currentDrag = localDevice.at();
            storyTopLeft.x = currentDrag.x - dragStart.x;
            storyTopLeft.y = currentDrag.y - dragStart.y;
            frame.setLocation(storyTopLeft.x, storyTopLeft.y);
            frame.repaint();
            log.trace("end dragging at: (%d,%d) story: %s", loc.x, loc.y, logStoryFrame());
            SX.pause(0.5);
          }
          dragStart = null;
          isDragging = false;
        }
      });

      frame.addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseDragged(MouseEvent e) {
          super.mouseDragged(e);
          if (!canDrag) {
            return;
          }
          String dragState = "continue";
          if (!isDragging) {
            isDragging = true;
            dragState = "start";
          }
          currentDrag = localDevice.at();
          storyTopLeft.x = currentDrag.x - dragStart.x;
          storyTopLeft.y = currentDrag.y - dragStart.y;
          frame.setLocation(storyTopLeft.x, storyTopLeft.y);
          frame.repaint();
          log.trace("%s dragging at (%d,%d) story: %s", dragState, currentDrag.x, currentDrag.y, logStoryFrame());
        }

        @Override
        public void mouseMoved(MouseEvent e) {
          super.mouseMoved(e);
        }
      });

      //<editor-fold desc="panel">
      JPanel panel = new JPanel() {
        @Override
        public void paintComponent(Graphics g) {
          super.paintComponent(g);
          Graphics2D gPanel = (Graphics2D) g.create();
          makeStoryImage(storyW, storyH);
          gPanel.drawImage(storyImg, 0, 0, null);
          gPanel.dispose();
        }
      };
      //</editor-fold>

      Element onElement = new Element(Do.getDevice().getMonitor(onMonitor));
      if (withBorder || shouldAddBorder) {
        margin = borderThickness;
      }
      setStoryDim();
      Dimension panelSize = new Dimension(storyW, storyH);
      panel.setPreferredSize(panelSize);
      Element location = new Element(panelSize).getCentered(onElement);
      Container contentPane = frame.getContentPane();
      contentPane.setLayout(new OverlayLayout(contentPane));
      contentPane.add(panel);
      frame.pack();
      frame.setLocation(storyTopLeft.x, storyTopLeft.y);
      if (waitBefore > 0) {
        new Thread(new ShowWaitBefore(frame, waitBefore)).start();
      } else {
        frame.setVisible(true);
        log.trace("Story frame visible");
      }
    }

    public void stop() {
      if (running) {
        frame.dispose();
        running = false;
        log.trace("Story frame disposed");
      }
    }

    //<editor-fold desc="painting">
    public boolean isVisible() {
      return frame.isVisible();
    }

    public Element whereShowing() {
      return new Element(frame.getLocation().x, frame.getLocation().y, frame.getWidth(), frame.getHeight());
    }
    //</editor-fold>
  }

  private class ShowStop implements Runnable {
    private ShowIt showThread = null;
    private int waitTime;

    public ShowStop(ShowIt showThread, int waitTime) {
      this.showThread = showThread;
      this.waitTime = waitTime;
    }

    @Override
    public void run() {
      log.trace("Story pause after visible before stop: %d", waitTime);
      SX.pause(waitTime);
      showThread.stop();
    }
  }

  private class ShowWaitBefore implements Runnable {
    private Frame frame = null;
    private int waitTime;

    public ShowWaitBefore(Frame frame, int waitTime) {
      this.frame = frame;
      this.waitTime = waitTime;
    }

    @Override
    public void run() {
      log.trace("Story pause before visible after start: %d", waitTime);
      SX.pause(waitTime);
      frame.setVisible(true);
      log.trace("Story frame visible");
    }
  }

  public void setWaitForFrame() {
    this.waitForFrame = 2;
  }

  public void resetWaitForFrame() {
    this.waitForFrame = 0;
  }

  private int waitForFrame = 0;

  //</editor-fold>

  //<editor-fold desc="handling">
  private BufferedImage plainBackground(Element elem) {
    int margin = withBorder ? 2 * borderThickness : 0;
    BufferedImage bImg = new BufferedImage(elem.w + margin, elem.h + margin, BufferedImage.TYPE_3BYTE_BGR);
    Graphics2D graphics = bImg.createGraphics();
    graphics.setPaint(new Color(255, 255, 255));
    graphics.fillRect(0, 0, bImg.getWidth(), bImg.getHeight());
    return bImg;
  }

  public Element whereShowing() {
    return showThread.whereShowing();
  }

  public Symbol getClickedSymbol() {
    Symbol clicked = new Symbol(activeElement);
    clicked.at(whereShowing());
    return clicked;
  }

  public boolean hasClickedSymbol() {
    return SX.isNotNull(activeElement);
  }
  //</editor-fold>

  public Story add(Object... args) {
//                (Element elem, Element where, Color lineColor, int lineThickness) {
    if (args.length == 0) {
      return this;
    }
    if (!(args[0] instanceof Element)) {
      return this;
    }
    int nextOption = 1;
    Element what = (Element) args[0];
    Element where = null;
    if (args.length > 1 && args[1] instanceof Element) {
      where = (Element) args[1];
      nextOption = 2;
    }
    Color lineColor = null;
    Integer lineThickness = null;
    while (args.length > nextOption) {
      if (args[nextOption] instanceof Color) {
        lineColor = (Color) args[nextOption];
      } else if (args[nextOption] instanceof Integer) {
        lineThickness = (Integer) args[nextOption];
      }
      nextOption++;
    }
    if (what.isSymbol()) {
      elements.add(what);
      options.put(what, new Object[]{where});
      if (((Symbol) what).isActive()) {
        activeElements.add((Symbol) what);
      }
    } else {
      elements.add(what);
      options.put(what, new Object[]{where, lineColor, lineThickness});
    }
    return this;
  }

  public static void testTransparency() {
    LocalDevice device = (LocalDevice) Do.getLocalDevice();
    GraphicsDevice gd = device.getGraphicsDevice(0);
    boolean hasTransparency = gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
//        hasTransparency = gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT);
//        hasTransparency = gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);
    if (hasTransparency) {
      JFrame frame = new JFrame();
      frame.setUndecorated(true);
      frame.setBackground(new Color(0, 0, 0, 0.1f));

      Container contentPane = frame.getContentPane();
      contentPane.setLayout(new OverlayLayout(contentPane));

      JLabel label = new JLabel("TRANSPARENCY");
      label.setOpaque(false);
      label.setAlignmentX(0.5f);
      label.setAlignmentY(0.5f);
      Font font = Story.myFont;
      label.setFont(font.deriveFont(Font.PLAIN, 72f));
      label.setForeground(Color.WHITE);
      label.setBackground(Color.RED);
      label.setOpaque(true);
      contentPane.add(label);

      Dimension fSize = new Dimension(800, 500);
      Element where = new Element(fSize).getCentered();
      frame.setLocation(where.x, where.y);
      frame.setPreferredSize(fSize);
      frame.pack();
      frame.setVisible(true);
      Do.wait(1.0);
      int n = 3;
      while(n > 0) {
        label.setVisible(false);
        Do.wait(0.3);
        label.setVisible(true);
        Do.wait(0.3);
        n--;
      }
    }
  }
}
