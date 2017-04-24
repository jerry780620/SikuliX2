/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.test;

import com.sikulix.core.*;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCoreContent {

  static SXLog log = SX.getSXLog("SX_TestCoreContent");

  private SXTest currentTest;

  @BeforeClass
  public static void setUpClass() {
    log.on(SXLog.INFO);
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
    log.info("!%s", currentTest);
  }

  @Test
  public void test_000_play() {
    currentTest = new SXTest();
    currentTest = new SXTest().onlyLocal();
    if (currentTest.shouldNotRun()) {
      return;
    }
    currentTest.setResult("test template");
  }

  @Test
  public void test_001_startup_userwork() {
    currentTest = new SXTest();
    if (currentTest.shouldNotRun()) {
      return;
    }
    String expected = SX.getSXUSERWORK();
    currentTest.setResult(expected);
    if (SX.isNotSet(expected)) {
      SX.show();
      assert false : currentTest.failed("null or not there: %s", expected);
    }
  }

  @Test
  public void test_008_startup_native_load() {
    currentTest = new SXTest();
    if (currentTest.shouldNotRun()) {
      return;
    }
    File fsxnative = new File(SX.getSXNATIVE());
    Content.deleteFileOrFolder(fsxnative);
    assert !fsxnative.exists() : currentTest.failed("not deleted: %s", fsxnative);
    SX.loadNative(SX.NATIVES.OPENCV);
    File test = new File(SX.getSXNATIVE(), SX.sxLibsCheckName);
    assert test.exists() : currentTest.failed("not loaded: %s", test);
    currentTest.setResult(SX.sxLibsCheckName);
  }

  @Test
  public void test_020_asImageFilename() {
    currentTest = new SXTest();
    if (currentTest.shouldNotRun()) {
      return;
    }
    Map<String, String> testCases = new HashMap<>();
    testCases.put("test", "test.png");
    testCases.put(".test", ".test.png");
    testCases.put("test.jpg", "test.jpg");
    testCases.put(".test.jpg", ".test.jpg");
    testCases.put(".test.jpg", ".test.jpg");
    testCases.put("test.xxx", "test.xxx");
    testCases.put(".test.xxx", ".test.xxx");
    testCases.put(".test.xxx", ".test.xxx");
    for (String given : testCases.keySet()) {
      String fName = Content.asImageFilename(given);
      String expected = testCases.get(given);
      assert fName.equals(expected) :
              currentTest.failed("given(%s) result(%s) expected(%s)", given, fName, expected);
      currentTest.addResult(String.format("[%s, %s]", given, fName));
    }
  }

  @Test
  public void test_021_asURL() {
    currentTest = new SXTest();
    currentTest = new SXTest().onlyLocal();
    if (currentTest.shouldNotRun()) {
      return;
    }
    Map<Object, URL> testCases = new HashMap<>();
    testCases.put(SX.getSXIMAGES(), null);
    testCases.put(SXTest.jarImagePathDefault, null);
    testCases.put(SXTest.jarImagePathClass, null);
    testCases.put(SXTest.gitImagePath, null);
    for (Object given : testCases.keySet()) {
      URL expected = testCases.get(given);
      URL path = Content.asURL(given);
      URL pathSub = Content.asURL(path, "/sub1///sub2/", "/sub3//");
      currentTest.addResult(String.format("\n*** testcase: %s\n%s\n%s", given, path, pathSub));
    }
  }

  @Test
  public void test_050_onClassPath() {
    currentTest = new SXTest();
    if (currentTest.shouldNotRun()) {
      return;
    }
    List<String> testCases = new ArrayList<>();
    testCases.add("testjar");
    testCases.add("target");
    testCases.add("test-");
    for (String given : testCases) {
      URL expectedURL = Content.onClasspath(given);
      if (SX.isNull(expectedURL)) {
        Content.dumpClasspath();
      }
      assert SX.isNotNull(expectedURL) :
              currentTest.failed("not found: %s", given);
      assert expectedURL.getProtocol().equals("file") :
              currentTest.failed("url protocol not file: %s", expectedURL);
      currentTest.addResult("[found: %s]", given);
      assert Content.isOnClasspath(expectedURL) :
              currentTest.failed("not on classpath: %s", expectedURL);
      String path = expectedURL.getPath();
      String name = new File(expectedURL.getPath()).getName();
      currentTest.addResult("[classpath: %s],", path.endsWith("jar") ? name : path);
    }
  }

  @Test
  public void test_060_getBundlePath() {
    currentTest = new SXTest();
    if (currentTest.shouldNotRun()) {
      return;
    }
    Content.clearImagePath();
    String expected = Content.getBundlePath();
    currentTest.setResult(expected);
    assert Content.existsFile(expected) : currentTest.failed();
  }

  @Test
  public void test_061_setBundlePathFile() {
    currentTest = new SXTest();
    if (currentTest.shouldNotRun()) {
      return;
    }
    Content.clearImagePath();
    boolean success = Content.setBundlePath(SXTest.mavenRoot, SXTest.defaultImagePath);
    String expected = Content.getBundlePath();
    currentTest.setResult(expected);
    assert success && Content.existsFile(expected) : currentTest.failed();
  }

  @Test
  public void test_062_setBundlePathByClass() {
    currentTest = new SXTest();
    if (currentTest.shouldNotRun()) {
      return;
    }
    Content.clearImagePath();
    assert Content.setBundlePath(SXTest.jarImagePathDefault) :
            currentTest.failed("not found in classpath: %s", SXTest.jarImagePathDefault);
    String expected = Content.getBundlePath();
    currentTest.setResult(expected);
    assert Content.existsFile(expected) : currentTest.failed();
  }

  @Test
  public void test_063_setBundlePathJarByClass() {
    currentTest = new SXTest();
    if (currentTest.shouldNotRun()) {
      return;
    }
    Content.clearImagePath();
    assert Content.setBundlePath(SXTest.jarImagePathClass) :
            currentTest.failed("not found in classpath: %s", SXTest.jarImagePathClass);
    String expected = Content.getBundlePath();
    currentTest.setResult(expected);
    assert expected.endsWith(".jar!/" + SXTest.defaultImagePath) : currentTest.failed();
  }

  @Test
  public void test_064_setBundlePathHttp() {
    currentTest = new SXTest();
    if (currentTest.shouldNotRun()) {
      return;
    }
    Content.clearImagePath();
    boolean success = Content.setBundlePath(SXTest.gitRoot, "src/main/resources/" + SXTest.defaultImagePath);
    String expected = Content.getBundlePath();
    currentTest.setResult(expected);
    success &= (SXTest.gitImagePath).equals(expected);
    assert success;
  }

  @Test
  public void test_065_getImagePath() {
    currentTest = new SXTest();
    if (currentTest.shouldNotRun()) {
      return;
    }
    Content.clearImagePath();
    Content.setBundlePath(SXTest.jarImagePathDefault);
    Content.getImagePath().add(SXTest.jarImagePathClass);
    Content.getImagePath().add(SXTest.gitImagePath);
    String[] paths = Content.getImagePath().getAll();
    currentTest.setResult("[");
    for (String path : paths) {
      currentTest.addResult(path + ",");
    }
    currentTest.addResult("]");
    assert 3 == paths.length;
  }

}
