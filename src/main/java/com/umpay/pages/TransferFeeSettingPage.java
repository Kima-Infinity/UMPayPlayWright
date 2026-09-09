package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.umpay.utility.Wait;

import java.util.ArrayList;
import java.util.List;

/**
 * Who pays the fee on a transfer, at /settings/transfer-fee.
 *
 * Reached from the profile drawer. One setting for the whole account, made of three ways it can
 * be settled and a Save that is only offered once one of them has been changed:
 *
 *   Fee will be selected by User          the sender is asked, transfer by transfer
 *   Fee will always be paid by other party
 *   Fee will always be paid by me
 *
 * The page writes each of these as a label around a radio. The radio carries the word the
 * application knows the setting by - user, sender, receiver - and the label carries the words the
 * account holder reads, so both are available: the words for a message somebody has to
 * understand, the value for saying plainly which setting the account is on.
 *
 * WHAT THIS COSTS TO TEST. This is the only page in the suite whose setting the transfer flows
 * read: put the account on "always paid by me" and the transfer form stops asking who pays. So
 * every scenario that saves a change here puts the setting back the way it found it before it
 * ends, and the page object is built to make that possible - what was chosen can be read before
 * anything is touched, and chosen again afterwards.
 */
public class TransferFeeSettingPage {

	/** Where the setting lives. */
	private static final String ROUTE = "/settings/transfer-fee";

	/**
	 * One way of settling the fee: the row the page draws its radio and its words in.
	 *
	 * The label is the radio's neighbour rather than its parent - the page writes
	 * {@code <input id="sender"><label for="sender">} side by side inside a row of their own - so
	 * the row is what holds a way of settling the fee whole, and the row is what has to be read
	 * to say which way is which.
	 */
	private static final String WAYS = "xpath=//div[input[@type='radio']]";

	/** The radio inside one of those labels. */
	private static final String RADIO = "input[type='radio']";

	private final Page page;

	public TransferFeeSettingPage(Page ldriver) {

		this.page = ldriver;
	}

