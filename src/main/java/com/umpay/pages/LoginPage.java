package com.umpay.pages;

import com.umpay.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;



import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class LoginPage {

	Page page;
	
	// --- Choosing how to sign in ---

		private final Locator emailTabButton;


		private final Locator phoneTabButton;


	// --- The form itself ---

		private final Locator email;


		private final Locator phoneCountryDropdown;


		private final Locator phoneField;


		private final Locator passWord;


	/*
	 * The Login button used to be found by its position in the form. That only held on the
	 * Email tab: choosing Phone Number adds the country control and moves the button down a
	 * div, so the same path picked nothing there. Matching the submit button by its type and
	 * label finds the same button on both tabs and survives a re-ordered form.
	 */
		private final Locator loginButton;


	// --- Everything else the page offers ---

		private final Locator forgotPasswordLink;


		private final Locator registerLink;


		private final Locator languageButton;


		private final Locator customerServiceButton;


	/*
	 * Refusals arrive two different ways and the scenarios need both.
	 *
	 * A rule the page can check by itself - a malformed address, a password under six
	 * characters - is answered inline. An answer that needs the server - an address nobody
	 * holds, a wrong password - arrives as a banner that removes itself after about five
	 * seconds, which is why errorMessage() polls rather than reads once.
	 *
	 * The banner is matched on the error border its container carries. The Toastify class
	 * the registration page keys on is not on this one.
	 */
		private final Locator errorBanner;


		private final Locator fieldErrorMessage;


	public LoginPage(Page ldriver) {

		this.page = ldriver;
		this.emailTabButton = page.locator("xpath=//button[normalize-space()='Email']");
		this.phoneTabButton = page.locator("xpath=//button[normalize-space()='Phone Number']");
		this.email = page.locator("[name=\'email\']");
		this.phoneCountryDropdown = page.locator("[name=\'phoneCountry\']");
		this.phoneField = page.locator("[name=\'phone\']");
		this.passWord = page.locator("[name=\'password\']");
		this.loginButton = page.locator("xpath=//button[@type='submit' and normalize-space()='Login']");
		this.forgotPasswordLink = page.locator("xpath=//a[@href='/forgot-password']");
		this.registerLink = page.locator("xpath=//a[@href='/register']");
		this.languageButton = page.locator("[id=\'locale.dropdown-icon\']");
		this.customerServiceButton = page.locator("xpath=//button[contains(@class,'fixed') and contains(@class,'bottom-3')]");
		this.errorBanner = page.locator("xpath=//div[contains(@class,'border-error-500')]//p[contains(@class,'text-sm')]");
		this.fieldErrorMessage = page.locator("xpath=//em[contains(@class,'text-error-600')]");
	}

	/**
	 * Blocks until the sign-in form is usable again.
	 *
	 * Signing out lands back here, and the page that performs the sign-out needs to
	 * know when that has happened. It asks this rather than borrowing the button:
	 * a page that hands out its elements gives every caller the power to click them.
	 */
	public void waitUntilReady() {

		loginButton.waitFor();

	}

	/**
	 * Opens the login page on an empty session.
	 *
	 * The browser is started once for the whole suite, so a scenario that runs after a
	 * signed-in one arrives holding that session and /login answers it with the dashboard.
	 * Clearing what the session is kept in and asking again puts the form back, which is
	 * what a scenario about signing in has to start from.
	 *
	 * The address comes from the caller because it lives in the suite's configuration,
	 * which is test-scoped; what belongs here is knowing what opening this page means.
	 */
	public void open(String loginUrl) {

		page.navigate(loginUrl);

		if (isShowing()) {
			return;
		}

		System.out.println("The login page answered with a signed-in session; clearing it.");

		page.evaluate("() => { window.localStorage.clear(); window.sessionStorage.clear(); }");
		page.context().clearCookies();

		page.navigate(loginUrl);
	}

	/** Whether the sign-in form is on screen, rather than the dashboard or a chat panel. */
	public boolean isShowing() {

		try {
			loginButton.waitFor(new Locator.WaitForOptions().setTimeout(10 * 1000));
			return true;
		} catch (com.microsoft.playwright.TimeoutError notThere) {
			return false;
		}
	}

	public void chooseEmailMethod() {

		clickWhenReady(emailTabButton, "Email sign-in method");
		email.waitFor();
	}

	public void choosePhoneMethod() {

		clickWhenReady(phoneTabButton, "Phone Number sign-in method");
		phoneField.waitFor();
	}

	/**
	 * Fills the email form and submits it, without waiting for the outcome.
	 *
	 * Deliberately separate from {@link #loginToUMPay(String, String)}: that one is for the
	 * scenarios that expect to get in and treats a stuck form as something to print and move
	 * past. These credentials are meant to be refused, so the caller needs the submit to
	 * happen and then to ask the page what it said.
	 */
	public void signInWithEmail(String emailAddress, String password) {

		chooseEmailMethod();

		typeInto(email, emailAddress);
		typeInto(passWord, password);

		submit();
	}

	/** The phone equivalent, choosing the country before the number. */
	public void signInWithPhone(String countryCode, String phoneNumber, String password) {

		enterPhoneNumber(countryCode, phoneNumber);

		typeInto(passWord, password);

		submit();
	}

	/**
	 * Signs in by phone, naming the country by its dialling code rather than its ISO code.
	 *
	 * Login_TestData holds the number the way a person writes it - "855 96443322" - so the
	 * country arrives as the +855 a reader recognises, not as the KH the select's value
	 * attribute uses. Matching on the label, which reads "CAMBODIA (+855)", keeps the test
	 * data readable and means nobody has to look up an ISO code to add a row.
	 */
	public void signInWithDiallingCode(String diallingCode, String phoneNumber, String password) {

		choosePhoneMethod();

		selectCountryByDiallingCode(diallingCode);

		typeInto(phoneField, phoneNumber);
		typeInto(passWord, password);

		submit();
	}

	/**
	 * Whether the sign in got through, judged by leaving the login page behind.
	 *
	 * The dashboard is another page's business to describe; all this needs to know is that
	 * the form accepted the credentials and stopped being the thing on screen.
	 */
	public boolean isSignedIn() {

		return Wait.until(() -> !page.url().contains("/login"), 30);
	}

	/**
	 * Fills the phone side of the form and stops there.
	 *
	 * The leading-zero scenario is about what the field accepts, so it must not sign in:
	 * submitting would only prove what the server thinks of the number.
	 */
	public void enterPhoneNumber(String countryCode, String phoneNumber) {

		choosePhoneMethod();

		phoneCountryDropdown.selectOption(countryCode);

		typeInto(phoneField, phoneNumber);
	}

	/** The phone number as the field itself holds it, which is not always what was typed. */
	public String phoneAsTyped() {

		String value = phoneField.inputValue();

		return value == null ? "" : value;
	}

	public void submit() {

		clickWhenReady(loginButton, "Login button");
	}

	/**
	 * The browser's own verdict on a field.
	 *
	 * Email is type email and both boxes are required, so an empty or malformed entry never
	 * reaches the server and there is no banner to read - the message lives on the element.
	 */
	public String validationMessageFor(String fieldName) {

		Locator field = page.locator("[name=\'" + fieldName + "\']");

		return (String) field.evaluate("el => el.validationMessage");
	}

	public boolean isFieldValid(String fieldName) {

		Locator field = page.locator("[name=\'" + fieldName + "\']");

		return (Boolean) field.evaluate("el => el.checkValidity()");
	}

	/**
	 * The banner the server's refusal arrives in, or an empty string if none appeared.
	 *
	 * Returning empty rather than throwing lets the step say which scenario was expecting a
	 * refusal and did not get one, which reads better in a report than a raw timeout.
	 */
	public String errorMessage() {

		try {
			errorBanner.waitFor(new Locator.WaitForOptions().setTimeout(15 * 1000));

			return errorBanner.innerText().trim();
		} catch (Exception noBanner) {
			return "";
		}
	}

	/** The inline complaint printed under a box, such as a password below the minimum. */
	public String fieldError() {

		try {
			fieldErrorMessage.waitFor();

			return fieldErrorMessage.innerText().trim();
		} catch (Exception noMessage) {
			return "";
		}
	}

	public String getCurrentUrl() {

		return page.url();
	}

	/** Leaves the login page by the "Forgot password" link. */
	public void openForgotPassword() {

		clickWhenReady(forgotPasswordLink, "Forgot password link");
	}

	/** Leaves the login page by the "Register" link underneath the form. */
	public void openRegister() {

		clickWhenReady(registerLink, "Register link");
	}

	/** Opens the support chat from the button pinned to the corner of the page. */
	public void openCustomerService() {

		clickWhenReady(customerServiceButton, "Customer Service button");
	}

	/**
	 * Whether the support chat is open.
	 *
	 * The chat is a separate application in an iframe, so the presence of that frame is
	 * what says it opened; reading anything inside it would be testing the other product.
	 */
	public boolean isCustomerServiceOpen() {

		try {
			page.locator("xpath=//iframe[contains(@src,'customer')]").first()
					.waitFor(new Locator.WaitForOptions()
							.setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED)
							.setTimeout(20 * 1000));
			return true;
		} catch (com.microsoft.playwright.TimeoutError notOpen) {
			return false;
		}
	}

	/** The language the page is currently shown in, as the switcher reports it. */
	public String selectedLanguage() {

		languageButton.waitFor();

		return languageButton.innerText().trim();
	}

	/** The languages the switcher offers, by name, with the list left open. */
	public List<String> languagesOffered() {

		clickWhenReady(languageButton, "Language switcher");

		List<String> languages = new ArrayList<>();

		for (Locator option : languageOptions()) {
			String name = option.innerText().trim();
			if (!name.isEmpty()) {
				languages.add(name);
			}
		}

		return languages;
	}

	/**
	 * Switches to the first language on offer that is not the one already shown, and says
	 * which it picked.
	 *
	 * The names are the languages' own - Chinese and Thai are written in their own scripts -
	 * so naming one in a scenario would put non-ASCII text in a feature file and in a test
	 * data sheet, where the encoding of whoever runs the suite decides whether it still
	 * matches. Letting the page choose keeps the scenario readable everywhere and still
	 * proves the thing under test: an option can be picked and the page comes back on it.
	 */
	public String switchToAnotherLanguage() {

		String current = selectedLanguage();

		for (String language : languagesOffered()) {
			if (!language.equals(current)) {
				chooseLanguage(language);
				return language;
			}
		}

		return current;
	}

	/** Switches the page to {@code language}, named as the switcher lists it. */
	public void chooseLanguage(String language) {

		if (languageOptions().isEmpty()) {
			clickWhenReady(languageButton, "Language switcher");
		}

		for (Locator option : languageOptions()) {
			if (language.equals(option.innerText().trim())) {
				clickWhenReady(option, "Language " + language);
				return;
			}
		}

		System.out.println("The language switcher does not offer \"" + language + "\"");
	}

	public void loginToUMPay(String username, String pass) {

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			System.out.println("Wait interrupted: " + e.getMessage());
		}

		try {
			email.waitFor();
			email.fill(username);

			passWord.waitFor();
			passWord.fill(pass);

			Thread.sleep(2000);

			loginButton.waitFor();
			loginButton.click();

			System.out.println("Logged In Successfully!");

			Thread.sleep(2000);
		} catch (Exception e) {
			System.out.println("Login operation failed: " + e.getMessage());
		}

	}

	/*
	 * The options are read on demand rather than held as a field: the list is built when the
	 * switcher is opened and torn down when it closes, so a cached set would go stale.
	 */
	private List<Locator> languageOptions() {

		return Wait.all(page.locator("xpath=" + 
				"//button[@id='locale.dropdown-icon']/following-sibling::div//button"));
	}

	/**
	 * Empties a box and types into it.
	 *
	 * An empty value still clears, which is the whole point of the scenarios that submit a
	 * blank field: the box has to be empty when the form is sent, not merely never filled.
	 */
	private void typeInto(Locator field, String value) {

		field.waitFor();

		field.clear();

		if (!value.isEmpty()) {
			field.fill(value);
		}
	}

	/** One retry is rarely enough on this app; three covers the re-render reliably. */
	private static final int CLICK_ATTEMPTS = 3;

	/**
	 * Clicks an element, taking a fresh hold of it each time.
	 *
	 * These pages re-render a moment after they load and again whenever a control changes,
	 * so an element found and then clicked is regularly a different element by the time the
	 * click lands - "stale element reference". Retrying is what fixes it: the field is a
	 * PageFactory proxy, so every attempt looks the element up again and gets whatever is
	 * on the page now. A scripted click as a last resort keeps the old reference and cannot
	 * recover from staleness on its own, which is why it comes after the retries rather
	 * than instead of them.
	 */
	private void clickWhenReady(Locator element, String elementName) {

		for (int attempt = 1; attempt <= CLICK_ATTEMPTS; attempt++) {

			try {
				element.waitFor();
				element.click();
				return;
			} catch (Exception notClickable) {
				System.out.println("Could not click " + elementName + " on attempt " + attempt
						+ " of " + CLICK_ATTEMPTS + ": " + firstLine(notClickable));
			}

			sleep(500);
		}

		try {
			element.dispatchEvent("click");
		} catch (Exception scriptFailed) {
			System.out.println("The scripted click on " + elementName + " failed too: "
					+ firstLine(scriptFailed));
		}
	}

	/**
	 * Picks the country whose label carries {@code diallingCode}.
	 *
	 * Select has no "contains" of its own and the labels carry a flag and a country name
	 * around the code, so the options are walked instead. Failing loudly matters here: a
	 * dialling code that does not exist would otherwise leave the form on whatever country
	 * it defaulted to and report the number as belonging to nobody, which says nothing
	 * about the number.
	 */
	private void selectCountryByDiallingCode(String diallingCode) {

		Locator countries = phoneCountryDropdown;
		String wanted = "(+" + diallingCode.trim() + ")";

		for (Locator option : Wait.all(countries.locator("option"))) {
			if (option.textContent().contains(wanted)) {
				countries.selectOption(option.getAttribute("value"));
				return;
			}
		}

		throw new IllegalStateException("The country list offers no dialling code " + wanted);
	}

	private void sleep(long millis) {

		try {
			Thread.sleep(millis);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	/** Selenium exceptions carry a page of diagnostics; the first line is the reason. */
	private String firstLine(Exception thrown) {

		String message = thrown.getMessage();

		if (message == null) {
			return thrown.getClass().getSimpleName();
		}

		int newline = message.indexOf('\n');

		return newline < 0 ? message : message.substring(0, newline).trim();
	}
}
