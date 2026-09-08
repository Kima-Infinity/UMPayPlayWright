package com.umpay.stepdefs;

import com.umpay.pages.CommissionListingPage;
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
 * What this account has earned in commission.
 *
 * <p>Column layout of TestData/CommissionListing_TestData.xlsx
 * 0 Scenario | 1 LoginID | 2 Password | 3 Type | 4 FromDate | 5 ToDate
 *
 * <p>Nothing here changes anything: commission is credited by the platform and this page only
 * shows it. The filters are cleared with Reset before a scenario that used them ends, so the page
 * is left the way it was found.
 */
public class CommissionListingStepDefs {

    /** How many times opening the page is worth trying, as elsewhere in this suite. */
    private static final int NAVIGATION_ATTEMPTS = 3;

    private CommissionListingPage commissionPage;

    private ExcelDataProvider excel;

    /** How many were listed before anything was asked for. */
    private int listedToBeginWith;

    /** The kind of commission this scenario asked for. */
    private String askedFor = "";

    /** The range this scenario asked for. */
    private String askedFrom = "";

    private String askedTo = "";

    private CommissionListingPage commissions() {

        if (commissionPage == null) {
            commissionPage = new CommissionListingPage(BaseClass.driver);
        }

        return commissionPage;
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

        String said = commissions().refusalShowing();

        return said.isEmpty() ? "" : ". The platform said: \"" + said + "\"";
    }

    @When("I open the Commission Listing")
    public void openTheCommissionListing() throws InterruptedException {

        HomePage home = new HomePage(BaseClass.driver);

        Thread.sleep(3000);

        home.dismissTwoFactorPromptIfShowing();

        commissions().waitUntilSignedIn(30);

        for (int attempt = 1; attempt <= NAVIGATION_ATTEMPTS; attempt++) {

            try {
                commissions().open();
            } catch (Exception notOpened) {
                System.out.println("The commission listing did not open (attempt " + attempt
                        + " of " + NAVIGATION_ATTEMPTS + "): " + notOpened.getMessage());
                continue;
            }

            if (commissions().isShowing()) {

                listedToBeginWith = commissions().howManyListed();

                BaseClass.logger.pass("Opened the commission listing at "
                        + commissions().getCurrentUrl() + ", showing " + listedToBeginWith);
                return;
            }
        }

        org.testng.Assert.fail("The commission listing did not open. Landed on "
                + commissions().getCurrentUrl() + refused());
    }

    @Then("the commissions should be listed")
    public void theCommissionsShouldBeListed() {

        assertTrue(commissions().isShowing(),
                "The commission listing is not on the screen. The page is at "
                        + commissions().getCurrentUrl() + refused());

        assertFalse(commissions().howManyListed() == 0 && !commissions().saysThereIsNothing(),
                "The page lists no commissions and does not say there are none, so there is no"
                        + " telling whether this account has earned nothing or whether the listing"
                        + " failed to load");

        System.out.println("The listing shows " + commissions().howManyListed()
                + " commissions from " + commissions().sourcesListed());

        BaseClass.logger.pass("The listing shows " + commissions().howManyListed()
                + " commissions");
    }

    @Then("every commission should carry a reference, an amount and a date")
    public void everyCommissionShouldCarryTheThree() {

        List<String> listed = commissions().listed();

        assertFalse(listed.isEmpty(), "There are no commissions to read" + refused());

        List<String> incomplete = new ArrayList<>();

        for (String commission : listed) {

            for (String label : new String[] {"Reference No:", "Amount:", "Created Date:"}) {

                if (commissions().onTheCard(commission, label).isEmpty()) {
                    incomplete.add(label + " missing from \"" + commission + "\"");
                }
            }
        }

        assertTrue(incomplete.isEmpty(),
                "These commissions are missing part of what they should say: " + incomplete);

        System.out.println("All " + listed.size() + " commissions carry a reference, an amount and"
                + " a date. The first: " + listed.get(0));

        BaseClass.logger.pass("All " + listed.size() + " commissions carry a reference, an amount"
                + " and a date");
    }

    @Then("every amount should be stated in a currency")
    public void everyAmountShouldHaveACurrency() {

        List<String> listed = commissions().listed();

        assertFalse(listed.isEmpty(), "There are no commissions to read" + refused());

        List<String> without = new ArrayList<>();

        for (String commission : listed) {

            String amount = commissions().onTheCard(commission, "Amount:");

            // An amount reads as its currency and then the figure: "HKD 1.00".
            if (!amount.matches("[A-Z]{3} .*\\d.*")) {
                without.add(amount.isEmpty() ? "(nothing)" : amount);
            }
        }

        assertTrue(without.isEmpty(),
                "These amounts do not say which currency they are in: " + without);

        System.out.println("Every amount names its currency. The first: "
                + commissions().onTheCard(listed.get(0), "Amount:"));

        BaseClass.logger.pass("All " + listed.size() + " amounts name the currency they were"
                + " earned in");
    }

