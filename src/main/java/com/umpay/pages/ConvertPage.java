package com.umpay.pages;

import com.umpay.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import com.umpay.utility.FormInput;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Currency conversion, reached from the Convert item in the sidebar and served
 * at /v2/exchange-rate under the title "UMPay | Convert".
 *
 * The screen is a single form: an amount, a wallet to take it from, a wallet to
 * put it into, and a live rate between them. Submitting raises a "Convert
 * successfully!" dialog and the source balance drops immediately.
 */
public class ConvertPage {

	Page page;
	
		private final Locator amountField;


	/**
	 * The two wallet pickers, in the order the form reads: [0] is the wallet being
	 * converted from, [1] the one under "Convert To".
	 *
	 * They share every attribute - same classes, same theme, no id between them -
	 * so position is the only thing that tells them apart. The unusual width class
	 * is what separates them from the other link-styled buttons on the page.
	 *
	 * Deliberately a locator rather than a PageFactory list: such a list re-queries
	 * the page on every single call, so a size() of two followed by a get(1) can
	 * find only one element in between - which is exactly what happens while the
	 * currency dialog is open. Callers take one snapshot and work from that.
	 */
	private static final String WALLET_PICKERS =
			"xpath=//button[@theme='link' and contains(@class,'w-[140px]')]";

	/** Fills the amount with everything in the source wallet. */
		private final Locator maxButton;


	/** The only submit button on the page; the sidebar's Convert is a nav item. */
		private final Locator convertButton;


		private final Locator rateDisplay;


	/** "Based on above rate, you will get" is followed by the converted figure. */
		private final Locator convertedAmount;


	/** The source wallet's balance, shown under its picker. */
		private final Locator sourceBalance;


		private final Locator successMessage;


		private final Locator okButton;


		private final Locator historyButton;


	public ConvertPage(Page ldriver) {

		this.page = ldriver;
		this.amountField = page.locator("[name=\'amount\']");
		this.maxButton = page.locator("xpath=//button[normalize-space()='Max']");
		this.convertButton = page.locator("xpath=//button[@type='submit']");
		this.rateDisplay = page.locator("xpath=//button[contains(normalize-space(),'Rate:')]");
		this.convertedAmount = page.locator("xpath=//*[contains(text(),'you will get')]/following::*[1]");
		this.sourceBalance = page.locator("xpath=//*[normalize-space()='Balance']/following::*[1]");
		this.successMessage = page.locator("xpath=//*[contains(text(),'Convert successfully')]");
		this.okButton = page.locator("xpath=//button[normalize-space()='Ok']");
		this.historyButton = page.locator("xpath=//button[normalize-space()='History']");
	}

	/**
	 * The address currently in the bar.
	 *
	 * The navigation step reports it when the form does not open, which is the one
	 * thing that says whether the click went nowhere or went somewhere unexpected.
	 */
	public String getCurrentUrl() {

		return page.url();

	}

	/** True once the conversion form is on screen. */
	public boolean isDisplayed() {

		try {
			return amountField.isVisible();
		} catch (Exception notThere) {
			return false;
		}
	}

	/**
	 * Waits until the form is not just present but filled in.
	 *
	 * The amount field renders before the wallets do. Acting on the form at that
	 * point finds a picker with no currency in it yet, decides it needs changing,
	 * and clicks into a page that is still assembling itself - which is how this
	 * first failed, reporting that HKD was not on offer when it simply had not
	 * been drawn yet.
	 */
	public boolean waitUntilReady(int timeoutSeconds) {

		// Polled rather than waited on: the condition is about two elements and their
		// contents together, which no single locator state describes.
		long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

		while (System.currentTimeMillis() < deadline) {

			try {
				List<Locator> pickers = pickers();

				if (isDisplayed()
						&& pickers.size() == 2
						&& !pickers.get(0).innerText().isBlank()
						&& !pickers.get(1).innerText().isBlank()) {
					return true;
				}
			} catch (Exception stillDrawing) {
				// The form is mid-render; look again on the next turn.
			}

			sleep(250);
		}

		return false;
	}

