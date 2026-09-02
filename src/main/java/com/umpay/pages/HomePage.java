package com.umpay.pages;

import com.umpay.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;



import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class HomePage {

	/** How long the 2FA dialog gets to render before it counts as absent. */
	private static final int PROMPT_SETTLE_SECONDS = 5;

	Page page;
	
		private final Locator setup2FAButton;


		private final Locator skipButton;


		private final Locator homeIcon;


		private final Locator homeButton;


		private final Locator internationalSchoolFeeButton;


		private final Locator depositButton;


		private final Locator withdrawButton;


    	private final Locator domesticTransferButton;


		private final Locator globalTransferButton;


		private final Locator transferButton;


		private final Locator billsButton;


		private final Locator convertButton;


		private final Locator maskButton;


		private final Locator mainWalletCurrency;


		private final Locator walletBalance;


		private final Locator showMoreButton;


	// --- The prompt a new account meets on its first visit to the home page ---

	/**
	 * Located by its text rather than by position in the dialog. The absolute
	 * xpaths above address the same two buttons but break whenever the dialog's
	 * markup shifts, and this one has to survive a new account's first login.
	 */
		private final Locator skipTwoFactorButton;


		private final Locator setupTwoFactorButton;


	/** "Total in USD:" - the heading of the balance card behind the prompt. */
		private final Locator totalLabel;

	/** The UnionPay card on the balance panel, and the way into the UnionPay flow. */
		private final Locator unionPayButton;

	/** "Total Blocked Amount: ..." - the held funds, beneath the total. */
		private final Locator totalBlockedAmount;

	/** The support button pinned to the bottom corner of every screen. */
		private final Locator customerServiceButton;

	/** The product name beside the logo in the sidebar. */
		private final Locator brandHeading;

	/**
	 * One per wallet, each holding that wallet's currency, balances and its own
	 * Main Wallet or Set as Main button.
	 *
	 * Matched on the class as a whole word rather than on the exact class attribute.
	 * The exact form this replaces - div[@class='py-1.5'] - stops matching the moment
	 * anybody adds a second class to that div, which in a Tailwind codebase is a matter
	 * of time rather than of chance.
	 */
		private final Locator walletCards;

	/** The badge naming whichever wallet is currently the main one. */
		private final Locator mainWalletBadge;



    public HomePage(Page ldriver) {

		this.page = ldriver;
		this.setup2FAButton = page.locator("xpath=//*[@id=\"root.dialog\"]/div/div[2]/div/div/div/div/div[2]/div/div/div/button[1]");
		this.skipButton = page.locator("xpath=//*[@id=\"root.dialog\"]/div/div[2]/div/div/div/div/div[2]/div/div/div/button[2]");
		this.homeIcon = page.locator("xpath=//*[@id=\"left-sidebar\"]/div/aside/div[1]/div/img");
		this.homeButton = page.locator("[id=\'home\']");
		this.internationalSchoolFeeButton = page.locator("[id=\'international_school_fees\']");
		this.depositButton = page.locator("[id=\'deposit\']");
		this.withdrawButton = page.locator("[id=\'withdraw\']");
		this.domesticTransferButton = page.locator("[id=\'domestic_transfer\']");
		this.globalTransferButton = page.locator("[id=\'global_transfer\']");
		this.transferButton = page.locator("[id=\'transfer\']");
		this.billsButton = page.locator("[id=\'bills\']");
		this.convertButton = page.locator("[id=\'convert\']");
		this.maskButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div[1]/div/div/div[1]/button");
		this.mainWalletCurrency = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div[1]/div/div/div[2]/h3[1]");
		this.walletBalance = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div[1]/div/div/div[2]/h3[2]");
		this.showMoreButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div[3]/button");
		this.skipTwoFactorButton = page.locator("xpath=//button[normalize-space()='Skip']");
		this.setupTwoFactorButton = page.locator("xpath=//button[contains(normalize-space(),'Setup 2-factor authentication')]");
		this.totalLabel = page.locator("xpath=//*[contains(text(),'Total in')]");
		this.unionPayButton = page.locator("button:has(img[alt='unionpay'])");
		this.totalBlockedAmount = page.locator("xpath=//p[contains(text(),'Total Blocked Amount')]");
		this.customerServiceButton = page.locator("button:has(img[src*='customer_service'])");
		this.brandHeading = page.locator("xpath=//aside//h2");
		this.walletCards = page.locator(
				"xpath=//div[contains(concat(' ', normalize-space(@class), ' '), ' py-1.5 ')]");
		this.mainWalletBadge = page.locator("xpath=//button[normalize-space()='Main Wallet']");
	}

	private void clickWhenReady(Locator element, String elementName) {
		try {
			element.waitFor();
			element.waitFor();
			element.click();
			System.out.println(elementName + " clicked successfully!");
		} catch (Exception e) {
			System.out.println("Failed to click " + elementName + ": " + e.getMessage());
			try {
				element.dispatchEvent("click");
				System.out.println(elementName + " clicked via JS successfully!");
			} catch (Exception jsException) {
				System.out.println("Failed to click " + elementName + " via JS: " + jsException.getMessage());
			}
		}
	}

	/**
	 * True while the Google 2FA prompt is covering the home page.
	 *
	 * The dialog's container element {@code #root.dialog} stays in the document
	 * whether or not anything is in it, so its presence proves nothing. The
	 * buttons being on screen is the only honest test.
	 */
	public boolean isTwoFactorPromptDisplayed() {

		return isShowing(skipTwoFactorButton)
				|| isShowing(setupTwoFactorButton)
				|| isShowing(skipButton)
				|| isShowing(setup2FAButton);
	}

	/**
	 * Whether one element of the prompt is on screen, treating "not in the document"
	 * as "not showing" rather than as an error.
	 *
	 * Each button is asked about separately because a PageFactory proxy throws the
	 * moment it cannot resolve, and one absent button would otherwise hide the answer
	 * for the ones after it - the dialog is addressed by two locators apiece, and only
	 * one pair of them exists on any given render.
	 */
	private boolean isShowing(Locator element) {

		try {
			return element.isVisible();
		} catch (Exception notThere) {
			return false;
		}
	}

	/**
	 * Dismisses the 2FA prompt with its Skip link.
	 *
	 * Setting up an authenticator is deliberately not automated - it would tie the
	 * account to a secret this suite would then have to keep and generate codes
	 * from. Skipping is what the flow offers for getting to the application, and it
	 * is what a registration test wants.
	 *
	 * @return whether the prompt was there to be skipped
	 */
	public boolean skipTwoFactorSetup() {

		// The dialog renders a moment after the home page it covers. Ask for it rather than
		// glancing: a glance says no prompt, and then the backdrop appears over the very
		// navigation this was called to clear the way for.
		Wait.until(this::isTwoFactorPromptDisplayed, PROMPT_SETTLE_SECONDS);

		if (!isTwoFactorPromptDisplayed()) {
			System.out.println("No 2FA prompt was shown");
			return false;
		}

		// The dialog is reached by two locators: the one that reads the button's text,
		// and the positional one the older tests were written against. They address the
		// same Skip button, but only one of them resolves on any given render of the
		// dialog, so whichever is actually on screen is the one that gets clicked.
		clickWhenReady(isShowing(skipTwoFactorButton) ? skipTwoFactorButton : skipButton,
				"Skip 2FA button");

		if (!Wait.until(() -> !isTwoFactorPromptDisplayed(), 15)) {
			throw new IllegalStateException("The 2FA prompt was still showing after clicking Skip");
		}

		System.out.println("Skipped the 2FA prompt");
		return true;
	}

	/**
	 * Skips the 2FA prompt when it is up, and says nothing when it is not.
	 *
	 * The prompt is raised after signing in and covers the page, so every click on a route
	 * tile behind it is intercepted. skipTwoFactorSetup throws if the prompt will not go
	 * away, which is right for a registration test that depends on getting past it; a
	 * navigation step only needs the page usable, so this wraps it in the softer form.
	 */
	public void dismissTwoFactorPromptIfShowing() {

		try {
			skipTwoFactorSetup();

		} catch (Exception stillShowing) {
			System.out.println("Could not clear the 2FA prompt: " + stillShowing.getMessage());
		}
	}

	/**
	 * The three transfer areas in the left navigation.
	 *
	 * Kept here rather than on the pages they open, because the navigation belongs to the
	 * frame the application draws around every screen, and a page that reached for another
	 * page's entry would give the same button two owners.
	 */
	public void openTransferHub() {

		clickNavigation(transferButton, "Transfer");

	}

	public void openDomesticTransfer() {

		clickNavigation(domesticTransferButton, "Domestic Transfer");

	}

	public void openGlobalTransfer() {

		clickNavigation(globalTransferButton, "Global Transfer");

	}

	public void openDeposit() {

		clickNavigation(depositButton, "Deposit");

	}

	public void openWithdraw() {

		clickNavigation(withdrawButton, "Withdraw");

	}

	public void openConvert() {

		clickNavigation(convertButton, "Convert");

	}

	/**
	 * Clicks a left navigation entry, waiting long enough for the sidebar to exist.
	 *
	 * The shared wait on this page is ten seconds, and the application regularly takes
	 * longer than that to draw its sidebar after signing in - long enough that #transfer is
	 * not merely invisible but absent from the document, so the JavaScript fallback inside
	 * clickWhenReady has nothing to click either. The symptom is a navigation step failing
	 * with "no such element" while the page is perfectly healthy a second later, and it is
	 * intermittent: the same three scenarios passed on one run and failed on the next.
	 *
	 * Thirty seconds is the wait the transfer pages already use for the same reason.
	 */
	private void clickNavigation(Locator entry, String name) {

		try {
			entry.waitFor(new Locator.WaitForOptions().setTimeout(30 * 1000));

		} catch (Exception neverAppeared) {
			System.out.println("The " + name + " navigation entry never appeared: "
					+ neverAppeared.getMessage());
		}

		clickWhenReady(entry, name);

	}

	/**
	 * The browser's title for whatever is currently loaded.
	 *
	 * The application titles its landing page "UMPay | Home", which is what a step
	 * checks to confirm the flow finished on the home page. Reading it through the
	 * page keeps the step from holding the page for a single string.
	 */
	public String getPageTitle() {

		return page.title();

	}

	/** The address currently in the bar, for reporting where a step ended up. */
	public String getCurrentUrl() {

		return page.url();

	}

	/** The "Total in <currency>:" heading, or an empty string when it is not on screen. */
	public String getTotalLabel() {

		try {
			totalLabel.waitFor();
			return totalLabel.innerText().trim();
		} catch (Exception notThere) {
			return "";
		}
	}

	/**
	 * One entry per wallet on the home page, each reading something like
	 * "USD | US Dollar | Balance: ... | Blocked Amount: ... | Main Wallet".
	 */
	public List<String> getWalletRows() {

		List<String> wallets = new ArrayList<>();

		for (Locator row : Wait.all(walletCards)) {

			String text = row.innerText().replace("\n", " ").replaceAll("\\s*\\|\\s*", " | ").trim();

			if (!text.isEmpty()) {
				wallets.add(text);
			}
		}

		return wallets;
	}

	/**
	 * Reveals the masked amounts if the eye toggle can be found.
	 *
	 * Best effort on purpose: the toggle is only reachable by a brittle absolute
	 * path, and reading the page is still worth doing with the amounts masked. A
	 * missing toggle is reported, not thrown.
	 */
	public boolean revealBalances() {

		try {
			maskButton.waitFor();
			maskButton.click();
			System.out.println("Revealed the masked amounts");
			return true;
		} catch (Exception cannotReveal) {
			System.out.println("Could not reveal the masked amounts, reading them as shown");
			return false;
		}
	}

	/**
	 * Reads what the home page is showing and prints it, returning the wallet rows
	 * so a caller can assert on them.
	 */
	public List<String> readHomePage() {

		revealBalances();

		String total = getTotalLabel();
		List<String> wallets = getWalletRows();

		System.out.println("Home page for this account:");
		System.out.println("  " + (total.isEmpty() ? "<no total shown>" : total));

		for (String wallet : wallets) {
			System.out.println("  " + wallet);
		}

		return wallets;
	}


	public void openBills() {

		clickNavigation(billsButton, "Bills");

	}

	public void openInternationalSchoolFees() {

		clickNavigation(internationalSchoolFeeButton, "International School Fees");

	}

	/** Opens the UnionPay card on the balance panel. */
	public void openUnionPay() {

		clickWhenReady(unionPayButton, "UnionPay card");

	}

	/** Opens the support widget. */
	public void openCustomerService() {

		clickWhenReady(customerServiceButton, "Customer service button");

	}

	/** Whether support is offered on this screen at all. */
	public boolean isCustomerServiceOffered() {

		return Wait.appears(customerServiceButton);

	}

	/** The held funds as shown, or an empty string when the line is not on screen. */
	public String getTotalBlockedAmount() {

		try {
			totalBlockedAmount.waitFor();
			return totalBlockedAmount.innerText().replaceAll("\\s+", " ").trim();
		} catch (Exception notThere) {
			return "";
		}
	}

	/** The product name in the sidebar, or an empty string when it is not on screen. */
	public String getBrandName() {

		try {
			brandHeading.waitFor();
			return brandHeading.innerText().trim();
		} catch (Exception notThere) {
			return "";
		}
	}

	// --- The wallet list -------------------------------------------------------------

	/** How many wallets the home page is showing. */
	public int walletCount() {

		return Wait.all(walletCards).size();

	}

	/**
	 * One wallet's row as shown, or an empty string when the account has no such wallet.
	 *
	 * @param currency the three letter code, for instance "HKD"
	 */
	public String getWalletRow(String currency) {

		for (String row : getWalletRows()) {
			if (row.startsWith(currency)) {
				return row;
			}
		}

		return "";
	}

	/** Whether this account holds a wallet in {@code currency} at all. */
	public boolean hasWallet(String currency) {

		return !getWalletRow(currency).isEmpty();

	}

	/**
	 * The currency of whichever wallet is currently the main one, or an empty string if
	 * no wallet claims the badge.
	 */
	public String getMainWalletCurrency() {

		for (String row : getWalletRows()) {
			if (row.contains("Main Wallet")) {
				return row.split("[\\s|]+")[0];
			}
		}

		return "";
	}

	/**
	 * Whether this wallet offers to become the main one.
	 *
	 * False both for a wallet that is already main - it shows a badge instead of a button -
	 * and for a currency the account does not hold, which the caller can tell apart with
	 * {@link #hasWallet}.
	 */
	public boolean canSetAsMain(String currency) {

		return setAsMainButton(currency).count() > 0;

	}

	/**
	 * Makes {@code currency} the main wallet.
	 *
	 * The main wallet is what the transfer, convert and withdraw screens open on, so this
	 * changes the starting point of those flows for the account. Worth knowing before
	 * calling it in a suite that shares one account.
	 */
	public void setWalletAsMain(String currency) {

		clickWhenReady(setAsMainButton(currency), "Set as Main for " + currency);

	}

	/** Opens a wallet's own screen. */
	public void openWallet(String currency) {

		clickWhenReady(walletCard(currency), currency + " wallet");

	}

	/** Whether the main wallet badge is showing anywhere in the list. */
	public boolean isMainWalletMarked() {

		return Wait.appears(mainWalletBadge);

	}

	/**
	 * One wallet's card.
	 *
	 * Private, and it stays private: handing a Locator back to a step definition would let
	 * the step do anything it liked with the element, which is the coupling the page object
	 * exists to prevent.
	 */
	private Locator walletCard(String currency) {

		return walletCards.filter(new Locator.FilterOptions().setHasText(currency));

	}

	private Locator setAsMainButton(String currency) {

		return walletCard(currency).locator("xpath=.//button[normalize-space()='Set as Main']");

	}

	public void homePage() {
		try {
			Thread.sleep(2000);
			if (setup2FAButton != null && skipButton != null) {
				try {
					skipButton.click();
				} catch (Exception e) {
					System.out.println("Skip button not clickable or not present: " + e.getMessage());
				}
			}

			clickWhenReady(maskButton, "Mask button");

			mainWalletCurrency.waitFor();
			System.out.println("Main Wallet Currency: " + mainWalletCurrency.innerText());
			Thread.sleep(2000);

			walletBalance.waitFor();
			System.out.println("Total Balance in Main Wallet Currency: " + walletBalance.innerText());
			Thread.sleep(2000);

			clickWhenReady(showMoreButton, "Show More button");

			List<Locator> elements = Wait.all(walletCards);
			for (Locator element : elements) {
				try {
					Locator walletCurrency = element.locator("xpath=" + ".//span[1]");
					Locator balance = element.locator("xpath=" + ".//span[1]/span");
					Locator blockedBalance = element.locator("xpath=" + ".//span[2]/span");

					System.out.println("Balance details for " + walletCurrency.innerText() + ":");
					System.out.println("Wallet Balance: " + balance.innerText());
					System.out.println("Wallet Blocked Balance: " + blockedBalance.innerText());
					System.out.println();
				} catch (Exception e) {
					System.out.println("Error locating sub-elements: " + e.getMessage());
				}
			}

			clickWhenReady(homeButton, "Home button");
			clickWhenReady(internationalSchoolFeeButton, "International School Fee button");
			clickWhenReady(depositButton, "Deposit button");
			clickWhenReady(withdrawButton, "Withdraw button");
			clickWhenReady(domesticTransferButton, "Domestic Transfer button");
			clickWhenReady(globalTransferButton, "Global Transfer button");
			clickWhenReady(transferButton, "Transfer button");
			clickWhenReady(billsButton, "Bills button");
			clickWhenReady(convertButton, "Convert button");

			System.out.println("Home page loaded Successfully!");

			clickWhenReady(homeIcon, "Home Icon");

		} catch (Exception e) {
			System.out.println("Home Page operation failed: " + e.getMessage());
		}
	}
}
