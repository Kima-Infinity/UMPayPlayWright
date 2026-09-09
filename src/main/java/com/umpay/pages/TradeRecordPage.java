package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.umpay.utility.Wait;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The trade record - every order this account has raised, at /v2/trade-record.
 *
 * Reached from the profile drawer. Where the Bills page lists money that moved and the
 * notification page lists what the platform said about it, this lists the orders themselves,
 * whatever became of them: paid, unpaid, cancelled, failed and completed all appear here.
 *
 * Three tabs, each with a card of its own shape:
 *
 *   Deposit/Withdraw         - kind, how it ended, Number, Currency, Amount, and where a
 *                              conversion happened, Received Amount and Exchange rate
 *   International Transfer   - Number, Currency, Total Amount, Fee, Amount, Received Currency,
 *                              Approximate Received and Created Date
 *   International School Fee - Order Number, Total Amount, Currency and Created Date
 *
 * The list grows as it is scrolled rather than paging - twenty cards at a time, out to 140 and
 * beyond - and there is no filter, no search and no date range anywhere on the page.
 *
 * A card is a link, unlike the notification page's: clicking one goes to that order's own page,
 * carrying the order's id and where it came from, so a reader can act on an order they find here.
 */
public class TradeRecordPage {

	/** Where the page lives, whichever way it was reached. */
	private static final String ROUTE = "/v2/trade-record";

	/** One order as the page draws it, by the shadow around each card. */
	private static final String CARDS = "xpath=//div[contains(@class,'shadow')]";

	/**
	 * What a card can be labelled with, longest first.
	 *
	 * Longest first because the short ones are inside the long ones: an order carrying "Total
	 * Amount:", "Received Amount:" and "Amount:" would otherwise have all three read as the
	 * amount, and the fee would be mistaken for the sum of the order.
	 */
	private static final String[] LABELS = {"Approximate Received:", "Received Currency:",
			"Received Amount:", "Order Number:", "Total Amount:", "Exchange rate:",
			"Created Date:", "Currency:", "Number:", "Amount:", "Fee:"};

	/** A figure at the front of what a label was given - "14.75 USD", "RM110", "?8.27". */
	private static final Pattern FIGURE = Pattern.compile("([0-9][0-9,]*(?:\\.[0-9]+)?)");

	/** The currency code written beside a figure - the USD in "14.75 USD". */
	private static final Pattern CURRENCY = Pattern.compile("\\b([A-Z]{3})\\b");

	private final Page page;

	private final Locator topBarButtons;

	public TradeRecordPage(Page ldriver) {

		this.page = ldriver;
		this.topBarButtons = page.locator("nav button");
	}

	/**
	 * Opens the trade record, by the drawer where it can and by the address otherwise.
	 *
	 * The drawer is how a reader gets here, so that is tried first and is what the scenarios are
	 * really about. The address is kept as a way in for the same reason the withdraw page keeps
	 * one: a scenario about what the page lists should not fail because a drawer would not open.
	 */
	public void open() {

		if (isShowing()) {
			return;
		}

		waitUntilSignedIn(30);

		try {
			ProfileDrawerPage drawer = new ProfileDrawerPage(page);

			drawer.open();
			drawer.open("Trade Record");

			if (Wait.until(this::isShowing, 10)) {
				waitUntilLoaded(20);
				return;
			}
		} catch (Exception theDrawerWouldNot) {
			System.out.println("The profile drawer did not lead to the trade record: "
					+ theDrawerWouldNot.getMessage());
		}

		page.navigate(page.url().replaceAll("(https?://[^/]+).*", "$1") + ROUTE);

		Wait.sleep(3000);

		if (!Wait.until(this::isShowing, 20)) {
			throw new IllegalStateException("Neither the profile drawer nor " + ROUTE + " opened"
					+ " the trade record. The page is at " + page.url());
		}

		waitUntilLoaded(20);
	}