    @Then("every commission should say what earned it and whether it is settled")
    public void everyCommissionShouldSayBoth() {

        List<String> sources = commissions().sourcesListed();
        List<String> states = commissions().statesListed();

        assertFalse(sources.isEmpty(), "There are no commissions to read" + refused());

        List<String> unnamed = new ArrayList<>();

        for (String source : sources) {

            if (source.trim().isEmpty()) {
                unnamed.add("(nothing)");
            }
        }

        assertTrue(unnamed.isEmpty(),
                "These commissions do not say what earned them: " + unnamed);

        assertEquals(states.size(), sources.size(),
                "The listing draws " + sources.size() + " commissions but only " + states.size()
                        + " of them say whether they have been settled");

        System.out.println("The commissions were earned by " + sources + ", and are " + states);

        BaseClass.logger.pass("All " + sources.size() + " commissions say what earned them - "
                + sources + " - and whether they are settled");
    }

    @Then("the types offered should be {string}")
    public void theTypesOfferedShouldBe(String expected) {

        List<String> offered = commissions().typesOffered();
        List<String> wanted = named(expected);
        List<String> missing = new ArrayList<>();

        for (String type : wanted) {

            if (!offered.contains(type)) {
                missing.add(type);
            }
        }

        assertTrue(missing.isEmpty(),
                "The listing can no longer be asked for: " + missing + ". It offers: " + offered);

        System.out.println("The listing can be asked for " + offered);

        BaseClass.logger.pass("The listing can be asked for all " + wanted.size() + ": " + offered);
    }

    @When("I ask for the commissions named in {string} of {string} of {string}")
    public void askForTheCommissionsNamed(String rowNumber, String sheetName, String fileName) {

        askedFor = fromTheSheet(rowNumber, sheetName, fileName, "Type");

        assertFalse(askedFor.trim().isEmpty(),
                "Row " + rowNumber + " of " + fileName + " names no kind of commission to ask for");

        commissions().askFor(askedFor);
        commissions().confirm();

        BaseClass.logger.pass("Asked for the " + askedFor + " commissions");
    }

    @Then("only commissions of that kind should be listed")
    public void onlyThatKindShouldBeListed() {

        if (commissions().howManyListed() == 0) {

            assertTrue(commissions().saysThereIsNothing(),
                    "Asking for the " + askedFor + " commissions left nothing on the page, and the"
                            + " page does not say so either");

            System.out.println("There are no " + askedFor + " commissions, and the page says so");

            BaseClass.logger.pass("There are no " + askedFor + " commissions, and the page says so");
            return;
        }

        List<String> states = commissions().statesListed();
        List<String> wrong = new ArrayList<>();

        for (String state : states) {

            if (!state.equalsIgnoreCase(askedFor)) {
                wrong.add(state);
            }
        }

        assertTrue(wrong.isEmpty(),
                "The " + askedFor + " commissions were asked for but the listing still holds "
                        + wrong + refused());

        System.out.println("Asking for " + askedFor + " left " + states.size()
                + " commissions, every one of them " + askedFor);

        BaseClass.logger.pass("Asking for " + askedFor + " left " + states.size()
                + " commissions, all of them " + askedFor);
    }

    @When("I ask for the commissions between the dates in {string} of {string} of {string}")
    public void askForTheCommissionsBetween(String rowNumber, String sheetName, String fileName) {

        askedFrom = fromTheSheet(rowNumber, sheetName, fileName, "FromDate");
        askedTo = fromTheSheet(rowNumber, sheetName, fileName, "ToDate");

        assertFalse(askedFrom.trim().isEmpty() || askedTo.trim().isEmpty(),
                "Row " + rowNumber + " of " + fileName + " does not give both ends of a range");

        commissions().askForBetween(askedFrom, askedTo);
        commissions().confirm();

        BaseClass.logger.pass("Asked for the commissions between " + askedFrom + " and " + askedTo);
    }

    @Then("it should say there is nothing to show")
    public void itShouldSayThereIsNothing() {

        assertEquals(commissions().howManyListed(), 0,
                "The range " + askedFrom + " to " + askedTo + " was asked for, which is before this"
                        + " account existed, and the listing still holds "
                        + commissions().howManyListed() + " commissions");

        assertTrue(commissions().saysThereIsNothing(),
                "The listing is empty but says nothing about it, so an account that has earned"
                        + " nothing cannot be told from a page that failed to load. It reads: "
                        + commissions().text());

        System.out.println("Between " + askedFrom + " and " + askedTo + " the page says there is"
                + " nothing to show");

        BaseClass.logger.pass("A range holding nothing is answered with \"No data\" rather than an"
                + " empty page");
    }

