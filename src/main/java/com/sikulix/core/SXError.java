/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.core;

public class SXError {

  public SXError(String error, Object... args) {
    this();
    this.error = String.format("[%s] %s]", methodName, error);
    this.args = args;
  }

  String error = "unknown Error";
  Object[] args = new Object[]{};
  String itShouldNotRun = "";

  public boolean shouldNotRun() {
    return SX.isSet(itShouldNotRun);
  }

  String methodName = "";

  public String getMethodName() {
    return methodName;
  }

  public SXError() {
    methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
    if (SX.isHeadless()) {
      itShouldNotRun = " - not on Headless";
    }
  }

  @Override
  public String toString() {
    return String.format(error, args);
  }
}
