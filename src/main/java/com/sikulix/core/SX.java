/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.core;

//import com.sikulix.scripting.JythonHelper;

import org.apache.commons.cli.*;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.CodeSource;
import java.util.*;
import java.util.List;

import static com.sikulix.core.SX.NATIVES.HOTKEY;
import static com.sikulix.core.SX.NATIVES.OPENCV;
import static com.sikulix.core.SX.NATIVES.SYSUTIL;

public class SX {

  private static long startTime = new Date().getTime();

  //<editor-fold desc="00*** logging">
  public static final int INFO = 1;
  public static final int DEBUG = 3;
  public static final int TRACE = 4;
  public static final int ERROR = -1;
  public static final int FATAL = -2;

  private static final SXLog log = new SXLog();

  private static void info(String message, Object... args) {
    log.info(message, args);
  }

  public static void debug(String message, Object... args) {
    log.debug(message, args);
  }

  public static void trace(String message, Object... args) {
    log.trace(message, args);
  }

  public static void error(String message, Object... args) {
    log.error(message, args);
  }

  public static void terminate(int retval, String message, Object... args) {
    if (retval != 0) {
      log.fatal(message, args);
    } else {
      info(message, args);
    }
    System.exit(retval);
  }

  public static void p(String msg, Object... args) {
    log.p(msg, args);
  }

  public static SXLog getSXLog(String className) {
    return getSXLog(className, null, -1);
  }

  public static SXLog getSXLog(String className, int level) {
    return getSXLog(className, null, level);
  }

  public static SXLog getSXLog(String className, String[] args) {
    return getSXLog(className, args, -1);
  }

  public static SXLog getSXLog(String className, String[] args, int level) {
    return new SXLog(className, args, level);
  }
  //</editor-fold>

  //<editor-fold desc="01*** init">
  private static String sxInstance = null;

  private static boolean shouldLock = false;
  private static FileOutputStream isRunningFile = null;
  static final Class sxGlobalClassReference = SX.class;