	/** One snapshot of the wallet pickers, safe to index into. */
	private List<Locator> pickers() {

		return Wait.all(page.locator(WALLET_PICKERS));
	}

	/** Picks the wallet to convert from. */
	public void selectFromCurrency(String currencyCode) {

		selectWallet(0, currencyCode, "from");
	}

	/** Picks the wallet to convert into. */
	public void selectToCurrency(String currencyCode) {

		selectWallet(1, currencyCode, "to");
	}

	/**
	 * Opens one of the wallet pickers and chooses a currency by its code.
	 *
	 * The picker is a "Select Currency" dialog listing every wallet with its
	 * balance. Each row names the currency in its own element, which is what makes
	 * a code like PHP addressable without depending on the row's position.
	 */
	private void selectWallet(int index, String currencyCode, String which) {

		if (currencyCode == null || currencyCode.isBlank()) {
			return;
		}

		if (!waitUntilReady(20)) {
			throw new IllegalStateException("The Convert form did not finish loading its wallets");
		}

		Locator picker = pickers().get(index);

		if (picker.innerText().replace("\n", " ").contains(currencyCode)) {
			System.out.println("The " + which + " wallet is already " + currencyCode);
			return;
		}

		click(picker, "the " + which + " wallet picker");

		try {
			page.locator("xpath=//*[normalize-space()='Select Currency']").first().waitFor();
		} catch (com.microsoft.playwright.TimeoutError neverOpened) {
			throw new IllegalStateException("The Select Currency list did not open for the "
					+ which + " wallet");
		}

		Locator option = page.locator("xpath=//div[@id='root.dialog']//*[normalize-space()='"
				+ currencyCode + "']").first();

		try {
			option.waitFor();
		} catch (com.microsoft.playwright.TimeoutError notListed) {
			throw new IllegalStateException("The currency " + currencyCode
					+ " was not offered for the " + which + " wallet. On offer: " + currenciesOnOffer());
		}

		click(option, currencyCode + " in the currency list");

		// The dialog closes and the rate is fetched for the new pair.
		long settled = System.currentTimeMillis() + 20_000L;

		while (System.currentTimeMillis() < settled) {

			try {
				List<Locator> current = pickers();

				if (current.size() > index && current.get(index).innerText().contains(currencyCode)) {
					break;
				}
			} catch (Exception stillChanging) {
				// The dialog is still closing; look again.
			}

			sleep(250);
		}

		System.out.println("Converting " + which + " " + currencyCode);
	}

	/**
	 * The currency codes the open picker is listing, for when the wanted one is not
	 * among them. A three letter row in the dialog is a code; the country names and
	 * balances beside them are longer.
	 */
	private String currenciesOnOffer() {

		StringBuilder codes = new StringBuilder();

		for (Locator row : Wait.all(page.locator("xpath=//div[@id='root.dialog']//p"))) {

			String text = row.innerText().trim();

			if (text.length() == 3 && text.equals(text.toUpperCase())) {
				codes.append(codes.length() == 0 ? "" : ", ").append(text);
			}
		}

		return codes.length() == 0 ? "nothing" : codes.toString();
	}

	public void enterAmount(String amount) {

		// clear() plus sendKeys was the same shape that dropped the withdraw amount: clear()
		// does not reliably raise the change event a controlled component listens for, and
		// nothing checked the field afterwards. Convert asserts on the balance falling, so a
		// half-typed amount here would convert the wrong sum rather than simply fail.
		FormInput.type(amountField, amount, "convert amount");

		System.out.println("Amount to convert: " + amount);
	}

	/** "Rate: / 1 HKD = 7.49043899 PHP", as shown. */
	public String getRate() {

		try {
			return rateDisplay.innerText().replace("\n", " ").trim();
		} catch (Exception notThere) {
			return "";
		}
	}

