/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.test;

import com.sikulix.core.Content;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCoreContent {

  static SXLog log = SX.getSXLog("SX_TestCoreContent");

  @BeforeClass
  public static void setUpClass() {
    log.on(SXLog.INFO);
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
    result = "";
  }

  @After
  public void tearDown() {
    log.info("%s (%s)", currentTest, result);
  }

  private String currentTest;
  private String result;

  @Test
  public void test_000_play() {
    currentTest = TestHelper.methodEntryNotOnTravis();
    if (TestHelper.shouldNotRun(currentTest)) {
      return;
    }
    Content.ImagePath imagePath = Content.getImagePath();
    imagePath.add("com.sikulix.testjar.Testjar");
    String[] paths = imagePath.getAll();
    for (String path: paths) {
      log.p("%s", path);
    }
  }

  @Test
  public void test_001_startup_userwork() {
    currentTest = TestHelper.methodEntry();
    if (TestHelper.shouldNotRun(currentTest)) {
      return;
    }
    result = SX.getSXUSERWORK();
    if (SX.isNotSet(result)) {
      SX.show();
      assert false : TestHelper.testError("null or not there: %s", result);
    }
  }

  @Test
  public void test_008_startup_native_load() {
    currentTest = "test_011_startup_native_load";
    File fsxnative = new File(SX.getSXNATIVE());
    Content.deleteFileOrFolder(fsxnative);
    assert !fsxnative.exists() : TestHelper.testError("not deleted: %s", fsxnative);
    SX.loadNative(SX.NATIVES.OPENCV);
    File test = new File(SX.getSXNATIVE(), SX.sxLibsCheckName);
    assert test.exists() : TestHelper.testError("not loaded: %s", test);
    result = SX.sxLibsCheckName;
  }

  @Test
  public void test_010_asImageFilename() {
    currentTest = TestHelper.methodEntry();
    if (TestHelper.shouldNotRun(currentTest)) {
      return;
    }
    Map<String, String> testCases = TestHelper.makeTestCasesWithExpected("test", "test.png", ".test", ".test.png",
              "test.jpg", "test.jpg", ".test.jpg", ".test.jpg", ".test.jpg", ".test.jpg", "test.xxx", "test.xxx",
              ".test.xxx", ".test.xxx", ".test.xxx", ".test.xxx");
    for (String given : testCases.keySet()) {
      String fName = Content.asImageFilename(given);
      String expected = testCases.get(given);
      assert fName.equals(expected) : TestHelper.testError("given(%s) result(%s) expected(%s)", given, fName, expected);
      result += String.format("[%s, %s] ", given, fName);
    }
  }

  @Test
  public void test_050_onClassPath() {
    currentTest = TestHelper.methodEntry();
    if (TestHelper.shouldNotRun(currentTest)) {
      return;
    }
    List<String> testCases = TestHelper.makeTestCases("testjar", "target", "test-");
    for (String given : testCases) {
      URL expectedURL = Content.onClasspath(given);
      if (SX.isNull(expectedURL)) {
        Content.dumpClasspath();
      }
      assert SX.isNotNull(expectedURL) : TestHelper.testError("not found: %s", given);
      assert expectedURL.getProtocol().equals("file") : "url protocol not file";
      result += String.format("[found: %s] ", given);
      assert Content.isOnClasspath(expectedURL);
      String path = expectedURL.getPath();
      String name = new File(expectedURL.getPath()).getName();
      result += String.format("[on classpath as: %s] ", path.endsWith("jar") ? name : path);
    }
  }
}
