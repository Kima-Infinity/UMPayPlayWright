package com.umpay.stepdefs;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import com.umpay.pages.HomePage;
import com.umpay.pages.RegisterPage;
import com.umpay.pages.SetupPinPage;
import com.umpay.pages.TermsAndConditionsPage;
import com.umpay.utility.BaseClass;
import com.umpay.utility.ExcelDataProvider;
import com.umpay.utility.Helper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

import java.util.List;

public class RegisterStepDefs {

    /*
     * Column layout of TestData/Register_TestData.xlsx
     * 0 Scenario | 1 Email | 2 Password | 3 PhoneCountry | 4 PhoneNumber
     * 5 CaptchaCode | 6 UniqueEmail | 7 ExpectedMessage | 8 Pin
     */
    private static final int EMAIL = 1;
    private static final int PASSWORD = 2;
    private static final int PHONE_COUNTRY = 3;
    private static final int PHONE_NUMBER = 4;
    private static final int CAPTCHA_CODE = 5;
    private static final int UNIQUE_EMAIL = 6;
    private static final int EXPECTED_MESSAGE = 7;
    private static final int PIN = 8;

    RegisterPage registerPage;
    TermsAndConditionsPage termsPage;
    SetupPinPage pinPage;
    HomePage homePage;
    ExcelDataProvider excel;

    /**
     * The +alias this scenario registered. The verification step matches the
     * emailed code on it, so it has to outlive the step that generated it.
     */
    private String registeredEmail;

    /** The PIN this scenario's row asks for, set on the last screen of registration. */
    private String pin;

    @Given("I am on the UMPay registration page")
    public void openRegistrationPage() {

        registerPage = new RegisterPage(BaseClass.driver);

        registerPage.open(BaseClass.config.getRegisterUrl());

        BaseClass.logger = BaseClass.report.createTest("Register a UMPay account");

        Assert.assertTrue(registerPage.isRegistrationFormDisplayed(), "Registration form was not displayed");

        BaseClass.logger.pass("Registration page opened");
    }

    @When("I register with an email address using {string} of {string} of {string}")
    public void registerWithEmail(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String email = excel.getStringData(excelSheetName, row, EMAIL);

        // A registered address cannot be reused, so the happy path asks for a fresh one.
        if ("Yes".equalsIgnoreCase(excel.getStringData(excelSheetName, row, UNIQUE_EMAIL))) {
            email = Helper.getUniqueEmail(email);
        }

        registeredEmail = email;
        pin = excel.getStringData(excelSheetName, row, PIN);

        registerPage.registerWithEmail(
                email,
                excel.getStringData(excelSheetName, row, PASSWORD),
                excel.getStringData(excelSheetName, row, CAPTCHA_CODE),
                BaseClass.config.getCaptchaManualTimeout());

        BaseClass.logger.pass("Submitted the registration form with the email address " + email);
    }

    @When("I register with a phone number using {string} of {string} of {string}")
    public void registerWithPhoneNumber(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String phoneNumber = excel.getStringData(excelSheetName, row, PHONE_NUMBER);

        registerPage.registerWithPhone(
                excel.getStringData(excelSheetName, row, PHONE_COUNTRY),
                phoneNumber,
                excel.getStringData(excelSheetName, row, PASSWORD),
                excel.getStringData(excelSheetName, row, CAPTCHA_CODE),
                BaseClass.config.getCaptchaManualTimeout());

        BaseClass.logger.pass("Submitted the registration form with the phone number " + phoneNumber);
    }

    @When("I fill the registration form using {string} of {string} of {string} without submitting it")
    public void fillRegistrationFormOnly(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String email = excel.getStringData(excelSheetName, row, EMAIL);

        if ("Yes".equalsIgnoreCase(excel.getStringData(excelSheetName, row, UNIQUE_EMAIL))) {
            email = Helper.getUniqueEmail(email);
        }

        registerPage.fillEmailRegistrationForm(email, excel.getStringData(excelSheetName, row, PASSWORD));

        BaseClass.logger.pass("Filled the registration form without submitting it");
    }

    /**
     * Confirmed on a real run: submitting the form lands on a "Verification Code"
     * screen asking for a six digit code emailed to the address just registered.
     * The account does not exist until that code is accepted, so the happy path
     * needs this step — without it the scenario passes while registering nobody.
     */
    @When("I enter the verification code sent to the email address")
    public void enterEmailVerificationCode() {

        Assert.assertTrue(registerPage.isOtpStepDisplayed(),
                "Expected the verification code step after submitting. Landed on " + registerPage.getCurrentUrl());

        registerPage.completeOtpVerification(registeredEmail, BaseClass.config.getCaptchaManualTimeout());

        BaseClass.logger.pass("Entered the emailed verification code");
    }

    /**
     * A new account is met by two policies before it can use anything - the
     * AML/KYC policy, then Terms and Conditions. The account already exists at this
     * point, so these gate the application rather than the registration, and
     * accepting them is what turns a registered account into a usable one.
     */
    @When("I accept the policies shown to a new account")
    public void acceptNewAccountPolicies() {

        termsPage = new TermsAndConditionsPage(BaseClass.driver);

        Assert.assertTrue(termsPage.isDisplayed(),
                "Expected a policy to accept after verification. Landed on " + registerPage.getCurrentUrl());

        termsPage.acceptAllPolicies();

        Assert.assertTrue(termsPage.waitUntilAccepted(30),
                "A policy modal was still covering the application after accepting them");

        BaseClass.logger.pass("Accepted the policies a new account is shown");
    }

