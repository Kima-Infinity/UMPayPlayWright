package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import java.time.Duration;

/**
 * The amount step of a UnionPay transfer, for both the China and Global routes.
 *
 * One class for both because it is one screen - /domestic-transfer/union-pay and
 * /global-transfer/union-pay are both titled "To UnionPay" and both state the same limits.
 * The difference is what they do with the amount: China converts, so it has a second box
 * showing what the recipient receives, while Global has only the paying amount. That
 * difference is asserted from the feature file rather than hidden in here.
 *
 * This is the amount step only. The receiver and template steps that follow it are covered
 * by GlobalTransfer.feature, which drives the whole flow from a spreadsheet; nothing here
 * duplicates that.
 *
 * NOTHING HERE SENDS THE TRANSFER.
 */
public class UnionPayAmountPage {

	private final Page page;
	/** The amount being paid, in the sending wallet's currency. */
		private final Locator amountToPayField;


	/** What the recipient receives. Present on the converting route only. */
		private final Locator amountToReceiveField;


		private final Locator remarkField;


		private final Locator nextButton;


	public UnionPayAmountPage(Page ldriver) {

		this.page = ldriver;
		this.amountToPayField = page.locator("[id=\'amount-pay\']");
		this.amountToReceiveField = page.locator("[id=\'amount-receive\']");
		this.remarkField = page.locator("[name=\'remark\']");
		this.nextButton = page.locator("xpath=//button[normalize-space()='Next']");
	}

	public boolean isShowing() {

		return isPresent(page.locator("xpath=" + "//*[normalize-space(text())='To UnionPay']"));

	}

	/** A label on the form - a stated limit, a balance, a currency heading. */
	public boolean shows(String label) {

		return isPresent(page.locator("xpath=" + "//*[contains(normalize-space(text()),\"" + label + "\")]"));

	}

	public boolean hasAmountToPayField() {

		return isPresent(page.locator("[id=\'amount-pay\']"));

	}

	/**
	 * Whether the converting route's second amount box is on the form.
	 *
	 * Asked as its own question because its absence is the point of the Global scenario,
	 * and an absence needs a locator that is allowed to find nothing.
	 */
	public boolean hasAmountToReceiveField() {

		return page.locator("[id=\'amount-receive\']").all().size() > 0;

	}

	public boolean hasRemarkField() {

		return isPresent(page.locator("[name=\'remark\']"));

	}

	public void enterAmountToPay(String amount) {

		amountToPayField.waitFor();
		amountToPayField.clear();
		amountToPayField.fill(amount);

		settle();

	}

	public String amountToReceive() {

		return amountToReceiveField.inputValue();

	}

	/**
	 * Whether the form will let the transfer go on to the receiver step.
	 *
	 * Next starts disabled and only becomes enabled once the amount satisfies the stated
	 * limits, so this is the form's validation being tested rather than its layout.
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

	private boolean isPresent(Locator locator) {

		try {
			locator.first().waitFor(new Locator.WaitForOptions()
					.setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
			return true;

		} catch (Exception notThere) {
			return false;
		}
	}

	/** The rate is fetched as the amount is typed, so the converted box fills a beat later. */
	private void settle() {

		try {
			Thread.sleep(3000);

		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}
}
