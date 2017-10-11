/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.test;

import com.sikulix.core.*;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.File;
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
    Content.clearImagePath();
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
    Map<Object, String> testCases = new HashMap<>();
    testCases.put(SX.getSXIMAGES(), "/Sikulix/SX2/Images");
    testCases.put(SXTest.jarImagePathDefault, "target/classes/SX_Images");
    testCases.put(SXTest.jarImagePathClass, ".jar!/SX_Images");
    testCases.put(SXTest.gitImagePath, "https://raw.githubusercontent.com/RaiMan/SikuliX2/master/src/main/resources/SX_Images");
    String expectedSub = "/sub1/sub2/sub3";
    String sPath = "";
    for (Object given : testCases.keySet()) {
      String expected = testCases.get(given);
      URL path = Content.asURL(given);
      sPath = Content.asPath(path);
      assert sPath.endsWith(expected) :
              currentTest.failed("asURL(%s) is %s: ", given, path);
      URL pathSub = Content.asURL(path, "/sub1///sub2/", "/sub3//");
      sPath = Content.asPath(pathSub);
      assert sPath.endsWith(expected + expectedSub) :
              currentTest.failed("asURL(%s, ...) is %s: ", given, pathSub);
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
      URL expectedURL = Content.getClasspath(given);
      if (SX.isNull(expectedURL)) {
        Content.dumpClasspath();
      }
      assert SX.isNotNull(expectedURL) :
              currentTest.failed("not found: %s", given);
      assert expectedURL.getProtocol().equals("file") :
              currentTest.failed("url protocol not file: %s", expectedURL);
      currentTest.addResult("[found: %s]", given);
      assert Content.onClasspath(expectedURL) :
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
    Content.setBundlePath();
    Content.getImagePath().add(SXTest.jarImagePathDefault);
    Content.getImagePath().add(SXTest.jarImagePathClass);
    Content.getImagePath().add(SXTest.gitImagePath);
    int pos = Content.getImagePath().add(SX.getSXIMAGES());
    assert pos == 0 : currentTest.failed("ImagePath.add: duplicate added: %s", SX.getSXIMAGES());
    pos = Content.getImagePath().add(SXTest.jarImagePathDefault);
    assert pos == 1 : currentTest.failed("ImagePath.add: duplicate added: %s", SXTest.jarImagePathDefault);
    pos = Content.getImagePath().add(SXTest.jarImagePathClass);
    assert pos == 2 : currentTest.failed("ImagePath.add: duplicate added: %s", SXTest.jarImagePathClass);
    pos = Content.getImagePath().add(SXTest.gitImagePath);
    assert pos == 3 : currentTest.failed("ImagePath.add: duplicate added: %s", SXTest.gitImagePath);
    String[] paths = Content.getImagePath().getAll();
    currentTest.setResult("[");
    for (String path : paths) {
      currentTest.addResult(path + ",");
    }
    currentTest.addResult("]");
    assert 4 == paths.length;
  }

  @Test
  public void test_069_locateImageOnImagePath() {
    currentTest = new SXTest();
    if (currentTest.shouldNotRun()) {
      return;
    }
    String image = SXTest.imageNameDefault;
    String imageJar = SXTest.imageNameGoogle;
    Content.setBundlePath();
    Content.getImagePath().add(SXTest.gitImagePath);
    Content.getImagePath().add(SXTest.jarImagePathClass);
    URL url = Content.onImagePath(image);
    assert SX.isNotNull(url) :
            currentTest.failed("not on imagepath: %s", image);
    assert url.getProtocol().startsWith("http") :
            currentTest.failed("should be http: %s", image);
    currentTest.addResult(image);
    url = Content.onImagePath("foobar");
    assert SX.isNull(url) :
            currentTest.failed("should not be on imagepath: %s", "foobar");
    currentTest.addResult("!foobar");
    url = Content.onImagePath(imageJar);
    assert SX.isNotNull(url) && url.getProtocol().startsWith("jar") :
            currentTest.failed("not on imagepath: %s", imageJar);
    currentTest.addResult(imageJar);
    //currentTest.setResult("%s", url);
  }
}
