package com.umpay.stepdefs;

import com.umpay.pages.FeeListingPage;
import com.umpay.pages.HomePage;
import com.umpay.utility.BaseClass;
import com.umpay.utility.ExcelDataProvider;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * What UMPay charges in each currency.
 *
 * <p>Column layout of TestData/FeeListing_TestData.xlsx
 * 0 Scenario | 1 LoginID | 2 Password | 3 Currency
 *
 * <p>Nothing here changes anything: a fee schedule is the platform's to set and this page only
 * shows it.
 */
public class FeeListingStepDefs {

    /** How many times opening the page is worth trying, as elsewhere in this suite. */
    private static final int NAVIGATION_ATTEMPTS = 3;

    private FeeListingPage feePage;

    private ExcelDataProvider excel;

    /** The currency this scenario asked the fees for. */
    private String askedFor = "";

    private FeeListingPage fees() {

        if (feePage == null) {
            feePage = new FeeListingPage(BaseClass.driver);
        }

        return feePage;
    }

    private String fromTheSheet(String rowNumber, String sheetName, String fileName, String columnName) {

        excel = new ExcelDataProvider(fileName, sheetName);

        for (int column = 0; column < 20; column++) {

            String heading;

            try {
                heading = excel.getStringData(sheetName, 0, column);
            } catch (Exception noHeadingHere) {
                continue;
            }

            if (columnName.equalsIgnoreCase(String.valueOf(heading).trim())) {
                try {
                    return excel.getStringData(sheetName, Integer.parseInt(rowNumber), column);
                } catch (Exception blank) {
                    return "";
                }
            }
        }

        throw new IllegalStateException("The sheet " + fileName + " has no column called \""
                + columnName + "\"");
    }

    /** "a, b, c" as the feature writes it, into the things it names. */
    private List<String> named(String commaSeparated) {

        List<String> each = new ArrayList<>();

        for (String one : commaSeparated.split(",")) {

            if (!one.trim().isEmpty()) {
                each.add(one.trim());
            }
        }

        return each;
    }

    /** What the platform said, if it answered with a dialog of its own, ready to be appended. */
    private String refused() {

        String said = fees().refusalShowing();

        return said.isEmpty() ? "" : ". The platform said: \"" + said + "\"";
    }

    @When("I open the Fee Listing")
    public void openTheFeeListing() throws InterruptedException {

        HomePage home = new HomePage(BaseClass.driver);

        Thread.sleep(3000);

        home.dismissTwoFactorPromptIfShowing();

        fees().waitUntilSignedIn(30);

        for (int attempt = 1; attempt <= NAVIGATION_ATTEMPTS; attempt++) {

            try {
                fees().open();
            } catch (Exception notOpened) {
                System.out.println("The fee listing did not open (attempt " + attempt + " of "
                        + NAVIGATION_ATTEMPTS + "): " + notOpened.getMessage());
                continue;
            }

            if (fees().isShowing()) {

                BaseClass.logger.pass("Opened the fee listing at " + fees().getCurrentUrl());
                return;
            }
        }

        org.testng.Assert.fail("The fee listing did not open. Landed on " + fees().getCurrentUrl()
                + refused());
    }

    @Then("the fee listing should be shown")
    public void theFeeListingShouldBeShown() {

        assertTrue(fees().isShowing(),
                "The fee listing is not on the screen. The page is at " + fees().getCurrentUrl()
                        + refused());

        assertFalse(fees().currenciesOffered().isEmpty(),
                "The page opened but offers no currency at all, so there is nothing to ask the"
                        + " fees for. It reads: " + fees().text());

        System.out.println("The fee listing opened at " + fees().getCurrentUrl() + ", offering "
                + fees().currenciesOffered());

        BaseClass.logger.pass("The fee listing opened at " + fees().getCurrentUrl() + ", offering "
                + fees().currenciesOffered().size() + " currencies");
    }

