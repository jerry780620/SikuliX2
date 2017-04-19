/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */
package org.sikuli.script;

import com.sikulix.api.Event;
import com.sikulix.api.Handler;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

/**
 * implements the SikuliX FindFailed exception class
 * and defines constants and settings for the feature FindFailedResponse
 */
public class FindFailed extends SikuliException {
	private static SXLog log = SX.getSXLog("API.FINDFAILED");

	/**
	 * default FindFailedResponse is ABORT
	 */
	public static FindFailedResponse defaultFindFailedResponse = FindFailedResponse.ABORT;

	/**
	 * FindFailedResponse PROMPT: should display a prompt dialog with the failing image
	 * having the options retry, skip and abort
	 */
	public static final FindFailedResponse PROMPT = FindFailedResponse.PROMPT;

	/**
	 * FindFailedResponse RETRY: should retry the find op on FindFailed
	 */
	public static final FindFailedResponse RETRY = FindFailedResponse.RETRY;

	/**
	 * FindFailedResponse SKIP: should silently continue on FindFailed
	 */
	public static final FindFailedResponse SKIP = FindFailedResponse.SKIP;

	/**
	 * FindFailedResponse ABORT: should abort the SikuliX application
	 */
	public static final FindFailedResponse ABORT = FindFailedResponse.ABORT;

	/**
	 * FindFailedResponse HANDLE: should call a given handler on FindFailed
	 */
	public static final FindFailedResponse HANDLE = FindFailedResponse.HANDLE;

  private static Object ffHandler = null;
  private static Object imHandler = null;
  private static Object defaultHandler = null;

  /**
	 * the exception
	 * @param message to be shown
	 */
	public FindFailed(String message) {
    super(message);
    _name = "FindFailed";
  }

  public static FindFailedResponse getResponse() {
    return defaultFindFailedResponse;
  }

  public static FindFailedResponse setResponse(FindFailedResponse response) {
    defaultFindFailedResponse = response;
    return defaultFindFailedResponse;
  }

  public static FindFailedResponse setHandler(Object observer) {
    if (observer != null && (observer.getClass().getName().contains("org.python")
            || observer.getClass().getName().contains("org.jruby"))) {
      observer = new Handler(observer, Event.TYPE.FINDFAILED);
    } else {
      ((Handler) observer).setType(Event.TYPE.FINDFAILED);
    }
    ffHandler = observer;
    return defaultFindFailedResponse;
  }

  protected void setFindFailedHandler(Object handler) {
    ffHandler = setHandler(handler, Event.TYPE.FINDFAILED);
  }

  public void setImageMissingHandler(Object handler) {
    imHandler = setHandler(handler, Event.TYPE.IMAGEMISSING);
  }

  private Object setHandler(Object handler, Event.TYPE type) {
    defaultFindFailedResponse = HANDLE;
    if (handler != null && (handler.getClass().getName().contains("org.python")
            || handler.getClass().getName().contains("org.jruby"))) {
      handler = new Handler(handler, type);
    } else {
      ((Handler) handler).setType(type);
    }
    return handler;
  }

  public static Object getFindFailedHandler() {
    return ffHandler;
  }

  public static Object getImageMissingHandler() {
    return imHandler;
  }

  public static FindFailedResponse reset() {
    defaultFindFailedResponse = ABORT;
    ffHandler = null;
    imHandler = null;
    return defaultFindFailedResponse;
  }
}
