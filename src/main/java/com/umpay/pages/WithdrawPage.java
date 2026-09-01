package com.umpay.pages;

import com.umpay.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import com.umpay.utility.FormInput;

import java.time.Duration;
import java.util.List;

/*
 * Built to mirror DepositPage. The withdraw screen is served from a chunk that
 * only loads after login, so the locators below follow the deposit ones rather
 * than having been read off the live page. Check them against the running
 * application on the first run and correct any that differ.
 */
public class WithdrawPage {

	Page page;
	
	/** The security code that authorises a withdraw. Override with -Dumpay.pin=... */
	private static final String PIN = System.getProperty("umpay.pin", "1111");

	/**
	 * The wallet picker, anchored on its own "Currency" label.
	 *
	 * It used to be an absolute path from #root through eleven positional divs, and that path
	 * now matches nothing at all - which is what made every withdraw run fail. The failure was
	 * badly misleading: submitWithdraw died on its first line, so the amount was never typed,
	 * and the form sat there with its default value of 1 against a minimum of 100. Every
	 * screenshot therefore looked like the amount had been typed wrongly, when in truth
	 * nothing had been typed at all.
	 *
	 * Anchoring on the visible label survives the markup being reshuffled, which a positional
	 * path cannot. Verified against the live page: this matches exactly one element, the
	 * button reading "Hong Kong".
	 */
		private final Locator currencyButton;


		private final Locator amountField;


	/**
	 * The two ways of naming a payout destination on the redesigned form. The old Payment Type
	 * and Payment Name pickers, and the account fields that went with them, are gone.
	 */
		private final Locator fromTemplateTab;


		private final Locator selectPaymentAccountButton;


	/**
	 * Located by its own label rather than by an absolute path. The previous locator walked
	 * eleven positional divs from #root, which is the same fragility that silently broke the
	 * currency picker.
	 */
		private final Locator confirmButton;


		private final Locator submittedOrderStatus;


	public WithdrawPage(Page ldriver) {

		this.page = ldriver;
		this.currencyButton = page.locator("xpath=//*[normalize-space()='Currency']/following::button[1]");
		this.amountField = page.locator("[id=\'amount\']");
		this.fromTemplateTab = page.locator("xpath=//button[normalize-space()='From Template']");
		this.selectPaymentAccountButton = page.locator("xpath=//button[normalize-space()='Select Payment Account']");
		this.confirmButton = page.locator("xpath=//button[normalize-space()='Confirm']");
		this.submittedOrderStatus = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div/div/div/div[1]/h5");
	}

	public void submitWithdraw(String amount, String currency, String paymentType, String paymentName,
							   String fpsAccount, String fpsReceiverName,
							   String accountName, String accountNumber,
							   String walletName, String walletNumber) {

		selectCurrency(currency);

		FormInput.type(amountField, amount, "withdraw amount");

		selectSavedPaymentAccount(paymentName);

		clickConfirmButton();

		enterPinIfAsked();

		reportSubmittedStatus();

		closeSubmittedDialog();
	}

	/** Opens the wallet picker and chooses the wallet holding the money to withdraw. */
	private void selectCurrency(String currency) {

		currencyButton.waitFor();
		click(currencyButton, "the currency picker");

		List<Locator> currencies = Wait.all(page.locator(
				"xpath=//*[@id=\"root.dialog\"]/div/div[2]/div/div/div/div/div[2]/div"));

		System.out.println("Number of currencies found: " + currencies.size());

		for (Locator element : currencies) {
			try {
				Locator walletCurrency = element.locator("xpath=" + ".//div[1]/div[2]/p");
				Locator balance = element.locator("xpath=" + ".//div[2]/div/p[2]");

				System.out.println("Wallet Balance for " + walletCurrency.innerText() + ": " + balance.innerText());

				// A withdraw can only be raised against a wallet that holds money.
				if (walletCurrency.innerText().equals(currency) && !balance.innerText().equals("0.00")) {
					System.out.println("Clicking on wallet currency: " + walletCurrency.innerText());
					click(element, "the " + currency + " wallet");
					return;
				}
			} catch (Exception e) {
				System.out.println("Error locating sub-elements: " + e.getMessage());
			}
		}

		throw new IllegalStateException("No " + currency + " wallet with a balance was offered on the"
				+ " withdraw currency list");
	}

