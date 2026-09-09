package com.umpay.stepdefs;

import com.umpay.pages.HomePage;
import com.umpay.pages.ProfileDrawerPage;
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
 * The profile drawer.
 *
 * <p>Column layout of TestData/Profile_TestData.xlsx
 * 0 Scenario | 1 LoginID | 2 Password | 3 ReferralCode | 4 DocumentVerification | 5 AccountStatus
 */
public class ProfileStepDefs {

    private static final int REFERRAL_CODE = 3;

    private static final int DOCUMENT_VERIFICATION = 4;

    private static final int ACCOUNT_STATUS = 5;

    /** The code the drawer was showing when it was copied, kept because copying closes it. */
    private String codeShown = "";

    private ProfileDrawerPage drawerPage;

    private ExcelDataProvider excel;

    private ProfileDrawerPage drawer() {

        if (drawerPage == null) {
            drawerPage = new ProfileDrawerPage(BaseClass.driver);
        }

        return drawerPage;
    }

    private String fromTheSheet(String rowNumber, String sheetName, String fileName, int column) {

        excel = new ExcelDataProvider(fileName, sheetName);

        return excel.getStringData(sheetName, Integer.parseInt(rowNumber), column);
    }

    /**
     * Opens the drawer.
     *
     * The two factor prompt is dismissed first because it covers the top bar the drawer opens
     * from, and a click that lands on it opens nothing while reporting no error at all.
     */
    @When("I open the profile drawer")
    public void openTheProfileDrawer() throws InterruptedException {

        Thread.sleep(3000);

        new HomePage(BaseClass.driver).dismissTwoFactorPromptIfShowing();

        drawer().open();

        assertTrue(drawer().isShowing(), "The profile drawer did not open");

        BaseClass.logger.pass("Opened the profile drawer");
    }

    @Then("the profile drawer should offer {string}")
    public void theDrawerShouldOffer(String functions) {

        List<String> offered = drawer().functionsOffered();

        List<String> missing = new ArrayList<>();

        for (String function : functions.split(",")) {
            if (offered.stream().noneMatch(said -> said.startsWith(function.trim()))) {
                missing.add(function.trim());
            }
        }

        assertTrue(missing.isEmpty(), "The profile drawer does not offer " + missing
                + ". It offers: " + offered);

        System.out.println("The profile drawer offers " + offered);

        BaseClass.logger.pass("The drawer offers " + offered);
    }

    /**
     * What the drawer says about the account, as the sheet records it.
     *
     * The referral code is this account's own and belongs in the sheet; a code that changed
     * would mean the drawer is showing somebody else's, which is worth failing for.
     */
    @Then("the profile drawer should show the account details in {string} of {string} of {string}")
    public void theDrawerShouldShowTheAccountDetails(String row, String sheetName, String fileName) {

        assertEquals(drawer().referralCode(),
                fromTheSheet(row, sheetName, fileName, REFERRAL_CODE), "Referral code");

        assertEquals(drawer().documentVerification(),
                fromTheSheet(row, sheetName, fileName, DOCUMENT_VERIFICATION), "Document verification");

        assertTrue(drawer().says(fromTheSheet(row, sheetName, fileName, ACCOUNT_STATUS)).length() > 0,
                "The drawer does not report the account as "
                        + fromTheSheet(row, sheetName, fileName, ACCOUNT_STATUS));

        BaseClass.logger.pass("The drawer shows referral code " + drawer().referralCode()
                + ", documents " + drawer().documentVerification());
    }

    /**
     * Copies the referral code the drawer is showing.
     *
     * The code is read first and kept, because the drawer closes as the copy is pressed and there
     * would be nothing left on the screen to compare the clipboard against afterwards.
     */
    @When("I copy my referral code")
    public void copyMyReferralCode() {

        assertTrue(drawer().canCopyTheReferralCode(),
                "The drawer shows the referral code " + drawer().referralCode() + " but offers no"
                        + " way to copy it, so it would have to be read off the screen and typed"
                        + " out again by hand - and a referral code mistyped is somebody else's"
                        + " commission");

        codeShown = drawer().referralCode();

        drawer().copyTheReferralCode();

        BaseClass.logger.pass("Pressed the control that copies the referral code " + codeShown);
    }

    @Then("the referral code should be on the clipboard")
    public void theCodeShouldBeOnTheClipboard() {

        assertFalse(codeShown.isEmpty(),
                "The drawer showed no referral code to copy, so there is nothing this account"
                        + " could pass on to anybody");

        String copied = drawer().whatWasCopied();

        assertFalse(copied.isEmpty(),
                "Nothing reached the clipboard, so anybody sharing their code would paste whatever"
                        + " happened to be there before. The drawer showed " + codeShown);

        assertEquals(copied, codeShown,
                "What was copied is not the code the drawer shows, so anybody passing it on would"
                        + " be handing out the wrong code");

        System.out.println("The clipboard holds " + copied + ", which is the code shown");

        BaseClass.logger.pass("Copying put " + copied + " on the clipboard, which is the code the"
                + " drawer shows");
    }

