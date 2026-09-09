package com.umpay.stepdefs;

import com.umpay.pages.HomePage;
import com.umpay.pages.SecurityPage;
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
 * What guards this account: its login password, its PIN and its authenticator.
 *
 * <p>Column layout of TestData/Security_TestData.xlsx
 * 0 Scenario | 1 LoginID | 2 Password | 3 Current | 4 New | 5 Confirm
 *
 * <p>The forms here are filled and submitted for real, but only ever with values the application
 * cannot accept, or cannot accept to any effect - nothing at all, a new value left empty, a PIN
 * mistyped in the confirm box, or a PIN equal to the one already set. A password or PIN this
 * account could actually end up on is never typed into a new box, because every other file in the
 * suite signs in with the ones it has.
 */
public class SecurityStepDefs {

    /** How many times opening the page is worth trying, as elsewhere in this suite. */
    private static final int NAVIGATION_ATTEMPTS = 3;

    /** What the sheet writes in a box that is to be left empty. */
    private static final String BLANK = "EMPTY";

    /** The boxes the password form asks for, in the order the sheet gives them. */
    private static final String[] PASSWORD_FIELDS =
            {"current_password", "new_password", "confirm_password"};

    /** The boxes the PIN form asks for, in the same order. */
    private static final String[] PIN_FIELDS = {"current_pin", "new_pin", "confirm_pin"};

    private SecurityPage securityPage;

    private ExcelDataProvider excel;

    /** What was put in the three boxes, so a message can say what was submitted. */
    private List<String> submitted = new ArrayList<>();

    /** Which of the two forms this scenario filled in. */
    private String formFilled = "";

    /**
     * The rule the PIN form states, read while the form is still on the screen.
     *
     * A form that accepts what it should refuse leaves the page altogether, taking its own rule
     * with it - so a message that asked the page afterwards what it had promised would come back
     * empty, in exactly the case where the promise matters most.
     */
    private String ruleWhenFilled = "";

    private SecurityPage security() {

        if (securityPage == null) {
            securityPage = new SecurityPage(BaseClass.driver);
        }

        return securityPage;
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

        String said = security().refusalShowing();

        return said.isEmpty() ? "" : ". The platform said: \"" + said + "\"";
    }

    /** Fills one of the two forms from the sheet, EMPTY meaning a box left empty. */
    private void fillFrom(String rowNumber, String sheetName, String fileName, String[] boxes) {

        submitted = new ArrayList<>();

        String[] columns = {"Current", "New", "Confirm"};

        for (int box = 0; box < boxes.length; box++) {

            String value = fromTheSheet(rowNumber, sheetName, fileName, columns[box]);

            String typed = BLANK.equalsIgnoreCase(value.trim()) ? "" : value.trim();

            security().typeInto(boxes[box], typed);

            submitted.add(columns[box] + "=" + (typed.isEmpty() ? "(empty)" : typed));
        }
    }

    @When("I open the Security page")
    public void openTheSecurityPage() throws InterruptedException {

        HomePage home = new HomePage(BaseClass.driver);

        Thread.sleep(3000);

        home.dismissTwoFactorPromptIfShowing();

        security().waitUntilSignedIn(30);

        for (int attempt = 1; attempt <= NAVIGATION_ATTEMPTS; attempt++) {

            try {
                security().open();
            } catch (Exception notOpened) {
                System.out.println("The security page did not open (attempt " + attempt + " of "
                        + NAVIGATION_ATTEMPTS + "): " + notOpened.getMessage());
                continue;
            }

            if (security().isShowing()) {

                BaseClass.logger.pass("Opened the security page at " + security().getCurrentUrl());
                return;
            }
        }

        org.testng.Assert.fail("The security page did not open. Landed on "
                + security().getCurrentUrl() + refused());
    }

    @Then("the page should list what guards the account")
    public void thePageShouldListWhatGuardsIt() {

        List<String> guarded = security().thingsProtected();

        assertFalse(guarded.isEmpty(),
                "The security page lists nothing at all, so the account holder has no way to see"
                        + " or change what guards their account. The page is at "
                        + security().getCurrentUrl() + refused());

        System.out.println("The account is guarded by " + guarded);

        BaseClass.logger.pass("The account is guarded by " + guarded);
    }

