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

    /** What the platform said when Confirm was pressed, kept for the step that reads it. */
    private String platformSaid = "";

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

    /**
     * Types at the amount box the way a person would, key by key.
     *
     * Not filled in one go: the form works on the box as it is typed, and setting a value on it
     * outright would walk straight past the very handling these scenarios are asking about.
     */
    @When("I type {string} at the withdraw amount box")
    public void typeAtTheWithdrawAmountBox(String typed) {

        withdrawPage.typeAmount(typed);

        System.out.println("Typed \"" + typed + "\" and the box was left holding \""
                + withdrawPage.amountBoxHolds() + "\"");

        BaseClass.logger.pass("Typed \"" + typed + "\" at the amount box");
    }

    @Then("the withdraw amount box should be left holding nothing")
    public void theAmountBoxShouldBeEmpty() {

        String held = withdrawPage.amountBoxHolds();

        org.testng.Assert.assertTrue(held.isEmpty(),
                "The amount box took \"" + held + "\", which is not an amount this wallet could"
                        + " ever pay out");

        BaseClass.logger.pass("The amount box was left holding nothing, as it should be");
    }

    @Then("the withdraw amount box should be left holding {string}")
    public void theAmountBoxShouldHold(String expected) {

        org.testng.Assert.assertEquals(withdrawPage.amountBoxHolds(), expected,
                "The amount box kept something other than what the currency can be paid in");

        BaseClass.logger.pass("The amount box kept " + expected);
    }

    /**
     * More than the wallet holds, and still inside the band the form states.
     *
     * Worked out from the balance the form itself shows rather than written down: a figure in
     * the feature file would go stale the first time the wallet moved, and the point is to ask
     * the platform about the balance rather than to ask the form about its limits.
     */
    @When("I enter more than the wallet holds as the withdraw amount")
    public void enterMoreThanTheWalletHolds() {

        double held = withdrawPage.balanceOfChosenWalletAsANumber();

        String tooMuch = new java.math.BigDecimal(held).setScale(0, java.math.RoundingMode.FLOOR)
                .add(new java.math.BigDecimal(1000)).toPlainString();

        double ceiling = Double.parseDouble(withdrawPage.statedMaximum());

        org.testng.Assert.assertTrue(Double.parseDouble(tooMuch) <= ceiling,
                "The wallet holds " + withdrawPage.balanceOfChosenWallet() + ", and a thousand more"
                        + " than that is above the " + ceiling + " the form will take - so this"
                        + " would be measuring the form's limits rather than the balance");

        withdrawPage.enterAmount(tooMuch);

        System.out.println("The wallet holds " + withdrawPage.balanceOfChosenWallet()
                + " and the form was asked for " + tooMuch);

        BaseClass.logger.pass("Asked to withdraw " + tooMuch + " from a wallet holding "
                + withdrawPage.balanceOfChosenWallet());
    }

    @Then("the withdraw should not be confirmable")
    public void theWithdrawShouldNotBeConfirmable() {

        org.testng.Assert.assertFalse(withdrawPage.canConfirm(),
                "The form will let this withdraw be confirmed with no payout account chosen, so"
                        + " there is nowhere for the money to go and it would be raised anyway");

        System.out.println("Confirm cannot be pressed while no payout account has been chosen");

        BaseClass.logger.pass("Confirm cannot be pressed with nowhere to send the money");
    }

    @Then("the withdraw should be confirmable")
    public void theWithdrawShouldBeConfirmable() {

        org.testng.Assert.assertTrue(withdrawPage.canConfirm(),
                "Confirm still cannot be pressed after a payout account was chosen, so the case"
                        + " above proves nothing about the payout account being what stops it");

        BaseClass.logger.pass("Confirm can be pressed once a payout account has been chosen");
    }

    @When("I choose the saved payout account {string}")
    public void chooseTheSavedPayoutAccount(String named) {

        withdrawPage.chooseSavedPaymentAccount(named);

        BaseClass.logger.pass("Chose the saved payout account " + named);
    }

    /**
     * Presses Confirm and gives the PIN if the platform asks for one.
     *
     * What the platform says is kept rather than thrown: a refusal is the expected answer in
     * these scenarios, and throwing here would fail the case for behaving correctly.
     */
    @When("I confirm the withdraw")
    public void confirmTheWithdraw() {

        platformSaid = withdrawPage.confirmAndSeeWhatThePlatformSays("the withdraw");

        System.out.println("The platform answered: "
                + (platformSaid.isEmpty() ? "(nothing)" : platformSaid));

        BaseClass.logger.pass("Confirmed the withdraw and read the platform's answer");
    }

    @Then("the platform should refuse to raise the order")
    public void thePlatformShouldRefuse() {

        boolean raised = withdrawPage.anOrderWasRaised();

        org.testng.Assert.assertFalse(raised,
                "The platform raised an order for a withdraw of more than the wallet holds ("
                        + withdrawPage.balanceOfChosenWallet() + "), which is money leaving an"
                        + " account that does not have it"
                        + (platformSaid.isEmpty() ? "" : ". It said: " + platformSaid));

        org.testng.Assert.assertFalse(platformSaid.isEmpty(),
                "The platform raised no order, which is right, but said nothing at all about why -"
                        + " so somebody withdrawing from their own wallet would be left pressing"
                        + " Confirm on a form that simply does nothing");

        System.out.println("The platform refused it: " + platformSaid);

        BaseClass.logger.pass("The platform refused to raise the order: " + platformSaid);
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
