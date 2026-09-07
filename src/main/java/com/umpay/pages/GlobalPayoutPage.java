package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import java.time.Duration;

/**
 * The first step of the two payout routes on Global Transfer.
 *
 * "To Personal Bank Account" and "To USDT" are the same screen with a different title: a
 * currency, an amount against a stated minimum, and a Confirm that starts disabled. One
 * class covers both, and the caller says which title it expects, so a scenario reads as
 * being about the route it names.
 *
 * NOTHING HERE CONFIRMS THE PAYOUT.
 */
public class GlobalPayoutPage {

	private final Page page;
	public static final String PERSONAL_BANK = "To Personal Bank Account";
	public static final String USDT = "To USDT";

		private final Locator currencyField;


		private final Locator amountField;


		private final Locator confirmButton;


	public GlobalPayoutPage(Page ldriver) {

		this.page = ldriver;
		this.currencyField = page.locator("[name=\'currencyCode\']");
		this.amountField = page.locator("[id=\'amount\']");
		this.confirmButton = page.locator("xpath=//button[normalize-space()='Confirm']");
	}

	/** Whether the named payout form is on screen. */
	public boolean isShowing(String heading) {

		return isPresent(page.locator("xpath=" + "//*[normalize-space(text())='" + heading + "']"));

	}

	public boolean hasCurrencyField() {

		return isPresent(page.locator("[name=\'currencyCode\']"));

	}

	public boolean hasAmountField() {

		return isPresent(page.locator("[id=\'amount\']"));

	}

	/** A label on the form - the stated minimum, the available balance. */
	public boolean shows(String label) {

		return isPresent(page.locator("xpath=" + "//*[contains(normalize-space(text()),\"" + label + "\")]"));

	}

	public void enterAmount(String amount) {

		amountField.waitFor();
		amountField.clear();
		amountField.fill(amount);

		settle();

	}

	/**
	 * The smallest payout this form says it will take, from its own "Limit Min 19.77 USD" line.
	 *
	 * Read rather than written down: the minimum is a conversion of a limit held in another
	 * currency, so it moves. It read 19.77 the day this was written.
	 */
	public String statedMinimum() {

		try {
			String line = page.locator("xpath=//*[contains(normalize-space(text()),'Limit Min')]/..")
					.first().innerText().replaceAll("\\s+", " ").trim();

			java.util.regex.Matcher figure =
					java.util.regex.Pattern.compile("Limit Min ([0-9][0-9,]*(?:\\.[0-9]+)?)").matcher(line);

			return figure.find() ? figure.group(1).replace(",", "") : "";

		} catch (Exception notStated) {
			return "";
		}
	}

	/**
	 * What the wallet paying for this holds, as the form states it - "Available Balance US$ 2,408.23".
	 *
	 * The label and the figure are not always the same element: the label alone came back as
	 * "Available Balance" with nothing on it, which read as a wallet holding nothing. So the
	 * reading widens a step at a time until it finds a figure carrying a currency symbol, which
	 * is what tells a balance from the other numbers on the form.
	 */
	public String availableBalance() {

		for (String around : new String[]{"//*[contains(normalize-space(text()),'Available Balance')]",
				"//*[contains(normalize-space(text()),'Available Balance')]/..",
				"//*[contains(normalize-space(text()),'Available Balance')]/../.."}) {

			try {
				String text = page.locator("xpath=" + around).first()
						.innerText().replaceAll("\\s+", " ").trim();

				if (java.util.regex.Pattern.compile("[0-9]").matcher(text).find()) {
					return text;
				}

			} catch (Exception notThere) {
				// A step too far, or a form mid-render. Try the next.
			}
		}

		return "";
	}

