package com.umpay.stepdefs;

import com.umpay.pages.HomePage;
import com.umpay.pages.LanguagesPage;
import com.umpay.utility.BaseClass;
import com.umpay.utility.ExcelDataProvider;

import io.cucumber.java.After;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

/**
 * The language this account reads the application in.
 *
 * <p>Column layout of TestData/Languages_TestData.xlsx
 * 0 Scenario | 1 LoginID | 2 Password | 3 Language
 *
 * <p>The language is one setting for the whole account and every other file in this suite reads
 * the screen in English, so every scenario that chooses one reads what the account was on before
 * it touched anything and puts it back before it ends.
 */
public class LanguagesStepDefs {

    /** How many times opening the page is worth trying, as elsewhere in this suite. */
    private static final int NAVIGATION_ATTEMPTS = 3;

    private LanguagesPage languagesPage;

    private ExcelDataProvider excel;

    /** The language the account was on before this scenario touched anything. */
    private String wasOn = "";

    /** What the interface read before this scenario touched anything. */
    private String readBefore = "";

    /** The language this scenario chose, so the steps that judge it can name it. */
    private String chosen = "";

    private LanguagesPage languages() {

        if (languagesPage == null) {
            languagesPage = new LanguagesPage(BaseClass.driver);
        }

        return languagesPage;
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

        String said = languages().refusalShowing();

        return said.isEmpty() ? "" : ". The platform said: \"" + said + "\"";
    }

    @When("I open the Languages page")
    public void openTheLanguagesPage() throws InterruptedException {

        HomePage home = new HomePage(BaseClass.driver);

        Thread.sleep(3000);

        home.dismissTwoFactorPromptIfShowing();

        languages().waitUntilSignedIn(30);

        for (int attempt = 1; attempt <= NAVIGATION_ATTEMPTS; attempt++) {

            try {
                languages().open();
            } catch (Exception notOpened) {
                System.out.println("The languages did not open (attempt " + attempt + " of "
                        + NAVIGATION_ATTEMPTS + "): " + notOpened.getMessage());
                continue;
            }

            if (languages().isShowing()) {

                // Read before anything is touched: this is what the account will be put back on.
                wasOn = languages().codeChosen();
                readBefore = languages().interfaceReads();

                System.out.println("The account reads in " + languages().languageChosen() + " ("
                        + wasOn + "), and the tab says \"" + readBefore + "\"");

                BaseClass.logger.pass("Opened the languages at " + languages().getCurrentUrl()
                        + ", with the account on " + wasOn);
                return;
            }
        }

        org.testng.Assert.fail("The languages did not open. Landed on "
                + languages().getCurrentUrl() + refused());
    }

    @Then("the languages should be shown")
    public void theLanguagesShouldBeShown() {

        assertTrue(languages().isShowing(),
                "The languages are not on the screen. The page is at "
                        + languages().getCurrentUrl() + refused());

        assertFalse(languages().languagesOffered().isEmpty(),
                "The page opened but offers no language at all. It reads: " + languages().text());

        System.out.println("The languages opened at " + languages().getCurrentUrl() + ", offering "
                + languages().codesOffered());

        BaseClass.logger.pass("The languages opened at " + languages().getCurrentUrl()
                + ", offering " + languages().codesOffered());
    }

    @Then("the languages offered should be {string}")
    public void theLanguagesOfferedShouldBe(String expected) {

        List<String> offered = languages().codesOffered();
        List<String> wanted = named(expected);
        List<String> missing = new ArrayList<>();

        for (String code : wanted) {

            if (!offered.contains(code)) {
                missing.add(code);
            }
        }

        assertTrue(missing.isEmpty(),
                "The application can no longer be read in: " + missing + ". It offers: " + offered);

        assertEquals(offered.size(), wanted.size(),
                "The page offers " + offered.size() + " languages rather than " + wanted.size()
                        + ": " + offered);

        System.out.println("The application can be read in " + offered + " - "
                + languages().languagesOffered());

        BaseClass.logger.pass("The application can be read in all " + wanted.size() + ": "
                + offered);
    }

