package com.umpay.stepdefs;

import com.umpay.pages.HomePage;
import com.umpay.pages.PaymentPage;
import com.umpay.utility.BaseClass;
import com.umpay.utility.ExcelDataProvider;
import com.umpay.utility.Wait;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * The payment accounts this account can be paid out to.
 *
 * <p>Column layout of TestData/Payment_TestData.xlsx
 * 0 Scenario | 1 LoginID | 2 Password | 3 Way | 4 SearchFor
 *
 * <p>Nothing here saves an account and nothing here removes one. The accounts on this page are
 * what the withdraw and payout scenarios draw on, so the remove control is read and reported and
 * never clicked - one removed here would fail a scenario in another file entirely, which is the
 * hardest kind of failure to account for.
 */
public class PaymentStepDefs {

    /** How many times opening the page is worth trying, as elsewhere in this suite. */
    private static final int NAVIGATION_ATTEMPTS = 3;

    private PaymentPage paymentPage;

    private ExcelDataProvider excel;

    /** What the providers were before a search narrowed them. */
    private List<String> offeredBefore = new ArrayList<>();

    /** What the change form held before anything on it was touched. */
    private Map<String, String> detailsBefore = new LinkedHashMap<>();

    /** What was searched for, so the step that judges it can say so. */
    private String searchedFor = "";

    /** Where the run was before it opened a saved account to change it. */
    private String startedAt = "";

