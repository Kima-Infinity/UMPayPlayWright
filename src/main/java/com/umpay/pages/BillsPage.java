package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.umpay.utility.Wait;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bills - the account's own ledger, at /v2/bill.
 *
 * Every movement the account has made, whatever made it: deposits, withdraws, conversions,
 * transfers, card top-ups and school fees, listed newest first and grouped by day. Above the
 * list sit the filters - a type, an amount range and a date range - with Reset and Confirm.
 *
 * An entry opens a detail of its own, carrying the transaction's number, what went in and what
 * came out, the rate where there was one, and the date. That detail is where Download and Share
 * live.
 *
 * Nothing here submits a transaction. The page only reads what other flows have already done,
 * which makes every scenario against it free to run as often as anybody likes.
 */
public class BillsPage {

	/**
	 * One entry as the list writes it - "Convert 2026-09-04 08:21:29 HK$4.44 HKD".
	 *
	 * The list has no table and no ids: entries are laid out as text, so the shape they repeat
	 * is what identifies one. A type, a timestamp, an amount with its symbol, and the currency
	 * code - an entry missing any of those is an entry displayed wrongly, which is worth
	 * failing for rather than working around.
	 */
	private static final Pattern ENTRY = Pattern.compile(
			"([A-Za-z][A-Za-z ]+?) (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}) (\\S*?[0-9][0-9,.]*) ([A-Z]{3})");

	/** A figure with the wallet's own symbol in front of it, for reading an amount. */
	private static final Pattern MONEY = Pattern.compile("[0-9][0-9,]*(?:\\.[0-9]+)?");

	private final Page page;

	public BillsPage(Page ldriver) {

		this.page = ldriver;
	}

	/** True once the ledger is on screen. */
	public boolean isShowing() {

		try {
			return page.url().contains("/bill")
					&& page.locator("[name='type']").count() > 0;
		} catch (Exception notThere) {
			return false;
		}
	}

	/**
	 * Waits for the ledger to finish arriving.
	 *
	 * The page is on screen before its contents are: the filters draw first, the kinds are
	 * fetched into the type list, and the transactions arrive after that. A ledger read the
	 * moment the address changes is an empty ledger, which reads as an account that has never
	 * done anything.
	 *
	 * Either outcome will do - transactions, or the page saying it has none - because both mean
	 * the fetching has finished.
	 */
	public boolean waitUntilLoaded(int timeoutSeconds) {

		Wait.until(() -> typesOffered().size() > 1, timeoutSeconds);

		return Wait.until(() -> !entries().isEmpty() || saysNoData(), timeoutSeconds);
	}

	/** The address, for a step that has to say where a click led. */
	public String getCurrentUrl() {

		return page.url();
	}

	/**
	 * The kinds of transaction the page will filter by, as it names them.
	 *
	 * Read rather than assumed: which kinds exist is the platform's business and it grows - a
	 * kind appearing or disappearing changes what the account can be asked about, and is worth
	 * noticing rather than discovering when a filter quietly returns nothing.
	 */
	public List<String> typesOffered() {

		List<String> offered = new ArrayList<>();

		for (Locator option : Wait.all(page.locator("[name='type'] option"))) {
			try {
				String said = option.innerText().trim();

				if (!said.isEmpty()) {
					offered.add(said);
				}
			} catch (Exception gone) {
				// An option that will not answer is not one to report.
			}
		}

		return offered;
	}

	/** Everything the ledger is listing, newest first, each as one line of text. */
	public List<String> entries() {

		List<String> listed = new ArrayList<>();

		Matcher entry = ENTRY.matcher(pageText());

		while (entry.find()) {
			listed.add(entry.group().trim());
		}

		return listed;
	}

	/** What one entry says it was - "Convert", "Deposit", "International School Fee". */
	public String typeOf(String entry) {

		Matcher parts = ENTRY.matcher(entry);

		return parts.find() ? parts.group(1).trim() : "";
	}

	/** When one entry happened. */
	public String dateOf(String entry) {

		Matcher parts = ENTRY.matcher(entry);

		return parts.find() ? parts.group(2).trim() : "";
	}