    /**
     * Each language is named in its own script.
     *
     * A list written entirely in English is no use to the one person who needs it - the one who
     * cannot read English - so what is asserted is that the names are not all Latin letters.
     */
    @Then("every language should be named in its own script")
    public void everyLanguageShouldBeInItsOwnScript() {

        List<String> offered = languages().languagesOffered();

        assertFalse(offered.isEmpty(), "There are no languages to read" + refused());

        List<String> inLatinLetters = new ArrayList<>();

        for (String name : offered) {

            if (name.matches("[\\p{ASCII}]+")) {
                inLatinLetters.add(name);
            }
        }

        assertTrue(inLatinLetters.size() < offered.size(),
                "Every language is written in Latin letters (" + offered + "), so somebody who"
                        + " cannot read them has no way to find their own");

        System.out.println("The languages name themselves: " + offered);

        BaseClass.logger.pass("Each language names itself in its own script: " + offered);
    }

    @Then("exactly one language should be chosen")
    public void exactlyOneShouldBeChosen() {

        int chosenNow = languages().howManyChosen();

        assertEquals(chosenNow, 1,
                "The page has " + chosenNow + " of its " + languages().codesOffered().size()
                        + " languages chosen rather than one");

        assertFalse(languages().languageChosen().isEmpty(),
                "One language is chosen but the page does not say which");

        System.out.println("The account reads in " + languages().languageChosen() + " ("
                + languages().codeChosen() + ")");

        BaseClass.logger.pass("Exactly one language is chosen: " + languages().languageChosen()
                + " (" + languages().codeChosen() + ")");
    }

    @Then("the page should offer nothing to save")
    public void thePageShouldOfferNothingToSave() {

        assertFalse(languages().offersToSave(),
                "The page offers something to save, so a language chosen and not saved would"
                        + " silently not apply. The page reads: " + languages().text());

        System.out.println("There is nothing to save - choosing a language is the whole of it");

        BaseClass.logger.pass("The page offers nothing to save: choosing a language is the whole"
                + " of it");
    }

    @When("I put the language on the one named in {string} of {string} of {string}")
    public void putTheLanguageOn(String rowNumber, String sheetName, String fileName) {

        chosen = fromTheSheet(rowNumber, sheetName, fileName, "Language");

        assertFalse(chosen.trim().isEmpty(),
                "Row " + rowNumber + " of " + fileName + " names no language");

        assertNotEquals(chosen, wasOn,
                "The account is already on " + chosen + ", so choosing it would prove nothing");

        languages().choose(chosen);

        BaseClass.logger.pass("Chose " + chosen + ", coming from " + wasOn);
    }

    @Then("the interface should come back in that language")
    public void theInterfaceShouldComeBackInIt() {

        assertEquals(languages().codeChosen(), chosen,
                "The page was asked for " + chosen + " but shows " + languages().codeChosen()
                        + " as the language of the account" + refused());

        String readsNow = languages().interfaceReads();

        assertNotEquals(readsNow, readBefore,
                "The account was put on " + chosen + " but the interface still reads \"" + readsNow
                        + "\", exactly as it did in " + wasOn + ", so nothing was translated");

        System.out.println("In " + chosen + " the tab reads \"" + readsNow + "\", where in "
                + wasOn + " it read \"" + readBefore + "\"");

        BaseClass.logger.pass("In " + chosen + " the interface reads \"" + readsNow + "\", where"
                + " in " + wasOn + " it read \"" + readBefore + "\"");
    }

    @Then("only that language should be chosen")
    public void onlyThatLanguageShouldBeChosen() {

        assertEquals(languages().codeChosen(), chosen,
                chosen + " was chosen but the page shows " + languages().codeChosen());

        assertEquals(languages().howManyChosen(), 1,
                "Choosing " + chosen + " left " + languages().howManyChosen() + " languages"
                        + " chosen, and the application could not know which was meant");

        System.out.println("Only " + chosen + " is chosen");

        BaseClass.logger.pass("Choosing " + chosen + " left only that language chosen");
    }