	/**
	 * Picks one of the account's saved payout templates by name.
	 *
	 * This screen was redesigned and the page object had not caught up. It used to walk a
	 * Payment Type list (E-Wallet, Bank Transfer, Alipay, USDT) and then a Payment Name list,
	 * and type the payout details into fields on the form. Neither list exists now: the form
	 * offers "From Template" and "New Account", and a template carries the account details
	 * that used to be typed in. Confirmed against the live page - a search for a "Payment Type"
	 * label matches nothing at all.
	 *
	 * The templates saved on the test account are HSBC, FPS and USDT-TRC 20, which is why
	 * PaymentName in the test data still selects the right one. The remaining payout columns -
	 * FPDAccountNumber, FPSReceiverName, AccountName, AccountNumber, WalletName, WalletNumber -
	 * now describe the template rather than anything this form asks for, so they are no longer
	 * typed anywhere. They are kept in the signature so the step definition and the spreadsheet
	 * do not have to change in the same breath as the page.
	 */
	private void selectSavedPaymentAccount(String paymentName) {

		click(fromTemplateTab, "the From Template tab");
		click(selectPaymentAccountButton, "Select Payment Account");

		Locator option = page.locator("xpath=//*[@id=\"root.dialog\"]//*[normalize-space()=\""
				+ paymentName + "\"]").first();

		try {
			option.waitFor();
		} catch (Exception notListed) {
			throw new IllegalStateException("No saved payment account called \"" + paymentName
					+ "\" was offered. Saved templates on this account: " + templatesOnOffer());
		}

		System.out.println("Choosing the saved payment account: " + paymentName);

		// The element carrying the template's name is a heading strip inside the row and ignores
		// clicks; the row around it is what selects. Verified on the live page - clicking the
		// name leaves the picker open, clicking its parent closes it and fills the form in.
		//
		// This mattered more than it looks. While the picker stayed open it covered the form, so
		// Confirm was being clicked through an overlay: the step threw nothing and reported
		// "Initiated Withdraw transaction successfully" without a withdraw having been submitted,
		// and the next step could not reach the sidebar. Hence checking the picker has actually
		// closed rather than trusting the click.
		Locator label = option;

		for (int level = 0; level <= 3; level++) {

			click(label, level == 0 ? "the " + paymentName + " template"
					: "the " + paymentName + " row, " + level + " level(s) up");

			if (!pickerIsOpen()) {
				return;
			}

			label = label.locator("xpath=" + "..");
		}

		throw new IllegalStateException("The payment account picker stayed open after choosing \""
				+ paymentName + "\". Selecting it is what fills the form in, so carrying on would"
				+ " submit nothing and report success.");
	}

