package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.umpay.utility.Wait;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Notifications - what the platform has told this account, at /v2/notification.
 *
 * Reached from the bell in the top bar, which carries the count of what is still unread. The page
 * holds three tabs and nothing else:
 *
 *   Transactions  - a card for every movement the account has made, carrying the kind of
 *                   transaction, its amount, its currency and when it happened
 *   Announcements - what the platform has announced to the account, each with its own message
 *   Messages      - the conversation with customer service
 *
 * WHAT THIS PAGE DOES NOT DO
 *
 * A card is not a link. Its cursor stays an arrow, it holds no button or anchor of any kind, and
 * clicking one changes neither the page nor the address - so there is no detail view to open, and
 * nothing here to download or share the way a bill can be. Nor is there any control that marks
 * notifications read: the whole page offers three tab buttons and no others.
 *
 * Being read happens by being seen. The bell went from 494 to 470 while this page was scrolled,
 * without a single click - so what a scenario can prove is that seeing notifications takes them
 * off the count, and that the count does not come back afterwards.
 *
 * The list grows as it is scrolled rather than paging: twenty cards arrive first, and reaching
 * the bottom brings the next of them.
 */
public class NotificationPage {

	/** Where the page lives, whichever way it was reached. */
	private static final String ROUTE = "/v2/notification";

	/** When something happened - "2026-09-07 11:34:52". */
	private static final Pattern WHEN = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");

	/** The count on the bell, which is how many are still unread. */
	private static final Pattern BADGE = Pattern.compile("^\\s*(\\d{1,4})\\+?\\s*$");

	/** A figure with the wallet's own symbol in front of it - "RM2.26", "US$0.56", "?7.49". */
	private static final Pattern MONEY = Pattern.compile("^(\\D*)([0-9][0-9,]*(?:\\.[0-9]+)?)$");

	/**
	 * One card in the list.
	 *
	 * By the shadow the page draws around each rather than by a class of its own, because the
	 * cards carry no id and no name - and by the ancestor of a dated row, so that a heading or a
	 * banner drawn in the same style is not mistaken for a notification.
	 */
	private static final String CARDS =
			"xpath=//div[contains(@class,'shadow')][.//text()[contains(.,'-')]]";

	/**
	 * The mark on a card that has not been read - a small orange dot.
	 *
	 * The page draws the dot on every card and hides it again on the ones that have been read, so
	 * what makes a card unread is the dot being shown rather than the dot being there.
	 */
	private static final String UNREAD_DOT =
			"xpath=//div[contains(@class,'bg-orange-400')][not(contains(@class,'hidden'))]";

	private final Page page;

	/** The top bar's controls. The bell is the one carrying a number. */
	private final Locator topBarButtons;

	public NotificationPage(Page ldriver) {

		this.page = ldriver;
		this.topBarButtons = page.locator("nav button");
	}

	/**
	 * Opens the notifications, by the bell where there is one and by the address otherwise.
	 *
	 * The bell is the control carrying a count, which is the only thing that tells it from the
	 * rest of a bar whose buttons have no id, no label and no text. An account with nothing
	 * unread has no count to find it by, so the address is kept as a way in - the same fallback
	 * the withdraw page keeps.
	 *
	 * The controls are tried left to right. Right to left reaches the profile avatar first, and
	 * the drawer it opens then swallows every click that follows.
	 */
	public void open() {

		if (isShowing()) {
			return;
		}

		waitUntilSignedIn(30);

		int count = topBarButtons.count();

		for (int control = 0; control < count; control++) {

			try {
				Locator one = topBarButtons.nth(control);

				if (!BADGE.matcher(one.innerText().trim()).matches()) {
					continue;
				}

				one.click(new Locator.ClickOptions().setTimeout(5000));

				if (Wait.until(this::isShowing, 8)) {
					waitUntilLoaded(20);
					return;
				}
			} catch (Exception intercepted) {
				// Not the bell, or not clickable just now. Try the next.
			}
		}

		page.navigate(page.url().replaceAll("(https?://[^/]+).*", "$1") + ROUTE);

		Wait.sleep(3000);

		if (!Wait.until(this::isShowing, 20)) {
			throw new IllegalStateException("Neither the bell nor " + ROUTE + " opened the"
					+ " notifications. The page is at " + page.url());
		}

		waitUntilLoaded(20);
	}

