package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.umpay.utility.Wait;

import java.util.ArrayList;
import java.util.List;

/**
 * The account's settings, at /settings.
 *
 * Reached from the profile drawer. Four things, each drawn as a row that carries where it goes in
 * a {@code path} attribute of its own - the same shape the security page uses:
 *
 *   Auto Logout     /settings/logout         whether to be logged out when idle, after how many
 *                                            minutes, and whether an order in flight stops it
 *   About UMPay     /settings/about          the terms, and where UMPay can be followed
 *   Email           /settings/email          a new address, verified by a code sent to it
 *   Phone Number    /settings/phone-number   a new number, verified the same way
 *
 * WHAT THIS PAGE COSTS TO TEST. The email is what the whole suite signs in with and reads its
 * mail at, the phone number is what it verifies with, and an auto logout of a few minutes would
 * throw a run out in the middle of a transfer. So this page object fills the forms and presses
 * things, but nothing here saves an auto logout and nothing here submits an address or a number
 * the application could accept.
 */
public class SettingsPage {

	/** Where the settings are listed. */
	private static final String ROUTE = "/settings";

	/** One setting: the page hands each row the address it opens. */
	private static final String ROWS = "a[path^='/settings']";

	/** Where being logged out when idle is set. */
	private static final String AUTO_LOGOUT = "/settings/logout";

	/** Where the terms and the rest of it are. */
	private static final String ABOUT = "/settings/about";

	/** Where the address is changed. */
	private static final String EMAIL = "/settings/email";

	/** Where the number is changed. */
	private static final String PHONE = "/settings/phone-number";

	/** How long the account may sit idle, kept in a box the page does not show. */
	private static final String MINUTES = "input[name='auto_logout_in_minute']";

	/** Whether being logged out when idle is on at all. */
	private static final String ENABLED = "input[name='auto_logout_status']";

	/** Whether an order in flight is left alone. */
	private static final String WHILE_ORDERING = "input[name='auto_logout_if_active_order']";

	private final Page page;

	public SettingsPage(Page ldriver) {

		this.page = ldriver;
	}

	/** True once the settings are listed. */
	public boolean isShowing() {

		try {
			return page.url().endsWith(ROUTE) && page.locator(ROWS).count() > 0;
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
	 * Opens the settings.
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
			drawer.open("Settings");

			if (Wait.until(this::isShowing, 10)) {
				return;
			}
		} catch (Exception theDrawerWouldNot) {
			System.out.println("The profile drawer did not lead to the settings: "
					+ theDrawerWouldNot.getMessage());
		}

		openByItsAddress(ROUTE);

		if (!Wait.until(this::isShowing, 20)) {
			throw new IllegalStateException("Neither the profile drawer nor " + ROUTE + " opened"
					+ " the settings. The page is at " + page.url());
		}
	}

	/** Opens one of the settings pages by its address. */
	public void openByItsAddress(String route) {

		page.navigate(page.url().replaceAll("(https?://[^/]+).*", "$1") + route);

		Wait.sleep(3500);
	}

	/** Goes back to the four of them. */
	public void openTheList() {

		openByItsAddress(ROUTE);

		Wait.until(this::isShowing, 20);
	}

	private List<Locator> rows() {

		return Wait.all(page.locator(ROWS));
	}

	/** Everything the settings offer, in the words they are named in. */
	public List<String> thingsOffered() {

		List<String> offered = new ArrayList<>();

		for (Locator row : rows()) {

			try {
				offered.add(row.innerText().replaceAll("\\s+", " ").trim());
			} catch (Exception reRendered) {
				// A row that redrew from under us is not one to report.
			}
		}

		return offered;
	}

	/** Opens the row named {@code named}. */
	public void openThe(String named) {

		for (Locator row : rows()) {

			String said;

			try {
				said = row.innerText().replaceAll("\\s+", " ").trim();
			} catch (Exception reRendered) {
				continue;
			}

			if (!said.equalsIgnoreCase(named)) {
				continue;
			}

			try {
				row.click();
			} catch (Exception intercepted) {
				row.dispatchEvent("click");
			}

			Wait.sleep(3500);
			return;
		}

		throw new IllegalStateException("The settings offer nothing called \"" + named + "\". They"
				+ " offer: " + thingsOffered());
	}

	// ------------------------------------------------------------------
	// Being logged out when idle
	// ------------------------------------------------------------------

	/** True once the auto logout is open. */
	public boolean isShowingAutoLogout() {

		return page.url().contains(AUTO_LOGOUT) && page.locator(MINUTES).count() > 0;
	}

	/** How many minutes of sitting still the account is allowed. */
	public String minutesAllowed() {

		try {
			return String.valueOf(page.locator(MINUTES).first().inputValue());
		} catch (Exception notThere) {
			return "";
		}
	}

