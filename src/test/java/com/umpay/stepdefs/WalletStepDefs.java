package com.umpay.stepdefs;

import com.umpay.pages.HomePage;
import com.umpay.pages.WalletPage;
import com.umpay.utility.BaseClass;
import com.umpay.utility.ExcelDataProvider;
import com.umpay.utility.Wait;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * The wallets this account holds.
 *
 * <p>Column layout of TestData/Wallet_TestData.xlsx
 * 0 Scenario | 1 LoginID | 2 Password | 3 Wallet | 4 MoveMainTo
 *
 * <p>Nothing here moves money. One scenario moves which wallet the account calls its main one,
 * which is a setting rather than a payment, and puts it back where it found it before it
 * finishes - the platform asks nothing before doing it, so nothing is left half done.
 */
public class WalletStepDefs {

    /** How many times opening the wallets is worth trying, as elsewhere in this suite. */
    private static final int NAVIGATION_ATTEMPTS = 3;

    private WalletPage walletPage;

    private ExcelDataProvider excel;

    /** Which wallet the account called its main one before a scenario moved it. */
    private String mainToBeginWith = "";

    /** Which wallet a scenario moved it to. */
    private String movedTo = "";

    private WalletPage wallet() {

        if (walletPage == null) {
            walletPage = new WalletPage(BaseClass.driver);
        }

        return walletPage;
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

    @When("I open the Wallet page")
    public void openTheWalletPage() throws InterruptedException {

        HomePage home = new HomePage(BaseClass.driver);

        Thread.sleep(3000);

        home.dismissTwoFactorPromptIfShowing();

        wallet().waitUntilSignedIn(30);

        for (int attempt = 1; attempt <= NAVIGATION_ATTEMPTS; attempt++) {

            try {
                wallet().open();
            } catch (Exception notOpened) {
                System.out.println("The wallets did not open (attempt " + attempt + " of "
                        + NAVIGATION_ATTEMPTS + "): " + notOpened.getMessage());
                continue;
            }

            if (wallet().isShowingTheList()) {

                BaseClass.logger.pass("Opened the wallets at " + wallet().getCurrentUrl());
                return;
            }
        }

        org.testng.Assert.fail("The wallets did not open. Landed on " + wallet().getCurrentUrl());
    }

    @Then("the page should list the wallets the account holds")
    public void thePageShouldListTheWallets() {

        List<String> held = wallet().wallets();

        assertFalse(held.isEmpty(),
                "The page lists no wallets at all, on an account that has been depositing,"
                        + " withdrawing and converting all day. The page is at "
                        + wallet().getCurrentUrl());

        List<String> currencies = new ArrayList<>();

        for (String one : held) {
            currencies.add(wallet().currencyOf(one));
        }

        System.out.println("The account holds " + held.size() + " wallets: " + currencies);

        BaseClass.logger.pass("The account holds " + held.size() + " wallets: " + currencies);
    }

    /**
     * Every wallet says what it is, where it belongs, and what is in it.
     *
     * A balance with no currency beside it is a number nobody can act on, and a wallet that does
     * not say what is blocked hides the part of the money the account cannot actually use.
     */
    @Then("every wallet should name its currency, country, balance and what is blocked")
    public void everyWalletShouldNameEverything() {

        List<String> held = wallet().wallets();

        assertFalse(held.isEmpty(), "There are no wallets to read");

        List<String> incomplete = new ArrayList<>();

        for (String one : held) {

            if (wallet().currencyOf(one).isEmpty()
                    || wallet().countryOf(one).isEmpty()
                    || !wallet().carries(one, "Balance:")
                    || !wallet().carries(one, "Blocked Amount:")) {

                incomplete.add(wallet().oneLine(one));
            }
        }

        assertTrue(incomplete.isEmpty(),
                "These wallets are missing part of what they should say: " + incomplete);

        BaseClass.logger.pass("All " + held.size() + " wallets name their currency, country,"
                + " balance and what is blocked");
    }

    /**
     * One main wallet, and only one.
     *
     * The main wallet is the one the rest of the product reaches for first, so two of them is a
     * question nobody can answer, and none at all leaves every other flow guessing.
     */
    @Then("exactly one wallet should be the main wallet")
    public void exactlyOneWalletShouldBeMain() {

        List<String> held = wallet().wallets();

        assertFalse(held.isEmpty(), "There are no wallets to read");

        List<String> main = new ArrayList<>();
        List<String> neither = new ArrayList<>();

        for (String one : held) {

            if (wallet().isMain(one)) {
                main.add(wallet().currencyOf(one));
            } else if (!wallet().offersToBecomeMain(one)) {
                neither.add(wallet().oneLine(one));
            }
        }

        assertEquals(main.size(), 1,
                "The account marks " + main.size() + " wallets as its main one: " + main);

        assertTrue(neither.isEmpty(),
                "These wallets are neither the main one nor offer to become it: " + neither);

        System.out.println("The main wallet is " + main.get(0) + ", and the other "
                + (held.size() - 1) + " each offer to become it");

        BaseClass.logger.pass("The main wallet is " + main.get(0) + ", and the other "
                + (held.size() - 1) + " each offer to become it");
    }

    /**
     * A wallet is written in one money throughout.
     *
     * Held against the card itself rather than a table of symbols kept here: what matters is not
     * which symbol the platform picked but that the balance and the blocked amount on one card
     * are in the same one, since a reader compares those two figures at a glance.
     */
    @Then("each wallet's figures should be written in its own currency")
    public void eachWalletsFiguresShouldMatch() {

        List<String> held = wallet().wallets();

        assertFalse(held.isEmpty(), "There are no wallets to read");

        Map<String, String> symbolFor = new HashMap<>();
        List<String> wrong = new ArrayList<>();

        for (String one : held) {

            String currency = wallet().currencyOf(one);

            String balance = wallet().saidAfter(one, "Balance:");
            String blocked = wallet().saidAfter(one, "Blocked Amount:");

            String balanceSymbol = symbolIn(balance);
            String blockedSymbol = symbolIn(blocked);

            if (balanceSymbol.isEmpty()) {
                wrong.add(currency + " states a balance of \"" + balance + "\", with no symbol");
                continue;
            }

            if (!balanceSymbol.equals(blockedSymbol)) {
                wrong.add(currency + " states its balance in \"" + balanceSymbol
                        + "\" and what is blocked in \"" + blockedSymbol + "\"");
            }

            String seen = symbolFor.put(currency, balanceSymbol);

            if (seen != null && !seen.equals(balanceSymbol)) {
                wrong.add(currency + " is written as both \"" + seen + "\" and \""
                        + balanceSymbol + "\"");
            }
        }

        assertTrue(wrong.isEmpty(), "These wallets are not written in one money: " + wrong);

        System.out.println("The wallets are written as " + symbolFor);

        BaseClass.logger.pass("All " + held.size() + " wallets state their balance and what is"
                + " blocked in the same money: " + symbolFor);
    }

    /** The symbol a figure was written with, which is whatever comes before the number. */
    private String symbolIn(String figure) {

        return figure.replaceAll("[0-9,.\\-].*$", "").trim();
    }

    /**
     * The chart and the wallets tell the same story.
     *
     * The page opens on a chart of where the account's money is, split by country. A country
     * charted that the account holds no wallet for would be money shown to be somewhere it
     * cannot be.
     */
    @Then("every country in the chart should be one the account holds a wallet for")
    public void everyChartedCountryShouldHaveAWallet() {

        List<String> charted = wallet().countriesCharted();

        assertFalse(charted.isEmpty(),
                "The page charts nothing at all. It reads: " + wallet().text());

        List<String> countries = new ArrayList<>();

        for (String one : wallet().wallets()) {
            countries.add(wallet().countryOf(one));
        }

        List<String> unaccounted = new ArrayList<>();

        for (String country : charted) {
            if (!countries.contains(country)) {
                unaccounted.add(country);
            }
        }

        assertTrue(unaccounted.isEmpty(), "The chart names " + unaccounted
                + ", which the account holds no wallet for. Its wallets are in " + countries);

        System.out.println("The chart names " + charted + ", all of them wallets the account holds");

        BaseClass.logger.pass("The chart names " + charted.size() + " countries - " + charted
                + " - and the account holds a wallet for each");
    }

    // ------------------------------------------------------------------
    // One wallet on its own
    // ------------------------------------------------------------------

    @When("I open the wallet named in {string} of {string} of {string}")
    public void openTheWallet(String row, String sheetName, String fileName) {

        String currency = fromTheSheet(row, sheetName, fileName, "Wallet");

        wallet().openWallet(currency);

        BaseClass.logger.pass("Opened the " + currency + " wallet");
    }

    @Then("that wallet's own page should open")
    public void thatWalletsOwnPageShouldOpen() {

        assertTrue(wallet().isShowingAWallet(),
                "Opening a wallet did not lead to a wallet of its own. Landed on "
                        + wallet().getCurrentUrl());

        assertFalse(wallet().walletShown().isEmpty(),
                "The address does not say which wallet is open: " + wallet().getCurrentUrl());

        System.out.println("The " + wallet().walletShown() + " wallet opened at "
                + wallet().getCurrentUrl());

        BaseClass.logger.pass("The " + wallet().walletShown() + " wallet opened at "
                + wallet().getCurrentUrl());
    }

    @Then("the wallet should state its total, what is available and what is locked")
    public void theWalletShouldStateAllThree() {

        List<String> missing = new ArrayList<>();

        for (String heading : new String[]{"Total Balance", "Available Balance", "Locked"}) {

            if (wallet().statedBalance(heading).isEmpty()) {
                missing.add(heading);
            }
        }

        assertTrue(missing.isEmpty(), "The wallet does not state " + missing
                + ". It reads: " + wallet().text());

        System.out.println("The wallet states " + wallet().statedBalance("Total Balance")
                + " in all, " + wallet().statedBalance("Available Balance") + " available and "
                + wallet().statedBalance("Locked") + " locked");

        BaseClass.logger.pass("The wallet states " + wallet().statedBalance("Total Balance")
                + " in all, " + wallet().statedBalance("Available Balance") + " available and "
                + wallet().statedBalance("Locked") + " locked");
    }

    /**
     * The three figures agree with each other.
     *
     * Locked is written as a negative but counts as money the account still has, so the total is
     * the available balance plus what is locked taken as a size. An account holder reads these
     * three numbers to work out what they can spend, and three numbers that do not add up leave
     * them unable to.
     */
    @Then("the wallet's total should be what is available plus what is locked")
    public void theTotalShouldAddUp() {

        BigDecimal total = wallet().figureIn(wallet().statedBalance("Total Balance"));
        BigDecimal available = wallet().figureIn(wallet().statedBalance("Available Balance"));
        BigDecimal locked = wallet().figureIn(wallet().statedBalance("Locked"));

        assertTrue(total != null && available != null && locked != null,
                "The wallet does not state all three figures as numbers. It reads: "
                        + wallet().text());

        BigDecimal expected = available.add(locked.abs());

        assertEquals(total, expected,
                "The wallet says " + total + " in all, but " + available + " available and "
                        + locked + " locked come to " + expected);

        System.out.println(available + " available plus " + locked.abs() + " locked comes to "
                + total);

        BaseClass.logger.pass("The wallet's total of " + total + " is exactly its " + available
                + " available plus the " + locked.abs() + " it has locked");
    }

    @Then("the wallet should offer the tabs {string}")
    public void theWalletShouldOfferTheTabs(String tabs) {

        List<String> offered = wallet().tabsOffered();
        List<String> missing = new ArrayList<>();

        for (String tab : tabs.split(",")) {
            if (!offered.contains(tab.trim())) {
                missing.add(tab.trim());
            }
        }

        assertTrue(missing.isEmpty(), "The wallet does not offer " + missing
                + ". It offers: " + offered);

        BaseClass.logger.pass("The wallet offers " + offered);
    }

    @When("I move to the wallet's {string} tab")
    public void moveToTheWalletTab(String tab) {

        wallet().chooseTab(tab);

        BaseClass.logger.pass("Moved to the " + tab + " tab");
    }

    /**
     * What has moved in this wallet, and what it says about each movement.
     *
     * A movement with no date cannot be placed against a statement, and one with no amount says
     * nothing at all.
     */
    @Then("every movement should name what it was, when it happened and how much")
    public void everyMovementShouldNameEverything() {

        List<String> listed = wallet().movements();

        assertFalse(listed.isEmpty(),
                "The tab lists no movements at all. The page reads: " + wallet().text());

        List<String> incomplete = new ArrayList<>();

        for (String movement : listed) {

            if (wallet().kindOf(movement).isEmpty()
                    || wallet().dateOf(movement).isEmpty()
                    || wallet().amountOf(movement).isEmpty()) {

                incomplete.add(movement);
            }
        }

        assertTrue(incomplete.isEmpty(),
                "These movements are missing part of what they should say: " + incomplete);

        System.out.println("The tab lists " + listed.size() + " movements. The newest: "
                + listed.get(0));

        BaseClass.logger.pass("All " + listed.size() + " movements name what they were, when they"
                + " happened and how much. The newest: \"" + listed.get(0) + "\"");
    }

    @Then("the wallet should offer to become the main wallet")
    public void theWalletShouldOfferToBecomeMain() {

        assertTrue(wallet().offersToSetAsMain(),
                "The wallet offers no way to make it the main one. It reads: " + wallet().text());

        BaseClass.logger.pass("The wallet offers to become the main wallet");
    }

    // ------------------------------------------------------------------
    // Moving the main wallet
    // ------------------------------------------------------------------

    /**
     * Moves which wallet the account calls its main one.
     *
     * A setting rather than a payment, and the platform asks nothing before doing it. Where it
     * was is remembered first so the scenario can put it back, because every other flow in this
     * product reaches for the main wallet and would otherwise be left pointing somewhere this
     * run chose.
     */
    @When("I make the wallet named in {string} of {string} of {string} the main one")
    public void makeAnotherWalletMain(String row, String sheetName, String fileName) {

        mainToBeginWith = wallet().mainWallet();

        assertFalse(mainToBeginWith.isEmpty(), "No wallet is marked as the main one to begin with");

        movedTo = fromTheSheet(row, sheetName, fileName, "MoveMainTo");

        assertFalse(movedTo.equals(mainToBeginWith),
                "The sheet asks to move the main wallet to " + movedTo + ", which it already is");

        wallet().setAsMain(movedTo);

        BaseClass.logger.pass("Moved the main wallet from " + mainToBeginWith + " to " + movedTo);
    }

    @Then("that wallet should become the main one")
    public void thatWalletShouldBecomeMain() {

        assertTrue(Wait.until(() -> movedTo.equals(wallet().mainWallet()), 20),
                "The main wallet was set to " + movedTo + " but the page calls "
                        + wallet().mainWallet() + " the main one");

        System.out.println("The main wallet moved from " + mainToBeginWith + " to "
                + wallet().mainWallet());

        BaseClass.logger.pass("The main wallet moved from " + mainToBeginWith + " to " + movedTo);
    }

    @When("I put the main wallet back")
    public void putTheMainWalletBack() {

        wallet().setAsMain(mainToBeginWith);

        BaseClass.logger.pass("Put the main wallet back to " + mainToBeginWith);
    }

    @Then("the wallet that was main should be main again")
    public void theOriginalShouldBeMainAgain() {

        assertTrue(Wait.until(() -> mainToBeginWith.equals(wallet().mainWallet()), 20),
                "The main wallet was put back to " + mainToBeginWith + " but the page calls "
                        + wallet().mainWallet() + " the main one. The account has been left"
                        + " pointing somewhere this run chose");

        BaseClass.logger.pass("The main wallet is " + mainToBeginWith + " again, as it was found");
    }

    @Then("the wallets should be listed again")
    public void theWalletsShouldBeListedAgain() {

        assertTrue(Wait.until(() -> wallet().isShowingTheList(), 20),
                "Going back did not return to the wallets. Landed on " + wallet().getCurrentUrl());

        assertTrue(Wait.until(() -> !wallet().wallets().isEmpty(), 20),
                "The wallets came back empty");

        BaseClass.logger.pass("The wallets are listed again, all " + wallet().wallets().size()
                + " of them");
    }

    @When("I go back to the wallets")
    public void goBackToTheWallets() {

        wallet().goBack();

        BaseClass.logger.pass("Went back to the wallets");
    }

    // ------------------------------------------------------------------
    // The home page and the wallet page, held against each other
    // ------------------------------------------------------------------

    /** What the home page said each wallet holds, before the run moved off it. */
    private Map<String, String> homeSaid = new HashMap<>();

    /**
     * Whether two figures are the same amount of money.
     *
     * Compared as numbers rather than as text. The two screens write the same amount differently
     * - the home page draws the Mexican wallet as 289.50 and the wallet page as 289.5 - and a
     * comparison of the characters would report that as the screens disagreeing about somebody's
     * money, which it plainly is not.
     */
    private boolean worthTheSame(String one, String other) {

        try {
            return new BigDecimal(one).compareTo(new BigDecimal(other)) == 0;
        } catch (NumberFormatException notNumbers) {
            return String.valueOf(one).equals(other);
        }
    }

    /** Only what a figure is worth: symbols, spaces and separators dropped. */
    private String justTheFigure(String said) {

        return String.valueOf(said).replace(",", "").replaceAll("[^0-9.-]", "");
    }

    /**
     * Reads what the home page says every wallet holds.
     *
     * The amounts arrive hidden and only three wallets are drawn, so both have to be dealt with
     * before there is anything to read.
     */
    @When("I note what the home page says each wallet holds")
    public void iNoteWhatTheHomePageSays() {

        HomePage home = new HomePage(BaseClass.driver);

        home.dismissTwoFactorPromptIfShowing();

        Wait.sleep(2500);

        home.revealBalances();

        Wait.sleep(2000);

        if (home.offersShowMore()) {
            home.showMoreWallets();
        }

        homeSaid.clear();

        for (String card : home.getWalletRows()) {

            String currency = card.split("[\s|]+")[0];

            int at = card.indexOf("Balance:");

            if (at < 0) {
                continue;
            }

            String rest = card.substring(at + "Balance:".length()).trim();
            int ends = rest.indexOf("Blocked");

            homeSaid.put(currency, justTheFigure(ends < 0 ? rest : rest.substring(0, ends)));
        }

        assertFalse(homeSaid.isEmpty(), "The home page said nothing about any wallet");

        System.out.println("The home page says: " + homeSaid);

        BaseClass.logger.pass("Noted what the home page says " + homeSaid.size()
                + " wallets hold");
    }

    /**
     * The two screens have to agree about the same money.
     *
     * They are drawn from the same figures and nothing had ever held one against the other. A
     * wallet reading one thing on the screen somebody lands on and another on the screen they go
     * to is worse than either being wrong on its own, because there is no way to tell which to
     * believe.
     */
    @Then("the wallet page should agree with the home page about what each wallet holds")
    public void theTwoPagesShouldAgree() {

        assertFalse(homeSaid.isEmpty(), "Nothing was noted from the home page to compare against");

        List<String> disagreeing = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        Map<String, String> walletSaid = new HashMap<>();

        for (String wallet : wallet().wallets()) {

            String currency = wallet().currencyOf(wallet);

            if (currency == null || currency.isEmpty()) {
                continue;
            }

            walletSaid.put(currency, justTheFigure(wallet().saidAfter(wallet, "Balance:")));
        }

        for (Map.Entry<String, String> onHome : homeSaid.entrySet()) {

            String here = walletSaid.get(onHome.getKey());

            if (here == null) {
                missing.add(onHome.getKey());
            } else if (!worthTheSame(onHome.getValue(), here)) {
                disagreeing.add(onHome.getKey() + ": the home page says " + onHome.getValue()
                        + " and the wallet page says " + here);
            }
        }

        assertTrue(missing.isEmpty(),
                "The home page lists wallets the wallet page does not: " + missing
                        + ". The wallet page lists " + walletSaid.keySet());

        assertTrue(disagreeing.isEmpty(),
                "The two screens disagree about the same money, so there is no way to tell which"
                        + " to believe: " + disagreeing);

        System.out.println("Both screens agree about all " + homeSaid.size() + " wallets");

        BaseClass.logger.pass("The home page and the wallet page agree about all "
                + homeSaid.size() + " wallets");
    }
}