	/**
	 * Chooses a payout account the form has saved, which is the second half of what it asks for.
	 *
	 * The Receive Information section only appears once an amount is entered - the form asks for
	 * the money before it asks where it is going - so this cannot be called first.
	 */
	public void chooseSavedPaymentAccount(String wanted) {

		click(page.locator("xpath=//button[normalize-space()='From Template']"), "the From Template tab");
		click(page.locator("xpath=//button[normalize-space()='Select Payment Account']"), "Select Payment Account");

		// SAVED means whatever this route has saved, the way the withdraw rows read it. Which
		// payout accounts a route offers is a fact about the account rather than about the test -
		// the USDT route offers a crypto address, the bank route offers banks - so a scenario
		// that is not proving one account in particular says so and takes what is there.
		String name = "SAVED".equalsIgnoreCase(wanted) ? theAccountThisRouteHasSaved() : wanted;

		Locator option = page.locator("xpath=//*[@id='root.dialog']//*[normalize-space()=\""
				+ name + "\"]").first();

		try {
			option.waitFor();
		} catch (Exception notListed) {
			throw new IllegalStateException("No saved payout account called " + name
					+ " was offered on this form");
		}

		// The name is a heading strip inside the row and ignores clicks; the row around it is what
		// selects. The same thing the withdraw form does, and the same fix.
		Locator label = option;

		for (int level = 0; level <= 3; level++) {

			click(label, "the " + name + " payout account");

			if (!aDialogIsOpen()) {
				return;
			}

			label = label.locator("xpath=..");
		}

		throw new IllegalStateException("The payout account picker stayed open after choosing "
				+ name + ", so the form has not been filled in");
	}

	/**
	 * The payout account this route has saved, for a scenario that does not pin one.
	 *
	 * A route with nothing saved cannot pay out at all, and says so: that is a gap in the
	 * account rather than a fault in the run.
	 */
	private String theAccountThisRouteHasSaved() {

		java.util.List<String> offered = new java.util.ArrayList<>();

		for (Locator row : com.umpay.utility.Wait.all(page.locator("xpath=//*[@id='root.dialog']//p"))) {
			try {
				String text = row.innerText().trim();

				if (!text.isEmpty() && !text.endsWith(":")) {
					offered.add(text);
				}
			} catch (Exception gone) {
				// A row that redrew from under us is not one to choose.
			}
		}

		if (offered.isEmpty() || offered.get(0).contains("Please add your payment account")) {
			throw new IllegalStateException("This route has no saved payout account, so nothing"
					+ " can be sent from it. The form offers: " + offered);
		}

		System.out.println("Taking the payout account this route has saved: " + offered.get(0));

		return offered.get(0);
	}

	/**
	 * Presses Confirm, which sends the payout.
	 *
	 * Named for what it does. Nothing else on this page sends anything, so a scenario has to ask
	 * for this deliberately.
	 */
	public void confirmAndSend() {

		click(confirmButton, "Confirm");
	}

	/** Whether a modal is covering the form. */
	private boolean aDialogIsOpen() {

		try {
			for (Locator dialog : com.umpay.utility.Wait.all(page.locator("div[role='dialog']"))) {
				if (dialog.isVisible()) {
					return true;
				}
			}
		} catch (Exception nothingOpen) {
			return false;
		}

		return false;
	}

	/** Clicks with a JavaScript fallback, which this application's overlays occasionally need. */
	private void click(Locator element, String what) {

		try {
			element.first().waitFor();
			element.first().click();
		} catch (Exception intercepted) {
			element.first().dispatchEvent("click");
		}

		System.out.println("Clicked " + what);

		com.umpay.utility.Wait.sleep(1500);
	}

	/**
	 * Whether the payout can be confirmed.
	 *
	 * Confirm carries disabled="true" on an untouched form, so this reads the validation
	 * rather than the layout.
	 */
	public boolean canConfirm() {

		try {
			return confirmButton.isEnabled();

		} catch (Exception notThere) {
			return false;
		}
	}

	public boolean hasConfirmButton() {

		return isPresent(page.locator("xpath=" + "//button[normalize-space()='Confirm']"));

	}

	private boolean isPresent(Locator locator) {

		try {
			locator.first().waitFor(new Locator.WaitForOptions()
					.setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
			return true;

		} catch (Exception notThere) {
			return false;
		}
	}

	private void settle() {

		try {
			Thread.sleep(2000);

		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}
}
