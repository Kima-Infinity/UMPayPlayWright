package com.umpay.pages;

import com.umpay.utility.PlatformRefusal;
import com.umpay.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import com.umpay.utility.FormInput;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
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
	 * Every wallet the form offers to convert between, in the order the picker lists them.
	 *
	 * Read rather than assumed, for the same reason the deposit and withdraw lists are: the
	 * picker holds this account's wallets, not the platform's currencies, so a run that walks
	 * every pair has to ask what the pairs are. The codes come back as text - handing the rows
	 * themselves to a step would let it click one, which is the coupling the page object exists
	 * to prevent.
	 */
	public List<String> walletsOffered() {

		if (!waitUntilReady(20)) {
			throw new IllegalStateException("The Convert form did not finish loading its wallets");
		}

		click(pickers().get(0), "the from wallet picker");

		try {
			page.locator("xpath=//*[normalize-space()='Select Currency']").first().waitFor();
		} catch (com.microsoft.playwright.TimeoutError neverOpened) {
			throw new IllegalStateException("The Select Currency list did not open");
		}

		List<String> offered = new ArrayList<>();

		for (String code : currenciesOnOffer().split(", ")) {
			if (!code.isBlank() && !offered.contains(code)) {
				offered.add(code);
			}
		}

		closeTheCurrencyList();

		System.out.println("The convert currency list offers " + offered);

		return offered;
	}

	/**
	 * Clears whatever is left on screen after a conversion that did not go as expected.
	 *
	 * A walk through ninety pairs cannot afford to leave a dialog up: the next pair would find
	 * the pickers covered and fail for a reason that has nothing to do with it, and the run
	 * would report one bad channel as ten.
	 */
	public void dismissAnythingOpen() {

		for (String label : new String[]{"Ok", "OK", "Close", "Cancel"}) {
			try {
				Locator button = page.locator("xpath=//button[normalize-space()='" + label + "']");

				if (button.count() > 0 && button.first().isVisible()) {
					button.first().click();
					sleep(1000);
				}
			} catch (Exception alreadyGone) {
				// A dialog that closed itself needs no help.
			}
		}

		closeTheCurrencyList();

		// Dismissing what is open does not always bring the form back, so the form itself is
		// asked - and fetched afresh when it has gone. One wedged page cost a whole row of the
		// matrix: eight pairs failing with "the form did not finish loading its wallets".
		if (!waitUntilReady(5)) {
			open();
		}
	}

	/** Closes the currency list without choosing from it, leaving the form as it was. */
	private void closeTheCurrencyList() {

		try {
			page.keyboard().press("Escape");
			sleep(1000);
		} catch (Exception alreadyClosed) {
			// Nothing open is nothing to close.
		}
	}

	/**
	 * Whatever the platform is saying against the conversion, or empty when it is saying
	 * nothing.
	 *
	 * The same reading the deposits and withdraws use. A conversion can be declined like any
	 * other transaction, and a run that walks ninety pairs will meet a decline sooner or later -
	 * it should report the platform's words rather than "no confirmation appeared".
	 */
	public String refusalShowing() {

		return PlatformRefusal.showing(page);
	}

	/**
	 * The currency codes the open picker is listing, for when the wanted one is not
	 * among them. A three letter row in the dialog is a code; the country names and
	 * balances beside them are longer.
	 */
	private String currenciesOnOffer() {

		StringBuilder codes = new StringBuilder();

		for (String word : dialogText().split("[^A-Za-z0-9]+")) {

			if (word.length() == 3 && word.equals(word.toUpperCase())
					&& word.chars().allMatch(Character::isLetter)
					&& codes.indexOf(word) < 0) {

				codes.append(codes.length() == 0 ? "" : ", ").append(word);
			}
		}

		return codes.length() == 0 ? "nothing" : codes.toString();
	}

	/**
	 * Everything the open currency list is saying.
	 *
	 * Read as one block rather than element by element. Picking the three-letter rows out of
	 * the dialog's own paragraphs missed BDT, which renders its code somewhere else - and a
	 * matrix walk that quietly leaves a wallet out is worse than one that fails.
	 */
	private String dialogText() {

		StringBuilder said = new StringBuilder();

		for (Locator part : Wait.all(page.locator("xpath=//div[@id='root.dialog']"))) {
			try {
				said.append(" ").append(part.innerText());
			} catch (Exception reRendered) {
				// A part that re-rendered from under us is not one to read.
			}
		}

		return said.toString();
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

			String shown = sourceBalance.innerText().trim();

			// What follows the Balance label is not always the figure: depending on how the page
			// has rendered it can be the Max button, and reading that alone reports a wallet with
			// no balance at all. The line the label sits on always carries the figure somewhere,
			// so it is worth falling back to.
			if (!FIRST_NUMBER.matcher(shown).find()) {

				String line = balanceLine();

				// Only the figure carrying a currency symbol. Taking the first number on the line
				// picks up the amount being converted, which sits on it in some renders.
				Matcher money = MONEY.matcher(line);

				return money.find() ? money.group().substring(1) : line;
			}

			return shown;

		} catch (Exception notThere) {
			return "";
		}
	}

	/**
	 * A figure with a currency symbol in front of it - "HK$9,846.16", "R$108.64".
	 *
	 * What tells a balance from the other numbers on the line. The amount box holds a bare
	 * "1.00", and a run that read that as the balance reported three conversions as "confirmed
	 * but the wallet did not go down" when the wallet had gone down perfectly well.
	 */
	private static final Pattern MONEY = Pattern.compile("[^\\s0-9][0-9][0-9,]*(?:\\.[0-9]+)?");

	/**
	 * The wallet's own card, for when the figure is not in the element beside the label.
	 *
	 * Widened one step at a time rather than going straight to the largest block. The parent
	 * alone sometimes holds nothing but the label - "VND Vietnam Balance" - with the figure in a
	 * sibling beyond it, and reading that as the balance failed a pair for a reason that had
	 * nothing to do with the pair. The first reading that actually carries money is the answer.
	 */
	private String balanceLine() {

		for (String around : new String[]{"//*[normalize-space()='Balance']/..",
				"//*[normalize-space()='Balance']/../..",
				"//*[normalize-space()='Balance']/../../.."}) {

			try {
				String text = page.locator("xpath=" + around).first()
						.innerText().replaceAll("\\s+", " ").trim();

				if (MONEY.matcher(text).find()) {
					return text;
				}

			} catch (Exception notThere) {
				// A step too far up the page, or a card mid-render. Try the next.
			}
		}

		return "";
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

		// Waited for rather than read once. The balance is fetched per wallet and the line holds
		// the Max wording before the figure arrives, so a reading taken the instant the wallet
		// changes can find no number at all - which failed one pair of a matrix walk for reasons
		// that had nothing to do with that pair.
		Wait.until(() -> FIRST_NUMBER.matcher(getSourceBalance()).find(), 15);

		String shown = getSourceBalance();

		Matcher amount = FIRST_NUMBER.matcher(shown);

		if (!amount.find()) {
			throw new IllegalStateException("Could not read a balance from \"" + shown + "\"");
		}

		return new BigDecimal(amount.group().replace(",", ""));
	}

	/**
	 * The browser's own verdict on the amount, in the application's wording.
	 *
	 * The amount box is a number field with its own bounds, so a figure outside them never
	 * reaches the platform and there is no dialog to read. Without asking the box, a conversion
	 * the form quietly refused looks exactly like one the platform ignored.
	 */
	public String amountValidationMessage() {

		try {
			return String.valueOf(amountField.evaluate("el => el.validationMessage"));
		} catch (Exception notThere) {
			return "";
		}
	}

	/**
	 * The smallest conversion the amount box will take, as the form states it.
	 *
	 * Waited for rather than read at once: the bounds arrive with the pair's rate, and a box
	 * asked before they do answers min=0 and max=nothing at all - which is not the pair's band
	 * but the form's blank state, and a test measuring one below it would be measuring nothing.
	 */
	public String statedMinimum() {

		waitForLimits();

		return amountField.getAttribute("min");
	}

	/** The largest conversion the amount box will take. */
	public String statedMaximum() {

		waitForLimits();

		return amountField.getAttribute("max");
	}

	/** Waits for the pair's own bounds to reach the amount box. */
	private void waitForLimits() {

		Wait.until(() -> amountField.getAttribute("max") != null, 20);
	}

	/** Whether the browser considers the amount acceptable. */
	public boolean isAmountValid() {

		try {
			return Boolean.TRUE.equals(amountField.evaluate("el => el.checkValidity()"));
		} catch (Exception notThere) {
			return true;
		}
	}

	/**
	 * Whether the form will take the conversion as it stands.
	 *
	 * Submitting a conversion the form will not accept clicks a disabled button and then waits
	 * out the timeout for a confirmation that was never coming - which is how a small amount in
	 * a weak currency reported itself as "neither confirmed nor refused within 30 seconds".
	 */
	public boolean canConvert() {

		try {
			return convertButton.isEnabled();
		} catch (Exception notThere) {
			return false;
		}
	}

	/**
	 * Goes back to the conversion form by its own address.
	 *
	 * A walk through the whole matrix cannot afford to lose the form: once the pickers went
	 * missing, every remaining pair of that row failed with "the form did not finish loading
	 * its wallets" - eight failures for one wedged page.
	 */
	public void open() {

		String origin = page.url().replaceAll("(https?://[^/]+).*", "$1");

		page.navigate(origin + "/v2/exchange-rate");

		waitUntilReady(20);
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

	/**
	 * One line of the Convert History.
	 *
	 * The history is a page rather than a table: no thead, no rows, nothing addressable per
	 * column. What it does have is a shape it repeats for every conversion - what went in, when,
	 * what came out, and the rate it went at - and that shape is what this reads:
	 *
	 *   20.00 THB   2026-09-04 08:22:20   37.61 PHP   Rate: 1 THB ~ 1.88 PHP
	 *
	 * An entry that does not match the shape is an entry displayed wrongly, which is the thing
	 * worth failing for, so the pattern is deliberately strict about all four parts.
	 */
	private static final Pattern HISTORY_ENTRY = Pattern.compile(
			"([0-9][0-9,]*\\.[0-9]{2}) ([A-Z]{3}) "
			+ "([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}) "
			+ "([0-9][0-9,]*\\.[0-9]{2}) ([A-Z]{3}) "
			+ "Rate: 1 ([A-Z]{3}) ~ ([0-9.,]+) ([A-Z]{3})");

	/**
	 * Opens the Convert History from the button on the conversion form.
	 *
	 * Waits for the list, not merely for the page. The heading and the column names are drawn
	 * before the conversions are fetched, so a history read the moment it appears reads as a
	 * history with nothing in it - which is a failure about an empty account rather than about
	 * a slow request.
	 */
	public void openHistory() {

		click(historyButton, "History");

		Wait.until(this::isHistoryShowing, 20);

		Wait.until(() -> !historyEntries().isEmpty(), 20);
	}

	/**
	 * Whether the history is on screen.
	 *
	 * Both the address and the heading, because either alone can lie: the address changes the
	 * moment the click lands, before anything has been drawn, and the heading is a word that
	 * could sit on the form behind it.
	 */
	public boolean isHistoryShowing() {

		try {
			return page.url().contains("exchange-rate-history")
					&& page.locator("xpath=//*[normalize-space()='Convert History']").first().isVisible();
		} catch (Exception notThere) {
			return false;
		}
	}

	/** The column headings the history says it is showing. */
	public List<String> historyHeadings() {

		List<String> headings = new ArrayList<>();

		for (String heading : new String[]{"Convert From", "Convert To"}) {
			try {
				if (page.locator("xpath=//*[normalize-space()='" + heading + "']").first().isVisible()) {
					headings.add(heading);
				}
			} catch (Exception notThere) {
				// A heading that is not there is one this does not report.
			}
		}

		return headings;
	}

	/**
	 * Every conversion the history is listing, newest first, each as one line of text.
	 *
	 * Text rather than elements, for the reason the wallet lists are: a step handed the rows
	 * themselves could click one, and a history is for reading.
	 */
	public List<String> historyEntries() {

		List<String> entries = new ArrayList<>();

		Matcher entry = HISTORY_ENTRY.matcher(historyText());

		while (entry.find()) {
			entries.add(entry.group().trim());
		}

		return entries;
	}

	/** How many lines the history is showing that do not read as a conversion at all. */
	public String historyText() {

		try {
			return page.locator("xpath=//*[@id='root']").first()
					.innerText().replaceAll("\\s+", " ").trim();

		} catch (Exception nothingThere) {
			return "";
		}
	}

	/**
	 * Opens one of the listed conversions, by its place in the list.
	 *
	 * The rows carry no id and two conversions of the same amount on the same day read alike, so
	 * a row is addressed by where it sits rather than by what it says. Its rate line is the part
	 * that belongs to one row and no other, which makes it the thing to click.
	 */
	public void openHistoryEntry(int position) {

		Locator rates = page.locator("xpath=//*[contains(normalize-space(text()),'Rate: 1 ')]");

		if (rates.count() <= position) {
			throw new IllegalStateException("The history lists " + rates.count()
					+ " conversions, so there is no entry " + (position + 1));
		}

		try {
			rates.nth(position).click();
		} catch (Exception intercepted) {
			rates.nth(position).dispatchEvent("click");
		}

		Wait.until(this::isShowingAReceipt, 15);
	}

	/** True once a conversion's receipt is open. */
	public boolean isShowingAReceipt() {

		return receiptSays("Created Date");
	}

	/** Whatever the open receipt says. */
	public String receiptText() {

		try {
			for (Locator dialog : Wait.all(page.locator("div[role='dialog']"))) {
				if (dialog.isVisible()) {
					return dialog.innerText().replaceAll("\\s+", " ").trim();
				}
			}
		} catch (Exception notOpen) {
			return "";
		}

		return "";
	}

	/** Whether the receipt carries a particular word or figure. */
	public boolean receiptSays(String said) {

		return receiptText().contains(said);
	}

	/** Closes the receipt, so the next entry can be opened. */
	public void closeReceipt() {

		try {
			page.locator("xpath=//button[normalize-space()='Close modal']").first().click();
			Wait.sleep(1500);
		} catch (Exception alreadyClosed) {
			// A receipt that closed itself needs no help.
		}
	}

	/**
	 * Downloads the receipt and hands back the file.
	 *
	 * The application builds the image in the browser and hands it over as a data URL rather
	 * than fetching it from the server, so there is no request to watch for - the download
	 * event is the only evidence that anything happened, and saving the file is what proves it
	 * arrived with something in it.
	 */
	public java.nio.file.Path downloadReceipt(java.nio.file.Path saveTo) {

		com.microsoft.playwright.Download download = page.waitForDownload(() ->
				page.locator("xpath=//button[normalize-space()='Download']").first().click());

		java.nio.file.Path file = saveTo.resolve(download.suggestedFilename());

		download.saveAs(file);

		return file;
	}

	/**
	 * Opens the sharing choices for the receipt.
	 *
	 * Waits for a way to copy to appear, because that is the first thing that is true only after
	 * the choices open. Waiting for the word "Share" proved nothing at all - the receipt carries
	 * a Share button, so it was already there, and the run went on to look for the choices before
	 * they existed and reported that sharing offered nothing.
	 */
	public void shareReceipt() {

		page.locator("xpath=//button[normalize-space()='Share']").first().click();

		Wait.until(this::shareOffersACopy, 15);
	}

	/** Whether the sharing choices offer a way to copy the receipt. */
	public boolean shareOffersACopy() {

		try {
			return page.locator("xpath=//button[normalize-space()='Copy']").count() > 0
					&& page.locator("xpath=//button[normalize-space()='Copy']").first().isVisible();
		} catch (Exception notOffered) {
			return false;
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
