package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.umpay.utility.Wait;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The wallets this account holds, at /v2/wallet, reached from the profile drawer.
 *
 * The page opens on a chart of everything the account holds, split by country, and below it a
 * card for each wallet - its currency, the country it belongs to, what is available in it and
 * what is blocked. One wallet is marked as the main one; every other offers to be made it.
 *
 * A card opens that wallet's own page at /v2/wallet/detail/record?currencyCode=HKD, which states
 * the wallet three ways - Total Balance, Available Balance and Locked - and holds two tabs:
 * Record, everything that has moved in this wallet, and Locked, what is being held against it.
 *
 * WHAT IS WORTH KNOWING ABOUT THE FIGURES
 *
 * Locked is written as a negative - "HK$ -5,544.32" - but the total counts it as money the
 * account still has: a wallet showing 8,369.51 available and -5,544.32 locked has a total of
 * 13,913.83. So the total is the available balance plus what is locked, taken as a size rather
 * than as a signed figure.
 *
 * The countries above the list are a chart's legend and nothing more. They look clickable, and
 * one of them even draws a pointer, but clicking changes neither the list nor the address - so
 * there is no filtering on this page, and nothing here pretends there is.
 */
public class WalletPage {

	/** Where the wallets are listed. */
	private static final String ROUTE = "/v2/wallet";

	/** Where one wallet is shown on its own. */
	private static final String DETAIL = "/v2/wallet/detail";

	/** One wallet, or on the wallet's own page one movement, by the shadow drawn around it. */
	private static final String CARDS = "xpath=//div[contains(@class,'shadow')]";

	/** The chart's legend, which names the countries the account holds money in. */
	private static final String LEGEND = "xpath=//li[contains(@class,'recharts-legend-item')]";

	/** A figure as the page writes it - "HK$8369.51", "Rp-30000", "HK$ 13,913.83". */
	private static final Pattern FIGURE = Pattern.compile("(-?[0-9][0-9,]*(?:\\.[0-9]+)?)");

	/** A currency code, which is what a wallet card leads with. */
	private static final Pattern CODE = Pattern.compile("^[A-Z]{3}$");

	private final Page page;

	private final Locator topBarButtons;

	public WalletPage(Page ldriver) {

		this.page = ldriver;
		this.topBarButtons = page.locator("nav button");
	}

	/**
	 * Opens the wallets, by the drawer where it can and by the address otherwise.
	 *
	 * The drawer is how a reader gets here, so it is tried first. The address is kept as a way in
	 * for the same reason the withdraw page keeps one: a scenario about what the wallets say
	 * should not fail because a drawer would not open.
	 */
	public void open() {

		if (isShowingTheList()) {
			return;
		}

		waitUntilSignedIn(30);

		try {
			ProfileDrawerPage drawer = new ProfileDrawerPage(page);

			drawer.open();
			drawer.open("Wallet");

			if (Wait.until(this::isShowingTheList, 10)) {
				waitUntilLoaded(20);
				return;
			}
		} catch (Exception theDrawerWouldNot) {
			System.out.println("The profile drawer did not lead to the wallets: "
					+ theDrawerWouldNot.getMessage());
		}

		page.navigate(page.url().replaceAll("(https?://[^/]+).*", "$1") + ROUTE);

		Wait.sleep(3000);

		if (!Wait.until(this::isShowingTheList, 20)) {
			throw new IllegalStateException("Neither the profile drawer nor " + ROUTE + " opened"
					+ " the wallets. The page is at " + page.url());
		}

		waitUntilLoaded(20);
	}

	/** Waits for the platform to finish letting the account in. */
	public boolean waitUntilSignedIn(int timeoutSeconds) {

		return Wait.until(() -> !page.url().contains("/login") && topBarButtons.count() > 0,
				timeoutSeconds);
	}

	/** True once the list of wallets is on screen, rather than one wallet's own page. */
	public boolean isShowingTheList() {

		try {
			return page.url().contains(ROUTE) && !page.url().contains(DETAIL);
		} catch (Exception notThere) {
			return false;
		}
	}

	/** True once one wallet's own page is on screen. */
	public boolean isShowingAWallet() {

		try {
			return page.url().contains(DETAIL);
		} catch (Exception notThere) {
			return false;
		}
	}

