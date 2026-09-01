package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import java.time.Duration;

/**
 * The school fee transfer form at /school-fee.
 *
 * Reached from Global Transfer as "UMPay Transfer to School Fees", and also from the left
 * navigation as "International School Fees" - the same form under two names, which is why
 * a scenario naming either should land here.
 *
 * It is the simplest of the transfer forms: a source wallet, an amount whose placeholder
 * states the minimum, and a remark.
 *
 * NOTHING HERE SENDS THE TRANSFER.
 */
public class SchoolFeeTransferPage {

	private final Page page;
		private final Locator heading;


		private final Locator amountField;


		private final Locator remarkField;


		private final Locator sourceWalletButton;


		private final Locator nextButton;


	public SchoolFeeTransferPage(Page ldriver) {

		this.page = ldriver;
		this.heading = page.locator("xpath=//*[normalize-space(text())='International School Fee']");
		this.amountField = page.locator("[name=\'amount\']");
		this.remarkField = page.locator("[name=\'remark\']");
		this.sourceWalletButton = page.locator("xpath=//button[contains(.,'Available Balance')]");
		this.nextButton = page.locator("xpath=//button[normalize-space()='Next']");
	}

	public boolean isShowing() {

		return isPresent(page.locator("xpath=" + "//*[normalize-space(text())='International School Fee']"));

	}

	public boolean hasAmountField() {

		return isPresent(page.locator("[name=\'amount\']"));

	}

	public boolean hasRemarkField() {

		return isPresent(page.locator("[name=\'remark\']"));

	}

	public boolean showsSourceWalletBalance() {

		return isPresent(page.locator("xpath=" + "//button[contains(.,'Available Balance')]"));

	}

	/**
	 * The minimum the form states, taken from the amount box's placeholder.
	 *
	 * Returned rather than asserted here so the scenario can name the figure it expects and
	 * a change to it fails somewhere a reader can see.
	 */
	public String statedMinimum() {

		amountField.waitFor();

		return amountField.getAttribute("placeholder");

	}

	public void enterAmount(String amount) {

		amountField.waitFor();
		amountField.clear();
		amountField.fill(amount);

		settle();

	}

	public boolean hasNextButton() {

		return isPresent(page.locator("xpath=" + "//button[normalize-space()='Next']"));

	}

	public boolean canGoNext() {

		try {
			return nextButton.isEnabled();

		} catch (Exception notThere) {
			return false;
		}
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
