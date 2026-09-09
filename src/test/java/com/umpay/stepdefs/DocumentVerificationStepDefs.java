package com.umpay.stepdefs;

import com.umpay.pages.DocumentVerificationPage;
import com.umpay.pages.HomePage;
import com.umpay.utility.BaseClass;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * What this account's documents say about it.
 *
 * <p>Column layout of TestData/DocumentVerification_TestData.xlsx
 * 0 Scenario | 1 LoginID | 2 Password
 *
 * <p>Nothing here changes anything: the dialog carries no form, no upload and nothing to submit,
 * and the only thing on it that can be pressed is Close. The expected details are deliberately not
 * kept in the sheet - these scenarios hold the dialog to being complete and consistent rather
 * than to one particular person's name, so they still mean something on another account.
 */
public class DocumentVerificationStepDefs {

    /** How many times opening it is worth trying, as elsewhere in this suite. */
    private static final int NAVIGATION_ATTEMPTS = 3;

    private DocumentVerificationPage documentPage;

    /** Where the run was before the dialog was opened over it. */
    private String openedOver = "";

    private DocumentVerificationPage documents() {

        if (documentPage == null) {
            documentPage = new DocumentVerificationPage(BaseClass.driver);
        }

        return documentPage;
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

        String said = documents().refusalShowing();

        return said.isEmpty() ? "" : ". The platform said: \"" + said + "\"";
    }

    @When("I open the Document Verification")
    public void openTheDocumentVerification() throws InterruptedException {

        HomePage home = new HomePage(BaseClass.driver);

        Thread.sleep(3000);

        home.dismissTwoFactorPromptIfShowing();

        documents().waitUntilSignedIn(30);

        openedOver = documents().getCurrentUrl();

        for (int attempt = 1; attempt <= NAVIGATION_ATTEMPTS; attempt++) {

            try {
                documents().open();
            } catch (Exception notOpened) {
                System.out.println("The document verification did not open (attempt " + attempt
                        + " of " + NAVIGATION_ATTEMPTS + "): " + notOpened.getMessage());
                continue;
            }

            if (documents().isShowing() || documents().isShowingTheForm()) {

                BaseClass.logger.pass("Opened the document verification over "
                        + documents().getCurrentUrl() + ", which "
                        + (documents().isShowing()
                                ? "shows what the account was verified on"
                                : "asks for a document to be submitted"));
                return;
            }
        }

        org.testng.Assert.fail("The document verification did not open. The page is at "
                + documents().getCurrentUrl() + refused());
    }

    @Then("the document verification should be shown")
    public void itShouldBeShown() {

        assertTrue(documents().isShowing(),
                "The document verification is not on the screen. The page is at "
                        + documents().getCurrentUrl() + refused());

        Map<String, String> shown = documents().detailsShown();

        assertFalse(shown.isEmpty(),
                "The dialog opened but says nothing about the account at all. It reads: "
                        + documents().text());

        System.out.println("The document verification opened over " + openedOver + ", showing "
                + shown.size() + " details: " + documents().labelsShown());

        BaseClass.logger.pass("The document verification opened over " + openedOver + ", showing "
                + shown.size() + " details");
    }

    /**
     * The drawer says where the account stands without anything being opened.
     *
     * An account that cannot tell whether it is verified is one that cannot tell why it is being
     * refused.
     */
    @Then("the drawer should say where the account stands")
    public void theDrawerShouldSayWhereItStands() {

        String verdict = documents().verdictInTheDrawer();

        assertFalse(verdict.trim().isEmpty(),
                "The profile drawer says nothing beside Document Verification, so there is no way"
                        + " to tell where the account stands without opening it");

        System.out.println("The drawer says the account is \"" + verdict + "\"");

        BaseClass.logger.pass("The drawer says the account is \"" + verdict + "\"");
    }

    @Then("it should carry {string}")
    public void itShouldCarry(String expected) {

        Map<String, String> shown = documents().detailsShown();
        List<String> missing = new ArrayList<>();
        List<String> blank = new ArrayList<>();

        for (String label : named(expected)) {

            String value = documents().shownUnder(label);

            if (!shown.containsKey(label)) {
                missing.add(label);
            } else if (value.trim().isEmpty()) {
                blank.add(label);
            }
        }

        assertTrue(missing.isEmpty(),
                "The dialog no longer says anything about: " + missing + ". It shows: "
                        + documents().labelsShown());

        assertTrue(blank.isEmpty(),
                "These are shown with nothing under them: " + blank);

        List<String> carried = new ArrayList<>();

        for (String label : named(expected)) {
            carried.add(label + " = " + documents().shownUnder(label));
        }

        System.out.println("The dialog carries " + carried);

        BaseClass.logger.pass("The dialog carries " + carried);
    }

    @Then("nothing it shows should be blank")
    public void nothingShouldBeBlank() {

        Map<String, String> shown = documents().detailsShown();

        assertFalse(shown.isEmpty(), "There is nothing on the dialog to read" + refused());

        List<String> blank = new ArrayList<>();

        for (Map.Entry<String, String> detail : shown.entrySet()) {

            if (detail.getValue().trim().isEmpty()) {
                blank.add(detail.getKey());
            }
        }

        assertTrue(blank.isEmpty(),
                "These are shown with nothing under them, so nobody can tell whether the account"
                        + " was verified without them or whether the dialog has simply lost them: "
                        + blank);

        System.out.println("All " + shown.size() + " details carry a value");

        BaseClass.logger.pass("All " + shown.size() + " details the dialog shows carry a value");
    }

