package com.umpay.stepdefs;

import com.umpay.pages.ResetPasswordPage;
import com.umpay.utility.BaseClass;
import com.umpay.utility.ExcelDataProvider;
import com.umpay.utility.MailUtils;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.testng.Assert;

/**
 * Resetting a forgotten password.
 *
 * <p>Column layout of TestData/ResetPassword_TestData.xlsx, sheet ResetPassword
 * 0 Scenario | 1 Country | 2 Identifier | 3 Captcha | 4 ExpectedMessage
 *
 * <p>THE RESET IS NEVER COMPLETED. The form ends at a step where a new password would be set,
 * and setting one would change the password the whole suite signs in with. Every step here
 * stops short of that.
 *
 * <p>These steps and the scenarios that use them lived in LoginToPageStepDefs and Login.feature
 * until the reset cases were given a module of their own. Nothing about what they do has
 * changed - they were moved across whole, and the tags the workbook links to are untouched.
 */
public class ResetPasswordStepDefs {

    /*
     * Column positions in the ResetPassword sheet. Scenario, Country and Identifier sit where
     * they do in the login sheets; the last two differ because this form asks for a captcha
     * rather than a password.
     */
    private static final int COUNTRY = 1;

    private static final int IDENTIFIER = 2;

    private static final int RESET_CAPTCHA = 3;

    private static final int RESET_EXPECTED_MESSAGE = 4;

    /** How long to keep watching the mailbox for the reset code. */
    private static final int MAIL_TIMEOUT_SECONDS = 120;

    /**
     * What a blank cell is written as.
     *
     * A genuinely empty cell reads back as a missing row entry rather than an empty string, so a
     * scenario that submits a blank box says so in a word instead.
     */
    private static final String BLANK = "EMPTY";

    ResetPasswordPage resetPasswordPage;

    ExcelDataProvider excel;

    /** The captcha picture as it was before the refresh button was pressed. */
    private String captchaBeforeRefresh;

    /**
     * The reset page, built the first time a step asks for it.
     *
     * Built lazily rather than only by the step that opens the form, because the form can also be
     * opened by the shared "I am on the UMPay ... page" step, which belongs to another class and
     * cannot reach in here to set this. Before this was lazy, any scenario starting that way went
     * on to the next step with nothing to talk to.
     */
    private ResetPasswordPage reset() {

        if (resetPasswordPage == null) {
            resetPasswordPage = new ResetPasswordPage(BaseClass.driver);
        }

        return resetPasswordPage;
    }

    @Given("I am on the UMPay password reset page")
    public void openResetPasswordPage() {

        resetPasswordPage = new ResetPasswordPage(BaseClass.driver);

        reset().open(BaseClass.config.getResetPasswordUrl());

        if (BaseClass.logger == null) {
            BaseClass.logger = BaseClass.report.createTest("UMPay password reset");
        }

        Assert.assertTrue(reset().isShowing(),
                "The reset form was not shown. Landed on " + reset().getCurrentUrl());

        BaseClass.logger.pass("Password reset page opened");
    }

    @When("I fill the phone reset form from {string} of {string} of {string} without sending it")
    public void fillPhoneResetForm(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        reset().fillPhoneForm(valueAt(excelSheetName, row, COUNTRY),
                valueAt(excelSheetName, row, IDENTIFIER),
                valueAt(excelSheetName, row, RESET_CAPTCHA));

        BaseClass.logger.pass("Filled the phone reset form from row " + row);
    }

    @When("I fill the email reset form from {string} of {string} of {string} without sending it")
    public void fillEmailResetForm(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        reset().fillEmailForm(valueAt(excelSheetName, row, IDENTIFIER),
                valueAt(excelSheetName, row, RESET_CAPTCHA));

        BaseClass.logger.pass("Filled the email reset form from row " + row);
    }

    @When("I send the reset form")
    public void sendTheResetForm() {

        reset().submit();
    }

    @When("I ask to reset the password by phone using {string} of {string} of {string}")
    public void requestResetByPhone(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        reset().requestResetByPhone(valueAt(excelSheetName, row, COUNTRY),
                valueAt(excelSheetName, row, IDENTIFIER),
                valueAt(excelSheetName, row, RESET_CAPTCHA));

        BaseClass.logger.pass("Asked for a reset by phone using row " + row
                + ": " + excel.getStringData(excelSheetName, row, 0));
    }

