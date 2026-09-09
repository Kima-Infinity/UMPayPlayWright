package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.umpay.utility.Wait;

import java.util.ArrayList;
import java.util.List;

/**
 * What UMPay charges, at /customer/fee.
 *
 * Reached from the profile drawer. A chooser of the currencies the platform deals in - BDT, BRL,
 * HKD, IDR, MXN, MYR, PHP, THB, USD, USDT, VND - and, in principle, what it costs to move money
 * in whichever is chosen.
 *
 * HOW A CURRENCY IS CHOSEN. By the button carrying its name. Each row also holds a radio, but the
 * radio is decorative: it is positioned behind the row with a negative z-index and carries neither
 * a name nor a value, so it can be neither clicked nor checked - Playwright answers "clicking the
 * checkbox did not change its state". The button is the control, and the button presses.
 *
 * WHAT THE PAGE DOES WITH IT is another matter, and the scenarios hold it to that rather than
 * assuming: the page asks the server for /api/agents/fees when it loads.
 *
 * Nothing here changes anything. A fee schedule is the platform's to set; this page only shows it.
 */
public class FeeListingPage {

	/** Where the fees are. */
	private static final String ROUTE = "/customer/fee";

	/** One currency on the chooser: the row its button and its decorative radio share. */
	private static final String ROWS = "div.text-left.relative.w-full";

	/** The currencies the platform deals in, as the page writes them. */
	private static final String CURRENCY = "^[A-Z]{3,4}$";

	private final Page page;

	public FeeListingPage(Page ldriver) {

		this.page = ldriver;
	}

	/** True once the fees are on the screen. */
	public boolean isShowing() {

		try {
			return page.url().contains(ROUTE) && text().contains("Fee Listing");
		} catch (Exception notThere) {
			return false;
		}
	}

	/** True once the run is past the login page and inside the application. */
	public boolean waitUntilSignedIn(int timeoutSeconds) {

		return Wait.until(() -> !page.url().contains("/login")
				&& page.locator("nav button").count() > 0, timeoutSeconds);
	}

	/**
	 * Opens the fees.
	 *
	 * By the drawer first, because that is the way somebody would reach them, and by the route
	 * only if the drawer would not - the same order the rest of the suite opens a page in.
	 */
	public void open() {

		if (isShowing()) {
			return;
		}

		waitUntilSignedIn(30);

		try {
			ProfileDrawerPage drawer = new ProfileDrawerPage(page);

			drawer.open();
			drawer.open("Fee Listing");

			if (Wait.until(this::isShowing, 10)) {
				return;
			}
		} catch (Exception theDrawerWouldNot) {
			System.out.println("The profile drawer did not lead to the fee listing: "
					+ theDrawerWouldNot.getMessage());
		}

		page.navigate(page.url().replaceAll("(https?://[^/]+).*", "$1") + ROUTE);

		Wait.sleep(4000);

		if (!Wait.until(this::isShowing, 20)) {
			throw new IllegalStateException("Neither the profile drawer nor " + ROUTE + " opened"
					+ " the fee listing. The page is at " + page.url());
		}
	}

	/** Every currency the fees can be asked for, as the page names them. */
	public List<String> currenciesOffered() {

		List<String> currencies = new ArrayList<>();

		for (Locator row : Wait.all(page.locator(ROWS))) {

			try {
				String named = row.innerText().replaceAll("\\s+", " ").trim();

				if (named.matches(CURRENCY)) {
					currencies.add(named);
				}
			} catch (Exception reRendered) {
				// A row that redrew from under us is not one to report.
			}
		}

		return currencies;
	}

	/** True while the page asks which currency the fees are wanted for. */
	public boolean asksForACurrency() {

		return text().contains("Currency");
	}

	/**
	 * Chooses the currency named {@code wanted}, by the button that carries its name.
	 *
	 * Pressed as a person would, and raised as an event only if that will not land - the same
	 * fallback the rest of this suite uses on controls that redraw underneath a click.
	 */
	public void choose(String wanted) {

		Locator button = page.getByRole(AriaRole.BUTTON,
				new Page.GetByRoleOptions().setName(wanted).setExact(true)).first();

		if (button.count() == 0) {
			throw new IllegalStateException("No currency here is called \"" + wanted + "\". The"
					+ " page offers: " + currenciesOffered());
		}

		try {
			button.click(new Locator.ClickOptions().setTimeout(8000));
		} catch (Exception wouldNotTakeIt) {

			try {
				button.dispatchEvent("click");
			} catch (Exception norThat) {
				throw new IllegalStateException("The currency \"" + wanted + "\" could not be"
						+ " pressed: " + norThat.getMessage());
			}
		}

		Wait.sleep(5000);
	}

	/**
	 * The currency the page marks as chosen, or "" while it marks none.
	 *
	 * Found by comparing the rows against each other rather than by looking for a class that means
	 * chosen: every currency button carries the same styling, so anything matched on one of them
	 * matches all eleven. A row is the chosen one only if it is drawn differently from the rest -
	 * and if they are all alike, nothing is chosen, which is itself the answer.
	 */
	public String currencyChosen() {

		try {
			Object marked = page.evaluate(
					"() => { const rows = Array.from(document.querySelectorAll("
					+ "'div.text-left.relative.w-full'));"
					+ " const drawn = rows.map(r => { const b = r.querySelector('button');"
					+ "   const radio = r.querySelector('input[type=radio]');"
					+ "   return ((b ? b.className : '') + '|' + (radio && radio.checked)); });"
					+ " const odd = drawn.filter((d, i) => drawn.indexOf(d) === drawn.lastIndexOf(d));"
					+ " if (odd.length !== 1) return '';"
					+ " const at = drawn.indexOf(odd[0]);"
					+ " return rows[at].innerText.replace(/\\s+/g, ' ').trim(); }");

			return String.valueOf(marked);
		} catch (Exception unreadable) {
			return "";
		}
	}

	/**
	 * What the page says a currency costs, beyond the names of the currencies themselves.
	 *
	 * A fee has to be a figure of some kind - a percentage or an amount - so what is looked for is
	 * anything on the page that reads like one, with the currency codes left out.
	 */
	public List<String> feesShown() {

		List<String> fees = new ArrayList<>();

		try {
			Object found = page.evaluate(
					"() => { const said = [];"
					+ " document.querySelectorAll('#root *').forEach(e => {"
					+ "   if (e.children.length !== 0) return;"
					+ "   const t = (e.innerText || '').replace(/\\s+/g, ' ').trim();"
					+ "   if (!t || t.length > 60) return;"
					+ "   if (/^[A-Z]{3,4}$/.test(t)) return;"
					+ "   if (/\\d/.test(t) && /%|fee|charge|min|max|rate/i.test(t)) said.push(t); });"
					+ " return said.slice(0, 20); }");

			if (found instanceof List) {

				for (Object one : (List<?>) found) {
					fees.add(String.valueOf(one));
				}
			}
		} catch (Exception unreadable) {
			// A page that would not answer shows no fees, as far as this can tell.
		}

		return fees;
	}

	/** Where the run is. */
	public String getCurrentUrl() {

		return page.url();
	}

	/** What the page says, on one line, for a message that has to name what was seen. */
	public String text() {

		try {
			return page.locator("xpath=//*[@id='root']").first()
					.innerText().replaceAll("\\s+", " ").trim();
		} catch (Exception unreadable) {
			return "";
		}
	}

	/** What the platform said, if it answered with one of its own dialogs. */
	public String refusalShowing() {

		return com.umpay.utility.PlatformRefusal.showing(page);
	}
}
