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
    @AfterAll
    public static void cucumberAfterAll() {
        // The browser outlives the scenarios now - each one only ends its own context - so
        // something has to close it. This hook and TestRunner's @AfterSuite both call it
        // for the same reason they both send the report: neither fires for every way of
        // starting a run, and shutdown is safe to call twice.
        BrowserFactory.shutdown();
        sendEmailStatic();
    }

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

    @After
    public void cucumberTearDown(Scenario scenario) {
        String screenshotPath = Helper.captureScreenShot(driver);
        System.setProperty("last.screenshot.path", screenshotPath);

        if (scenario.isFailed()) {
            if (logger != null) {
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
