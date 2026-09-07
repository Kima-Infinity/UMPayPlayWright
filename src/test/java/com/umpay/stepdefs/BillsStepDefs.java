package com.umpay.stepdefs;

import com.umpay.pages.BillsPage;
import com.umpay.pages.HomePage;
import com.umpay.utility.BaseClass;
import com.umpay.utility.ExcelDataProvider;
import com.umpay.utility.Wait;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * The Bills page - the account's ledger.
 *
 * <p>Column layout of TestData/Bills_TestData.xlsx
 * 0 Scenario | 1 LoginID | 2 Password | 3 Type | 4 FromAmount | 5 ToAmount | 6 FromDate | 7 ToDate
 */
public class BillsStepDefs {

    /** How many times the sidebar click is worth trying, as elsewhere in this suite. */
    private static final int NAVIGATION_ATTEMPTS = 3;

    private BillsPage billsPage;

    private ExcelDataProvider excel;

    private BillsPage bills() {

        if (billsPage == null) {
            billsPage = new BillsPage(BaseClass.driver);
        }

        return billsPage;
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

    @When("I open the Bills page")
    public void openTheBillsPage() throws InterruptedException {

        HomePage home = new HomePage(BaseClass.driver);

        Thread.sleep(3000);

        for (int attempt = 1; attempt <= NAVIGATION_ATTEMPTS; attempt++) {

            home.dismissTwoFactorPromptIfShowing();
            home.openBills();

            if (Wait.until(() -> bills().isShowing(), 15)) {

                // The page arrives before the ledger does, so what is waited for is the ledger.
                bills().waitUntilLoaded(20);

                BaseClass.logger.pass("Opened the Bills page at " + bills().getCurrentUrl());
                return;
            }

            System.out.println("The Bills page was not ready (attempt " + attempt + " of "
                    + NAVIGATION_ATTEMPTS + "), at " + bills().getCurrentUrl());
        }

        org.testng.Assert.fail("The Bills page did not open. Landed on " + bills().getCurrentUrl());
    }

    /**
     * The ledger lists the account's movements, and each of them reads as one.
     *
     * A page that opens is not the same as a page that shows anything: what is asserted is that
     * entries are listed and that each names its kind, when it happened and how much moved.
     */
    @Then("the ledger should list the account's transactions")
    public void theLedgerShouldListTransactions() {

        List<String> listed = bills().entries();

        assertFalse(listed.isEmpty(),
                "The Bills page lists no transactions at all, on an account that has been"
                        + " depositing, withdrawing and converting all day");

        System.out.println("The ledger lists " + listed.size() + " transactions. The newest: "
                + listed.get(0));

        BaseClass.logger.pass("The ledger lists " + listed.size()
                + " transactions, each naming its kind, when it happened and how much moved");
    }

    @Then("the ledger should offer to filter by {string}")
    public void theLedgerShouldOfferToFilterBy(String kinds) {

        List<String> offered = bills().typesOffered();
        List<String> missing = new ArrayList<>();

        for (String kind : kinds.split(",")) {
            if (!offered.contains(kind.trim())) {
                missing.add(kind.trim());
            }
        }

        assertTrue(missing.isEmpty(), "The ledger does not offer " + missing
                + ". It offers: " + offered);

        System.out.println("The ledger filters by " + offered);

        BaseClass.logger.pass("The ledger offers " + offered.size() + " kinds: " + offered);
    }

    @When("I filter the ledger by the type in {string} of {string} of {string}")
    public void filterByType(String row, String sheetName, String fileName) {

        String type = fromTheSheet(row, sheetName, fileName, "Type");

        bills().filterByType(type);
        bills().confirm();

        BaseClass.logger.pass("Filtered the ledger by " + type);
    }

    @Then("every transaction listed should be a {string}")
    public void everyTransactionShouldBe(String type) {

        List<String> listed = bills().entries();

        assertFalse(listed.isEmpty(),
                "Filtering by " + type + " left nothing at all, so the filter cannot be judged");

        List<String> wrong = new ArrayList<>();

        for (String entry : listed) {
            if (!bills().typeOf(entry).equalsIgnoreCase(type)) {
                wrong.add(entry);
            }
        }

        assertTrue(wrong.isEmpty(), "Filtering by " + type + " also listed: " + wrong);

        BaseClass.logger.pass("All " + listed.size() + " transactions listed are " + type);
    }

    @When("I filter the ledger by the amounts in {string} of {string} of {string}")
    public void filterByAmount(String row, String sheetName, String fileName) {

        bills().filterByAmount(fromTheSheet(row, sheetName, fileName, "FromAmount"),
                fromTheSheet(row, sheetName, fileName, "ToAmount"));

        bills().confirm();

        BaseClass.logger.pass("Filtered the ledger to amounts between "
                + fromTheSheet(row, sheetName, fileName, "FromAmount") + " and "
                + fromTheSheet(row, sheetName, fileName, "ToAmount"));
    }

    /**
     * Nothing outside the range asked for.
     *
     * The figures are read without their currency symbols, which is how the filter treats them:
     * the page filters on the number rather than on what it is worth, so a hundred pesos and a
     * hundred dollars are both inside a band that ends at a hundred.
     */
    @Then("every amount listed should be between {string} and {string}")
    public void everyAmountShouldBeBetween(String from, String to) {

        List<String> listed = bills().entries();

        assertFalse(listed.isEmpty(),
                "Filtering to between " + from + " and " + to + " left nothing to judge");

        BigDecimal least = new BigDecimal(from);
        BigDecimal most = new BigDecimal(to);

        List<String> outside = new ArrayList<>();

        for (String entry : listed) {

            BigDecimal amount = bills().amountOf(entry);

            if (amount.compareTo(least) < 0 || amount.compareTo(most) > 0) {
                outside.add(entry);
            }
        }

        assertTrue(outside.isEmpty(), "These are outside " + from + " to " + to + ": " + outside);

        BaseClass.logger.pass("All " + listed.size() + " amounts are between " + from + " and " + to);
    }

    /**
     * Narrows the ledger to the day its newest transaction happened.
     *
     * Worked out from the list rather than written in the sheet, because a date in a sheet stops
     * matching anything the day after it is written.
     */
    @When("I filter the ledger to the day of its newest transaction")
    public void filterToTheDayOfTheNewestTransaction() {

        List<String> listed = bills().entries();

        assertFalse(listed.isEmpty(), "The ledger lists nothing to take a date from");

        String day = bills().dateOf(listed.get(0)).split(" ")[0];

        bills().filterByDate(day, day);
        bills().confirm();

        System.setProperty("bills.day.filtered", day);

        BaseClass.logger.pass("Filtered the ledger to " + day);
    }

    @Then("every transaction listed should be from that day")
    public void everyTransactionShouldBeFromThatDay() {

        String day = System.getProperty("bills.day.filtered", "");

        List<String> listed = bills().entries();

        assertFalse(listed.isEmpty(), "Filtering to " + day + " left nothing at all");

        List<String> wrong = new ArrayList<>();

        for (String entry : listed) {
            if (!bills().dateOf(entry).startsWith(day)) {
                wrong.add(entry);
            }
        }

        assertTrue(wrong.isEmpty(), "Filtering to " + day + " also listed: " + wrong);

        BaseClass.logger.pass("All " + listed.size() + " transactions listed are from " + day);
    }

    @When("I filter the ledger by the dates in {string} of {string} of {string}")
    public void filterByDate(String row, String sheetName, String fileName) {

        bills().filterByDate(fromTheSheet(row, sheetName, fileName, "FromDate"),
                fromTheSheet(row, sheetName, fileName, "ToDate"));

        bills().confirm();

        BaseClass.logger.pass("Filtered the ledger to between "
                + fromTheSheet(row, sheetName, fileName, "FromDate") + " and "
                + fromTheSheet(row, sheetName, fileName, "ToDate"));
    }

    @Then("the ledger should say it has nothing to show")
    public void theLedgerShouldSayNothingToShow() {

        assertTrue(bills().saysNoData(),
                "The ledger did not say it had nothing to show. It lists: " + bills().entries());

        BaseClass.logger.pass("The ledger says it has nothing to show");
    }

    @When("I reset the filters")
    public void resetTheFilters() {

        bills().reset();

        BaseClass.logger.pass("Reset the filters");
    }

    @Then("the ledger should list the account's transactions again")
    public void theLedgerShouldListAgain() {

        Wait.until(() -> !bills().entries().isEmpty(), 15);

        assertFalse(bills().entries().isEmpty(),
                "Reset left the ledger empty, so the filters are still on");

        BaseClass.logger.pass("Reset brought back " + bills().entries().size() + " transactions");
    }

    @When("I open the newest transaction")
    public void openTheNewestTransaction() {

        bills().openEntry(0);

        assertTrue(bills().isShowingADetail(), "The transaction did not open");

        BaseClass.logger.pass("Opened the newest transaction");
    }

    /**
     * The detail names the transaction rather than merely appearing.
     *
     * Its number is the part that makes it this transaction's detail and no other, so that is
     * asserted alongside the date and the amounts.
     */
    @Then("the transaction detail should name the transaction, when it happened and what moved")
    public void theDetailShouldNameEverything() {

        for (String part : new String[]{"Number:", "Date:"}) {

            assertTrue(bills().detailSays(part),
                    "The detail does not say " + part + ". It reads: " + bills().detailText());
        }

        assertTrue(bills().detailText().matches(".*[0-9].*"),
                "The detail names no figures at all: " + bills().detailText());

        System.out.println("The transaction detail reads: " + bills().detailText());

        BaseClass.logger.pass("The detail reads: " + bills().detailText());
    }

    @Then("downloading the transaction should produce a file")
    public void downloadingShouldProduceAFile() throws java.io.IOException {

        java.nio.file.Path folder = java.nio.file.Paths.get("target", "downloads");

        java.nio.file.Files.createDirectories(folder);

        java.nio.file.Path file = bills().downloadDetail(folder);

        assertTrue(java.nio.file.Files.exists(file), "Download produced no file at " + file);

        long size = java.nio.file.Files.size(file);

        assertTrue(size > 1000,
                "The downloaded receipt is only " + size + " bytes, which is not a receipt");

        System.out.println("The transaction downloaded as " + file.getFileName()
                + " (" + size + " bytes)");

        BaseClass.logger.pass("Downloaded " + file.getFileName() + ", " + size + " bytes");
    }

    @When("I share the transaction")
    public void shareTheTransaction() {

        bills().shareDetail();

        BaseClass.logger.pass("Opened the sharing choices");
    }

    @Then("the sharing choices should offer a way to copy the transaction")
    public void sharingShouldOfferACopy() {

        assertTrue(bills().shareOffersACopy(),
                "Share opened nothing that can be copied. It shows: " + bills().detailText());

        BaseClass.logger.pass("Share offers a way to copy the transaction");
    }
}