    @Then("what guards the account should be {string}")
    public void whatGuardsItShouldBe(String expected) {

        List<String> guarded = security().thingsProtected();
        List<String> wanted = named(expected);
        List<String> missing = new ArrayList<>();

        for (String one : wanted) {

            if (!guarded.contains(one)) {
                missing.add(one);
            }
        }

        assertTrue(missing.isEmpty(),
                "The security page no longer offers: " + missing + ". It offers: " + guarded);

        assertEquals(guarded.size(), wanted.size(),
                "The security page offers " + guarded.size() + " things rather than "
                        + wanted.size() + ": " + guarded);

        System.out.println("The account is guarded by all " + wanted.size() + ": " + guarded);

        BaseClass.logger.pass("The account is guarded by all " + wanted.size() + ": " + guarded);
    }

    /**
     * The authenticator row says where it stands.
     *
     * Whether a second factor is on is the most important thing this page can tell somebody, and
     * a row that said nothing would leave them guessing at it.
     */
    @Then("the authenticator should say whether it is connected")
    public void theAuthenticatorShouldSayWhereItStands() {

        String said = security().statusOf("2FA Authenticator");

        assertFalse(said.isEmpty(),
                "The 2FA Authenticator row says nothing about whether it is connected, so there"
                        + " is no way to tell from this page whether a second factor is on");

        System.out.println("The authenticator says: " + said);

        BaseClass.logger.pass("The authenticator says it is \"" + said + "\"");
    }

    @When("I open {string} from the security page")
    public void openOneOfThem(String named) {

        security().openThe(named);

        BaseClass.logger.pass("Opened \"" + named + "\" at " + security().getCurrentUrl());
    }

    @Then("the form that changes the login password should open")
    public void thePasswordFormShouldOpen() {

        assertTrue(security().isShowingThePasswordForm(),
                "Login Password did not open the form that changes it. Landed on "
                        + security().getCurrentUrl() + ", asking for " + security().fieldsAsked()
                        + refused());

        System.out.println("The password form opened at " + security().getCurrentUrl()
                + ", asking for " + security().fieldsAsked());

        BaseClass.logger.pass("The password form opened at " + security().getCurrentUrl()
                + ", asking for " + security().fieldsAsked());
    }

    @Then("everything it asks for should be hidden as it is typed")
    public void everythingShouldBeHidden() {

        assertTrue(security().everythingAskedIsHidden(),
                "Something on this form shows what is typed into it. It asks for "
                        + security().fieldsAsked());

        System.out.println("All " + security().fieldsAsked().size() + " boxes hide what is typed");

        BaseClass.logger.pass("All " + security().fieldsAsked().size() + " boxes on the form hide"
                + " what is typed into them");
    }

    @When("I fill the password form from {string} of {string} of {string}")
    public void fillThePasswordForm(String rowNumber, String sheetName, String fileName) {

        formFilled = "password";

        fillFrom(rowNumber, sheetName, fileName, PASSWORD_FIELDS);

        BaseClass.logger.pass("Filled the password form with " + submitted);
    }

    @Then("the form that changes the PIN should open, saying what a PIN may be")
    public void thePinFormShouldOpen() {

        assertTrue(security().isShowingThePinForm(),
                "PIN Code did not open the form that changes it. Landed on "
                        + security().getCurrentUrl() + ", asking for " + security().fieldsAsked()
                        + refused());

        assertEquals(security().lengthAllowedIn("new_pin"), "4",
                "The new PIN box takes " + security().lengthAllowedIn("new_pin")
                        + " characters rather than the 4 the page asks for");

        assertFalse(security().ruleStated().isEmpty(),
                "The form does not say what a PIN may be, so nobody typing one knows what will be"
                        + " refused. It reads: " + security().text());

        System.out.println("The PIN form opened at " + security().getCurrentUrl()
                + ", saying: " + security().ruleStated());

        BaseClass.logger.pass("The PIN form opened at " + security().getCurrentUrl() + ", saying: "
                + security().ruleStated());
    }

