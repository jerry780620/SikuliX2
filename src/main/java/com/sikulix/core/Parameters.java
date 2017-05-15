/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.Element;

import javax.swing.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class Parameters {

  private static SXLog log = SX.getSXLog("SX.Parameters");

  private Map<String, String> parameterTypes = new HashMap<>();
  private String[] parameterNames = null;
  private Object[] parameterDefaults = new Object[0];
  private Object[] parameterNotSet = new Object[0];
  private Map<String, Object> parametersActual = new HashMap<>();

  private Map<String, Object> parameters = new HashMap<>();

  public Parameters(String theNames, String theClasses) {
    this(theNames, theClasses, new Object[0], new Object[0]);
  }

  public Parameters(String theNames, String theClasses, Object[] theDefaults, Object[] theNotsets) {
    String[] names = theNames.split(",");
    String[] classes = theClasses.split(",");
    if (names.length == classes.length) {
      for (int n = 0; n < names.length; n++) {
        String clazz = classes[n];
        if (clazz.length() == 1) {
          clazz = clazz.toLowerCase();
          if ("s".equals(clazz)) {
            clazz = "String";
          } else if ("h".equals(clazz)) {
            clazz = "Hidden";
          } else if ("i".equals(clazz)) {
            clazz = "Integer";
          } else if ("d".equals(clazz)) {
            clazz = "Double";
          } else if ("b".equals(clazz)) {
            clazz = "Boolean";
          } else if ("e".equals(clazz)) {
            clazz = "Element";
          } else if ("o".equals(clazz)) {
            clazz = "Object";
          }
        }
        if ("Hidden".equals(clazz) || "String".equals(clazz) ||
                "Integer".equals(clazz) || "Double".equals(clazz) || "Boolean".equals(clazz) ||
                "Element".equals(clazz) || "Object".equals(clazz)) {
          parameterTypes.put(names[n], clazz);
        }
      }
      parameterNames = names;
      parameterDefaults = theDefaults;
      parameterNotSet = theNotsets;
    } else {
      log.error("different length: names: %s classes: %s", theNames, theClasses);
    }
  }

  public static Map<String, Object> get(Object... args) {
    String theNames = (String) args[0];
    String theClasses = (String) args[1];
    Object[] theDefaults = (Object[]) args[2];
    Object[] theArgs = (Object[]) args[3];
    Parameters theParameters = new Parameters(theNames, theClasses, theDefaults, new Object[0]);
    return theParameters.getParameters(theArgs);
  }

  public boolean isValid() {
    return parameterTypes.size() > 0;
  }

  public Map<String, Object> asParameters(Object... args) {
    Map<String, Object> params = new HashMap<>();
    if (args.length > 0) {
      for (int n = 0; n < args.length; n++) {
        if (isParameter(args[n])) {
          String parameterName = (String) args[n];
          Object value = null;
          if (n + 1 < args.length) {
            if (!isParameter(args[n + 1])) {
              value = getParameter(args, n + 1, parameterName);
              n++;
            }
            params.put(parameterName, value);
          }
        }
      }
    }
    return params;
  }

  public void initParameters(Object... args) {
    int argsLength = args.length;
    if (argsLength > 1) {
      if (args[1] instanceof Map) {
        try {
          setParameters(args[0], (Map<String, Object>) args[1]);
        } catch (Exception ex) {
          log.error("start(): invalid parameter list");
        }
      } else {
        guessParameters(args[0], (Object[]) args[1]);
      }
    } else {
      guessParameters(args[0], new Object[0]);
    }
  }

  private boolean isParameter(Object parameter) {
    return parameter instanceof String && SX.isNotNull(parameterTypes.get(parameter));
  }

  private Object getParameter(Object[] possibleValues, int ix, String parameterName) {
    String clazz = parameterTypes.get(parameterName);
    Object value = null;
    Object possibleValue = possibleValues[ix];
    if ("String".equals(clazz) || "Hidden".equals(clazz)) {
      if (SX.isNull(possibleValue) || possibleValue instanceof String) {
        if (SX.isNotSet(possibleValue) || possibleValue.equals(parameterNotSet[ix])) {
          value = parameterDefaults[ix];
        } else {
          value = possibleValue;
        }
      }
    } else if ("Integer".equals(clazz)) {
      if (possibleValue instanceof Integer) {
        if (parameterNotSet[ix] == possibleValue) {
          value = parameterDefaults[ix];
        } else {
          value = possibleValue;
        }
      }
    } else if ("Double".equals(clazz)) {
      if (possibleValue instanceof Double) {
        value = possibleValue;
      }
    } else if ("Boolean".equals(clazz)) {
      if (possibleValue instanceof Boolean) {
        value = possibleValue;
      }
    } else if ("Object".equals(clazz)) {
        value = possibleValue;
    } else if ("Element".equals(clazz)) {
      if (possibleValue instanceof Element) {
        value = possibleValue;
      } else if (possibleValue instanceof JFrame) {
        value = possibleValue;
      }
    }
    return value;
  }

  public void setParameters(Object instance, Map<String, Object> parameters) {
    for (String parameter : parameters.keySet()) {
      setParameter(instance, parameter, parameters.get(parameter));
    }
  }

  private void setParameter(Object instance, String parameter, Object value) {
    String methodName = String.format("set%s%s",
            parameter.substring(0, 1).toUpperCase(),
            parameter.substring(1, parameter.length()));
    try {
      Method method = instance.getClass().getMethod(methodName, new Class[]{Object.class});
      method.invoke(instance, value);
      parametersActual.put(parameter, value);
    } catch (Exception e) {
      log.error("setParameter(): did not work: %s (%s = %s)", e.getMessage(), parameter, value);
    }
  }

  public void guessParameters(Object instance, Object[] args) {
    if (SX.isNotNull(parameterNames)) {
      int n = 0;
      for (String parameterName : parameterNames) {
        if (n >= args.length) {
          setParameter(instance, parameterName, parameterDefaults[n]);
        } else {
          setParameter(instance, parameterName, getParameter(args, n, parameterName));
        }
        n++;
      }
    }
  }

  public Map<String, Object> getParameters(Object[] args) {
    Map<String, Object> params = new HashMap<>();
    if (SX.isNotNull(parameterNames)) {
      int n = 0;
      int argsn = 0;
      for (String parameterName : parameterNames) {
        params.put(parameterName, parameterDefaults[n]);
        if (args.length > 0 && argsn < args.length) {
          Object arg = getParameter(args,argsn, parameterName);
          if (SX.isNotNull(arg)) {
            params.put(parameterName, arg);
            argsn++;
          }
        }
        n++;
      }
    }
    return params;
  }

  public String toString() {
    String parameters = "";
    for (String parameter : parameterNames) {
      if (parametersActual.containsKey(parameter)) {
        Object value = parametersActual.get(parameter);
        if (parameterTypes.get(parameter).equals("Hidden") && SX.isSet(value)) {
          value = "***";
        }
        parameters += String.format("%s = %s, ", parameter, value);
      }
    }
    return parameters;
  }
}