    private PaymentPage payment() {

        if (paymentPage == null) {
            paymentPage = new PaymentPage(BaseClass.driver);
        }

        return paymentPage;
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

    @When("I open the Payment page")
    public void openThePaymentPage() throws InterruptedException {

        HomePage home = new HomePage(BaseClass.driver);

        Thread.sleep(3000);

        home.dismissTwoFactorPromptIfShowing();

        payment().waitUntilSignedIn(30);

        for (int attempt = 1; attempt <= NAVIGATION_ATTEMPTS; attempt++) {

            try {
                payment().open();
            } catch (Exception notOpened) {
                System.out.println("The payment accounts did not open (attempt " + attempt
                        + " of " + NAVIGATION_ATTEMPTS + "): " + notOpened.getMessage());
                continue;
            }

            if (payment().isShowingTheList()) {

                BaseClass.logger.pass("Opened the payment accounts at "
                        + payment().getCurrentUrl());
                return;
            }
        }

        org.testng.Assert.fail("The payment accounts did not open. Landed on "
                + payment().getCurrentUrl());
    }

    @Then("the page should list the payment accounts the account can be paid out to")
    public void thePageShouldListTheAccounts() {

        List<String> saved = payment().savedAccounts();

        assertFalse(saved.isEmpty(),
                "The page lists no payment accounts at all, and the withdraw and payout flows"
                        + " draw on these. The page is at " + payment().getCurrentUrl());

        List<String> providers = new ArrayList<>();

        for (String one : saved) {
            providers.add(payment().providerOf(one));
        }

        System.out.println("The account can be paid out to " + saved.size() + ": " + providers);

        BaseClass.logger.pass("The account can be paid out to " + saved.size()
                + " saved accounts: " + providers);
    }

    /**
     * Every saved account says who it is with and what identifies it.
     *
     * Which details an account carries depends on its kind - a bank has an account name and
     * number, an e-wallet may carry an email and a name besides, a USDT address carries an
     * address - so what is asserted is that there are some, and that none of them is blank. An
     * account saved with an empty field is one a payout would be sent into the dark.
     */
    @Then("every saved account should name its provider and what identifies it")
    public void everySavedAccountShouldNameEverything() {

        List<String> saved = payment().savedAccounts();

        assertFalse(saved.isEmpty(), "There are no payment accounts to read");

        List<String> incomplete = new ArrayList<>();

        for (String one : saved) {

            if (payment().providerOf(one).isEmpty() || payment().detailsOf(one).isEmpty()) {
                incomplete.add(payment().oneLine(one));
                continue;
            }

            for (String label : payment().detailsOf(one).keySet()) {

                if (payment().detailsOf(one).get(label).isEmpty()) {
                    incomplete.add(payment().providerOf(one) + " has nothing under " + label);
                }
            }
        }

        assertTrue(incomplete.isEmpty(),
                "These payment accounts are missing part of what they should say: " + incomplete);

        System.out.println("The newest reads: " + payment().oneLine(saved.get(0)));

        BaseClass.logger.pass("All " + saved.size() + " payment accounts name their provider and"
                + " what identifies them. The first: \"" + payment().oneLine(saved.get(0)) + "\"");
    }

    /**
     * Every account can be changed and every account can be removed.
     *
     * Counted rather than clicked. An account whose details are wrong and which cannot be
     * changed is a payout waiting to go astray, and one that cannot be removed is worse - but
     * proving that by removing one would break the withdraw scenarios that rely on these.
     */
    @Then("every saved account should offer to be changed and removed")
    public void everySavedAccountShouldOfferBoth() {

        // Counted from the rows rather than the blocks: a block is a provider, and a provider
        // can hold more than one account - the USDT block on this account holds several
        // addresses, so six blocks stood for twelve accounts.
        int saved = payment().accountsSaved();

        assertFalse(saved == 0, "There are no payment accounts to read");

        assertEquals(payment().offeringToBeChanged(), saved,
                "The page holds " + saved + " payment accounts but offers to change "
                        + payment().offeringToBeChanged());

        assertEquals(payment().offeringToBeRemoved(), saved,
                "The page holds " + saved + " payment accounts but offers to remove "
                        + payment().offeringToBeRemoved());

        System.out.println(saved + " payment accounts across "
                + payment().savedAccounts().size() + " providers, each offering to be changed"
                + " and removed");

        BaseClass.logger.pass("All " + saved + " payment accounts, across "
                + payment().savedAccounts().size() + " providers, offer to be changed and"
                + " removed");
    }

    @When("I start adding a payment account")
    public void startAddingAPaymentAccount() {

        assertTrue(payment().offersToAdd(),
                "The page offers no way to add a payment account. It reads: " + payment().text());

        payment().addPayment();

        BaseClass.logger.pass("Started adding a payment account");
    }

    @Then("the form for a new payment account should open")
    public void theFormShouldOpen() {

        assertTrue(payment().isShowingTheForm(),
                "Adding a payment account did not open the form. Landed on "
                        + payment().getCurrentUrl());

        System.out.println("The form opened at " + payment().getCurrentUrl());

        BaseClass.logger.pass("The form opened at " + payment().getCurrentUrl());
    }

    /**
     * The form asks which money is being paid out and offers every way of receiving it.
     *
     * A way that disappeared would quietly take a whole kind of payout with it, and nobody would
     * notice until somebody went looking for it.
     */
    @Then("the form should ask for a currency and offer {string}")
    public void theFormShouldAskAndOffer(String ways) {

        assertFalse(payment().currencyChosen().isEmpty(),
                "The form does not say which currency it is set to. It reads: " + payment().text());

        List<String> offered = payment().waysToBePaidOffered();
        List<String> missing = new ArrayList<>();

        for (String way : ways.split(",")) {
            if (!offered.contains(way.trim())) {
                missing.add(way.trim());
            }
        }

        assertTrue(missing.isEmpty(), "The form does not offer " + missing
                + ". It offers: " + offered);

        System.out.println("The form is set to " + payment().currencyChosen()
                + " and offers " + offered);

        BaseClass.logger.pass("The form is set to " + payment().currencyChosen() + " and offers "
                + offered);
    }

    @When("I choose the way of being paid named in {string} of {string} of {string}")
    public void chooseTheWay(String row, String sheetName, String fileName) {

        String way = fromTheSheet(row, sheetName, fileName, "Way");

        payment().chooseTheWay(way);

        offeredBefore = payment().providersOffered();

        BaseClass.logger.pass("Chose to be paid by " + way);
    }

    /**
     * The providers are offered with what they cost and how long they take.
     *
     * That is the whole of what somebody is choosing on. A list of names alone would make the
     * choice arbitrary, and a fee that only appeared later would make it a surprise.
     */
    @Then("the providers offered should each name how long they take and what they charge")
    public void everyProviderShouldNameItsTerms() {

        List<String> offered = payment().providersOffered();

        assertFalse(offered.isEmpty(),
                "Nothing was offered to be paid through. The page shows: " + payment().text());

        List<String> incomplete = new ArrayList<>();

        for (String provider : offered) {

            if (payment().nameOf(provider).isEmpty()
                    || payment().durationOf(provider).isEmpty()
                    || payment().feeOf(provider).isEmpty()) {

                incomplete.add(provider);
            }
        }

        assertTrue(incomplete.isEmpty(),
                "These providers do not say what they cost or how long they take: " + incomplete);

        System.out.println("Offered " + offered.size() + ": " + offered);

        BaseClass.logger.pass("All " + offered.size() + " providers name how long they take and"
                + " what they charge. The first: \"" + offered.get(0) + "\"");
    }

    @Then("the providers should be searchable")
    public void theProvidersShouldBeSearchable() {

        assertTrue(payment().offersToSearch(),
                "The providers cannot be searched. What is on screen: " + payment().text());

        BaseClass.logger.pass("The providers can be searched");
    }

    @When("I search the providers for what is in {string} of {string} of {string}")
    public void searchTheProviders(String row, String sheetName, String fileName) {

        offeredBefore = payment().providersOffered();

        searchedFor = fromTheSheet(row, sheetName, fileName, "SearchFor");

        payment().searchProviders(searchedFor);

        BaseClass.logger.pass("Searched the providers for \"" + searchedFor + "\"");
    }

    /**
     * A search has to leave only what was searched for.
     *
     * A search that merely reorders the list, or leaves something else in it, is worse than no
     * search at all, because it is believed.
     */
    @Then("only the providers matching it should be left")
    public void onlyMatchingProvidersShouldBeLeft() {

        List<String> offered = payment().providersOffered();

        assertFalse(offered.isEmpty(),
                "Searching for \"" + searchedFor + "\" left nothing at all, out of "
                        + offeredBefore.size() + " providers");

        List<String> wrong = new ArrayList<>();

        for (String provider : offered) {

            if (!payment().nameOf(provider).toLowerCase().contains(searchedFor.toLowerCase())) {
                wrong.add(payment().nameOf(provider));
            }
        }

        assertTrue(wrong.isEmpty(), "Searching for \"" + searchedFor + "\" also left " + wrong);

        System.out.println("Searching for \"" + searchedFor + "\" took " + offeredBefore.size()
                + " providers down to " + offered.size());

        BaseClass.logger.pass("Searching for \"" + searchedFor + "\" took " + offeredBefore.size()
                + " providers down to " + offered.size() + ", all of them matching");
    }

    @When("I close what is being asked")
    public void closeWhatIsBeingAsked() {

        payment().closeWhatIsBeingAsked();

        BaseClass.logger.pass("Closed the providers");
    }

    @Then("the form should still be there")
    public void theFormShouldStillBeThere() {

        assertTrue(payment().isShowingTheForm(),
                "Closing the providers left the form. Landed on " + payment().getCurrentUrl());

        assertFalse(payment().isAskingSomething(), "The providers did not close");

        assertFalse(payment().waysToBePaidOffered().isEmpty(),
                "The form came back without its ways of being paid. It reads: " + payment().text());

        BaseClass.logger.pass("The form is still there, offering "
                + payment().waysToBePaidOffered());
    }

    // ------------------------------------------------------------------
    // Changing one
    // ------------------------------------------------------------------

    @When("I open the first saved account to change it")
    public void openTheFirstAccountToChange() {

        startedAt = payment().getCurrentUrl();

        payment().changeTheFirstAccount();

        BaseClass.logger.pass("Opened the first saved account to change it");
    }

    /**
     * Changing an account has to lead somewhere it can be changed.
     *
     * Either a page of its own or a panel over the list will do - what matters is that something
     * opened, since an account nobody can correct is a payout waiting to go astray.
     */
    @Then("somewhere to change it should open")
    public void somewhereToChangeItShouldOpen() {

        boolean moved = !payment().getCurrentUrl().equals(startedAt);

        assertTrue(moved || payment().isAskingSomething(),
                "Asking to change a payment account did nothing at all. Still at "
                        + payment().getCurrentUrl());

        System.out.println("Changing an account opened " + payment().getCurrentUrl());

        BaseClass.logger.pass("Changing an account opened " + payment().getCurrentUrl());
    }

    @Then("the payment accounts should be listed again")
    public void thePaymentAccountsShouldBeListedAgain() {

        assertTrue(Wait.until(() -> payment().isShowingTheList(), 20),
                "Going back did not return to the payment accounts. Landed on "
                        + payment().getCurrentUrl());

        assertTrue(Wait.until(() -> !payment().savedAccounts().isEmpty(), 20),
                "The payment accounts came back empty");

        BaseClass.logger.pass("The payment accounts are listed again, all "
                + payment().savedAccounts().size() + " of them");
    }

    @When("I go back to the payment accounts")
    public void goBackToThePaymentAccounts() {

        payment().goBack();

        BaseClass.logger.pass("Went back to the payment accounts");
    }

    // ------------------------------------------------------------------
    // What the pages will not let happen
    // ------------------------------------------------------------------

    @When("I search the providers for {string}")
    public void iSearchTheProvidersFor(String said) {

        offeredBefore = payment().providersOffered();

        payment().searchProviders(said);

        BaseClass.logger.pass("Searched the providers for \"" + said + "\"");
    }

    @Then("no provider should be listed")
    public void noProviderShouldBeListed() {

        List<String> left = payment().providersOffered();

        assertTrue(left.isEmpty(),
                "Searching for something nobody offers still left " + left.size() + " provider(s)"
                        + " listed, so the search is not narrowing anything: " + left);

        System.out.println("Nothing matched, out of the " + offeredBefore.size()
                + " offered before");

        BaseClass.logger.pass("Nothing matched, out of the " + offeredBefore.size() + " offered");
    }

    @Then("the list should say there is nothing to show")
    public void theListShouldSaySo() {

        assertTrue(payment().saysThereIsNoData(),
                "The list went empty without saying why, so somebody who mistyped cannot tell"
                        + " their own search is what emptied it. It reads: " + payment().text());

        BaseClass.logger.pass("The list says there is nothing to show");
    }

    @When("I clear the provider search")
    public void iClearTheProviderSearch() {

        payment().searchProviders("");

        BaseClass.logger.pass("Cleared the provider search");
    }

    @Then("the providers should be listed again")
    public void theProvidersShouldBeListedAgain() {

        List<String> back = payment().providersOffered();

        assertFalse(back.isEmpty(),
                "Clearing the search left the list empty, so a search that found nothing is a dead"
                        + " end - somebody who mistyped would have to close the panel and start"
                        + " the form again");

        assertEquals(back.size(), offeredBefore.size(),
                "Clearing the search brought back " + back.size() + " providers where there had"
                        + " been " + offeredBefore.size() + ", so the list did not come back as it"
                        + " was. Now: " + back + ". Before: " + offeredBefore);

        System.out.println("Clearing the search brought all " + back.size() + " providers back");

        BaseClass.logger.pass("Clearing the search brought all " + back.size() + " back");
    }

    @Then("the change form should insist on the details that identify the account")
    public void theChangeFormShouldInsist() {

        List<String> insisted = payment().whatTheChangeFormInsistsOn();

        assertFalse(insisted.isEmpty(),
                "The change form insists on nothing at all, so an account could be saved with the"
                        + " details a payout is addressed to left blank. It holds: "
                        + payment().whatTheChangeFormHolds());

        System.out.println("The form insists on " + insisted);

        BaseClass.logger.pass("The change form insists on " + insisted);
    }

    /**
     * Empties a box the form will not do without, and saves nothing.
     *
     * The value is only set on the screen. Save is never pressed, here or anywhere in this file -
     * an emptied field saved over one of these accounts would fail the withdraw and payout
     * scenarios that pay into it, in another file entirely.
     */
    @When("I empty the first detail the form insists on")
    public void iEmptyTheFirstDetail() {

        detailsBefore = payment().whatTheChangeFormHolds();

        payment().emptyTheFirstDetail();

        BaseClass.logger.pass("Emptied the first detail the form insists on, without saving");
    }

    @Then("the emptied detail should not be acceptable")
    public void theEmptiedDetailShouldNotBeAcceptable() {

        assertFalse(payment().theFirstDetailIsAcceptable(),
                "The form would accept this account with the detail a payout is addressed to left"
                        + " blank, so money could be sent into the dark. It holds: "
                        + payment().whatTheChangeFormHolds());

        System.out.println("The emptied detail is refused: \""
                + payment().whatTheFirstDetailSays() + "\"");

        BaseClass.logger.pass("The emptied detail is refused: "
                + payment().whatTheFirstDetailSays());
    }

    @When("I remember what the change form holds")
    public void iRememberWhatTheFormHolds() {

        detailsBefore = payment().whatTheChangeFormHolds();

        assertFalse(detailsBefore.isEmpty(), "The change form holds nothing to remember");

        System.out.println("The form holds " + detailsBefore);

        BaseClass.logger.pass("Remembered what the change form holds: " + detailsBefore);
    }

    @Then("the change form should hold what it held before")
    public void theFormShouldHoldWhatItHeldBefore() {

        Map<String, String> now = payment().whatTheChangeFormHolds();

        assertEquals(now, detailsBefore,
                "The account changed although nothing was saved. It held " + detailsBefore
                        + " and now holds " + now + ", which would send a payout somewhere other"
                        + " than where it was addressed");

        System.out.println("Leaving without saving changed nothing: " + now);

        BaseClass.logger.pass("Leaving the form without saving left the account as it was");
    }
}
