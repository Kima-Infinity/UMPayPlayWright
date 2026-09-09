package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.umpay.utility.Wait;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The payment accounts this account can be paid out to, at /settings-payment.
 *
 * Reached from the profile drawer. These are the accounts the withdraw and payout flows draw on -
 * when one of those says "Please add your payment account", this is the page it means - so what
 * is listed here decides what the rest of the product can do.
 *
 * An account is shown as a block: the provider across the top, and under it whatever that kind of
 * account is identified by. The shape differs by kind, which is the point of reading it rather
 * than assuming it: a bank carries an Account Name and an Account Number, an e-wallet may carry
 * an email address and a first and last name besides, a fast payment carries a Wallet Name and
 * Number, and a USDT address carries a Wallet Address and sometimes a Remark.
 *
 * Every block offers two things: change it, and remove it.
 *
 * ADDING ONE
 *
 * Add Payment opens /settings-payment/create, which asks for a currency and then offers three
 * ways to be paid - E-Wallet, Bank and USDT. Choosing one opens a searchable list of the
 * providers that take that currency, each naming how long it takes and what it charges.
 *
 * NOTHING HERE REMOVES AN ACCOUNT. The remove control is read and reported on, never clicked:
 * the accounts on this page are what the withdraw and payout scenarios rely on, and removing one
 * would break them somewhere else entirely.
 */
public class PaymentPage {

	/** Where the payment accounts are listed. */
	private static final String ROUTE = "/settings-payment";

	/** Where one is added. */
	private static final String CREATE = "/settings-payment/create";

	/** One saved account, by the box the page draws around it. */
	private static final String ACCOUNTS =
			"xpath=//div[contains(@class,'my-6')][contains(@class,'border')]";

	/** What a saved account can be labelled with. */
	private static final String[] LABELS = {"Account Name:", "Account Number:", "Email Address:",
			"First Name:", "Last Name:", "Wallet Name:", "Wallet Number:", "Wallet Address:",
			"Remark:"};

	/** One provider in the picker - "Bank of China Duration:2mn Fee Charge:6.20%". */
	private static final Pattern PROVIDER = Pattern.compile(
			"([A-Za-z0-9][A-Za-z0-9 .&'()-]*?) ?Duration:\\s*(\\S+) ?Fee Charge:\\s*(\\S+)");

	private final Page page;

	private final Locator topBarButtons;

	public PaymentPage(Page ldriver) {

		this.page = ldriver;
		this.topBarButtons = page.locator("nav button");
	}

	/** Opens the payment accounts, by the drawer where it can and by the address otherwise. */
	public void open() {

		if (isShowingTheList()) {
			return;
		}

		waitUntilSignedIn(30);

		try {
			ProfileDrawerPage drawer = new ProfileDrawerPage(page);

			drawer.open();
			drawer.open("Payment");

			if (Wait.until(this::isShowingTheList, 10)) {
				waitUntilLoaded(20);
				return;
			}
		} catch (Exception theDrawerWouldNot) {
			System.out.println("The profile drawer did not lead to the payment accounts: "
					+ theDrawerWouldNot.getMessage());
		}

		page.navigate(page.url().replaceAll("(https?://[^/]+).*", "$1") + ROUTE);

		Wait.sleep(3000);

		if (!Wait.until(this::isShowingTheList, 20)) {
			throw new IllegalStateException("Neither the profile drawer nor " + ROUTE + " opened"
					+ " the payment accounts. The page is at " + page.url());
		}

		waitUntilLoaded(20);
	}

	/** Waits for the platform to finish letting the account in. */
	public boolean waitUntilSignedIn(int timeoutSeconds) {

		return Wait.until(() -> !page.url().contains("/login") && topBarButtons.count() > 0,
				timeoutSeconds);
	}

	/** True once the saved accounts are on screen, rather than the form for adding one. */
	public boolean isShowingTheList() {

		try {
			return page.url().contains(ROUTE) && !page.url().contains(CREATE);
		} catch (Exception notThere) {
			return false;
		}
	}

