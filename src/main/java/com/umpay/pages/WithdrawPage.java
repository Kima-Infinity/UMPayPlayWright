package com.umpay.pages;

import com.umpay.utility.PlatformRefusal;
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

/*
 * Built to mirror DepositPage. The withdraw screen is served from a chunk that
 * only loads after login, so the locators below follow the deposit ones rather
 * than having been read off the live page. Check them against the running
 * application on the first run and correct any that differ.
 */
public class WithdrawPage {

	Page page;
	
	/** How a row asks for the New Account half of Receive Information rather than a template. */
	private static final String NEW_ACCOUNT = "New Account";

	/** The security code that authorises a withdraw. Override with -Dumpay.pin=... */
	private static final String PIN = System.getProperty("umpay.pin", "1111");

	/**
	 * What the chosen wallet held when it was chosen, in the form's own words.
	 *
	 * Read from the picker row on the way past, because that is the only moment the balance and
	 * the wallet it belongs to are on the screen together.
	 */
	private String chosenBalance = "";

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
	 * The New Account side of Receive Information, and the boxes it asks for.
	 *
	 * The form asks for different things in every currency, and the pickers are anchored on
	 * their own labels rather than on a path through the form - the same lesson the currency
	 * picker taught. What each wallet wants, read off the live form:
	 *
	 *   HKD  choose E-Wallet, Bank or USDT; E-Wallet asks for an FPS account and payer name
	 *   PHP  Bank, then one of ninety-odd banks; asks for an account name and number
	 *   BDT  USDT / USDT-TRC 20 throughout; asks for a wallet address
	 *   MXN  Bank, then a bank chosen from a list with a search box
	 *   BRL  E-Wallet / CPF throughout; asks for a CPF tax number and account number
	 *   VND  choose E-Wallet or Bank; E-Wallet offers no names at all
	 *   IDR  Bank, then DANA, OVO and the rest; asks for an account name and number
	 *   THB  choose Bank or USDT; the banks ask for an account number and name
	 *   USD  USDT / USDT-TRC 20 throughout; asks for a wallet address
	 */
		private final Locator newAccountTab;


		private final Locator paymentTypeButton;


		private final Locator paymentNameButton;


		private final Locator fpsAccountField;


		private final Locator fpsReceiverNameField;


		private final Locator accountNameField;


		private final Locator accountNumberField;


		private final Locator walletNumberField;


		private final Locator taxNumberField;