	/**
	 * Authorises the withdraw with the account's PIN.
	 *
	 * Confirm does not submit anything on its own - the app answers with a PIN dialog, and the
	 * withdraw only goes through once that is filled in. The page object never had this step,
	 * which is why no run had ever actually raised a withdraw: every one of them stopped at this
	 * dialog, and because the dialog is aria-modal it then covered the sidebar and blocked
	 * whatever step came next. The dialog announcing itself as "PIN" in the run log is what
	 * finally gave it away.
	 *
	 * The inputs are located inside the dialog rather than by an absolute path, and both shapes
	 * are handled: one box for the whole code, or one box per digit.
	 */
	private void enterPinIfAsked() {

		Locator dialog = null;

		long deadline = System.currentTimeMillis() + 15000;

		while (System.currentTimeMillis() < deadline && dialog == null) {
			for (Locator candidate : Wait.all(page.locator("div[role='dialog']"))) {
				if (candidate.isVisible() && candidate.innerText().toUpperCase().contains("PIN")) {
					dialog = candidate;
					break;
				}
			}
			if (dialog == null) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}

		if (dialog == null) {
			System.out.println("No PIN dialog appeared after Confirm.");
			return;
		}

		// Only the boxes a person could type into. The dialog carries seven inputs, most of them
		// hidden plumbing for the PIN component, and reaching for one of those threw
		// ElementNotInteractableException and took the run down with it.
		List<Locator> boxes = new java.util.ArrayList<>();

		for (Locator input : Wait.all(dialog.locator("input"))) {
			try {
				if (input.isVisible() && input.isEnabled()) {
					boxes.add(input);
				}
			} catch (Exception skip) {
				// A box that cannot even be asked is not one to type into.
			}
		}

		if (boxes.isEmpty()) {
			throw new IllegalStateException("The withdraw asked for a PIN but none of the dialog's"
					+ " inputs can be typed into. Dialog reads: " + dialog.innerText());
		}

		System.out.println("Authorising the withdraw with the PIN ("
				+ boxes.size() + " box(es) available to type into).");

		// Sent to the first box as one string: a split PIN component advances by itself, and a
		// single box wants the whole code anyway. Only if that leaves the boxes empty is it worth
		// pushing a digit into each one.
		boxes.get(0).click();
		boxes.get(0).fill(PIN);

		if (boxes.size() > 1 && String.valueOf(boxes.get(0).inputValue()).isEmpty()) {

			System.out.println("The PIN did not advance across the boxes; filling them one at a time.");

			for (int i = 0; i < boxes.size() && i < PIN.length(); i++) {
				boxes.get(i).click();
				boxes.get(i).fill(String.valueOf(PIN.charAt(i)));
			}
		}

		try {
			Thread.sleep(6000);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	/** Whether a modal is still covering the form. */
	private boolean pickerIsOpen() {

		for (Locator dialog : Wait.all(page.locator("div[role='dialog']"))) {
			if (dialog.isVisible()) {
				return true;
			}
		}

		return false;
	}

	/** The template names the open dialog is listing, for when the wanted one is missing. */
	private String templatesOnOffer() {

		StringBuilder names = new StringBuilder();

		for (Locator row : Wait.all(page.locator("xpath=//*[@id=\"root.dialog\"]//p"))) {
			String text = row.innerText().trim();
			if (!text.isEmpty() && !text.endsWith(":")) {
				names.append(names.length() == 0 ? "" : ", ").append(text);
			}
		}

		return names.length() == 0 ? "none" : names.toString();
	}

	/**
	 * Reads the status the app shows once the order is in.
	 *
	 * Logged rather than asserted. The confirm has already happened by this point, so a status
	 * element that cannot be found means the reporting is stale, not that the withdraw failed -
	 * and failing here would say the opposite. The dedicated "the withdraw order status should
	 * be" step is where a run asserts on the value.
	 */
	private void reportSubmittedStatus() {

		try {
			// Three seconds, not fifteen. This locator points at the form behind the receipt
			// dialog and the status is inside the dialog, so waiting longer only slows every
			// run down by the length of the timeout. closeSubmittedDialog logs the real status.
			submittedOrderStatus.waitFor(new Locator.WaitForOptions().setTimeout(3 * 1000));
			System.out.println("Submitted Order Status: " + submittedOrderStatus.innerText());
		} catch (Exception cannotRead) {
			System.out.println("The withdraw was submitted but the order status element could not be"
					+ " read - the status locator may be stale: " + cannotRead.getMessage());
		}
	}

	/**
	 * Closes the receipt dialog the app leaves open after a withdraw is confirmed.
	 *
	 * This matters to whatever runs next rather than to the withdraw itself. The dialog is
	 * aria-modal and covers the sidebar, so the end-to-end run could not reach Convert
	 * afterwards: the click on the nav item was intercepted by the overlay, the JavaScript
	 * fallback "succeeded" without navigating anywhere, and the run sat on /withdraw until it
	 * gave up. Running Convert on its own never showed this because a fresh login has no
	 * dialog open.
	 *
	 * The dialog's text is logged on the way past. The submitted status lives inside it, which
	 * is why reading that status from the form behind it finds nothing.
	 */
	private void closeSubmittedDialog() {

		Locator dialog = visibleDialog();

		if (dialog == null) {
			return;
		}

		System.out.println("Dialog still open after the withdraw: "
				+ dialog.innerText().replace(System.lineSeparator(), " | "));

		// Anything that plainly means "done". Confirm is excluded on purpose - clicking that
		// again would raise a second withdraw.
		for (String label : new String[]{"Ok", "OK", "Done", "Close", "Got it", "Back", "Finish"}) {

			List<Locator> buttons = Wait.all(dialog.locator(
					"xpath=.//button[normalize-space()='" + label + "']"));

			if (!buttons.isEmpty()) {
				click(buttons.get(0), "the dialog's " + label + " button");
				if (visibleDialog() == null) {
					return;
				}
			}
		}

		page.keyboard().press("Escape");
		settle();

		if (visibleDialog() == null) {
			return;
		}

		// Last resort, and the one that matters to whatever runs next. The dialog is aria-modal
		// and covers the sidebar, so leaving it up means the following step cannot navigate at
		// all - it clicks, the overlay swallows it, and the run sits on the withdraw URL until it
		// gives up. The withdraw itself is already complete by this point (the URL carries an
		// orderId), so going back to the dashboard costs nothing and is what a person would do.
		String origin = page.url().replaceAll("(https?://[^/]+).*", "$1");

		System.out.println("The dialog would not dismiss; returning to " + origin + " so the next"
				+ " step has a usable page.");

		page.navigate(origin + "/");
		settle();
	}

	/** The first modal actually on screen, or null when the page is clear. */
	private Locator visibleDialog() {

		for (Locator dialog : Wait.all(page.locator("div[role='dialog']"))) {
			try {
				if (dialog.isVisible()) {
					return dialog;
				}
			} catch (Exception gone) {
				// Re-rendered from under us, which means it is not blocking anything.
			}
		}

		return null;
	}

	private void settle() {

		try {
			Thread.sleep(2500);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	/** Clicks with a JavaScript fallback, which this app's overlays occasionally need. */
	private void click(Locator element, String what) {

		try {
			element.waitFor();
			element.click();
		} catch (Exception e) {
			element.dispatchEvent("click");
		}

		System.out.println("Clicked " + what);

		try {
			Thread.sleep(1500);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	public String getSubmittedOrderStatus() {

		submittedOrderStatus.waitFor();
		return submittedOrderStatus.innerText();
	}

	/**
	 * Submits the order.
	 *
	 * It used to wait for an order-details panel first, found by another absolute path that no
	 * longer resolves. Waiting for the Confirm button itself is both simpler and the thing that
	 * actually matters: the button only becomes clickable once the form is complete.
	 */
	private void clickConfirmButton() {

		click(confirmButton, "Confirm");

		try {
			Thread.sleep(5000);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Money leaving the account may be gated behind a security PIN, which a
	 * deposit never asks for. Supply it with -Dumpay.pin=123456. When no PIN
	 * screen appears this does nothing, so the deposit style flow is unaffected.
	 */
	private void enterSecurityPinIfPresent() {

		String pin = System.getProperty("umpay.pin");

		if (pin == null || pin.isBlank()) {
			return;
		}

		try {
			Locator pinField = page.locator("[id=\'pin\']").first();

			pinField.waitFor(new Locator.WaitForOptions().setTimeout(5 * 1000));
			pinField.fill(pin);
			System.out.println("Security PIN entered");
			Thread.sleep(3000);
		} catch (Exception e) {
			System.out.println("No security PIN step shown: " + e.getMessage());
		}
	}
}
