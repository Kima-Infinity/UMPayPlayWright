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


	/**
	 * The button that sends the transfer, on the route that has one.
	 *
	 * The routes reached from Domestic and Global Transfer carry Next and go on to a summary;
	 * the one reached by choosing a saved template opens this same form with the destination
	 * already settled, so its button is Transfer and pressing it would send the money.
	 */
		private final Locator transferButton;


	public UnionPayAmountPage(Page ldriver) {

		this.page = ldriver;
		this.amountToPayField = page.locator("[id=\'amount-pay\']");
		this.amountToReceiveField = page.locator("[id=\'amount-receive\']");
		this.remarkField = page.locator("[name=\'remark\']");
		this.nextButton = page.locator("xpath=//button[normalize-space()='Next']");
		this.transferButton = page.locator("xpath=//button[normalize-space()='Transfer']");
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

	/**
	 * What the recipient receives, on the route that converts. Empty on the route that does not.
	 *
	 * Answers rather than waits. The Global route has no converted amount box at all, and asking
	 * for its value there blocks for the full timeout - which turned a failure message that
	 * merely mentions the figure into a thirty second hang, and reported the timeout instead of
	 * the assertion that had actually failed.
	 */
	public String amountToReceive() {

		try {
			if (!hasAmountToReceiveField()) {
				return "";
			}

			return amountToReceiveField.first().inputValue();

		} catch (Exception notThere) {
			return "";
		}
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

	/**
	 * The smallest transfer this form says it will take, from its own "Limit Min 10 USD" line.
	 *
	 * Read rather than written down, for the reason every other figure in this suite is: the
	 * band belongs to the route and the currency, and a number in a feature file is a number
	 * that goes stale.
	 */
	/** The largest transfer this form says it will take, from its own "Limit Max" line. */
	public String statedMaximum() {

		try {
			String line = page.locator("xpath=//*[contains(normalize-space(text()),'Limit Max')]/..")
					.first().innerText().replaceAll("\\s+", " ").trim();

			java.util.regex.Matcher figure =
					java.util.regex.Pattern.compile("Limit Max ([0-9][0-9,]*(?:\\.[0-9]+)?)").matcher(line);

			return figure.find() ? figure.group(1).replace(",", "") : "";

		} catch (Exception notStated) {
			return "";
		}
	}

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
	 * What the platform answered the send, when it answered the browser and not the user.
	 *
	 * This route says nothing on screen. A refused transfer draws no dialog, shows no message
	 * and simply returns to the hub, so the reading the deposits and withdraws rely on finds
	 * nothing to read - and a test that trusted the screen would call a refusal a success. The
	 * answer does exist, in the call the page made, which the run records as it goes.
	 */
	public String answerFromTheApi() {

		for (com.umpay.utility.ApiLog.Call call : com.umpay.utility.ApiLog.failures(page)) {

			if (call.url().contains("transfer") || call.url().contains("orders")) {

				String said = call.responseBody() == null ? "" : call.responseBody();

				return call.status() + " " + call.method() + " "
						+ call.url().replaceAll("https?://[^/]+", "") + (said.isBlank() ? "" : ": " + said);
			}
		}

		return "";
	}

	/** Whether the send left the form behind, which is all the screen says about it. */
	public boolean hasLeftTheForm() {

		return !page.url().contains("union-pay") && !isShowing();
	}

	/**
	 * Presses Transfer, which sends the money.
	 *
	 * The only method in this suite that sends anything from a transfer form, and it exists
	 * because it was asked for. Everything else on these screens stops at the state before
	 * sending, so a scenario has to call this deliberately - it cannot happen by accident.
	 */
	public void send() {

		transferButton.first().click();
	}

	/** Whether this is the form that sends, rather than the one that goes on to a summary. */
	public boolean hasTransferButton() {

		return isPresent(transferButton);
	}

	/**
	 * Whether the form would send the transfer as it stands.
	 *
	 * Read, never pressed. Nothing in this suite presses Transfer - it would move real money and
	 * no test can undo it - so what is asserted is that the form refuses to send until it has
	 * what it needs.
	 */
	public boolean canTransfer() {

		try {
			return transferButton.first().isEnabled();
		} catch (Exception notThere) {
			return false;
		}
	}

	/**
	 * Goes on from the amount to the receiver step.
	 *
	 * Next does not send anything - the routes that carry it ask who is receiving before they
	 * ask for a PIN - so this is safe in a way pressing Transfer is not.
	 */
	public void next() {

		nextButton.first().click();
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