    /**
     * The application says the code was copied.
     *
     * Copying leaves nothing on the screen to see - the clipboard is somewhere else - so a notice
     * is the only way somebody knows the press was heard rather than pressing it again.
     */
    @Then("the application should confirm the referral code was copied")
    public void theApplicationShouldConfirmTheCopy() {

        String said = drawer().noticeShowing();

        assertFalse(said.isEmpty(),
                "Nothing was said back, so there is no way to tell the code was copied from the"
                        + " press having done nothing at all");

        assertTrue(said.contains(codeShown),
                "The application said \"" + said + "\", which does not name the code it copied ("
                        + codeShown + "), so somebody could not tell which code they now hold");

        System.out.println("The application said: " + said);

        BaseClass.logger.pass("The application confirmed the copy: " + said);
    }

    /**
     * The code is the same every time it is looked at.
     *
     * A referral code that changed between readings would quietly break every link and message
     * this account had already sent out, and the commission would go nowhere.
     */
    @Then("the referral code should read the same each time the drawer is opened")
    public void theCodeShouldNotChange() {

        String first = drawer().referralCode();

        assertFalse(first.isEmpty(), "The drawer shows no referral code at all");

        drawer().close();

        drawer().open();

        String again = drawer().referralCode();

        assertEquals(again, first,
                "The drawer showed " + first + " and then " + again + ". A referral code that"
                        + " changes between readings breaks every link this account has already"
                        + " sent out");

        System.out.println("The referral code read " + first + " both times");

        BaseClass.logger.pass("The referral code read " + first + " on both openings of the drawer");
    }

    @When("I open {string} from the profile drawer")
    public void openFromTheProfileDrawer(String function) {

        drawer().open(function);

        BaseClass.logger.pass("Opened " + function + " from the profile drawer");
    }

    /**
     * A function opened something of its own.
     *
     * Both the address and the heading, because either alone can mislead: several of these
     * pages live under /settings and would look alike by address, and a heading can be left
     * over from the page before while the new one is still loading.
     */
    @Then("it should open the {string} page at {string}")
    public void itShouldOpenThePage(String heading, String address) {

        com.umpay.utility.Wait.until(() -> drawer().getCurrentUrl().contains(address), 15);

        assertTrue(drawer().getCurrentUrl().contains(address),
                "Expected to land on " + address + " but the page is at " + drawer().getCurrentUrl());

        assertEquals(drawer().headingShown(), heading,
                "The page at " + drawer().getCurrentUrl() + " does not read " + heading);

        BaseClass.logger.pass(heading + " opened at " + drawer().getCurrentUrl());
    }

    @When("I ask to log out")
    public void askToLogOut() {

        drawer().askToLogOut();

        BaseClass.logger.pass("Asked to log out");
    }

    @Then("the application should ask whether I mean it")
    public void theApplicationShouldAskWhetherIMeanIt() {

        assertTrue(drawer().asksToConfirmLoggingOut(),
                "Logout did not ask before signing out. The dialog reads: \""
                        + drawer().whatTheQuestionSays() + "\" and offers "
                        + drawer().theQuestionsButtons());

        System.out.println("The logout question offers " + drawer().theQuestionsButtons());

        BaseClass.logger.pass("The application asked before signing out, offering "
                + drawer().theQuestionsButtons());
    }

    @When("I answer {string}")
    public void iAnswer(String answer) {

        drawer().answerTheLogOutQuestion(answer);

        BaseClass.logger.pass("Answered " + answer);
    }

    @Then("I should still be signed in")
    public void iShouldStillBeSignedIn() {

        assertFalse(drawer().isSignedOut(),
                "Cancelling the logout signed the account out anyway");

        BaseClass.logger.pass("Still signed in, as cancelling should leave it");
    }

    @Then("I should be signed out")
    public void iShouldBeSignedOut() {

        com.umpay.utility.Wait.until(() -> drawer().isSignedOut(), 20);

        assertTrue(drawer().isSignedOut(),
                "Confirming the logout left the run signed in. The page is at "
                        + drawer().getCurrentUrl());

        BaseClass.logger.pass("Signed out");
    }

    @When("I go back in the browser")
    public void iGoBackInTheBrowser() {

        drawer().goBackInTheBrowser();

        BaseClass.logger.pass("Went back in the browser");
    }

    /**
     * Going back must not put the account back on the screen.
     *
     * A page held in the browser's history was drawn while somebody was signed in. If stepping
     * back to it shows it again, then anybody with the machine after them can read the account -
     * signing out on a shared computer would have done nothing at all.
     */
    @Then("the account should not be shown again")
    public void theAccountShouldNotBeShownAgain() {

        assertTrue(drawer().isSignedOut(),
                "Going back after signing out put the account on the screen again. The page is at "
                        + drawer().getCurrentUrl() + ", so signing out on a shared machine leaves"
                        + " the account readable by whoever sits down next");

        System.out.println("Going back after signing out does not show the account again");

        BaseClass.logger.pass("Going back after signing out does not show the account again");
    }
}