    /**
     * Accepting the policies lands on /setup-pin rather than on home: a new
     * account has to choose a four digit PIN before the application will let it
     * through. This is the last gate of registration.
     */
    @When("I set a PIN security code")
    public void setPinSecurityCode() {

        pinPage = new SetupPinPage(BaseClass.driver);

        Assert.assertTrue(pinPage.isDisplayed(),
                "Expected the PIN screen after accepting the policies. Landed on "
                        + registerPage.getCurrentUrl());

        pinPage.setPin(pin);

        Assert.assertTrue(pinPage.waitUntilDone(30),
                "The application was still asking for a PIN after it was set");

        BaseClass.logger.pass("Set the PIN security code");
    }

    /**
     * The home page is reached with one last prompt over it, offering to set up a
     * Google authenticator. Skipping is the documented way past it; setting one up
     * would tie the account to a secret the suite would have to keep and generate
     * codes from, which is a different test from registration.
     */
    @When("I skip the two factor authentication prompt")
    public void skipTwoFactorPrompt() {

        homePage = new HomePage(BaseClass.driver);

        Assert.assertTrue(homePage.isTwoFactorPromptDisplayed(),
                "Expected the 2FA prompt on the home page. Landed on " + registerPage.getCurrentUrl());

        homePage.skipTwoFactorSetup();

        BaseClass.logger.pass("Skipped the 2FA prompt");
    }

    @Then("I should land on the UMPay home page")
    public void shouldLandOnHomePage() {

        String url = registerPage.getCurrentUrl();
        String title = homePage.getPageTitle();

        System.out.println("Landed on: " + url + " (" + title + ")");

        Assert.assertFalse(termsPage.isDisplayed(),
                "A policy modal is still covering the home page. URL: " + url);

        Assert.assertFalse(pinPage.isDisplayed(),
                "The application is still asking for a PIN. URL: " + url);

        Assert.assertFalse(homePage.isTwoFactorPromptDisplayed(),
                "The 2FA prompt is still covering the home page. URL: " + url);

        // The application titles the landing page "UMPay | Home", which survives the
        // route still reading /v2/term-and-condition for a moment after Confirm.
        Assert.assertTrue(title != null && title.contains("Home"),
                "Expected the home page after accepting the policy but the title was \"" + title
                        + "\" at " + url);

        BaseClass.logger.pass("Landed on the home page at " + url);
    }

    /**
     * Reads what the new account's home page actually shows. This is what makes
     * the flow's ending meaningful: the title alone said "Home" while the 2FA
     * prompt was still covering everything, so the last word belongs to the
     * content underneath rather than to the tab.
     */
    @Then("I should see the account wallets on the home page")
    public void shouldSeeAccountWallets() {

        List<String> wallets = homePage.readHomePage();

        Assert.assertTrue(homePage.getTotalLabel().contains("Total in"),
                "The home page did not show a total balance heading");

        Assert.assertFalse(wallets.isEmpty(),
                "The home page showed no wallets for the new account");

        BaseClass.logger.pass("Home page shows " + wallets.size() + " wallet(s): "
                + String.join(", ", wallets));
    }

    @Then("the registration should be accepted")
    public void registrationShouldBeAccepted() {

        String message = registerPage.getToastMessage();
        String url = registerPage.getCurrentUrl();

        System.out.println("Landed on: " + url);

        Assert.assertFalse(registerPage.isRegistrationFormDisplayed(),
                "Still on the registration form after submitting. Toast: " + message + ", URL: " + url);

        // Leaving the form is not the same as being registered: the verification step
        // sits in between, and stopping there is what made this pass without creating
        // an account.
        Assert.assertFalse(registerPage.isOtpStepDisplayed(),
                "Still waiting for the emailed verification code, so no account was created. URL: " + url);

        BaseClass.logger.pass("Registration accepted. Landed on " + url
                + (message.isBlank() ? "" : " with the message: " + message));
    }

    @Then("the registration should be rejected with the message in {string} of {string} of {string}")
    public void registrationShouldBeRejected(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String expectedMessage = excel.getStringData(excelSheetName, row, EXPECTED_MESSAGE);
        String actualMessage = registerPage.getErrorMessage();

        Assert.assertTrue(registerPage.isRegistrationFormDisplayed(),
                "Registration was accepted but it should have been rejected");

        Assert.assertTrue(actualMessage.toLowerCase().contains(expectedMessage.toLowerCase()),
                "Expected the rejection message to contain '" + expectedMessage + "' but it was '" + actualMessage + "'");

        BaseClass.logger.pass("Registration rejected with the message: " + actualMessage);
    }

    @Then("the browser should reject the {string} field with the message {string}")
    public void fieldShouldFailBrowserValidation(String fieldName, String expectedMessage) {

        String validationMessage = registerPage.getValidationMessage(fieldName);

        Assert.assertFalse(registerPage.isFieldValid(fieldName),
                "The " + fieldName + " field was accepted by the browser but it should not have been");

        Assert.assertEquals(validationMessage, expectedMessage,
                "Unexpected validation message on the " + fieldName + " field");

        System.out.println("Validation message on the " + fieldName + " field: " + validationMessage);

        BaseClass.logger.pass("The " + fieldName + " field was rejected with: " + validationMessage);
    }

    @Then("I should be able to go to the login page from the registration page")
    public void goToLoginPage() {

        registerPage.openLoginPage();

        Assert.assertTrue(registerPage.getCurrentUrl().contains("/login"),
                "The login link did not open the login page. Landed on " + registerPage.getCurrentUrl());

        BaseClass.logger.pass("Navigated from the registration page to the login page");
    }
}