	/** True once the form for adding an account is on screen. */
	public boolean isShowingTheForm() {

		try {
			return page.url().contains(CREATE);
		} catch (Exception notThere) {
			return false;
		}
	}

	/** Waits for the saved accounts to arrive, which they do after the page itself. */
	public boolean waitUntilLoaded(int timeoutSeconds) {

		return Wait.until(() -> !savedAccounts().isEmpty(), timeoutSeconds);
	}

	/** The address, for a step that has to say where a click led. */
	public String getCurrentUrl() {

		return page.url();
	}

	/** Back to where the last click came from. */
	public void goBack() {

		page.goBack();

		Wait.sleep(3000);
	}

	// ------------------------------------------------------------------
	// What is saved
	// ------------------------------------------------------------------

	/** Every payment account saved, each as its block's own text. */
	public List<String> savedAccounts() {

		List<String> saved = new ArrayList<>();

		for (Locator block : Wait.all(page.locator(ACCOUNTS))) {

			try {
				if (!block.isVisible()) {
					continue;
				}

				String said = block.innerText().trim();

				if (!said.isEmpty() && !saved.contains(said)) {
					saved.add(said);
				}
			} catch (Exception gone) {
				// A block that will not answer is not one to report.
			}
		}

		return saved;
	}

	/** One saved account written out on a single line, for a step that has to quote it. */
	public String oneLine(String account) {

		return account.replaceAll("\\s+", " ").trim();
	}

	/** Who the account is with - "HSBC", "DANA", "USDT-TRC 20". */
	public String providerOf(String account) {

		List<String> lines = linesOf(account);

		return lines.isEmpty() ? "" : lines.get(0);
	}

	/**
	 * What one saved account is identified by, label by label.
	 *
	 * The page writes a label and its value as separate lines, and which labels appear depends on
	 * the kind of account - which is why they are read rather than assumed.
	 */
	public Map<String, String> detailsOf(String account) {

		Map<String, String> said = new LinkedHashMap<>();

		String label = null;

		for (String line : linesOf(account)) {

			if (isALabel(line)) {
				label = line;
				said.put(label, "");
				continue;
			}

			if (label != null && said.get(label).isEmpty()) {
				said.put(label, line);
			}
		}

		return said;
	}

	/**
	 * How many accounts are saved, which is not the same as how many blocks are drawn.
	 *
	 * A block is a provider, and a provider can hold more than one account - the USDT block on
	 * this account holds several addresses. So the accounts are the rows inside the blocks, and
	 * it is those that each offer to be changed and removed.
	 */
	public int accountsSaved() {

		return count("xpath=//div[contains(@class,'gap-4')][contains(@class,'p-3')]"
				+ "[contains(@class,'min-h-[45px]')]");
	}

	/** How many of the saved accounts offer to be changed. */
	public int offeringToBeChanged() {

		return count("xpath=//button[contains(@class,'w-5')][contains(@class,'h-5')]"
				+ "[not(contains(@class,'text-error-500'))]");
	}

	/**
	 * How many of the saved accounts offer to be removed.
	 *
	 * Counted and never clicked. These accounts are what the withdraw and payout scenarios draw
	 * on, and one removed here would fail a scenario in another file entirely, which is the
	 * hardest kind of failure to account for.
	 */
	public int offeringToBeRemoved() {

		return count("xpath=//button[contains(@class,'text-error-500')]");
	}

	/** Opens the first saved account to be changed. */
	public void changeTheFirstAccount() {

		String startedAt = page.url();

		page.locator("xpath=//button[contains(@class,'w-5')][contains(@class,'h-5')]"
				+ "[not(contains(@class,'text-error-500'))]").first().click();

		Wait.until(() -> !page.url().equals(startedAt) || isAskingSomething(), 15);
		Wait.sleep(2000);
	}

	// ------------------------------------------------------------------
	// Adding one
	// ------------------------------------------------------------------

	/** Whether the page offers to add an account. */
	public boolean offersToAdd() {

		return count("xpath=//*[normalize-space(text())='Add Payment']") > 0;
	}

