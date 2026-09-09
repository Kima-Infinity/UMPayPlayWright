package com.umpay.stepdefs;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.umpay.pages.*;
import com.umpay.utility.*;
import com.umpay.utility.BaseClass;
import com.aventstack.extentreports.ExtentTest;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

import java.io.File;
import java.util.List;

public class LoginToPageStepDefs {

    LoginPage loginPage;
    ResetPasswordPage resetPasswordPage;
    HomePage homePage;
    HeaderPage headerPage;
    ProfilePage profilePage;
    ExcelDataProvider excel;

    /*
     * Column positions in the NegativeLogin sheet of Login_TestData.xlsx. Row 0 is the
     * header, so the row named by a scenario is the row of the same number.
     */
    private static final int COUNTRY = 1;
    private static final int IDENTIFIER = 2;
    private static final int PASSWORD = 3;
    private static final int EXPECTED_MESSAGE = 4;

    /**
     * What a blank cell is written as.
     *
     * A genuinely empty cell reads back as a missing row entry rather than an empty string,
     * so the scenarios that submit a blank box say so in a word instead.
     */
    private static final String BLANK = "EMPTY";

    /*
     * Column positions in the ResetPassword sheet. Scenario, Country and Identifier sit
     * where they do in NegativeLogin; the last two differ because that form asks for a
     * captcha rather than a password.
     */
    private static final int RESET_CAPTCHA = 3;
    private static final int RESET_EXPECTED_MESSAGE = 4;

    /** How long to keep watching the mailbox for the reset code. */
    private static final int MAIL_TIMEOUT_SECONDS = 120;

    /** The language the switcher moved to, so the next step can check it stuck. */
    private String chosenLanguage;

    /** The captcha picture as it was before the refresh button was pressed. */
    private String captchaBeforeRefresh;

    @Given("I log into the UMPay application with valid email credentials using {string} of {string} of {string}")
    public void logIntoApplication(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);
        loginPage = new LoginPage(BaseClass.driver);
        homePage = new HomePage(BaseClass.driver);
        headerPage = new HeaderPage(BaseClass.driver);
        profilePage = new ProfilePage(BaseClass.driver);

        if (BaseClass.logger == null) {
            BaseClass.logger = BaseClass.report.createTest("Login to UMPay");
        }

        loginPage.loginToUMPay(excel.getStringData(excelSheetName, row, 1), excel.getStringData(excelSheetName, row, 2));