  static void sxinit(String[] args) {
    if (null == sxInstance) {
      sxInstance = "SX INIT DONE";

      //<editor-fold desc="*** shutdown hook">
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          if (shouldLock && isSet(isRunningFile)) {
            try {
              isRunningFile.close();
            } catch (IOException ex) {
            }
          }
          for (File f : new File(getSXSYSTEMP()).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
              File aFile = new File(dir, name);
              boolean isObsolete = false;
              long lastTime = aFile.lastModified();
              if (lastTime == 0) {
                return false;
              }
              if (lastTime < ((new Date().getTime()) - 7 * 24 * 60 * 60 * 1000)) {
                isObsolete = true;
              }
              if (name.contains("BridJExtractedLibraries") && isObsolete) {
                return true;
              }
              if (name.toLowerCase().contains("sikuli")) {
                if (name.contains("Sikulix_")) {
                  if (isObsolete || aFile.equals(new File(getSXTEMP()))) {
                    return true;
                  }
                } else {
                  return true;
                }
              }
              return false;
            }
          })) {
            trace("cleanTemp: " + f.getName());
            Content.deleteFileOrFolder("#" + f.getAbsolutePath());
          }
        }
      });
      //</editor-fold>

      // TODO Content class must be initialized for lock in shutdown
      Content.start();

      //<editor-fold desc="*** sx lock (not active)">
      if (shouldLock) {
        File fLock = new File(getSXSYSTEMP(), "SikuliX2-i-s-r-u-n-n-i-n-g");
        String shouldTerminate = "";
        try {
          fLock.createNewFile();
          isRunningFile = new FileOutputStream(fLock);
          if (isNull(isRunningFile.getChannel().tryLock())) {
            shouldTerminate = "SikuliX2 already running";
            isRunningFile = null;
          }
        } catch (Exception ex) {
          shouldTerminate = "cannot access SX2 lock: " + ex.toString();
          isRunningFile = null;
        }
        if (isSet(shouldTerminate)) {
          terminate(1, shouldTerminate);
        }
      }
      //</editor-fold>

      // *** command line args
      if (!isNull(args)) {
        checkArgs(args);
      }

      trace("!sxinit: entry");

      // *** get SX options
      loadOptions();

      // *** get the version info
      getSXVERSION();

      // *** check how we are running
      APPTYPE = "from a jar";
      String base = whereIs(sxGlobalClassReference);
      if (isSet(base)) {
        SXBASEJAR = base;
        File jarBase = new File(base);
        String jarBaseName = jarBase.getName();
        File fJarBase = jarBase.getParentFile();
        trace("sxRunningAs: runs as %s in: %s", jarBaseName, fJarBase.getAbsolutePath());
        if (jarBaseName.contains("classes")) {
          SXPROJEKTf = fJarBase.getParentFile().getParentFile();
          trace("sxRunningAs: not jar - supposing Maven project: %s", SXPROJEKTf);
          APPTYPE = "in Maven project from classes";
        } else if ("target".equals(fJarBase.getName())) {
          SXPROJEKTf = fJarBase.getParentFile().getParentFile();
          trace("sxRunningAs: folder target detected - supposing Maven project: %s", SXPROJEKTf);
          APPTYPE = "in Maven project from some jar";
        } else {
          if (isWindows()) {
            if (jarBaseName.endsWith(".exe")) {
              setSXRUNNINGASAPP(true);
              APPTYPE = "as application .exe";
            }
          } else if (isMac()) {
            if (fJarBase.getAbsolutePath().contains("SikuliX.app/Content")) {
              setSXRUNNINGASAPP(true);
              APPTYPE = "as application .app";
              if (!fJarBase.getAbsolutePath().startsWith("/Applications")) {
                APPTYPE += " (not from /Applications folder)";
              }
            }
          }
        }
      } else {
        terminate(1, "sxRunningAs: no valid Java context for SikuliX available "
                + "(java.security.CodeSource.getLocation() is null)");
      }

      //TODO i18n SXGlobal_sxinit_complete=complete %.3f
      trace("!sxinit: exit %.3f (%s)", (new Date().getTime() - startTime) / 1000.0f, APPTYPE);
    }
  }
  //</editor-fold>

  //<editor-fold desc="02*** command line args">
  private static List<String> sxArgs = new ArrayList<String>();
  private static List<String> userArgs = new ArrayList<String>();
  private static CommandLine sxCommandArgs = null;

  static void checkArgs(String[] args) {
    boolean hasUserArgs = false;
    for (String arg : args) {
      if ("--".equals(arg)) {
        hasUserArgs = true;
        continue;
      }
      if (hasUserArgs) {
        trace("checkargs: user: %s", arg);
        userArgs.add(arg);
      } else {
        trace("checkargs: --sx: %s", arg);
        sxArgs.add(arg);
      }
    }
    if (sxArgs.size() > 0) {
      CommandLineParser parser = new PosixParser();
      Options opts = new Options();
      opts.addOption(OptionBuilder.hasOptionalArg().create('d'));
      opts.addOption(OptionBuilder.hasArg().create('o'));
      opts.addOption(OptionBuilder.hasArgs().create('r'));
      opts.addOption(OptionBuilder.hasArgs().create('t'));
      opts.addOption(OptionBuilder.hasArg(false).create('c'));
      opts.addOption(OptionBuilder.hasArg(false).create('q'));
      try {
        sxCommandArgs = parser.parse(opts, sxArgs.toArray(new String[0]));
      } catch (ParseException e) {
        terminate(1, "checkArgs: %s", e.getMessage());
      }
      if (!isNull(sxCommandArgs)) {
        if (isArg("q")) {
          log.globalStop();
        } else if (isArg("d")) {
          log.globalOn(log.DEBUG);
        }
      }
    }
    //TODO make options from SX args
  }

  private static boolean isArg(String arg) {
    return sxCommandArgs != null && sxCommandArgs.hasOption(arg);
  }

  private static String getArg(String arg) {
    if (sxCommandArgs != null && sxCommandArgs.hasOption(arg)) {
      String val = sxCommandArgs.getOptionValue(arg);
      return val == null ? "" : val;
    }
    return null;
  }

  public static String[] getUserArgs() {
    return userArgs.toArray(new String[0]);
  }
  //</editor-fold>

  //<editor-fold desc="03*** check how we are running">
  private static String APPTYPE = "?APPTYPE?";

  private static String SXBASEJAR = null;
  private static File SXPROJEKTf;

  private static String SXJYTHONMAVEN;
  private static String SXJYTHONLOCAL;

  private static String SXJRUBYMAVEN;
  private static String SXJRUBYLOCAL;

  private static Map<String, String> SXTESSDATAS = new HashMap<String, String>();

  private static String SXMAVEN = "https://repo1.maven.org/maven2/";
  private static String SXOSSRH = "https://oss.sonatype.org/content/groups/public/";

  private static String whereIs(Class clazz) {
    CodeSource codeSrc = clazz.getProtectionDomain().getCodeSource();
    String base = null;
    if (codeSrc != null && codeSrc.getLocation() != null) {
      base = Content.slashify(codeSrc.getLocation().getPath(), false);
    }
    return base;
  }

  private static String BASECLASS = "";

  public static void setSXBASECLASS() {
    log.trace("setBaseClass: start");
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    boolean takeit = false;
    for (StackTraceElement traceElement : stackTrace) {
      String tName = traceElement.getClassName();
      if (takeit) {
        BASECLASS = tName;
        break;
      }
      if (tName.equals(SX.class.getName())) {
        takeit = true;
      }
    }
  }

  public static String getSXBASECLASS() {
    return BASECLASS;
  }
  //</editor-fold>

  //<editor-fold desc="04*** get SX options at startup">
  private static File fOptions = null;
  private static String fnOptions = "sxoptions.txt";

  private static PropertiesConfiguration SXOPTIONS = null;

  private static void loadOptions() {
    boolean success = true;
    URL urlOptions = SX.class.getClassLoader().getResource("Settings/sxoptions.txt");
    if (!isNull(urlOptions)) {
      Configurations configs = new Configurations();
      try {
        SXOPTIONS = configs.properties(urlOptions);
      } catch (ConfigurationException cex) {
        success = false;
      }
    } else {
      success = false;
    }
    if (!success) {
      terminate(1, "loadOptions: SX Options not available: %s", urlOptions);
    }

    PropertiesConfiguration extraOptions = null;

    File aFile = null;
    String argFile = getArg("o");
    if (!isNull(argFile)) {
      aFile = Content.asFile(argFile);
      if (!aFile.isDirectory()) {
        if (aFile.exists()) {
          fOptions = aFile;
          trace("loadOptions: arg: %s (from arg -o)", aFile);
        } else {
          fnOptions = aFile.getName();
          trace("loadOptions: file name given: %s (from arg -o)", fnOptions);
        }
      }
    }

    if (isNull(fOptions)) {
      for (String sFile : new String[]{getSXUSERWORK(), getSXUSERHOME(), getSXSTORE()}) {
        if (isNull(sFile)) {
          continue;
        }
        aFile = Content.asFile(sFile);
        trace("loadOptions: check: %s", aFile);
        fOptions = new File(aFile, fnOptions);
        if (fOptions.exists()) {
          break;
        } else {
          fOptions = null;
        }
      }
    }
    if (fOptions != null) {
      trace("loadOptions: found Options file at: %s", fOptions);
      Configurations configs = new Configurations();
      try {
        extraOptions = configs.properties(fOptions);
      } catch (ConfigurationException cex) {
        error("loadOptions: Options not valid: %s", cex.getMessage());
      }
      if (!isNull(extraOptions)) {
        mergeExtraOptions(SXOPTIONS, extraOptions);
      }
    } else {
      trace("loadOptions: no extra Options file found");
    }
  }


  private static void mergeExtraOptions(PropertiesConfiguration baseOptions, PropertiesConfiguration extraOptions) {
    if (isNull(extraOptions) || extraOptions.size() == 0) {
      return;
    }
    trace("loadOptions: have to merge extra Options");
    Iterator<String> allKeys = extraOptions.getKeys();
    while (allKeys.hasNext()) {
      String key = allKeys.next();
      if ("sxversion".equals(key)) {
        baseOptions.setProperty("sxversion_saved", extraOptions.getProperty(key));
        continue;
      }
      if ("sxbuild".equals(key)) {
        baseOptions.setProperty("sxbuild_saved", extraOptions.getProperty(key));
        continue;
      }
      Object value = baseOptions.getProperty(key);
      if (isNull(value)) {
        baseOptions.addProperty(key, extraOptions.getProperty(key));
        trace("Option added: %s", key);
      } else {
        Object extraValue = extraOptions.getProperty(key);
        if (!value.getClass().getName().equals(extraValue.getClass().getName()) ||
                !value.toString().equals(extraValue.toString())) {
          baseOptions.setProperty(key, extraValue);
          trace("Option changed: %s = %s", key, extraValue);
        }
      }
    }
  }

