package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.umpay.utility.Wait;

import java.util.ArrayList;
import java.util.List;

/**
 * What this account has earned in commission, at /customer/commission.
 *
 * Reached from the profile drawer. A list of what the account has been credited for, each entry a
 * card naming where the commission came from - Transfer, Payment Gateway, Deposit, Withdraw -
 * whether it has been settled, its reference number, its amount and when it was made. Pressing one
 * opens a Commission View Detail with the same in full, and offers to download or share it.
 *
 * THE FILTERS. A wallet to narrow it to, a Type of All, Unsettled or Settled, and a start and end
 * date, applied by Confirm and cleared by Reset. A range with nothing in it answers "No data"
 * rather than an empty page, which is worth holding it to: an empty list that says nothing is
 * indistinguishable from one that failed to load.
 *
 * Nothing here changes anything. Commission is credited by the platform, and this page only shows
 * it - there is nothing on it to submit.
 */
public class CommissionListingPage {

	/** Where the commissions are. */
	private static final String ROUTE = "/customer/commission";

	/** One commission, by the card the page draws around it. */
	private static final String CARDS = "xpath=//div[contains(@class,'p-6')]"
			+ "[contains(@class,'shadow')][contains(@class,'rounded')]";

	/** Whether a commission has been settled, shown as a badge on its card. */
	private static final String STATE = "span.badge";

	/** The type of commission to show: All, Unsettled or Settled. */
	private static final String TYPE = "select[name='status']";

	/** The range the commissions have to fall in. */
	private static final String FROM = "input[name='startDate']";

	private static final String TO = "input[name='endDate']";

	/** Where the application mounts a dialog - the wallets, and a commission's own detail. */
	private static final String DIALOG = "#root\\.dialog";

	/** What the page says when the filters leave nothing to show. */
	private static final String NOTHING = "No data";

	private final Page page;

	public CommissionListingPage(Page ldriver) {

		this.page = ldriver;
	}

	/** True once the commissions are on the screen. */
	public boolean isShowing() {

		try {
			return page.url().contains(ROUTE) && page.locator(TYPE).count() > 0;
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
	 * Opens the commissions.
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
			drawer.open("Commission Listing");

			if (Wait.until(this::isShowing, 10)) {
				return;
			}
		} catch (Exception theDrawerWouldNot) {
			System.out.println("The profile drawer did not lead to the commission listing: "
					+ theDrawerWouldNot.getMessage());
		}

		page.navigate(page.url().replaceAll("(https?://[^/]+).*", "$1") + ROUTE);

		Wait.sleep(4000);

		if (!Wait.until(this::isShowing, 20)) {
			throw new IllegalStateException("Neither the profile drawer nor " + ROUTE + " opened"
					+ " the commission listing. The page is at " + page.url());
		}
	}

	private List<Locator> cards() {

		return Wait.all(page.locator(CARDS));
	}

	/** How many commissions are listed. */
	public int howManyListed() {

		return page.locator(CARDS).count();
	}

	/** Every commission listed, as its card reads. */
	public List<String> listed() {

		List<String> commissions = new ArrayList<>();

		for (Locator card : cards()) {

			try {
				commissions.add(card.innerText().replaceAll("\\s+", " ").trim());
			} catch (Exception reRendered) {
				// A card that redrew from under us is not one to report.
			}
		}

		return commissions;
	}

	/**
	 * What a commission says under {@code label} - "Reference No:", "Amount:", "Created Date:".
	 *
	 * The card writes each as a label and a value side by side, so the value is what follows the
	 * label on the same card.
	 */
	public String onTheCard(String commission, String label) {

		int at = commission.indexOf(label);

		if (at < 0) {
			return "";
		}

		String after = commission.substring(at + label.length()).trim();

		for (String next : new String[] {"Reference No:", "Amount:", "Created Date:"}) {

			int ends = after.indexOf(next);

			if (ends > 0) {
				after = after.substring(0, ends).trim();
			}
		}

		return after;
	}

	/** Where each commission came from - Transfer, Payment Gateway, Deposit, Withdraw. */
	public List<String> sourcesListed() {

		List<String> sources = new ArrayList<>();

		for (String commission : listed()) {

			// The card opens with where it came from, then says whether it is settled.
			int ends = commission.indexOf(" Reference No:");

			String opening = ends > 0 ? commission.substring(0, ends) : commission;

			for (String state : new String[] {"Unsettled", "Settled"}) {

				int badge = opening.lastIndexOf(state);

				if (badge > 0) {
					opening = opening.substring(0, badge).trim();
				}
			}

			sources.add(opening.trim());
		}

		return sources;
	}

	/** Whether each commission has been settled, as its badge says. */
	public List<String> statesListed() {

		List<String> states = new ArrayList<>();

		for (Locator badge : Wait.all(page.locator(STATE))) {

			try {
				states.add(badge.innerText().replaceAll("\\s+", " ").trim());
			} catch (Exception reRendered) {
				// As above.
			}
		}

		return states;
	}