    @When("I fill the PIN form from {string} of {string} of {string}")
    public void fillThePinForm(String rowNumber, String sheetName, String fileName) {

        formFilled = "PIN";

        ruleWhenFilled = security().ruleStated();

        fillFrom(rowNumber, sheetName, fileName, PIN_FIELDS);

        BaseClass.logger.pass("Filled the PIN form with " + submitted);
    }

    @When("I save the change")
    public void saveTheChange() {

        security().save();

        BaseClass.logger.pass("Pressed Save");
    }

    /**
     * The change was not made.
     *
     * A refusal here is the form standing its ground: the run is still on it and the application
     * has not said it saved anything.
     */
    @Then("the change should be refused")
    public void theChangeShouldBeRefused() {

        assertFalse(security().saidItSaved(),
                "The " + formFilled + " form said it saved " + submitted + ", which would have"
                        + " changed what this account signs in with");

        boolean stillOnTheForm = "password".equals(formFilled)
                ? security().isShowingThePasswordForm()
                : security().isShowingThePinForm();

        assertTrue(stillOnTheForm,
                "The " + formFilled + " form was submitted with " + submitted + " and the run"
                        + " left it for " + security().getCurrentUrl() + ", so something was"
                        + " accepted" + refused());

        System.out.println("The " + formFilled + " form refused " + submitted);

        BaseClass.logger.pass("The " + formFilled + " form refused " + submitted
                + " and stayed where it was");
    }

    /**
     * A PIN the page's own rule forbids is refused.
     *
     * The PIN submitted is the one the account already has, so the account is left on the PIN it
     * started with whichever way this goes - but the page states a rule, and this holds it to it.
     */
    @Then("the PIN should be refused, as the page requires")
    public void thePinShouldBeRefused() {

        String rule = ruleWhenFilled.isEmpty() ? security().ruleStated() : ruleWhenFilled;

        assertFalse(security().saidItSaved(),
                "The PIN form accepted " + submitted + " and answered \"Updated successfully\","
                        + " though the page itself says: \"" + rule + "\"");

        assertTrue(security().isShowingThePinForm(),
                "The PIN form was submitted with " + submitted + " and the run left it for "
                        + security().getCurrentUrl() + ", so the PIN was taken though the page"
                        + " says: \"" + rule + "\"" + refused());

        System.out.println("The PIN form refused " + submitted + ", as it says it must");

        BaseClass.logger.pass("The PIN form refused " + submitted + ", holding to its own rule");
    }

    @Then("the authenticator should be connected and offer to be removed or reset")
    public void theAuthenticatorShouldOfferBoth() {

        assertTrue(security().isShowingTheAuthenticator(),
                "2FA Authenticator did not open the authenticator. Landed on "
                        + security().getCurrentUrl() + refused());

        assertTrue(security().text().contains("Connected"),
                "The authenticator page does not say the authenticator is connected. It reads: "
                        + security().text());

        List<String> offered = security().authenticatorOffers();

        assertEquals(offered.size(), 2,
                "The authenticator offers " + offered + " rather than both removing and resetting"
                        + " it, and an authenticator that cannot be reset is one a lost phone"
                        + " takes the account with");

        System.out.println("The authenticator is connected and offers " + offered);

        BaseClass.logger.pass("The authenticator is connected and offers " + offered);
    }

    @When("I go back to what guards the account")
    public void goBackToTheSecurityPage() {

        security().goBack();

        BaseClass.logger.pass("Went back from the form");
    }

    @Then("what guards the account should be listed again")
    public void itShouldBeListedAgain() {

        assertTrue(security().isShowing(),
                "Going back did not return to the security page. Landed on "
                        + security().getCurrentUrl() + refused());

        List<String> guarded = security().thingsProtected();

        assertFalse(guarded.isEmpty(),
                "Going back returned to the page but it lists nothing at all");

        System.out.println("Back on the security page, still offering " + guarded);

        BaseClass.logger.pass("Going back returned to the security page, still offering "
                + guarded);
    }
}
