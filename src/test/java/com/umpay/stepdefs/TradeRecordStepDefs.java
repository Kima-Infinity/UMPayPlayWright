package com.umpay.stepdefs;

import com.umpay.pages.HomePage;
import com.umpay.pages.TradeRecordPage;
import com.umpay.utility.BaseClass;
import com.umpay.utility.ExcelDataProvider;
import com.umpay.utility.Wait;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * The trade record - every order this account has raised.
 *
 * <p>Column layout of TestData/TradeRecord_TestData.xlsx
 * 0 Scenario | 1 LoginID | 2 Password | 3 Tab
 *
 * <p>Nothing here raises an order or pays one. The one scenario that opens an order stops at the
 * order's own page and goes back, without touching what is on it - an unpaid deposit opens the
 * page where it would be paid, and paying it is not this file's business.
 */
public class TradeRecordStepDefs {

    /** How many times opening the trade record is worth trying, as elsewhere in this suite. */
    private static final int NAVIGATION_ATTEMPTS = 3;

    private TradeRecordPage tradeRecordPage;

    private ExcelDataProvider excel;

    /** What was listed before a tab was chosen or the list was scrolled. */
    private List<String> listedBefore = new ArrayList<>();

    /** Where the run was before it opened an order, so the way back can be judged. */
    private String startedAt = "";

