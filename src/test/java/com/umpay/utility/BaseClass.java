package com.umpay.utility;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import com.microsoft.playwright.Page;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;

import java.io.File;
import java.util.Collections;

public class BaseClass {

    /**
     * The tab every step works through, in place of Selenium's WebDriver.
     *
     * Named driver rather than page on purpose: every step definition and page object in
     * the suite refers to BaseClass.driver, and keeping the name means the port is a change
     * of engine rather than a rename spread across forty files.
     */
    public static Page driver;

    public static ConfigDataProvider config;

    public static ExtentReports report;

    public static ExcelDataProvider excel;

    public static ExtentTest logger;

    public static String reportPath;

    @BeforeSuite
    public void setUpSuite(){

        Reporter.log("Starting the test execution",true);

        String fileName = null;
        String sheetName = null;
        excel=new ExcelDataProvider(fileName, sheetName);
        config = new ConfigDataProvider();

        if (reportPath == null) {
            reportPath = System.getProperty("user.dir") + "/Reports/"+Helper.getCurrentDateTime()+"TestReport.html";
        }
        ExtentSparkReporter extent = new ExtentSparkReporter(new File(reportPath));
        report = new ExtentReports();
        report.attachReporter(extent);

        Reporter.log("Setting Done. Test can be started",true);

    }