	/** The types of commission that can be asked for, as the page names them. */
	public List<String> typesOffered() {

		List<String> types = new ArrayList<>();

		try {
			for (String option : page.locator(TYPE + " option").allInnerTexts()) {

				String named = option.replaceAll("\\s+", " ").trim();

				if (!named.isEmpty()) {
					types.add(named);
				}
			}
		} catch (Exception notThere) {
			// A page with no chooser offers no types.
		}

		return types;
	}

	/** Asks for one type of commission - All, Unsettled or Settled. */
	public void askFor(String type) {

		page.selectOption(TYPE, new com.microsoft.playwright.options.SelectOption()
				.setLabel(type));

		Wait.sleep(1500);
	}

	/** Narrows the commissions to those between two dates. */
	public void askForBetween(String from, String to) {

		page.locator(FROM).first().fill(from);
		page.locator(TO).first().fill(to);

		Wait.sleep(1500);
	}

	/** Applies whatever has been asked for. */
	public void confirm() {

		press("Confirm");
	}

	/** Clears the filters. */
	public void reset() {

		press("Reset");
	}

	/**
	 * Presses one of the two buttons under the filters.
	 *
	 * Pressed as a person would first, and raised as an event if that will not land. The two sit
	 * under a chooser that has just been used, and a click that arrives while the chooser is still
	 * closing waits out its whole timeout for a button that is right there.
	 */
	private void press(String named) {

		Locator button = page.getByRole(AriaRole.BUTTON,
				new Page.GetByRoleOptions().setName(named).setExact(true)).first();

		try {
			button.click(new Locator.ClickOptions().setTimeout(8000));
		} catch (Exception wouldNotTakeIt) {

			try {
				button.dispatchEvent("click");
			} catch (Exception norThat) {
				throw new IllegalStateException("The " + named + " under the filters could not be"
						+ " pressed: " + norThat.getMessage());
			}
		}

		Wait.sleep(5000);
	}

	/** What the filters hold now, as the page would submit them. */
	public String typeAskedFor() {

		try {
			return page.locator(TYPE).first().inputValue();
		} catch (Exception notThere) {
			return "";
		}
	}

	public String fromDateAskedFor() {

		try {
			return page.locator(FROM).first().inputValue();
		} catch (Exception notThere) {
			return "";
		}
	}

	public String toDateAskedFor() {

		try {
			return page.locator(TO).first().inputValue();
		} catch (Exception notThere) {
			return "";
		}
	}

	/** True while the page is saying there is nothing to show. */
	public boolean saysThereIsNothing() {

		return text().contains(NOTHING);
	}

	/** Opens the wallets the commissions can be narrowed to. */
	public void openTheWallets() {

		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Select wallet"))
				.first().click();

		Wait.sleep(3000);
	}

	/** The wallets offered to narrow the commissions to. */
	public List<String> walletsOffered() {

		List<String> wallets = new ArrayList<>();

		try {
			String said = page.locator(DIALOG).first().innerText().replaceAll("\\s+", " ");

			java.util.regex.Matcher wallet = java.util.regex.Pattern
					.compile("([A-Z]{3}) ([A-Z][a-z]+(?: [A-Z][a-z]+)*)")
					.matcher(said);

			while (wallet.find()) {

				String found = wallet.group(1) + " " + wallet.group(2);

				if (!wallets.contains(found)) {
					wallets.add(found);
				}
			}
		} catch (Exception unreadable) {
			// Nothing to say about a dialog that is not there.
		}

		return wallets;
	}

	/** True while a dialog is open over the listing. */
	public boolean isShowingADialog() {

		try {
			return page.locator(DIALOG).count() > 0
					&& !page.locator(DIALOG).first().innerText().trim().isEmpty();
		} catch (Exception notThere) {
			return false;
		}
	}

	/** What the open dialog says. */
	public String whatTheDialogSays() {

		try {
			return page.locator(DIALOG).first().innerText().replaceAll("\\s+", " ").trim();
		} catch (Exception notThere) {
			return "";
		}
	}

	/** Closes whatever dialog is open. */
	public void closeTheDialog() {

		Locator close = page.locator(DIALOG + " button");

		if (close.count() > 0) {

			try {
				close.first().click();
			} catch (Exception intercepted) {
				close.first().dispatchEvent("click");
			}
		}

		Wait.sleep(2500);
	}

	/** Opens the commission listed first. */
	public void openTheFirstCommission() {

		List<Locator> listed = cards();

		if (listed.isEmpty()) {
			throw new IllegalStateException("There are no commissions to open. The page is at "
					+ page.url());
		}

		try {
			listed.get(0).click();
		} catch (Exception intercepted) {
			listed.get(0).dispatchEvent("click");
		}

		Wait.sleep(3500);
	}

	/** What a commission's own detail offers to do with it. */
	public List<String> whatTheDetailOffers() {

		List<String> offered = new ArrayList<>();

		try {
			for (Locator button : Wait.all(page.locator(DIALOG + " button"))) {

				String said = button.innerText().replaceAll("\\s+", " ").trim();

				if (!said.isEmpty() && !said.equalsIgnoreCase("Close modal")) {
					offered.add(said);
				}
			}
		} catch (Exception unreadable) {
			// As above.
		}

		return offered;
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