    @When("I clear the filters")
    public void clearTheFilters() {

        commissions().reset();

        BaseClass.logger.pass("Cleared the filters");
    }

    /**
     * Reset gives back everything, and leaves the page as it was found.
     *
     * Both halves matter: the listing has to come back, and the boxes have to be empty, or the
     * next thing anybody does starts from a filter they did not set.
     */
    @Then("the whole listing should come back")
    public void theWholeListingShouldComeBack() {

        assertEquals(commissions().howManyListed(), listedToBeginWith,
                "The listing showed " + listedToBeginWith + " commissions before anything was"
                        + " asked for and shows " + commissions().howManyListed() + " after Reset"
                        + refused());

        assertTrue(commissions().typeAskedFor().isEmpty()
                        && commissions().fromDateAskedFor().isEmpty()
                        && commissions().toDateAskedFor().isEmpty(),
                "Reset left the filters holding type=\"" + commissions().typeAskedFor()
                        + "\", from=\"" + commissions().fromDateAskedFor() + "\", to=\""
                        + commissions().toDateAskedFor() + "\"");

        System.out.println("Reset brought back all " + listedToBeginWith
                + " commissions and emptied the filters");

        BaseClass.logger.pass("Reset brought back all " + listedToBeginWith + " commissions and"
                + " emptied the filters");
    }

    @When("I open the wallets the commissions can be narrowed to")
    public void openTheWallets() {

        commissions().openTheWallets();

        BaseClass.logger.pass("Opened the wallets the commissions can be narrowed to");
    }

    @Then("every wallet the account holds should be offered to narrow by")
    public void everyWalletShouldBeOffered() {

        List<String> wallets = commissions().walletsOffered();

        assertFalse(wallets.isEmpty(),
                "The wallets did not open, so the commissions cannot be looked at one currency at"
                        + " a time. The page is at " + commissions().getCurrentUrl() + refused());

        assertTrue(wallets.size() > 1,
                "Only " + wallets + " is offered, so an account earning in more than one currency"
                        + " can only look at that one");

        System.out.println("The commissions can be narrowed to " + wallets.size() + " wallets: "
                + wallets);

        BaseClass.logger.pass("The commissions can be narrowed to " + wallets.size() + " wallets: "
                + wallets);

        commissions().closeTheDialog();
    }

    @When("I open the first commission")
    public void openTheFirstCommission() {

        commissions().openTheFirstCommission();

        BaseClass.logger.pass("Opened the first commission");
    }

    @Then("its detail should carry {string}")
    public void itsDetailShouldCarry(String expected) {

        assertTrue(commissions().isShowingADialog(),
                "Pressing a commission opened nothing, so there is no way to see it in full. The"
                        + " page is at " + commissions().getCurrentUrl() + refused());

        String said = commissions().whatTheDialogSays();
        List<String> missing = new ArrayList<>();

        for (String detail : named(expected)) {

            if (!said.contains(detail)) {
                missing.add(detail);
            }
        }

        assertTrue(missing.isEmpty(),
                "The commission's detail does not say: " + missing + ". It reads: " + said);

        System.out.println("The detail reads: " + said);

        BaseClass.logger.pass("The commission's detail carries " + named(expected));
    }

    @Then("the detail should offer {string}")
    public void itShouldOffer(String expected) {

        List<String> offered = commissions().whatTheDetailOffers();
        List<String> missing = new ArrayList<>();

        for (String what : named(expected)) {

            if (!offered.contains(what)) {
                missing.add(what);
            }
        }

        assertTrue(missing.isEmpty(),
                "The commission's detail no longer offers: " + missing + ". It offers: " + offered);

        System.out.println("The detail offers " + offered);

        BaseClass.logger.pass("The commission's detail offers " + offered);
    }

    @When("I close the detail")
    public void closeTheDetail() {

        commissions().closeTheDialog();

        BaseClass.logger.pass("Closed the commission's detail");
    }

    @Then("the commissions should be listed again")
    public void theyShouldBeListedAgain() {

        assertFalse(commissions().isShowingADialog(),
                "The detail is still on the screen after being closed" + refused());

        assertTrue(commissions().isShowing(),
                "Closing the detail did not give back the listing. The page is at "
                        + commissions().getCurrentUrl());

        assertEquals(commissions().howManyListed(), listedToBeginWith,
                "The listing showed " + listedToBeginWith + " commissions before one was opened"
                        + " and shows " + commissions().howManyListed() + " after it was closed");

        System.out.println("Back on the listing, all " + listedToBeginWith + " still there");

        BaseClass.logger.pass("Closing the detail gave back the listing, all " + listedToBeginWith
                + " still there");
    }
}
