/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.test;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTemplate {

  static SXLog log = SX.getSXLog("SX_TestTemplate");

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
    log.info("%s (%s)", currentTest, result);
  }

  private String currentTest;
  private String result;

  private void methodEntry() {
    currentTest = Thread.currentThread().getStackTrace()[2].getMethodName();
    result = "";
  }

  @Ignore
  public void test_000_template() {
    currentTest = TestHelper.methodEntry();
    if (TestHelper.shouldNotRun(currentTest)) {
      return;
    }
    result = "test template";
  }
}
