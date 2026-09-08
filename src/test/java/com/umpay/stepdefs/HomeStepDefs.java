package com.umpay.stepdefs;

import com.umpay.pages.HomePage;
import com.umpay.utility.BaseClass;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * The screen every account lands on.
 *
 * <p>Column layout of TestData/Home_TestData.xlsx
 * 0 Scenario | 1 LoginID | 2 Password
 *
 * <p>What the account holds is not kept in the sheet. The balances move every time the suite
 * sends anything, and the withdraw and transfer cases send a great deal, so a figure written down
 * here would be wrong by the end of the next full run. What is held to is the shape of the page.
 */
public class HomeStepDefs {

    private HomePage homePage;

    /** How many wallets were listed before Show More was pressed. */
    private int walletsBefore = -1;

    private HomePage home() {

        if (homePage == null) {
            homePage = new HomePage(BaseClass.driver);
        }

        return homePage;
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

    /** True while what the card shows is written with a minus in front of it. */
    private boolean readsAsNegative(String amount) {

        return amount != null && amount.contains("-");
    }

    @When("I am on the home page")
    public void iAmOnTheHomePage() throws InterruptedException {

        Thread.sleep(3000);

        home().dismissTwoFactorPromptIfShowing();

        Thread.sleep(2000);

        assertTrue(home().getCurrentUrl().contains("umpay"),
                "Signing in did not land anywhere in the application. The page is at "
                        + home().getCurrentUrl());

        BaseClass.logger.pass("On the home page at " + home().getCurrentUrl());
    }

    @Then("the home page should show the account's total and its wallets")
    public void theHomePageShouldShowTheTotalAndWallets() {

        assertFalse(home().getTotalLabel().isEmpty(),
                "The home page shows no total at all, so nothing says what the account comes to."
                        + " It reads: " + home().whatTheHomePageSays());

        assertFalse(home().getBrandName().isEmpty(), "The sidebar carries no product name");

        assertTrue(home().walletCount() > 0,
                "The home page lists no wallets, so an account that holds money cannot see any of"
                        + " it. It reads: " + home().whatTheHomePageSays());

        System.out.println("The home page shows \"" + home().getTotalLabel() + "\" and "
                + home().walletCount() + " wallets");

        BaseClass.logger.pass("The home page shows " + home().getTotalLabel() + " and "
                + home().walletCount() + " wallets");
    }

    @Then("the amounts should be hidden")
    public void theAmountsShouldBeHidden() {

        assertTrue(home().amountsAreHidden(),
                "The amounts are on the screen the moment the page opens, so anybody behind"
                        + " somebody signing in sees what they hold");

        System.out.println("The amounts arrive hidden");

        BaseClass.logger.pass("The amounts arrive hidden");
    }

    @When("I reveal the amounts")
    public void iRevealTheAmounts() {

        assertTrue(home().revealBalances(),
                "There is nothing on the home page that reveals the amounts, so the account's own"
                        + " figures cannot be read by the person they belong to");

        BaseClass.logger.pass("Revealed the amounts");
    }

    @Then("the amounts should be readable")
    public void theAmountsShouldBeReadable() {

        assertFalse(home().amountsAreHidden(),
                "The amounts are still hidden after the eye was pressed, so the page holds the"
                        + " account's own figures back from it. It reads: "
                        + home().whatTheHomePageSays());

        System.out.println("Revealed: " + home().getTotalBlockedAmount());

        BaseClass.logger.pass("The amounts are readable once revealed");
    }

    @Then("the home page should offer to show more wallets")
    public void theHomePageShouldOfferShowMore() {

        walletsBefore = home().walletCount();

        assertTrue(home().offersShowMore(),
                "The home page lists " + walletsBefore + " wallets and offers no way to see the"
                        + " rest, so anything beyond those is out of reach from here");

        BaseClass.logger.pass("The home page lists " + walletsBefore
                + " wallets and offers to show more");
    }

    @When("I show more wallets")
    public void iShowMoreWallets() {

        if (walletsBefore < 0) {
            walletsBefore = home().walletCount();
        }

        if (home().offersShowMore()) {
            home().showMoreWallets();
        }

        BaseClass.logger.pass("Showed the rest of the wallets");
    }

    @Then("more wallets should be listed than before")
    public void moreWalletsShouldBeListed() {

        int now = home().walletCount();

        assertTrue(now > walletsBefore,
                "Show More was pressed and the page still lists " + now + " wallets, the same as"
                        + " before, so the wallets it was holding back are still out of reach");

        System.out.println("Show More took the list from " + walletsBefore + " wallets to " + now);

        BaseClass.logger.pass("Show More took the list from " + walletsBefore + " to " + now
                + " wallets");
    }

    @Then("every wallet card should name its currency, country, balance and what is held")
    public void everyCardShouldCarryTheFour() {

        List<String> cards = home().getWalletRows();

        assertFalse(cards.isEmpty(), "There are no wallet cards to read");

        List<String> incomplete = new ArrayList<>();

        for (String card : cards) {

            if (!card.contains("|")) {
                incomplete.add(card + " names no country");
            }

            if (!card.contains("Balance:")) {
                incomplete.add(card + " states no balance");
            }

            if (!card.contains("Blocked Amount:")) {
                incomplete.add(card + " says nothing about what is held against it");
            }
        }

        assertTrue(incomplete.isEmpty(),
                "These cards are missing part of what they should say: " + incomplete);

        System.out.println("All " + cards.size() + " wallet cards name a currency, a country, a"
                + " balance and what is held");

        BaseClass.logger.pass("All " + cards.size() + " wallet cards are complete");
    }

    @Then("exactly one wallet on the home page should be the main wallet")
    public void exactlyOneWalletShouldBeMain() {

        int claiming = home().mainWalletCount();

        assertEquals(claiming, 1,
                "The home page shows " + claiming + " wallets claiming to be the main one. The"
                        + " main wallet is what the transfer, convert and withdraw screens open"
                        + " on, so anything but one leaves those starting somewhere nobody chose");

        System.out.println("The main wallet is " + home().getMainWalletCurrency());

        BaseClass.logger.pass("Exactly one wallet is the main wallet: "
                + home().getMainWalletCurrency());
    }

    @Then("every other wallet should offer to become the main one")
    public void everyOtherWalletShouldOfferToBeMain() {

        int listed = home().walletCount();

        List<String> offering = home().walletsOfferingToBeMain();

        assertEquals(offering.size(), listed - 1,
                "The home page lists " + listed + " wallets, one of them the main one, but "
                        + offering.size() + " offer to become it - so at least one wallet cannot"
                        + " be made the main one from here. Those offering: " + offering);

        System.out.println(offering.size() + " wallets offer to become the main one");

        BaseClass.logger.pass("All " + offering.size() + " other wallets offer to become main");
    }

    @Then("the sidebar should offer {string}")
    public void theSidebarShouldOffer(String expected) {

        List<String> offered = home().sidebarOptions();

        List<String> missing = new ArrayList<>();

        for (String wanted : named(expected)) {

            if (!offered.contains(wanted)) {
                missing.add(wanted);
            }
        }

        assertTrue(missing.isEmpty(),
                "The sidebar does not offer " + missing + ", so those cannot be started from the"
                        + " home page at all. It offers: " + offered);

        System.out.println("The sidebar offers " + offered);

        BaseClass.logger.pass("The sidebar offers " + offered);
    }

    /**
     * What is held against a wallet, read as a quantity rather than as a debit.
     *
     * Money set aside is not money owed. A minus in front of it says the account is owed that
     * amount rather than kept from spending it, and the total then reads as a negative sum.
     */
    @Then("no wallet should show what is held against it as a negative amount")
    public void nothingHeldShouldReadNegative() {

        Map<String, String> held = home().blockedAmountsShown();

        assertFalse(held.isEmpty(), "No wallet says anything about what is held against it");

        List<String> negative = new ArrayList<>();

        for (Map.Entry<String, String> wallet : held.entrySet()) {

            if (readsAsNegative(wallet.getValue())) {
                negative.add(wallet.getKey() + " shows " + wallet.getValue());
            }
        }

        assertTrue(negative.isEmpty(),
                "These wallets write what is held against them as a negative amount, which reads"
                        + " as money the account is owed rather than money it cannot spend: "
                        + negative + ". The total says \"" + home().getTotalBlockedAmount() + "\"");

        System.out.println("Nothing held is written as a negative amount: " + held);

        BaseClass.logger.pass("No wallet writes what is held against it as a negative amount");
    }

    @Then("Customer Service should be offered on the home page")
    public void customerServiceShouldBeOffered() {

        assertTrue(home().isCustomerServiceOffered(),
                "The home page offers no way to reach support, so somebody whose account is not"
                        + " behaving has nowhere to go from the screen they are on");

        System.out.println("Customer Service is offered on the home page");

        BaseClass.logger.pass("Customer Service is offered on the home page");
    }
}
