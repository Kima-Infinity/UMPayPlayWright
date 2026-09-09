package com.umpay.stepdefs;

import com.umpay.pages.HomePage;
import com.umpay.pages.UserListPage;
import com.umpay.utility.BaseClass;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * The people this account has brought to UMPay.
 *
 * <p>Column layout of TestData/UserList_TestData.xlsx
 * 0 Scenario | 1 LoginID | 2 Password
 *
 * <p>Nothing about the people listed is kept in the sheet: who this account has brought will
 * change, and a scenario naming one of them would go stale the moment somebody else signs up.
 * What is held to is the shape of what is shown, which does not.
 */
public class UserListStepDefs {

    /** How many times opening the page is worth trying, as elsewhere in this suite. */
    private static final int NAVIGATION_ATTEMPTS = 3;

    private UserListPage userPage;

    private UserListPage users() {

        if (userPage == null) {
            userPage = new UserListPage(BaseClass.driver);
        }

        return userPage;
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

        String said = users().refusalShowing();

        return said.isEmpty() ? "" : ". The platform said: \"" + said + "\"";
    }

    @When("I open the User List")
    public void openTheUserList() throws InterruptedException {

        HomePage home = new HomePage(BaseClass.driver);

        Thread.sleep(3000);

        home.dismissTwoFactorPromptIfShowing();

        users().waitUntilSignedIn(30);

        for (int attempt = 1; attempt <= NAVIGATION_ATTEMPTS; attempt++) {

            try {
                users().open();
            } catch (Exception notOpened) {
                System.out.println("The user list did not open (attempt " + attempt + " of "
                        + NAVIGATION_ATTEMPTS + "): " + notOpened.getMessage());
                continue;
            }

            if (users().isShowing()) {

                BaseClass.logger.pass("Opened the user list at " + users().getCurrentUrl());
                return;
            }
        }

        org.testng.Assert.fail("The user list did not open. Landed on " + users().getCurrentUrl()
                + refused());
    }

    @Then("the people this account has brought should be listed")
    public void theyShouldBeListed() {

        assertTrue(users().isShowing(),
                "The user list is not on the screen. The page is at " + users().getCurrentUrl()
                        + refused());

        assertFalse(users().howManyListed() == 0,
                "The page opened but lists nobody at all, so an account that has brought people"
                        + " cannot see who they are. It reads: " + users().text());

        System.out.println("This account has brought " + users().howManyListed() + ": "
                + users().accountNumbersListed());

        BaseClass.logger.pass("This account has brought " + users().howManyListed() + " people: "
                + users().accountNumbersListed());
    }

    /**
     * Each of the three things a user is listed by is there.
     *
     * A user with no account number cannot be asked about, and one with no status cannot be told
     * from an account that has been suspended. A user with no name of their own is written N/A,
     * which is the page saying so rather than the page losing it - that is reported, not failed.
     */
    @Then("every user should be listed with a number, a name and a status")
    public void everyUserShouldCarryTheThree() {

        Map<String, String> names = users().namesListed();
        Map<String, String> states = users().statesListed();

        assertFalse(names.isEmpty(), "There is nobody listed to read" + refused());

        List<String> incomplete = new ArrayList<>();

        for (Map.Entry<String, String> user : names.entrySet()) {

            if (user.getKey().trim().isEmpty()) {
                incomplete.add("somebody is listed with no account number");
            }

            if (user.getValue().trim().isEmpty()) {
                incomplete.add(user.getKey() + " is listed with nothing under User Name");
            }

            if (!states.containsKey(user.getKey())
                    || states.get(user.getKey()).trim().isEmpty()) {
                incomplete.add(user.getKey() + " is listed with nothing under Status");
            }
        }

        assertTrue(incomplete.isEmpty(),
                "These are missing part of what they should say: " + incomplete);

        List<String> unnamed = users().thoseWithNoName();

        System.out.println("All " + names.size() + " carry a number, a name and a status"
                + (unnamed.isEmpty() ? "" : ", though " + unnamed.size() + " of them go under no"
                        + " name of their own: " + unnamed));

        BaseClass.logger.pass("All " + names.size() + " are listed with a number, a name and a"
                + " status" + (unnamed.isEmpty() ? "" : "; " + unnamed + " go under N/A"));
    }

    @Then("every account number should read as one the platform issues")
    public void everyNumberShouldReadRight() {

        List<String> numbers = users().accountNumbersListed();

        assertFalse(numbers.isEmpty(), "There is nobody listed to read" + refused());

        List<String> wrong = new ArrayList<>();

        for (String number : numbers) {

            if (!users().readsAsAnAccountNumber(number)) {
                wrong.add(number);
            }
        }

        assertTrue(wrong.isEmpty(),
                "These do not read as account numbers the platform issues, so quoting one to"
                        + " support would get nowhere: " + wrong);

        System.out.println("All " + numbers.size() + " account numbers read as the platform"
                + " issues them: " + numbers);

        BaseClass.logger.pass("All " + numbers.size() + " account numbers are twelve figures: "
                + numbers);
    }

    @Then("nobody should be listed twice")
    public void nobodyShouldBeListedTwice() {

        List<String> numbers = users().accountNumbersListed();

        assertFalse(numbers.isEmpty(), "There is nobody listed to read" + refused());

        List<String> twice = new ArrayList<>();
        List<String> seen = new ArrayList<>();

        for (String number : numbers) {

            if (seen.contains(number) && !twice.contains(number)) {
                twice.add(number);
            }

            seen.add(number);
        }

        assertTrue(twice.isEmpty(),
                "These are listed more than once, and anybody reading this page would count their"
                        + " commission twice: " + twice);

        System.out.println("All " + numbers.size() + " are listed once each");

        BaseClass.logger.pass("All " + numbers.size() + " people are listed exactly once");
    }

    @Then("every status should be one of {string}")
    public void everyStatusShouldBeKnown(String expected) {

        Map<String, String> states = users().statesListed();

        assertFalse(states.isEmpty(), "There is nobody listed to read" + refused());

        List<String> known = named(expected);
        List<String> strange = new ArrayList<>();

        for (Map.Entry<String, String> user : states.entrySet()) {

            boolean recognised = false;

            for (String state : known) {

                if (state.equalsIgnoreCase(user.getValue())) {
                    recognised = true;
                }
            }

            if (!recognised) {
                strange.add(user.getKey() + " is \"" + user.getValue() + "\"");
            }
        }

        assertTrue(strange.isEmpty(),
                "These are in a state this page has never been said to use, so nobody reading it"
                        + " would know what it means: " + strange);

        System.out.println("Everybody listed is in a state the platform uses: " + states.values());

        BaseClass.logger.pass("All " + states.size() + " are in a state the platform uses: "
                + states.values());
    }

    /**
     * Nothing here reaches into somebody else's account.
     *
     * The status is written rather than offered - a span, not a button - and that is worth
     * holding the page to rather than merely observing: a control here would be a way to lock or
     * unlock another person's money from a page meant only to be read.
     */
    @Then("there should be nothing here that changes somebody else's account")
    public void nothingShouldChangeAnotherAccount() {

        List<String> controls = users().anythingThatWouldChangeAnAccount();

        assertTrue(controls.isEmpty(),
                "The user list offers " + controls + ", which would reach into somebody else's"
                        + " account from a page meant only to be read");

        System.out.println("The list is read only: the status is written rather than offered, and"
                + " there is nothing here to press");

        BaseClass.logger.pass("The user list offers nothing that would change another account:"
                + " the status is written rather than offered");
    }
}
