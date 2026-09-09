package com.umpay.stepdefs;

import com.umpay.pages.HomePage;
import com.umpay.pages.TransferFeeSettingPage;
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
 * Who pays the fee on this account's transfers.
 *
 * <p>Column layout of TestData/TransferFeeSetting_TestData.xlsx
 * 0 Scenario | 1 LoginID | 2 Password | 3 Way
 *
 * <p>This is the one setting in the suite the transfer flows read, so every scenario that saves a
 * change here reads what the account was on before it touched anything and puts it back before it
 * ends. The setting is genuinely saved - a settings page that is never saved has not been tested -
 * but the account finishes each scenario the way it started it.
 */
public class TransferFeeSettingStepDefs {

    /** How many times opening the page is worth trying, as elsewhere in this suite. */
    private static final int NAVIGATION_ATTEMPTS = 3;

    private TransferFeeSettingPage feePage;

    private ExcelDataProvider excel;

    /** What the account was on before this scenario touched anything. */
    private String wasOn = "";

    /** The way this scenario chose, so the steps that judge it can name it. */
    private String chosen = "";

    private TransferFeeSettingPage fee() {

        if (feePage == null) {
            feePage = new TransferFeeSettingPage(BaseClass.driver);
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

        String said = fee().refusalShowing();

        return said.isEmpty() ? "" : ". The platform said: \"" + said + "\"";
    }

    @When("I open the Transfer Fee Setting page")
    public void openTheTransferFeeSettingPage() throws InterruptedException {

        HomePage home = new HomePage(BaseClass.driver);

        Thread.sleep(3000);

        home.dismissTwoFactorPromptIfShowing();

        fee().waitUntilSignedIn(30);

        for (int attempt = 1; attempt <= NAVIGATION_ATTEMPTS; attempt++) {

            try {
                fee().open();
            } catch (Exception notOpened) {
                System.out.println("The transfer fee setting did not open (attempt " + attempt
                        + " of " + NAVIGATION_ATTEMPTS + "): " + notOpened.getMessage());
                continue;
            }

            if (fee().isShowing()) {

                // Read before anything is touched: this is what the account will be put back on.
                wasOn = fee().wayChosen();

                System.out.println("The account settles the fee by \"" + wasOn + "\" ("
                        + fee().valueChosen() + ")");

                BaseClass.logger.pass("Opened the transfer fee setting at "
                        + fee().getCurrentUrl() + ", which is on \"" + wasOn + "\"");
                return;
            }
        }

        org.testng.Assert.fail("The transfer fee setting did not open. Landed on "
                + fee().getCurrentUrl() + refused());
    }

    @Then("the transfer fee setting should be shown")
    public void theSettingShouldBeShown() {

        assertTrue(fee().isShowing(),
                "The transfer fee setting is not on the screen. The page is at "
                        + fee().getCurrentUrl() + refused());

        assertFalse(fee().waysOffered().isEmpty(),
                "The page opened but offers no way of settling the fee. It reads: " + fee().text());

        System.out.println("The transfer fee setting opened at " + fee().getCurrentUrl()
                + ", offering " + fee().waysOffered());

        BaseClass.logger.pass("The transfer fee setting opened at " + fee().getCurrentUrl()
                + ", offering " + fee().waysOffered());
    }

    /**
     * Every way of settling the fee is still offered.
     *
     * One that quietly disappeared would take a whole way of settling a fee with it, and the
     * first anybody would know of it is a transfer that charged the wrong person.
     */
    @Then("the ways of settling the fee should be {string}")
    public void theWaysShouldBe(String expected) {

        List<String> offered = fee().waysOffered();
        List<String> wanted = named(expected);
        List<String> missing = new ArrayList<>();

        for (String way : wanted) {

            if (!offered.contains(way)) {
                missing.add(way);
            }
        }

        assertTrue(missing.isEmpty(),
                "The page no longer offers to settle the fee by: " + missing + ". It offers: "
                        + offered);

        assertEquals(offered.size(), wanted.size(),
                "The page offers " + offered.size() + " ways rather than " + wanted.size() + ": "
                        + offered);

        System.out.println("The fee can be settled " + wanted.size() + " ways: " + offered);

        BaseClass.logger.pass("The fee can be settled all " + wanted.size() + " ways: " + offered);
    }

    /**
     * One way is chosen and only one.
     *
     * A setting with nothing chosen is a question the account has never answered, and one with
     * two chosen is a question nobody can act on.
     */
    @Then("exactly one way of settling the fee should be chosen")
    public void exactlyOneShouldBeChosen() {

        int chosenNow = fee().waysChosen();

        assertEquals(chosenNow, 1,
                "The setting has " + chosenNow + " of its " + fee().waysOffered().size()
                        + " ways chosen rather than one. The page offers: " + fee().waysOffered());

        assertFalse(fee().wayChosen().isEmpty(),
                "One way is chosen but the page does not say which");

        System.out.println("The account is on \"" + fee().wayChosen() + "\" ("
                + fee().valueChosen() + ")");

        BaseClass.logger.pass("Exactly one way is chosen: \"" + fee().wayChosen() + "\" ("
                + fee().valueChosen() + ")");
    }

    @Then("the page should not offer to save anything")
    public void thePageShouldNotOfferToSave() {

        assertFalse(fee().offersToSave(),
                "Save can be pressed on a setting nobody has changed, which invites somebody to"
                        + " press it and wonder what they just changed");

        System.out.println("Save is not offered until something is changed");

        BaseClass.logger.pass("Nothing is offered to be saved until the setting is changed");
    }

    @When("I choose the way of settling the fee named in {string} of {string} of {string}")
    public void chooseTheWayNamedInTheSheet(String rowNumber, String sheetName, String fileName) {

        chosen = fromTheSheet(rowNumber, sheetName, fileName, "Way");

        assertFalse(chosen.trim().isEmpty(),
                "Row " + rowNumber + " of " + fileName + " names no way of settling the fee");

        fee().choose(chosen);

        BaseClass.logger.pass("Chose \"" + chosen + "\"");
    }

    @Then("the page should offer to save it")
    public void thePageShouldOfferToSaveIt() {

        assertTrue(fee().offersToSave(),
                "The setting was changed to \"" + chosen + "\" but the page will not save it, so"
                        + " the change could not be kept" + refused());

        System.out.println("Choosing \"" + chosen + "\" offered to save it");

        BaseClass.logger.pass("Choosing \"" + chosen + "\" offered to save it");
    }

    @Then("only that way should be chosen")
    public void onlyThatWayShouldBeChosen() {

        assertEquals(fee().wayChosen(), chosen,
                "\"" + chosen + "\" was chosen but the page shows \"" + fee().wayChosen()
                        + "\" as the setting");

        assertEquals(fee().waysChosen(), 1,
                "Choosing \"" + chosen + "\" left " + fee().waysChosen() + " ways chosen, and two"
                        + " of these would contradict the third");

        System.out.println("Only \"" + chosen + "\" is chosen");

        BaseClass.logger.pass("Choosing \"" + chosen + "\" left only that way chosen");
    }

    /**
     * Puts the setting on the named way, having genuinely arrived there.
     *
     * If the account is already on it, it is moved off and saved first: saving a page that was
     * never changed proves nothing, and the way the account is already on deserves testing as
     * much as the other two.
     */
    @When("I put the fee setting on the way named in {string} of {string} of {string}")
    public void putTheSettingOn(String rowNumber, String sheetName, String fileName) {

        chosen = fromTheSheet(rowNumber, sheetName, fileName, "Way");

        assertFalse(chosen.trim().isEmpty(),
                "Row " + rowNumber + " of " + fileName + " names no way of settling the fee");

        if (chosen.equalsIgnoreCase(fee().wayChosen())) {

            String somewhereElse = "";

            for (String way : fee().waysOffered()) {

                if (!way.equalsIgnoreCase(chosen)) {
                    somewhereElse = way;
                    break;
                }
            }

            assertFalse(somewhereElse.isEmpty(),
                    "The account is already on \"" + chosen + "\" and the page offers nowhere else"
                            + " to move it, so saving it could not be told from doing nothing");

            System.out.println("The account is already on \"" + chosen + "\", so it is moved to \""
                    + somewhereElse + "\" first");

            fee().choose(somewhereElse);
            fee().save();

            fee().openByItsAddress();
        }

        fee().choose(chosen);

        BaseClass.logger.pass("Put the setting on \"" + chosen + "\", coming from \""
                + fee().wayChosen() + "\"");
    }

    @When("I save the fee setting")
    public void saveTheFeeSetting() {

        assertTrue(fee().offersToSave(),
                "There is nothing the page will save, so \"" + chosen + "\" cannot be kept"
                        + refused());

        fee().save();

        BaseClass.logger.pass("Saved the fee setting");
    }

    @Then("the fee setting should still be that way when the page is opened again")
    public void theSettingShouldHaveStuck() {

        fee().openByItsAddress();

        assertTrue(fee().isShowing(),
                "The transfer fee setting did not open again. Landed on " + fee().getCurrentUrl()
                        + refused());

        assertEquals(fee().wayChosen(), chosen,
                "\"" + chosen + "\" was saved but opening the page again shows \""
                        + fee().wayChosen() + "\" (" + fee().valueChosen() + "), so the setting"
                        + " did not keep" + refused());

        System.out.println("Opened again, the account is on \"" + fee().wayChosen() + "\" ("
                + fee().valueChosen() + ")");

        BaseClass.logger.pass("\"" + chosen + "\" was still the setting when the page was opened"
                + " again");
    }

    /**
     * Puts the account back on what it was, because the transfer flows read this setting.
     *
     * Saved rather than merely chosen: a change left unsaved here would be discarded anyway, and
     * the point is that the account ends the scenario where it started it.
     */
    @When("I put the fee setting back the way it was")
    public void putItBack() {

        assertFalse(wasOn.isEmpty(),
                "What the account was on was never read, so it cannot be put back");

        if (wasOn.equalsIgnoreCase(fee().wayChosen())) {

            System.out.println("The account is already back on \"" + wasOn + "\"");

            BaseClass.logger.pass("The account is already back on \"" + wasOn + "\"");
            return;
        }

        fee().choose(wasOn);

        assertTrue(fee().offersToSave(),
                "The setting was put back to \"" + wasOn + "\" but the page will not save it, so"
                        + " the account would be left on \"" + fee().wayChosen() + "\"" + refused());

        fee().save();

        BaseClass.logger.pass("Put the setting back on \"" + wasOn + "\"");
    }

    @When("I leave the page without saving")
    public void leaveWithoutSaving() {

        fee().leaveWithoutSaving();

        BaseClass.logger.pass("Left the page without saving \"" + chosen + "\"");
    }

    @Then("the fee setting should be what it was before")
    public void theSettingShouldBeWhatItWas() {

        fee().openByItsAddress();

        assertTrue(fee().isShowing(),
                "The transfer fee setting did not open again. Landed on " + fee().getCurrentUrl()
                        + refused());

        assertEquals(fee().wayChosen(), wasOn,
                "The account started on \"" + wasOn + "\" and is now on \"" + fee().wayChosen()
                        + "\" (" + fee().valueChosen() + ")" + refused());

        System.out.println("The account is on \"" + wasOn + "\", the way it was found");

        BaseClass.logger.pass("The account is back on \"" + wasOn + "\", the way it was found");
    }
}
