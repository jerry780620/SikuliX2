/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.test;

import com.sikulix.core.SX;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Ignore
public class TestHelper {

  protected static String methodEntry() {
    return doMethodEntry("");
  }

  protected static String methodEntryNotOnTravis() {
    String notRunReason = "";
    if (SX.isTravisCI()) notRunReason = " - not on Travis";
    return doMethodEntry(notRunReason);
  }

  private static String doMethodEntry(String notRunReason) {
    String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
    String shouldNotRun = notRunReason;
    if (SX.isHeadless()) {
      shouldNotRun = " - not on Headless";
    }
    if (SX.isSet(shouldNotRun)) {
      return "! " + methodName + shouldNotRun;
    } else {
      return methodName;
    }
  }

  protected static boolean shouldNotRun(String currentMethod) {
    return currentMethod.startsWith("!");
  }

  protected static String testError(String msg, Object... args) {
    return String.format(msg, args);
  }

  protected static Map<String, String> makeTestCasesWithExpected(String... cases) {
    Map<String, String> testCases = new HashMap<>();
    for (int n = 0; n < cases.length; n += 2) {
      testCases.put(cases[n], cases[n+1]);
    }
    return testCases;
  }

  protected static List<String> makeTestCases(String... cases) {
    List<String> testCases = new ArrayList<>();
    for (int n = 0; n < cases.length; n++) {
      testCases.add(cases[n]);
    }
    return testCases;
  }

}