	/**
	 * Waits for the top bar to arrive, which is what carries the bell.
	 *
	 * Signing in returns before the platform has finished letting the account in: the address is
	 * still the login page for a moment afterwards, and the bar is drawn later still. Asking for
	 * the notifications in that moment is asking as a stranger, and the platform answers by
	 * sending the run back to the login page - which is what happened to two scenarios that read
	 * a count of nothing and one that never got in at all.
	 */
	public boolean waitUntilSignedIn(int timeoutSeconds) {

		return Wait.until(() -> !page.url().contains("/login") && topBarButtons.count() > 0,
				timeoutSeconds);
	}

	/** True once the notifications are on screen. */
	public boolean isShowing() {

		try {
			return page.url().contains(ROUTE);
		} catch (Exception notThere) {
			return false;
		}
	}

	/**
	 * Waits for the notifications to finish arriving.
	 *
	 * The page draws before its cards do, exactly as the ledger does - a list read the moment the
	 * bell is clicked is an empty list, which reads as an account nobody has ever written to.
	 */
	public boolean waitUntilLoaded(int timeoutSeconds) {

		Wait.until(() -> tabsOffered().size() > 1, timeoutSeconds);

		return Wait.until(() -> !entries().isEmpty(), timeoutSeconds);
	}

	/** The address, for a step that has to say where a click led. */
	public String getCurrentUrl() {

		return page.url();
	}

	/** Loads the page again, for asking whether being read was saved or only forgotten. */
	public void reload() {

		page.reload();

		Wait.sleep(3000);
		waitUntilLoaded(20);
	}

	/**
	 * The tabs the page offers, as it names them.
	 *
	 * Read rather than assumed, the way the ledger's kinds are: which tabs exist is what decides
	 * what an account can be told about, and one appearing or disappearing is worth noticing here
	 * rather than discovering when a scenario quietly reads the wrong list.
	 */
	public List<String> tabsOffered() {

		List<String> offered = new ArrayList<>();

		for (Locator tab : Wait.all(page.locator("xpath=//*[contains(@class,'tab')]//button"))) {

			try {
				String said = tab.innerText().replaceAll("\\s+", " ").trim();

				if (tab.isVisible() && !said.isEmpty() && !offered.contains(said)) {
					offered.add(said);
				}
			} catch (Exception gone) {
				// A tab that will not answer is not one to report.
			}
		}

		return offered;
	}

	/** Moves to one of the tabs. */
	public void chooseTab(String tab) {

		Locator control = page.locator("xpath=//button[normalize-space()=\"" + tab + "\"]");

		try {
			control.first().click();
		} catch (Exception intercepted) {
			control.first().dispatchEvent("click");
		}

		Wait.sleep(3000);
	}

	/**
	 * Everything the open tab is listing, newest first, each card as one line of text.
	 *
	 * A card's own words, run together, so that a step can ask what kind it is and when it
	 * happened without holding an element of the page.
	 */
	public List<String> entries() {

		List<String> listed = new ArrayList<>();

		for (Locator card : Wait.all(page.locator(CARDS))) {

			try {
				if (!card.isVisible()) {
					continue;
				}

				String said = card.innerText().replaceAll("\\s+", " ").trim();

				if (!said.isEmpty() && WHEN.matcher(said).find() && !listed.contains(said)) {
					listed.add(said);
				}
			} catch (Exception gone) {
				// A card that will not answer is not one to report.
			}
		}

		return listed;
	}

	/** What one notification is about - "Convert", "Deposit", "Appeal". */
	public String kindOf(String entry) {

		String said = entry.trim();

		int upTo = said.length();

		for (String label : new String[]{"Amount:", "Currency:", "Created Date:"}) {

			int at = said.indexOf(label);

			if (at >= 0 && at < upTo) {
				upTo = at;
			}
		}

		return said.substring(0, upTo).trim();
	}

	/** How much one notification says moved, with the wallet's symbol as the page writes it. */
	public String amountOf(String entry) {

		return valueAfter(entry, "Amount:");
	}

