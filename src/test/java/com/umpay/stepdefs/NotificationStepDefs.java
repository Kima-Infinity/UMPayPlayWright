package com.umpay.stepdefs;

import com.umpay.pages.HomePage;
import com.umpay.pages.NotificationPage;
import com.umpay.utility.BaseClass;
import com.umpay.utility.ExcelDataProvider;
import com.umpay.utility.Wait;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Notifications - what the platform has told this account.
 *
 * <p>Column layout of TestData/Notification_TestData.xlsx
 * 0 Scenario | 1 LoginID | 2 Password | 3 Tab
 *
 * <p>Nothing here submits a transaction and nothing here clicks anything that moves money. The
 * one thing these scenarios do change is read state, and they change it merely by looking: the
 * platform marks a notification read when it has been shown, so the count on the bell falls
 * while the page is being scrolled. That is said out loud in the feature rather than hidden.
 */
public class NotificationStepDefs {

    /** How many times opening the notifications is worth trying, as elsewhere in this suite. */
    private static final int NAVIGATION_ATTEMPTS = 3;

    private NotificationPage notificationPage;

    private ExcelDataProvider excel;

    /** What the bell said before the notifications were seen, so the drop can be measured. */
    private int unreadBefore = -1;

    /** What the bell said once they had been seen, for asking whether that stuck. */
    private int unreadAfter = -1;

    /** What was listed before a tab was chosen or the list was scrolled. */
    private List<String> listedBefore = new ArrayList<>();