    @Then("it should still be chosen when the page is opened again")
    public void itShouldStillBeChosen() {

        languages().openByItsAddress();

        assertTrue(languages().isShowing(),
                "The languages did not open again. Landed on " + languages().getCurrentUrl()
                        + refused());

        assertEquals(languages().codeChosen(), chosen,
                chosen + " was chosen but opening the page again shows "
                        + languages().codeChosen() + ", so the language did not keep" + refused());

        System.out.println("Opened again, the account still reads in " + chosen);

        BaseClass.logger.pass(chosen + " was still chosen when the page was opened again");
    }

    @When("I go to the home page")
    public void goToTheHomePage() {

        languages().openTheHomePage();

        BaseClass.logger.pass("Went to the home page");
    }

    @Then("the interface should still be in that language")
    public void theInterfaceShouldStillBeInIt() {

        String readsNow = languages().interfaceReads();

        assertNotEquals(readsNow, readBefore,
                "The account was put on " + chosen + " but the home page reads \"" + readsNow
                        + "\", the same as it did in " + wasOn + ", so the language did not follow"
                        + " off the page it was chosen on");

        System.out.println("The home page reads \"" + readsNow + "\" in " + chosen);

        BaseClass.logger.pass("The language followed onto the home page, which reads \"" + readsNow
                + "\"");
    }

    /**
     * Puts the account back on the language it was found on.
     *
     * Every other file in this suite reads the screen in English, so this is what keeps a
     * scenario about Chinese from failing a scenario about a transfer.
     */
    @When("I put the language back the way it was")
    public void putItBack() {

        assertFalse(wasOn.isEmpty(),
                "What the account was on was never read, so it cannot be put back");

        languages().openByItsAddress();

        if (!wasOn.equalsIgnoreCase(languages().codeChosen())) {
            languages().choose(wasOn);
        }

        BaseClass.logger.pass("Put the language back on " + wasOn);
    }

    @Then("the interface should be back in the language it was in")
    public void theInterfaceShouldBeBack() {

        languages().openByItsAddress();

        assertEquals(languages().codeChosen(), wasOn,
                "The account started in " + wasOn + " and is now in " + languages().codeChosen()
                        + ", which would hand every later scenario a screen it was not written"
                        + " for" + refused());

        assertEquals(languages().interfaceReads(), readBefore,
                "The account is back on " + wasOn + " but the interface reads \""
                        + languages().interfaceReads() + "\" where it read \"" + readBefore
                        + "\" before");

        System.out.println("The account is back in " + wasOn + ", reading \"" + readBefore + "\"");

        BaseClass.logger.pass("The account is back in " + wasOn + ", the language it was found in");
    }

    /**
     * Puts the account back on the language it was found on, however the scenario ended.
     *
     * The step that puts it back is part of each scenario and says so in the report, which is
     * where it belongs - but a scenario that fails half way through never reaches its own last
     * step, and would leave the whole suite reading a screen none of its other files were
     * written for. So this runs whatever happened, and does nothing when the scenario already
     * put it back.
     */
    @After(value = "@languages", order = 100)
    public void leaveTheAccountInTheLanguageItWasFound() {

        if (wasOn.isEmpty()) {
            return;
        }

        try {
            if (wasOn.equalsIgnoreCase(languages().codeChosen())) {
                return;
            }

            languages().openByItsAddress();

            if (!wasOn.equalsIgnoreCase(languages().codeChosen())) {

                languages().choose(wasOn);

                System.out.println("Put the account back on " + wasOn
                        + " after the scenario ended");
            }
        } catch (Exception couldNotPutItBack) {
            System.out.println("WARNING: the account may have been left in "
                    + languages().codeChosen() + " rather than " + wasOn + ": "
                    + couldNotPutItBack.getMessage());
        }
    }
}