	/**
	 * Waits for the platform to finish letting the account in.
	 *
	 * Signing in returns before the top bar is drawn, and asking for a page of the account's own
	 * orders in that moment is asking as a stranger - the platform answers by sending the run
	 * back to the login page.
	 */
	public boolean waitUntilSignedIn(int timeoutSeconds) {

		return Wait.until(() -> !page.url().contains("/login") && topBarButtons.count() > 0,
				timeoutSeconds);
	}

	/** True once the trade record is on screen. */
	public boolean isShowing() {

		try {
			return page.url().contains(ROUTE);
		} catch (Exception notThere) {
			return false;
		}
	}

	/** Waits for the orders to arrive, which they do after the page itself. */
	public boolean waitUntilLoaded(int timeoutSeconds) {

		Wait.until(() -> tabsOffered().size() > 1, timeoutSeconds);

		return Wait.until(() -> !entries().isEmpty(), timeoutSeconds);
	}

	/** The address, for a step that has to say where a click led. */
	public String getCurrentUrl() {

		return page.url();
	}

	/** Back to where the last click came from. */
	public void goBack() {

		page.goBack();

		Wait.sleep(3000);
	}

	/**
	 * The tabs the page offers, as it names them.
	 *
	 * Read rather than assumed: which kinds of order the page will show is what decides what an
	 * account can look up here, and one appearing or disappearing is worth noticing.
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

		Wait.sleep(3500);
	}

	/**
	 * Every order the open tab is listing, newest first, each as its card's own text.
	 *
	 * Kept as the page laid it out, a line at a time, because that is what tells a label from
	 * the value under it. A step that wants to show one in a failure message asks for it on one
	 * line instead.
	 */
	public List<String> entries() {

		List<String> listed = new ArrayList<>();

		for (Locator card : Wait.all(page.locator(CARDS))) {

			try {
				if (!card.isVisible()) {
					continue;
				}

				String said = card.innerText().trim();

				if (!said.isEmpty() && said.contains(":") && !listed.contains(said)) {
					listed.add(said);
				}
			} catch (Exception gone) {
				// A card that will not answer is not one to report.
			}
		}

		return listed;
	}

	/** One order written out on a single line, for a step that has to quote it. */
	public String oneLine(String entry) {

		return entry.replaceAll("\\s+", " ").trim();
	}

	/** What kind of order it is - "Deposit", "Withdraw", "Unionpay", "International School Fee". */
	public String kindOf(String entry) {

		List<String> lines = linesOf(entry);

		return lines.isEmpty() ? "" : lines.get(0);
	}

	/** How the order ended - "Unpaid", "Completed", "Cancelled", "failed". */
	public String statusOf(String entry) {

		List<String> lines = linesOf(entry);

		if (lines.size() < 2) {
			return "";
		}

		String said = lines.get(1);

		return said.endsWith(":") ? "" : said;
	}

	/** What a card says under one of its labels, or nothing if it does not carry that label. */
	public String fieldOf(String entry, String label) {

		return fieldsOf(entry).getOrDefault(label, "");
	}

	/** Whether a card carries a label at all. */
	public boolean carries(String entry, String label) {

		return fieldsOf(entry).containsKey(label);
	}

	/**
	 * Everything one card says, label by label.
	 *
	 * The page writes a label and its value as separate lines, and a value that runs to more
	 * than one line - a figure and the currency beside it - belongs to the label above it. So
	 * lines are gathered under the last label seen.
	 */
	public Map<String, String> fieldsOf(String entry) {

		Map<String, String> said = new LinkedHashMap<>();

		String label = null;

		for (String line : linesOf(entry)) {

			String matched = labelIn(line);

			if (matched != null) {
				label = matched;
				said.put(label, "");
				continue;
			}

			if (label != null) {
				said.put(label, (said.get(label) + " " + line).trim());
			}
		}

		return said;
	}

	/** The figure a value starts with, without its symbol or its currency. */
	public BigDecimal figureIn(String value) {

		Matcher figure = FIGURE.matcher(value.replace(",", ""));

		return figure.find() ? new BigDecimal(figure.group(1)) : null;
	}