		private final Locator networkNumberField;


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
		this.newAccountTab = page.locator("xpath=//button[normalize-space()='New Account']");
		this.paymentTypeButton = page.locator(
				"xpath=//*[normalize-space()='Payment Type']/following::button[1]").first();
		this.paymentNameButton = page.locator(
				"xpath=//*[normalize-space()='Payment Name']/following::button[1]").first();
		this.fpsAccountField = page.locator("[id='account']");
		this.fpsReceiverNameField = page.locator("[id='FPSReceiverName']");
		this.accountNameField = page.locator("[id='accountName']");
		this.accountNumberField = page.locator("[id='accountNumber']");
		this.walletNumberField = page.locator("[id='walletNumber']");
		this.taxNumberField = page.locator("[id='taxNumber']");
		this.networkNumberField = page.locator("[id='networkNumber']");
		this.confirmButton = page.locator("xpath=//button[normalize-space()='Confirm']");
		this.submittedOrderStatus = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div/div/div/div[1]/h5");
	}

	/**
	 * Raises a withdraw, by whichever half of Receive Information the row asks for.
	 *
	 * The form offers two ways to say where the money is going: From Template, which picks a
	 * payout account already saved, and New Account, which types a fresh one in. They are
	 * different code paths in the application - different pickers, different fields, different
	 * validation - so a suite that only ever uses one of them covers half the screen.
	 */
	public void submitWithdraw(String amount, String currency, String paymentType, String paymentName,
							   String fpsAccount, String fpsReceiverName,
							   String accountName, String accountNumber,
							   String walletName, String walletNumber,
							   String receiveInformation) {

		String withdraw = "the " + currency + " withdraw";

		chooseCurrency(currency);
		stopIfRefused(withdraw, "choosing the wallet");

		// MIN rather than a figure, for the same reason the deposits read it that way: every
		// wallet's minimum is its own and some are a conversion of a limit held elsewhere - the
		// US dollar wallet asked for 19.77 the day this was written - so a number in the sheet
		// is a number that goes stale.
		String toWithdraw = "MIN".equalsIgnoreCase(amount) ? statedMinimum() : amount;

		stopIfTheWalletIsTooEmpty(currency, toWithdraw);

		System.out.println("Withdrawing " + toWithdraw + " " + currency + " to " + paymentName
				+ "  (the wallet holds " + chosenBalance
				+ ", and the form states: " + statedLimitNote() + ")");

		enterAmount(toWithdraw);
		stopIfRefused(withdraw, "entering the amount");

		if (NEW_ACCOUNT.equalsIgnoreCase(receiveInformation)) {

			withdraw = withdraw + " to a new " + paymentType + " / " + paymentName + " account";

			typeInANewAccount(paymentType, paymentName, fpsAccount, fpsReceiverName,
					accountName, accountNumber, walletNumber);

		} else {
			withdraw = withdraw + " to " + selectSavedPaymentAccount(paymentName);
		}

		stopIfRefused(withdraw, "giving the receive information");

		clickConfirmButton();
		stopIfRefused(withdraw, "confirming");

		enterPinIfAsked(withdraw);

		stopUnlessAnOrderWasRaised(withdraw);

		reportSubmittedStatus();

		closeSubmittedDialog();
	}

	/**
	 * Stops the withdraw if the platform has something to say about it.
	 *
	 * The platform can decline at any point and for any combination - a deposit was answered
	 * "This service not available please try again with other methods", and a withdraw can be
	 * answered the same way for any currency and any payout account - so every step is checked
	 * rather than the one that happened to show it first. The reading itself is shared with the
	 * other flows; only the naming of the transaction belongs to this page.
	 */
	private void stopIfRefused(String withdraw, String afterDoing) {

		PlatformRefusal.stopIfRefused(page, withdraw, afterDoing);
	}

	/**
	 * The address currently in the bar.
	 *
	 * The navigation step reports it when the form does not open, which is the one thing that
	 * says whether the click went nowhere or went somewhere unexpected.
	 */
	public String getCurrentUrl() {

		return page.url();
	}

	/** True once the withdraw form is on screen and ready to be filled in. */
	public boolean isDisplayed() {

		try {
			return amountField.isVisible() && currencyButton.isVisible();
		} catch (Exception notThere) {
			return false;
		}
	}

	/**
	 * Waits for the withdraw form to open, saying whether it did.
	 *
	 * The sidebar click is not always the navigation it looks like: a run of ten withdraws had
	 * two scenarios where the click on Withdraw resolved to the sidebar entry, reported the
	 * element visible and stable, and still left the page on the dashboard - the same swallowed
	 * click the Convert page already retries around. Answering rather than throwing lets the
	 * step try again instead of failing thirty seconds later on an amount box that was never
	 * going to appear.
	 */
	public boolean waitUntilReady(int timeoutSeconds) {

		return Wait.until(this::isDisplayed, timeoutSeconds);
	}

	/**
	 * Goes straight to the withdraw form by its own address.
	 *
	 * The last resort when the sidebar will not open it. The dashboard sometimes keeps a
	 * full-screen loading mask over the navigation - "bg-gray-200 cursor-not-allowed h-screen",
	 * which Playwright reports as intercepting pointer events - and sometimes a modal on top of
	 * that, and a click that cannot land leaves the run on the dashboard with nothing to say
	 * about why. The address is the same one the sidebar would have gone to, so a scenario
	 * about withdrawing money is not lost to a sidebar that would not take a click.
	 */
	public void open() {

		String origin = page.url().replaceAll("(https?://[^/]+).*", "$1");

		page.navigate(origin + "/withdraw");
	}

	/**
	 * Opens the wallet picker and chooses the wallet the money is to come out of.
	 *
	 * A wallet is chosen whatever it holds. It used to skip any wallet reading zero, which
	 * conflated two quite different things: a currency this account has no wallet for, and a
	 * wallet that is simply empty. Both came back as "no wallet with a balance was offered",
	 * and an empty wallet is worth a better answer than that - what it holds and what the
	 * withdraw needed. That is what the balance check in submitWithdraw says.
	 */
	public void chooseCurrency(String currency) {

		for (Locator wallet : openWalletList()) {

			String code;

			// Only the reading is guarded. Choosing the wallet is not: an error there is the
			// answer to what went wrong rather than a row to skip past.
			try {
				code = wallet.locator("xpath=.//div[1]/div[2]/p").innerText().trim();
			} catch (Exception notAWalletRow) {
				continue;
			}

			if (code.equals(currency)) {

				// Read while the row is in front of us. Asking again afterwards means opening the
				// picker a second time over a half-filled form, and the balance is wanted on the
				// very next line to say whether this withdraw can be raised at all.
				chosenBalance = balanceOn(wallet);

				click(wallet, "the " + currency + " wallet");
				settleLimits(currency);
				return;
			}
		}

		throw new IllegalStateException("This account has no " + currency + " wallet. The withdraw"
				+ " currency list offers " + walletBalances().keySet());
	}

	/**
	 * Every wallet the currency list offers, in the order shown, with the balance each holds.
	 *
	 * The list is not the platform's currencies - it is this account's wallets - so what it
	 * offers is a fact about the account and has to be read rather than assumed. The codes and
	 * balances come back as text; handing the rows themselves to a caller would let a step click
	 * one, which is the coupling the page object exists to prevent.
	 */
	public Map<String, String> walletBalances() {

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

	/** Types an amount and leaves it there, without submitting. */
	public void enterAmount(String amount) {

		FormInput.type(amountField, amount, "withdraw amount");
	}

	/**
	 * Stops the withdraw when the wallet does not hold what it would take.
	 *
	 * Asked before anything is filled in, because a wallet that cannot cover the withdraw has
	 * already decided the outcome: going on would fill in a form, submit it, and come back with
	 * whatever the platform says about it - which is a slower and less clear way of learning
	 * that the account is short. The failure names what the wallet holds and what was asked
	 * for, so a reader can see the size of the gap without opening the application.
	 */
	private void stopIfTheWalletIsTooEmpty(String currency, String amount) {

		double held = asANumber(chosenBalance, "the " + currency + " wallet's balance");
		double asked = asANumber(amount, "the amount to withdraw");

		if (held < asked) {
			throw new IllegalStateException("The " + currency + " wallet holds " + chosenBalance
					+ ", which is not enough to withdraw " + amount + " " + currency + ".");
		}
	}

	/**
	 * A figure the form states, as a number.
	 *
	 * Balances come with the wallet's own furniture around them - "HK$10273.16", "Rp1969141.78",
	 * "RM92603.81" - and none of it is worth parsing, so everything that is not a digit or a
	 * decimal point is dropped. A figure that will not read as a number is said so plainly
	 * rather than guessed at: quietly treating an unreadable balance as zero would fail every
	 * withdraw, and treating it as plenty would submit one the wallet cannot cover.
	 */
	private double asANumber(String figure, String what) {

		String digits = String.valueOf(figure).replace(",", "").replaceAll("[^0-9.]", "");

		try {
			return Double.parseDouble(digits);
		} catch (NumberFormatException notANumber) {
			throw new IllegalStateException("Could not read " + what + " from \"" + figure + "\"");
		}
	}

	/** What one wallet row of the picker says it holds. */
	private String balanceOn(Locator wallet) {

		try {
			return wallet.locator("xpath=.//div[2]/div/p[2]").innerText().trim();
		} catch (Exception notAWalletRow) {
			return "";
		}
	}

	/** Opens the wallet picker and hands back its rows. */
	private List<Locator> openWalletList() {

		// The form has to be on the screen before its wallet list can be opened: open it while
		// the page is still rendering and the list comes up empty, which reads as "this account
		// has no such wallet" and is not that at all.
		amountField.waitFor();

		currencyButton.waitFor();
		click(currencyButton, "the currency picker");

		return Wait.all(page.locator(
				"xpath=//*[@id=\"root.dialog\"]/div/div[2]/div/div/div/div/div[2]/div"));
	}

	/** Closes an open picker without choosing from it, leaving the form as it was. */
	private void dismissList() {

		try {
			page.keyboard().press("Escape");
			Wait.until(() -> !pickerIsOpen(), 5);
		} catch (Exception alreadyClosed) {
			// Nothing open is nothing to close.
		}
	}

	/**
	 * Waits for the amount box to be talking about the currency that was asked for.
	 *
	 * The same trap the deposit form set: the limits are fetched per wallet and arrive a moment
	 * after the wallet is chosen, so an amount typed too early is measured against the previous
	 * currency's band. Numbers alone cannot say whose they are - the form's own "Limit Min 100
	 * HKD" line can, so that is what is waited for. A form that never states one is not held up
	 * on account of it, since not every screen prints the line.
	 */
	private void settleLimits(String currency) {

		Wait.until(() -> {
			String note = statedLimitNote();
			return note != null && note.contains(currency);
		}, 20);
	}

	/**
	 * What the amount box says it will take, in the form's own words - "Limit Min 100 HKD".
	 *
	 * The only place on the form where a limit and the currency it belongs to are stated
	 * together, which is what makes it worth reading rather than the bare min attribute.
	 */
	public String statedLimitNote() {

		try {
			return page.locator("xpath=//*[contains(normalize-space(text()),'Limit Min')]/..")
					.first().innerText().replaceAll("\\s+", " ").trim();
		} catch (Exception notStatedYet) {
			return null;
		}
	}

	/**
	 * The browser's own verdict on the amount.
	 *
	 * The box is type=number with min and max, so an amount outside them never reaches the
	 * server and there is no banner to read - the message lives on the element, and it is the
	 * application's own wording rather than the browser's stock phrasing.
	 */
	public String amountValidationMessage() {

		return (String) amountField.evaluate("el => el.validationMessage");
	}

	/** Whether the browser considers the amount acceptable. */
	public boolean isAmountValid() {

		return (Boolean) amountField.evaluate("el => el.checkValidity()");
	}

	/** The upper bound the amount box enforces. */
	public String statedMaximum() {

		return amountField.getAttribute("max");
	}

	/** The lower bound the amount box enforces, as the form states it. */
	public String statedMinimum() {

		return amountField.getAttribute("min");
	}

	/**
	 * What the amount box is actually holding, which is not always what was typed at it.
	 *
	 * The box is type=number and the form works on it as it is typed, so letters, a zero and a
	 * negative never land in it at all - it is left empty rather than filled with something
	 * wrong. A scenario that only asked whether the box was valid could not tell the difference
	 * between an amount that was refused and one that was never entered.
	 */
	public String amountBoxHolds() {

		try {
			return String.valueOf(amountField.inputValue()).trim();
		} catch (Exception notThere) {
			return "";
		}
	}

	/** Types at the amount box the way a person would, key by key. */
	public void typeAmount(String amount) {

		amountField.fill("");

		Wait.sleep(300);

		try {
			amountField.pressSequentially(amount,
					new Locator.PressSequentiallyOptions().setDelay(60));
		} catch (Exception theBoxWouldNotTakeIt) {
			// A box that refuses the keystrokes outright has refused the amount, which is what
			// the scenario is asking about - it is read back rather than thrown from here.
		}

		Wait.sleep(800);
	}

	/**
	 * True while the form will let the withdraw be confirmed.
	 *
	 * Confirm is disabled rather than hidden until the form has everything it needs, so this is
	 * what says whether a withdraw could be raised - not whether the button is on the screen.
	 */
	public boolean canConfirm() {

		try {
			return confirmButton.first().isVisible() && confirmButton.first().isEnabled();
		} catch (Exception notThere) {
			return false;
		}
	}

	/** What the wallet held when it was chosen, in the form's own words. */
	public String balanceOfChosenWallet() {

		return chosenBalance;
	}

	/** What the wallet held when it was chosen, as a number. */
	public double balanceOfChosenWalletAsANumber() {

		return asANumber(chosenBalance, "the chosen wallet's balance");
	}

	/** Chooses a payout account this account already holds, without submitting anything. */
	public void chooseSavedPaymentAccount(String named) {

		selectSavedPaymentAccount(named);
	}

	/**
	 * Presses Confirm, gives the PIN if the platform asks for one, and says what it made of it.
	 *
	 * Hands back the platform's own words where it refused and "" where it did not. A refusal is
	 * the expected answer in the negative scenarios, so it is reported rather than thrown - the
	 * throwing version is what submitWithdraw wants, where a refusal means the withdraw failed.
	 */
	public String confirmAndSeeWhatThePlatformSays(String describedAs) {

		try {
			clickConfirmButton();
			enterPinIfAsked(describedAs);

		} catch (Exception refused) {
			return String.valueOf(refused.getMessage()).replaceAll("\s+", " ").trim();
		}

		return refusalShowing();
	}

	/** True once the platform has an order to show for what was confirmed. */
	public boolean anOrderWasRaised() {

		return Wait.until(() -> {
			try {
				return orderRaised() || submittedOrderStatus.isVisible();
			} catch (Exception notYet) {
				return false;
			}
		}, 12);
	}

	/** What the platform said, if it answered with one of its own dialogs. */
	public String refusalShowing() {

		return PlatformRefusal.showing(page);
	}

	/**
	 * Types a fresh payout account in, rather than choosing one already saved.
	 *
	 * The form is asked what it wants at every step. The payment type and name pickers are
	 * disabled in the wallets that offer no choice - BDT and USD are USDT / USDT-TRC 20 and
	 * nothing else - and asking for what is already settled on counts as choosing it, while a
	 * mismatch is worth failing for: it means the row names a channel the form no longer
	 * offers.
	 *
	 * Save Template is deliberately left alone. Ticking it would add a payout account to the
	 * test account on every run, and the templates it holds are what the From Template rows
	 * read - so the two halves of this feature would slowly rewrite each other's data.
	 */
	private void typeInANewAccount(String paymentType, String paymentName,
								   String fpsAccount, String fpsReceiverName,
								   String accountName, String accountNumber,
								   String walletNumber) {

		click(newAccountTab, "the New Account tab");

		choosePaymentType(paymentType);
		choosePaymentName(paymentName);

		fillInWhatIsAsked(fpsAccount, fpsReceiverName, accountName, accountNumber, walletNumber);
	}

	/**
	 * Chooses the payment type, or checks the one the form has settled on by itself.
	 *
	 * A wallet with a single channel shows it and disables the picker. Asking for it by name is
	 * still the right thing for a row to do, so that counts as chosen.
	 */
	private void choosePaymentType(String paymentType) {

		paymentTypeButton.waitFor();

		if (!paymentTypeButton.isEnabled()) {

			String settledOn = paymentTypeButton.innerText().trim();

			if (settledOn.equalsIgnoreCase(paymentType)) {
				return;
			}

			throw new IllegalStateException("The only payment type on offer is " + settledOn
					+ ", but the row asks for " + paymentType);
		}

		click(paymentTypeButton, "the payment type picker");

		chooseFromTheOpenList(paymentType, "payment type");
	}

	/**
	 * Chooses the payment name under the chosen type - a bank, a wallet or a network.
	 *
	 * The names are fetched after the type is chosen, so the picker is waited on rather than
	 * read while it is still disabled and empty. Some lists are long enough to carry their own
	 * search box - Mexico's does - and typing into it is how the wanted row is reached.
	 */
	private void choosePaymentName(String paymentName) {

		if (paymentNameButton.count() == 0) {
			throw new IllegalStateException("The form asks for no payment name at all under this"
					+ " payment type, so \"" + paymentName + "\" cannot be chosen");
		}

		paymentNameButton.waitFor();

		Wait.until(paymentNameButton::isEnabled, 10);

		if (!paymentNameButton.isEnabled()) {

			String settledOn = paymentNameButton.innerText().trim();

			if (settledOn.equalsIgnoreCase(paymentName)) {
				return;
			}

			throw new IllegalStateException("The form settled on " + settledOn
					+ " and offers no choice, but the row asks for " + paymentName);
		}

		click(paymentNameButton, "the payment name picker");

		searchTheOpenList(paymentName);

		chooseFromTheOpenList(paymentName, "payment name");
	}

	/** Types into a picker's own search box, for the lists too long to scroll. */
	private void searchTheOpenList(String wanted) {

		try {
			Locator search = page.locator("xpath=//*[@id=\"root.dialog\"]//input").first();

			if (search.count() > 0 && search.isVisible()) {
				search.fill(wanted);
				Wait.sleep(1500);
			}
		} catch (Exception noSearchBox) {
			// A list short enough to show everything does not need one.
		}
	}

	/** Clicks the row of the open picker that reads as the wanted one. */
	private void chooseFromTheOpenList(String wanted, String what) {

		for (Locator row : Wait.all(page.locator(
				"xpath=//*[@id=\"root.dialog\"]/div/div[2]/div/div/div/div/div[2]/div"
				+ " | //*[@id=\"root.dialog\"]/div/div[2]/div/div/div/div/div/div[3]/div"))) {

			try {
				if (!row.innerText().replaceAll("\\s+", " ").trim().startsWith(wanted)) {
					continue;
				}
			} catch (Exception keepLooking) {
				continue;
			}

			click(row, "the " + wanted + " " + what);

			if (!pickerIsOpen()) {
				return;
			}
		}

		if (!pickerIsOpen()) {
			return;
		}

		throw new IllegalStateException("The " + what + " list does not offer " + wanted
				+ ". It offers: " + templatesOnOffer());
	}

	/**
	 * Fills in whichever boxes the chosen channel is asking for.
	 *
	 * Asked rather than switched on, the same way the deposits are. The same payment type wants
	 * different things in different currencies - Hong Kong's E-Wallet wants an FPS account and
	 * a payer name, Brazil's wants a CPF and an account number, Indonesia's and Thailand's want
	 * an account name and number, and the USDT wallets want an address - so a switch on the
	 * type fills the wrong boxes in the first currency it has not met.
	 */
	private void fillInWhatIsAsked(String fpsAccount, String fpsReceiverName,
								   String accountName, String accountNumber,
								   String walletNumber) {

		fillIfAsked(fpsAccountField, fpsAccount, "the FPS account");
		fillIfAsked(fpsReceiverNameField, fpsReceiverName, "the FPS receiver name");
		fillIfAsked(accountNameField, accountName, "the account name");
		fillIfAsked(accountNumberField, accountNumber, "the account number");
		fillIfAsked(taxNumberField, accountName, "the CPF tax number");
		fillIfAsked(walletNumberField, walletNumber, "the wallet number");
		fillIfAsked(networkNumberField, walletNumber, "the wallet address");
	}

	/** Fills one box, if the form is showing it. */
	private void fillIfAsked(Locator field, String value, String what) {

		try {
			if (field.count() > 0 && field.first().isVisible()) {
				field.first().fill(value);
				System.out.println("Filled in " + what + ": " + value);
			}
		} catch (Exception notAsked) {
			// A box that is not on the form is a box this channel does not want.
		}
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
	private String selectSavedPaymentAccount(String wanted) {

		click(fromTemplateTab, "the From Template tab");
		click(selectPaymentAccountButton, "Select Payment Account");

		String paymentName = "SAVED".equalsIgnoreCase(wanted) ? theAccountThisWalletHasSaved() : wanted;

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
				return paymentName;
			}

			label = label.locator("xpath=" + "..");
		}

		throw new IllegalStateException("The payment account picker stayed open after choosing \""
				+ paymentName + "\". Selecting it is what fills the form in, so carrying on would"
				+ " submit nothing and report success.");
	}

	/**
	 * The payout account this wallet has saved, for a row that names SAVED rather than one.
	 *
	 * Which templates an account has saved is a fact about the account rather than about the
	 * test - HKD has three, the Indonesian wallet has DANA, and several wallets have none at
	 * all - so a row that has no particular account to prove says SAVED and takes what the
	 * wallet offers. A wallet with nothing saved cannot pay out at all, and says so: that is a
	 * gap in the account rather than a fault in the run, and it is worth reporting as such.
	 */
	private String theAccountThisWalletHasSaved() {

		String offered = templatesOnOffer();

		if (offered.contains("Please add your payment account")) {
			throw new IllegalStateException("This wallet has no saved payout account, so a withdraw"
					+ " cannot be raised from one. The form offers: " + offered);
		}

		return offered.split(", ")[0];
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
	private void enterPinIfAsked(String withdraw) {

		Locator dialog = null;

		long deadline = System.currentTimeMillis() + 15000;

		while (System.currentTimeMillis() < deadline && dialog == null) {
			for (Locator candidate : Wait.all(page.locator("div[role='dialog']"))) {
				if (candidate.isVisible() && candidate.innerText().toUpperCase().contains("PIN")) {
					dialog = candidate;
					break;
				}
			}

			// A refused withdraw is never asked to authorise itself, so waiting the full fifteen
			// seconds for a PIN dialog that is not coming only delays a failure that is already
			// decided - and the run then walked past the refusal and reported a withdraw that
			// never happened.
			stopIfRefused(withdraw, "confirming");

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

		// An authorised withdraw ends one of two ways, so both are waited for. A fixed sleep has
		// to be long enough for the slowest good case, which is exactly the wait a refusal does
		// not need: the platform has already answered and the run should say so.
		Locator pinDialog = dialog;

		Wait.until(() -> !PlatformRefusal.showing(page).isEmpty() || orderRaised()
				|| !pinDialog.isVisible(), 20);

		stopIfRefused(withdraw, "authorising with the PIN");

		// No order and nothing said. Left alone this is the quiet way a run reports a withdraw
		// that never happened: every earlier run stopped at this dialog and still passed.
		if (!orderRaised() && pinDialog.isVisible()) {
			throw new IllegalStateException("The PIN was entered and " + withdraw + " was neither"
					+ " raised nor refused - the dialog is still up and reads: "
					+ pinDialog.innerText().replaceAll("\\s+", " ").trim());
		}
	}

	/**
	 * Whether the platform has an order for this withdraw.
	 *
	 * It says so in the address bar: a raised withdraw lands on /withdraw?orderId=...&amp;
	 * orderStatus=... This is worth asking separately from the dialog, because the PIN dialog
	 * has been seen still sitting there over an order that went through perfectly well.
	 */
	private boolean orderRaised() {

		return page.url().contains("orderId=");
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
	 * Stops unless the platform has an order to show for the withdraw.
	 *
	 * Without this a withdraw that quietly does nothing passes: the refusal check finds no
	 * message because none was given, the PIN dialog closes as it always does, and the status
	 * is logged rather than asserted - so the run reports success on the strength of having
	 * filled a form in. Every withdraw that has gone through says so in one of two ways, and
	 * either will do: the receipt names a status, and the address carries the order's id.
	 */
	private void stopUnlessAnOrderWasRaised(String withdraw) {

		boolean raised = Wait.until(() -> orderRaised() || submittedOrderStatus.isVisible(), 20);

		if (!raised) {
			throw new IllegalStateException("The platform raised no order for " + withdraw
					+ " and said nothing about why. The page is at " + page.url());
		}
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
			// The address is worth printing here: a raised withdraw lands on /withdraw?orderId=...,
			// so it says whether an order exists even when the status element cannot be found.
			System.out.println("The withdraw was submitted but the order status element could not be"
					+ " read. The page is at " + page.url());
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