//</editor-fold>

  //<editor-fold desc="05*** handle options at runtime">
  public static void loadOptions(String fpOptions) {
    error("loadOptions: not yet implemented");
  }

  public static boolean saveOptions(String fpOptions) {
    error("saveOptions: not yet implemented");
    return false;
  }

  public static boolean saveOptions() {
    try {
      SXOPTIONS.write(new FileWriter(Content.asFile(SX.getSXSTORE(), "sxoptions.txt")));
    } catch (Exception e) {
      log.error("saveOptions: %s", e);
    }
    return false;
  }

  public static boolean hasOptions() {
    return SXOPTIONS != null && SXOPTIONS.size() > 0;
  }

  public static boolean isOption(String pName) {
    return isOption(pName, false);
  }

  public static boolean isOption(String pName, Boolean bDefault) {
    if (SXOPTIONS == null) {
      return bDefault;
    }
    String pVal = SXOPTIONS.getString(pName, bDefault.toString()).toLowerCase();
    if (pVal.contains("yes") || pVal.contains("true") || pVal.contains("on")) {
      return true;
    }
    return false;
  }

  public static String getOption(String pName) {
    return getOption(pName, "");
  }

  public static String getOption(String pName, String sDefault) {
    if (!hasOptions()) {
      return "";
    }
    return SXOPTIONS.getString(pName, sDefault);
  }

  public static void setOption(String pName, String sValue) {
    SXOPTIONS.setProperty(pName, sValue);
  }

  public static double getOptionNumber(String pName) {
    return getOptionNumber(pName, 0);
  }

  public static double getOptionNumber(String pName, double nDefault) {
    double nVal = SXOPTIONS.getDouble(pName, nDefault);
    return nVal;
  }

  public static Map<String, String> getOptions() {
    Map<String, String> mapOptions = new HashMap<String, String>();
    if (hasOptions()) {
      Iterator<String> allKeys = SXOPTIONS.getKeys();
      while (allKeys.hasNext()) {
        String key = allKeys.next();
        mapOptions.put(key, getOption(key));
      }
    }
    return mapOptions;
  }

  public static void dumpOptions() {
    if (hasOptions()) {
      p("*** options dump");
      for (String sOpt : getOptions().keySet()) {
        p("%s = %s", sOpt, getOption(sOpt));
      }
      p("*** options dump end");
    }
  }
  //</editor-fold>

  //<editor-fold desc="06*** system/java version info">

  /**
   * @return path seperator : or ;
   */
  public static String getSeparator() {
    if (isWindows()) {
      return ";";
    }
    return ":";
  }

  static enum theSystem {
    WIN, MAC, LUX, FOO
  }

  /**
   * ***** Property SXSYSTEM *****
   *
   * @return info about the system running on
   */

  public static String getSXSYSTEM() {
    if (isNotSet(SYSTEM)) {
      String osName = System.getProperty("os.name");
      String osVersion = System.getProperty("os.version");
      if (osName.toLowerCase().startsWith("windows")) {
        SYS = theSystem.WIN;
        osName = "Windows";
      } else if (osName.toLowerCase().startsWith("mac")) {
        SYS = theSystem.MAC;
        osName = "Mac OSX";
      } else if (osName.toLowerCase().startsWith("linux")) {
        SYS = theSystem.LUX;
        osName = "Linux";
      } else {
        terminate(-1, "running on not supported System: %s (%s)", osName, osVersion);
      }
      SYSTEMVERSION = osVersion;
      SYSTEM = String.format("%s (%s)", osName, SYSTEMVERSION);
    }
    return SYSTEM;
  }

  static String SYSTEM = "";
  static theSystem SYS = theSystem.FOO;

  private static String getSYSGENERIC() {
    getSXSYSTEM();
    if (isWindows()) {
      return "windows";
    }
    if (isMac()) {
      return "mac";
    }
    if (isLinux()) {
      return "linux";
    }
    return "unknown";
  }


  /**
   * ***** Property SXSYSTEMVERSION *****
   *
   * @return the running system's version info
   */
  public static String getSXSYSTEMVERSION() {
    if (isNotSet(SYSTEMVERSION)) {
      getSXSYSTEM();
    }
    return SYSTEMVERSION;
  }

  static String SYSTEMVERSION = "";

  /**
   * @return true/false
   */
  public static boolean isWindows() {
    getSXSYSTEM();
    return theSystem.WIN.equals(SYS);
  }

  /**
   * @return true/false
   */
  public static boolean isLinux() {
    getSXSYSTEM();
    return theSystem.LUX.equals(SYS);
  }

  /**
   * @return true/false
   */
  public static boolean isMac() {
    getSXSYSTEM();
    return theSystem.MAC.equals(SYS);
  }

  public static boolean isOSX10() {
    return getSXSYSTEMVERSION().startsWith("10.10.") || getSXSYSTEMVERSION().startsWith("10.11.");
  }

  /**
   * ***** Property RUNNINGASAPP *****
   *
   * @return to know wether running as .exe/.app
   */
  public static boolean isSXRUNNINGASAPP() {
    if (isNotSet(RUNNINGASAPP)) {
      //TODO getASAPP detect running as .exe/.app
      setSXRUNNINGASAPP(false);
    }
    return RUNNINGASAPP;
  }

  static Boolean RUNNINGASAPP = null;

  public static boolean setSXRUNNINGASAPP(boolean val) {
    RUNNINGASAPP = val;
    return RUNNINGASAPP;
  }


  /**
   * ***** Property JAVAHOME *****
   *
   * @return the Java installation path
   */
  public static String getSXJAVAHOME() {
    if (isNotSet(JAVAHOME)) {
      String jhome = System.getProperty("java.home");
      if (isSet(jhome)) {
        JAVAHOME = jhome;
      }
    }
    return JAVAHOME;
  }

  static String JAVAHOME = "";

  /**
   * ***** Property JAVAVERSION *****
   *
   * @return Java version info
   */
  public static String getSXJAVAVERSION() {
    if (isNotSet(JAVAVERSION)) {
      String vJava = System.getProperty("java.runtime.version");
      String vVM = System.getProperty("java.vm.version");
      String vClass = System.getProperty("java.class.version");
      String vSysArch = System.getProperty("os.arch");
      int javaVersion = 0;
      if (vSysArch == null || !vSysArch.contains("64")) {
        terminate(1, "Java arch not 64-Bit or not detected: JavaSystemProperty::os.arch = %s", vSysArch);
      }
      try {
        javaVersion = Integer.parseInt(vJava.substring(2, 3));
        JAVAVERSION = String.format("Java %s vm %s class %s arch %s", vJava, vVM, vClass, vSysArch);
      } catch (Exception ex) {
        terminate(1, "Java version not detected: JavaSystemProperty::java.runtime.version = %s", vJava);
      }
      if (javaVersion < 7 || javaVersion > 8) {
        terminate(1, "Java version must be 7 or 8");
      }
    }
    return JAVAVERSION;
  }

  static String JAVAVERSION = "";

  /**
   * ***** Property JAVAVERSIONNUMBER *****
   *
   * @return Java version number
   */
  public static int getSXJAVAVERSIONNUMBER() {
    if (isNotSet(JAVAVERSIONNUMBER)) {
      JAVAVERSIONNUMBER = Integer.parseInt(getSXJAVAVERSION().substring(5, 6));
    }
    return JAVAVERSIONNUMBER;
  }

  static Integer JAVAVERSIONNUMBER = null;

  public static boolean isJava8() {
    return getSXJAVAVERSIONNUMBER() > 7;
  }

  public static boolean isJava7() {
    return getSXJAVAVERSIONNUMBER() > 6;
  }
  //</editor-fold>

  //<editor-fold desc="07*** temp folders">

  /**
   * ***** Property SYSTEMP *****
   *
   * @return the path for temporary stuff according to JavaSystemProperty::java.io.tmpdir
   */
  public static String getSXSYSTEMP() {
    if (isNotSet(SYSTEMP)) {
      String tmpdir = System.getProperty("java.io.tmpdir");
      if (tmpdir == null || tmpdir.isEmpty() || !Content.asFile(tmpdir).exists()) {
        terminate(1, "JavaSystemProperty::java.io.tmpdir not valid");
      }
      SYSTEMP = Content.asFile(tmpdir).getAbsolutePath();
    }
    return SYSTEMP;
  }

  static String SYSTEMP = "";

  /**
   * ***** Property TEMP *****
   *
   * @return the path to the area where Sikulix stores temporary stuff (located in SYSTEMP)
   */
  public static String getSXTEMP() {
    if (isNotSet(TEMP)) {
      File fSXTempPath = Content.asFile(getSXSYSTEMP(), String.format("Sikulix_%d", getRandomInt()));
      for (String aFile : Content.asFile(SYSTEMP).list()) {
        if ((aFile.startsWith("Sikulix") && (new File(aFile).isFile()))
                || (aFile.startsWith("jffi") && aFile.endsWith(".tmp"))) {
          Content.deleteFileOrFolder(new File(getSXSYSTEMP(), aFile));
        }
      }
      fSXTempPath.mkdirs();
      if (!fSXTempPath.exists()) {
        terminate(1, "getTEMP: could not create: %s", fSXTempPath.getAbsolutePath());
      }
      TEMP = fSXTempPath.getAbsolutePath();
    }
    return TEMP;
  }

  static String TEMP = "";

  /**
   * @return a positive random int > 0 using Java's Random().nextInt()
   */
  static int getRandomInt() {
    int rand = 1 + new Random().nextInt();
    return (rand < 0 ? rand * -1 : rand);
  }
  //</editor-fold>

  //<editor-fold desc="08*** user/work/appdata folder">

  /**
   * ***** Property USERHOME *****
   *
   * @return the system specific User's home folder
   */
  public static String getSXUSERHOME() {
    if (isNotSet(USERHOME)) {
      String aFolder = System.getProperty("user.home");
      if (aFolder == null || aFolder.isEmpty() || !Content.asFile(aFolder).exists()) {
        terminate(-1, "getUSERHOME: JavaSystemProperty::user.home not valid");
      }
      USERHOME = Content.asFile(aFolder).getAbsolutePath();
    }
    return USERHOME;
  }

  static String USERHOME = "";

  /**
   * ***** Property USERWORK *****
   *
   * @return the working folder from JavaSystemProperty::user.dir
   */
  public static String getSXUSERWORK() {
    if (isNotSet(USERWORK)) {
      String aFolder = System.getProperty("user.dir");
      if (aFolder == null || aFolder.isEmpty() || !new File(aFolder).exists()) {
        terminate(-1, "getUSERWORK: JavaSystemProperty::user.dir not valid");
      }
      USERWORK = Content.asFolder(aFolder).getAbsolutePath();
    }
    return USERWORK;
  }

  static String USERWORK = "";


  /**
   * ***** Property SYSAPPDATA *****
   *
   * @return the system specific path to the users application storage area
   */
  public static String getSXSYSAPPDATA() {
    if (isNotSet(SYSAPPDATA)) {
      String appDataMsg = "";
      File fSysAppPath = null;
      if (isWindows()) {
        String sDir = System.getenv("APPDATA");
        if (sDir == null || sDir.isEmpty()) {
          terminate(1, "setSYSAPP: Windows: %s not valid", "%APPDATA%");
        }
        fSysAppPath = Content.asFile(sDir);
      } else if (isMac()) {
        fSysAppPath = Content.asFile(getSXUSERHOME(), "Library/Application Support");
      } else if (isLinux()) {
        fSysAppPath = Content.asFile(getSXUSERHOME());
        SXAPPdefault = ".Sikulix/SX2";
      }
      SYSAPPDATA = fSysAppPath.getAbsolutePath();
    }
    return SYSAPPDATA;
  }

  static String SYSAPPDATA = "";
  //</editor-fold>

  //<editor-fold desc="09*** SX app data folder">
  public static String getSXWEBHOME() {
    if (isNotSet(SXWEBHOME)) {
      SXWEBHOME = SXWEBHOMEdefault;
    }
    return SXWEBHOME;
  }

  static String SXWEBHOME = "";
  static String SXWEBHOMEdefault = "http://sikulix.com";

  public static String getSXWEBDOWNLOAD() {
    if (isNotSet(SXWEBDOWNLOAD)) {
      SXWEBDOWNLOAD = SXWEBDOWNLOADdefault;
    }
    return SXWEBDOWNLOAD;
  }

  static String SXWEBDOWNLOAD = "";
  static String SXWEBDOWNLOADdefault = "http://download.sikulix.com";

  /**
   * ***** Property SXAPPSTORE *****
   *
   * @return the path to the area in SYSAPPDATA where Sikulix stores all stuff
   */
  public static String getSXAPP() {
    if (isNotSet(SXAPPSTORE)) {
      File fDir = Content.asFile(getSXSYSAPPDATA(), SXAPPdefault);
      fDir.mkdirs();
      if (!fDir.exists()) {
        terminate(1, "setSXAPP: folder not available or cannot be created: %s", fDir);
      }
      SXAPPSTORE = fDir.getAbsolutePath();
    }
    return SXAPPSTORE;
  }

  static String SXAPPSTORE = "";
  static String SXAPPdefault = "Sikulix/SX2";

  /**
   * ***** Property SXDOWNLOADS *****
   *
   * @return path where Sikulix stores downloaded stuff
   */
  public static String getSXDOWNLOADS() {
    if (isNotSet(SXDOWNLOADS)) {
      String fBase = getSXAPP();
      File fDir = Content.asFile(fBase, SXDOWNLOADSdefault);
      setSXDOWNLOADS(fDir);
    }
    return SXDOWNLOADS;
  }

  static String SXDOWNLOADS = "";
  static String SXDOWNLOADSdefault = "Downloads";

  public static String setSXDOWNLOADS(Object oDir) {
    File fDir = Content.asFile(oDir, null);
    if (isSet(fDir)) {
      fDir.mkdirs();
    }
    if (isNotSet(fDir) || !Content.existsFile(fDir) || !fDir.isDirectory()) {
      terminate(1, "setSXDOWNLOADS: not posssible or not valid: %s", fDir);
    }
    SXDOWNLOADS = fDir.getAbsolutePath();
    return SXDOWNLOADS;
  }

  /**
   * ***** Property SXNATIVE *****
   *
   * @return path where Sikulix stores the native stuff
   */
  public static String getSXNATIVE() {
    if (isNotSet(SXNATIVE)) {
      String fBase = getSXAPP();
      File fDir = Content.asFolder(fBase, SXNATIVEdefault);
      setSXNATIVE(fDir);
    }
    return SXNATIVE;
  }

  static String SXNATIVE = "";
  static String SXNATIVEdefault = "Native";

  public static String setSXNATIVE(Object oDir) {
    File fDir = Content.asFolder(oDir);
    if (isNotSet(fDir) || !Content.existsFile(fDir) || !fDir.isDirectory()) {
      terminate(1, "setSXNATIVE: not posssible or not valid: %s", fDir);
    }
    SXNATIVE = fDir.getAbsolutePath();
    return SXNATIVE;
  }

  /**
   * ***** Property SXLIB *****
   *
   * @return path to folder containing complementary stuff for scripting languages
   */
  public static String getSXLIB() {
    if (isNotSet(SXLIB)) {
      String fBase = getSXAPP();
      File fDir = Content.asFile(fBase, SXLIBdefault);
      setSXLIB(fDir);
    }
    return SXLIB;
  }

  static String SXLIB = "";
  static String SXLIBdefault = "LIB";

  public static String setSXLIB(Object oDir) {
    File fDir = Content.asFile(oDir, null);
    if (isSet(fDir)) {
      fDir.mkdirs();
    }
    if (isNotSet(fDir) || !Content.existsFile(fDir) || !fDir.isDirectory()) {
      terminate(1, "setSXLIB: not posssible or not valid: %s", fDir);
    }
    SXLIB = fDir.getAbsolutePath();
    return SXLIB;
  }

  /**
   * ***** Property SXSTORE *****
   *
   * @return path where other stuff is found or stored at runtime (options, logs, ...)
   */
  public static String getSXSTORE() {
    if (isNotSet(SXSTORE)) {
      String fBase = getSXAPP();
      File fDir = Content.asFile(fBase, SXSTOREdefault);
      setSXSTORE(fDir);
    }
    return SXSTORE;
  }

  static String SXSTORE = "";
  static String SXSTOREdefault = "Store";

  public static String setSXSTORE(Object oDir) {
    File fDir = Content.asFolder(oDir);
    if (isNotSet(fDir) || !Content.existsFile(fDir) || !fDir.isDirectory()) {
      terminate(1, "setSXSTORE: not posssible or not valid: %s", fDir);
    }
    SXSTORE = fDir.getAbsolutePath();
    return SXSTORE;
  }

  /**
   * ***** Property SXEDITOR *****
   *
   * @return path to folder containing supporting stuff for Sikulix IDE
   */
  public static String getSXEDITOR() {
    if (isNotSet(SXEDITOR)) {
      String fBase = getSXAPP();
      File fDir = Content.asFolder(fBase, SXEDITORdefault);
      setSXEDITOR(fDir);
    }
    return SXEDITOR;
  }

  static String SXEDITOR = "";
  static String SXEDITORdefault = "Extensions/SXEditor";

  public static String setSXEDITOR(Object oDir) {
    File fDir = Content.asFile(oDir);
    if (isSet(fDir)) {
      fDir.mkdirs();
    }
    if (isNotSet(fDir) || !Content.existsFile(fDir) || !fDir.isDirectory()) {
      terminate(1, "setSXEDITOR: not posssible or not valid: %s", fDir);
    }
    SXEDITOR = fDir.getAbsolutePath();
    return SXEDITOR;
  }

  /**
   * ***** Property SXTESSERACT *****
   *
   * @return path to folder for stuff supporting Tesseract
   */
  public static String getSXTESSERACT() {
    if (isNotSet(SXTESSERACT)) {
      String fBase = getSXAPP();
      File fDir = Content.asFile(fBase, SXTESSERACTdefault);
      setSXTESSERACT(fDir);
    }
    return SXTESSERACT;
  }

  static String SXTESSERACT = "";
  static String SXTESSERACTdefault = "TESSERACT";

  public static String setSXTESSERACT(Object oDir) {
    File fDir = Content.asFile(oDir, null);
    if (isSet(fDir)) {
      fDir.mkdirs();
    }
    if (isNotSet(fDir) || !Content.existsFile(fDir) || !fDir.isDirectory()) {
      terminate(1, "setSXTESSERACT: not posssible or not valid: %s", fDir);
    }
    SXTESSERACT = fDir.getAbsolutePath();
    return SXTESSERACT;
  }

  /**
   * ***** Property EXTENSIONSFOLDER *****
   *
   * @return path to folder containg extensions or plugins
   */
  public static String getSXEXTENSIONSFOLDER() {
    if (isNotSet(EXTENSIONSFOLDER)) {
      String fBase = getSXAPP();
      File fDir = Content.asFile(fBase, EXTENSIONSdefault);
      setSXEXTENSIONS(fDir);
    }
    return EXTENSIONSFOLDER;
  }

  static String EXTENSIONSFOLDER = "";
  static String EXTENSIONSdefault = "Extensions";

  static String[] theExtensions = new String[]{"selenium4sikulix"};

  public static String setSXEXTENSIONS(Object oDir) {
    File fDir = Content.asFile(oDir, null);
    if (isSet(fDir)) {
      fDir.mkdirs();
    }
    if (isNotSet(fDir) || !Content.existsFile(fDir) || !fDir.isDirectory()) {
      terminate(1, "setSXEXTENSIONS: not posssible or not valid: %s", fDir);
    }
    EXTENSIONSFOLDER = fDir.getAbsolutePath();
    return EXTENSIONSFOLDER;
  }

  /**
   * ***** Property SXIMAGES *****
   *
   * @return
   */
  public static String getSXIMAGES() {
    if (isNotSet(SXIMAGES)) {
      String fBase = getSXAPP();
      File fDir = Content.asFolder(fBase, SXIMAGESdefault);
      setSXIMAGES(fDir);
    }
    return SXIMAGES;
  }

  static String SXIMAGES = "";
  static String SXIMAGESdefault = "Images";

  public static String setSXIMAGES(Object oDir) {
    File fDir = Content.asFile(oDir, null);
    if (isSet(fDir)) {
      fDir.mkdirs();
    }
    if (isNotSet(fDir) || !Content.existsFile(fDir) || !fDir.isDirectory()) {
      terminate(1, "setSXIMAGES: not posssible or not valid: %s", fDir);
    }
    SXIMAGES = fDir.getAbsolutePath();
    return SXIMAGES;
  }
  //</editor-fold>

  //<editor-fold desc="10*** SX version info">

  /**
   * ***** Property VERSION *****
   *
   * @return Sikulix version
   */
  public static String getSXVERSION() {
    if (isNotSet(VERSION)) {
      String sxVersion = "?sxVersion?";
      String sxBuild = "?sxBuild?";
      String sxVersionShow = "?sxVersionShow?";
      String sxStamp = "?sxStamp?";
      sxVersion = SXOPTIONS.getString("sxversion");
      sxBuild = SXOPTIONS.getString("sxbuild");
      sxBuild = sxBuild.replaceAll("\\-", "");
      sxBuild = sxBuild.replaceAll("_", "");
      sxBuild = sxBuild.replaceAll("\\:", "");
      String sxlocalrepo = Content.slashify(SXOPTIONS.getString("sxlocalrepo"), true);
      String sxJythonVersion = SXOPTIONS.getString("sxjython");
      String sxJRubyVersion = SXOPTIONS.getString("sxjruby");

      debug("getVERSION: version: %s build: %s", sxVersion, sxBuild);
      sxStamp = String.format("%s_%s", sxVersion, sxBuild);

      // used for download of production versions
      String dlProdLink = "https://launchpad.net/raiman/sikulix2013+/";
      String dlProdLinkSuffix = "/+download/";
      // used for download of development versions (nightly builds)
      String dlDevLink = "http://nightly.sikuli.de/";

      SXJYTHONMAVEN = "org/python/jython-standalone/"
              + sxJythonVersion + "/jython-standalone-" + sxJythonVersion + ".jar";
      SXJYTHONLOCAL = sxlocalrepo + SXJYTHONMAVEN;
      SXJRUBYMAVEN = "org/jruby/jruby-complete/"
              + sxJRubyVersion + "/jruby-complete-" + sxJRubyVersion + ".jar";
      SXJRUBYLOCAL = sxlocalrepo + SXJRUBYMAVEN;
      SXTESSDATAS.put("eng", "http://download.sikulix.com/tesseract-ocr-3.02.eng.tar.gz");

      sxLibsCheckName = String.format(sxLibsCheckStamp, sxStamp);
      VERSION = sxVersion;
      BUILD = sxBuild;
      VERSIONSHOW = String.format("%s (%s)", sxVersion, sxBuild);
      STAMP = sxStamp;
    }
    return VERSION;
  }

  static String VERSION = "";

  /**
   * ***** Property BUILD *****
   *
   * @return Sikulix build timestamp
   */
  public static String getSXBUILD() {
    if (isNotSet(BUILD)) {
      getSXVERSION();
    }
    return BUILD;
  }

  static String BUILD = "";

  /**
   * ***** Property VERSIONSHOW *****
   *
   * @return Version (Build)
   */
  public static String getSXVERSIONSHOW() {
    if (isNotSet(VERSIONSHOW)) {
      getSXVERSION();
    }
    return VERSIONSHOW;
  }

  static String VERSIONSHOW = "";

  /**
   * ***** Property STAMP *****
   *
   * @return Version_Build
   */
  public static String getSXSTAMP() {
    if (isNotSet(STAMP)) {
      getSXVERSION();
    }
    return STAMP;
  }

  static String STAMP = "";

  public static boolean isSnapshot() {
    return getSXVERSION().endsWith("-SNAPSHOT");
  }
  //</editor-fold>

  //<editor-fold desc="11*** monitor / local device">

  /**
   * checks, whether Java runs with a valid GraphicsEnvironment (usually means real screens connected)
   *
   * @return false if Java thinks it has access to screen(s), true otherwise
   */
  public static boolean isHeadless() {
    return GraphicsEnvironment.isHeadless();
  }

  public static boolean isTravisCI() {
    return SX.isSet(System.getenv("TRAVIS"), "true");
  }

  /**
   * ***** Property LOCALDEVICE *****
   *
   * @return
   */
  public static LocalDevice getSXLOCALDEVICE() {
    if (isNotSet(LOCALDEVICE)) {
      LOCALDEVICE = (LocalDevice) new LocalDevice().start();
    }
    return LOCALDEVICE;
  }

  public static boolean isSetSXLOCALDEVICE() {
    return SX.isNotNull(LOCALDEVICE);
  }

  public static void setSXLOCALDEVICE(LocalDevice LOCALDEVICE) {
    SX.LOCALDEVICE = LOCALDEVICE;
  }

  private static LocalDevice LOCALDEVICE = null;
  //</editor-fold>

  //<editor-fold desc="12*** handle native libs">
  public static File fLibsProvided;
  public static boolean useLibsProvided;
  public static String linuxNeededLibs = "";
  public static String linuxAppSupport = "";
  static boolean areLibsExported = false;
  static String fpJarLibs = null;
  static Map<NATIVES, Boolean> libsLoaded = new HashMap<NATIVES, Boolean>();

  static String sxLibsCheckStamp = "MadeForSikuliX_%s";
  static String sflibsCheckFileStored = "MadeForSikuliX2";
  public static String sxLibsCheckName = "";
  public static String sfLibOpencvJava = "_ext_opencv_java";
  public static String sfLibJXGrabKey = "_ext_JXGrabKey";
  public static String sfLibJIntellitype = "_ext_JIntellitype";
  public static String sfLibWinUtil = "_ext_WinUtil";
  public static String sfLibMacUtil = "_ext_MacUtil";
  public static String sfLibMacHotkey = "_ext_MacHotkeyManager";

  static class LibsFilter implements FilenameFilter {

    String sAccept = "";

    public LibsFilter(String toAccept) {
      sAccept = toAccept;
    }

    @Override
    public boolean accept(File dir, String name) {
      if (dir.getPath().contains(sAccept)) {
        return true;
      }
      return false;
    }
  }

  static void addToWindowsSystemPath(File fLibsFolder) {
    String syspath = SXJNA.WinKernel32.getEnvironmentVariable("PATH");
    if (syspath == null) {
      terminate(1, "addToWindowsSystemPath: cannot access system path");
    } else {
      String libsPath = (fLibsFolder.getAbsolutePath()).replaceAll("/", "\\");
      if (!syspath.toUpperCase().contains(libsPath.toUpperCase())) {
        if (!SXJNA.WinKernel32.setEnvironmentVariable("PATH", libsPath + ";" + syspath)) {
          terminate(999, "", "");
        }
        syspath = SXJNA.WinKernel32.getEnvironmentVariable("PATH");
        if (!syspath.toUpperCase().contains(libsPath.toUpperCase())) {
          terminate(1, "addToWindowsSystemPath: did not work: %s", syspath);
        }
        debug("addToWindowsSystemPath: added: %s", libsPath);
      }
    }
  }

  static boolean checkJavaUsrPath(File fLibsFolder) {
    String fpLibsFolder = fLibsFolder.getAbsolutePath();
    Field usrPathsField = null;
    boolean contained = false;
    try {
      usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
    } catch (NoSuchFieldException ex) {
      error("checkJavaUsrPath: get (%s)", ex);
    } catch (SecurityException ex) {
      error("checkJavaUsrPath: get (%s)", ex);
    }
    if (usrPathsField != null) {
      usrPathsField.setAccessible(true);
      try {
        //get array of paths
        String[] javapaths = (String[]) usrPathsField.get(null);
        //check if the path to add is already present
        for (String p : javapaths) {
          if (new File(p).equals(fLibsFolder)) {
            contained = true;
            break;
          }
        }
        //add the new path
        if (!contained) {
          final String[] newPaths = Arrays.copyOf(javapaths, javapaths.length + 1);
          newPaths[newPaths.length - 1] = fpLibsFolder;
          usrPathsField.set(null, newPaths);
          debug("checkJavaUsrPath: added to ClassLoader.usrPaths");
          contained = true;
        }
      } catch (IllegalAccessException ex) {
        error("checkJavaUsrPath: set (%s)", ex);
      } catch (IllegalArgumentException ex) {
        error("checkJavaUsrPath: set (%s)", ex);
      }
      return contained;
    }
    return false;
  }

  static void exportLibraries() {
    if (areLibsExported) {
      return;
    }
    File fSXNative = Content.asFile(getSXNATIVE());
    if (!new File(fSXNative, sxLibsCheckName).exists()) {
      debug("exportLibraries: folder empty or has wrong content");
      Content.deleteFileOrFolder(fSXNative);
    }
    if (fSXNative.exists()) {
      debug("exportLibraries: folder exists: %s", fSXNative);
    } else {
      fSXNative.mkdirs();
      if (!fSXNative.exists()) {
        terminate(1, "exportLibraries: folder not available: %s", fSXNative);
      }
      debug("exportLibraries: new folder: %s", fSXNative);
      fpJarLibs = "/Native/" + getSYSGENERIC();
      extractLibraries(sxGlobalClassReference, fpJarLibs, fSXNative);
      try {
        extractLibraries(Class.forName("com.sikulix.opencv.Sikulix"), fpJarLibs, fSXNative);
      } catch (ClassNotFoundException e) {
        log.error("exportLibraries: package com.sikulix.opencv not on classpath");
      }
      if (!new File(fSXNative, sflibsCheckFileStored).exists()) {
        terminate(1, "exportLibraries: did not work");
      }
      new File(fSXNative, sflibsCheckFileStored).renameTo(new File(fSXNative, sxLibsCheckName));
      if (!new File(fSXNative, sxLibsCheckName).exists()) {
        terminate(1, "exportLibraries: did not work");
      }
    }
    for (String aFile : fSXNative.list()) {
      if (aFile.contains("opencv_java")) {
        sfLibOpencvJava = aFile;
      } else if (aFile.contains("JXGrabKey")) {
        sfLibJXGrabKey = aFile;
      } else if (aFile.contains("JIntellitype")) {
        sfLibJIntellitype = aFile;
      } else if (aFile.contains("WinUtil")) {
        sfLibWinUtil = aFile;
      } else if (aFile.contains("MacUtil")) {
        sfLibMacUtil = aFile;
      } else if (aFile.contains("MacHotkey")) {
        sfLibMacHotkey = aFile;
      }
    }
    areLibsExported = true;
  }

  private static void extractLibraries(Class classRef, String from, File fTo) {
    String classLocation = whereIs(classRef);
    List<String> libraries;
    String source = classLocation;
    String sourceType = " from jar";
    if (classLocation.endsWith(".jar")) {
      libraries = Content.extractResourcesToFolderFromJar(classLocation, from, fTo, null);
    } else {
      URL uLibsFrom = classRef.getResource(from);
      libraries = Content.extractResourcesToFolder(from, fTo, null);
      source = uLibsFrom.toString();
      sourceType = "";
    }
    int libCount = libraries.size();
    if (libCount == 0) {
      error("extractLibraries: (none)%s: %s", sourceType, source);
    } else {
      if (libraries.contains("MadeForSikuliX2")) {
        libCount--;
      }
      trace("extractLibraries: (%d)%s: %s", libCount, sourceType, source);
    }
  }

  public static enum NATIVES {
    OPENCV, TESSERACT, SYSUTIL, HOTKEY
  }

  public static boolean loadNative(NATIVES type) {
    boolean success = true;
    if (libsLoaded.isEmpty()) {
      for (NATIVES nType : NATIVES.values()) {
        libsLoaded.put(nType, false);
      }
      exportLibraries();
      if (isWindows()) {
        addToWindowsSystemPath(Content.asFile(getSXNATIVE()));
        if (!checkJavaUsrPath(Content.asFile(getSXNATIVE()))) {
          error("exportLibraries: JavaUserPath: see errors - might not work and crash later");
        }
        String lib = "jawt.dll";
        File fJawtDll = new File(Content.asFile(getSXNATIVE()), lib);
        Content.deleteFileOrFolder(fJawtDll);
        Content.xcopy(new File(getSXJAVAHOME() + "/bin/" + lib), fJawtDll);
        if (!fJawtDll.exists()) {
          terminate(1, "exportLibraries: problem copying %s", fJawtDll);
        }
      }
    }
    if (OPENCV.equals(type) && !libsLoaded.get(OPENCV)) {
      loadNativeLibrary(sfLibOpencvJava);
    } else if (SYSUTIL.equals(type) && !libsLoaded.get(SYSUTIL)) {
      if (isWindows()) {
        loadNativeLibrary(sfLibWinUtil);
      } else if (isMac()) {
        loadNativeLibrary(sfLibMacUtil);
      }
    } else if (HOTKEY.equals(type) && !libsLoaded.get(HOTKEY)) {
      if (isWindows()) {
        loadNativeLibrary(sfLibJIntellitype);
      } else if (isMac()) {
        loadNativeLibrary(sfLibMacHotkey);
      } else if (isLinux()) {
        loadNativeLibrary(sfLibJXGrabKey);
      }
    } else {
      success = false;
    }
    if (success) {
      libsLoaded.put(type, true);
    }
    return success;
  }

  static void loadNativeLibrary(String aLib) {
    try {
      if (aLib.startsWith("_ext_")) {
        error("loadNativeLibrary: loading external library not implemented: %s", aLib);
      } else {
        String sf_aLib = new File(getSXNATIVE(), aLib).getAbsolutePath();
        System.load(sf_aLib);
        trace("loadNativeLibrary: bundled: %s", aLib);
      }
    } catch (UnsatisfiedLinkError ex) {
      terminate(1, "loadNativeLibrary: loading library error: %s (%s)", aLib, ex.getMessage());
    }
  }
  //</editor-fold>

  //<editor-fold desc="13*** global helper methods">
  public static List<String> listPublicMethods(Class clazz) {
    return listPublicMethods(clazz, true);
  }

  public static List<String> listPublicMethods(Class clazz, boolean silent) {
    Method[] declaredMethods = clazz.getDeclaredMethods();
    List<String> publicMethods = new ArrayList<>();
    for (Method method : declaredMethods) {
      int modifiers = method.getModifiers();
      if (Modifier.isPublic(modifiers)) {
        int parameterCount = method.getParameterCount();
        String name = method.getName();
        String prefix = "";
        if (name.startsWith("get")) {
          prefix = "get";
        } else if (name.startsWith("set")) {
          prefix = "set";
        } else if (name.startsWith("isSet")) {
          prefix = "isSet";
        } else if (name.startsWith("is")) {
          prefix = "is";
        } else if (name.startsWith("has")) {
          prefix = "has";
        } else if (name.startsWith("as")) {
          prefix = "as";
        } else if (name.startsWith("load")) {
          prefix = "load";
        } else if (name.startsWith("save")) {
          prefix = "save";
        } else if (name.startsWith("dump")) {
          prefix = "dump";
        } else if (name.startsWith("make")) {
          prefix = "make";
        } else if (name.startsWith("eval")) {
          prefix = "eval";
        } else if (name.startsWith("exists")) {
          prefix = "exists";
        } else if (name.startsWith("equals")) {
          prefix = "equals";
        }
        name = name.substring(prefix.length());
        publicMethods.add(String.format("%s%s-%d", name, SX.isSet(prefix) ? "-" + prefix : "", parameterCount));
      }
    }
    Collections.sort(publicMethods);
    if (!silent) {
      for (String entry : publicMethods) {
        if (entry.startsWith("SX") || entry.startsWith("Option")) continue;
        log.p("%s", entry);
      }
    }
    return publicMethods;
  }
  /**
   * check wether the given object is in JSON format as ["ID", ...]
   *
   * @param json
   * @return true if object is in JSON format, false otherwise
   */
  public static boolean isJSON(Object json) {
    if (json instanceof String) {
      return ((String) json).trim().startsWith("[\"") || ((String) json).trim().startsWith("{\"");
    }
    return false;
  }

  public static void dumpSysProps() {
    dumpSysProps(null);
  }

  public static void dumpSysProps(String filter) {
    filter = filter == null ? "" : filter;
    p("*** system properties dump " + filter);
    Properties sysProps = System.getProperties();
    ArrayList<String> keysProp = new ArrayList<String>();
    Integer nL = 0;
    String entry;
    for (Object e : sysProps.keySet()) {
      entry = (String) e;
      if (entry.length() > nL) {
        nL = entry.length();
      }
      if (filter.isEmpty() || !filter.isEmpty() && entry.contains(filter)) {
        keysProp.add(entry);
      }
    }
    Collections.sort(keysProp);
    String form = "%-" + nL.toString() + "s = %s";
    for (Object e : keysProp) {
      p(form, e, sysProps.get(e));
    }
    p("*** system properties dump end" + filter);
  }

  public static void show() {
    if (hasOptions()) {
      dumpOptions();
    }
    p("***** show environment (%s)", getSXVERSIONSHOW());
    p("user.home: %s", getSXUSERHOME());
    p("user.dir (work dir): %s", getSXUSERWORK());
    p("java.io.tmpdir: %s", getSXSYSTEMP());
    p("running on %s", getSXSYSTEM());
    p(getSXJAVAVERSION());
    p("app data folder: %s", getSXAPP());
    p("libs folder: %s", getSXNATIVE());
    if (isSet(SXBASEJAR)) {
      p("executing jar: %s", SXBASEJAR);
    }
    Content.dumpClassPath("sikulix");
    //TODO ScriptingHelper
//    if (isJythonReady) {
//      JythonHelper.get().showSysPath();
//    }
    p("***** show environment end");
  }

  public static boolean isNull(Object obj) {
    return null == obj;
  }

  public static boolean isNotNull(Object obj) {
    return null != obj;
  }

  public static boolean isNotSet(Object obj) {
    if (null != obj && obj instanceof String) {
      if (((String) obj).isEmpty()) {
        return true;
      } else {
        return false;
      }
    }
    return null == obj;
  }

  public static boolean isSet(Object obj) {
    if (null != obj && obj instanceof String) {
      if (((String) obj).isEmpty()) {
        return false;
      } else {
        return true;
      }
    }
    return null != obj;
  }

  public static boolean isSet(String var, String val) {
    if (null != var && null != val) {
      if (var.isEmpty()) {
        return false;
      } else {
        return val.equals(var);
      }
    }
    return false;
  }

  public static void pause(int time) {
    try {
      Thread.sleep(time * 1000);
    } catch (InterruptedException ex) {
    }
  }

  public static void pause(float time) {
    try {
      Thread.sleep((int) (time * 1000));
    } catch (InterruptedException ex) {
    }
  }

  public static void pause(double time) {
    try {
      Thread.sleep((int) (time * 1000));
    } catch (InterruptedException ex) {
    }
  }
  //</editor-fold>
}
