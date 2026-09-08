package com.umpay.stepdefs;

import com.umpay.pages.HomePage;
import com.umpay.pages.SettingsPage;
import com.umpay.utility.BaseClass;
import com.umpay.utility.ExcelDataProvider;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

/**
 * How the application treats this account: when it lets go of it, and where it reaches it.
 *
 * <p>Column layout of TestData/Settings_TestData.xlsx
 * 0 Scenario | 1 LoginID | 2 Password | 3 Email | 4 Captcha | 5 Phone
 *
 * <p>Nothing here saves an auto logout and nothing here submits an address or a number the
 * application could accept. The email on this account is what the whole suite signs in with, the
 * phone number is what it verifies with, and an auto logout of a few minutes would throw a run out
 * in the middle of a transfer.
 */
public class SettingsStepDefs {

    /** How many times opening the page is worth trying, as elsewhere in this suite. */
    private static final int NAVIGATION_ATTEMPTS = 3;

    /** What the sheet writes in a box that is to be left empty. */
    private static final String BLANK = "EMPTY";

    private SettingsPage settingsPage;

    private ExcelDataProvider excel;

    /** The minutes the auto logout was on before this scenario pressed anything. */
    private String minutesBefore = "";

    /** Whether being logged out when idle was on before this scenario opened it. */
    private boolean wasOnBefore;

    /** What was put in the boxes, so a message can say what was submitted. */
    private List<String> submitted = new ArrayList<>();

    private SettingsPage settings() {

        if (settingsPage == null) {
            settingsPage = new SettingsPage(BaseClass.driver);
        }

        return settingsPage;
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

        String said = settings().refusalShowing();

        return said.isEmpty() ? "" : ". The platform said: \"" + said + "\"";
    }

    @When("I open the Settings page")
    public void openTheSettingsPage() throws InterruptedException {

        HomePage home = new HomePage(BaseClass.driver);

        Thread.sleep(3000);

        home.dismissTwoFactorPromptIfShowing();

        settings().waitUntilSignedIn(30);

        for (int attempt = 1; attempt <= NAVIGATION_ATTEMPTS; attempt++) {

            try {
                settings().open();
            } catch (Exception notOpened) {
                System.out.println("The settings did not open (attempt " + attempt + " of "
                        + NAVIGATION_ATTEMPTS + "): " + notOpened.getMessage());
                continue;
            }

            if (settings().isShowing()) {

                BaseClass.logger.pass("Opened the settings at " + settings().getCurrentUrl());
                return;
            }
        }

        org.testng.Assert.fail("The settings did not open. Landed on "
                + settings().getCurrentUrl() + refused());
    }

    @Then("the settings should be shown")
    public void theSettingsShouldBeShown() {

        assertTrue(settings().isShowing(),
                "The settings are not on the screen. The page is at " + settings().getCurrentUrl()
                        + refused());

        assertFalse(settings().thingsOffered().isEmpty(),
                "The settings opened but offer nothing at all. The page reads: " + settings().text());

        System.out.println("The settings opened at " + settings().getCurrentUrl() + ", offering "
                + settings().thingsOffered());

        BaseClass.logger.pass("The settings opened at " + settings().getCurrentUrl()
                + ", offering " + settings().thingsOffered());
    }

    @Then("the settings offered should be {string}")
    public void theSettingsOfferedShouldBe(String expected) {

        List<String> offered = settings().thingsOffered();
        List<String> wanted = named(expected);
        List<String> missing = new ArrayList<>();

        for (String one : wanted) {

            if (!offered.contains(one)) {
                missing.add(one);
            }
        }

        assertTrue(missing.isEmpty(),
                "The settings no longer offer: " + missing + ". They offer: " + offered);

        assertEquals(offered.size(), wanted.size(),
                "The settings offer " + offered.size() + " things rather than " + wanted.size()
                        + ": " + offered);

        System.out.println("The settings offer all " + wanted.size() + ": " + offered);

        BaseClass.logger.pass("The settings offer all " + wanted.size() + ": " + offered);
    }

    @When("I open {string} from the settings")
    public void openOneOfThem(String named) {

        settings().openThe(named);

        if (settings().isShowingAutoLogout()) {

            // Read before anything is pressed, so that leaving it alone can be proved.
            minutesBefore = settings().minutesAllowed();
            wasOnBefore = settings().autoLogoutIsOn();
        }

        BaseClass.logger.pass("Opened \"" + named + "\" at " + settings().getCurrentUrl());
    }