	/** How much one entry moved, as a number, without its currency symbol. */
	public java.math.BigDecimal amountOf(String entry) {

		Matcher parts = ENTRY.matcher(entry);

		if (!parts.find()) {
			return java.math.BigDecimal.ZERO;
		}

		Matcher figure = MONEY.matcher(parts.group(3));

		return figure.find()
				? new java.math.BigDecimal(figure.group().replace(",", ""))
				: java.math.BigDecimal.ZERO;
	}

	/** Whether the ledger is saying there is nothing to show. */
	public boolean saysNoData() {

		return pageText().contains("No data");
	}

	/** Narrows the list to one kind of transaction. */
	public void filterByType(String type) {

		page.locator("[name='type']").first().selectOption(new com.microsoft.playwright.options.SelectOption()
				.setLabel(type));

		Wait.sleep(1000);
	}

	/** Narrows the list to transactions of a certain size. */
	public void filterByAmount(String from, String to) {

		fill("from_amount", from);
		fill("to_amount", to);
	}

	/** Narrows the list to a stretch of days. */
	public void filterByDate(String from, String to) {

		fill("from_date", from);
		fill("to_date", to);
	}

	/** Applies whatever the filters have been set to. */
	public void confirm() {

		click("Confirm");

		Wait.sleep(1500);
		waitUntilLoaded(20);
	}

	/** Puts the filters back to how the page opened. */
	public void reset() {

		click("Reset");

		Wait.sleep(1500);
		waitUntilLoaded(20);
	}

	/**
	 * Opens one of the listed transactions, by its place in the list.
	 *
	 * By position rather than by what it says, because two movements of the same size on the
	 * same day read alike - and the timestamp, which does tell them apart, is the part most
	 * likely to be reformatted.
	 */
	public void openEntry(int position) {

		List<Locator> rows = Wait.all(page.locator(
				"xpath=//*[contains(normalize-space(text()),':')][contains(normalize-space(text()),'-')]"));

		List<String> listed = entries();

		if (listed.size() <= position) {
			throw new IllegalStateException("The ledger lists " + listed.size()
					+ " transactions, so there is no entry " + (position + 1));
		}

		String when = dateOf(listed.get(position));

		page.locator("xpath=//*[contains(normalize-space(text()),\"" + when + "\")]").first().click();

		Wait.until(this::isShowingADetail, 15);
	}

	/** True once a transaction's own detail is open. */
	public boolean isShowingADetail() {

		return detailText().contains("Number:");
	}

	/** Whatever the open detail says. */
	public String detailText() {

		try {
			for (Locator dialog : Wait.all(page.locator("div[role='dialog']"))) {
				if (dialog.isVisible()) {
					return dialog.innerText().replaceAll("\\s+", " ").trim();
				}
			}
		} catch (Exception notOpen) {
			return "";
		}

		return "";
	}

	/** Whether the detail carries a particular word or figure. */
	public boolean detailSays(String said) {

		return detailText().contains(said);
	}

	/** Closes the detail, so the next entry can be opened. */
	public void closeDetail() {

		try {
			page.locator("xpath=//button[normalize-space()='Close modal']").first().click();
			Wait.sleep(1500);
		} catch (Exception alreadyClosed) {
			// A detail that closed itself needs no help.
		}
	}

	/** Downloads the transaction's receipt and hands back the file. */
	public java.nio.file.Path downloadDetail(java.nio.file.Path saveTo) {

		com.microsoft.playwright.Download download = page.waitForDownload(() ->
				page.locator("xpath=//button[normalize-space()='Download']").first().click());

		java.nio.file.Path file = saveTo.resolve(download.suggestedFilename());

		download.saveAs(file);

		return file;
	}

	/** Opens the sharing choices for the transaction. */
	public void shareDetail() {

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

	/** Everything the page is saying, which is where the entries are read from. */
	private String pageText() {

		try {
			return page.locator("xpath=//*[@id='root']").first()
					.innerText().replaceAll("\\s+", " ").trim();
		} catch (Exception unreadable) {
			return "";
		}
	}

	private void fill(String name, String value) {

		if (value == null || value.isBlank()) {
			return;
		}

		page.locator("[name='" + name + "']").first().fill(value);

		Wait.sleep(500);
	}

	private void click(String label) {

		Locator button = page.locator("xpath=//button[normalize-space()='" + label + "']");

		try {
			button.first().click();
		} catch (Exception intercepted) {
			button.first().dispatchEvent("click");
		}
	}
}