    @BeforeClass
    public void setup() {

        Reporter.log("Starting Browser and Application",true);

        driver = BrowserFactory.startBrowser(config.getUrl());

        Reporter.log("Browser and Application started",true);
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            BrowserFactory.quitBrowser(driver);
        }
    }

    @AfterMethod
    public void tearDownMethod(ITestResult result) {

        Reporter.log("Test is about to end.",true);

        String screenshotPath = null;
        if(result.getStatus() == ITestResult.FAILURE){
            screenshotPath = Helper.captureScreenShot(driver);

            // A TestNG test is a failed test too. Cucumber's After hook never sees one, so
            // without this the only tests in the suite that failed without an explanation
            // were the ones not written as scenarios.
            String failureReport = FailureReport.ofTest(
                    result.getTestClass().getName(), result.getMethod().getMethodName(),
                    driver, screenshotPath);

            System.out.println(failureReport);

            logger.fail("<pre>" + failureReport
                    .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    + "</pre>");
            logger.fail("Test Failed", MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());
        }
        else if(result.getStatus()== ITestResult.SUCCESS){
            screenshotPath = Helper.captureScreenShot(driver);
            logger.pass("Test Passed", MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());
        }
        else if(result.getStatus()== ITestResult.SKIP){
            screenshotPath = Helper.captureScreenShot(driver);
            logger.skip("Test Skipped", MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());
        }
        report.flush();

        Reporter.log("Test Completed and Report Generated!",true);
        
        // Store the last screenshot path to be sent in the email after the suite ends
        System.setProperty("last.screenshot.path", screenshotPath != null ? screenshotPath : "");
    }

    @AfterSuite(alwaysRun = true)
    public void sendEmail() {
        sendEmailStatic();
    }

    /**
     * Sends the report when Cucumber finishes, which is the only hook that fires when the
     * run did not come through TestNG.
     *
     * Both this and TestRunner's @AfterSuite call the same guarded method on purpose. They
     * cover different ways of starting a run and neither covers both:
     *
     *   mvn test / a TestNG run configuration  - both fire
     *   a .feature launched straight from the IDE - only this one fires, because IntelliJ
     *                                              runs Cucumber itself and no TestNG suite
     *                                              ever exists
     *
     * Emptying this method is what stopped reports arriving for runs started from the IDE.
     * The duplicate emails that emptying it was meant to solve are handled by the guard in
     * sendEmailStatic instead, which is the right place: it fixes double sending without
     * caring which hook won.
     */

    /** One report per run, however many hooks reach this. */
    private static final java.util.concurrent.atomic.AtomicBoolean REPORT_SENT =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * Emails the report, at most once per run.
     *
     * Before this guard a plain run sent two copies, one from TestRunner's @AfterSuite and
     * one from Cucumber's @AfterAll. Both fire after everything has finished, so whichever
     * arrives first sends the complete report and the other is a no-op.
     */
    public static void sendEmailStatic() {

        if (!REPORT_SENT.compareAndSet(false, true)) {
            return;
        }

        if (config == null) {
            config = new ConfigDataProvider();
        }

        if (!config.isMailEnabled()) {
            System.out.println("Report email is switched off (mail.enabled=false). The report"
                    + " is still written to " + BaseClass.reportPath);
            return;
        }

        String reportPath = BaseClass.reportPath;
        String screenshotPath = System.getProperty("last.screenshot.path");
        
        java.util.List<String> attachments = new java.util.ArrayList<>();
        if (reportPath != null && new File(reportPath).exists()) {
            attachments.add(reportPath);
        }

        if (screenshotPath != null && !screenshotPath.isEmpty() && new File(screenshotPath).exists()) {
            attachments.add(screenshotPath);
        }

        String body = "<h3>Test Automation Report</h3>" +
                      "<p>Please find the attached test execution report.</p>";
        if (screenshotPath != null && !screenshotPath.isEmpty()) {
            body += "<p><b>Last Screenshot Captured:</b><br><img src='cid:screenshot' width='600'/></p>";
        }

        MailUtils.sendEmail(
                config.getMailHost(),
                config.getMailPort(),
                config.getMailAuth(),
                config.getMailStartTLS(),
                config.getMailFrom(),
                config.getMailPassword(),
                config.getMailTo(),
                "Test Automation Report - " + Helper.getCurrentDateTime(),
                body,
                attachments
        );
    }

    @Before(order = 0)
    public void cucumberSetUp() {
        if (config == null) {
            config = new ConfigDataProvider();
        }

        if (report == null) {
            if (reportPath == null) {
                reportPath = System.getProperty("user.dir") + "/Reports/" + Helper.getCurrentDateTime() + "TestReport.html";
            }
            ExtentSparkReporter extent = new ExtentSparkReporter(new File(reportPath));
            report = new ExtentReports();
            report.attachReporter(extent);
        }

        if (driver == null) {
            driver = BrowserFactory.startBrowser(config.getUrl());
        }
    }

    /**
     * Puts the screenshot into the Cucumber report.
     *
     * Best effort. A run that cannot read back the file it just wrote is still a run worth
     * reporting, and the path is named in the text report either way.
     */
    private static void attachScreenshot(Scenario scenario, String screenshotPath) {

        if (screenshotPath == null || screenshotPath.isBlank()) {
            return;
        }

        try {
            byte[] picture = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(screenshotPath));
            scenario.attach(picture, "image/png", "the screen when it failed");
        } catch (Exception cannotAttach) {
            System.out.println("Could not attach the screenshot to the Cucumber report: "
                    + cannotAttach.getMessage());
        }
    }

    @After
    public void cucumberTearDown(Scenario scenario) {
        String screenshotPath = Helper.captureScreenShot(driver);
        System.setProperty("last.screenshot.path", screenshotPath);

        if (scenario.isFailed()) {

            // Built before the browser is closed: the calls it made are read off the page,
            // and a closed page has none to give.
            String failureReport = FailureReport.of(scenario, driver, screenshotPath);

            // To the console, so a terminal run shows it without opening anything.
            System.out.println(failureReport);

            // To the Cucumber report and the JSON, so CI carries it too.
            scenario.attach(failureReport.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "text/plain", "how to reproduce, and what the API said");

            // The picture goes into the Cucumber report too. It was reaching the Extent
            // HTML only, so a CI run reading the JSON - which is the one that matters when
            // nobody is at a desk - had the whole account of the failure except the screen.
            attachScreenshot(scenario, screenshotPath);

            if (logger != null) {
                logger.fail("<pre>" + failureReport
                        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                        + "</pre>");
                logger.fail("Scenario Failed", MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());
            }
        } else {
            if (logger != null) {
                logger.pass("Scenario Passed", MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());
            }
        }

        if (report != null) {
            report.flush();
        }

        if (driver != null) {
            BrowserFactory.quitBrowser(driver);
            driver = null;
        }
    }
}
