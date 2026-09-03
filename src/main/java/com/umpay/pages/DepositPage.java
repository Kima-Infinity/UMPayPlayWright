package com.umpay.pages;

import com.umpay.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import com.umpay.utility.FormInput;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DepositPage {

	Page page;
	
		private final Locator currencyButton;


		private final Locator amountField;


		private final Locator paymentTypeButton;


		private final Locator paymentNameButton;


		private final Locator fpdPayerNameField;


		private final Locator fpsAccountField;


		private final Locator accountNameField;


		private final Locator accountNumberField;


		private final Locator walletNameField;


		private final Locator walletNumberField;


		private final Locator payAddressField;


		private final Locator orderDetailsField;


		private final Locator confirmButton;


		private final Locator submittedOrderStatus;


	public DepositPage(Page ldriver) {

		this.page = ldriver;
		this.currencyButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/form/div/div[1]/div[1]/div[1]/button");
		this.amountField = page.locator("[id=\'amount\']");
		// Once a payment type has been chosen this shape matches the payment name button as
		// well - both render as the same kind of picker - so the first match is taken rather
		// than left ambiguous. Before a choice there is only one, so this changes nothing for
		// the deposit that walks the form in order.
		this.paymentTypeButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/form/div/div[2]/div/div/div/div/div/div/button").first();
		this.paymentNameButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/form/div/div[2]/div/div[1]/div/div[2]/div/div/button");
		this.fpdPayerNameField = page.locator("[id=\'FPSReceiverName\']");
		this.fpsAccountField = page.locator("[id=\'account\']");
		this.accountNameField = page.locator("[id=\'accountName\']");
		this.accountNumberField = page.locator("[id=\'accountNumber\']");
		this.walletNameField = page.locator("[id=\'walletName\']");
		this.walletNumberField = page.locator("[id=\'walletNumber\']");
		// A USDT deposit asks for the address the money is coming from. The box has no id worth
		// holding on to, so it is found by the label above it.
		this.payAddressField = page.locator(
				"xpath=//*[contains(normalize-space(text()),'Pay Address')]/following::input[1]").first();
		this.orderDetailsField = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/form/div/div[3]/div/div");
		this.confirmButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/form/button");
		this.submittedOrderStatus = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div/div/div/div[1]/h5");
	}


	/**
	 * Chooses a wallet by its currency code.
	 *
	 * submitDeposit does this as part of a whole deposit; the limit scenarios need to stop at
	 * the amount box, so the currency step is available on its own.
	 */
	public void chooseCurrency(String currency) {

		// The form has to be on the screen before its wallet list can be opened: open it while
		// the page is still rendering and the list comes up empty, which reads as "this account
		// has no such wallet" and is not that at all.
		amountField.waitFor();

		for (int attempt = 1; attempt <= 3; attempt++) {

			List<String> offered = new java.util.ArrayList<>();

			for (Locator wallet : openWalletList()) {

				String code;

				// Only the reading is guarded. Choosing the wallet is not: an error there is the
				// answer to what went wrong, and swallowing it once cost an afternoon - the run
				// went on to the end of the list and reported that a list plainly holding HKD
				// did not offer HKD, while the real complaint went unseen.
				try {
					code = wallet.locator("xpath=.//div[1]/div[2]/p").innerText();
				} catch (Exception notAWalletRow) {
					continue;
				}

				offered.add(code.trim() + describe(code));

				if (sameCode(code, currency)) {

					try {
						wallet.click();
					} catch (Exception notClickable) {
						wallet.dispatchEvent("click");
					}

					settleLimits(currency);
					return;
				}
			}

			// A list that was read and does not hold the currency is an answer; an empty one is
			// only the list having closed again, which is worth another try.
			if (!offered.isEmpty()) {
				throw new IllegalStateException("The wallet list offers " + offered + ", not " + currency);
			}
		}

		throw new IllegalStateException("The wallet list would not stay open long enough to read,"
				+ " so whether this account holds a " + currency + " wallet is unknown");
	}

	/**
	 * Whether two currency codes are the same code.
	 *
	 * The list's codes do not always arrive as bare letters - a no-break space or a zero width
	 * character can sit inside the row and survive trim(), which strips nothing above a plain
	 * space. A comparison that failed on that reported a list holding HKD as not offering HKD,
	 * which is a confusing thing for a run to say. Everything that is not a letter or a digit
	 * comes out before the two are compared.
	 */
	private boolean sameCode(String code, String wanted) {

		return bare(code).equalsIgnoreCase(bare(wanted));
	}

	private String bare(String text) {

		return text == null ? "" : text.replaceAll("[^A-Za-z0-9]", "");
	}

	/** The codepoints of anything unprintable in a code, so a mismatch can be explained. */
	private String describe(String code) {

		StringBuilder oddities = new StringBuilder();

		for (char character : code.toCharArray()) {
			if (!Character.isLetterOrDigit(character) && character != ' ') {
				oddities.append(String.format("<U+%04X>", (int) character));
			}
		}

		return oddities.toString();
	}

	/**
	 * Opens the wallet list and hands back its rows.
	 *
	 * Two things go wrong here and they look the same from the outside. A click on the picker
	 * is sometimes lost, because the button is on the screen before the page has finished
	 * wiring it up; and a list that did open sometimes closes again while it is being read. In
	 * both cases nothing throws and the rows are simply not there, which the caller used to
	 * report as "the account has no such wallet" - a confident answer to a question that was
	 * never actually asked. So opening is confirmed rather than assumed, and a list that will
	 * not stay open is tried again.
	 *
	 * @return the rows, or an empty list if none could be read
	 */
	private List<Locator> openWalletList() {

		currencyButton.waitFor();

		for (int attempt = 1; attempt <= 3; attempt++) {

			try {
				currencyButton.click();
			} catch (Exception notClickable) {
				currencyButton.dispatchEvent("click");
			}

			List<Locator> rows = Wait.all(page.locator(
					"xpath=//*[@id='root.dialog']/div/div[2]/div/div/div/div/div[2]/div"), 8);

			if (!rows.isEmpty()) {
				return rows;
			}
		}

		return List.of();
	}

	/**
	 * Waits for the form to fetch the chosen currency's own limits.
	 *
	 * Every wallet has its own band - BRL takes 100 to 5000, IDR takes 320000 to 10000000 - and
	 * they arrive from the server a moment after the wallet is picked. Read the box in that
	 * moment and it still holds the last choice's numbers, which is a reading that looks
	 * perfectly plausible and belongs to a different currency.
	 *
	 * The first attempt at this waited for the numbers to change from whatever they were. That
	 * is not enough, and a whole run of deposits proved it: every currency was given the
	 * previous one's minimum, five of them fell outside their own band and failed, and the
	 * other five quietly deposited the wrong amount - the worse outcome of the two. The numbers
	 * alone cannot say which currency they belong to.
	 *
	 * The placeholder can. It reads "Limit Min 100 HKD" - the form's own words, with the
	 * currency named - so this waits until the box is talking about the currency that was asked
	 * for, and says so plainly rather than deposit a figure belonging to another wallet.
	 */
	private void settleLimits(String currency) {

		boolean settled = Wait.until(() -> {
			String note = statedLimitNote();
			return note != null && note.contains(currency);
		}, 20);

		if (!settled) {
			throw new IllegalStateException("The deposit form never stated a limit for " + currency
					+ "; the amount box still reads \"" + statedLimitNote() + "\"");
		}
	}

	/**
	 * What the amount box says it will take, in the form's own words - "Limit Min 100 HKD".
	 *
	 * The only place on the form where a limit and the currency it belongs to are stated
	 * together, which is what makes it worth reading rather than the bare min attribute. It is
	 * a row of its own under the amount box - the box's placeholder is just a faint 1 - so it
	 * is found by the words it starts with rather than by a path through the form.
	 */
	public String statedLimitNote() {

		try {
			return page.locator("xpath=//*[contains(normalize-space(text()),'Limit Min')]/..")
					.first().innerText().replaceAll("\\s+", " ").trim();
		} catch (Exception notStatedYet) {
			return null;
		}
	}

	/** Types an amount and leaves it there, without submitting. */
	public void enterAmount(String amount) {

		FormInput.type(amountField, amount, "deposit amount");

	}

	/**
	 * The browser's own verdict on the amount.
	 *
	 * The box is type=number with min and max, so an amount outside them never reaches the
	 * server and there is no banner to read - the message lives on the element, and it is
	 * the application's own wording rather than the browser's stock phrasing.
	 */
	public String amountValidationMessage() {

		return (String) amountField.evaluate("el => el.validationMessage");

	}

	/** Whether the browser considers the amount acceptable. */
	public boolean isAmountValid() {

		return (Boolean) amountField.evaluate("el => el.checkValidity()");

	}

	/** The lower bound the box enforces, as the form states it. */
	public String statedMinimum() {

		return amountField.getAttribute("min");

	}

	/** The upper bound the box enforces. The form does not print this one anywhere. */
	public String statedMaximum() {

		return amountField.getAttribute("max");

	}

	/**
	 * Every wallet the currency list offers, in the order shown, with the balance each holds.
	 *
	 * The dropdown is not the platform's currency list - it is this account's wallets - so
	 * what it offers is a fact about the account and has to be read rather than assumed. The
	 * codes and balances come back as text; handing the rows themselves to a caller would let
	 * a step click one, which is the coupling the page object exists to prevent.
	 */
	public Map<String, String> walletBalances() {

		amountField.waitFor();

		Map<String, String> wallets = new LinkedHashMap<>();

		for (Locator wallet : openWalletList()) {

			try {
				String code = wallet.locator("xpath=.//div[1]/div[2]/p").innerText().trim();
				String balance = wallet.locator("xpath=.//div[2]/div/p[2]").innerText().trim();

				wallets.put(code, balance);

			} catch (Exception notAWalletRow) {
				// A row that will not answer is not a wallet.
			}
		}

		dismissList();

		return wallets;
	}

	/**
	 * Chooses a payment type without going on to fill in the form underneath it.
	 *
	 * A currency with a single channel does not offer a choice: the picker shows that channel
	 * and is disabled. Asking for it by name is still the right thing for a caller to do, so
	 * that counts as chosen rather than as a failure.
	 */
	private void choosePaymentType(String paymentType) {

		paymentTypeButton.waitFor();

		if (!paymentTypeButton.isEnabled()) {

			if (paymentTypeButton.innerText().trim().equals(paymentType)) {
				return;
			}

			throw new IllegalStateException("The only payment type on offer is "
					+ paymentTypeButton.innerText().trim() + ", not " + paymentType);
		}

		paymentTypeButton.click();

		for (Locator option : Wait.all(page.locator(
				"xpath=//*[@id='root.dialog']/div/div[2]/div/div/div/div/div[2]/div"))) {

			try {
				if (option.locator("xpath=.//div[1]/div[2]/span").innerText().trim().equals(paymentType)) {
					try {
						option.click();
					} catch (Exception notClickable) {
						option.dispatchEvent("click");
					}
					return;
				}
			} catch (Exception keepLooking) {
				// A row that will not answer is a row this is not looking for.
			}
		}

		throw new IllegalStateException("The payment type list does not offer " + paymentType);
	}

	/**
	 * Chooses a payment name under the chosen type - FPS under HKD's E-Wallet, MOMO under
	 * VND's.
	 *
	 * The names are fetched after the type is chosen and the picker stays disabled until they
	 * arrive, so this waits rather than reading "nothing on offer" from a list that has not
	 * loaded. A channel with one name arrives already chosen and the picker stays disabled;
	 * that is only correct if it settled on the name that was asked for, so a mismatch is
	 * reported rather than passed over. It means the test data names a channel the form no
	 * longer offers, which is worth failing for.
	 */
	private void choosePaymentName(String paymentName) {

		paymentNameButton.waitFor();

		Wait.until(paymentNameButton::isEnabled, 10);

		if (!paymentNameButton.isEnabled()) {

			String settledOn = chosenPaymentName();

			if (settledOn.equals(paymentName)) {
				return;
			}

			throw new IllegalStateException("The form settled on " + settledOn
					+ " and offers no choice, but the test data asks for " + paymentName);
		}

		try {
			paymentNameButton.click();
		} catch (Exception notClickable) {
			paymentNameButton.dispatchEvent("click");
		}

		List<Locator> options = Wait.all(page.locator(
				"xpath=//*[@id='root.dialog']/div/div[2]/div/div/div/div/div/div[3]/div"));

		for (int option = 1; option <= options.size(); option++) {

			Locator name = page.locator("xpath=//*[@id='root.dialog']/div/div[2]/div/div/div/div/div/div[3]/div["
					+ option + "]/div[1]/p");

			try {
				if (name.innerText().trim().equals(paymentName)) {
					try {
						name.click();
					} catch (Exception notClickable) {
						name.dispatchEvent("click");
					}
					return;
				}
			} catch (Exception keepLooking) {
				// A row that will not answer is a row this is not looking for.
			}
		}

		throw new IllegalStateException("The payment name list does not offer " + paymentName);
	}

	/** Whatever the payment name picker has settled on, chosen or not. */
	private String chosenPaymentName() {

		paymentNameButton.waitFor();

		return paymentNameButton.innerText().trim();
	}

	/**
	 * Fills in whichever detail fields the chosen channel is asking for.
	 *
	 * This used to be a switch on the payment type, which reads well and is wrong: the same
	 * type asks for different things in different currencies. HKD's E-Wallet wants the payer's
	 * name and an account, Brazil's wants an account number alone, Indonesia's and Thailand's
	 * want an account name and number, and the Philippines' wants nothing at all. A deposit in
	 * a currency the switch had not met filled the wrong boxes or none, so the form asks and
	 * this answers.
	 */
	private void fillDetailsAsked(String fpsAccount, String fpsPayerName,
								  String accountName, String accountNumber,
								  String walletName, String walletNumber) {

		if (fpdPayerNameField.isVisible()) fpdPayerNameField.fill(fpsPayerName);
		if (fpsAccountField.isVisible()) fpsAccountField.fill(fpsAccount);
		if (accountNameField.isVisible()) accountNameField.fill(accountName);
		if (accountNumberField.isVisible()) accountNumberField.fill(accountNumber);
		if (walletNameField.isVisible()) walletNameField.fill(walletName);
		if (walletNumberField.isVisible()) walletNumberField.fill(walletNumber);

		// The USDT channels ask for this one and no other channel does. It was missed at first
		// because the box still held an address typed into it by hand on some earlier visit, so
		// the deposit went through and the gap only showed itself on a form that had been
		// cleared - which is the state any other machine would start from.
		if (payAddressField.isVisible()) payAddressField.fill(walletNumber);
	}

	/**
	 * Fills in and sends a deposit.
	 *
	 * An amount of MIN means the smallest the chosen wallet will take, read off the form. Every
	 * currency has its own minimum and several of them are a conversion of a figure held in
	 * another currency - the US dollar wallet asked for 19.77 on the day this was written - so
	 * a number written into the test data would be a number that goes stale. Anything else is
	 * used exactly as it is given.
	 */
	public void submitDeposit(String amount, String currency, String paymentType, String paymentName,
							  String fpsAccount, String fpsPayerName,
							  String accountName, String accountNumber,
							  String walletName, String walletNumber) {

		String deposit = "the " + currency + " deposit by " + paymentType + " / " + paymentName;

		chooseCurrency(currency);
		stopIfRefused(deposit, "choosing the wallet");

		String toDeposit = "MIN".equalsIgnoreCase(amount) ? statedMinimum() : amount;

		System.out.println("Depositing " + toDeposit + " " + currency
				+ " by " + paymentType + " / " + paymentName
				+ "  (the form states: " + statedLimitNote() + ")");

		FormInput.type(amountField, toDeposit, "deposit amount");
		stopIfRefused(deposit, "entering the amount");

		choosePaymentType(paymentType);
		stopIfRefused(deposit, "choosing the payment type");

		choosePaymentName(paymentName);
		stopIfRefused(deposit, "choosing the payment name");

		fillDetailsAsked(fpsAccount, fpsPayerName, accountName, accountNumber, walletName, walletNumber);
		stopIfRefused(deposit, "filling in the payment details");

		clickConfirmButton();

		// A sent deposit ends one of two ways, so both are waited for. Waiting only for the good
		// one turns a refusal into thirty seconds of silence and then a timeout that says
		// nothing about why.
		Wait.until(() -> submittedOrderStatus.isVisible() || !refusalShowing().isEmpty(), 30);

		stopIfRefused(deposit, "confirming");

		if (!submittedOrderStatus.isVisible()) {
			throw new IllegalStateException("The platform was sent " + deposit
					+ " and neither accepted it nor said why");
		}

		System.out.println("Submitted Order Status: " + submittedOrderStatus.innerText());
	}

	/**
	 * Stops the deposit if the platform has something to say about it.
	 *
	 * The platform can decline at any point and for any combination: a Malaysian ringgit
	 * deposit through Duitnow was answered "This service not available please try again with
	 * other methods", and there is nothing about that answer particular to Malaysia, to an
	 * e-wallet, or to Duitnow. Any currency, any channel and any payment name can be answered
	 * the same way, so every step of every deposit is checked rather than the one combination
	 * that happened to show it first.
	 *
	 * The message names the combination and the step, so a failure reads as the platform's
	 * decision about a particular deposit rather than as a broken test.
	 */
	private void stopIfRefused(String deposit, String afterDoing) {

		String refusal = refusalShowing();

		if (!refusal.isEmpty()) {
			throw new IllegalStateException("The platform would not take " + deposit
					+ ", after " + afterDoing + ": " + refusal);
		}
	}

	/**
	 * Whatever a dialog is saying, if one is up. Empty when nothing is.
	 *
	 * Several nodes answer to the dialog's id and the outermost is not always the one holding
	 * the words - taking the first gave "Deposit", the dialog's title and nothing else - so the
	 * one with most to say is the one worth reporting.
	 */
	private String refusalShowing() {

		// A picker is a dialog too, and its rows are not a message. Reading one as a refusal
		// would stop a perfectly good deposit and quote a list of wallets as the reason.
		if (aPickerIsOpen()) {
			return "";
		}

		String said = "";

		try {
			for (Locator part : page.locator("xpath=//*[@id='root.dialog']").all()) {

				String text = part.innerText().replaceAll("\\s+", " ").trim();

				if (text.length() > said.length()) {
					said = text;
				}
			}
		} catch (Exception nothingShowing) {
			return "";
		}

		return withoutBoilerplate(said);
	}

	/**
	 * The dialog's own words, with the parts it always says taken off the front.
	 *
	 * Every one of these dialogs opens with its title and a standing notice about not
	 * refreshing the page, which pushes the sentence that matters to the end of a long line in
	 * the failure report. What is left is the platform's actual answer. Anything that does not
	 * match is left exactly as it was found, so a change to the wording costs nothing worse
	 * than the longer message this used to print.
	 */
	private String withoutBoilerplate(String said) {

		String trimmed = said
				.replace("Deposit If your operation is still in progress."
						+ " Avoid refreshing or closing this page.", "")
				.trim();

		return trimmed.isEmpty() ? said : trimmed;
	}

	/** Whether an open dialog is one of the form's own pickers rather than a message. */
	private boolean aPickerIsOpen() {

		try {
			return page.locator("xpath=//*[@id='root.dialog']/div/div[2]/div/div/div/div/div[2]/div").count() > 0
					|| page.locator("xpath=//*[@id='root.dialog']/div/div[2]/div/div/div/div/div/div[3]/div").count() > 0;
		} catch (Exception nothingOpen) {
			return false;
		}
	}

	/**
	 * Sends the deposit, once the form is ready to send it.
	 *
	 * The wait used to be on the order summary, found by its path through the form. That holds
	 * for a bank or e-wallet deposit and not for a USDT one, which carries an extra pay-address
	 * section and puts the summary somewhere else: the US dollar deposit sat on a filled form
	 * with Confirm lit up and failed waiting thirty seconds for a summary that was on the screen
	 * the whole time. Confirm coming alive is the same moment and the form says it directly.
	 */
	private void clickConfirmButton() {

		confirmButton.waitFor();

		Wait.untilOrFail(confirmButton::isEnabled, 20, "The deposit form never offered to confirm");

		try {
			confirmButton.click();
			Thread.sleep(5000);
		} catch (Exception e) {
			confirmButton.dispatchEvent("click");
		}
	}

	/** Closes an open picker without choosing from it, leaving the form as it was. */
	private void dismissList() {

		try {
			page.keyboard().press("Escape");
		} catch (Exception alreadyClosed) {
			// Nothing open is nothing to close.
		}
	}
}