    @Then("the auto logout should say how long the account may sit idle")
    public void theAutoLogoutShouldSayHowLong() {

        assertTrue(settings().isShowingAutoLogout(),
                "Auto Logout did not open. Landed on " + settings().getCurrentUrl() + refused());

        assertFalse(settings().minutesAllowed().isEmpty(),
                "The auto logout does not say after how many minutes it acts, so nobody setting"
                        + " it knows what they are setting. It reads: " + settings().text());

        assertTrue(settings().offersToSave() && settings().offersToReset(),
                "The auto logout offers Save: " + settings().offersToSave() + " and Reset: "
                        + settings().offersToReset() + ", and a setting that cannot be put back is"
                        + " one nobody should be asked to experiment with");

        System.out.println("The auto logout is "
                + (settings().autoLogoutIsOn() ? "on" : "off") + " at "
                + settings().minutesAllowed() + " minutes of sitting still");

        BaseClass.logger.pass("The auto logout is "
                + (settings().autoLogoutIsOn() ? "on" : "off") + " after "
                + settings().minutesAllowed() + " minutes, and offers both Save and Reset");
    }

    @Then("it should offer to leave an order in flight alone")
    public void itShouldOfferToLeaveAnOrderAlone() {

        assertTrue(settings().isShowingAutoLogout(),
                "The auto logout is not open. The page is at " + settings().getCurrentUrl());

        assertTrue(settings().text().contains("active order"),
                "The auto logout says nothing about an order in flight, so somebody paying a"
                        + " school has no way to keep from being thrown out halfway. It reads: "
                        + settings().text());

        System.out.println("An order in flight is "
                + (settings().leavesAnOrderAlone() ? "left alone" : "not left alone"));

        BaseClass.logger.pass("The auto logout offers to leave an order in flight alone, which is"
                + " currently " + (settings().leavesAnOrderAlone() ? "on" : "off"));
    }

    @When("I put the minutes up")
    public void putTheMinutesUp() {

        settings().step("+");

        BaseClass.logger.pass("Put the minutes up from " + minutesBefore);
    }

    @When("I put the minutes down")
    public void putTheMinutesDown() {

        settings().step("-");

        BaseClass.logger.pass("Put the minutes back down");
    }

    @Then("the minutes should have gone up")
    public void theMinutesShouldHaveGoneUp() {

        assertNotEquals(settings().minutesAllowed(), minutesBefore,
                "The minutes are still " + minutesBefore + " after pressing +, so the setting"
                        + " cannot be changed from what the account was given");

        System.out.println("The minutes went from " + minutesBefore + " to "
                + settings().minutesAllowed());

        BaseClass.logger.pass("The minutes went from " + minutesBefore + " to "
                + settings().minutesAllowed());
    }

    @Then("the minutes should be back where they started")
    public void theMinutesShouldBeBack() {

        assertEquals(settings().minutesAllowed(), minutesBefore,
                "The minutes were put up and back down but read " + settings().minutesAllowed()
                        + " rather than the " + minutesBefore + " they started at");

        System.out.println("The minutes are back at " + minutesBefore);

        BaseClass.logger.pass("The minutes are back at " + minutesBefore);
    }

    @When("I leave the auto logout without saving")
    public void leaveWithoutSaving() {

        settings().openTheList();

        BaseClass.logger.pass("Left the auto logout without saving");
    }

    /**
     * Nothing was saved, which is both the point of the scenario and the safety of this file.
     *
     * An auto logout changed by accident would throw a later run out in the middle of a transfer.
     */
    @Then("the auto logout should be as it was")
    public void theAutoLogoutShouldBeAsItWas() {

        settings().openByItsAddress("/settings/logout");

        assertTrue(settings().isShowingAutoLogout(),
                "The auto logout did not open again. Landed on " + settings().getCurrentUrl()
                        + refused());

        assertEquals(settings().minutesAllowed(), minutesBefore,
                "The minutes were changed and not saved, but the account now reads "
                        + settings().minutesAllowed() + " rather than the " + minutesBefore
                        + " it was on");

        assertEquals(settings().autoLogoutIsOn(), wasOnBefore,
                "Being logged out when idle was " + (wasOnBefore ? "on" : "off")
                        + " and is now " + (settings().autoLogoutIsOn() ? "on" : "off")
                        + ", though nothing was saved");

        System.out.println("Nothing was kept: the auto logout is still "
                + (wasOnBefore ? "on" : "off") + " at " + minutesBefore + " minutes");

        BaseClass.logger.pass("Leaving without saving left the auto logout on " + minutesBefore
                + " minutes, " + (wasOnBefore ? "on" : "off") + " as it was");
    }