	/** Which wallet one notification is about. */
	public String currencyOf(String entry) {

		return valueAfter(entry, "Currency:");
	}

	/** When one notification happened. */
	public String dateOf(String entry) {

		Matcher when = WHEN.matcher(entry);

		return when.find() ? when.group().trim() : "";
	}

	/** The symbol an amount was written with - "RM", "US$", nothing at all. */
	public String symbolOf(String amount) {

		Matcher figure = MONEY.matcher(amount.trim());

		return figure.matches() ? figure.group(1).trim() : "";
	}

	/** Whether an amount was written as a figure at all. */
	public boolean isAFigure(String amount) {

		return MONEY.matcher(amount.trim()).matches();
	}

	/** Whatever a card says after one of its labels. */
	private String valueAfter(String entry, String label) {

		int at = entry.indexOf(label);

		if (at < 0) {
			return "";
		}

		String rest = entry.substring(at + label.length()).trim();

		for (String next : new String[]{"Amount:", "Currency:", "Created Date:"}) {

			int upTo = rest.indexOf(next);

			if (upTo >= 0) {
				rest = rest.substring(0, upTo);
			}
		}

		return rest.trim();
	}

	/**
	 * How many are still unread, as the bell itself counts them.
	 *
	 * Read off the bar rather than counted from the list, because the badge is the platform's own
	 * answer and the list is only evidence for it. A bell with no badge is not an error - it means
	 * nothing is waiting - so that is answered as zero.
	 */
	public int unreadOnTheBell() {

		for (Locator control : Wait.all(topBarButtons)) {

			try {
				Matcher badge = BADGE.matcher(control.innerText().trim());

				if (badge.matches()) {
					return Integer.parseInt(badge.group(1));
				}
			} catch (Exception gone) {
				// A control that will not answer is not carrying a badge worth reading.
			}
		}

		return 0;
	}

	/** How many of the cards on screen are still marked unread. */
	public int unreadInTheList() {

		int unread = 0;

		for (Locator dot : Wait.all(page.locator(UNREAD_DOT))) {

			try {
				if (dot.isVisible()) {
					unread++;
				}
			} catch (Exception gone) {
				// A dot that will not answer is not evidence of anything.
			}
		}

		return unread;
	}

	/**
	 * Scrolls to the end of what is listed, bringing in whatever comes next.
	 *
	 * The list grows rather than pages, so reaching the bottom is what asks for more. Twice,
	 * because the first stretch of new cards moves the bottom further down.
	 */
	public void scrollToTheEnd() {

		scrollThrough(2);
	}

	/**
	 * Scrolls on through the list, however far is asked for.
	 *
	 * Twenty more cards arrive with each pass. The count of what is unread does not move with
	 * them: watched pass by pass it held at 469 for three passes and then fell away - 467, 461,
	 * 455, down to 413 by the twelfth - so the platform marks what has been shown some way
	 * behind the showing of it. A scenario that scrolls twice and expects the count to have
	 * moved is asking too early, which is what two of these scenarios did at first.
	 */
	public void scrollThrough(int passes) {

		for (int pass = 0; pass < passes; pass++) {

			page.mouse().wheel(0, 20000);

			Wait.sleep(2500);
		}
	}

	/**
	 * Waits for the bell to carry its count.
	 *
	 * The bar is drawn before the count is fetched into it, so a count read the moment the bar
	 * appears can be a count of nothing - which would quietly pass a scenario asking for a drop,
	 * since anything is a drop from nothing. An account with nothing unread waits out the whole
	 * time and answers zero honestly.
	 */
	public boolean waitForTheCount(int timeoutSeconds) {

		return Wait.until(() -> unreadOnTheBell() > 0, timeoutSeconds);
	}

	/** Everything the open tab says, for a tab whose contents are not cards. */
	public String text() {

		try {
			return page.locator("xpath=//*[@id='root']").first()
					.innerText().replaceAll("\\s+", " ").trim();
		} catch (Exception unreadable) {
			return "";
		}
	}

	/** Whether the open tab is saying it has nothing to show. */
	public boolean saysItHasNone() {

		String said = text();

		for (String word : new String[]{"No data", "No notification", "No message", "No records"}) {
			if (said.contains(word)) {
				return true;
			}
		}

		return false;
	}
}