	/** What the form says the conversion will produce, such as "7.49 PHP". */
	public String getConvertedAmount() {

		try {
			return convertedAmount.innerText().trim();
		} catch (Exception notThere) {
			return "";
		}
	}

	/**
	 * Waits for the form to quote a figure for the amount just typed.
	 *
	 * The quote is fetched after the typing settles, so reading it straight away
	 * returns the 0.00 the form was showing beforehand. Waiting for it also means
	 * the form is done recalculating before anything is submitted.
	 */
	public String waitForQuote(int timeoutSeconds) {

		boolean quoted = Wait.until(() -> {
			String quote = getConvertedAmount();
			return !quote.isBlank() && !quote.startsWith("0.00");
		}, timeoutSeconds);

		if (!quoted) {
			System.out.println("The form never quoted a converted amount");
		}

		return getConvertedAmount();
	}

	/** The source wallet balance as displayed, such as "HK$10288.16". */
	public String getSourceBalance() {

		try {
			sourceBalance.waitFor();
			return sourceBalance.innerText().trim();
		} catch (Exception notThere) {
			return "";
		}
	}

	/** A number, with or without thousands separators and a decimal part. */
	private static final Pattern FIRST_NUMBER = Pattern.compile("[0-9][0-9,]*(?:\\.[0-9]+)?");

	/**
	 * The source balance as a number that two readings can be compared by.
	 *
	 * Only the first number in the text is taken. Depending on how the page has
	 * rendered, the element holding the balance sometimes carries the Max and
	 * Minimum wording after it, and deleting every non-digit from that ran the
	 * balance and the minimum together into "10286.161.00".
	 */
	public BigDecimal getSourceBalanceAmount() {

		String shown = getSourceBalance();

		Matcher amount = FIRST_NUMBER.matcher(shown);

		if (!amount.find()) {
			throw new IllegalStateException("Could not read a balance from \"" + shown + "\"");
		}

		return new BigDecimal(amount.group().replace(",", ""));
	}

	public void submit() {

		convertButton.waitFor();

		click(convertButton, "the Convert button");
	}

	/**
	 * Waits for the outcome dialog and returns what it said, or an empty string if
	 * nothing appeared within the timeout.
	 */
	public String waitForSuccessMessage(int timeoutSeconds) {

		try {
			successMessage.waitFor(new Locator.WaitForOptions().setTimeout(timeoutSeconds * 1000));

			return successMessage.innerText().trim();

		} catch (com.microsoft.playwright.TimeoutError noDialog) {
			return "";
		}
	}

	/** Dismisses the success dialog so the form underneath can be read again. */
	public void acknowledgeSuccess() {

		try {
			okButton.waitFor();
			click(okButton, "the Ok button");
			okButton.waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));
		} catch (Exception alreadyGone) {
			System.out.println("No success dialog left to dismiss");
		}
	}

	/**
	 * Waits for the source balance to move off what it was before the conversion.
	 *
	 * The application updates it as soon as the conversion lands, so this is what
	 * proves money actually moved rather than only that a dialog said so.
	 */
	public BigDecimal waitForBalanceBelow(BigDecimal before, int timeoutSeconds) {

		// Let the caller compare and report; the reading is returned either way.
		Wait.until(() -> getSourceBalanceAmount().compareTo(before) < 0, timeoutSeconds);

		return getSourceBalanceAmount();
	}

	/** Clicks, falling back to JavaScript when the normal click is intercepted. */
	private void click(Locator element, String what) {

		try {
			element.waitFor();
			element.click();
		} catch (Exception intercepted) {
			System.out.println("Falling back to a JavaScript click on " + what);
			element.dispatchEvent("click");
		}
	}

	private void sleep(long millis) {

		try {
			Thread.sleep(millis);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}
}
