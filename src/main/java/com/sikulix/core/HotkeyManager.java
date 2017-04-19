/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.tulskiy.keymaster.common.HotKey;
import com.tulskiy.keymaster.common.HotKeyListener;
import com.tulskiy.keymaster.common.Provider;

import javax.swing.KeyStroke;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton class to bind hotkeys to hotkey listeners
 */
public class HotkeyManager {

  private static SXLog log = SX.getSXLog("SX.HotkeyManager");

  private static HotkeyManager instance = null;
  private Provider hotkeyProvider = null;
  private Map<String, HotkeyCallback> hotkeys = new HashMap<>();

  public static HotkeyManager get() {
    if (SX.isNull(instance)) {
      instance = new HotkeyManager();
      instance.initProvider();
    }
    return instance;
  }

  private void initProvider() {
    if (SX.isNull(hotkeyProvider)) {
      hotkeyProvider = Provider.getCurrentProvider(false);
    }
  }

  /**
   * remove all hotkeys
   */
  public void stop() {
    if (SX.isNull(hotkeyProvider)) {
      return;
    }
    log.trace("stopping hotkey provider");
    hotkeyProvider.stop();
    hotkeyProvider = null;
    hotkeys.clear();
  }

  /**
   * install a hotkey listener for a global hotkey
   *
   * @param hotkeys  one or more strings each with a valid key name (1+ modifiers, 1 key)<br>
   *                 or one string with valid key names separated by whitespace
   * @param callback HotkeyListener
   * @return success
   */
  public String addHotkey(HotkeyCallback callback, String... hotkeys) {
    initProvider();
    String finalKey = installHotkey(callback, hotkeys);
    if (SX.isNotSet(finalKey)) {
      String hotkey = "";
      for (String key : hotkeys) {
        hotkey += key + " ";
      }
      log.error("HotkeyManager: addHotkey: invalid arguments: %s %s", hotkey,
              (SX.isNull(callback) ? "(no callback)" : ""));
    }
    return finalKey;
  }

  private String installHotkey(HotkeyCallback listener, String... keys) {
    if (SX.isNull(listener)) {
      return "";
    }
    if (keys.length > 0) {
      String hkey = "";
      String hmods = "";
      String givenKey = "";
      for (String key : keys) {
        givenKey += key + " ";
      }
      if (keys.length == 1) {
        keys = keys[0].split("\\s");
      }
      if (keys.length > 1 && SX.isNotNull(listener)) {
        for (String key : keys) {
          String modifier = Keys.getModifierName(key);
          if (SX.isSet(modifier)) {
            hmods += modifier + " ";
            continue;
          }
          if (hkey.isEmpty()) {
            hkey = Keys.getKeyName(key);
          }
        }
      }
      if (SX.isSet(hmods) && SX.isSet(hkey)) {
        String finalKey = hmods + hkey;
        log.trace("installHotkey: %s", finalKey);
        HotKeyListenerWrapper hotKeyListenerWrapper = new HotKeyListenerWrapper(hkey, hmods, listener);
        hotkeyProvider.register(KeyStroke.getKeyStroke(hmods + hkey), hotKeyListenerWrapper);
        hotkeys.put(finalKey, listener);
        return finalKey;
      }
    }
    return "";
  }

  private void installHotkeyChecked(String keys) {
    hotkeyProvider.register(KeyStroke.getKeyStroke(keys),
            new HotKeyListenerWrapper(keys, hotkeys.get(keys)));
    hotkeys.put(keys, hotkeys.get(keys));
  }

  private class HotKeyListenerWrapper implements HotKeyListener {
    private String key = "";
    private String modifier = "";
    private HotkeyCallback callback = null;

    public HotKeyListenerWrapper(String key, String modifier, HotkeyCallback callback) {
      this.key = key;
      this.modifier = modifier;
      this.callback = callback;
    }

    public HotKeyListenerWrapper(String keys, HotkeyCallback callback) {
      String[] keyParts = keys.split("\\s");
      this.key = keyParts[keyParts.length - 1];
      this.modifier = keys.substring(0, keys.length() - key.length());
      this.callback = callback;
    }

    @Override
    public void onHotKey(HotKey hotKey) {
      callback.hotkeyPressed(new HotkeyEvent(key, modifier));
    }
  }

  public boolean removeHotkey(String givenKey) {
    if (SX.isNotNull(hotkeyProvider) && !hotkeys.isEmpty()) {
      hotkeyProvider.stop();
      hotkeyProvider = null;
      hotkeys.remove(givenKey);
      log.trace("removeHotkey: %s", givenKey);
      if (!hotkeys.isEmpty()) {
        initProvider();
        for (String key : hotkeys.keySet()) {
          log.trace("installHotkey again: %s", key);
          installHotkeyChecked(key);
        }
      }
    }
    return true;
  }
}