	/**
	 * The currency a value was written in - "14.75 USD" is in USD, "RM110" names none.
	 *
	 * Needed because two figures on the same card are not always in the same money: a transfer
	 * states what it took in one currency and what will arrive in another, and adding those
	 * together would produce a number that means nothing.
	 */
	public String currencyIn(String value) {

		Matcher code = CURRENCY.matcher(value.trim());

		return code.find() ? code.group(1) : "";
	}

	/**
	 * Scrolls on through the list, twenty more orders at a time.
	 *
	 * The list grows rather than pages, so reaching the bottom is what asks for the next of them.
	 */
	public void scrollThrough(int passes) {

		for (int pass = 0; pass < passes; pass++) {

			page.mouse().wheel(0, 20000);

			Wait.sleep(2500);
		}
	}

	/** Opens one of the listed orders, by its place in the list. */
	public void openEntry(int position) {

		List<Locator> cards = Wait.all(page.locator(CARDS));

		if (cards.size() <= position) {
			throw new IllegalStateException("The trade record lists " + cards.size()
					+ " orders, so there is no order " + (position + 1));
		}

		String startedAt = page.url();

		cards.get(position).click();

		Wait.until(() -> !page.url().equals(startedAt), 15);
	}

	/**
	 * Whether the order offers its receipt to be kept.
	 *
	 * An order somebody cannot keep a copy of is one they cannot show anybody - a bank, an
	 * employer, whoever is asking where the money went. Bills and the commission listing both
	 * offer it and the trade record was never asked whether it does.
	 */
	public boolean orderOffersToBeKept() {

		// Waited for rather than read once. The order's own page says "LOADING Please wait while
		// loading resource" for a moment after it opens, and a reading taken then finds no
		// controls at all - which reads as an order that offers nothing rather than one that has
		// not finished arriving.
		Wait.until(() -> offers("Download") && offers("Share"), 20);

		return offers("Download") && offers("Share");
	}

	/** Downloads the order's receipt and hands back the file it produced. */
	public java.nio.file.Path downloadOrder(java.nio.file.Path saveTo) {

		com.microsoft.playwright.Download download = page.waitForDownload(() ->
				page.locator("xpath=//button[normalize-space()='Download']").first().click());

		java.nio.file.Path file = saveTo.resolve(download.suggestedFilename());

		download.saveAs(file);

		return file;
	}

	/** Opens the sharing choices for the order. */
	public void shareOrder() {

		page.locator("xpath=//button[normalize-space()='Share']").first().click();

		Wait.until(this::shareOffersACopy, 15);
	}

	/** Whether the sharing choices offer a way to copy it. */
	public boolean shareOffersACopy() {

		try {
			Locator copy = page.locator("xpath=//button[normalize-space()='Copy']");

			return copy.count() > 0 && copy.first().isVisible();
		} catch (Exception notOffered) {
			return false;
		}
	}

	/** Whether the page offers a control saying {@code named}. */
	private boolean offers(String named) {

		try {
			Locator control = page.locator("xpath=//button[normalize-space()='" + named + "']");

			return control.count() > 0 && control.first().isVisible();
		} catch (Exception notThere) {
			return false;
		}
	}

	/** Everything the page says, for a step that has to report what it found instead. */
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

		for (String word : new String[]{"No data", "No record", "No order"}) {
			if (said.contains(word)) {
				return true;
			}
		}

		return false;
	}

	/** One card's lines, emptied of the blank ones the layout leaves behind. */
	private List<String> linesOf(String entry) {

		List<String> lines = new ArrayList<>();

		for (String line : entry.split("\n")) {

			String said = line.trim();

			if (!said.isEmpty()) {
				lines.add(said);
			}
		}

		return lines;
	}

	/** Which label a line is, if it is one. Longest first, so "Amount:" cannot win over
	 * "Total Amount:". */
	private String labelIn(String line) {

		String said = line.trim();

		for (String label : LABELS) {
			if (said.equals(label)) {
				return label;
			}
		}

		return null;
	}
}
