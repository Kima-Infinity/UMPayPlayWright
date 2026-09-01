package com.umpay.utility;

import com.microsoft.playwright.Page;

import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Helper {

    /**
     * Writes a screenshot and returns where it went.
     *
     * Playwright saves straight to the path it is given, so the copy-from-a-temp-file dance
     * Selenium needed is gone. Full page rather than just the viewport: a failure is often
     * something below the fold, and this costs nothing.
     */
    public static String captureScreenShot(Page page) {

        String screenShotPath = System.getProperty("user.dir") + "/Screenshots/"
                + getCurrentDateTime() + "screenshot.png";

        try {
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(screenShotPath))
                    .setFullPage(true));
        } catch (Exception e) {
            System.out.println("Not able to capture screenshot" + e.getMessage());
        }

        return screenShotPath;
    }

    public static String getCurrentDateTime(){

        DateFormat customFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
        Date date = new Date();
        return customFormat.format(date);

    }

    /**
     * Registration rejects an address that is already in use, so the happy path
     * needs a fresh one on every run. Plus addressing keeps the mail in the same
     * inbox as the base address while still being a new account to UMPay.
     */
    public static String getUniqueEmail(String baseEmail) {

        int at = baseEmail.indexOf("@");

        if (at < 0) {
            System.out.println("Not a valid email address, using it as it is: " + baseEmail);
            return baseEmail;
        }

        String local = baseEmail.substring(0, at);
        String domain = baseEmail.substring(at);

        // Drop any plus tag already present so repeated runs do not stack them up.
        if (local.contains("+")) {
            local = local.substring(0, local.indexOf("+"));
        }

        String uniqueEmail = local + "+" + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date()) + domain;

        System.out.println("Generated unique email address: " + uniqueEmail);

        return uniqueEmail;
    }
}
