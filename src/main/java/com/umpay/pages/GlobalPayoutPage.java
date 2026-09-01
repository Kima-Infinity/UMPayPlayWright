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