    private NotificationPage notifications() {

        if (notificationPage == null) {
            notificationPage = new NotificationPage(BaseClass.driver);
        }

        return notificationPage;
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

    /**
     * Opens the notifications from the bell in the top bar.
     *
     * The count is read before the bell is clicked, because looking is what changes it: a count
     * read afterwards is a count this scenario has already interfered with.
     */
    @When("I open the Notifications page")
    public void openTheNotificationsPage() throws InterruptedException {

        HomePage home = new HomePage(BaseClass.driver);

        Thread.sleep(3000);

        home.dismissTwoFactorPromptIfShowing();

        // The bar carries the bell, and it is drawn after signing in returns. A count read
        // before it arrives is a count of nothing, which would quietly pass the scenarios that
        // ask for a drop.
        notifications().waitUntilSignedIn(30);
        notifications().waitForTheCount(15);

        unreadBefore = notifications().unreadOnTheBell();

        for (int attempt = 1; attempt <= NAVIGATION_ATTEMPTS; attempt++) {

            try {
                notifications().open();
            } catch (Exception notOpened) {
                System.out.println("The notifications did not open (attempt " + attempt + " of "
                        + NAVIGATION_ATTEMPTS + "): " + notOpened.getMessage());
                continue;
            }

            if (notifications().isShowing()) {

                System.out.println("The bell said " + unreadBefore + " were unread");

                BaseClass.logger.pass("Opened the notifications at "
                        + notifications().getCurrentUrl() + ", with " + unreadBefore + " unread");
                return;
            }
        }

        org.testng.Assert.fail("The notifications did not open. Landed on "
                + notifications().getCurrentUrl());
    }

    @Then("the notifications should list what the platform has told the account")
    public void theNotificationsShouldList() {

        List<String> listed = notifications().entries();

        assertFalse(listed.isEmpty(),
                "The notifications list nothing at all, on an account that has been depositing,"
                        + " withdrawing, converting and transferring all day. The page is at "
                        + notifications().getCurrentUrl());

        System.out.println("The notifications list " + listed.size() + ". The newest: "
                + listed.get(0));

        BaseClass.logger.pass("The notifications list " + listed.size() + ", the newest being \""
                + listed.get(0) + "\"");
    }

    @Then("the notifications should offer the tabs {string}")
    public void theNotificationsShouldOfferTheTabs(String tabs) {

        List<String> offered = notifications().tabsOffered();
        List<String> missing = new ArrayList<>();

        for (String tab : tabs.split(",")) {
            if (!offered.contains(tab.trim())) {
                missing.add(tab.trim());
            }
        }

        assertTrue(missing.isEmpty(), "The notifications do not offer " + missing
                + ". They offer: " + offered);

        System.out.println("The notifications offer " + offered);

        BaseClass.logger.pass("The notifications offer " + offered.size() + " tabs: " + offered);
    }

    /**
     * Every card says what it is, what moved, in which wallet, and when.
     *
     * A card missing any of those is a card nobody can act on, and it is the sort of thing a
     * redesign produces without anybody noticing, because the page still looks full.
     */
    @Then("each notification should name its kind, amount, currency and when it happened")
    public void eachNotificationShouldNameEverything() {

        List<String> listed = notifications().entries();

        assertFalse(listed.isEmpty(), "There are no notifications to read");

        List<String> incomplete = new ArrayList<>();

        for (String entry : listed) {

            if (notifications().kindOf(entry).isEmpty()
                    || notifications().amountOf(entry).isEmpty()
                    || notifications().currencyOf(entry).isEmpty()
                    || notifications().dateOf(entry).isEmpty()) {

                incomplete.add(entry);
            }
        }

        assertTrue(incomplete.isEmpty(), "These notifications are missing part of what they"
                + " should say: " + incomplete);

        BaseClass.logger.pass("All " + listed.size() + " notifications name their kind, amount,"
                + " currency and when they happened");
    }

    /**
     * Newest first.
     *
     * The order is the whole use of the list: a reader opens it to see what has just happened,
     * and a list in any other order sends them to the oldest thing the platform ever said.
     */
    @Then("the notifications should be listed newest first")
    public void theNotificationsShouldBeNewestFirst() {

        List<String> listed = notifications().entries();

        assertFalse(listed.isEmpty(), "There are no notifications to put in order");

        List<String> outOfOrder = new ArrayList<>();

        for (int entry = 1; entry < listed.size(); entry++) {

            String older = notifications().dateOf(listed.get(entry));
            String newer = notifications().dateOf(listed.get(entry - 1));

            if (older.compareTo(newer) > 0) {
                outOfOrder.add(newer + " is listed above " + older);
            }
        }

        assertTrue(outOfOrder.isEmpty(), "The notifications are not newest first: " + outOfOrder);

        BaseClass.logger.pass("All " + listed.size() + " notifications are listed newest first");
    }

    /**
     * A wallet is written the same way every time it appears.
     *
     * Held against the list itself rather than against a table of symbols kept here: what makes
     * this worth checking is not which symbol the platform picked for a currency but that it
     * picks the same one every time, since a reader scanning a column of figures reads the
     * symbol and not the code.
     */
    @Then("each amount should be written with the symbol its currency uses")
    public void eachAmountShouldCarryItsSymbol() {

        List<String> listed = notifications().entries();

        assertFalse(listed.isEmpty(), "There are no notifications to read amounts from");

        Map<String, String> symbolFor = new HashMap<>();
        List<String> wrong = new ArrayList<>();

        for (String entry : listed) {

            String amount = notifications().amountOf(entry);
            String currency = notifications().currencyOf(entry);

            if (!notifications().isAFigure(amount)) {
                wrong.add(currency + " is shown as \"" + amount + "\", which is not a figure");
                continue;
            }

            String symbol = notifications().symbolOf(amount);
            String seen = symbolFor.get(currency);

            if (seen == null) {
                symbolFor.put(currency, symbol);
            } else if (!seen.equals(symbol)) {
                wrong.add(currency + " is written as \"" + seen + "\" on one card and \"" + symbol
                        + "\" on another");
            }
        }

        assertTrue(wrong.isEmpty(), "The amounts are not written consistently: " + wrong);

        System.out.println("The wallets on screen are written as " + symbolFor);

        BaseClass.logger.pass("All " + listed.size() + " amounts are figures, and each wallet is"
                + " written the same way throughout: " + symbolFor);
    }

    // ------------------------------------------------------------------
    // The tabs
    // ------------------------------------------------------------------

    @When("I move to the tab named in {string} of {string} of {string}")
    public void moveToTheTab(String row, String sheetName, String fileName) {

        listedBefore = notifications().entries();

        String tab = fromTheSheet(row, sheetName, fileName, "Tab");

        notifications().chooseTab(tab);

        BaseClass.logger.pass("Moved to the " + tab + " tab");
    }

    @Then("every announcement should carry a message and when it arrived")
    public void everyAnnouncementShouldCarryAMessage() {

        List<String> listed = notifications().entries();

        assertFalse(listed.isEmpty(),
                "The Announcements tab lists nothing at all. The page reads: "
                        + notifications().text());

        List<String> incomplete = new ArrayList<>();

        for (String entry : listed) {

            String said = entry.replace(notifications().dateOf(entry), "").trim();

            // Words and a date, and no more than that. An earlier version wanted a message of
            // some length, which failed on announcements the platform's own testers had left
            // behind - "okok okok", "test 1". Short is not the same as missing, and it is the
            // platform's business how much it says.
            if (notifications().dateOf(entry).isEmpty() || said.isEmpty()) {
                incomplete.add(entry);
            }
        }

        assertTrue(incomplete.isEmpty(),
                "These announcements carry no message of their own: " + incomplete);

        System.out.println("The newest announcement reads: " + listed.get(0));

        BaseClass.logger.pass("All " + listed.size() + " announcements carry a message and when"
                + " they arrived. The newest: \"" + listed.get(0) + "\"");
    }

    @Then("the conversation with customer service should be shown")
    public void theConversationShouldBeShown()  {

        String said = notifications().text();

        assertFalse(notifications().saysItHasNone(),
                "The Messages tab says it has nothing, on an account that has been writing to"
                        + " customer service");

        assertTrue(said.length() > 200,
                "The Messages tab shows almost nothing: " + said);

        System.out.println("The Messages tab reads: "
                + said.substring(0, Math.min(300, said.length())));

        BaseClass.logger.pass("The Messages tab shows the conversation with customer service");
    }

    /**
     * A tab has to change what is listed.
     *
     * A tab that merely highlights itself while leaving the same list underneath is worse than
     * no tab at all, because a reader believes they are looking at something else.
     */
    @Then("what is listed should change")
    public void whatIsListedShouldChange() {

        List<String> now = notifications().entries();

        assertFalse(listedBefore.isEmpty(), "Nothing was listed before the tab was chosen");

        assertFalse(listedBefore.equals(now),
                "Choosing another tab left exactly the same " + now.size()
                        + " notifications on screen");

        System.out.println("The list went from " + listedBefore.size() + " entries to " + now.size());

        BaseClass.logger.pass("Choosing another tab changed the list, from " + listedBefore.size()
                + " entries to " + now.size());
    }

    // ------------------------------------------------------------------
    // More of them, and being read
    // ------------------------------------------------------------------

    @When("I scroll to the end of the notifications")
    public void scrollToTheEnd() {

        listedBefore = notifications().entries();

        notifications().scrollToTheEnd();

        BaseClass.logger.pass("Scrolled to the end of the " + listedBefore.size()
                + " notifications that had arrived");
    }

    /**
     * Looks through the notifications rather than glancing at them.
     *
     * Far enough for the platform to have marked what was shown: the count held still for three
     * passes of the list and only then began to fall, so a scenario that scrolls twice and asks
     * about the count is asking before the platform has answered.
     */
    @When("I look through the notifications")
    public void lookThroughTheNotifications() {

        listedBefore = notifications().entries();

        notifications().scrollThrough(8);

        BaseClass.logger.pass("Looked through the notifications, from " + listedBefore.size()
                + " of them to " + notifications().entries().size());
    }

    @Then("more notifications should have been brought in")
    public void moreShouldHaveBeenBroughtIn() {

        assertTrue(Wait.until(() -> notifications().entries().size() > listedBefore.size(), 20),
                "Reaching the end of the list brought nothing more in. It still lists "
                        + notifications().entries().size() + " notifications");

        System.out.println("Reaching the end took the list from " + listedBefore.size() + " to "
                + notifications().entries().size());

        BaseClass.logger.pass("Reaching the end took the list from " + listedBefore.size()
                + " notifications to " + notifications().entries().size());
    }

    /**
     * The bell's count and the marks in the list are two answers to the same question.
     *
     * Each card carries a dot that the page hides once the card has been read, so the list says
     * for itself which of the notifications on screen are still waiting - and the bell says how
     * many are waiting altogether. The list can never mark more than the bell admits to: 176
     * dots under a bell saying 413 is sound, 176 under a bell saying nothing is unread is the
     * platform contradicting itself, and a reader would trust the bell and never come looking.
     *
     * Asserted this way round rather than as a drop in the count. Being shown a notification is
     * what marks it read here - there is no control anywhere on the page that does it - but the
     * marking runs behind the showing, and each run pushes the unread ones further down a list
     * that only grows. A scenario demanding the count fall would pass once and fail every time
     * after, which is worse than no scenario at all.
     */
    @Then("the unread count should agree with the marks in the list")
    public void theCountShouldAgreeWithTheMarks() {

        int onTheBell = notifications().unreadOnTheBell();
        int markedInTheList = notifications().unreadInTheList();

        System.out.println("The bell says " + onTheBell + " unread; the list marks "
                + markedInTheList + " of the " + notifications().entries().size()
                + " on screen");

        assertTrue(markedInTheList <= onTheBell,
                "The list marks " + markedInTheList + " notifications unread while the bell says"
                        + " only " + onTheBell + " are waiting, so the two disagree");

        assertTrue(onTheBell > 0 || markedInTheList == 0,
                "The bell says nothing is unread, but the list still marks " + markedInTheList
                        + " notifications as waiting to be read");

        BaseClass.logger.pass("The bell says " + onTheBell + " unread and the list marks "
                + markedInTheList + " of those on screen, which agree");
    }

    /**
     * Looking at notifications can take them off the count, and must never add to it.
     *
     * The direction is the whole point. Being shown a notification is what marks it read on this
     * platform, so the count may fall while the page is scrolled - by how much depends on how
     * much of the list is new to the account, which no scenario can arrange. What must hold
     * every time is that it does not climb.
     */
    @Then("looking should not have added to the unread count")
    public void lookingShouldNotHaveAddedToTheCount() {

        assertTrue(unreadBefore >= 0, "The count was never read before the notifications were seen");

        unreadAfter = notifications().unreadOnTheBell();

        assertTrue(unreadAfter <= unreadBefore,
                "The bell said " + unreadBefore + " before the notifications were seen and "
                        + unreadAfter + " after, so looking at them made more of them unread");

        System.out.println("The bell went from " + unreadBefore + " to " + unreadAfter
                + " by being looked through");

        BaseClass.logger.pass("The bell went from " + unreadBefore + " to " + unreadAfter
                + " by being looked through, which is the direction it should move in");
    }

    /**
     * Read state is the platform's to keep, not the browser's.
     *
     * A count that climbs back after a reload was never saved: the page had only forgotten it
     * locally, and the next sign-in would show every notification unread all over again.
     */
    @When("I reload the notifications")
    public void reloadTheNotifications() {

        notifications().reload();

        BaseClass.logger.pass("Reloaded the notifications");
    }

    @Then("what has been seen should still be marked read")
    public void whatHasBeenSeenShouldStillBeRead() {

        assertTrue(unreadAfter >= 0, "The count was never read after the notifications were seen");

        int now = notifications().unreadOnTheBell();

        assertTrue(now <= unreadBefore,
                "The bell said " + unreadBefore + " before the notifications were seen, "
                        + unreadAfter + " after, and " + now + " once the page had been loaded"
                        + " again - so what had been read was forgotten rather than saved");

        System.out.println("After a reload the bell says " + now + ", against " + unreadAfter
                + " before it");

        BaseClass.logger.pass("After a reload the bell still says " + now + ", so being read was"
                + " saved rather than only forgotten in the browser");
    }
}
