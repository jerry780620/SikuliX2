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

  private String testError(String msg, Object... args) {
    return String.format(msg, args);
  }

  private Map<String, String> makeTestCasesWithExpected(String... cases) {
    Map<String, String> testCases = new HashMap<>();
    for (int n = 0; n < cases.length; n += 2) {
      testCases.put(cases[n], cases[n+1]);
    }
    return testCases;
  }

  private List<String> makeTestCases(String... cases) {
    List<String> testCases = new ArrayList<>();
    for (int n = 0; n < cases.length; n++) {
      testCases.add(cases[n]);
    }
    return testCases;
  }

  @Test
  public void test_000_template() {
    methodEntry();
    result = "test template";
  }


}