    @Then("it should offer {string}")
    public void itShouldOffer(String expected) {

        assertTrue(settings().isShowingAbout(),
                "About UMPay did not open. Landed on " + settings().getCurrentUrl() + refused());

        String reads = settings().text();
        List<String> missing = new ArrayList<>();

        for (String one : named(expected)) {

            if (!reads.contains(one)) {
                missing.add(one);
            }
        }

        assertTrue(missing.isEmpty(),
                "About UMPay no longer carries: " + missing + ". It reads: " + reads);

        System.out.println("About UMPay carries " + named(expected));

        BaseClass.logger.pass("About UMPay carries " + named(expected));
    }

    @Then("the form should ask for {string}")
    public void theFormShouldAskFor(String expected) {

        List<String> asked = settings().fieldsAsked();
        List<String> wanted = named(expected);
        List<String> missing = new ArrayList<>();

        for (String box : wanted) {

            if (!asked.contains(box)) {
                missing.add(box);
            }
        }

        assertTrue(missing.isEmpty(),
                "The form does not ask for: " + missing + ". It asks for: " + asked
                        + ". The page is at " + settings().getCurrentUrl() + refused());

        System.out.println("The form at " + settings().getCurrentUrl() + " asks for " + asked);

        BaseClass.logger.pass("The form at " + settings().getCurrentUrl() + " asks for " + asked);
    }

    @When("I fill the email form from {string} of {string} of {string}")
    public void fillTheEmailForm(String rowNumber, String sheetName, String fileName) {

        submitted = new ArrayList<>();

        for (String box : new String[] {"email", "captcha"}) {

            String value = fromTheSheet(rowNumber, sheetName, fileName,
                    "email".equals(box) ? "Email" : "Captcha");

            String typed = BLANK.equalsIgnoreCase(value.trim()) ? "" : value.trim();

            settings().typeInto(box, typed);

            submitted.add(box + "=" + (typed.isEmpty() ? "(empty)" : typed));
        }

        BaseClass.logger.pass("Filled the email form with " + submitted);
    }

    @When("I fill the phone form from {string} of {string} of {string}")
    public void fillThePhoneForm(String rowNumber, String sheetName, String fileName) {

        submitted = new ArrayList<>();

        for (String box : new String[] {"phone", "captcha"}) {

            String value = fromTheSheet(rowNumber, sheetName, fileName,
                    "phone".equals(box) ? "Phone" : "Captcha");

            String typed = BLANK.equalsIgnoreCase(value.trim()) ? "" : value.trim();

            settings().typeInto(box, typed);

            submitted.add(box + "=" + (typed.isEmpty() ? "(empty)" : typed));
        }

        BaseClass.logger.pass("Filled the phone form with " + submitted);
    }

    @When("I go on from the form")
    public void goOnFromTheForm() {

        settings().next();

        BaseClass.logger.pass("Pressed Next");
    }

    /**
     * The form did not go on.
     *
     * Going on is the step that sends a code to the new address or number, so a form that went on
     * from what was submitted here would have started moving the account somewhere it cannot be
     * reached.
     */
    @Then("the form should refuse to go on")
    public void theFormShouldRefuse() {

        assertFalse(settings().isAskingForACode(),
                "The form went on from " + submitted + " and is asking for the code it has just"
                        + " sent, which is this account being moved to an address or a number it"
                        + " cannot be reached at");

        boolean stillOnTheForm = settings().isShowingTheEmailForm()
                || settings().isShowingThePhoneForm();

        assertTrue(stillOnTheForm,
                "The form was submitted with " + submitted + " and the run left it for "
                        + settings().getCurrentUrl() + ", so something was accepted" + refused());

        System.out.println("The form refused " + submitted);

        BaseClass.logger.pass("The form refused " + submitted + " and stayed where it was");
    }

    @When("I go back to the settings")
    public void goBackToTheSettings() {

        settings().goBack();

        BaseClass.logger.pass("Went back from the setting");
    }

    @Then("the settings should be listed again")
    public void theSettingsShouldBeListedAgain() {

        assertTrue(settings().isShowing(),
                "Going back did not return to the settings. Landed on "
                        + settings().getCurrentUrl() + refused());

        assertFalse(settings().thingsOffered().isEmpty(),
                "Going back returned to the page but it offers nothing at all");

        System.out.println("Back on the settings, still offering " + settings().thingsOffered());

        BaseClass.logger.pass("Going back returned to the settings, still offering "
                + settings().thingsOffered());
    }
}
