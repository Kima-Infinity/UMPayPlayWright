package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import java.time.Duration;

/**
 * The wallet to wallet transfer form at /v2/transfer/umpay-wallet.
 *
 * The recipient is identified by phone number here, not by the wallet UUID the Android app
 * asks for - the same product feature reached two different ways, which is worth knowing
 * when comparing the two suites.
 *
 * NOTHING HERE SENDS THE TRANSFER. The form is filled and its state read back; Next is
 * never pressed. Sending moves real money on the test environment and no test can undo it.
 */
public class UMPayWalletTransferPage {

	private final Page page;
		private final Locator heading;


		private final Locator phoneCountrySelect;


		private final Locator phoneField;


		private final Locator amountField;


		private final Locator remarkField;


	/** The wallet being sent from, shown as a button carrying its balance. */
		private final Locator sourceWalletButton;


		private final Locator nextButton;


	public UMPayWalletTransferPage(Page ldriver) {

		this.page = ldriver;
		this.heading = page.locator("xpath=//*[normalize-space(text())='To UMPay Wallet']");
		this.phoneCountrySelect = page.locator("[name=\'phoneCountry\']");
		this.phoneField = page.locator("[name=\'phone\']");
		this.amountField = page.locator("[name=\'amount\']");
		this.remarkField = page.locator("[name=\'remark\']");
		this.sourceWalletButton = page.locator("xpath=//button[contains(.,'Available Balance')]");
		this.nextButton = page.locator("xpath=//button[normalize-space()='Next']");
	}

	public boolean isShowing() {

		return isPresent(page.locator("xpath=" + "//*[normalize-space(text())='To UMPay Wallet']"));

	}

	public boolean hasRecipientPhoneField() {

		return isPresent(page.locator("[name=\'phone\']")) && isPresent(page.locator("[name=\'phoneCountry\']"));

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

	public void enterRecipientPhone(String phone) {

		type(phoneField, phone);

	}

	/**
	 * Types an amount.
	 *
	 * Only possible once the phone number has resolved to a real UMPay account - until then
	 * the box is disabled and this throws InvalidElementStateException. Kept because it is
	 * what a scenario with a real recipient in its test data needs; no scenario calls it
	 * today, for want of that recipient.
	 */
	public void enterAmount(String amount) {

		type(amountField, amount);

	}

	public void enterRemark(String remark) {

		type(remarkField, remark);

	}

	/**
	 * Whether the amount box will accept anything yet.
	 *
	 * It opens carrying disabled="true", and stays that way until the phone number resolves
	 * to a real UMPay account. That is the form's main rule: you cannot name an amount until
	 * the application knows who is receiving it.
	 */
	public boolean amountFieldIsEnabled() {

		try {
			return amountField.isEnabled();

		} catch (Exception notThere) {
			return false;
		}
	}

	public boolean remarkFieldIsEnabled() {

		try {
			return remarkField.isEnabled();

		} catch (Exception notThere) {
			return false;
		}
	}

	/** The message the form gives when the phone number belongs to nobody. */
	public boolean showsRecipientNotFound() {

		return isPresent(page.locator("xpath=" + "//*[contains(normalize-space(text()),'User does not exist')]"));

	}

	/**
	 * Whether the form will let the transfer go forward.
	 *
	 * Next is on the page from the moment the form opens but carries disabled="true" until
	 * the form is satisfied, so asking whether it is enabled is a real test of the
	 * validation where asking whether the button exists would pass on an empty form.
	 */
	public boolean canGoNext() {

		try {
			return nextButton.isEnabled();

		} catch (Exception notThere) {
			return false;
		}
	}

	public boolean hasNextButton() {

		return isPresent(page.locator("xpath=" + "//button[normalize-space()='Next']"));

	}

	private void type(Locator field, String text) {

		field.waitFor();
		field.clear();
		field.fill(text);

		// The form revalidates as it is typed into, and the button state follows a beat
		// behind; reading it immediately reads the state before the last keystroke.
		settle();

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
