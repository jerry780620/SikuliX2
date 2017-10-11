/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.core;

//import org.sikuli.basics.PreferencesUser;

import com.sikulix.util.SplashFrame;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.Charset;
import java.security.CodeSource;
import java.util.*;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Content {

  private static SXLog log = SX.getSXLog("SX.Content");
  private static int lvl = SXLog.DEBUG;

  public static void start() {
    //log.trace("!start: class init");
  }

  //<editor-fold desc="001*** Proxy">
  private static String proxyName = "";
  private static String proxyIP = "";
  private static InetAddress proxyAddress = null;
  private static String proxyPort = "";
  private static boolean proxyChecked = false;
  private static Proxy sxProxy = null;

  public static Proxy getProxy() {
    Proxy proxy = sxProxy;
    if (!proxyChecked) {
      String phost = proxyName;
      String padr = proxyIP;
      String pport = proxyPort;
      InetAddress a = null;
      int p = -1;
      if (phost != null) {
        a = getAddress(phost);
      }
      if (a == null && padr != null) {
        a = getAddress(padr);
      }
      if (a != null && pport != null) {
        p = getPort(pport);
      }
      if (a != null && p > 1024) {
        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(a, p));
        log.debug("Proxy defined: %s : %d", a.getHostAddress(), p);
      }
      proxyChecked = true;
      sxProxy = proxy;
    }
    return proxy;
  }

  public static boolean setProxy(String pName, String pPort) {
    InetAddress a = null;
    String host = null;
    String adr = null;
    int p = -1;
    if (pName != null) {
      a = getAddress(pName);
      if (a == null) {
        a = getAddress(pName);
        if (a != null) {
          adr = pName;
        }
      } else {
        host = pName;
      }
    }
    if (a != null && pPort != null) {
      p = getPort(pPort);
    }
    if (a != null && p > 1024) {
      log.debug("Proxy stored: %s : %d", a.getHostAddress(), p);
      proxyChecked = true;
      proxyName = host;
      proxyIP = adr;
      proxyPort = pPort;
      sxProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(a, p));
////TODO options
//      PreferencesUser prefs = PreferencesUser.getInstance();
//      prefs.put("ProxyName", (host == null ? "" : host));
//      prefs.put("ProxyIP", (adr == null ? "" : adr));
//      prefs.put("ProxyPort", ""+p);
      return true;
    }
    return false;
  }

  public static int getProxyPort(String p) {
    int port;
    int pDefault = 50000;
    if (p != null) {
      try {
        port = Integer.parseInt(p);
      } catch (NumberFormatException ex) {
        return -1;
      }
    } else {
      return pDefault;
    }
    if (port < 1024) {
      port += pDefault;
    }
    return port;
  }

  private static int getPort(String p) {
    int port;
    int pDefault = 8080;
    if (p != null) {
      try {
        port = Integer.parseInt(p);
      } catch (NumberFormatException ex) {
        return -1;
      }
    } else {
      return pDefault;
    }
    return port;
  }

  public static String getProxyAddress(String arg) {
    try {
      if (arg == null) {
        return InetAddress.getLocalHost().getHostAddress();
      }
      return InetAddress.getByName(arg).getHostAddress();
    } catch (UnknownHostException ex) {
      return null;
    }
  }

  private static InetAddress getAddress(String arg) {
    try {
      return InetAddress.getByName(arg);
    } catch (UnknownHostException ex) {
      return null;
    }
  }
  //</editor-fold>

  //<editor-fold desc="010*** java class path">
  public static String whereIs(Class clazz) {
    CodeSource codeSrc = clazz.getProtectionDomain().getCodeSource();
    String base = null;
    if (codeSrc != null && codeSrc.getLocation() != null) {
      base = slashify(codeSrc.getLocation().getPath(), false);
    }
    return base;
  }

  public static String whereIs(String sclazz) {
    try {
      Class clazz = Class.forName(sclazz, false, ClassLoader.getSystemClassLoader());
      return whereIs(clazz);
    } catch (ClassNotFoundException e) {
      log.p("");
    }
    return null;
  }

  public static List<URL> listClasspath() {
    URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
    return Arrays.asList(sysLoader.getURLs());
  }

  public static void dumpClasspath() {
    dumpClasspath(null);
  }

  public static void dumpClasspath(String filter) {
    filter = filter == null ? "" : filter;
    log.p("*** classpath dump %s", filter);
    String sEntry;
    filter = filter.toUpperCase();
    int n = 0;
    for (URL uEntry : listClasspath()) {
      if (!filter.isEmpty()) {
        if (!uEntry.toString().toUpperCase().contains(filter)) {
          n++;
          continue;
        }
      }
      log.p("%3d: %s", n, uEntry);
      n++;
    }
    log.p("*** classpath dump end");
  }

  public static URL getClasspath(String given) {
    given = normalize(given).toUpperCase();
    URL expectedURL = null;
    for (URL entry : listClasspath()) {
      if (normalize(entry.getPath()).toUpperCase().contains(given)) {
        expectedURL = entry;
      }
    }
    return expectedURL;
  }

  public static boolean addClasspath(String jarOrFolder) {
    URL uJarOrFolder = Content.asURL(jarOrFolder);
    if (!new File(jarOrFolder).exists()) {
      log.error("addToClasspath: does not exist - not added:\n%s", jarOrFolder);
      return false;
    }
    if (onClasspath(uJarOrFolder)) {
      return true;
    }
    log.trace("addToClasspath:\n%s", uJarOrFolder);
    Method method;
    URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
    Class sysclass = URLClassLoader.class;
    try {
      method = sysclass.getDeclaredMethod("addURL", new Class[]{URL.class});
      method.setAccessible(true);
      method.invoke(sysLoader, new Object[]{uJarOrFolder});
    } catch (Exception ex) {
      log.error("Did not work: %s", ex.getMessage());
      return false;
    }
    return true;
  }

  public static boolean onClasspath(Object given) {
    URL expectedURL = null;
    if (given instanceof URL) {
      expectedURL = (URL) given;
    } else {
      expectedURL = asURL(given);
    }
    if (SX.isNotNull(expectedURL)) {
      for (URL entry : listClasspath()) {
        if (new File(expectedURL.getPath()).equals(new File(entry.getPath()))) {
          return true;
        }
      }
    }
    return false;
  }
  //</editor-fold>

  //<editor-fold desc="012*** script / image path">

  /**
   * try to find the given relative image file name on the image path<br>
   * starting from entry 0, the first found existence is taken<br>
   * absolute file names are checked for existence
   *
   * @param names one or more name fragments to form a path
   * @return a valid URL or null if not found/exists
   */
  public static URL onImagePath(String... names) {
    String name = asPath(concatenateFolders(0, names));
    name = asImageFilename(name);
    URL url = null;
    File file = new File(name);
    if (file.isAbsolute()) {
      if (file.exists()) {
        url = asURL(name);
      }
    } else {
      for (URL path : getImagePath().all()) {
        url = asURL(path, name);
        if (existsFile(url)) {
          break;
        }
        url = null;
      }
    }
    return url;
  }
  //</editor-fold>

  //<editor-fold desc="015*** bundle path">
  public static boolean setBundlePath(Object... args) {
    getImagePath().init(args);
    String path = getImagePath().get(0);
    return SX.isSet(path);
  }

  public static void clearImagePath() {
    resetImagePath();
  }

  public static String resetImagePath(Object... args) {
    getImagePath().clear();
    imagePath = null;
    getImagePath().init(args);
    return getImagePath().get(0);
  }

  public static String getBundlePath() {
    getImagePath().init();
    return getImagePath().get(0);
  }

  public static boolean isBundlePathFile() {
    return getImagePath().bundlePathIsFile();
  }
  //</editor-fold>

  //<editor-fold desc="020*** zip jar util">
  public static void zip(String path, String outZip) throws IOException, FileNotFoundException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outZip));
    zipDir(path, zos);
    zos.close();
  }

  private static void zipDir(String dir, ZipOutputStream zos) throws IOException {
    File zipDir = new File(dir);
    String[] dirList = zipDir.list();
    byte[] readBuffer = new byte[1024];
    int bytesIn;
    for (int i = 0; i < dirList.length; i++) {
      File f = new File(zipDir, dirList[i]);
      if (f.isFile()) {
        FileInputStream fis = new FileInputStream(f);
        ZipEntry anEntry = new ZipEntry(f.getName());
        zos.putNextEntry(anEntry);
        while ((bytesIn = fis.read(readBuffer)) != -1) {
          zos.write(readBuffer, 0, bytesIn);
        }
        fis.close();
      }
    }
  }

  public static boolean unzip(String inpZip, String target) {
    return unzip(new File(inpZip), new File(target));
  }

  public static boolean unzip(File fZip, File fTarget) {
    String fpZip = null;
    String fpTarget = null;
    log.debug("unzip: from: %s\nto: %s", fZip, fTarget);
    try {
      fpZip = fZip.getCanonicalPath();
      if (!new File(fpZip).exists()) {
        throw new IOException();
      }
    } catch (IOException ex) {
      log.error("unzip: source not found:\n%s\n%s", fpZip, ex);
      return false;
    }
    try {
      fpTarget = fTarget.getCanonicalPath();
      deleteFileOrFolder(fpTarget);
      new File(fpTarget).mkdirs();
      if (!new File(fpTarget).exists()) {
        throw new IOException();
      }
    } catch (IOException ex) {
      log.error("unzip: target cannot be created:\n%s\n%s", fpTarget, ex);
      return false;
    }
    ZipInputStream inpZip = null;
    ZipEntry entry = null;
    try {
      final int BUF_SIZE = 2048;
      inpZip = new ZipInputStream(new BufferedInputStream(new FileInputStream(fZip)));
      while ((entry = inpZip.getNextEntry()) != null) {
        if (entry.getName().endsWith("/") || entry.getName().endsWith("\\")) {
          new File(fpTarget, entry.getName()).mkdir();
          continue;
        }
        int count;
        byte data[] = new byte[BUF_SIZE];
        File outFile = new File(fpTarget, entry.getName());
        File outFileParent = outFile.getParentFile();
        if (!outFileParent.exists()) {
          outFileParent.mkdirs();
        }
        FileOutputStream fos = new FileOutputStream(outFile);
        BufferedOutputStream dest = new BufferedOutputStream(fos, BUF_SIZE);
        while ((count = inpZip.read(data, 0, BUF_SIZE)) != -1) {
          dest.write(data, 0, count);
        }
        dest.close();
      }
    } catch (Exception ex) {
      log.error("unzip: not possible: source:\n%s\ntarget:\n%s\n(%s)%s",
          fpZip, fpTarget, entry.getName(), ex);
      return false;
    } finally {
      try {
        inpZip.close();
      } catch (IOException ex) {
        log.error("unzip: closing source:\n%s\n%s", fpZip, ex);
      }
    }
    return true;
  }

  public static String unzipSKL(String fpSkl) {
    File fSkl = new File(fpSkl);
    if (!fSkl.exists()) {
      log.error("unzipSKL: file not found: %s", fpSkl);
    }
    String name = fSkl.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    File fSikuliDir = Content.createTempDir(name + ".sikuli");
    if (null != fSikuliDir) {
      fSikuliDir.deleteOnExit();
      Content.unzip(fSkl, fSikuliDir);
    }
    if (null == fSikuliDir) {
      log.error("unzipSKL: not possible for:\n%s", fpSkl);
      return null;
    }
    return fSikuliDir.getAbsolutePath();
  }

  public static boolean zipJar(String folderName, String jarName, String prefix) {
    jarName = Content.slashify(jarName, false);
    if (!jarName.endsWith(".jar")) {
      jarName += ".jar";
    }
    folderName = Content.slashify(folderName, true);
    if (!(new File(folderName)).isDirectory()) {
      log.error("packJar: not a directory or does not exist: " + folderName);
      return false;
    }
    try {
      File dir = new File((new File(jarName)).getAbsolutePath()).getParentFile();
      if (dir != null) {
        if (!dir.exists()) {
          dir.mkdirs();
        }
      } else {
        throw new Exception("workdir is null");
      }
      log.debug("packJar: %s from %s in workDir %s", jarName, folderName, dir.getAbsolutePath());
      if (!folderName.startsWith("http://") && !folderName.startsWith("https://")) {
        folderName = "file://" + (new File(folderName)).getAbsolutePath();
      }
      URL src = new URL(folderName);
      JarOutputStream jout = new JarOutputStream(new FileOutputStream(jarName));
      addToJar(jout, new File(src.getFile()), prefix);
      jout.close();
    } catch (Exception ex) {
      log.error("packJar: " + ex.getMessage());
      return false;
    }
    log.debug("packJar: completed");
    return true;
  }

  public static boolean zipJar(String targetJar, String[] jars,
                               String[] files, String[] prefixs, JarFileFilter filter) {
    boolean logShort = false;
    if (targetJar.startsWith("#")) {
      logShort = true;
      targetJar = targetJar.substring(1);
      log.debug("buildJar: %s", new File(targetJar).getName());
    } else {
      log.debug("buildJar:\n%s", targetJar);
    }
    try {
      JarOutputStream jout = new JarOutputStream(new FileOutputStream(targetJar));
      ArrayList done = new ArrayList();
      for (int i = 0; i < jars.length; i++) {
        if (jars[i] == null) {
          continue;
        }
        if (logShort) {
          log.debug("buildJar: adding: %s", new File(jars[i]).getName());
        } else {
          log.debug("buildJar: adding:\n%s", jars[i]);
        }
        BufferedInputStream bin = new BufferedInputStream(new FileInputStream(jars[i]));
        ZipInputStream zin = new ZipInputStream(bin);
        for (ZipEntry zipentry = zin.getNextEntry(); zipentry != null; zipentry = zin.getNextEntry()) {
          if (filter == null || filter.accept(zipentry, jars[i])) {
            if (!done.contains(zipentry.getName())) {
              jout.putNextEntry(zipentry);
              if (!zipentry.isDirectory()) {
                bufferedWrite(zin, jout);
              }
              done.add(zipentry.getName());
              log.trace("adding: %s", zipentry.getName());
            }
          }
        }
        zin.close();
        bin.close();
      }
      if (files != null) {
        for (int i = 0; i < files.length; i++) {
          if (files[i] == null) {
            continue;
          }
          if (logShort) {
            log.debug("buildJar: adding %s at %s", new File(files[i]).getName(), prefixs[i]);
          } else {
            log.debug("buildJar: adding %s at %s", files[i], prefixs[i]);
          }
          addToJar(jout, new File(files[i]), prefixs[i]);
        }
      }
      jout.close();
    } catch (Exception ex) {
      log.error("buildJar: %s", ex);
      return false;
    }
    log.debug("buildJar: completed");
    return true;
  }

  /**
   * unpack a jar file to a folder
   *
   * @param jarName    absolute path to jar file
   * @param folderName absolute path to the target folder
   * @param del        true if the folder should be deleted before unpack
   * @param strip      true if the path should be stripped
   * @param filter     to select specific content
   * @return true if success,  false otherwise
   */
  public static boolean unzipJar(String jarName, String folderName,
                                 boolean del, boolean strip, JarFileFilter filter) {
    jarName = Content.slashify(jarName, false);
    if (!jarName.endsWith(".jar")) {
      jarName += ".jar";
    }
    if (!new File(jarName).isAbsolute()) {
      log.error("unpackJar: jar path not absolute");
      return false;
    }
    if (folderName == null) {
      folderName = jarName.substring(0, jarName.length() - 4);
    } else if (!new File(folderName).isAbsolute()) {
      log.error("unpackJar: folder path not absolute");
      return false;
    }
    folderName = Content.slashify(folderName, true);
    ZipInputStream in;
    BufferedOutputStream out;
    try {
      if (del) {
        Content.deleteFileOrFolder(folderName);
      }
      in = new ZipInputStream(new BufferedInputStream(new FileInputStream(jarName)));
      log.debug("unpackJar: %s to %s", jarName, folderName);
      boolean isExecutable;
      int n;
      File f;
      for (ZipEntry z = in.getNextEntry(); z != null; z = in.getNextEntry()) {
        if (filter == null || filter.accept(z, null)) {
          if (z.isDirectory()) {
            (new File(folderName, z.getName())).mkdirs();
          } else {
            n = z.getName().lastIndexOf(EXECUTABLE);
            if (n >= 0) {
              f = new File(folderName, z.getName().substring(0, n));
              isExecutable = true;
            } else {
              f = new File(folderName, z.getName());
              isExecutable = false;
            }
            if (strip) {
              f = new File(folderName, f.getName());
            } else {
              f.getParentFile().mkdirs();
            }
            out = new BufferedOutputStream(new FileOutputStream(f));
            bufferedWrite(in, out);
            out.close();
            if (isExecutable) {
              f.setExecutable(true, false);
            }
          }
        }
      }
      in.close();
    } catch (Exception ex) {
      log.error("unpackJar: " + ex.getMessage());
      return false;
    }
    log.debug("unpackJar: completed");
    return true;
  }

  private static void addToJar(JarOutputStream jar, File dir, String prefix) throws IOException {
    File[] content;
    prefix = prefix == null ? "" : prefix;
    if (dir.isDirectory()) {
      content = dir.listFiles();
      for (int i = 0, l = content.length; i < l; ++i) {
        if (content[i].isDirectory()) {
          jar.putNextEntry(new ZipEntry(prefix + (prefix.equals("") ? "" : "/") + content[i].getName() + "/"));
          addToJar(jar, content[i], prefix + (prefix.equals("") ? "" : "/") + content[i].getName());
        } else {
          addToJarWriteFile(jar, content[i], prefix);
        }
      }
    } else {
      addToJarWriteFile(jar, dir, prefix);
    }
  }

  private static void addToJarWriteFile(JarOutputStream jar, File file, String prefix) throws IOException {
    if (file.getName().startsWith(".")) {
      return;
    }
    String suffix = "";
//TODO buildjar: suffix EXECUTABL
//    if (file.canExecute()) {
//      suffix = EXECUTABLE;
//    }
    jar.putNextEntry(new ZipEntry(prefix + (prefix.equals("") ? "" : "/") + file.getName() + suffix));
    FileInputStream in = new FileInputStream(file);
    bufferedWrite(in, jar);
    in.close();
  }
  //</editor-fold>

  //<editor-fold desc="022*** basic IO">
  public static void copy(InputStream in, OutputStream out) throws IOException {
    byte[] tmp = new byte[8192];
    int len;
    while (true) {
      len = in.read(tmp);
      if (len <= 0) {
        break;
      }
      out.write(tmp, 0, len);
    }
    out.flush();
  }

  public static byte[] copy(InputStream inputStream) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int length = 0;
    while ((length = inputStream.read(buffer)) != -1) {
      baos.write(buffer, 0, length);
    }
    return baos.toByteArray();
  }

  private static synchronized void bufferedWrite(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[1024 * 512];
    int read;
    while (true) {
      read = in.read(buffer);
      if (read == -1) {
        break;
      }
      out.write(buffer, 0, read);
    }
    out.flush();
  }

  public static boolean writeStringToFile(String text, String path) {
    return writeStringToFile(text, new File(path));
  }

  public static boolean writeStringToFile(String text, File fPath) {
    PrintStream out = null;
    try {
      out = new PrintStream(new FileOutputStream(fPath));
      out.print(text);
    } catch (Exception e) {
      log.error("writeStringToFile: did not work: " + fPath + "\n" + e.getMessage());
    }
    if (out != null) {
      out.close();
      return true;
    }
    return false;
  }

  public static String readFileToString(File fPath) {
    try {
      return doReadFileToString(fPath);
    } catch (Exception ex) {
      return "";
    }
  }

  private static String doReadFileToString(File fPath) throws IOException {
    StringBuilder result = new StringBuilder();
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(fPath));
      char[] buf = new char[1024];
      int r = 0;
      while ((r = reader.read(buf)) != -1) {
        result.append(buf, 0, r);
      }
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
    return result.toString();
  }

  public static String convertStreamToString(InputStream is) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    StringBuilder sb = new StringBuilder();
    String line;
    try {
      while ((line = reader.readLine()) != null) {
        sb.append(line).append("\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return sb.toString();
  }
  //</editor-fold>

  //<editor-fold desc="025*** filter">
  public interface JarFileFilter {
    public boolean accept(ZipEntry entry, String jarname);
  }

  public interface FileFilter {
    public boolean accept(File entry);
  }

  public class oneFileFilter implements FilenameFilter {

    String aFile;

    public oneFileFilter(String aFileGiven) {
      aFile = aFileGiven;
    }

    @Override
    public boolean accept(File dir, String name) {
      if (name.contains(aFile)) {
        return true;
      }
      return false;
    }
  }

  public static void traverseFolder(File fPath, FileFilter filter) {
    if (fPath == null) {
      return;
    }
    File aFile;
    String[] entries;
    if (fPath.isDirectory()) {
      entries = fPath.list();
      for (int i = 0; i < entries.length; i++) {
        aFile = new File(fPath, entries[i]);
        if (filter != null) {
          filter.accept(aFile);
        }
        if (aFile.isDirectory()) {
          traverseFolder(aFile, filter);
        }
      }
    }
  }
  //</editor-fold>

  //<editor-fold desc="027*** delete File / Folder">
  public static boolean deleteFileOrFolder(File fPath, FileFilter filter) {
    return doDeleteFileOrFolder(fPath, filter);
  }

  public static boolean deleteFileOrFolder(File fPath) {
    return doDeleteFileOrFolder(fPath, null);
  }

  public static boolean deleteFileOrFolder(String fpPath, FileFilter filter) {
    if (fpPath.startsWith("#")) {
      fpPath = fpPath.substring(1);
    } else {
      log.debug("deleteFileOrFolder: %s\n%s", (filter == null ? "" : "filtered: "), fpPath);
    }
    return doDeleteFileOrFolder(new File(fpPath), filter);
  }

  public static boolean deleteFileOrFolder(String fpPath) {
    if (fpPath.startsWith("#")) {
      fpPath = fpPath.substring(1);
    } else {
      log.debug("deleteFileOrFolder:\n%s", fpPath);
    }
    return doDeleteFileOrFolder(new File(fpPath), null);
  }

  public static void resetFolder(File fPath) {
    log.debug("resetFolder:\n%s", fPath);
    doDeleteFileOrFolder(fPath, null);
    fPath.mkdirs();
  }

  private static boolean doDeleteFileOrFolder(File fPath, FileFilter filter) {
    if (fPath == null) {
      return false;
    }
    File aFile;
    String[] entries;
    boolean somethingLeft = false;
    if (fPath.exists() && fPath.isDirectory()) {
      entries = fPath.list();
      for (int i = 0; i < entries.length; i++) {
        aFile = new File(fPath, entries[i]);
        if (filter != null && !filter.accept(aFile)) {
          somethingLeft = true;
          continue;
        }
        if (aFile.isDirectory()) {
          if (!doDeleteFileOrFolder(aFile, filter)) {
            return false;
          }
        } else {
          try {
            aFile.delete();
          } catch (Exception ex) {
            log.error("deleteFile: not deleted:\n%s\n%s", aFile, ex);
            return false;
          }
        }
      }
    }
    // deletes intermediate empty directories and finally the top now empty dir
    if (!somethingLeft && fPath.exists()) {
      try {
        fPath.delete();
      } catch (Exception ex) {
        log.error("deleteFolder: not deleted:\n" + fPath.getAbsolutePath() + "\n" + ex.getMessage());
        return false;
      }
    }
    return true;
  }

  public static void deleteNotUsedImages(String bundle, Set<String> usedImages) {
    File scriptFolder = new File(bundle);
    if (!scriptFolder.isDirectory()) {
      return;
    }
    String path;
    for (File image : scriptFolder.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        if ((name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"))) {
          if (!name.startsWith("_")) {
            return true;
          }
        }
        return false;
      }
    })) {
      if (!usedImages.contains(image.getName())) {
        log.debug("Content: delete not used: %s", image.getName());
        image.delete();
      }
    }
  }
  //</editor-fold>

  //<editor-fold desc="028*** copy File / Folder">

  /**
   * Copy a file *src* to the path *dest* and check if the file name conflicts. If a file with the
   * same name exists in that path, rename *src* to an alternative name.
   *
   * @param src  source file
   * @param dest destination path
   * @return the destination file if ok, null otherwise
   * @throws IOException on failure
   */
  public static File smartCopy(String src, String dest) throws IOException {
    File fSrc = new File(src);
    String newName = fSrc.getName();
    File fDest = new File(dest, newName);
    if (fSrc.equals(fDest)) {
      return fDest;
    }
    while (fDest.exists()) {
      newName = getAltFilename(newName);
      fDest = new File(dest, newName);
    }
    xcopy(src, fDest.getAbsolutePath());
    if (fDest.exists()) {
      return fDest;
    }
    return null;
  }

  private static String getAltFilename(String filename) {
    int pDot = filename.lastIndexOf('.');
    int pDash = filename.lastIndexOf('-');
    int ver = 1;
    String postfix = filename.substring(pDot);
    String name;
    if (pDash >= 0) {
      name = filename.substring(0, pDash);
      ver = Integer.parseInt(filename.substring(pDash + 1, pDot));
      ver++;
    } else {
      name = filename.substring(0, pDot);
    }
    return name + "-" + ver + postfix;
  }

  public static boolean xcopy(File fSrc, File fDest) {
    if (fSrc == null || fDest == null) {
      return false;
    }
    try {
      doXcopy(fSrc, fDest, null);
    } catch (Exception ex) {
      log.debug("xcopy from: %s\nto: %s\n%s", fSrc, fDest, ex);
      return false;
    }
    return true;
  }

  public static boolean xcopy(File fSrc, File fDest, FileFilter filter) {
    if (fSrc == null || fDest == null) {
      return false;
    }
    try {
      doXcopy(fSrc, fDest, filter);
    } catch (Exception ex) {
      log.debug("xcopy from: %s\nto: %s\n%s", fSrc, fDest, ex);
      return false;
    }
    return true;
  }

  public static void xcopy(String src, String dest) throws IOException {
    doXcopy(new File(src), new File(dest), null);
  }

  public static void xcopy(String src, String dest, FileFilter filter) throws IOException {
    doXcopy(new File(src), new File(dest), filter);
  }

  private static void doXcopy(File fSrc, File fDest, FileFilter filter) throws IOException {
    if (fSrc.getAbsolutePath().equals(fDest.getAbsolutePath())) {
      return;
    }
    if (fSrc.isDirectory()) {
      if (filter == null || filter.accept(fSrc)) {
        if (!fDest.exists()) {
          fDest.mkdirs();
        }
        String[] children = fSrc.list();
        for (String child : children) {
          if (child.equals(fDest.getName())) {
            continue;
          }
          doXcopy(new File(fSrc, child), new File(fDest, child), filter);

        }
      }
    } else {
      if (filter == null || filter.accept(fSrc)) {
        if (fDest.isDirectory()) {
          fDest = new File(fDest, fSrc.getName());
        }
        InputStream in = new FileInputStream(fSrc);
        OutputStream out = new FileOutputStream(fDest);
        // Copy the bits from instream to outstream
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
          out.write(buf, 0, len);
        }
        in.close();
        out.close();
      }
    }
  }
  //</editor-fold>

  //<editor-fold desc="029*** temp / timed File / Folder">
  public static File createTempDir(String path) {
    File fTempDir = new File(SX.getSXTEMP(), path);
    log.debug("createTempDir:\n%s", fTempDir);
    if (!fTempDir.exists()) {
      fTempDir.mkdirs();
    } else {
      Content.resetFolder(fTempDir);
    }
    if (!fTempDir.exists()) {
      log.error("createTempDir: not possible: %s", fTempDir);
      return null;
    }
    return fTempDir;
  }

  public static File createTempDir() {
    File fTempDir = createTempDir("tmp-" + SX.getRandomInt() + ".sikuli");
    if (null != fTempDir) {
      fTempDir.deleteOnExit();
    }
    return fTempDir;
  }

  public static void deleteTempDir(String path) {
    if (!deleteFileOrFolder(path)) {
      log.error("deleteTempDir: not possible");
    }
  }

  public static File createTempFile(String suffix) {
    return createTempFile(suffix, null);
  }

  public static File createTempFile(String suffix, String path) {
    String temp1 = "sikuli-";
    String temp2 = "." + suffix;
    File fpath = new File(SX.getSXTEMP());
    if (path != null) {
      fpath = new File(path);
    }
    try {
      fpath.mkdirs();
      File temp = File.createTempFile(temp1, temp2, fpath);
      temp.deleteOnExit();
      String fpTemp = temp.getAbsolutePath();
      if (!fpTemp.endsWith(".script")) {
        log.debug("tempfile create:\n%s", temp.getAbsolutePath());
      }
      return temp;
    } catch (IOException ex) {
      log.error("createTempFile: IOException: %s\n%s", ex.getMessage(),
          fpath + File.separator + temp1 + "12....56" + temp2);
      return null;
    }
  }

  public static String saveTmpImage(BufferedImage img) {
    return saveTmpImage(img, null, "png");
  }

  public static String saveTmpImage(BufferedImage img, String typ) {
    return saveTmpImage(img, null, typ);
  }

  public static String saveTmpImage(BufferedImage img, String path, String typ) {
    File tempFile;
    boolean success;
    try {
      tempFile = createTempFile(typ, path);
      if (tempFile != null) {
        success = ImageIO.write(img, typ, tempFile);
        if (success) {
          return tempFile.getAbsolutePath();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static String saveTimedImage(BufferedImage img) {
    return saveTimedImage(img, getBundlePath(), null);
  }

  public static String saveTimedImage(BufferedImage img, String path) {
    return saveTimedImage(img, path, null);
  }

  public static String saveTimedImage(BufferedImage img, String path, String name) {
    SX.pause(0.01f);
    File fImage = new File(path, String.format("%s-%d.png", name, new Date().getTime()));
    try {
      ImageIO.write(img, "png", fImage);
    } catch (Exception ex) {
      return "";
    }
    return fImage.getAbsolutePath();
  }
  //</editor-fold>

  //<editor-fold desc="030*** evaluate File / URL">
  public static boolean existsFile(Object... args) {
    boolean exists = false;
    URL url = asURL(args);
    if (SX.isNotNull(url)) {
      if ("file".equals(url.getProtocol())) {
        File file = new File(asPath(url));
        if (SX.isNull(file)) {
          return false;
        }
        exists = file.exists();
      } else if ("jar".equals(url.getProtocol())) {
        exists = isInJar(url);
      } else if (url.getProtocol().startsWith("http")) {
        int urlUseable = isUrlUseabel(url);
        exists = 1 == urlUseable;
      }
    }
    return exists;
  }

  private static boolean isInJar(URL jarURL, String... resources) {
    String jarSep = "!/";
    ZipInputStream jarZip = null;
    String jarPath = asPath(jarURL);
    String resource = null;
    boolean found = false;
    if (jarPath.contains(jarSep)) {
      String[] parts = jarPath.split(jarSep);
      jarPath = parts[0];
      resource = parts[1];
      try {
        jarURL = new URL("file:" + jarPath);
      } catch (MalformedURLException e) {
        log.error("isInjar: invalid: %s (%s)", jarPath, e.getMessage());
      }
    }
    try {
      jarZip = new ZipInputStream(jarURL.openStream());
      ZipEntry entry = jarZip.getNextEntry();
      while (entry != null) {
        if (resource.equals(entry.toString())) {
          found = true;
          break;
        }
        entry = jarZip.getNextEntry();
      }
    } catch (Exception ex) {
      log.error("isInJar: error: %s", ex.getMessage());
    }
    return found;
  }

  private static final Pattern validEndings = Pattern.compile(".*?(.png|.jpg|.jpeg|.tiff|.bmp)$");

  public static String asImageFilename(String fname) {
    if (!validEndings.matcher(fname).matches()) {
      int indexOfDot = fname.substring(1).indexOf(".");
      if (0 > indexOfDot) {
        fname += ".png";
      } else {
        log.error("asImageFilename: image type might not be supported: %s", fname.substring(indexOfDot + 1));
      }
    }
    return fname;
  }

  public static String evalJarPath(String className) {
    Class clazz = null;
    try {
      clazz = Class.forName(className);
    } catch (ClassNotFoundException e) {
      log.error("evalJarPath: not on classpath: %s", className);
    }
    String path = "";
    if (SX.isNotNull(clazz)) {
      CodeSource src = clazz.getProtectionDomain().getCodeSource();
      if (SX.isNotNull(src.getLocation())) {
        path = new File(src.getLocation().getPath()).getAbsolutePath();
      }
    }
    return path;
  }

  public static String evalJarName(String className) {
    String jarPath = evalJarPath(className);
    String name = "";
    if (SX.isSet(jarPath)) {
      name = new File(jarPath).getName();
    }
    return name;
  }

  public static String asPath(Object path) {
    String sPath = SX.isNull(path) ? "" : path.toString();
    if (path instanceof URL) {
      URL uPath = (URL) path;
      String proto = "";
      sPath = uPath.getPath();
      proto = uPath.getProtocol();
      if ("file".equals(proto) || "jar".equals(proto)) {
        sPath = sPath.replaceFirst("file:", "");
      } else {
        sPath = uPath.toExternalForm();
      }
    }
    return slashify(sPath, false);
  }

  public static File asFile(Object... args) {
    URL url = asURL(args);
    if (SX.isNull(url) || !"file".equals(url.getProtocol())) {
      return null;
    }
    return new File(asPath(url));
  }

  public static File asFolder(Object... args) {
    File file = asFile(args);
    if (SX.isNull(file)) {
      return null;
    }
    if (file.isDirectory()) {
      return file;
    }
    file.mkdirs();
    if (file.isDirectory()) {
      return file;
    }
    log.error("asFolder: not created %s", file);
    return null;
  }

  public static URL asURL(Object... args) {
    URL url = null;
    File subs = null;
    if (args.length > 0) {
      if (args.length > 1) {
        subs = concatenateFolders(1, args);
      }
      if (args[0] instanceof String) {
        String path = (String) args[0];
        if (path.startsWith("http")) {
          url = asNetURL(path, subs);
        } else if (path.startsWith("jar:") || path.equals("jar")
            || path.endsWith(".jar") || path.contains(".jar!/")) {
          url = asJarURL(path, subs);
        } else {
          url = asFileURL(path, subs);
        }
      } else if (args[0] instanceof URL) {
        url = (URL) args[0];
        String proto = ((URL) args[0]).getProtocol();
        if (proto.startsWith("http")) {
          url = asNetURL(url, subs);
        } else if (proto.startsWith("jar")) {
          url = asJarURL(url, subs);
        } else {
          url = asFileURL(url, subs);
        }
      } else if (args[0] instanceof File) {
        url = asFileURL(args[0], subs);
      } else {
        log.error("asURL: invalid arg0: %s", args[0]);
      }
    }
    return url;
  }

  private static File concatenateFolders(int start, Object... folders) {
    File file = null; //new File("");
    File checkFile = null;
//    if (start == 0 && (folders[0] instanceof String && ((String) folders[0]).startsWith("./"))) {
//      return file;
//    }
//    boolean slashAdded = false;
//    if (!file.isAbsolute()) {
//      file = new File("/" + asPath(folders[start]));
//      slashAdded = true;
//    }
    for (int n = start; n < folders.length; n++) {
      file = new File(file, folders[n].toString());
    }
    try {
      checkFile = file.getCanonicalFile();
    } catch (IOException e) {
      file = null;
    }
//    if (slashAdded) {
//      file = new File(file.toString().substring(1));
//    }
    return file;
  }

  private static URL asFileURL(Object base, File subs) {
    URL url = null;
    File file = new File(base.toString().replaceFirst("file:", ""));
    if (SX.isNotNull(subs)) {
      file = new File(file, subs.toString());
    }
    String genericPath = slashify(file.toString(), false);
    if (genericPath.startsWith("./") || !file.exists()) {
      url = asClassURL(genericPath, subs);
      if (SX.isNotNull(url)) {
        return url;
      }
    }
    try {
      url = new URL("file:" + file.toString());
    } catch (MalformedURLException e) {
      SX.error("asFileURL: %s (%s)", file, e.getMessage());
    }
    return url;
  }

  private static URL asClassURL(String base, File subs) {
    URL url = null;
    Class clazz = null;
    String clazzName;
    String subPath = null;
    int n = base.indexOf("/");
    if (n > 0) {
      clazzName = base.substring(0, n);
      if (n < base.length() - 2) {
        subPath = base.substring(n + 1);
        if (subPath.isEmpty()) {
          subPath = null;
        }
      }
    } else {
      clazzName = base;
    }
    if (".".equals(clazzName)) {
      if (SX.isSet(SX.getSXBASECLASS())) {
        clazzName = SX.getSXBASECLASS();
      } else {
        clazzName = SX.sxGlobalClassReference.getName();
      }
    }
    try {
      clazz = Class.forName(clazzName);
    } catch (ClassNotFoundException ex) {
      //log.error("asClassURL: not on classpath: %s", clazzName);
    }
    if (clazz != null) {
      CodeSource codeSrc = clazz.getProtectionDomain().getCodeSource();
      if (codeSrc != null && codeSrc.getLocation() != null) {
        url = codeSrc.getLocation();
        File allSubs = new File(subPath, (SX.isNull(subs) ? "" : subs.toString()));
        if (url.getPath().endsWith(".jar")) {
          url = asJarURL(url.getPath(), allSubs);
        } else {
          url = asFileURL(url.getPath(), allSubs);
        }
      }
    }
    return url;
  }

  private static URL asJarURL(Object base, File subs) {
    String basePath = asPath(base);
    String folder = asPath(subs);
    URL jarURL = null;
    try {
      String jarSeparator = "!/";
      String separator = jarSeparator;
      if (basePath.contains(".jar" + jarSeparator)) {
        if (!basePath.endsWith("/") && !basePath.endsWith(jarSeparator)) {
          separator = "/";
        }
      }
      if (folder.isEmpty()) {
        jarURL = new URL("jar:file:" + basePath);
      } else {
        jarURL = new URL("jar:file:" + basePath + separator +
            (folder.startsWith("/") ? folder.substring(1) : folder));
      }
    } catch (MalformedURLException e) {
      SX.error("asJarURL: %s %s (%s)", basePath, folder, e.getMessage());
    }
    return jarURL;
  }

  private static URL asNetURL(Object base, File subs) {
    String basePath;
    if (base instanceof String) {
      basePath = (String) base;
      if (!basePath.startsWith("http://") && !basePath.startsWith("https://")) {
        basePath = "http://" + basePath;
      }
    } else if (base instanceof URL && ((URL) base).getProtocol().startsWith("http")) {
      basePath = ((URL) base).toExternalForm();
    } else {
      log.error("asNetURL: invalid arg0: %s", base);
      return null;
    }
    String folder = "";
    if (SX.isNotNull(subs)) {
      folder = asPath(subs);
      if (folder.startsWith("/")) {
        folder = folder.substring(1);
      }
    }
    URL netURL = null;
    try {
      if (SX.isSet(folder)) {
        netURL = new URL((basePath.endsWith("/") ? basePath : basePath + "/") + folder);
      } else {
        netURL = new URL(basePath);
      }
    } catch (MalformedURLException e) {
      SX.error("asNetURL: %s %s (%s)", base, folder, e.getMessage());
    }
    return netURL;
  }

  public static String slashify(String path, Boolean isDirectory) {
    if (path != null) {
      if (path.contains("%")) {
        try {
          path = URLDecoder.decode(path, "UTF-8");
        } catch (Exception ex) {
          log.debug("slashify: decoding problem with %s\nwarning: filename might not be useable.", path);
        }
      }
      if (File.separatorChar != '/') {
        path = path.replace(File.separatorChar, '/');
      }
      if (isDirectory != null) {
        if (isDirectory) {
          if (!path.endsWith("/")) {
            path = path + "/";
          }
        } else if (path.endsWith("/")) {
          path = path.substring(0, path.length() - 1);
        }
      }
      return path;
    } else {
      return "";
    }
  }

  public static String normalize(String filename) {
    String path = slashify(filename, false);
    if (path.startsWith("./")) {
      path = path.substring(2);
    }
    return path;
  }

  public static String normalizeAbsolute(String filename, boolean withTrailingSlash) {
    filename = slashify(filename, false);
    String jarSuffix = "";
    int nJarSuffix;
    if (-1 < (nJarSuffix = filename.indexOf(".jar!/"))) {
      jarSuffix = filename.substring(nJarSuffix + 4);
      filename = filename.substring(0, nJarSuffix + 4);
    }
    File aFile = new File(filename);
    try {
      filename = aFile.getCanonicalPath();
      aFile = new File(filename);
    } catch (Exception ex) {
    }
    String fpFile = aFile.getAbsolutePath();
    if (!fpFile.startsWith("/")) {
      fpFile = "/" + fpFile;
    }
    return slashify(fpFile + jarSuffix, withTrailingSlash);
  }

//  private static String makeFileListString;
//  private static String makeFileListPrefix;
//
//  public static String makeFileList(File path, String prefix) {
//    makeFileListPrefix = prefix;
//    return makeFileListDo(path, true);
//  }
//
//  private static String makeFileListDo(File path, boolean starting) {
//    String x;
//    if (starting) {
//      makeFileListString = "";
//    }
//    if (!path.exists()) {
//      return makeFileListString;
//    }
//    if (path.isDirectory()) {
//      String[] fcl = path.list();
//      for (String fc : fcl) {
//        makeFileListDo(new File(path, fc), false);
//      }
//    } else {
//      x = path.getAbsolutePath();
//      if (!makeFileListPrefix.isEmpty()) {
//        x = x.replace(makeFileListPrefix, "").replace("\\", "/");
//        if (x.startsWith("/")) {
//          x = x.substring(1);
//        }
//      }
//      makeFileListString += x + "\n";
//    }
//    return makeFileListString;
//  }

  /**
   * compares to path strings using java.io.File.equals()
   *
   * @param path1 string
   * @param path2 string
   * @return true if same file or folder
   */
  public static boolean equalsPath(String path1, String path2) {
    return new File(path1).equals(new File(path2));
  }
  //</editor-fold>

  //<editor-fold desc="035*** extract ressources from jar">
  public static List<String> extractTessData(File folder) {
    List<String> files = new ArrayList<String>();

    String tessdata = "/sikulixtessdata";
    URL uContentList = SX.sxGlobalClassReference.getResource(tessdata + "/" + fpContent);
    if (uContentList != null) {
      files = doResourceListWithList(tessdata, files, null);
      if (files.size() > 0) {
        files = doExtractToFolderWithList(tessdata, folder, files);
      }
    } else {
      files = extractResourcesToFolder("/sikulixtessdata", folder, null);
    }
    return (files.size() == 0 ? null : files);
  }

  /**
   * export all resource files from the given subtree on classpath to the given folder retaining the subtree<br>
   * to export a specific file from classpath lock extractResourceToFile or extractResourceToString
   *
   * @param fpRessources path of the subtree relative to root
   * @param fFolder      folder where to export (if null, only list - no export)
   * @param filter       implementation of interface FilenameFilter or null for no filtering
   * @return the filtered list of files (compact sikulixcontent format)
   */
  public static List<String> extractResourcesToFolder(String fpRessources, File fFolder, FilenameFilter filter) {
    List<String> content = null;
    content = makeResourceList(fpRessources, filter);
    if (content == null) {
      return null;
    }
    if (fFolder == null) {
      return content;
    }
    return doExtractToFolderWithList(fpRessources, fFolder, content);
  }

  private static List<String> doExtractToFolderWithList(String fpRessources, File fFolder, List<String> content) {
    int count = 0;
    int ecount = 0;
    String subFolder = "";
    if (content != null && content.size() > 0) {
      for (String eFile : content) {
        if (eFile == null) {
          continue;
        }
        if (eFile.endsWith("/")) {
          subFolder = eFile.substring(0, eFile.length() - 1);
          continue;
        }
        if (!subFolder.isEmpty()) {
          eFile = new File(subFolder, eFile).getPath();
        }
        if (extractResourceToFile(fpRessources, eFile, fFolder)) {
          log.trace("extractResourceToFile done: %s", eFile);
          count++;
        } else {
          ecount++;
        }
      }
    }
    if (ecount > 0) {
      log.debug("files exported: %d - skipped: %d from %s to:\n%s", count, ecount, fpRessources, fFolder);
    } else {
      log.debug("files exported: %d from: %s to:\n%s", count, fpRessources, fFolder);
    }
    return content;
  }

  /**
   * store a resource found on classpath to a file in the given folder with same filename
   *
   * @param inPrefix a subtree found in classpath
   * @param inFile   the filename combined with the prefix on classpath
   * @param outDir   a folder where to export
   * @return success
   */
  public static boolean extractResourceToFile(String inPrefix, String inFile, File outDir) {
    return extractResourceToFile(inPrefix, inFile, outDir, "");
  }

  /**
   * store a resource found on classpath to a file in the given folder
   *
   * @param inPrefix a subtree found in classpath
   * @param inFile   the filename combined with the prefix on classpath
   * @param outDir   a folder where to export
   * @param outFile  the filename for export
   * @return success
   */
  public static boolean extractResourceToFile(String inPrefix, String inFile, File outDir, String outFile) {
    InputStream aIS;
    FileOutputStream aFileOS;
    String content = inPrefix + "/" + inFile;
    try {
      content = SX.isWindows() ? content.replace("\\", "/") : content;
      if (!content.startsWith("/")) {
        content = "/" + content;
      }
      aIS = (InputStream) SX.sxGlobalClassReference.getResourceAsStream(content);
      if (aIS == null) {
        throw new IOException("resource not accessible");
      }
      File out = outFile.isEmpty() ? new File(outDir, inFile) : new File(outDir, inFile);
      if (!out.getParentFile().exists()) {
        out.getParentFile().mkdirs();
      }
      aFileOS = new FileOutputStream(out);
      copy(aIS, aFileOS);
      aIS.close();
      aFileOS.close();
    } catch (Exception ex) {
      log.error("extractResourceToFile: %s (%s)", content, ex);
      return false;
    }
    return true;
  }

  /**
   * store the content of a resource found on classpath in the returned string
   *
   * @param inPrefix a subtree from root found in classpath (leading /)
   * @param inFile   the filename combined with the prefix on classpath
   * @return file content in UTF-8 encoding
   */
  public static String extractResourceToString(String inPrefix, String inFile) {
    return extractResourceToString(inPrefix, inFile, "");
  }

  /**
   * store the content of a resource found on classpath in the returned string
   *
   * @param inPrefix a subtree from root found in classpath (leading /)
   * @param inFile   the filename combined with the prefix on classpath
   * @param encoding
   * @return file content
   */
  private static String extractResourceToString(String inPrefix, String inFile, String encoding) {
    InputStream aIS = null;
    String out = null;
    String content = inPrefix + "/" + inFile;
    if (!content.startsWith("/")) {
      content = "/" + content;
    }
    try {
      content = SX.isWindows() ? content.replace("\\", "/") : content;
      aIS = (InputStream) SX.sxGlobalClassReference.getResourceAsStream(content);
      if (aIS != null) {
        if (encoding == null || encoding.isEmpty()) {
          encoding = "UTF-8";
          out = new String(copy(aIS), "UTF-8");
        } else {
          out = new String(copy(aIS), encoding);
        }
        aIS.close();
      }
      aIS = null;
    } catch (Exception ex) {
      log.error("extractResourceToString error: %s from: %s (%s)", encoding, content, ex);
    }
    try {
      if (aIS != null) {
        aIS.close();
      }
    } catch (Exception ex) {
    }
    return out;
  }

  private static URL evalResourceLocation(String folderOrFile) {
    log.debug("resourceLocation: (%s) %s", SX.sxGlobalClassReference, folderOrFile);
    if (!folderOrFile.startsWith("/")) {
      folderOrFile = "/" + folderOrFile;
    }
    return SX.sxGlobalClassReference.getResource(folderOrFile);
  }

  private static List<String> makeResourceList(String folder, FilenameFilter filter) {
    log.debug("resourceList: enter");
    List<String> files = new ArrayList<String>();
    if (!folder.startsWith("/")) {
      folder = "/" + folder;
    }
    URL uFolder = evalResourceLocation(folder);
    if (uFolder == null) {
      log.debug("resourceList: not found: %s", folder);
      return files;
    }
    try {
      uFolder = new URL(uFolder.toExternalForm().replaceAll(" ", "%20"));
    } catch (Exception ex) {
    }
    URL uContentList = SX.sxGlobalClassReference.getResource(folder + "/" + fpContent);
    if (uContentList != null) {
      return doResourceListWithList(folder, files, filter);
    }
    File fFolder = null;
    try {
      fFolder = new File(uFolder.toURI());
      log.debug("resourceList: having folder:\n%s", fFolder);
      String sFolder = normalizeAbsolute(fFolder.getPath(), false);
      if (":".equals(sFolder.substring(2, 3))) {
        sFolder = sFolder.substring(1);
      }
      files.add(sFolder);
      files = doResourceListFolder(new File(sFolder), files, filter);
      files.remove(0);
      return files;
    } catch (Exception ex) {
      if (!"jar".equals(uFolder.getProtocol())) {
        log.debug("resourceList:\n%s", folder);
        log.error("resourceList: URL neither folder nor jar:\n%s", ex);
        return null;
      }
    }
    String[] parts = uFolder.getPath().split("!");
    if (parts.length < 2 || !parts[0].startsWith("file:")) {
      log.debug("resourceList:\n%s", folder);
      log.error("resourceList: not a valid jar URL:\n" + uFolder.getPath());
      return null;
    }
    String fpFolder = parts[1];
    log.debug("resourceList: having jar:\n%s", uFolder);
    return doResourceListJar(uFolder, fpFolder, files, filter);
  }

  private static List<String> doResourceListFolder(File fFolder, List<String> files, FilenameFilter filter) {
    int localLevel = lvl + 1;
    String subFolder = "";
    if (fFolder.isDirectory()) {
      if (!equalsPath(fFolder.getPath(), files.get(0))) {
        subFolder = fFolder.getPath().substring(files.get(0).length() + 1).replace("\\", "/") + "/";
        if (filter != null && !filter.accept(new File(files.get(0), subFolder), "")) {
          return files;
        }
      } else {
        log.trace("scanning folder:\n%s", fFolder);
        subFolder = "/";
        files.add(subFolder);
      }
      String[] subList = fFolder.list();
      for (String entry : subList) {
        File fEntry = new File(fFolder, entry);
        if (fEntry.isDirectory()) {
          files.add(fEntry.getAbsolutePath().substring(1 + files.get(0).length()).replace("\\", "/") + "/");
          doResourceListFolder(fEntry, files, filter);
          files.add(subFolder);
        } else {
          if (filter != null && !filter.accept(fFolder, entry)) {
            continue;
          }
          log.trace("from %s adding: %s", (subFolder.isEmpty() ? "." : subFolder), entry);
          files.add(fEntry.getAbsolutePath().substring(1 + fFolder.getPath().length()));
        }
      }
    }
    return files;
  }

  private static List<String> doResourceListWithList(String folder, List<String> files, FilenameFilter filter) {
    String content = extractResourceToString(folder, fpContent, "");
    String[] contentList = content.split(content.indexOf("\r") != -1 ? "\r\n" : "\n");
    if (filter == null) {
      files.addAll(Arrays.asList(contentList));
    } else {
      for (String fpFile : contentList) {
        if (filter.accept(new File(fpFile), "")) {
          files.add(fpFile);
        }
      }
    }
    return files;
  }

  private static List<String> doResourceListJar(URL uJar, String fpResource, List<String> files, FilenameFilter filter) {
    int localLevel = lvl + 1;
    ZipInputStream zJar;
    String fpJar = uJar.getPath().split("!")[0];
    String fileSep = "/";
    if (!fpJar.endsWith(".jar")) {
      return files;
    }
    log.trace("scanning jar:\n%s", uJar);
    fpResource = fpResource.startsWith("/") ? fpResource.substring(1) : fpResource;
    File fFolder = new File(fpResource);
    File fSubFolder = null;
    ZipEntry zEntry;
    String subFolder = "";
    boolean skip = false;
    try {
      zJar = new ZipInputStream(new URL(fpJar).openStream());
      while ((zEntry = zJar.getNextEntry()) != null) {
        if (zEntry.getName().endsWith("/")) {
          continue;
        }
        String zePath = zEntry.getName();
        if (zePath.startsWith(fpResource)) {
          if (fpResource.length() == zePath.length()) {
            files.add(zePath);
            return files;
          }
          String zeName = zePath.substring(fpResource.length() + 1);
          int nSep = zeName.lastIndexOf(fileSep);
          String zefName = zeName.substring(nSep + 1, zeName.length());
          String zeSub = "";
          if (nSep > -1) {
            zeSub = zeName.substring(0, nSep + 1);
            if (!subFolder.equals(zeSub)) {
              subFolder = zeSub;
              fSubFolder = new File(fFolder, subFolder);
              skip = false;
              if (filter != null && !filter.accept(fSubFolder, "")) {
                skip = true;
                continue;
              }
              files.add(zeSub);
            }
            if (skip) {
              continue;
            }
          } else {
            if (!subFolder.isEmpty()) {
              subFolder = "";
              fSubFolder = fFolder;
              files.add("/");
            }
          }
          if (filter != null && !filter.accept(fSubFolder, zefName)) {
            continue;
          }
          files.add(zefName);
          log.trace("from %s adding: %s", (zeSub.isEmpty() ? "." : zeSub), zefName);
        }
      }
    } catch (Exception ex) {
      log.error("doResourceListJar: %s", ex);
      return files;
    }
    return files;
  }

  /**
   * export all resource files from the given subtree in given jar to the given folder retaining the subtree
   *
   * @param aJar         absolute path to an existing jar or a string identifying the jar on classpath (no leading /)
   * @param fpRessources path of the subtree or file relative to root
   * @param fFolder      folder where to export (if null, only list - no export)
   * @param filter       implementation of interface FilenameFilter or null for no filtering
   * @return the filtered list of files (compact sikulixcontent format)
   */
  public static List<String> extractResourcesToFolderFromJar(String aJar, String fpRessources, File fFolder, FilenameFilter filter) {
    List<String> content = new ArrayList<String>();
    File faJar = new File(aJar);
    URL uaJar = null;
    fpRessources = slashify(fpRessources, false);
    if (faJar.isAbsolute()) {
      if (!faJar.exists()) {
        log.error("extractResourcesToFolderFromJar: does not exist: %s", faJar);
        return null;
      }
      try {
        uaJar = new URL("jar", null, "file:" + aJar);
        log.info("%s", uaJar);
      } catch (MalformedURLException ex) {
        log.error("extractResourcesToFolderFromJar: bad URL for: %s", faJar);
        return null;
      }
    } else {
      uaJar = getClasspath(aJar);
      if (uaJar == null) {
        log.error("extractResourcesToFolderFromJar: not on classpath: %s", aJar);
        return null;
      }
      try {
        String sJar = "file:" + uaJar.getPath() + "!/";
        uaJar = new URL("jar", null, sJar);
      } catch (MalformedURLException ex) {
        log.error("extractResourcesToFolderFromJar: bad URL for: %s", uaJar);
        return null;
      }
    }
    content = doResourceListJar(uaJar, fpRessources, content, filter);
    if (fFolder == null) {
      return content;
    }
    copyFromJarToFolderWithList(uaJar, fpRessources, content, fFolder);
    return content;
  }

  /**
   * write the list as it is produced by calling extractResourcesToFolder to the given file with system line
   * separator<br>
   * non-compact format: every file with full path
   *
   * @param folder path of the subtree relative to root with leading /
   * @param target the file to write the list (if null, only list - no file)
   * @param filter implementation of interface FilenameFilter or null for no filtering
   * @return success
   */
  public static String[] resourceListAsFile(String folder, File target, FilenameFilter filter) {
    String content = resourceListAsString(folder, filter);
    if (content == null) {
      log.error("resourceListAsFile: did not work: %s", folder);
      return null;
    }
    if (target != null) {
      try {
        deleteFileOrFolder(target.getAbsolutePath());
        target.getParentFile().mkdirs();
        PrintWriter aPW = new PrintWriter(target);
        aPW.write(content);
        aPW.close();
      } catch (Exception ex) {
        log.error("resourceListAsFile: %s:\n%s", target, ex);
      }
    }
    return content.split(System.getProperty("line.separator"));
  }

  /**
   * write the list as it is produced by calling extractResourcesToFolder to the given file with system line
   * separator<br>
   * compact sikulixcontent format
   *
   * @param folder       path of the subtree relative to root with leading /
   * @param targetFolder the folder where to store the file sikulixcontent (if null, only list - no export)
   * @param filter       implementation of interface FilenameFilter or null for no filtering
   * @return success
   */
  public static String[] resourceListAsSXContent(String folder, File targetFolder, FilenameFilter filter) {
    List<String> contentList = makeResourceList(folder, filter);
    if (contentList == null) {
      log.error("resourceListAsSXContent: did not work: %s", folder);
      return null;
    }
    File target = null;
    String arrString[] = new String[contentList.size()];
    try {
      PrintWriter aPW = null;
      if (targetFolder != null) {
        target = new File(targetFolder, fpContent);
        deleteFileOrFolder(target);
        target.getParentFile().mkdirs();
        aPW = new PrintWriter(target);
      }
      int n = 0;
      for (String line : contentList) {
        arrString[n++] = line;
        if (targetFolder != null) {
          aPW.println(line);
        }
      }
      if (targetFolder != null) {
        aPW.close();
      }
    } catch (Exception ex) {
      log.error("resourceListAsFile: %s:\n%s", target, ex);
    }
    return arrString;
  }

  /**
   * write the list as it is produced by calling extractResourcesToFolder to the given file with system line
   * separator<br>
   * compact sikulixcontent format
   *
   * @param aJar         absolute path to an existing jar or a string identifying the jar on classpath (no leading /)
   * @param folder       path of the subtree relative to root with leading /
   * @param targetFolder the folder where to store the file sikulixcontent (if null, only list - no export)
   * @param filter       implementation of interface FilenameFilter or null for no filtering
   * @return success
   */
  public static String[] resourceListAsSXContentFromJar(String aJar, String folder, File targetFolder, FilenameFilter filter) {
    List<String> contentList = extractResourcesToFolderFromJar(aJar, folder, null, filter);
    if (contentList == null || contentList.size() == 0) {
      log.error("resourceListAsSXContentFromJar: did not work: %s", folder);
      return null;
    }
    File target = null;
    String arrString[] = new String[contentList.size()];
    try {
      PrintWriter aPW = null;
      if (targetFolder != null) {
        target = new File(targetFolder, fpContent);
        deleteFileOrFolder(target);
        target.getParentFile().mkdirs();
        aPW = new PrintWriter(target);
      }
      int n = 0;
      for (String line : contentList) {
        arrString[n++] = line;
        if (targetFolder != null) {
          aPW.println(line);
        }
      }
      if (targetFolder != null) {
        aPW.close();
      }
    } catch (Exception ex) {
      log.error("resourceListAsFile: %s:\n%s", target, ex);
    }
    return arrString;
  }

  /**
   * write the list produced by calling extractResourcesToFolder to the returned string with system line separator<br>
   * non-compact format: every file with full path
   *
   * @param folder path of the subtree relative to root with leading /
   * @param filter implementation of interface FilenameFilter or null for no filtering
   * @return the resulting string
   */
  public static String resourceListAsString(String folder, FilenameFilter filter) {
    return resourceListAsString(folder, filter, null);
  }

  /**
   * write the list produced by calling extractResourcesToFolder to the returned string with given separator<br>
   * non-compact format: every file with full path
   *
   * @param folder    path of the subtree relative to root with leading /
   * @param filter    implementation of interface FilenameFilter or null for no filtering
   * @param separator to be used to separate the entries
   * @return the resulting string
   */
  public static String resourceListAsString(String folder, FilenameFilter filter, String separator) {
    List<String> aList = makeResourceList(folder, filter);
    if (aList == null) {
      return null;
    }
    if (separator == null) {
      separator = System.getProperty("line.separator");
    }
    String out = "";
    String subFolder = "";
    if (aList != null && aList.size() > 0) {
      for (String eFile : aList) {
        if (eFile == null) {
          continue;
        }
        if (eFile.endsWith("/")) {
          subFolder = eFile.substring(0, eFile.length() - 1);
          continue;
        }
        if (!subFolder.isEmpty()) {
          eFile = new File(subFolder, eFile).getPath();
        }
        out += eFile.replace("\\", "/") + separator;
      }
    }
    return out;
  }

  public static boolean copyFromJarToFolderWithList(URL uJar, String fpRessource, List<String> files, File fFolder) {
    if (files == null || files.isEmpty()) {
      log.debug("copyFromJarToFolderWithList: list of files is empty");
      return false;
    }
    String fpJar = uJar.getPath().split("!")[0];
    if (!fpJar.endsWith(".jar")) {
      return false;
    }
    log.trace("scanning jar:\n%s", uJar);
    fpRessource = fpRessource.startsWith("/") ? fpRessource.substring(1) : fpRessource;

    String subFolder = "";

    int maxFiles = files.size() - 1;
    int nFiles = 0;

    ZipEntry zEntry;
    ZipInputStream zJar;
    String zPath;
    int prefix = fpRessource.length();
    fpRessource += !fpRessource.isEmpty() ? "/" : "";
    String current = "/";
    boolean shouldStop = false;
    try {
      zJar = new ZipInputStream(new URL(fpJar).openStream());
      while ((zEntry = zJar.getNextEntry()) != null) {
        zPath = zEntry.getName();
        if (zPath.endsWith("/")) {
          continue;
        }
        while (current.endsWith("/")) {
          if (nFiles > maxFiles) {
            shouldStop = true;
            break;
          }
          subFolder = current.length() == 1 ? "" : current;
          current = files.get(nFiles++);
          if (!current.endsWith("/")) {
            current = fpRessource + subFolder + current;
            break;
          }
        }
        if (shouldStop) {
          break;
        }
        if (zPath.startsWith(current)) {
          if (zPath.length() == fpRessource.length() - 1) {
            log.error("extractResourcesToFolderFromJar: only ressource folders allowed - lock filter");
            return false;
          }
          log.trace("copying: %s", zPath);
          File out = new File(fFolder, zPath.substring(prefix));
          if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
          }
          FileOutputStream aFileOS = new FileOutputStream(out);
          copy(zJar, aFileOS);
          aFileOS.close();
          if (nFiles > maxFiles) {
            break;
          }
          current = files.get(nFiles++);
          if (!current.endsWith("/")) {
            current = fpRessource + subFolder + current;
          }
        }
      }
      zJar.close();
    } catch (Exception ex) {
      log.error("doResourceListJar: %s", ex);
      return false;
    }
    return true;
  }

  public static String extractResourceAsLines(String src) {
    String res = null;
    ClassLoader cl = Content.class.getClassLoader();
    InputStream isContent = cl.getResourceAsStream(src);
    if (isContent != null) {
      res = "";
      String line;
      try {
        BufferedReader cnt = new BufferedReader(new InputStreamReader(isContent));
        line = cnt.readLine();
        while (line != null) {
          res += line + "\n";
          line = cnt.readLine();
        }
        cnt.close();
      } catch (Exception ex) {
        log.error("extractResourceAsLines: %s\n%s", src, ex);
      }
    }
    return res;
  }

  public static boolean extractResource(String src, File tgt) {
    ClassLoader cl = Content.class.getClassLoader();
    InputStream isContent = cl.getResourceAsStream(src);
    if (isContent != null) {
      try {
        log.trace("extractResource: %s to %s", src, tgt);
        tgt.getParentFile().mkdirs();
        OutputStream osTgt = new FileOutputStream(tgt);
        bufferedWrite(isContent, osTgt);
        osTgt.close();
      } catch (Exception ex) {
        log.error("extractResource:\n%s", src, ex);
        return false;
      }
    } else {
      return false;
    }
    return true;
  }
  //</editor-fold>

  //<editor-fold desc="037*** download">
  public static final int DOWNLOAD_BUFFER_SIZE = 153600;
  private static SplashFrame _progress = null;
  private static final String EXECUTABLE = "#executable";

  static final String fpContent = "sikulixcontent";

  public static int tryGetFileSize(URL aUrl) {
    HttpURLConnection conn = null;
    try {
      if (getProxy() != null) {
        conn = (HttpURLConnection) aUrl.openConnection(getProxy());
      } else {
        conn = (HttpURLConnection) aUrl.openConnection();
      }
      conn.setConnectTimeout(30000);
      conn.setReadTimeout(30000);
      conn.setRequestMethod("HEAD");
      conn.getInputStream();
      return conn.getContentLength();
    } catch (Exception ex) {
      return 0;
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  public static int isUrlUseabel(String sURL) {
    try {
      return isUrlUseabel(new URL(sURL));
    } catch (Exception ex) {
      return -1;
    }
  }

  public static int isUrlUseabel(URL aURL) {
    HttpURLConnection conn = null;
    try {
//			HttpURLConnection.setFollowRedirects(false);
      if (getProxy() != null) {
        conn = (HttpURLConnection) aURL.openConnection(getProxy());
      } else {
        conn = (HttpURLConnection) aURL.openConnection();
      }
//			con.setInstanceFollowRedirects(false);
      conn.setRequestMethod("HEAD");
      int retval = conn.getResponseCode();
//				HttpURLConnection.HTTP_BAD_METHOD 405
//				HttpURLConnection.HTTP_NOT_FOUND 404
      if (retval == HttpURLConnection.HTTP_OK) {
        return 1;
      } else if (retval == HttpURLConnection.HTTP_NOT_FOUND) {
        return 0;
      } else if (retval == HttpURLConnection.HTTP_FORBIDDEN) {
        return 0;
      } else {
        return -1;
      }
    } catch (Exception ex) {
      return -1;
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  /**
   * download a file at the given url to a local folder
   *
   * @param url       a valid url
   * @param localPath the folder where the file should go (will be created if necessary)
   * @return the absolute path to the downloaded file or null on any error
   */
  public static String downloadURL(URL url, String localPath) {
    String[] path = url.getPath().split("/");
    String filename = path[path.length - 1];
    String targetPath = null;
    int srcLength = 1;
    int srcLengthKB = 0;
    int done;
    int totalBytesRead = 0;
    File fullpath = new File(localPath);
    if (fullpath.exists()) {
      if (fullpath.isFile()) {
        log.error("downloadURL: target path must be a folder:\n%s", localPath);
        fullpath = null;
      }
    } else {
      if (!fullpath.mkdirs()) {
        log.error("downloadURL: could not create target folder:\n%s", localPath);
        fullpath = null;
      }
    }
    if (fullpath != null) {
      srcLength = tryGetFileSize(url);
      srcLengthKB = (int) (srcLength / 1024);
      log.trace("downloadURL: %s", url);
      fullpath = new File(localPath, filename);
      targetPath = fullpath.getAbsolutePath();
      done = 0;
      if (_progress != null) {
        _progress.setProFile(filename);
        _progress.setProSize(srcLengthKB);
        _progress.setProDone(0);
        _progress.setVisible(true);
      }
      InputStream reader = null;
      FileOutputStream writer = null;
      try {
        writer = new FileOutputStream(fullpath);
        if (getProxy() != null) {
          reader = url.openConnection(getProxy()).getInputStream();
        } else {
          reader = url.openConnection().getInputStream();
        }
        byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
        int bytesRead = 0;
        long begin_t = (new Date()).getTime();
        long chunk = (new Date()).getTime();
        while ((bytesRead = reader.read(buffer)) > 0) {
          writer.write(buffer, 0, bytesRead);
          totalBytesRead += bytesRead;
          if (srcLength > 0) {
            done = (int) ((totalBytesRead / (double) srcLength) * 100);
          } else {
            done = (int) (totalBytesRead / 1024);
          }
          if (((new Date()).getTime() - chunk) > 1000) {
            if (_progress != null) {
              _progress.setProDone(done);
            }
            chunk = (new Date()).getTime();
          }
        }
        writer.close();
        log.trace("downloadURL: %d KB %.2f secs",
            (totalBytesRead / 1024),
            ((new Date()).getTime() - begin_t) / 1000.0);
      } catch (Exception ex) {
        log.error("problems while downloading\n%s", ex);
        targetPath = null;
      } finally {
        if (reader != null) {
          try {
            reader.close();
          } catch (IOException ex) {
          }
        }
        if (writer != null) {
          try {
            writer.close();
          } catch (IOException ex) {
          }
        }
      }
      if (_progress != null) {
        if (targetPath == null) {
          _progress.setProDone(-1);
        } else {
          if (srcLength <= 0) {
            _progress.setProSize((int) (totalBytesRead / 1024));
          }
          _progress.setProDone(100);
        }
        _progress.closeAfter(3);
        _progress = null;
      }
    }
    if (targetPath == null) {
      fullpath.delete();
    }
    return targetPath;
  }

  /**
   * download a file at the given url to a local folder
   *
   * @param url       a string representing a valid url
   * @param localPath the folder where the file should go (will be created if necessary)
   * @return the absolute path to the downloaded file or null on any error
   */
  public static String downloadURL(String url, String localPath) {
    URL urlSrc = null;
    try {
      urlSrc = new URL(url);
    } catch (MalformedURLException ex) {
      log.error("download: bad URL: " + url);
      return null;
    }
    return downloadURL(urlSrc, localPath);
  }

  public static String downloadURL(String url, String localPath, JFrame progress) {
    _progress = (SplashFrame) progress;
    return downloadURL(url, localPath);
  }

  public static String downloadURLtoString(String src) {
    URL url = null;
    try {
      url = new URL(src);
    } catch (MalformedURLException ex) {
      log.error("downloadURLtoString: bad URL: %s", src);
      return null;
    }
    return downloadURLtoString(url);
  }

  public static String downloadURLtoString(URL uSrc) {
    String content = "";
    InputStream reader = null;
    log.trace("downloadURLtoString: %s,", uSrc);
    try {
      if (getProxy() != null) {
        reader = uSrc.openConnection(getProxy()).getInputStream();
      } else {
        reader = uSrc.openConnection().getInputStream();
      }
      byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
      int bytesRead = 0;
      while ((bytesRead = reader.read(buffer)) > 0) {
        content += (new String(Arrays.copyOfRange(buffer, 0, bytesRead), Charset.forName("utf-8")));
      }
    } catch (Exception ex) {
      log.error("downloadURLtoString: " + ex.getMessage());
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException ex) {
        }
      }
    }
    return content;
  }

  public static String downloadScriptToString(URL url) {
    return downloadFileToString(url, true);
  }

  public static String downloadFileToString(URL url) {
    return downloadFileToString(url, true);
  }

  public static String downloadFileToString(URL url, boolean silent) {
    HttpURLConnection httpConn = null;
    String content = "";
    try {
      httpConn = (HttpURLConnection) url.openConnection();
      if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
        InputStream inputStream = httpConn.getInputStream();
        int bytesRead = -1;
        byte[] buffer = new byte[Content.DOWNLOAD_BUFFER_SIZE];
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          content += (new String(Arrays.copyOfRange(buffer, 0, bytesRead), Charset.forName("utf-8")));
        }
        inputStream.close();
        log.trace("downloadFileToString: %s (%s)", url, httpConn.getContentType());
      } else {
        if (silent) {
          log.trace("downloadFileToString: (HTTP:%d) %s", httpConn.getResponseCode(), url);
        } else {
          log.error("downloadFileToString: (HTTP:%d) %s", httpConn.getResponseCode(), url);
        }
      }
      httpConn.disconnect();
    } catch (IOException e) {
      if (silent) {
        log.trace("downloadFileToString: (%s) %s", e.getMessage(), url);
      } else {
        log.error("downloadFileToString: (%s) %s", e.getMessage(), url);
      }
    }
    return content;
  }
  //</editor-fold>

  //<editor-fold desc="040*** extension">
  private static String sikulixMavenGroup = "com/sikulix";
  private static String sikulixVersion = "2.0.0";
  private static String pMavenRelease = "https://repo1.maven.org/maven2/";
  private static String pMavenSnapshot = "https://oss.sonatype.org/content/groups/public/";

  private static URL getMavenJarURL(String givenItem) {
    String mPath;
    String mJar = "";
    String[] parts = givenItem.split(":");
    String item = parts[0];
    String version = sikulixVersion;
    if (SX.isNotSet(item)) {
      return null;
    }
    if (parts.length > 1 && SX.isSet(parts[1])) {
      version = parts[1];
    }
    boolean isSnapshot = parts.length > 2;
    mPath = pMavenRelease + String.format("%s/%s/%s/", sikulixMavenGroup, item, version);
    mJar = String.format("%s-%s.jar", item, version);
    if (isSnapshot) {
      String mavenSnapshotPrefix = String.format("%s/%s/%s-SNAPSHOT/", sikulixMavenGroup, item, version);
      mPath = pMavenSnapshot + mavenSnapshotPrefix;
      String metadata = Content.downloadURLtoString(mPath + "maven-metadata.xml");
      String timeStamp = "";
      String buildNumber = "";
      if (metadata != null && !metadata.isEmpty()) {
        Matcher m = Pattern.compile("<timestamp>(.*?)</timestamp>").matcher(metadata);
        if (m.find()) {
          timeStamp = m.group(1);
          m = Pattern.compile("<buildNumber>(.*?)</buildNumber>").matcher(metadata);
          if (m.find()) {
            buildNumber = m.group(1);
          }
        }
      }
      if (!timeStamp.isEmpty() && !buildNumber.isEmpty()) {
        mJar = String.format("%s-%s-%s-%s.jar", item, version, timeStamp, buildNumber);
        log.trace("getMavenJar: %s", mJar);
      } else {
        log.error("Maven download: could not get timestamp nor buildnumber for %s from:"
            + "\n%s\nwith content:\n%s", givenItem, mPath, metadata);
        return null;
      }
    }
    return asURL(mPath, mJar);
  }

  public static boolean addExtensionFromMaven(String extension) {
    String sxextensions = SX.getOption("sxextensions");
    if (sxextensions.contains(extension)) {
      String sxextension = SX.getOption("sxextension." + extension);
      if (SX.isSet(sxextension)) {
        String[] parts = sxextension.split(",");
        if (parts.length == 4) {
          return addExtensionFromMaven(String.format("%s:%s:%s",
              parts[0].trim(), parts[1].trim(), parts[2].trim()), parts[3].trim());
        }
      }
    }
    return false;
  }

  private static boolean addExtensionFromMaven(String item, String clazz) {
    if (SX.isSet(whereIs(clazz))) {
      return true;
    }
    URL mavenJarURL = getMavenJarURL(item);
    if (SX.isSet(mavenJarURL)) {
      log.trace("addExtensionFromMaven: %s", mavenJarURL);
      String localJar = Content.downloadURL(mavenJarURL, SX.getSXEXTENSIONSFOLDER());
      if (SX.isSet(localJar)) {
        if (addClasspath(localJar)) {
          return SX.isSet(whereIs(clazz));
        }
      }
    }
    return false;
  }
  //</editor-fold>

  public static class ImagePath extends SXPathList {
    private ImagePath() {
      super(asURL(SX.getSXIMAGES()));
    }

    boolean init(Object... args) {
      if (args.length > 0) {
        URL url = asURL(args);
        if (SX.isNotNull(url)) {
          set(0, url);
        } else {
          return false;
        }
      }
      return true;
    }

    public boolean bundlePathIsFile() {
      URL url = getURL(0);
      if (SX.isNull(url)) {
        return false;
      }
      return "file".equals(url.getProtocol());
    }
  }

  private static ImagePath imagePath = null;

  public static ImagePath getImagePath() {
    if (SX.isNull(imagePath)) {
      imagePath = new ImagePath();
    }
    return imagePath;
  }

  private static class SXPathList {
    private final List<URL> pathList = Collections.synchronizedList(new ArrayList<URL>());

    SXPathList() {
    }

    SXPathList(URL url) {
      pathList.add(url);
    }

    public void clear() {
      pathList.clear();
    }

    public String[] getAll(String filter) {
      String[] paths = new String[pathList.size()];
      int n = 0;
      String path;
      for (URL url : pathList) {
        path = asPath(url);
        if (SX.isSet(filter) && !path.contains(filter)) {
          continue;
        }
        paths[n++] = path;
      }
      return paths;
    }

    public String[] getAll() {
      return getAll("");
    }

    List<URL> all() {
      return pathList;
    }

    public void dump() {
      String[] paths = getAll("");
      int n = 0;
      for (String path : paths) {
        log.p("***** Path (%d): %s", n, path);
        n++;
      }
    }

    public int add(Object... args) {
      if (args.length == 0) {
        return -1;
      }
      URL urlPath = asURL(args);
      if (SX.isSet(urlPath)) {
        int exists = get(urlPath);
        if (exists > -1) {
          return exists;
        }
        pathList.add(urlPath);
        return pathList.size() - 1;
      }
      return -1;
    }

    public String get(int n) {
      if (n < 0 || n > pathList.size() - 1) {
        return "";
      }
      return asPath(pathList.get(n));
    }

    public URL getURL(int n) {
      if (n < 0 || n > pathList.size() - 1) {
        return null;
      }
      return pathList.get(n);
    }

    public int get(Object args) {
      URL given = asURL(args);
      if (SX.isNotSet(given)) {
        return -1;
      }
      int n = 0;
      for (URL path : pathList) {
        if (given.sameFile(path)) {
          return n;
        }
        n++;
      }
      return -1;
    }

    public boolean set(int n, Object path) {
      if (SX.isNull(path)) {
        return false;
      }
      if (n < 0 || n > pathList.size() - 1) {
        return false;
      }
      URL urlPath = path instanceof URL ? ((URL) path) : asURL(path);
      if (SX.isSet(urlPath)) {
        pathList.set(n, urlPath);
        return true;
      }
      return false;
    }

    public String remove(int n) {
      if (n < 0 || n > pathList.size() - 1) {
        return "";
      }
      return asPath(pathList.remove(n));
    }
  }

}
