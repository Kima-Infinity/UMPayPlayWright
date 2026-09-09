package com.umpay.stepdefs;

import com.umpay.pages.HomePage;
import com.umpay.pages.TemplatePage;
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
 * The destinations this account has saved to send money to.
 *
 * <p>Column layout of TestData/Template_TestData.xlsx
 * 0 Scenario | 1 LoginID | 2 Password | 3 Route | 4 SearchFor
 *
 * <p>Nothing here saves a template and nothing here removes one. The bin on each card is counted
 * and reported and never clicked - these are the templates the Transfer scenarios choose from by
 * name, and one removed here would fail a scenario in another file entirely. Adding one is walked
 * as far as the form and stops there.
 */
public class TemplateStepDefs {

    /** How many times opening the page is worth trying, as elsewhere in this suite. */
    private static final int NAVIGATION_ATTEMPTS = 3;

    private TemplatePage templatePage;

    private ExcelDataProvider excel;

    /** Where the run was before it opened a saved template to change it. */
    private String startedAt = "";

    /** The way of sending money this scenario chose, so the step that judges it can say so. */
    private String chosenRoute = "";

    private TemplatePage template() {

        if (templatePage == null) {
            templatePage = new TemplatePage(BaseClass.driver);
        }

        return templatePage;
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

    /** "a, b, c" as the feature writes it, into the three things it names. */
    private List<String> named(String commaSeparated) {

        List<String> each = new ArrayList<>();

        for (String one : commaSeparated.split(",")) {

            if (!one.trim().isEmpty()) {
                each.add(one.trim());
            }
        }

        return each;
    }

    @When("I open the Template page")
    public void openTheTemplatePage() throws InterruptedException {

        HomePage home = new HomePage(BaseClass.driver);

        Thread.sleep(3000);

        home.dismissTwoFactorPromptIfShowing();

        template().waitUntilSignedIn(30);

        for (int attempt = 1; attempt <= NAVIGATION_ATTEMPTS; attempt++) {

            try {
                template().openTheList();
            } catch (Exception notOpened) {
                System.out.println("The saved templates did not open (attempt " + attempt + " of "
                        + NAVIGATION_ATTEMPTS + "): " + notOpened.getMessage());
                continue;
            }

            if (template().isShowingTheList()) {

                BaseClass.logger.pass("Opened the saved templates at "
                        + template().getCurrentUrl());
                return;
            }
        }

        org.testng.Assert.fail("The saved templates did not open. Landed on "
                + template().getCurrentUrl()
                + refused());
    }

    /** What the platform said, if it answered with a dialog of its own, ready to be appended. */
    private String refused() {

        String said = template().refusalShowing();

        return said.isEmpty() ? "" : ". The platform said: \"" + said + "\"";
    }

    @Then("the page should list the templates the account has saved")
    public void thePageShouldListTheTemplates() {

        List<String> saved = template().savedTemplates();

        assertFalse(saved.isEmpty(),
                "The page lists no saved templates at all, and the transfer flows draw on these."
                        + " The page is at " + template().getCurrentUrl() + refused());

        System.out.println("The account has saved " + saved.size() + " templates: "
                + template().namesSaved());

        BaseClass.logger.pass("The account has saved " + saved.size() + " templates: "
                + template().namesSaved());
    }

    /**
     * Every card carries the two things anybody chooses on: what the template was called, and
     * what it pays.
     *
     * A template with no name cannot be picked out of a list of ten, and one with no account
     * under it is a destination that has lost its destination.
     */
    @Then("every saved template should carry a name and the account it pays")
    public void everySavedTemplateShouldCarryBoth() {

        List<String> names = template().namesSaved();
        List<String> accounts = template().accountsPaid();

        assertFalse(names.isEmpty(), "There are no saved templates to read" + refused());

        assertEquals(accounts.size(), names.size(),
                "The page draws " + names.size() + " templates but only " + accounts.size()
                        + " of them say what they pay");

        List<String> incomplete = new ArrayList<>();

        for (int card = 0; card < names.size(); card++) {

            if (names.get(card).trim().isEmpty()) {
                incomplete.add("the template paying " + accounts.get(card) + " has no name");
            }

            if (accounts.get(card).trim().isEmpty()) {
                incomplete.add("\"" + names.get(card) + "\" does not say what it pays");
            }
        }

        assertTrue(incomplete.isEmpty(),
                "These saved templates are missing part of what they should say: " + incomplete);

        System.out.println("All " + names.size() + " saved templates name themselves and what they"
                + " pay. The first: \"" + names.get(0) + "\" paying " + accounts.get(0));

        BaseClass.logger.pass("All " + names.size() + " saved templates carry a name and the"
                + " account they pay. The first: \"" + names.get(0) + "\" paying "
                + accounts.get(0));
    }

    /**
     * The icon is the only thing on a card that says which way of sending money it was saved for.
     *
     * Two templates can name the same card number and pay it by different routes, so a card with
     * no icon leaves them indistinguishable until one is opened.
     */
    @Then("every saved template should show the way of sending it was saved for")
    public void everySavedTemplateShouldShowItsWay() {

        List<String> icons = template().iconsSaved();
        List<String> names = template().namesSaved();

        assertFalse(icons.isEmpty(), "There are no saved templates to read" + refused());

        List<String> without = new ArrayList<>();

        for (int card = 0; card < icons.size(); card++) {

            if (icons.get(card).trim().isEmpty()) {
                without.add(names.get(card));
            }
        }

        assertTrue(without.isEmpty(),
                "These saved templates show nothing about the way they were saved for: " + without);

        System.out.println("All " + icons.size() + " saved templates carry the picture of the way"
                + " they were saved for: " + icons);

        BaseClass.logger.pass("All " + icons.size() + " saved templates show the way they were"
                + " saved for");
    }

    /**
     * Every template can be changed and every template can be removed.
     *
     * Counted rather than clicked. A template whose account has changed and which cannot be
     * changed is a transfer waiting to go astray - but proving the bin by emptying it would take
     * a template the Transfer scenarios choose by name.
     */
    @Then("every saved template should offer to be changed and removed")
    public void everySavedTemplateShouldOfferBoth() {

        List<Integer> controls = template().controlsOffered();

        assertFalse(controls.isEmpty(), "There are no saved templates to read" + refused());

        List<String> names = template().namesSaved();
        List<String> lacking = new ArrayList<>();

        for (int card = 0; card < controls.size(); card++) {

            if (controls.get(card) < 2) {
                lacking.add("\"" + names.get(card) + "\" offers " + controls.get(card)
                        + " of the two");
            }
        }

        assertTrue(lacking.isEmpty(),
                "These saved templates do not offer both changing and removing: " + lacking);

        System.out.println(controls.size() + " saved templates, each offering to be changed and"
                + " removed");

        BaseClass.logger.pass("All " + controls.size() + " saved templates offer to be changed"
                + " and removed");
    }

    @When("I open the first saved template to change it")
    public void openTheFirstSavedTemplate() {

        startedAt = template().getCurrentUrl();

        template().changeTheFirstTemplate();

        BaseClass.logger.pass("Opened the first saved template to change it");
    }

    @Then("somewhere to change that template should open")
    public void somewhereToChangeItShouldOpen() {

        assertTrue(template().isChangingATemplate(),
                "Opening a saved template to change it did not lead anywhere that is changing a"
                        + " template. The run went from " + startedAt + " to "
                        + template().getCurrentUrl() + refused());

        assertTrue(template().asksForATemplateName(),
                "What opened does not ask what the template is called, so it is not somewhere the"
                        + " template can be changed. The page is at " + template().getCurrentUrl());

        System.out.println("Changing a template opened " + template().getCurrentUrl());

        BaseClass.logger.pass("Changing a saved template opened " + template().getCurrentUrl());
    }

    @When("I start adding a template")
    public void startAddingATemplate() {

        assertTrue(template().offersToAdd(),
                "The page offers no way to add a template. It reads: " + template().text());

        template().addTemplate();

        BaseClass.logger.pass("Started adding a template");
    }

    @Then("the ways a template can be saved for should open")
    public void theWaysShouldOpen() {

        assertTrue(template().isShowingTheRoutes(),
                "Adding a template did not open the ways one can be saved for. Landed on "
                        + template().getCurrentUrl() + refused());

        System.out.println("The ways opened at " + template().getCurrentUrl() + ": "
                + template().routesOffered());

        BaseClass.logger.pass("The ways a template can be saved for opened at "
                + template().getCurrentUrl() + ": " + template().routesOffered());
    }

    /**
     * Every way of sending money that a template can be saved for is still offered.
     *
     * One that quietly disappeared would take a whole kind of saved destination with it, and
     * nobody would notice until somebody went looking for the template they used to have.
     */
    @Then("the ways offered should be {string}")
    public void theWaysOfferedShouldBe(String expected) {

        List<String> offered = template().routesOffered();
        List<String> wanted = named(expected);
        List<String> missing = new ArrayList<>();

        for (String way : wanted) {

            if (!offered.contains(way)) {
                missing.add(way);
            }
        }

        assertTrue(missing.isEmpty(),
                "The page no longer offers to save a template for: " + missing
                        + ". It offers: " + offered);

        assertEquals(offered.size(), wanted.size(),
                "The page offers " + offered.size() + " ways rather than " + wanted.size()
                        + ": " + offered);

        System.out.println("A template can be saved for " + offered);

        BaseClass.logger.pass("A template can be saved for all " + wanted.size() + " ways: "
                + offered);
    }

    /**
     * The ways the page has closed say so on the page.
     *
     * A way that is closed and does not say so is worse than one that is missing: somebody
     * chooses it, nothing happens, and there is nothing there to explain why.
     */
    @Then("the ways {string} should be marked as under maintenance")
    public void theWaysShouldBeMarkedUnderMaintenance(String expected) {

        List<String> closed = template().routesUnderMaintenance();
        List<String> wanted = named(expected);
        List<String> unmarked = new ArrayList<>();

        for (String way : wanted) {

            if (!closed.contains(way)) {
                unmarked.add(way);
            }
        }

        assertTrue(unmarked.isEmpty(),
                "These ways are offered without saying they are under maintenance: " + unmarked
                        + ". The page marks: " + closed);

        System.out.println("Marked as under maintenance: " + closed);

        BaseClass.logger.pass("The page marks " + closed + " as under maintenance");
    }

    @When("I choose the way named in {string} of {string} of {string}")
    public void chooseTheWayNamedInTheSheet(String rowNumber, String sheetName, String fileName) {

        chosenRoute = fromTheSheet(rowNumber, sheetName, fileName, "Route");

        assertFalse(chosenRoute.trim().isEmpty(),
                "Row " + rowNumber + " of " + fileName + " names no way to save a template for");

        template().chooseRoute(chosenRoute);

        BaseClass.logger.pass("Chose to save a template for " + chosenRoute);
    }

    @Then("somewhere to save that template should open")
    public void somewhereToSaveItShouldOpen() {

        assertTrue(template().isSomewhereToSaveOne(),
                "Choosing \"" + chosenRoute + "\" opened nowhere a template can be saved. The run"
                        + " is still at " + template().getCurrentUrl() + refused());

        assertTrue(template().asksForATemplateName(),
                "What opened for \"" + chosenRoute + "\" does not ask what to call the template,"
                        + " so nothing saved there would be findable again. The page is at "
                        + template().getCurrentUrl());

        System.out.println("Saving a template for " + chosenRoute + " opened "
                + template().getCurrentUrl());

        BaseClass.logger.pass("Saving a template for " + chosenRoute + " opened "
                + template().getCurrentUrl());
    }

    /**
     * A way the page has closed does not open.
     *
     * The other side of the maintenance mark: the page says the way is closed, and choosing it
     * leaves the run where it was rather than half-way into a form it cannot finish.
     */
    @Then("the ways a template can be saved for should still be offered")
    public void theWaysShouldStillBeOffered() {

        assertTrue(template().isShowingTheRoutes(),
                "Choosing \"" + chosenRoute + "\", which the page marks as under maintenance, led"
                        + " to " + template().getCurrentUrl() + " instead of leaving the ways as"
                        + " they were" + refused());

        System.out.println("Choosing " + chosenRoute + " left the ways as they were: "
                + template().routesOffered());

        BaseClass.logger.pass("\"" + chosenRoute + "\" is under maintenance and did not open."
                + " The ways are still offered: " + template().routesOffered());
    }

    @When("I go back to the saved templates")
    public void goBackToTheSavedTemplates() {

        template().goBack();

        BaseClass.logger.pass("Went back from the ways a template can be saved for");
    }

    @Then("the saved templates should be listed again")
    public void theSavedTemplatesShouldBeListedAgain() {

        assertTrue(template().isShowingTheList(),
                "Going back did not return to the saved templates. Landed on "
                        + template().getCurrentUrl() + refused());

        List<String> saved = template().savedTemplates();

        assertFalse(saved.isEmpty(),
                "Going back returned to the page but it lists no templates at all");

        System.out.println("Back on the saved templates, all " + saved.size() + " still listed");

        BaseClass.logger.pass("Going back returned to the saved templates, all " + saved.size()
                + " still listed");
    }
}