    @When("I ask to reset the password by email using {string} of {string} of {string}")
    public void requestResetByEmail(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        reset().requestResetByEmail(valueAt(excelSheetName, row, IDENTIFIER),
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

        reset().requestResetByPhoneDiallingCode(parts[0], parts[1],
                ResetPasswordPage.AUTO_CAPTCHA);

        BaseClass.logger.pass("Asked for a reset for +" + parts[0] + " " + parts[1]);
    }

    @Then("the browser should reject the reset {string} field with the message in {string} of {string} of {string}")
    public void browserShouldRejectResetField(String fieldName, String rowNumber,
                                              String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String expected = valueAt(excelSheetName, row, RESET_EXPECTED_MESSAGE);

        Assert.assertFalse(reset().isFieldValid(fieldName),
                "The " + fieldName + " field was accepted by the browser but it should not have been");

        Assert.assertEquals(reset().validationMessageFor(fieldName), expected,
                "Unexpected validation message on the " + fieldName + " field");

        BaseClass.logger.pass("The " + fieldName + " field was rejected with: " + expected);
    }

    @Then("the reset form should complain with the message in {string} of {string} of {string}")
    public void resetFormShouldComplainWith(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String expected = valueAt(excelSheetName, row, RESET_EXPECTED_MESSAGE);

        Assert.assertEquals(reset().fieldError(), expected,
                "Unexpected complaint under the field");

        BaseClass.logger.pass("The reset form complained: " + expected);
    }

    @Then("the reset should be refused with the message in {string} of {string} of {string}")
    public void resetShouldBeRefusedWith(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String expected = valueAt(excelSheetName, row, RESET_EXPECTED_MESSAGE);
        String actual = reset().errorMessage();

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

        Assert.assertEquals(reset().phoneAsTyped(), typed,
                "The phone number field did not keep what was typed into it");

        BaseClass.logger.pass("The phone number field kept \"" + typed + "\"");
    }

    @When("I ask the reset form for a new captcha")
    public void askForANewCaptcha() {

        captchaBeforeRefresh = reset().captchaSource();

        Assert.assertFalse(captchaBeforeRefresh.isBlank(),
                "There was no captcha image on the reset form to refresh");

        reset().refreshCaptcha();
    }

    @Then("a different captcha image should be shown")
    public void aDifferentCaptchaShouldBeShown() {

        String after = reset().captchaSource();

        Assert.assertFalse(after.isBlank(), "The captcha image went away instead of changing");

        Assert.assertNotEquals(after, captchaBeforeRefresh,
                "The refresh button left the same captcha image on the form");

        BaseClass.logger.pass("The refresh button issued a different captcha");
    }

    @Then("the reset form should still be shown")
    public void resetFormShouldStillBeShown() {

        Assert.assertTrue(reset().isShowing(),
                "The reset went through when it should have been refused. Landed on "
                        + reset().getCurrentUrl());

        BaseClass.logger.pass("Still on the reset form");
    }

    @Then("the verification step should be reached")
    public void verificationStepShouldBeReached() {

        Assert.assertTrue(reset().isVerificationStepShowing(),
                "The reset did not reach the verification step. Landed on "
                        + reset().getCurrentUrl());

        BaseClass.logger.pass("Reached the verification step");
    }

    @Then("the verification step should offer to send the code again")
    public void verificationStepShouldOfferResend() {

        Assert.assertTrue(reset().offersResendCode(),
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

        Assert.assertTrue(reset().canReadTheMailbox(),
                "The mailbox cannot be read, so nothing can be said about whether a code"
                        + " arrived. Check mail.imap.enabled and the mail credential.");

        reset().noteMailboxPosition();

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
        String code = reset().verificationCodeSentTo(address, MAIL_TIMEOUT_SECONDS);

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

        reset().goBack();
    }

    /**
     * Deliberately worded for the back-navigation scenario rather than reusing the refusal
     * step: nothing was refused there, so a failure saying so would send a reader looking
     * for a rejection that never happened.
     */
    @Then("the reset form should be shown again")
    public void resetFormShouldBeShownAgain() {

        Assert.assertTrue(reset().isShowing(),
                "The reset form did not come back. Landed on " + reset().getCurrentUrl());

        BaseClass.logger.pass("The reset form came back");
    }

    /**
     * A cell from the test data, with the blank marker turned back into a blank.
     *
     * The same helper the login steps keep, copied rather than shared: one Cucumber step class
     * cannot reach into another, and a couple of lines duplicated is a cheaper price than the
     * two files having to know about each other.
     */
    private String valueAt(String sheetName, int row, int column) {

        String value = excel.getStringData(sheetName, row, column);

        return BLANK.equals(value) ? "" : value;
    }
}