	/** Starts adding a payment account. */
	public void addPayment() {

		page.locator("xpath=//*[normalize-space(text())='Add Payment']").first().click();

		Wait.until(this::isShowingTheForm, 15);
		Wait.sleep(2000);
	}

	/** Which currency the form is set to pay out in. */
	public String currencyChosen() {

		for (Locator button : Wait.all(page.locator("button"))) {

			try {
				String said = button.innerText().replaceAll("\\s+", " ").trim();

				if (!said.isEmpty() && said.length() < 30 && !said.matches("\\d+")
						&& !isNavigation(said)) {

					return said;
				}
			} catch (Exception gone) {
				// Not this one.
			}
		}

		return "";
	}

	/** The ways of being paid the form offers - E-Wallet, Bank, USDT. */
	public List<String> waysToBePaidOffered() {

		List<String> offered = new ArrayList<>();

		for (String way : new String[]{"E-Wallet", "Bank", "USDT"}) {

			if (count("xpath=//*[normalize-space(text())='" + way + "']") > 0) {
				offered.add(way);
			}
		}

		return offered;
	}

	/** Chooses one of the ways of being paid, which opens the providers that take it. */
	public void chooseTheWay(String way) {

		page.locator("xpath=//*[normalize-space(text())='" + way + "']").last().click();

		Wait.until(this::isAskingSomething, 15);
		Wait.sleep(1500);
	}

	/** Whether something is being asked in a panel over the form. */
	public boolean isAskingSomething() {

		try {
			return page.locator("div[role='dialog']").count() > 0
					&& page.locator("div[role='dialog']").first().isVisible();
		} catch (Exception notOpen) {
			return false;
		}
	}

	/**
	 * The providers offered, each with how long it takes and what it charges.
	 *
	 * Read as the picker writes them - "Bank of China Duration:2mn Fee Charge:6.20%" - because
	 * what a payout costs and how long it takes is the whole of what a reader is choosing on.
	 */
	public List<String> providersOffered() {

		List<String> offered = new ArrayList<>();

		Matcher provider = PROVIDER.matcher(askedText());

		while (provider.find()) {
			offered.add(provider.group().trim());
		}

		return offered;
	}

	/** What one provider is called. */
	public String nameOf(String provider) {

		Matcher parts = PROVIDER.matcher(provider);

		return parts.find() ? parts.group(1).trim() : "";
	}

	/** How long one provider says it takes. */
	public String durationOf(String provider) {

		Matcher parts = PROVIDER.matcher(provider);

		return parts.find() ? parts.group(2).trim() : "";
	}

	/** What one provider says it charges. */
	public String feeOf(String provider) {

		Matcher parts = PROVIDER.matcher(provider);

		return parts.find() ? parts.group(3).trim() : "";
	}

	/** Whether the providers can be searched through. */
	public boolean offersToSearch() {

		return count("input[placeholder='Search']") > 0;
	}

	/** Searches the providers for something. Passing "" clears the search. */
	public void searchProviders(String said) {

		page.locator("input[placeholder='Search']").first().fill(said);

		Wait.sleep(2500);
	}

	/**
	 * True while the list is saying there is nothing to show.
	 *
	 * Worth asking separately from counting the providers: a list that has quietly gone empty and
	 * one that says "No data" look the same to a count, and only the second tells somebody
	 * searching that their search is what emptied it.
	 */
	public boolean saysThereIsNoData() {

		return askedText().contains("No data");
	}

	// ------------------------------------------------------------------
	// Changing one, without saving anything
	// ------------------------------------------------------------------

	/**
	 * What the change form asks for, by the label each box carries.
	 *
	 * The form names its boxes accountDetails[n][value] and carries the label for each in a
	 * hidden box beside it, so the two are read together - a map of "Account Name" to what is in
	 * it is what a scenario can be written against, where accountDetails[0][value] is not.
	 */
	public Map<String, String> whatTheChangeFormHolds() {

		Map<String, String> holding = new LinkedHashMap<>();

		// Handed back as a list of "label=value" rather than as JSON. Playwright turns a JS array
		// into a List of its own, so nothing has to be parsed on this side - the first attempt
		// did parse JSON here and lost every entry to its own quoting.
		for (String pair : readBoxes("label ? label.value : box.getAttribute('name')")) {

			int at = pair.indexOf(SEPARATOR);

			if (at > 0) {
				holding.put(pair.substring(0, at), pair.substring(at + 1));
			}
		}

		return holding;
	}