	/** Waits for the wallets to arrive, which they do after the page itself. */
	public boolean waitUntilLoaded(int timeoutSeconds) {

		return Wait.until(() -> !wallets().isEmpty(), timeoutSeconds);
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

	// ------------------------------------------------------------------
	// The list of wallets
	// ------------------------------------------------------------------

	/** Every wallet the account holds, each as its card's own text. */
	public List<String> wallets() {

		List<String> held = new ArrayList<>();

		for (Locator card : Wait.all(page.locator(CARDS))) {

			try {
				if (!card.isVisible()) {
					continue;
				}

				String said = card.innerText().trim();

				if (!said.isEmpty() && said.contains("Balance:") && !held.contains(said)) {
					held.add(said);
				}
			} catch (Exception gone) {
				// A card that will not answer is not one to report.
			}
		}

		return held;
	}

	/** One wallet written out on a single line, for a step that has to quote it. */
	public String oneLine(String wallet) {

		return wallet.replaceAll("\\s+", " ").trim();
	}

	/** Which currency a wallet is in, which is what its card leads with. */
	public String currencyOf(String wallet) {

		for (String line : linesOf(wallet)) {
			if (CODE.matcher(line).matches()) {
				return line;
			}
		}

		return "";
	}

	/** Which country a wallet belongs to - "Hong Kong", "Indonesia", "US Dollar". */
	public String countryOf(String wallet) {

		List<String> lines = linesOf(wallet);

		for (int line = 0; line < lines.size(); line++) {

			if (CODE.matcher(lines.get(line)).matches()) {

				// The code, then a divider the page draws, then the country.
				for (int next = line + 1; next < lines.size(); next++) {

					String said = lines.get(next);

					if (!said.equals("|") && !said.startsWith("Balance:")) {
						return said;
					}
				}
			}
		}

		return "";
	}

	/** What a wallet says under one of its labels, as written, symbol and all. */
	public String saidAfter(String wallet, String label) {

		for (String line : linesOf(wallet)) {
			if (line.startsWith(label)) {
				return line.substring(label.length()).trim();
			}
		}

		return "";
	}

	/** Whether a wallet carries a label at all. */
	public boolean carries(String wallet, String label) {

		for (String line : linesOf(wallet)) {
			if (line.startsWith(label)) {
				return true;
			}
		}

		return false;
	}

	/** Whether this is the wallet the account is calling its main one. */
	public boolean isMain(String wallet) {

		return wallet.contains("Main Wallet");
	}

	/** Whether this wallet offers to become the main one. */
	public boolean offersToBecomeMain(String wallet) {

		return wallet.contains("Set as Main");
	}

	/** Which currency the account is calling its main wallet. */
	public String mainWallet() {

		for (String wallet : wallets()) {
			if (isMain(wallet)) {
				return currencyOf(wallet);
			}
		}

		return "";
	}

	/** Makes one of the wallets the main one. */
	public void setAsMain(String currency) {

		page.locator("xpath=//div[contains(@class,'shadow')][.//text()[normalize-space()='"
				+ currency + "']]//button[normalize-space()='Set as Main']").first().click();

		Wait.sleep(4000);
	}

	/** The countries the chart names, which is where the account's money is. */
	public List<String> countriesCharted() {

		List<String> charted = new ArrayList<>();

		for (Locator item : Wait.all(page.locator(LEGEND))) {

			try {
				String said = item.innerText().replaceAll("\\s+", " ").trim();

				if (!said.isEmpty() && !charted.contains(said)) {
					charted.add(said);
				}
			} catch (Exception gone) {
				// A legend that will not answer is not one to report.
			}
		}

		return charted;
	}

	/** Opens one of the wallets. */
	public void openWallet(String currency) {

		String startedAt = page.url();

		page.locator("xpath=//div[contains(@class,'shadow')][.//text()[normalize-space()='"
				+ currency + "']]").first().click();

		Wait.until(() -> !page.url().equals(startedAt), 15);
		Wait.sleep(2000);
	}

	// ------------------------------------------------------------------
	// One wallet's own page
	// ------------------------------------------------------------------

	/** Which wallet is open, as its address names it. */
	public String walletShown() {

		Matcher named = Pattern.compile("currencyCode=([A-Z]{3})").matcher(page.url());

		return named.find() ? named.group(1) : "";
	}

	/**
	 * One of the three figures a wallet states itself in - Total Balance, Available Balance,
	 * Locked - as written.
	 *
	 * The page puts the figure on the line after its heading, with blank lines between, so the
	 * next line with anything on it is the answer. Taken at the first heading that matches,
	 * because "Locked" is also the name of a tab further down.
	 */
	public String statedBalance(String heading) {

		List<String> lines = linesOf(pageText());

		for (int line = 0; line < lines.size(); line++) {

			if (!lines.get(line).equals(heading)) {
				continue;
			}

			for (int next = line + 1; next < lines.size(); next++) {

				if (FIGURE.matcher(lines.get(next)).find()) {
					return lines.get(next).trim();
				}
			}
		}

		return "";
	}

	/** The figure inside what a wallet says, without its symbol, spaces or thousands marks. */
	public BigDecimal figureIn(String said) {

		Matcher figure = FIGURE.matcher(said.replace(",", "").replace(" ", ""));

		return figure.find() ? new BigDecimal(figure.group(1)) : null;
	}

	/** The tabs one wallet's page offers - "Record" and "Locked". */
	public List<String> tabsOffered() {

		List<String> offered = new ArrayList<>();

		for (String said : new String[]{"Record", "Locked"}) {

			Locator tab = page.locator("xpath=//button[normalize-space()='" + said + "']");

			try {
				if (tab.count() > 0 && tab.first().isVisible()) {
					offered.add(said);
				}
			} catch (Exception gone) {
				// Not offered, which is an answer in itself.
			}
		}

		return offered;
	}

	/** Moves to one of the tabs on a wallet's page. */
	public void chooseTab(String tab) {

		page.locator("xpath=//button[normalize-space()='" + tab + "']").first().click();

		Wait.sleep(3500);
	}

	/** Whether the wallet's page offers to make this the main wallet. */
	public boolean offersToSetAsMain() {

		Locator button = page.locator("xpath=//button[normalize-space()='Set as main']");

		try {
			return button.count() > 0 && button.first().isVisible();
		} catch (Exception notOffered) {
			return false;
		}
	}

	/**
	 * What the open tab lists, each movement as one line.
	 *
	 * A movement is a kind, a time and an amount - "Convert 2026-09-07 15:44:33 HK$-1" - and the
	 * time is what tells one from the next, so a card without one is not a movement.
	 */
	public List<String> movements() {

		List<String> listed = new ArrayList<>();

		for (Locator card : Wait.all(page.locator(CARDS))) {

			try {
				if (!card.isVisible()) {
					continue;
				}

				String said = card.innerText().replaceAll("\\s+", " ").trim();

				if (said.matches(".*\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.*")
						&& !listed.contains(said)) {

					listed.add(said);
				}
			} catch (Exception gone) {
				// A card that will not answer is not one to report.
			}
		}

		return listed;
	}

	/** When one movement happened. */
	public String dateOf(String movement) {

		Matcher when = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
				.matcher(movement);

		return when.find() ? when.group() : "";
	}

	/** What kind of movement it was - "Convert", "Withdraw", "International Transfer". */
	public String kindOf(String movement) {

		String when = dateOf(movement);

		return when.isEmpty() ? "" : movement.substring(0, movement.indexOf(when)).trim();
	}

	/** How much one movement was for, as written. */
	public String amountOf(String movement) {

		String when = dateOf(movement);

		return when.isEmpty() ? ""
				: movement.substring(movement.indexOf(when) + when.length()).trim();
	}

	/** Everything the page says, for a step that has to report what it found instead. */
	public String text() {

		return pageText().replaceAll("\\s+", " ").trim();
	}

	private String pageText() {

		try {
			return page.locator("xpath=//*[@id='root']").first().innerText();
		} catch (Exception unreadable) {
			return "";
		}
	}

	/** A card's or a page's lines, emptied of the blank ones the layout leaves behind. */
	private List<String> linesOf(String said) {

		List<String> lines = new ArrayList<>();

		for (String line : said.split("\n")) {

			String one = line.trim();

			if (!one.isEmpty()) {
				lines.add(one);
			}
		}

		return lines;
	}
}
