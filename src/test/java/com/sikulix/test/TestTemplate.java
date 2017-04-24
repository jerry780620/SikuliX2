/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.test;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.SXTest;
import org.junit.*;
import org.junit.runners.MethodSorters;

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
    log.info("%s", currentTest);
  }

  private SXTest currentTest;

  @Ignore
  public void test_000_template() {
    currentTest = new SXTest();
    currentTest = new SXTest().onlyLocal();
    if (currentTest.shouldNotRun()) {
      return;
    }
    currentTest.setResult("test template");
  }
}