    private TradeRecordPage tradeRecord() {

        if (tradeRecordPage == null) {
            tradeRecordPage = new TradeRecordPage(BaseClass.driver);
        }

        return tradeRecordPage;
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

    @When("I open the Trade Record page")
    public void openTheTradeRecordPage() throws InterruptedException {

        HomePage home = new HomePage(BaseClass.driver);

        Thread.sleep(3000);

        home.dismissTwoFactorPromptIfShowing();

        tradeRecord().waitUntilSignedIn(30);

        for (int attempt = 1; attempt <= NAVIGATION_ATTEMPTS; attempt++) {

            try {
                tradeRecord().open();
            } catch (Exception notOpened) {
                System.out.println("The trade record did not open (attempt " + attempt + " of "
                        + NAVIGATION_ATTEMPTS + "): " + notOpened.getMessage());
                continue;
            }

            if (tradeRecord().isShowing()) {

                BaseClass.logger.pass("Opened the trade record at "
                        + tradeRecord().getCurrentUrl());
                return;
            }
        }

        org.testng.Assert.fail("The trade record did not open. Landed on "
                + tradeRecord().getCurrentUrl());
    }

    @Then("the trade record should list the orders the account has raised")
    public void theTradeRecordShouldListOrders() {

        List<String> listed = tradeRecord().entries();

        assertFalse(listed.isEmpty(),
                "The trade record lists no orders at all, on an account that has been depositing,"
                        + " withdrawing and transferring all day. The page is at "
                        + tradeRecord().getCurrentUrl());

        System.out.println("The trade record lists " + listed.size() + ". The newest: "
                + tradeRecord().oneLine(listed.get(0)));

        BaseClass.logger.pass("The trade record lists " + listed.size() + " orders, the newest"
                + " being \"" + tradeRecord().oneLine(listed.get(0)) + "\"");
    }

    @Then("the trade record should offer the tabs {string}")
    public void theTradeRecordShouldOfferTheTabs(String tabs) {

        List<String> offered = tradeRecord().tabsOffered();
        List<String> missing = new ArrayList<>();

        for (String tab : tabs.split(",")) {
            if (!offered.contains(tab.trim())) {
                missing.add(tab.trim());
            }
        }

        assertTrue(missing.isEmpty(), "The trade record does not offer " + missing
                + ". It offers: " + offered);

        System.out.println("The trade record offers " + offered);

        BaseClass.logger.pass("The trade record offers " + offered.size() + " tabs: " + offered);
    }

    /**
     * Every order says what it was, how it ended, and what it was for.
     *
     * An order with no number cannot be looked up by anybody - not by the account holder, not by
     * customer service - and an order with no status is one nobody can act on.
     */
    @Then("every order should name its number, currency, amount and how it ended")
    public void everyOrderShouldNameEverything() {

        List<String> listed = tradeRecord().entries();

        assertFalse(listed.isEmpty(), "There are no orders to read");

        List<String> incomplete = new ArrayList<>();

        for (String entry : listed) {

            boolean numbered = tradeRecord().carries(entry, "Number:")
                    || tradeRecord().carries(entry, "Order Number:");

            boolean priced = tradeRecord().carries(entry, "Amount:")
                    || tradeRecord().carries(entry, "Total Amount:");

            if (!numbered || !priced
                    || !tradeRecord().carries(entry, "Currency:")
                    || tradeRecord().kindOf(entry).isEmpty()
                    || tradeRecord().statusOf(entry).isEmpty()) {

                incomplete.add(tradeRecord().oneLine(entry));
            }
        }

        assertTrue(incomplete.isEmpty(),
                "These orders are missing part of what they should say: " + incomplete);

        BaseClass.logger.pass("All " + listed.size() + " orders name their number, currency,"
                + " amount and how they ended");
    }

    /**
     * No two orders share a number.
     *
     * The number is what identifies an order to everybody who has to talk about it. Two orders
     * carrying the same one is the worst kind of listing bug, because both look right.
     */
    @Then("no two orders should share a number")
    public void noTwoOrdersShouldShareANumber() {

        List<String> listed = tradeRecord().entries();

        assertFalse(listed.isEmpty(), "There are no orders to check");

        Set<String> seen = new HashSet<>();
        List<String> repeated = new ArrayList<>();

        for (String entry : listed) {

            String number = tradeRecord().carries(entry, "Order Number:")
                    ? tradeRecord().fieldOf(entry, "Order Number:")
                    : tradeRecord().fieldOf(entry, "Number:");

            if (!seen.add(number)) {
                repeated.add(number);
            }
        }

        assertTrue(repeated.isEmpty(), "These order numbers appear more than once: " + repeated);

        BaseClass.logger.pass("All " + listed.size() + " orders carry a number of their own");
    }

    /**
     * An order that was converted shows both halves of the conversion.
     *
     * A received amount with no rate cannot be checked by the reader, and a rate with nothing
     * received is a rate applied to nothing. Either alone is worse than neither.
     */
    @Then("any converted order should show both what was received and the rate")
    public void convertedOrdersShouldShowBoth() {

        List<String> listed = tradeRecord().entries();

        assertFalse(listed.isEmpty(), "There are no orders to check");

        List<String> half = new ArrayList<>();
        int converted = 0;

        for (String entry : listed) {

            boolean received = tradeRecord().carries(entry, "Received Amount:");
            boolean rate = tradeRecord().carries(entry, "Exchange rate:");

            if (received && rate) {
                converted++;
            } else if (received || rate) {
                half.add(tradeRecord().oneLine(entry));
            }
        }

        assertTrue(half.isEmpty(),
                "These orders show one half of a conversion without the other: " + half);

        System.out.println(converted + " of the " + listed.size() + " orders were converted");

        BaseClass.logger.pass(converted + " of the " + listed.size() + " orders carry a"
                + " conversion, and every one shows both what was received and the rate");
    }

    @When("I scroll to the end of the trade record")
    public void scrollToTheEnd() {

        listedBefore = tradeRecord().entries();

        tradeRecord().scrollThrough(2);

        BaseClass.logger.pass("Scrolled to the end of the " + listedBefore.size()
                + " orders that had arrived");
    }

    @Then("more orders should have been brought in")
    public void moreOrdersShouldHaveBeenBroughtIn() {

        assertTrue(Wait.until(() -> tradeRecord().entries().size() > listedBefore.size(), 20),
                "Reaching the end of the list brought nothing more in. It still lists "
                        + tradeRecord().entries().size() + " orders");

        System.out.println("Reaching the end took the list from " + listedBefore.size() + " to "
                + tradeRecord().entries().size());

        BaseClass.logger.pass("Reaching the end took the list from " + listedBefore.size()
                + " orders to " + tradeRecord().entries().size());
    }

    // ------------------------------------------------------------------
    // The other tabs
    // ------------------------------------------------------------------

    @When("I move to the trade record tab named in {string} of {string} of {string}")
    public void moveToTheTab(String row, String sheetName, String fileName) {

        listedBefore = tradeRecord().entries();

        String tab = fromTheSheet(row, sheetName, fileName, "Tab");

        tradeRecord().chooseTab(tab);

        BaseClass.logger.pass("Moved to the " + tab + " tab");
    }

    /**
     * An international transfer adds up.
     *
     * The card shows what was taken altogether, what the platform kept and what was sent, so the
     * three can be held against each other: a total that is not the fee plus the amount means
     * somebody is being charged something the card does not name.
     */
    @Then("every international transfer should add up to its total")
    public void everyTransferShouldAddUp() {

        List<String> listed = tradeRecord().entries();

        assertFalse(listed.isEmpty(), "The International Transfer tab lists no orders at all");

        List<String> wrong = new ArrayList<>();
        List<String> unchecked = new ArrayList<>();
        int checked = 0;

        for (String entry : listed) {

            BigDecimal total = tradeRecord().figureIn(tradeRecord().fieldOf(entry, "Total Amount:"));
            BigDecimal fee = tradeRecord().figureIn(tradeRecord().fieldOf(entry, "Fee:"));

            if (total == null || fee == null) {
                wrong.add("does not name its total and its fee: " + tradeRecord().oneLine(entry));
                continue;
            }

            /*
             * What was sent, whichever way this card names it.
             *
             * The platform has changed the shape of this card. Orders raised in May carry a
             * total, a fee and an approximate received all in the one currency; newer ones carry
             * an amount as well, and state what will arrive in the currency it arrives in. So
             * the figure to add to the fee is the amount where the card names one, and otherwise
             * what was received - but only while that is in the same money as the total, since
             * 68.96 CNY and 4.75 USD do not add up to anything at all.
             */
            String money = tradeRecord().fieldOf(entry, "Currency:").trim();

            String sent = tradeRecord().fieldOf(entry, "Amount:");

            if (sent.isEmpty()) {

                String received = tradeRecord().fieldOf(entry, "Approximate Received:");

                if (tradeRecord().currencyIn(received).equals(money)) {
                    sent = received;
                }
            }

            BigDecimal amount = tradeRecord().figureIn(sent);

            if (amount == null) {
                unchecked.add(tradeRecord().oneLine(entry));
                continue;
            }

            checked++;

            if (total.compareTo(fee.add(amount)) != 0) {
                wrong.add(total + " is charged for a fee of " + fee + " and an amount of " + amount
                        + ": " + tradeRecord().oneLine(entry));
            }
        }

        assertTrue(wrong.isEmpty(), "These international transfers do not add up: " + wrong);

        assertFalse(checked == 0, "Not one international transfer named enough to be added up."
                + " They read: " + unchecked);

        System.out.println("All " + checked + " international transfers add up to their total"
                + (unchecked.isEmpty() ? "" : ", and " + unchecked.size()
                + " name no figure in the currency they were charged in"));

        BaseClass.logger.pass("All " + checked + " international transfers are charged exactly"
                + " their fee plus their amount");
    }

    @Then("every school fee order should name its order number, total and when it was created")
    public void everySchoolFeeShouldNameEverything() {

        List<String> listed = tradeRecord().entries();

        assertFalse(listed.isEmpty(), "The International School Fee tab lists no orders at all");

        List<String> incomplete = new ArrayList<>();

        for (String entry : listed) {

            if (!tradeRecord().carries(entry, "Order Number:")
                    || !tradeRecord().carries(entry, "Total Amount:")
                    || !tradeRecord().carries(entry, "Created Date:")) {

                incomplete.add(tradeRecord().oneLine(entry));
            }
        }

        assertTrue(incomplete.isEmpty(),
                "These school fee orders are missing part of what they should say: " + incomplete);

        BaseClass.logger.pass("All " + listed.size() + " school fee orders name their order"
                + " number, total and when they were created");
    }

    @Then("what the trade record lists should change")
    public void whatIsListedShouldChange() {

        List<String> now = tradeRecord().entries();

        assertFalse(listedBefore.isEmpty(), "Nothing was listed before the tab was chosen");

        assertFalse(listedBefore.equals(now),
                "Choosing another tab left exactly the same " + now.size() + " orders on screen");

        System.out.println("The list went from " + listedBefore.size() + " orders to " + now.size());

        BaseClass.logger.pass("Choosing another tab changed the list, from " + listedBefore.size()
                + " orders to " + now.size());
    }

    // ------------------------------------------------------------------
    // Opening one
    // ------------------------------------------------------------------

    @When("I open the newest order")
    public void openTheNewestOrder() {

        listedBefore = tradeRecord().entries();

        assertFalse(listedBefore.isEmpty(), "There are no orders to open");

        startedAt = tradeRecord().getCurrentUrl();

        tradeRecord().openEntry(0);

        BaseClass.logger.pass("Opened \"" + tradeRecord().oneLine(listedBefore.get(0)) + "\"");
    }

    /**
     * An order in the list leads to the order itself.
     *
     * The whole use of a record is being able to act on what is in it. The address it lands on
     * names the order it came from and where it came from, which is what lets the platform bring
     * a reader back where they were.
     */
    @Then("the order's own page should open")
    public void theOrdersOwnPageShouldOpen() {

        String landedOn = tradeRecord().getCurrentUrl();

        assertFalse(landedOn.equals(startedAt),
                "Opening an order went nowhere. Still at " + landedOn);

        assertTrue(landedOn.contains("orderId="),
                "Opening an order did not lead to an order of its own. Landed on " + landedOn);

        System.out.println("The order opened at " + landedOn);

        BaseClass.logger.pass("The order opened at " + landedOn);
    }

    @When("I go back to the trade record")
    public void goBackToTheTradeRecord() {

        tradeRecord().goBack();

        BaseClass.logger.pass("Went back to the trade record");
    }

    @Then("the trade record should be listing again")
    public void theTradeRecordShouldBeListingAgain() {

        assertTrue(Wait.until(() -> tradeRecord().isShowing(), 20),
                "Going back did not return to the trade record. Landed on "
                        + tradeRecord().getCurrentUrl());

        assertTrue(Wait.until(() -> !tradeRecord().entries().isEmpty(), 20),
                "The trade record came back empty");

        BaseClass.logger.pass("The trade record is listing again, with "
                + tradeRecord().entries().size() + " orders");
    }

    @Then("the order should offer to be kept")
    public void theOrderShouldOfferToBeKept() {

        org.testng.Assert.assertTrue(tradeRecord().orderOffersToBeKept(),
                "The order offers no way to keep a copy of itself, so somebody asked where their"
                        + " money went has nothing to show. The page reads: " + tradeRecord().text());

        BaseClass.logger.pass("The order offers to be downloaded and shared");
    }

    @Then("downloading the order should produce a file")
    public void downloadingTheOrderShouldProduceAFile() {

        java.nio.file.Path saved;

        try {
            saved = tradeRecord().downloadOrder(java.nio.file.Paths.get("Downloads"));
        } catch (Exception nothingCameBack) {
            throw new AssertionError("Download produced no file at all: "
                    + nothingCameBack.getMessage());
        }

        org.testng.Assert.assertTrue(java.nio.file.Files.exists(saved),
                "Download named a file that is not there: " + saved);

        long size = saved.toFile().length();

        org.testng.Assert.assertTrue(size > 0,
                "The order downloaded an empty file, which is worse than none: " + saved);

        System.out.println("The order downloaded as " + saved.getFileName() + " (" + size
                + " bytes)");

        BaseClass.logger.pass("The order downloaded as " + saved.getFileName());
    }

    @When("I share the order")
    public void iShareTheOrder() {

        tradeRecord().shareOrder();

        BaseClass.logger.pass("Asked to share the order");
    }

    @Then("the sharing choices should offer a way to copy the order")
    public void sharingShouldOfferACopy() {

        org.testng.Assert.assertTrue(tradeRecord().shareOffersACopy(),
                "Sharing the order offers no way to copy it. The page reads: " + tradeRecord().text());

        BaseClass.logger.pass("Sharing the order offers a way to copy it");
    }
}