    @Then("the currencies offered should be {string}")
    public void theCurrenciesOfferedShouldBe(String expected) {

        List<String> offered = fees().currenciesOffered();
        List<String> wanted = named(expected);
        List<String> missing = new ArrayList<>();

        for (String currency : wanted) {

            if (!offered.contains(currency)) {
                missing.add(currency);
            }
        }

        assertTrue(missing.isEmpty(),
                "The fees can no longer be asked for: " + missing + ". The page offers: " + offered);

        assertEquals(offered.size(), wanted.size(),
                "The page offers " + offered.size() + " currencies rather than " + wanted.size()
                        + ": " + offered);

        System.out.println("The fees can be asked for " + offered);

        BaseClass.logger.pass("The fees can be asked for all " + wanted.size() + ": " + offered);
    }

    @Then("each currency should be offered exactly once")
    public void eachCurrencyShouldBeOfferedOnce() {

        List<String> offered = fees().currenciesOffered();

        assertFalse(offered.isEmpty(), "There are no currencies to read" + refused());

        List<String> twice = new ArrayList<>();
        List<String> seen = new ArrayList<>();

        for (String currency : offered) {

            if (seen.contains(currency) && !twice.contains(currency)) {
                twice.add(currency);
            }

            seen.add(currency);
        }

        assertTrue(twice.isEmpty(),
                "These currencies are offered more than once: " + twice + ", so pressing one of"
                        + " them and pressing the other could answer differently");

        System.out.println("All " + offered.size() + " currencies are offered once each");

        BaseClass.logger.pass("All " + offered.size() + " currencies are offered exactly once");
    }

    @Then("it should ask which currency the fees are wanted for")
    public void itShouldAskWhichCurrency() {

        assertTrue(fees().asksForACurrency(),
                "The page does not say what the list of currencies is for, so nobody knows what"
                        + " pressing one would tell them. It reads: " + fees().text());

        System.out.println("The page asks which currency the fees are wanted for");

        BaseClass.logger.pass("The page asks which currency the fees are wanted for");
    }

    @When("I ask for the fees in the currency named in {string} of {string} of {string}")
    public void askForTheFeesIn(String rowNumber, String sheetName, String fileName) {

        askedFor = fromTheSheet(rowNumber, sheetName, fileName, "Currency");

        assertFalse(askedFor.trim().isEmpty(),
                "Row " + rowNumber + " of " + fileName + " names no currency to ask the fees for");

        fees().choose(askedFor);

        BaseClass.logger.pass("Asked for the fees in " + askedFor);
    }

    /**
     * The currency that was pressed is shown as the one chosen.
     *
     * Somebody who presses a currency and is shown no sign that it took presses it again, and
     * again, with nothing to tell them whether the page heard them.
     */
    @Then("that currency should be shown as the one chosen")
    public void itShouldBeShownAsChosen() {

        assertEquals(fees().currencyChosen(), askedFor,
                "\"" + askedFor + "\" was pressed but the page shows \"" + fees().currencyChosen()
                        + "\" as the currency chosen, so there is nothing on the screen to say the"
                        + " press was heard" + refused());

        System.out.println(askedFor + " is shown as the currency chosen");

        BaseClass.logger.pass(askedFor + " is shown as the currency chosen");
    }

    /**
     * The page shows what the chosen currency costs.
     *
     * This is the whole point of the page: a fee listing that lists no fees tells nobody what
     * anything costs, and the cost is the one thing somebody comes here to find out.
     */
    @Then("it should show what that currency costs")
    public void itShouldShowWhatItCosts() {

        List<String> shown = fees().feesShown();

        assertFalse(shown.isEmpty(),
                "The fees in " + askedFor + " were asked for and the page shows no fee, no charge"
                        + " and no rate of any kind. All it holds is the list of currencies: "
                        + fees().text() + refused());

        System.out.println("The fees in " + askedFor + " read: " + shown);

        BaseClass.logger.pass("The fees in " + askedFor + " read: " + shown);
    }
}