    @Then("all three pictures should be there, one under each caption")
    public void allThreePicturesShouldBeThere() {

        List<String> captions = documents().captionsShown();
        List<String> pictures = documents().picturesShown();

        assertEquals(captions.size(), 3,
                "The dialog offers " + captions.size() + " captions rather than the three the"
                        + " verification was granted on: " + captions);

        assertEquals(pictures.size(), captions.size(),
                "The dialog captions " + captions + " but carries " + pictures.size()
                        + " pictures, and a caption with no picture under it is a document nobody"
                        + " can check");

        System.out.println("All three pictures are there under " + captions);

        BaseClass.logger.pass("All three pictures are there, one under each of " + captions);
    }

    /**
     * The document standing behind the account has not expired.
     *
     * An account verified on a document that has since run out is verified on nothing: the whole
     * point of an expiry date is that the document stops standing for anything after it.
     */
    @Then("the document it is verified on should not have expired")
    public void itShouldNotHaveExpired() {

        String expiry = documents().shownUnder("National ID Expiry Date");

        assertFalse(expiry.trim().isEmpty(),
                "The dialog does not say when the document expires, so there is no telling"
                        + " whether it still stands for anything. It shows: "
                        + documents().labelsShown());

        LocalDate expiresOn;

        try {
            expiresOn = LocalDate.parse(expiry.trim());
        } catch (DateTimeParseException notADate) {
            throw new AssertionError("The document expires on \"" + expiry + "\", which is not a"
                    + " date anybody can act on");
        }

        LocalDate today = LocalDate.now();

        assertFalse(expiresOn.isBefore(today),
                "The account is \"" + documents().verdictInTheDrawer() + "\" on a "
                        + documents().shownUnder("Document Type") + " that expired on " + expiry
                        + ", which is " + java.time.temporal.ChronoUnit.DAYS.between(expiresOn, today)
                        + " days ago");

        System.out.println("The document expires on " + expiry + ", which is still ahead of "
                + today);

        BaseClass.logger.pass("The document the account is verified on expires on " + expiry
                + ", which has not passed");
    }

    @When("I close the document verification")
    public void closeIt() {

        documents().close();

        BaseClass.logger.pass("Closed the document verification");
    }

    @Then("it should be gone, and the page underneath should still be there")
    public void itShouldBeGone() {

        assertFalse(documents().isShowing(),
                "The dialog is still on the screen after being closed" + refused());

        assertEquals(documents().getCurrentUrl(), openedOver,
                "Closing the dialog left the run on " + documents().getCurrentUrl()
                        + " rather than giving back " + openedOver + ", which is where it was"
                        + " opened over");

        System.out.println("The dialog closed, giving back " + openedOver);

        BaseClass.logger.pass("The dialog closed and gave back " + openedOver);
    }

    // ------------------------------------------------------------------
    // An account that has not been verified
    // ------------------------------------------------------------------

    /**
     * A new account has not been verified, and the drawer says so rather than saying nothing.
     *
     * The two states are told apart by where the drawer leads: a verified account is shown what
     * it was verified on, and one that is not is sent to the form that submits a document.
     */
    @Then("the account should still have a document to submit")
    public void itShouldStillHaveADocumentToSubmit() {

        String verdict = documents().verdictInTheDrawer();

        assertFalse(verdict.trim().isEmpty(),
                "The drawer says nothing beside Document Verification, so a new account has no"
                        + " way to learn that it still has to verify itself");

        assertFalse(verdict.equalsIgnoreCase("Verified"),
                "The drawer says this account is \"" + verdict + "\", but it was registered"
                        + " without a document ever being submitted");

        assertTrue(documents().isShowingTheForm(),
                "The drawer said \"" + verdict + "\" but did not lead to the form that submits a"
                        + " document. The page is at " + documents().getCurrentUrl() + refused());

        assertFalse(documents().isShowing(),
                "A document was never submitted for this account, yet it is being shown details"
                        + " as though it had been verified: " + documents().labelsShown());

        System.out.println("The drawer says \"" + verdict + "\" and leads to "
                + documents().getCurrentUrl());

        BaseClass.logger.pass("The account is not verified: the drawer says \"" + verdict
                + "\" and leads to " + documents().getCurrentUrl());
    }

    /**
     * The form asks for everything a document needs to be judged on.
     *
     * A form that asks for the number but not the expiry, or for neither picture, is one that
     * cannot produce a verification anybody could stand behind.
     */
    @Then("the form should ask for a document and its pictures")
    public void theFormShouldAskForADocument() {

        assertTrue(documents().isShowingTheForm(),
                "The form that submits a document is not open. The page is at "
                        + documents().getCurrentUrl() + refused());

        List<String> asked = documents().askedForOnTheForm();
        List<String> missing = new ArrayList<>();

        for (String wanted : new String[] {"documentId", "expiryDate"}) {

            if (!asked.contains(wanted)) {
                missing.add(wanted);
            }
        }

        assertTrue(missing.isEmpty(),
                "The form does not ask for: " + missing + ". It asks for: " + asked);

        assertTrue(documents().picturesAskedFor() > 0,
                "The form takes no pictures at all, so there would be nothing to verify the"
                        + " document against. It asks for: " + asked);

        assertTrue(documents().offersToSubmit(),
                "The form asks for a document but offers no way to submit it");

        System.out.println("The form asks for " + asked + ", takes "
                + documents().picturesAskedFor() + " pictures (" + documents().fileKindsAccepted()
                + ") and offers to submit them");

        BaseClass.logger.pass("The form asks for " + asked + ", takes "
                + documents().picturesAskedFor() + " pictures and offers to submit them");
    }
}
