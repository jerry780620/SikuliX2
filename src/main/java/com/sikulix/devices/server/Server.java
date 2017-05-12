/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.devices.server;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Server extends NanoHTTPD {

  static SXLog log;

  static {
    log = SX.getSXLog("SXServer");
    log.isSX();
    log.on(SXLog.TRACE);
  }

  static boolean shouldStop = false;
  static Server server = null;
  int currentPort = -1;

  public Server() throws IOException {
    this(8080);
  }

  public Server(int port) throws IOException {
    super(port);
    currentPort = port;
    start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    log.trace("started on localhost:%d", port);
  }

  static void createStopListener() {
    Runnable stopListener = new Runnable() {
      public void run() {
        log.trace("createStopListener: stop listener started");
        while (!shouldStop) {
          SX.pause(1);
        }
        if (SX.isNotNull(server)) {
          log.trace("StopListener: stopping in 3 secs");
          SX.pause(3);
          log.trace("StopListener: stopping now");
          server.stop();
        }
      }
    };
    new Thread(stopListener).start();
  }

  public static boolean start(String[] args) {
    createStopListener();
    try {
      server = new Server();
    } catch (IOException ioe) {
      log.error("could not start on localhost:8080 (%s)", ioe.getMessage());
      shouldStop = true;
      return false;
    }
    return true;
  }

  @Override
  public Response serve(IHTTPSession session) {
    Response.IStatus status = Response.Status.OK;
    boolean isImplemented = true;
    boolean isError = false;
    String uri = session.getUri();
    Method method = session.getMethod();
    boolean isGet = Method.GET.equals(method);
    boolean isDelete = Method.DELETE.equals(method);
    boolean isPost = Method.POST.equals(method);
    Map<String, String> headers = session.getHeaders();

    log.trace("%s - %s", method, uri);

    String responseTemplate = "{\"sessionId\" : \"%s\", \"status\" : \"%s\", \"value\" : %s}";
    String responseValue = "";
    String responseValueContent = "";
    String responseValueTemplate = "{\"message\" : \"%s(%s)\", \"content\" : %s}";
    String responseValueMsg = uri;
    String responseValueMsgSorry = "not implemented: " + uri;
    String responseValueMsgError = "error: " + uri;

    String command = "";
    int commandStatus = 0;
    boolean isCommandSession = false;
    String sessionID = "0";
    String subCommand = "";
    String subCommandParm = "";
    String[] route = uri.split("/");

    if (route.length > 1) {
      command = route[1].toLowerCase();
      if ("session".equals(command)) {
        isCommandSession = true;
        if (route.length > 2) {
          sessionID = route[2].toLowerCase();
        }
        if (route.length > 3) {
          subCommand = route[3].toLowerCase();
        }
        if (route.length > 4) {
          subCommandParm = route[4].toLowerCase();
        }
      }
    } else {
      isGet = true;
      command = "state";
      responseValueMsg = "using GET/state: " + uri;
    }
    if (isGet) {
      if (isCommandSession) {
        isImplemented = false;
      } else if (command.startsWith("stop")) {
        shouldStop = true;
        log.trace("stopping intentionally");
        responseValueMsg = "server is stopping";
      } else if (command.startsWith("state")) {
        responseValueContent = String.format("\"running on localhost:%d\"", currentPort);
      } else if (command.startsWith("sessions")) {
        isImplemented = false;
      } else {
        isImplemented = false;
      }
    } else if (isDelete) {

    } else if (isPost) {
      final HashMap<String, String> bodyFiles = new HashMap<String, String>();
      if ("application/json".equals(headers.get("content-type"))) {
        try {
          session.parseBody(bodyFiles);
          responseValueContent = bodyFiles.get("postData");
        } catch (Exception e) {
          log.error("POST: invalid: %s", e.getMessage());
          isImplemented = false;
        }
        if ("execute".equals(subCommand)) {
          log.trace("EXECUTE: %s", responseValueContent);
        }
      } else {
        isImplemented = false;
      }
      if (isImplemented) {
      }
    } else {
        isImplemented = false;
    }
    if (!isImplemented) {
      status = Response.Status.NOT_IMPLEMENTED;
      responseValueMsg = responseValueMsgSorry;
    } else if (isError) {
      responseValueMsg = responseValueMsgError;
    }
    if (!Response.Status.OK.equals(status)) {
      commandStatus = status.getRequestStatus();
    }
    responseValue = String.format(responseValueTemplate, method, responseValueMsg, responseValueContent);
    String theResponse = String.format(responseTemplate, sessionID, commandStatus, responseValue);
    log.trace("Response: %s", theResponse);
    return newFixedLengthResponse(status, "application/json", theResponse);
  }
}
