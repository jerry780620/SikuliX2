/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.remote.android;

import com.sikulix.api.Do;
import com.sikulix.api.Picture;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import org.sikuli.script.FindFailed;

/**
 * Created by RaiMan on 12.07.16.
 * <p>
 * Test for the basic ADB based features
 */
public class ADBTest {

  static SXLog log = SX.getSXLog("SX.ADBTest");

  private static boolean runTests = true;

  public static void main(String[] args) throws FindFailed {

    ADBScreen aScr = startTest();

    if (aScr.isValid()) {
      if (runTests) {

        basicTest(aScr);

        ADBScreen.stop();

        System.exit(0);
      }
    } else {
      System.exit(1);
    }

    // ********* playground
  }

  private static ADBScreen startTest() {
    log.on(SXLog.TRACE);
    ADBScreen adbs = new ADBScreen();
    if (adbs.isValid()) {
      adbs.wakeUp(2);
      adbs.wait(1f);
      if (runTests) {
        adbs.aKey(ADBGadget.KEY_HOME);
        adbs.wait(1f);
      }
    }
    return adbs;
  }


  private static void basicTest(ADBScreen adbs) throws FindFailed {
    log.trace("**************** running basic test");
    adbs.aSwipeLeft();
    adbs.aSwipeRight();
    adbs.wait(1f);
    Picture sIMg = adbs.userCapture("Android");
    sIMg.save(SX.getSXSTORE(), "android");
    adbs.aTap(sIMg);
  }


  /**
   * used in SikuliIDE menu tool to run a test against an attached device
   *
   * @param aScr
   */
  public static void ideTest(ADBScreen aScr) {
    String title = "Android Support - Testing device";
    Do.popup("Take care\n\nthat device is on and unlocked\n\nbefore clicking ok", title);
    aScr.wakeUp(2);
    aScr.aKey(ADBGadget.KEY_HOME);
    if (Do.popAsk("Now the device should show the HOME screen.\n" +
            "\nclick YES to proceed watching the test on the device" +
            "\nclick NO to end the test now", title)) {
      aScr.aSwipeLeft();
      aScr.aSwipeRight();
      aScr.wait(1f);
      if (Do.popAsk("You should have seen a swipe left and a swipe right.\n" +
              "\nclick YES to capture an icon from homescreen and then aTap it" +
              "\nclick NO to end the test now", title)) {
        Picture sIMg = aScr.userCapture("AndroidTest");
        sIMg.save(SX.getSXSTORE(), "android");
        try {
          aScr.aTap(sIMg);
          Do.popup("The image was found on the device's current screen" +
                  "\nand should have been tapped.\n" +
                  "\nIf you think it worked, you can now try\n" +
                  "to capture needed images from the device.\n" +
                  "\nYou have to come back here and click Default!", title);
        } catch (FindFailed findFailed) {
          Do.popError("Sorry, the image you captured was\nnot found on the device's current screen", title);
        }
      }
    }
  }
}
