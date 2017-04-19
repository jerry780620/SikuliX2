/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SXLog {

  public static final int INFO = 1;
  public static final int DEBUG = 3;
  public static final int TRACE = 4;
  public static final int OFF = 0;
  public static final int ERROR = -1;
  public static final int FATAL = -2;

  int initLevel = -1;
  int currentLevel = -1;

  static int globalLevel = -2;
  static int globalLevelSaved = -1;

  boolean logError = true;

  Logger logger = null;

  static long globalStart = 0;
  static long globalLap = 0;

  public String getCurrentMethodName() {
    StackTraceElement stackTraceElements[] = (new Throwable()).getStackTrace();
    StackTraceElement stackTraceElement = stackTraceElements[1];
    String methodName = stackTraceElement.getMethodName();
    return methodName;
  }

  public Map<String, String> getCaller() {
    Map<String, String> caller = new HashMap<>();
    StackTraceElement stackTraceElements[] = (new Throwable()).getStackTrace();
    StackTraceElement stackTraceElement = stackTraceElements[2];
    caller.put("class", stackTraceElement.getClassName());
    caller.put("method", stackTraceElement.getMethodName());
    caller.put("line", "" + stackTraceElement.getLineNumber());
    caller.put("file", stackTraceElement.getFileName());
    return caller;
  }

  public void startTimer() {
    globalStart = new Date().getTime();
    globalLap = globalStart;
    globalLevelSaved = globalLevel;
    globalLevel = TRACE;
    trace("global timer start");
  }

  public void stopTimer() {
    globalStart = 0;
    globalLevel = globalLevelSaved;
    trace("global timer end");
  }

  protected SXLog() {
    init(null, null, -1);
    logger = LogManager.getLogger("SX.Global");
    getTranslation("sxinit: entry", "");
  }

  public SXLog(String className, String[] args, int level) {
    init(className, args, level);
  }

  private void init(String className, String[] args, int level) {
    if (globalLevel < ERROR) {
      String logOption = System.getProperty("sikulix.logging");
      if (SX.isSet(logOption)) {
        logOption = logOption.toLowerCase();
        if (logOption.startsWith("q")) {
          globalLevel = OFF;
        } else if (logOption.startsWith("d")) {
          globalLevel = DEBUG;
        } else if (logOption.startsWith("t")) {
          globalLevel = TRACE;
        }
      } else {
        globalLevel = ERROR;
      }
    }
    on(initLevel);
    initLevel = currentLevel;
    if (SX.isNull(className)) {
      return;
    }
    SX.sxinit(args);
    if (className.startsWith("!")) {
      translation = false;
      className = className.substring(1);
    } else {
      translation = className.startsWith("SX.");
    }
    if (className.startsWith("TestSX")) {
//      globalTranslation = false;
    }
    logger = LogManager.getLogger(className);
  }

  public void on(int level) {
    if (sxComponent && (isGlobalLevel(DEBUG) || isGlobalLevel(TRACE))) {
      currentLevel = globalLevel;
    } else {
      if (level > ERROR && level <= TRACE) {
        currentLevel = level;
      } else {
        currentLevel = ERROR;
      }
    }
    errorOn();
  }

  public void globalOn(int level) {
    if (level > ERROR && level <= TRACE) {
      globalLevel = level;
    } else {
      globalLevel = ERROR;
    }
  }

  public boolean isLevel(int level) {
    return currentLevel >= level;
  }

  public boolean isGlobalLevel(int level) {
    return globalLevel >= level;
  }

  public void reset() {
    currentLevel = initLevel;
  }

  public void stop() {
    currentLevel = OFF;
    errorOff();
  }

  public void globalStop() {
    globalLevel = 0;
  }

  public void errorOff() {
    logError = false;
  }

  public void errorOn() {
    logError = true;
  }

  boolean sxComponent = false;

  public void isSX() {
    sxComponent = true;
  }

  public void p(String msg, Object... args) {
    System.out.println(String.format(msg, args));
  }

  public void p() {
    System.out.println(String.format("******** intended breakpoint ********* %s", getCurrentMethodName()));
  }

  public void info(String message, Object... args) {
    log(INFO, message, args);
  }

  public void error(String message, Object... args) {
    log(ERROR, message, args);
  }

  public void debug(String message, Object... args) {
    log(DEBUG, message, args);
  }

  public void trace(String message, Object... args) {
    if (globalStart > 0) {
      long lap = new Date().getTime();
      message = "G(" + (lap - globalStart) + " | " + (lap - globalLap) + ") " + message;
      globalLap = lap;
    }
    log(TRACE, message, args);
  }

  public void fatal(String message, Object... args) {
    log(FATAL, message, args);
  }

  public void terminate(int retval, String message, Object... args) {
    //TODO terminate: check IDE is running (additionally popup)
    if (retval != 0) {
      log(FATAL, message, args);
    } else {
      log(INFO, message, args);
    }
    System.exit(retval);
  }

  private void log(int level, String message, Object... args) {
    String msgPlus = "";
    if (level == FATAL) {
      msgPlus = "terminating";
    }
    if (isLogging(level)) {
      message = getTranslation(message, msgPlus);
      if (!message.startsWith("!")) {
        message = String.format(message, args).replaceAll("\\n", " ");
      } else {
        message = String.format(message.substring(1), args);
      }
      if (level == DEBUG) {
        logger.debug(message, args);
      } else if (level > DEBUG) {
        logger.trace(message, args);
      } else if (level == ERROR) {
        if (logError) {
          logger.error(message, args);
        }
      } else {
        logger.info(message, args);
      }
    }
  }

  private boolean isLogging(int level) {
    if (level == FATAL) {
      return true;
    }
    if (globalLevel == OFF) {
      return false;
    }
    if (level < OFF || currentLevel == OFF) {
      return logError;
    }
    return currentLevel >= level || globalLevel >= level;
  }

  //<editor-fold desc="translation">
  private static Map<String, Properties> translateProps = new HashMap<>();

  private static boolean globalTranslation = true;

  public static void setGlobalTranslation(boolean state) {
    globalTranslation = state;
  }

  public static boolean isGlobalTranslation() {
    return globalTranslation;
  }

  private boolean translation = true;

  public void setTranslation(boolean state) {
    translation = state;
  }

  public boolean isTranslation() {
    return translation;
  }

  private String getTranslation(String msg, String msgPlus) {
    if (!globalTranslation || !translation) {
      return (SX.isSet(msgPlus) ? "*** " + msgPlus + ": " : "") + msg;
    }
    if (msg.startsWith("!")) {
      return msg.substring(1);
    }
    String orgMsg = msg;
    String clazz = logger.getName().replaceAll("\\.", "");
    Properties currentProps = null;
    if (!translateProps.containsKey(clazz)) {
      currentProps = null;
      String resProp = "--NotSet--";
      InputStream isProps = null;
      try {
        String language = "en_US";
        resProp = "i18n/translations/sikulix2." + clazz.toLowerCase() + "_en_usproperties";
        resProp += "/" + language + ".properties";
        isProps = this.getClass().getClassLoader().getResourceAsStream(resProp);
        if (!SX.isNull(isProps)) {
          currentProps = new Properties();
          currentProps.load(isProps);
        }
      } catch (IOException e) {
        isProps = null;
      }
      if (SX.isNull(isProps)) {
        System.out.println(String.format("SX.Log: getTranslation: missing ressource: %s", resProp));
        currentProps = null;
      }
      translateProps.put(clazz, currentProps);
    }
    msgPlus = getTranslationGlobal(msgPlus);
    String tKey = "";
    String msgToTranslate = "";
    if (!SX.isNull(currentProps = translateProps.get(clazz))) {
      tKey = clazz;
      String method = "no_method:";
      String phrase = "";
      String[] parts = null;
      if (msg.contains(":")) {
        parts = msg.split(":");
        method = parts[0];
        tKey += "_" + method;
        msg = msg.substring(method.length() + 1).trim();
        msgToTranslate = msg;
      }
      if (msg.contains(" ")) {
        parts = msg.split(" ");
        phrase = parts[0].replaceAll(":", "");
        if (phrase.contains("#")) {
          phrase = phrase.split("#")[1];
        }
      } else {
        phrase = msg;
      }
      tKey = tKey + "_" + phrase.toLowerCase();
      tKey = tKey.replaceAll("%", "#");
      String trans = currentProps.getProperty(tKey);
      if (!SX.isNull(trans)) {
        return method + ": " + trans;
      }
    }
    String transError = "";
    if (clazz.startsWith("sx") && currentLevel > DEBUG) {
      transError = "*** " + String.format("%s (%s = %s)", getTranslationGlobal("translation"),
              tKey, msgToTranslate.replaceAll("%", "#")) + ": ";
    }
    return transError + (SX.isSet(msgPlus) ? "*** " + msgPlus + ": " : "") + orgMsg;
  }

  private String getTranslationGlobal(String msg) {
    if (!globalTranslation) {
      return msg;
    }
    if (SX.isSet(msg)) {
      Properties props = translateProps.get("SXGlobal");
      String transMsgPlus = msg;
      if (!SX.isNull(props)) {
        transMsgPlus = props.getProperty("SXGlobal_" + msg.toLowerCase().split(" ")[0]);
      }
      msg = transMsgPlus;
    }
    return msg;
  }
  //</editor-fold>
}