	/** True while being logged out when idle is switched on. */
	public boolean autoLogoutIsOn() {

		try {
			return page.locator(ENABLED).first().isChecked();
		} catch (Exception notThere) {
			return false;
		}
	}

	/** True while an order in flight is left alone by the auto logout. */
	public boolean leavesAnOrderAlone() {

		try {
			return page.locator(WHILE_ORDERING).first().isChecked();
		} catch (Exception notThere) {
			return false;
		}
	}

	/**
	 * Presses the + or the - beside the minutes.
	 *
	 * Asked for exactly. A button named "-" is otherwise matched by anything whose name merely
	 * contains one - four of them on this page - and pressing the first of those walks off the
	 * page altogether, taking the minutes with it.
	 */
	public void step(String which) {

		Locator stepper = page.getByRole(AriaRole.BUTTON,
				new Page.GetByRoleOptions().setName(which).setExact(true));

		if (stepper.count() == 0) {
			throw new IllegalStateException("The auto logout offers no \"" + which + "\" to press");
		}

		try {
			stepper.first().click();
		} catch (Exception intercepted) {
			stepper.first().dispatchEvent("click");
		}

		Wait.sleep(1500);
	}

	/** True while the page offers to keep what has been changed. */
	public boolean offersToSave() {

		try {
			return page.getByRole(AriaRole.BUTTON,
					new Page.GetByRoleOptions().setName("Save")).count() > 0;
		} catch (Exception notThere) {
			return false;
		}
	}

	/** True while the page offers to put its settings back. */
	public boolean offersToReset() {

		try {
			return page.getByRole(AriaRole.BUTTON,
					new Page.GetByRoleOptions().setName("Reset")).count() > 0;
		} catch (Exception notThere) {
			return false;
		}
	}

	// ------------------------------------------------------------------
	// What UMPay says about itself
	// ------------------------------------------------------------------

	/** True once the about page is open. */
	public boolean isShowingAbout() {

		return page.url().contains(ABOUT);
	}

	// ------------------------------------------------------------------
	// The address and the number
	// ------------------------------------------------------------------

	/** True once the form for a new address is open. */
	public boolean isShowingTheEmailForm() {

		return page.url().contains(EMAIL) && page.locator("input[name='email']").count() > 0;
	}

	/** True once the form for a new number is open. */
	public boolean isShowingThePhoneForm() {

		return page.url().contains(PHONE) && page.locator("input[name='phone']").count() > 0;
	}

	/**
	 * What the open form asks for, by the names it gives its boxes.
	 *
	 * The hidden ones are left out: the captcha's key and the code the account has not been sent
	 * yet are the form's own business, not something anybody is asked for.
	 */
	public List<String> fieldsAsked() {

		List<String> asked = new ArrayList<>();

		for (Locator field : Wait.all(page.locator("input[name], select[name]"))) {

			try {
				if ("hidden".equals(field.getAttribute("type"))) {
					continue;
				}

				asked.add(String.valueOf(field.getAttribute("name")));
			} catch (Exception reRendered) {
				// As above.
			}
		}

		return asked;
	}

	/** The country the number is to be read in, as the form starts on it. */
	public String countryChosen() {

		try {
			Locator country = page.locator("[name='phoneCountry']");

			if (country.count() == 0) {
				return "";
			}

			return String.valueOf(country.first().inputValue());
		} catch (Exception notThere) {
			return "";
		}
	}

	/** Types {@code value} into the box called {@code named}, or empties it. */
	public void typeInto(String named, String value) {

		Locator field = page.locator("input[name='" + named + "']");

		if (field.count() == 0) {
			throw new IllegalStateException("This form has no box called \"" + named + "\". It"
					+ " asks for: " + fieldsAsked());
		}

		field.first().fill(value);

		Wait.sleep(500);
	}

	/** Presses Next, which is what these two forms have in place of a Save. */
	public void next() {

		Locator next = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next"))
				.first();

		try {
			next.click();
		} catch (Exception intercepted) {
			next.dispatchEvent("click");
		}

		Wait.sleep(4000);
	}

	/**
	 * True once the application has said it wants a code, which is the step after these forms are
	 * accepted and the point at which a change would begin.
	 */
	public boolean isAskingForACode() {

		String said = text();

		return said.contains("Verification Code") || said.contains("verification code")
				|| said.contains("OTP");
	}

	// ------------------------------------------------------------------

	/** Where the run is. */
	public String getCurrentUrl() {

		return page.url();
	}

	/** Steps back the way the browser would. */
	public void goBack() {

		page.goBack();

		Wait.sleep(3000);
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
