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
	/**
	 * The three ways this form lets a recipient be named: MOBILE, EMAIL and ID.
	 *
	 * The form opens on MOBILE and the other two were never covered, though they are separate
	 * code paths - a different box, a different lookup, and a different answer when nobody holds
	 * what was typed.
	 */
	public void nameTheRecipientBy(String how) {

		// Matched whatever case the markup uses. The tabs read MOBILE, EMAIL and ID on screen and
		// are not written that way in the page - the capitals are the stylesheet's doing - so a
		// locator that asked for the rendered wording found nothing at all.
		String upper = "translate(normalize-space(text()),"
				+ "'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')";

		Locator tab = page.locator("xpath=//*[" + upper + "=\"" + how.toUpperCase() + "\"]");

		try {
			tab.first().waitFor();
			tab.first().click();
		} catch (Exception notOffered) {
			throw new IllegalStateException("The wallet transfer form offers no way to name a"
					+ " recipient by " + how);
		}

		settle();
	}

	/**
	 * Types whatever identifies the recipient into whichever box the chosen way is showing.
	 *
	 * One method for all three, because from the scenario's point of view it is one action -
	 * saying who is being paid - and which box that lands in is this page's business.
	 */
	public void enterRecipient(String identifier) {

		// Whichever box the chosen way is showing, rather than a list of the names it might have.
		// Naming them was a guess that held for the phone and email boxes and not for the ID one,
		// and a guess about a name is a locator that quietly matches nothing. What is certain is
		// that the box for naming a recipient is the only one open before an amount can be
		// entered: the amount and the remark are locked until somebody is found, and the country
		// beside a phone number is a picker rather than a box to type in.
		for (Locator box : com.umpay.utility.Wait.all(page.locator("input"))) {

			try {
				String name = String.valueOf(box.getAttribute("name"));

				if (name.equals("amount") || name.equals("remark") || name.equals("phoneCountry")) {
					continue;
				}

				if (box.isVisible() && box.isEnabled()) {

					box.fill(identifier);
					settle();
					return;
				}

			} catch (Exception notThisOne) {
				// A box that will not answer is not the one being typed into.
			}
		}

		throw new IllegalStateException("The wallet transfer form is showing no box to name a"
				+ " recipient in");
	}

	/**
	 * Names a recipient by mobile, which takes two things rather than one.
	 *
	 * The number is asked for beside a country, and the country is not a box to type in - it is a
	 * list of two hundred entries reading "CAMBODIA (+855)". So the dialling code is looked up in
	 * it and the rest of the number typed. The sheet holds them together, the way a person would
	 * write the number down.
	 */
	public void nameTheRecipientByMobile(String diallingCode, String number) {

		Locator country = page.locator("[name='phoneCountry']");

		try {
			country.first().selectOption(new com.microsoft.playwright.options.SelectOption()
					.setLabel(labelFor(diallingCode)));

		} catch (Exception notASelect) {

			// Not a select after all: open it and choose the row that carries the code.
			try {
				country.first().click();
				settle();

				page.locator("xpath=//*[contains(normalize-space(.),\"(+" + diallingCode + ")\")]")
						.first().click();

			} catch (Exception cannotChoose) {
				throw new IllegalStateException("The wallet form would not take +" + diallingCode
						+ " as the recipient's country");
			}
		}

		settle();

		enterRecipient(number);
	}

	/** The entry in the country list that carries a dialling code, as the list writes it. */
	private String labelFor(String diallingCode) {

		for (Locator option : com.umpay.utility.Wait.all(page.locator("[name='phoneCountry'] option"))) {

			try {
				String said = option.innerText().trim();

				if (said.contains("(+" + diallingCode + ")")) {
					return said;
				}

			} catch (Exception gone) {
				// An option that will not answer is not the one being looked for.
			}
		}

		throw new IllegalStateException("The country list offers no +" + diallingCode);
	}

	/** Whether the form has found somebody to pay, which is what unlocks the rest of it. */
	public boolean hasFoundTheRecipient() {

		return amountFieldIsEnabled();
	}

	/** Goes on from the amount, which does not send: a summary and a PIN come after it. */
	public void next() {

		nextButton.first().click();

		settle();
	}

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
