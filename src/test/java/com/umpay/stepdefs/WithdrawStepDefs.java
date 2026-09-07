package com.umpay.stepdefs;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import com.umpay.pages.*;
import com.umpay.utility.BaseClass;
import com.umpay.utility.ExcelDataProvider;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class WithdrawStepDefs {

    HomePage homePage;
    WithdrawPage withdrawPage;
    ExcelDataProvider excel;

    /** How many times the sidebar click is worth trying before calling it a failure. */
    private static final int NAVIGATION_ATTEMPTS = 3;

    @When("I navigate to Withdraw page")
    public void i_navigate_to_withdraw_page() throws InterruptedException {

        homePage = new HomePage(BaseClass.driver);
        withdrawPage = new WithdrawPage(BaseClass.driver);

        Thread.sleep(3000);

        homePage.dismissTwoFactorPromptIfShowing();

        // The click is retried rather than trusted once, the same way the Convert page's is.
        // A run of ten withdraws had two scenarios where the sidebar entry was found, reported
        // visible and stable, clicked - and the page stayed on the dashboard. Trusting the
        // click turns that into a failure thirty seconds later about an amount box, which
        // says nothing about what actually went wrong.
        for (int attempt = 1; attempt <= NAVIGATION_ATTEMPTS; attempt++) {

            // Asked again on every attempt, not just once before the first. The prompt does not
            // always arrive within the three seconds above, and one that turns up later is a
            // modal over the sidebar - which is one of the two things seen swallowing the click.
            homePage.dismissTwoFactorPromptIfShowing();

            homePage.openWithdraw();

            if (withdrawPage.waitUntilReady(20)) {
                System.out.println("Navigated to Withdraw Page Successfully!");
                BaseClass.logger.pass("Navigated to Withdraw page");
                return;
            }

            System.out.println("The Withdraw form was not ready at " + withdrawPage.getCurrentUrl()
                    + " (attempt " + attempt + " of " + NAVIGATION_ATTEMPTS + ")");
        }

        // The sidebar has had its chances. Going to the address directly keeps a scenario about
        // withdrawing money from being lost to a dashboard that would not take a click, and says
        // so plainly rather than quietly - a sidebar that stops working is worth knowing about.
        System.out.println("The sidebar would not open the withdraw form; going to its address"
                + " directly.");

        withdrawPage.open();

        if (withdrawPage.waitUntilReady(20)) {
            BaseClass.logger.pass("Navigated to Withdraw page by its address, after the sidebar"
                    + " click was swallowed " + NAVIGATION_ATTEMPTS + " times");
            return;
        }

        // Landing on the login page is a different complaint from a click that would not land,
        // and worth saying plainly: the run is not signed in any more, so no amount of clicking
        // was ever going to open the form.
        String landedOn = withdrawPage.getCurrentUrl();

        org.testng.Assert.fail(landedOn.contains("/login")
                ? "The withdraw form could not be opened because the run is no longer signed in -"
                        + " it was sent to " + landedOn
                : "The Withdraw form did not open, by the sidebar in " + NAVIGATION_ATTEMPTS
                        + " attempts or by its own address. Landed on " + landedOn);
    }

    @Then("I should be able to initiate a withdraw transaction using {string} of {string} of {string}")
    public void submitWithdrawTransaction(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        withdrawPage.submitWithdraw(excel.getStringData(excelSheetName, row, 3),
                excel.getStringData(excelSheetName, row, 4),
                excel.getStringData(excelSheetName, row, 5),
                excel.getStringData(excelSheetName, row, 6),
                excel.getStringData(excelSheetName, row, 7),
                excel.getStringData(excelSheetName, row, 8),
                excel.getStringData(excelSheetName, row, 9),
                excel.getStringData(excelSheetName, row, 10),
                excel.getStringData(excelSheetName, row, 11),
                excel.getStringData(excelSheetName, row, 12),
                excel.getStringData(excelSheetName, row, 13));

        System.out.println("Test Completed!");
        if (BaseClass.logger != null) {
            BaseClass.logger.pass("Initiated Withdraw transaction successfully");
        }
    }

    @When("I choose the {string} wallet on the Withdraw page")
    public void chooseWithdrawCurrency(String currency) {

        withdrawPage.chooseCurrency(currency);
        BaseClass.logger.pass("Chose the " + currency + " wallet");
    }

    @When("I enter {string} as the withdraw amount")
    public void enterWithdrawAmount(String amount) {

        withdrawPage.enterAmount(amount);
        BaseClass.logger.pass("Entered " + amount + " as the withdraw amount");
    }

    /**
     * A figure the form must refuse, worked out from what the form itself says it will take.
     *
     * The bands are the platform's rather than the suite's - HKD starts at 100, VND at
     * 1100000, and the US dollar minimum is a conversion that moves - so a figure written into
     * the feature file would be measuring a number that has since changed. One below the
     * stated minimum is outside the band whatever the band happens to be today.
     */
    @When("I enter one below the stated minimum as the withdraw amount")
    public void enterOneBelowTheStatedMinimum() {

        String tooLittle = new java.math.BigDecimal(withdrawPage.statedMinimum())
                .subtract(java.math.BigDecimal.ONE).toPlainString();

        withdrawPage.enterAmount(tooLittle);
        BaseClass.logger.pass("Entered " + tooLittle + ", one below the stated minimum of "
                + withdrawPage.statedMinimum());
    }

    @When("I enter one above the stated maximum as the withdraw amount")
    public void enterOneAboveTheStatedMaximum() {

        String tooMuch = new java.math.BigDecimal(withdrawPage.statedMaximum())
                .add(java.math.BigDecimal.ONE).toPlainString();

        withdrawPage.enterAmount(tooMuch);
        BaseClass.logger.pass("Entered " + tooMuch + ", one above the stated maximum of "
                + withdrawPage.statedMaximum());
    }

    @When("I enter the stated minimum as the withdraw amount")
    public void enterTheStatedMinimum() {

        withdrawPage.enterAmount(withdrawPage.statedMinimum());
        BaseClass.logger.pass("Entered the stated minimum of " + withdrawPage.statedMinimum());
    }

    @Then("the withdraw amount should be refused with {string}")
    public void withdrawAmountShouldBeRefused(String expected) {

        org.testng.Assert.assertFalse(withdrawPage.isAmountValid(),
                "The browser accepted the amount, but it is outside the stated limits of "
                        + withdrawPage.statedMinimum() + " to " + withdrawPage.statedMaximum());

        org.testng.Assert.assertEquals(withdrawPage.amountValidationMessage(), expected,
                "Unexpected wording on the amount box");

        BaseClass.logger.pass("The amount was refused with: " + expected);
    }

    @Then("the withdraw amount should be accepted")
    public void withdrawAmountShouldBeAccepted() {

        org.testng.Assert.assertTrue(withdrawPage.isAmountValid(),
                "The browser refused the amount with: " + withdrawPage.amountValidationMessage());

        BaseClass.logger.pass("The amount was accepted");
    }

    @Then("the withdraw currency list should offer {string}")
    public void withdrawCurrencyListShouldOffer(String expected) {

        java.util.List<String> offered = new java.util.ArrayList<>(withdrawPage.walletBalances().keySet());
        java.util.Collections.sort(offered);

        java.util.List<String> wanted = new java.util.ArrayList<>();
        for (String currency : expected.split(",")) {
            wanted.add(currency.trim());
        }
        java.util.Collections.sort(wanted);

        org.testng.Assert.assertEquals(offered, wanted,
                "The withdraw currency list does not offer what it used to");

        BaseClass.logger.pass("The withdraw list offers " + offered);
    }

    @Then("the withdraw order status should be {string}")
    public void withdrawOrderStatusShouldBe(String expectedStatus) {

        String actualStatus = withdrawPage.getSubmittedOrderStatus();

        org.testng.Assert.assertEquals(actualStatus, expectedStatus,
                "Unexpected status on the submitted withdraw order");

        BaseClass.logger.pass("Withdraw order status is " + actualStatus);
    }
}