        BaseClass.logger.pass("Login to UMPay application successful");
    }

    @When("I check and validate all the homepage contents")
    public void checkHomePageContents() {

        homePage.homePage();
        BaseClass.logger.pass("Validated all the contents of Home page.");

    }

    @Then("I should be able to successfully log out")
    public void logOutFromApplication() throws InterruptedException {

        // These are normally set up by the login step, but the end-to-end scenario also signs
        // out straight after registering - at that point the session belongs to the account
        // just created and the login step has never run, so the page objects would be null.
        // Building them on demand lets this step stand on its own wherever a session exists.
        if (headerPage == null) {
            headerPage = new HeaderPage(BaseClass.driver);
        }

        if (profilePage == null) {
            profilePage = new ProfilePage(BaseClass.driver);
        }

        headerPage.profile();
        profilePage.logout();


        if (BaseClass.logger != null) {
            BaseClass.logger.pass("Logout successful");
        }
    }

    // ------------------------------------------------------------------
    // Reaching the login page in a known state
    // ------------------------------------------------------------------

    /**
     * Opens the registration page the way the registration steps expect to find it.
     *
     * Everything the step it replaces did, not merely the navigation: the report entry a
     * scenario writes its steps into is created here too. Opening the page without it left
     * BaseClass.logger null, and the first step that recorded a pass threw on it - a failure
     * about reporting, in a scenario about registering.
     */
    private void openRegistrationPage() {

        com.umpay.pages.RegisterPage registerPage = new com.umpay.pages.RegisterPage(BaseClass.driver);

        registerPage.open(BaseClass.config.getRegisterUrl());

        if (BaseClass.logger == null) {
            BaseClass.logger = BaseClass.report.createTest("Register a UMPay account");
        }

        // Waited for rather than asserted the instant the address changes. The form is drawn
        // after the page loads, and a scenario that opened it straight after another one had
        // just finished failed here for being early rather than for anything about registering.
        com.umpay.utility.Wait.until(registerPage::isRegistrationFormDisplayed, 20);

        Assert.assertTrue(registerPage.isRegistrationFormDisplayed(),
                "Registration form was not displayed. The page is at " + registerPage.getCurrentUrl());
    }

    /**
     * The page a scenario starts on, named by the scenario rather than baked into the step.
     *
     * The three pages a run can start from without signing in each had a step of their own, and
     * a scenario that starts on one of them had nothing to put in an Examples table. Naming the
     * page gives those scenarios something real to be driven by, and delegates to the step that
     * already knows how to open it.
     */
    /**
     * Opens the reset page without going through the step that owns it.
     *
     * The reset steps live in ResetPasswordStepDefs now, and one Cucumber step class cannot call
     * a method on another - each is built fresh for the scenario that uses it. So this dispatcher
     * opens the page itself, which is a few lines duplicated in exchange for the two files
     * staying independent of each other.
     */
    private void openTheResetPage() {

        ResetPasswordPage resetPage = new ResetPasswordPage(BaseClass.driver);

        resetPage.open(BaseClass.config.getResetPasswordUrl());

        if (BaseClass.logger == null) {
            BaseClass.logger = BaseClass.report.createTest("UMPay password reset");
        }

        Assert.assertTrue(resetPage.isShowing(),
                "The reset form was not shown. Landed on " + resetPage.getCurrentUrl());

        BaseClass.logger.pass("Password reset page opened");
    }

    @Given("I am on the UMPay {string} page")
    public void openThePage(String page) {

        switch (page.toLowerCase()) {
            case "login" -> openLoginPage();
            case "password reset" -> openTheResetPage();
            case "registration" -> openRegistrationPage();
            default -> Assert.fail("There is no UMPay \"" + page + "\" page to start from."
                    + " Try login, registration or password reset.");
        }
    }

    @Given("I am on the UMPay login page")
    public void openLoginPage() {

        loginPage = new LoginPage(BaseClass.driver);

        loginPage.open(BaseClass.config.getUrl());

        if (BaseClass.logger == null) {
            BaseClass.logger = BaseClass.report.createTest("UMPay login page");
        }

        Assert.assertTrue(loginPage.isShowing(),
                "The login form was not shown. Landed on " + loginPage.getCurrentUrl());

        BaseClass.logger.pass("Login page opened");
    }

    // ------------------------------------------------------------------
    // Signing in with credentials that are meant to be refused
    // ------------------------------------------------------------------

    /**
     * Signs in with the phone number held in the login sheet.
     *
     * Sheet1 has no column of its own for a country, so the number lives in the UserName
     * column written the way a person would write it - "855 96443322" - and is split here.
     * Splitting test data is the step's job: the page is told a dialling code and a number,
     * which is what the form actually asks for.
     */
    @When("I sign in with the phone number in {string} of {string} of {string}")
    public void signInWithPhoneNumberFrom(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);
        loginPage = new LoginPage(BaseClass.driver);
        homePage = new HomePage(BaseClass.driver);
        headerPage = new HeaderPage(BaseClass.driver);
        profilePage = new ProfilePage(BaseClass.driver);

        if (BaseClass.logger == null) {
            BaseClass.logger = BaseClass.report.createTest("Login to UMPay");
        }

        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String[] parts = excel.getStringData(excelSheetName, row, 1).trim().split("\\s+", 2);

        Assert.assertEquals(parts.length, 2,
                "The phone row should read \"<dialling code> <number>\", such as \"855 96443322\","
                        + " but it reads \"" + excel.getStringData(excelSheetName, row, 1) + "\"");

        loginPage.signInWithDiallingCode(parts[0], parts[1],
                excel.getStringData(excelSheetName, row, 2));

        BaseClass.logger.pass("Signed in with +" + parts[0] + " " + parts[1]);
    }

    @Then("I should be signed in")
    public void shouldBeSignedIn() {

        Assert.assertTrue(loginPage.isSignedIn(),
                "The sign in did not go through. Still on " + loginPage.getCurrentUrl());

        BaseClass.logger.pass("Signed in successfully");
    }

    @When("I try to sign in with the email address in {string} of {string} of {string}")
    public void signInWithEmailFrom(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        loginPage.signInWithEmail(valueAt(excelSheetName, row, IDENTIFIER),
                valueAt(excelSheetName, row, PASSWORD));

        BaseClass.logger.pass("Submitted the sign-in form for row " + row
                + ": " + excel.getStringData(excelSheetName, row, 0));
    }

    @When("I try to sign in with the phone number in {string} of {string} of {string}")
    public void signInWithPhoneFrom(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        loginPage.signInWithPhone(valueAt(excelSheetName, row, COUNTRY),
                valueAt(excelSheetName, row, IDENTIFIER),
                valueAt(excelSheetName, row, PASSWORD));

        BaseClass.logger.pass("Submitted the sign-in form for row " + row
                + ": " + excel.getStringData(excelSheetName, row, 0));
    }

    @When("I enter the phone number in {string} of {string} of {string} without signing in")
    public void enterPhoneNumberFrom(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        loginPage.enterPhoneNumber(valueAt(excelSheetName, row, COUNTRY),
                valueAt(excelSheetName, row, IDENTIFIER));

        BaseClass.logger.pass("Typed the phone number from row " + row);
    }

    // ------------------------------------------------------------------
    // What the page said about it
    // ------------------------------------------------------------------

    @Then("the sign in should be refused with the message in {string} of {string} of {string}")
    public void signInShouldBeRefusedWith(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String expected = valueAt(excelSheetName, row, EXPECTED_MESSAGE);
        String actual = loginPage.errorMessage();

        Assert.assertFalse(actual.isBlank(),
                "The sign in was expected to be refused with \"" + expected
                        + "\" but nothing was said about it");

        // A contains rather than an equals: the wrong-password refusal counts down the
        // attempts left in the same sentence, so the number in it changes run to run.
        Assert.assertTrue(actual.contains(expected),
                "Expected the sign in to be refused with \"" + expected
                        + "\" but it said \"" + actual + "\"");

        BaseClass.logger.pass("Sign in refused with: " + actual);
    }

    /**
     * The same check against a message named in the scenario rather than in the test data.
     *
     * The lock scenario needs this: what it expects is not a property of the row it signs in
     * with, it is what the third refusal in a row turns into.
     */
    @Then("the sign in should be refused with the message {string}")
    public void signInShouldBeRefusedWith(String expected) {

        String actual = loginPage.errorMessage();

        Assert.assertFalse(actual.isBlank(),
                "The sign in was expected to be refused with \"" + expected
                        + "\" but nothing was said about it");

        Assert.assertTrue(actual.contains(expected),
                "Expected the sign in to be refused with \"" + expected
                        + "\" but it said \"" + actual + "\"");

        BaseClass.logger.pass("Sign in refused with: " + actual);
    }

    @Then("the browser should reject the login {string} field with the message in {string} of {string} of {string}")
    public void browserShouldRejectLoginField(String fieldName, String rowNumber,
                                              String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String expected = valueAt(excelSheetName, row, EXPECTED_MESSAGE);

        Assert.assertFalse(loginPage.isFieldValid(fieldName),
                "The " + fieldName + " field was accepted by the browser but it should not have been");

        Assert.assertEquals(loginPage.validationMessageFor(fieldName), expected,
                "Unexpected validation message on the " + fieldName + " field");

        BaseClass.logger.pass("The " + fieldName + " field was rejected with: " + expected);
    }

    @Then("the form should complain with the message in {string} of {string} of {string}")
    public void formShouldComplainWith(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String expected = valueAt(excelSheetName, row, EXPECTED_MESSAGE);
        String actual = loginPage.fieldError();

        Assert.assertEquals(actual, expected, "Unexpected complaint under the field");

        BaseClass.logger.pass("The form complained: " + actual);
    }

    @Then("the phone number should be kept as typed in {string} of {string} of {string}")
    public void phoneNumberShouldBeKeptAsTyped(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String typed = valueAt(excelSheetName, row, IDENTIFIER);

        Assert.assertEquals(loginPage.phoneAsTyped(), typed,
                "The phone number field did not keep what was typed into it");

        BaseClass.logger.pass("The phone number field kept \"" + typed + "\"");
    }

    @Then("I should still be on the login page")
    public void shouldStillBeOnTheLoginPage() {

        Assert.assertTrue(loginPage.isShowing(),
                "The sign in went through when it should have been refused. Landed on "
                        + loginPage.getCurrentUrl());

        BaseClass.logger.pass("Still on the login page");
    }

    // ------------------------------------------------------------------
    // The other ways off the login page
    // ------------------------------------------------------------------

    @When("I follow the Forgot password link")
    public void followForgotPasswordLink() {

        loginPage.openForgotPassword();
    }

    @Then("the password reset page should open")
    public void passwordResetPageShouldOpen() {

        Assert.assertTrue(loginPage.getCurrentUrl().contains("/forgot-password"),
                "The Forgot password link did not open the reset page. Landed on "
                        + loginPage.getCurrentUrl());

        BaseClass.logger.pass("Forgot password opened the reset page");
    }

    @When("I follow the Register link")
    public void followRegisterLink() {

        loginPage.openRegister();
    }

    @Then("the registration page should open")
    public void registrationPageShouldOpen() {

        Assert.assertTrue(loginPage.getCurrentUrl().contains("/register"),
                "The Register link did not open the registration page. Landed on "
                        + loginPage.getCurrentUrl());

        BaseClass.logger.pass("Register opened the registration page");
    }

    @When("I open Customer Service from the login page")
    public void openCustomerService() {

        loginPage.openCustomerService();
    }

    @Then("the customer service chat should open")
    public void customerServiceChatShouldOpen() {

        Assert.assertTrue(loginPage.isCustomerServiceOpen(),
                "The Customer Service button did not open the chat");

        BaseClass.logger.pass("Customer Service opened");
    }

    // ------------------------------------------------------------------
    // Language
    // ------------------------------------------------------------------

    @Then("the login page should offer more than one language")
    public void loginPageShouldOfferMoreThanOneLanguage() {

        List<String> offered = loginPage.languagesOffered();

        Assert.assertTrue(offered.size() > 1,
                "The language switcher offers only " + offered);

        BaseClass.logger.pass("The language switcher offers " + offered);
    }

    @When("I choose another language")
    public void chooseAnotherLanguage() {

        chosenLanguage = loginPage.switchToAnotherLanguage();

        BaseClass.logger.pass("Chose the language " + chosenLanguage);
    }

    @Then("the login page should come back in the language I chose")
    public void loginPageShouldComeBackInTheChosenLanguage() {

        Assert.assertEquals(loginPage.selectedLanguage(), chosenLanguage,
                "The page did not come back in the language that was chosen");

        BaseClass.logger.pass("The login page is shown in " + chosenLanguage);
    }

    @When("I choose the language {string}")
    public void chooseLanguage(String language) {

        loginPage.chooseLanguage(language);
    }

    @Then("the login page should be shown in {string}")
    public void loginPageShouldBeShownIn(String language) {

        Assert.assertEquals(loginPage.selectedLanguage(), language,
                "The page did not come back in the language that was chosen");

        BaseClass.logger.pass("The login page is shown in " + language);
    }

    /**
     * A cell from the test data, with the blank marker turned back into a blank.
     */
    private String valueAt(String sheetName, int row, int column) {

        String value = excel.getStringData(sheetName, row, column);

        return BLANK.equals(value) ? "" : value;
    }

    /**
     * The two factor prompt that meets every sign-in.
     *
     * The suite skips this roughly three hundred times a run and had never once asked whether it
     * was there to be skipped. Setting an authenticator up is deliberately left alone - it would
     * tie this account to a secret the suite would then have to keep and generate codes from -
     * so what is held to is that the prompt is offered and that skipping it is not permanent.
     */
    @Then("the two factor prompt should be shown")
    public void theTwoFactorPromptShouldBeShown() {

        homePage = new HomePage(BaseClass.driver);

        com.umpay.utility.Wait.until(() -> homePage.isTwoFactorPromptDisplayed(), 20);

        Assert.assertTrue(homePage.isTwoFactorPromptDisplayed(),
                "Signing in did not offer to set up two factor authentication, so an account"
                        + " without it is never asked. The page is at " + homePage.getCurrentUrl());

        System.out.println("The two factor prompt was offered");

        BaseClass.logger.pass("Signing in offered to set up two factor authentication");
    }

    /**
     * Skips the two factor prompt, from the login side of the application.
     *
     * The registration steps have a skip of their own, and borrowing it does not work: it is
     * written for the flow that has just registered an account and carries that flow's state, so
     * a scenario starting from an ordinary sign-in walks into a half-built RegisterStepDefs and
     * fails on something that has nothing to do with two factor authentication.
     */
    @When("I skip the two factor prompt")
    public void iSkipTheTwoFactorPrompt() {

        homePage = new HomePage(BaseClass.driver);

        Assert.assertTrue(homePage.skipTwoFactorSetup(),
                "There was no two factor prompt to skip. The page is at "
                        + homePage.getCurrentUrl());

        BaseClass.logger.pass("Skipped the two factor prompt");
    }

    @Then("I should be let through to the home page")
    public void iShouldBeLetThroughToTheHomePage() {

        homePage = new HomePage(BaseClass.driver);

        com.umpay.utility.Wait.until(() -> !homePage.isTwoFactorPromptDisplayed(), 15);

        Assert.assertFalse(homePage.isTwoFactorPromptDisplayed(),
                "The two factor prompt is still covering the home page after being skipped");

        Assert.assertTrue(homePage.walletCount() > 0,
                "Skipping the prompt did not leave the account on its own home page. The page is"
                        + " at " + homePage.getCurrentUrl());

        System.out.println("Skipping let the account through to " + homePage.getCurrentUrl()
                + " with " + homePage.walletCount() + " wallets showing");

        BaseClass.logger.pass("Skipping the prompt let the account through to the home page");
    }
}
