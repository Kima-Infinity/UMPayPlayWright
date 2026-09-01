package com.umpay.utility;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class ConfigDataProvider {

    Properties pro;

    public ConfigDataProvider() {

        File src = new File("./Config/config.properties");

        try {
            FileInputStream fis = new FileInputStream(src);

            pro = new Properties();

            pro.load(fis);

        } catch (Exception e) {
            System.out.println("Not able to load Config File" +e.getMessage());

        }
    }

    public String getUrl() {
        return pro.getProperty("url");
    }

    public String getRegisterUrl() {
        return pro.getProperty("register.url");
    }

    /**
     * The Forgot Password page. Falls back to the login address with the path swapped in,
     * so an older config file that predates the property still points somewhere sensible
     * rather than sending the reset scenarios to a null URL.
     */
    public String getResetPasswordUrl() {

        String configured = pro.getProperty("reset.password.url");

        if (configured != null && !configured.isBlank()) {
            return configured;
        }

        return getUrl().replace("/login", "/forgot-password");
    }

    /**
     * Whether Chrome should run without a visible window. Defaults to false, so a
     * missing property keeps the old behaviour of showing the browser.
     *
     * {@link BrowserFactory#isHeadless()} lets -Dheadless=true win over this, so a
     * one-off headless run needs no edit here.
     */
    public boolean isHeadless() {

        String headless = pro.getProperty("headless");

        if (headless == null || headless.isBlank()) {
            return false;
        }
        return Boolean.parseBoolean(headless.trim());
    }

    public int getCaptchaManualTimeout() {

        String timeout = pro.getProperty("captcha.manual.timeout");

        if (timeout == null || timeout.isBlank()) {
            return 90;
        }
        return Integer.parseInt(timeout.trim());
    }

    /**
     * Whether to email the report at the end of a run. Defaults to true, so nothing changes
     * for a developer.
     *
     * CI wants it off: Jenkins archives the report itself, and the SMTP round trip is one
     * more thing that can time out and fill a build log with a stack trace about a mail
     * server when the tests are what matters. Override with -Dmail.enabled=false.
     */
    public boolean isMailEnabled() {

        String override = System.getProperty("mail.enabled");

        if (override != null && !override.isBlank()) {
            return Boolean.parseBoolean(override.trim());
        }

        String configured = pro.getProperty("mail.enabled");

        return configured == null || configured.isBlank() || Boolean.parseBoolean(configured.trim());
    }

    public String getMailHost() {
        return pro.getProperty("mail.smtp.host");
    }

    public String getMailPort() {
        return pro.getProperty("mail.smtp.port");
    }

    public String getMailFrom() {
        return pro.getProperty("mail.from");
    }

    /**
     * Never read from config.properties, so the Gmail app password stays out of
     * source control. {@link MailCredentials} decides where it does come from, and
     * is shared with the IMAP reader so both halves of the mailbox work off one
     * credential.
     */
    public String getMailPassword() {

        String password = MailCredentials.password();

        if (password.isEmpty()) {
            System.out.println("No mail password found in " + MailCredentials.source()
                    + ". Report email will be skipped.");
            return null;
        }

        return password;
    }

    public String getMailTo() {
        return pro.getProperty("mail.to");
    }

    public String getMailAuth() {
        return pro.getProperty("mail.smtp.auth");
    }

    public String getMailStartTLS() {
        return pro.getProperty("mail.smtp.starttls.enable");
    }
}