	/** True once the setting and its ways are on the screen. */
	public boolean isShowing() {

		try {
			return page.url().contains(ROUTE) && page.locator(WAYS).count() > 0;
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
	 * Opens the setting.
	 *
	 * By the drawer first, because that is the way somebody would reach it, and by the route only
	 * if the drawer would not - the same order the rest of the suite opens a page in.
	 */
	public void open() {

		if (isShowing()) {
			return;
		}

		waitUntilSignedIn(30);

		try {
			ProfileDrawerPage drawer = new ProfileDrawerPage(page);

			drawer.open();
			drawer.open("Transfer Fee Setting");

			if (Wait.until(this::isShowing, 10)) {
				return;
			}
		} catch (Exception theDrawerWouldNot) {
			System.out.println("The profile drawer did not lead to the transfer fee setting: "
					+ theDrawerWouldNot.getMessage());
		}

		openByItsAddress();

		if (!Wait.until(this::isShowing, 20)) {
			throw new IllegalStateException("Neither the profile drawer nor " + ROUTE + " opened"
					+ " the transfer fee setting. The page is at " + page.url());
		}
	}

	/**
	 * Opens the setting again from nothing, which is how a saved setting is proved to have been
	 * saved rather than merely shown.
	 */
	public void openByItsAddress() {

		page.navigate(page.url().replaceAll("(https?://[^/]+).*", "$1") + ROUTE);

		Wait.sleep(3000);

		Wait.until(this::isShowing, 20);
	}

	/** Every way the fee can be settled, in the words the page offers them in. */
	public List<String> waysOffered() {

		List<String> ways = new ArrayList<>();

		for (Locator way : Wait.all(page.locator(WAYS))) {

			try {
				String said = way.innerText().replaceAll("\\s+", " ").trim();

				if (!said.isEmpty()) {
					ways.add(said);
				}
			} catch (Exception reRendered) {
				// A label that redrew from under us is not one to report.
			}
		}

		return ways;
	}

	/** The way the account is on, in the words the page offers it in, or "" if none is. */
	public String wayChosen() {

		for (Locator way : Wait.all(page.locator(WAYS))) {

			try {
				if (way.locator(RADIO).first().isChecked()) {
					return way.innerText().replaceAll("\\s+", " ").trim();
				}
			} catch (Exception reRendered) {
				// A label that redrew from under us cannot be the answer.
			}
		}

		return "";
	}

	/**
	 * The word the application knows the chosen setting by - user, sender or receiver.
	 *
	 * Worth having beside the label: the words on the screen say who pays in the reader's terms
	 * and the value says which of the three the account is actually on, which is the thing a
	 * message about a setting that did not save has to name.
	 */
	public String valueChosen() {

		for (Locator way : Wait.all(page.locator(WAYS))) {

			try {
				Locator radio = way.locator(RADIO).first();

				if (radio.isChecked()) {
					return String.valueOf(radio.getAttribute("value"));
				}
			} catch (Exception reRendered) {
				// As above.
			}
		}

		return "";
	}

	/** How many of the ways are chosen. One, on a page that is behaving. */
	public int waysChosen() {

		int chosen = 0;

		for (Locator way : Wait.all(page.locator(WAYS))) {

			try {
				if (way.locator(RADIO).first().isChecked()) {
					chosen++;
				}
			} catch (Exception reRendered) {
				// As above.
			}
		}

		return chosen;
	}

	/**
	 * Chooses the way whose label says {@code named}.
	 *
	 * Checking the radio is tried first because it says what is meant; the row is clicked as a
	 * fallback, which is what somebody pressing the words beside it would do.
	 */
	public void choose(String named) {

		for (Locator way : Wait.all(page.locator(WAYS))) {

			String said;

			try {
				said = way.innerText().replaceAll("\\s+", " ").trim();
			} catch (Exception reRendered) {
				continue;
			}

			if (!said.equalsIgnoreCase(named)) {
				continue;
			}

			try {
				way.locator(RADIO).first().check();
			} catch (Exception styledAway) {

				try {
					way.click();
				} catch (Exception intercepted) {
					way.dispatchEvent("click");
				}
			}

			Wait.sleep(1500);
			return;
		}

		throw new IllegalStateException("No way of settling the fee says \"" + named + "\". The"
				+ " page offers: " + waysOffered());
	}

	/**
	 * True while the page offers to save.
	 *
	 * Save is there from the start but refuses to be pressed until something has been changed,
	 * so what matters is whether it can be pressed rather than whether it exists.
	 */
	public boolean offersToSave() {

		try {
			Locator save = saveButton();

			return save.count() > 0 && save.first().isEnabled();
		} catch (Exception notThere) {
			return false;
		}
	}

	/** Saves what has been chosen. */
	public void save() {

		Locator save = saveButton().first();

		try {
			save.click();
		} catch (Exception intercepted) {
			save.dispatchEvent("click");
		}

		Wait.sleep(4000);
	}

	private Locator saveButton() {

		return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save"));
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

	/**
	 * What the platform said, if it answered with one of its own dialogs.
	 *
	 * A setting that will not save is the kind of thing the application answers rather than
	 * simply ignores, and a step that fails saying only what is chosen would hide it.
	 */
	public String refusalShowing() {

		return com.umpay.utility.PlatformRefusal.showing(page);
	}

	/**
	 * Leaves the page for the home screen, which is how a change nobody saved is abandoned.
	 *
	 * A reload would do the same to the page, but leaving is what somebody actually does, and it
	 * is leaving rather than reloading that the setting has to survive.
	 */
	public void leaveWithoutSaving() {

		page.navigate(page.url().replaceAll("(https?://[^/]+).*", "$1") + "/");

		Wait.sleep(3000);
	}
}