	/** What a label is joined to its value by, chosen so no label or value could contain it. */
	private static final char SEPARATOR = (char) 1;

	/**
	 * Every box the change form holds a detail in, described by {@code as}.
	 *
	 * The boxes are named accountDetails[n][value] and the label for each sits in a box of its
	 * own beside it, so the index is taken off the name and used to find the label. The regular
	 * expression matters: written with one backslash too many it matches nothing, and every box
	 * then comes back called accountDetails[0][value] instead of "Account Name".
	 */
	private List<String> readBoxes(String as) {

		List<String> read = new ArrayList<>();

		try {
			Object found = page.evaluate(
					"() => Array.from(document.querySelectorAll('input[name^=\"accountDetails\"][name$=\"[value]\"]'))"
					+ ".map(box => {"
					+ "   const at = (box.getAttribute('name') || '').match(/\\[(\\d+)\\]/);"
					+ "   const label = at && document.querySelector("
					+ "     'input[name=\"accountDetails[' + at[1] + '][label]\"]');"
					+ "   return {box: box, label: label}; })"
					+ ".filter(pair => pair.box)"
					+ ".map(({box, label}) => (" + as + ") + '\\u0001' + box.value)");

			if (found instanceof List) {

				for (Object one : (List<?>) found) {
					read.add(String.valueOf(one));
				}
			}
		} catch (Exception unreadable) {
			// A form that would not answer holds nothing, as far as this can tell.
		}

		return read;
	}

	/** The labels of the boxes the change form will not do without. */
	public List<String> whatTheChangeFormInsistsOn() {

		List<String> insisted = new ArrayList<>();

		for (String label : requiredBoxes()) {

			if (!insisted.contains(label)) {
				insisted.add(label);
			}
		}

		return insisted;
	}

	/** The labels of the boxes the form marks as required. */
	private List<String> requiredBoxes() {

		List<String> required = new ArrayList<>();

		try {
			Object found = page.evaluate(
					"() => Array.from(document.querySelectorAll('input[name^=\"accountDetails\"][name$=\"[value]\"]'))"
					+ ".filter(box => box.required)"
					+ ".map(box => {"
					+ "   const at = (box.getAttribute('name') || '').match(/\\[(\\d+)\\]/);"
					+ "   const label = at && document.querySelector("
					+ "     'input[name=\"accountDetails[' + at[1] + '][label]\"]');"
					+ "   return label ? label.value : box.getAttribute('name'); })");

			if (found instanceof List) {

				for (Object one : (List<?>) found) {
					required.add(String.valueOf(one));
				}
			}
		} catch (Exception unreadable) {
			// A form that would not answer insists on nothing, as far as this can tell.
		}

		return required;
	}

	/**
	 * Empties the first box the change form insists on, without saving.
	 *
	 * The value is set the way the browser sets it rather than by typing, because the form is
	 * drawn by a framework that watches its own setter - filling the box any other way leaves the
	 * form still holding the old value behind the screen.
	 */
	public void emptyTheFirstDetail() {

		page.evaluate(
				"() => { const box = document.querySelector('input[name^=\"accountDetails\"][name$=\"[value]\"]');"
				+ " if (!box) return;"
				+ " const setter = Object.getOwnPropertyDescriptor("
				+ "   window.HTMLInputElement.prototype, 'value').set;"
				+ " setter.call(box, '');"
				+ " box.dispatchEvent(new Event('input', {bubbles: true}));"
				+ " box.dispatchEvent(new Event('change', {bubbles: true})); }");

		Wait.sleep(1500);
	}

