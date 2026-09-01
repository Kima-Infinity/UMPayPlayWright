package com.umpay.stepdefs;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import com.umpay.pages.ConvertPage;
import com.umpay.pages.HomePage;
import com.umpay.utility.BaseClass;
import com.umpay.utility.ExcelDataProvider;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

import java.math.BigDecimal;

public class ConvertStepDefs {

    /*
     * Column layout of TestData/Convert_TestData.xlsx
     * 0 Scenario | 1 LoginID | 2 Password | 3 FromCurrency | 4 ToCurrency
     * 5 Amount | 6 ExpectedMessage
     */
    private static final int FROM_CURRENCY = 3;
    private static final int TO_CURRENCY = 4;
    private static final int AMOUNT = 5;
    private static final int EXPECTED_MESSAGE = 6;

    /** How long to give the application to answer a submitted conversion. */
    private static final int OUTCOME_TIMEOUT_SECONDS = 30;

    HomePage homePage;
    ConvertPage convertPage;
    ExcelDataProvider excel;

    /** The source balance as it stood before converting, for comparison after. */
    private BigDecimal balanceBefore;

    /** The amount taken out of the source wallet, from the test data. */
    private BigDecimal amountConverted;

    /** How many times to ask for the Convert page before giving up on it. */
    private static final int NAVIGATION_ATTEMPTS = 3;

    @When("I navigate to the Convert page")
    public void navigateToConvertPage() {

        homePage = new HomePage(BaseClass.driver);
        convertPage = new ConvertPage(BaseClass.driver);

        homePage.skipTwoFactorSetup();

        // The click is retried rather than trusted once: dismissing the 2FA dialog
        // leaves it fading for a moment, and a click that lands on the fading
        // backdrop is swallowed without any error - the page simply stays on home.
        for (int attempt = 1; attempt <= NAVIGATION_ATTEMPTS; attempt++) {

            homePage.openConvert();

            if (convertPage.waitUntilReady(20)) {
                BaseClass.logger.pass("Navigated to the Convert page");
                return;
            }

            System.out.println("The Convert form was not ready at " + convertPage.getCurrentUrl()
                    + " (attempt " + attempt + " of " + NAVIGATION_ATTEMPTS + ")");
        }

        Assert.fail("The Convert form did not open after " + NAVIGATION_ATTEMPTS
                + " attempts. Landed on " + convertPage.getCurrentUrl());
    }

    @When("I convert the amount in {string} of {string} of {string}")
    public void convertTheAmount(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String from = excel.getStringData(excelSheetName, row, FROM_CURRENCY);
        String to = excel.getStringData(excelSheetName, row, TO_CURRENCY);
        String amount = excel.getStringData(excelSheetName, row, AMOUNT);

        convertPage.selectFromCurrency(from);
        convertPage.selectToCurrency(to);
        convertPage.enterAmount(amount);

        // Read the balance after the wallets are chosen: it belongs to whichever
        // wallet is being converted from, so reading it earlier could measure a
        // different one.
        balanceBefore = convertPage.getSourceBalanceAmount();
        amountConverted = new BigDecimal(amount);

        String quote = convertPage.waitForQuote(20);

        System.out.println(convertPage.getRate());
        System.out.println("Converting " + amount + " " + from + " to " + to
                + ", expecting " + quote);
        System.out.println(from + " balance before: " + balanceBefore);

        convertPage.submit();

        BaseClass.logger.pass("Submitted a conversion of " + amount + " " + from + " to " + to);
    }

    @Then("the conversion should be confirmed with the message in {string} of {string} of {string}")
    public void conversionShouldBeConfirmed(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String expected = excel.getStringData(excelSheetName, row, EXPECTED_MESSAGE);
        String actual = convertPage.waitForSuccessMessage(OUTCOME_TIMEOUT_SECONDS);

        Assert.assertFalse(actual.isBlank(),
                "The conversion produced no confirmation within " + OUTCOME_TIMEOUT_SECONDS + " seconds");

        Assert.assertTrue(actual.contains(expected),
                "Expected the conversion to be confirmed with \"" + expected + "\" but it said \"" + actual + "\"");

        convertPage.acknowledgeSuccess();

        BaseClass.logger.pass("Conversion confirmed: " + actual);
    }

    /**
     * The confirmation dialog only says the request was accepted. This checks the
     * source wallet actually paid for it, which is the part a conversion test
     * exists to prove.
     */
    @Then("the source wallet balance should have gone down by the converted amount")
    public void sourceBalanceShouldHaveGoneDown() {

        BigDecimal after = convertPage.waitForBalanceBelow(balanceBefore, OUTCOME_TIMEOUT_SECONDS);
        BigDecimal taken = balanceBefore.subtract(after);

        System.out.println("Source balance after: " + after + " (down by " + taken + ")");

        Assert.assertTrue(after.compareTo(balanceBefore) < 0,
                "The source wallet balance did not go down. It was " + balanceBefore
                        + " before the conversion and " + after + " after it");

        Assert.assertEquals(taken.stripTrailingZeros(), amountConverted.stripTrailingZeros(),
                "The source wallet went down by " + taken + " but " + amountConverted + " was converted");

        BaseClass.logger.pass("Source wallet went from " + balanceBefore + " to " + after);
    }
}
