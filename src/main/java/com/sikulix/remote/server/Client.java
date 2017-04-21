/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.remote.server;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.sikulix.core.SX;
import com.sikulix.core.SXJson;
import com.sikulix.core.SXLog;
import org.json.JSONObject;

public class Client {

  static SXLog log;
  static {
    log = SX.getSXLog("SXClient");
    log.isSX();
    log.on(SXLog.TRACE);
  }

  static String urlBase = "http://localhost:8080";

  public static JSONObject post(String body) {
    return post("/noroute", body);
  }

  public static JSONObject post(String urlCommand, String body) {
    log.trace("post(): %s body: %s", urlCommand, body);
    HttpResponse<JsonNode> jsonResponse = null;
    JSONObject response = null;
    try {
      jsonResponse = Unirest.post(urlBase + urlCommand)
              .header("accept", "application/json")
              .header("content-type", "application/json")
              .body(body)
              .asJson();
    } catch (UnirestException e) {
      log.error("post(): %s", e.getMessage());
    }
    String responseBody = "null";
    if (SX.isNotNull(jsonResponse)) {
      responseBody = jsonResponse.getBody().toString();
      response = SXJson.makeObject(responseBody);
    }
    log.trace("post() response: %s", responseBody);
    return response;
  }

  public static JSONObject get() {
    return get("/state");
  }

  public static JSONObject get(String urlCommand) {
    log.trace("getAll(): %s", urlCommand);
    HttpResponse<JsonNode> jsonResponse = null;
    JSONObject response = null;
    try {
      jsonResponse = Unirest.get(urlBase + urlCommand)
              .header("accept", "application/json")
              .asJson();
    } catch (UnirestException e) {
      log.error("getAll(): %s", e.getMessage());
    }
    String responseBody = "null";
    if (SX.isNotNull(jsonResponse)) {
      responseBody = jsonResponse.getBody().toString();
      response = SXJson.makeObject(responseBody);
    }
    log.trace("getAll() response: %s",jsonResponse.getBody().toString());
    return response;
  }

  public static boolean stopServer() {
    try {
      log.trace("stopServer: trying");
      String url = urlBase + "/stop";
      GetRequest request = Unirest.get(url);
      try {
        HttpResponse<?> response = request.asString();
//      InputStream respIS = response.getRawBody();
//      String respText = IOUtils.toString(respIS, "UTF-8");
//      log.trace("%s", respText);
      } catch (Exception ex) {
        log.error("%s", ex.getMessage());
      }
    } catch (Exception e) {
      log.error("stopServer: %s", e.getMessage());
      return false;
    }
    return true;
  }
}