	/** True while the first box the change form insists on is one the browser would accept. */
	public boolean theFirstDetailIsAcceptable() {

		try {
			Object verdict = page.evaluate(
					"() => { const box = document.querySelector('input[name^=\"accountDetails\"][name$=\"[value]\"]');"
					+ " return box ? box.checkValidity() : true; }");

			return Boolean.parseBoolean(String.valueOf(verdict));

		} catch (Exception unreadable) {
			return true;
		}
	}

	/** What the browser says about the first box, in the form's own wording. */
	public String whatTheFirstDetailSays() {

		try {
			Object said = page.evaluate(
					"() => { const box = document.querySelector('input[name^=\"accountDetails\"][name$=\"[value]\"]');"
					+ " return box ? box.validationMessage : ''; }");

			return said == null ? "" : String.valueOf(said).trim();

		} catch (Exception unreadable) {
			return "";
		}
	}

	/**
	 * Closes whatever was being asked, leaving the form as it was.
	 *
	 * Every way of closing is tried in turn and each is given a moment to take, rather than the
	 * first one being trusted: clicking a control that says Close is not the same as the panel
	 * having closed, and a panel still open would fail the next step for the wrong reason.
	 */
	public void closeWhatIsBeingAsked() {

		for (String label : new String[]{"Close modal", "Close", "Cancel"}) {

			Locator button = page.locator("xpath=//button[normalize-space()='" + label + "']");

			try {
				if (button.count() == 0 || !button.first().isVisible()) {
					continue;
				}

				try {
					button.first().click(new Locator.ClickOptions().setTimeout(4000));

				} catch (Exception intercepted) {

					// The provider cards are drawn over the corner this control sits in, so a
					// click aimed at it lands on a card instead and Playwright waits out its
					// whole timeout on a button that was never going to be reachable.
					button.first().dispatchEvent("click");
				}

				if (Wait.until(() -> !isAskingSomething(), 5)) {
					return;
				}

			} catch (Exception alreadyClosed) {
				// Try the next way of saying it.
			}
		}

		for (int press = 0; press < 3 && isAskingSomething(); press++) {

			page.keyboard().press("Escape");

			Wait.until(() -> !isAskingSomething(), 3);
		}
	}

	/** Everything the page says, for a step that has to report what it found instead. */
	public String text() {

		try {
			return page.locator("xpath=//*[@id='root']").first()
					.innerText().replaceAll("\\s+", " ").trim();
		} catch (Exception unreadable) {
			return "";
		}
	}

	/**
	 * Whatever is being asked in the panel over the form, or the page itself if none is.
	 *
	 * The panel's own furniture is taken off the front of it - the control that closes it and the
	 * heading over the list - because otherwise the first provider comes back called "Close modal
	 * Payment Name Bank of China", which is what a report would then quote.
	 */
	private String askedText() {

		try {
			if (isAskingSomething()) {

				String said = page.locator("div[role='dialog']").first()
						.innerText().replaceAll("\\s+", " ").trim();

				for (String furniture : new String[]{"Close modal", "Payment Name"}) {

					if (said.startsWith(furniture)) {
						said = said.substring(furniture.length()).trim();
					}
				}

				return said;
			}
		} catch (Exception unreadable) {
			return "";
		}

		return text();
	}

	private int count(String locator) {

		try {
			return page.locator(locator).count();
		} catch (Exception notThere) {
			return 0;
		}
	}

	private boolean isALabel(String line) {

		for (String label : LABELS) {
			if (line.equals(label)) {
				return true;
			}
		}

		return false;
	}

	/** Whether a button is one of the ones that sit in every page's furniture. */
	private boolean isNavigation(String said) {

		for (String where : new String[]{"Home", "International School Fees", "Deposit", "Withdraw",
				"Domestic Transfer", "Global Transfer", "Transfer", "Bills", "Convert",
				"Add Payment", "Close modal"}) {

			if (said.equals(where)) {
				return true;
			}
		}

		return false;
	}

	private List<String> linesOf(String said) {

		List<String> lines = new ArrayList<>();

		for (String line : said.split("\n")) {

			String one = line.trim();

			if (!one.isEmpty()) {
				lines.add(one);
			}
		}

		return lines;
	}
}
