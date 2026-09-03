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

        BaseClass.logger = BaseClass.report.createTest("Login to UMPay");

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

    @Given("I am on the UMPay login page")
    public void openLoginPage() {

        loginPage = new LoginPage(BaseClass.driver);

        loginPage.open(BaseClass.config.getUrl());

        BaseClass.logger = BaseClass.report.createTest("UMPay login page");

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

        BaseClass.logger = BaseClass.report.createTest("Login to UMPay");

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

    // ------------------------------------------------------------------
    // Forgot Password
    // ------------------------------------------------------------------

    @Given("I am on the UMPay password reset page")
    public void openResetPasswordPage() {

        resetPasswordPage = new ResetPasswordPage(BaseClass.driver);

        resetPasswordPage.open(BaseClass.config.getResetPasswordUrl());

        BaseClass.logger = BaseClass.report.createTest("UMPay password reset");

        Assert.assertTrue(resetPasswordPage.isShowing(),
                "The reset form was not shown. Landed on " + resetPasswordPage.getCurrentUrl());

        BaseClass.logger.pass("Password reset page opened");
    }

    @When("I fill the phone reset form from {string} of {string} of {string} without sending it")
    public void fillPhoneResetForm(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        resetPasswordPage.fillPhoneForm(valueAt(excelSheetName, row, COUNTRY),
                valueAt(excelSheetName, row, IDENTIFIER),
                valueAt(excelSheetName, row, RESET_CAPTCHA));

        BaseClass.logger.pass("Filled the phone reset form from row " + row);
    }

    @When("I fill the email reset form from {string} of {string} of {string} without sending it")
    public void fillEmailResetForm(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        resetPasswordPage.fillEmailForm(valueAt(excelSheetName, row, IDENTIFIER),
                valueAt(excelSheetName, row, RESET_CAPTCHA));

        BaseClass.logger.pass("Filled the email reset form from row " + row);
    }

    @When("I send the reset form")
    public void sendTheResetForm() {

        resetPasswordPage.submit();
    }

    @When("I ask to reset the password by phone using {string} of {string} of {string}")
    public void requestResetByPhone(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        resetPasswordPage.requestResetByPhone(valueAt(excelSheetName, row, COUNTRY),
                valueAt(excelSheetName, row, IDENTIFIER),
                valueAt(excelSheetName, row, RESET_CAPTCHA));

        BaseClass.logger.pass("Asked for a reset by phone using row " + row
                + ": " + excel.getStringData(excelSheetName, row, 0));
    }

    @When("I ask to reset the password by email using {string} of {string} of {string}")
    public void requestResetByEmail(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        resetPasswordPage.requestResetByEmail(valueAt(excelSheetName, row, IDENTIFIER),
                valueAt(excelSheetName, row, RESET_CAPTCHA));

        BaseClass.logger.pass("Asked for a reset by email using row " + row
                + ": " + excel.getStringData(excelSheetName, row, 0));
    }

    /**
     * Asks for a reset using the account's phone number, read from the login sheet.
     *
     * The number is taken from Login_TestData's login sheet rather than repeated in the
     * ResetPassword sheet, so there is one place to change when the account's number
     * changes. The captcha is read from its image, the same way the other reset scenarios
     * that reach the server do.
     */
    @When("I ask to reset the password for the phone number in {string} of {string} of {string}")
    public void requestResetForPhoneNumberFrom(String rowNumber, String excelSheetName,
                                               String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String[] parts = excel.getStringData(excelSheetName, row, 1).trim().split("\\s+", 2);

        Assert.assertEquals(parts.length, 2,
                "The phone row should read \"<dialling code> <number>\", such as \"855 96443322\","
                        + " but it reads \"" + excel.getStringData(excelSheetName, row, 1) + "\"");

        resetPasswordPage.requestResetByPhoneDiallingCode(parts[0], parts[1],
                ResetPasswordPage.AUTO_CAPTCHA);

        BaseClass.logger.pass("Asked for a reset for +" + parts[0] + " " + parts[1]);
    }

    @Then("the browser should reject the reset {string} field with the message in {string} of {string} of {string}")
    public void browserShouldRejectResetField(String fieldName, String rowNumber,
                                              String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String expected = valueAt(excelSheetName, row, RESET_EXPECTED_MESSAGE);

        Assert.assertFalse(resetPasswordPage.isFieldValid(fieldName),
                "The " + fieldName + " field was accepted by the browser but it should not have been");

        Assert.assertEquals(resetPasswordPage.validationMessageFor(fieldName), expected,
                "Unexpected validation message on the " + fieldName + " field");

        BaseClass.logger.pass("The " + fieldName + " field was rejected with: " + expected);
    }

    @Then("the reset form should complain with the message in {string} of {string} of {string}")
    public void resetFormShouldComplainWith(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String expected = valueAt(excelSheetName, row, RESET_EXPECTED_MESSAGE);

        Assert.assertEquals(resetPasswordPage.fieldError(), expected,
                "Unexpected complaint under the field");

        BaseClass.logger.pass("The reset form complained: " + expected);
    }

    @Then("the reset should be refused with the message in {string} of {string} of {string}")
    public void resetShouldBeRefusedWith(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String expected = valueAt(excelSheetName, row, RESET_EXPECTED_MESSAGE);
        String actual = resetPasswordPage.errorMessage();

        Assert.assertFalse(actual.isBlank(),
                "The reset was expected to be refused with \"" + expected
                        + "\" but nothing was said about it");

        Assert.assertTrue(actual.contains(expected),
                "Expected the reset to be refused with \"" + expected
                        + "\" but it said \"" + actual + "\"");

        BaseClass.logger.pass("Reset refused with: " + actual);
    }

    @Then("the reset phone number should be kept as typed in {string} of {string} of {string}")
    public void resetPhoneNumberShouldBeKeptAsTyped(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String typed = valueAt(excelSheetName, row, IDENTIFIER);

        Assert.assertEquals(resetPasswordPage.phoneAsTyped(), typed,
                "The phone number field did not keep what was typed into it");

        BaseClass.logger.pass("The phone number field kept \"" + typed + "\"");
    }

    @When("I ask the reset form for a new captcha")
    public void askForANewCaptcha() {

        captchaBeforeRefresh = resetPasswordPage.captchaSource();

        Assert.assertFalse(captchaBeforeRefresh.isBlank(),
                "There was no captcha image on the reset form to refresh");

        resetPasswordPage.refreshCaptcha();
    }

    @Then("a different captcha image should be shown")
    public void aDifferentCaptchaShouldBeShown() {

        String after = resetPasswordPage.captchaSource();

        Assert.assertFalse(after.isBlank(), "The captcha image went away instead of changing");

        Assert.assertNotEquals(after, captchaBeforeRefresh,
                "The refresh button left the same captcha image on the form");

        BaseClass.logger.pass("The refresh button issued a different captcha");
    }

    @Then("the reset form should still be shown")
    public void resetFormShouldStillBeShown() {

        Assert.assertTrue(resetPasswordPage.isShowing(),
                "The reset went through when it should have been refused. Landed on "
                        + resetPasswordPage.getCurrentUrl());

        BaseClass.logger.pass("Still on the reset form");
    }

    @Then("the verification step should be reached")
    public void verificationStepShouldBeReached() {

        Assert.assertTrue(resetPasswordPage.isVerificationStepShowing(),
                "The reset did not reach the verification step. Landed on "
                        + resetPasswordPage.getCurrentUrl());

        BaseClass.logger.pass("Reached the verification step");
    }

    @Then("the verification step should offer to send the code again")
    public void verificationStepShouldOfferResend() {

        Assert.assertTrue(resetPasswordPage.offersResendCode(),
                "The verification step did not offer to resend the code");

        BaseClass.logger.pass("The verification step offers to resend the code");
    }

    @Then("no new password should be set")
    public void noNewPasswordShouldBeSet() {

        // Nothing to do but say so. The account is shared with every other scenario in the
        // suite, so the reset is deliberately abandoned here; the step exists to make that
        // visible in the report rather than to leave a reader wondering.
        BaseClass.logger.pass("The reset was left unfinished on purpose - the password is unchanged");
    }

    @When("I note where the mailbox has got to")
    public void noteMailboxPosition() {

        Assert.assertTrue(resetPasswordPage.canReadTheMailbox(),
                "The mailbox cannot be read, so nothing can be said about whether a code"
                        + " arrived. Check mail.imap.enabled and the mail credential.");

        resetPasswordPage.noteMailboxPosition();

        BaseClass.logger.pass("Noted where the mailbox had got to before asking for a reset");
    }

    /**
     * The point of this step is freshness, not the digits.
     *
     * A reset code goes to the account's real address, which already holds codes from
     * earlier runs, so only a message that arrived after the mailbox was noted proves
     * anything. The code itself is deliberately never entered - doing so would set a new
     * password on the account the whole suite signs in with.
     */
    @Then("a verification code should arrive for the address in {string} of {string} of {string}")
    public void verificationCodeShouldArriveFor(String rowNumber, String excelSheetName,
                                                String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String address = valueAt(excelSheetName, row, IDENTIFIER);
        String code = resetPasswordPage.verificationCodeSentTo(address, MAIL_TIMEOUT_SECONDS);

        Assert.assertFalse(code.isBlank(),
                "No verification code reached " + address + " within " + MAIL_TIMEOUT_SECONDS
                        + " seconds of asking for the reset. Only a message that arrived after"
                        + " the mailbox was noted counts, so an older code in the same inbox"
                        + " does not make this pass.");

        Assert.assertTrue(code.matches("\\d{6}"),
                "Expected a six digit verification code but the mailbox gave \"" + code + "\"");

        BaseClass.logger.pass("A new verification code arrived for " + address);
    }

    @When("I go back from the verification step")
    public void goBackFromVerificationStep() {

        resetPasswordPage.goBack();
    }

    /**
     * Deliberately worded for the back-navigation scenario rather than reusing the refusal
     * step: nothing was refused there, so a failure saying so would send a reader looking
     * for a rejection that never happened.
     */
    @Then("the reset form should be shown again")
    public void resetFormShouldBeShownAgain() {

        Assert.assertTrue(resetPasswordPage.isShowing(),
                "The reset form did not come back. Landed on " + resetPasswordPage.getCurrentUrl());

        BaseClass.logger.pass("The reset form came back");
    }

    /**
     * A cell from the test data, with the blank marker turned back into a blank.
     */
    private String valueAt(String sheetName, int row, int column) {

        String value = excel.getStringData(sheetName, row, column);

        return BLANK.equals(value) ? "" : value;
    }
}
