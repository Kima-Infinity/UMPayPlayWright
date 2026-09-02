package com.umpay.pages;

import com.umpay.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import com.umpay.utility.CaptchaSolver;
import com.umpay.utility.OtpMailReader;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.List;
import java.util.Base64;
import java.util.Date;

public class RegisterPage {

	/**
	 * Captcha value that tells the page object to stop and let a human read the
	 * image and type the code into the browser. The captcha is an image served
	 * by the backend, so the happy path cannot run fully unattended.
	 */
	public static final String MANUAL_CAPTCHA = "MANUAL";

	/**
	 * Captcha value that asks the ddddocr model to read the image instead of a
	 * human. The model guesses wrong often enough that a rejected code refreshes
	 * the captcha and tries again - see {@link #submitWithCaptcha}.
	 */
	public static final String AUTO_CAPTCHA = "AUTO";

	Page page;
	
	private final CaptchaSolver captchaSolver = new CaptchaSolver();

	private final OtpMailReader otpMailReader = new OtpMailReader();

		private final Locator emailTabButton;


		private final Locator phoneTabButton;


		private final Locator emailField;


		private final Locator phoneCountryDropdown;


		private final Locator phoneField;


		private final Locator passWord;


		private final Locator showPasswordButton;


		private final Locator captchaField;


		private final Locator captchaImage;


		private final Locator refreshCaptchaButton;


	// --- Email verification step, shown after the form is accepted ---

		private final Locator otpField;


		private final Locator otpNextButton;


		private final Locator registerButton;


		private final Locator termsAndConditionsLink;


		private final Locator loginLink;


		private final Locator toastMessage;

	/**
	 * The alert the registration form raises when the application refuses.
	 *
	 * The message sits in a paragraph inside a bordered block. Anchored on the block,
	 * because the paragraph's own classes are the same generic ones the rest of the form
	 * carries, while border-primary-900 appears on this screen only when an alert does.
	 */
		private final Locator alertMessage;


	/**
	 * Field level rejections render inline underneath the field as an
	 * em.text-error-600, not as a toast. A wrong captcha is reported this way,
	 * confirmed against the test environment.
	 */
		private final Locator fieldErrorMessage;


	public RegisterPage(Page ldriver) {

		this.page = ldriver;
		this.emailTabButton = page.locator("xpath=//button[normalize-space()='Email']");
		this.phoneTabButton = page.locator("xpath=//button[normalize-space()='Phone Number']");
		this.emailField = page.locator("[name=\'email\']");
		this.phoneCountryDropdown = page.locator("[name=\'phoneCountry\']");
		this.phoneField = page.locator("[name=\'phone\']");
		this.passWord = page.locator("[name=\'password\']");
		this.showPasswordButton = page.locator("xpath=//input[@name='password']/following-sibling::button");
		this.captchaField = page.locator("[name=\'captcha\']");
		this.captchaImage = page.locator("xpath=//input[@name='captcha']/ancestor::div[contains(@class,'gap-2')][1]//img");
		this.refreshCaptchaButton = page.locator("xpath=//input[@name='captcha']/ancestor::div[contains(@class,'gap-2')][1]//button");
		this.otpField = page.locator("xpath=//input[contains(@placeholder,'OTP') or contains(@placeholder,'6-digits')]");
		this.otpNextButton = page.locator("xpath=//button[normalize-space()='Next']");
		this.registerButton = page.locator("xpath=//button[@type='submit' and normalize-space()='Register']");
		this.termsAndConditionsLink = page.locator("xpath=//a[normalize-space()='Terms and Conditions']");
		this.loginLink = page.locator("xpath=//a[@href='/login']");
		this.toastMessage = page.locator("xpath=//div[contains(@class,'Toastify__toast-body')]");
		this.alertMessage = page.locator("xpath=//div[contains(@class,'border-primary-900')]//p");
		this.fieldErrorMessage = page.locator("xpath=//em[contains(@class,'text-error-600')]");
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
	 * Loads the registration page.
	 *
	 * The address comes from the caller because it lives in the suite's configuration,
	 * which is test-scoped; what belongs here is knowing that opening this page means
	 * a fresh navigation rather than a click from somewhere else.
	 */
	public void open(String registrationUrl) {

		page.navigate(registrationUrl);

	}

	/** Leaves the registration page by the "Login" link underneath the form. */
	public void openLoginPage() {

		clickWhenReady(loginLink, "Login link");

	}

	public void openEmailTab() {

		clickWhenReady(emailTabButton, "Email tab");
		emailField.waitFor();
	}

	public void openPhoneTab() {

		clickWhenReady(phoneTabButton, "Phone Number tab");
		phoneField.waitFor();
	}

	/**
	 * Fills the email variant of the form. Kept separate from the submit so that
	 * the validation-only scenarios can inspect the fields without registering.
	 */
	public void fillEmailRegistrationForm(String email, String password) {

		openEmailTab();

		emailField.waitFor();
		emailField.clear();
		emailField.fill(email);

		passWord.waitFor();
		passWord.clear();
		passWord.fill(password);

		System.out.println("Registration form filled with email: " + email);
	}

	/**
	 * Fills the phone variant of the form. The country dropdown is a native
	 * select whose values are ISO country codes, e.g. HK, CN, IN, US.
	 */
	public void fillPhoneRegistrationForm(String countryCode, String phoneNumber, String password) {

		openPhoneTab();

		phoneCountryDropdown.waitFor();
		Locator country = phoneCountryDropdown;
		try {
			country.selectOption(countryCode);
		} catch (Exception e) {
			System.out.println("Country code " + countryCode + " not found, selecting by visible text instead: " + e.getMessage());
			country.selectOption(new com.microsoft.playwright.options.SelectOption().setLabel(countryCode));
		}
		System.out.println("Country selected: " + country.locator("option:checked").textContent());

		phoneField.waitFor();
		phoneField.clear();
		phoneField.fill(phoneNumber);

		passWord.waitFor();
		passWord.clear();
		passWord.fill(password);

		System.out.println("Registration form filled with phone number: " + phoneNumber);
	}

	/**
	 * Types the captcha code. When the code is blank or {@link #MANUAL_CAPTCHA},
	 * the image is written to the Screenshots folder and the run waits for a
	 * human to type the code into the open browser.
	 * A {@code -Dumpay.captcha=123456} system property overrides the test data,
	 * which is how the suite is meant to run on a captcha bypassed environment.
	 */
	public void enterCaptcha(String captchaCode, int manualTimeoutSeconds) {

		captchaField.waitFor();

		String override = System.getProperty("umpay.captcha");
		if (override != null && !override.isBlank()) {
			System.out.println("Captcha taken from the umpay.captcha system property");
			captchaCode = override;
		}

		// AUTO lands here only as a fallback: reading the image is worthwhile solely
		// in the submit-and-retry cycle that owns it, so submitWithCaptcha handles
		// that case and calls this method for a human once OCR is out of attempts.
		if (captchaCode == null || captchaCode.isBlank()
				|| MANUAL_CAPTCHA.equalsIgnoreCase(captchaCode)
				|| AUTO_CAPTCHA.equalsIgnoreCase(captchaCode)) {

			System.out.println("Captcha image saved to: " + saveCaptchaImage());
			System.out.println("Type the captcha code into the browser within " + manualTimeoutSeconds + " seconds.");

			System.out.println("Captcha entered manually: " + waitForTypedCaptcha(manualTimeoutSeconds));
		} else {
			captchaField.clear();
			captchaField.fill(captchaCode);
			System.out.println("Captcha entered from test data: " + captchaCode);
		}
	}

	/** Shortest captcha the backend issues; four digits on the test environment. */
	private static final int MIN_CAPTCHA_LENGTH = 4;

	/** The emailed one time code is always six digits. */
	/** How long the verification step gets to appear after the form is submitted. */
	private static final int OTP_STEP_SECONDS = 20;

	/**
	 * Refusals that a fresh captcha will never turn into an acceptance.
	 *
	 * Matched in lower case against whatever the application says. Anything not on this
	 * list is treated as the captcha's fault and retried, which is the right default: OCR
	 * is expected to misread some images.
	 *
	 * The rate limit belongs here for the same reason the others do, even though it is
	 * temporary where they are permanent: it lasts an hour, and no run waits that long. A
	 * blocked source spent all ten captcha attempts and then stopped for a person to type
	 * a code, turning a clear "the endpoint is blocked" into several minutes of apparent
	 * captcha trouble. Failing in seconds with the reason on the screen is worth more.
	 */
	private static final List<String> FINAL_REFUSALS =
			List.of("user already exists", "already registered", "already in use",
					"blocked for 1 hour");

	/**
	 * The last refusal seen on screen, kept because it does not stay there.
	 *
	 * The message arrives as a toast and leaves on its own after a few seconds. Every
	 * caller that wants it arrives after it has gone, so it is written down as it passes
	 * rather than looked for afterwards.
	 */
	private String lastRefusal = "";

	/**
	 * Where the picture of the last refusal was written.
	 *
	 * Kept so the step can put it in the report. A String rather than anything richer,
	 * because a page object reporting on itself is a page object doing the test's job.
	 */
	private String lastRefusalScreenshot = "";

	private static final int OTP_LENGTH = 6;

	/** Codes expire and get mistyped, so allow a few goes before failing the run. */
	private static final int OTP_ATTEMPTS = 3;

	/** How long a field must stop changing before the code counts as finished. */
	private static final long TYPING_SETTLE_MILLIS = 1500;

	private String waitForTypedCaptcha(int manualTimeoutSeconds) {
		return waitForTypedValue(captchaField, MIN_CAPTCHA_LENGTH, manualTimeoutSeconds,
				"captcha code", "The image is in the Screenshots folder.");
	}

	/**
	 * Waits for a human to finish typing into a field.
	 *
	 * Returning as soon as the field is non-blank is not enough: that fires on the
	 * first keystroke, so a four digit captcha submitted as "1" and the backend
	 * rejected it. This waits until the value is long enough AND has stopped
	 * changing, which is the only reliable signal that typing has finished — there
	 * is no submit event to hook.
	 */
	private String waitForTypedValue(Locator field, int minLength, int timeoutSeconds,
			String what, String hint) {

		long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);
		String lastSeen = "";
		long lastChangedAt = System.currentTimeMillis();

		while (System.currentTimeMillis() < deadline) {

			String typed = field.inputValue();
			typed = (typed == null) ? "" : typed.trim();

			if (!typed.equals(lastSeen)) {
				lastSeen = typed;
				lastChangedAt = System.currentTimeMillis();
			}

			boolean longEnough = lastSeen.length() >= minLength;
			boolean settled = (System.currentTimeMillis() - lastChangedAt) >= TYPING_SETTLE_MILLIS;

			if (longEnough && settled) {
				return lastSeen;
			}

			try {
				Thread.sleep(250);
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				break;
			}
		}

		throw new IllegalStateException("No " + what + " was typed within " + timeoutSeconds
				+ " seconds (saw \"" + lastSeen + "\"). " + hint);
	}

	/**
	 * Completes the email verification step that follows a successful submit.
	 *
	 * Registration is only finished once this six digit code is accepted — the form
	 * submit alone leaves the account uncreated, which is why the scenario used to
	 * pass while stopping short of registering anyone. The code is emailed, so a
	 * human reads it and types it into the open browser, same as the captcha.
	 */
	public void completeOtpVerification(int manualTimeoutSeconds) {

		completeOtpVerification(null, manualTimeoutSeconds);
	}

	/**
	 * As above, but reads the code out of the mailbox when IMAP is configured.
	 *
	 * @param registeredEmail the +alias the form registered, which is what the
	 *                        message is matched on. Null falls back to a human.
	 */
	public void completeOtpVerification(String registeredEmail, int manualTimeoutSeconds) {

		otpField.waitFor();

		// Empty the box before the first wait, not just after a rejection. Chrome
		// offers a previous run's code here, and a prefilled field is already six
		// settled digits - so the wait returns instantly with a stale code that
		// nobody typed and the backend has long since expired.
		clearOtpField();

		if (registeredEmail != null && !registeredEmail.isBlank() && otpMailReader.isConfigured()) {

			if (submitCodeFromMailbox(registeredEmail, manualTimeoutSeconds)) {
				return;
			}

			// The mailbox route did not finish. The browser is still on the code step,
			// so a human can pick it up from here rather than losing the registration.
			System.out.println("Falling back to typing the verification code by hand.");
			clearOtpField();
		}

		System.out.println("Verification code sent by email. Type the 6 digit code into the browser within "
				+ manualTimeoutSeconds + " seconds.");

		// Codes get mistyped and they expire, so a single attempt is not enough. Each
		// pass reads what was typed, submits it, and if the step does not clear, empties
		// the box and waits for the next attempt.
		for (int attempt = 1; attempt <= OTP_ATTEMPTS; attempt++) {

			String code = waitForTypedValue(otpField, OTP_LENGTH, manualTimeoutSeconds,
					"verification code", "Check the inbox for the address the form registered.");

			System.out.println("Verification code entered manually: " + code
					+ " (attempt " + attempt + " of " + OTP_ATTEMPTS + ")");

			clickWhenReady(otpNextButton, "Verification code Next button");

			try {
				// The step is done when the code box goes away.
				otpField.waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN).setTimeout(15 * 1000));
				return;
			} catch (com.microsoft.playwright.TimeoutError notAccepted) {

				String reason = getErrorMessage();
				System.out.println("The code " + code + " was not accepted"
						+ (reason.isBlank() ? "" : ": " + reason)
						+ ". Clearing the box - type the current code again.");

				clearOtpField();
			}
		}

		throw new IllegalStateException("The verification code was not accepted after "
				+ OTP_ATTEMPTS + " attempts. The account was not created.");
	}

	/**
	 * Reads the code from the mailbox and submits it, returning whether the step
	 * cleared.
	 *
	 * Only one attempt is made. Unlike a mistyped code there is nothing to correct
	 * by trying again: the mailbox would hand back the same digits, so a rejection
	 * here means the code expired or the account is in a state the retry cannot
	 * fix, and a human is better placed to see why.
	 */
	private boolean submitCodeFromMailbox(String registeredEmail, int timeoutSeconds) {

		String code = otpMailReader.waitForCode(registeredEmail, timeoutSeconds);

		if (code.isBlank()) {
			return false;
		}

		otpField.fill(code);

		System.out.println("Verification code entered from the mailbox: " + code);

		clickWhenReady(otpNextButton, "Verification code Next button");

		try {
			otpField.waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN).setTimeout(15 * 1000));
			return true;
		} catch (com.microsoft.playwright.TimeoutError notAccepted) {

			String reason = getErrorMessage();
			System.out.println("The mailbox code " + code + " was not accepted"
					+ (reason.isBlank() ? "" : ": " + reason));

			return false;
		}
	}

	/** Empties the code box so the next wait sees fresh typing rather than the old value. */
	private void clearOtpField() {

		try {
			otpField.fill("");
		} catch (Exception e) {
			System.out.println("Could not clear the verification code box: " + e.getMessage());
		}
	}

	/** True while the emailed one time code is being asked for. */
	public boolean isOtpStepDisplayed() {
		try {
			return otpField.isVisible();
		} catch (Exception notThere) {
			return false;
		}
	}

	/**
	 * Whether the flow has reached the verification step, giving it time to arrive.
	 *
	 * isOtpStepDisplayed above asks whether the field is on screen this instant, which is
	 * the right question when the answer is expected to be no. This is the opposite case:
	 * the form has just been submitted and the step is waiting for the application to move
	 * on, so an instant answer would report "not there" before the page had a chance to
	 * render it. Under Selenium the implicit wait hid this difference; Playwright answers
	 * immediately and truthfully, so the waiting has to be asked for.
	 */
	public boolean hasReachedOtpStep() {

		return Wait.appears(otpField, OTP_STEP_SECONDS);

	}

	/**
	 * Asks the backend for a different captcha and waits until the image really
	 * changed.
	 *
	 * Returning as soon as the button is clicked is not enough for the retry loop:
	 * the new image arrives asynchronously, so the next read would grab the old
	 * base64 src and feed OCR the very picture it just failed on, burning attempts
	 * on a captcha that can never be different.
	 */
	public void refreshCaptcha() {

		String before = captchaSource();

		clickWhenReady(refreshCaptchaButton, "Refresh captcha button");

		if (!Wait.until(() -> {
						String now = captchaSource();
						return !now.isEmpty() && !now.equals(before);}, 10)) {
			System.out.println("The captcha image did not change within 10 seconds of the refresh");
		}
	}

	/** The image's base64 src, or an empty string when it cannot be read. */
	private String captchaSource() {

		try {
			String source = captchaImage.getAttribute("src");
			return source == null ? "" : source;
		} catch (Exception notThere) {
			return "";
		}
	}

	/**
	 * The captcha is a base64 JPEG embedded in the page, so it can be written to
	 * disk without a second request and read by a human or an OCR step.
	 */
	public String saveCaptchaImage() {

		String captchaPath = System.getProperty("user.dir") + "/Screenshots/" + getCurrentDateTime() + "captcha.jpg";

		try {
			captchaImage.waitFor();

			String source = captchaImage.getAttribute("src");
			byte[] image = Base64.getDecoder().decode(source.substring(source.indexOf(",") + 1));

			File target = new File(captchaPath);
			target.getParentFile().mkdirs();

			try (FileOutputStream out = new FileOutputStream(target)) {
				out.write(image);
			}
		} catch (Exception e) {
			System.out.println("Not able to save the captcha image: " + e.getMessage());
		}

		return captchaPath;
	}

	public void submitRegistration() {

		clickWhenReady(registerButton, "Register button");

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			System.out.println("Wait interrupted: " + e.getMessage());
		}
	}

	public void registerWithEmail(String email, String password, String captchaCode, int manualTimeoutSeconds) {

		fillEmailRegistrationForm(email, password);
		submitWithCaptcha(captchaCode, manualTimeoutSeconds);
	}

	public void registerWithPhone(String countryCode, String phoneNumber, String password, String captchaCode, int manualTimeoutSeconds) {

		fillPhoneRegistrationForm(countryCode, phoneNumber, password);
		submitWithCaptcha(captchaCode, manualTimeoutSeconds);
	}

	/**
	 * Answers the captcha and submits, retrying on rejection when the code came
	 * from OCR.
	 *
	 * The retry only applies to {@link #AUTO_CAPTCHA}. A code that came from the
	 * test data is submitted exactly once, because the negative scenario depends
	 * on a deliberately wrong captcha being rejected and staying rejected. A human
	 * typing the code is not retried either - they can see the image, so a second
	 * guess adds nothing.
	 *
	 * A rejected OCR reading is not an error worth failing on: the model is
	 * expected to misread some images. Only exhausting every attempt is a failure.
	 */
	private void submitWithCaptcha(String captchaCode, int manualTimeoutSeconds) {

		boolean readByOcr = AUTO_CAPTCHA.equalsIgnoreCase(captchaCode) && captchaSolver.isEnabled();

		if (!readByOcr) {
			enterCaptcha(captchaCode, manualTimeoutSeconds);
			submitRegistration();
			return;
		}

		int attempts = captchaSolver.getAttempts();

		for (int attempt = 1; attempt <= attempts; attempt++) {

			System.out.println("Captcha attempt " + attempt + " of " + attempts);

			String reading = captchaSolver.solve(saveCaptchaImage(), MIN_CAPTCHA_LENGTH);

			// A misread costs nothing to discard here. Submitting it would spend a
			// form round trip and the full rejection timeout to learn what the length
			// check already knows, so take a fresh image instead.
			if (reading.isBlank()) {
				System.out.println("Nothing usable from this image. Taking a fresh captcha.");
				refreshCaptcha();
				continue;
			}

			captchaField.clear();
			captchaField.fill(reading);
			System.out.println("Captcha entered from OCR: " + reading);

			lastRefusal = "";

			submitRegistration();

			if (captchaAccepted()) {
				System.out.println("Captcha accepted on attempt " + attempt);
				return;
			}

			// The captcha was fine and the application still said no. Nothing about a
			// fresh image would change that answer, and retrying would spend the rest of
			// the attempts before stopping for a person who is not there.
			if (refusedForGood()) {
				System.out.println("The application refused for good: " + lastRefusal
						+ ". Not retrying the captcha.");
				return;
			}

			System.out.println("The captcha " + reason("was rejected") + ". Taking a fresh captcha.");
			refreshCaptcha();
		}

		// Out of attempts rather than out of options: the browser is still sitting on
		// the form, so a human present at the keyboard can finish what OCR could not.
		System.out.println("OCR did not get the captcha in " + attempts + " attempts. Over to you.");

		enterCaptcha(MANUAL_CAPTCHA, manualTimeoutSeconds);
		submitRegistration();
	}

	/**
	 * True once the form has moved on. The email verification step replacing the
	 * form is the positive signal; a rejected captcha leaves the form in place
	 * with an inline error, so this simply times out and the caller tries again.
	 */
	private boolean captchaAccepted() {

		// The page carries a five second implicit wait, which would charge five
		// seconds for every look at an element that is not there - and on this screen
		// something is always missing, because absence is exactly what is being
		// measured. Dropping it makes both answers immediate, so a rejected captcha
		// costs a moment instead of the full timeout. Restored in the finally block.

		try {
			long deadline = System.currentTimeMillis() + OUTCOME_TIMEOUT_MILLIS;

			while (System.currentTimeMillis() < deadline) {

				if (isShowing(otpField) || !isShowing(registerButton)) {
					return true;
				}

				recordRefusal();

				// A refusal on screen is the answer. The form did not move on, and waiting
				// out the rest of the deadline only delays what is already decided - which
				// on this form is the whole deadline, because it raises an alert rather
				// than the inline field error the check below looks for.
				if (!lastRefusal.isEmpty()) {
					return false;
				}

				if (isShowing(fieldErrorMessage)) {
					return false;
				}

				try {
					Thread.sleep(250);
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					return false;
				}
			}

			return false;

		} finally {
		}
	}

	/** How long to let a submitted captcha declare itself accepted or rejected. */
	private static final long OUTCOME_TIMEOUT_MILLIS = 15000;

	/** How long to look for a message that has probably already gone. */
	private static final double MESSAGE_TIMEOUT_MILLIS = 2000;

	/** Matches the implicit wait BrowserFactory sets, so it can be put back. */
	private static final Duration IMPLICIT_WAIT = Duration.ofSeconds(5);

	/** Present and visible right now, with no waiting either way. */
	private boolean isShowing(Locator element) {

		try {
			return element.isVisible();
		} catch (Exception notThere) {
			return false;
		}
	}

	/** Appends the backend's own words to a rejection line, when it left any on screen. */
	/**
	 * Writes down a refusal if one is on screen right now.
	 *
	 * Called from the outcome poll, so it runs every 250ms for as long as the page is
	 * being watched - often enough to catch a toast that only lives a few seconds. The
	 * first message of an attempt wins; a later look finding nothing does not erase it.
	 */
	private void recordRefusal() {

		if (!lastRefusal.isEmpty()) {
			return;
		}

		// The alert first: it is the one this form actually raises. The other two are kept
		// because other screens use them and a refusal is worth catching wherever it lands.
		for (Locator candidate : List.of(alertMessage, fieldErrorMessage, toastMessage)) {
			try {
				if (candidate.count() > 0 && candidate.first().isVisible()) {

					String text = candidate.first().innerText().trim();

					if (!text.isEmpty()) {
						lastRefusal = text;
						System.out.println("Noted the refusal while it was on screen: " + text);
						saveRefusalScreenshot();
						return;
					}
				}
			} catch (Exception gone) {
				// It left between the check and the read. The next tick will try again.
			}
		}
	}

	/**
	 * Saves a picture of the page while the refusal is still showing.
	 *
	 * This is the only moment worth photographing. The alert clears itself after a few
	 * seconds, so the screenshot the tear down takes on failure shows an ordinary empty
	 * form and proves nothing about what the application said.
	 *
	 * Best effort: a run that cannot write the file is not a run worth failing, and the
	 * message itself has already been recorded either way.
	 */
	private void saveRefusalScreenshot() {

		String path = System.getProperty("user.dir") + "/Screenshots/"
				+ getCurrentDateTime() + "refusal.png";

		try {
			File target = new File(path);
			target.getParentFile().mkdirs();

			page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(path)));

			lastRefusalScreenshot = path;

			System.out.println("Refusal captured in: " + path);

		} catch (Exception cannotSave) {
			System.out.println("Not able to save the refusal screenshot: " + cannotSave.getMessage());
		}
	}

	/**
	 * The picture taken while the last refusal was on screen, or an empty string if there
	 * was none to take.
	 */
	public String getRefusalScreenshot() {

		return lastRefusalScreenshot;

	}

	/** Whether the application has refused for a reason no new captcha will change. */
	private boolean refusedForGood() {

		String message = lastRefusal.toLowerCase();

		return FINAL_REFUSALS.stream().anyMatch(message::contains);

	}

	/**
	 * A wait short enough to ask about something that is probably not there.
	 *
	 * Playwright's default is thirty seconds, which is right when the answer is expected to
	 * be yes and far too long when the question is "is anything showing?". These calls are
	 * the second kind: they run after the outcome is already decided, purely to put a
	 * message on it.
	 */
	private static Locator.WaitForOptions briefly() {

		return new Locator.WaitForOptions().setTimeout(MESSAGE_TIMEOUT_MILLIS);

	}

	private String reason(String prefix) {

		String message = getErrorMessage();
		return message.isBlank() ? prefix : prefix + ": " + message;
	}

	/**
	 * Text of the toast the backend raises after a submit, or an empty string
	 * when no toast is on screen.
	 */
	public String getToastMessage() {

		try {
			toastMessage.waitFor(briefly());
			String message = toastMessage.innerText();
			System.out.println("Toast message: " + message);
			return message;
		} catch (Exception e) {
			System.out.println("No toast message displayed: " + e.getMessage());
			return "";
		}
	}

	/**
	 * Reason a submitted form was rejected. The application reports field level
	 * problems inline and falls back to a toast for anything else, so both
	 * surfaces are checked before giving up.
	 */
	public String getErrorMessage() {

		// Seen and written down while it was on screen. There is nothing to wait for, and
		// waiting anyway costs a minute across the two absent elements below.
		if (!lastRefusal.isEmpty()) {
			return lastRefusal;
		}

		try {
			fieldErrorMessage.waitFor(briefly());
			String message = fieldErrorMessage.innerText();
			System.out.println("Inline error message: " + message);
			return message;
		} catch (Exception e) {
			System.out.println("No inline error message displayed, checking for a toast instead");

			String toast = getToastMessage();

			// A toast that has already gone still counts. It was there, the poll saw it,
			// and the caller asking now is not a reason to pretend it never appeared.
			return toast.isBlank() ? lastRefusal : toast;
		}
	}

	/**
	 * True while the browser is still showing the registration form, which is
	 * how a rejected submission is told apart from an accepted one.
	 */
	public boolean isRegistrationFormDisplayed() {

		try {
			return registerButton.isVisible();
		} catch (Exception e) {
			return false;
		}
	}

	public String getCurrentUrl() {

		return page.url();
	}

	/**
	 * The form relies on HTML5 constraints - email is type email, password
	 * carries pattern ".{6,}", and email, password and captcha are all required.
	 * These read the browser's own verdict, so the short password and empty
	 * field scenarios need no captcha at all.
	 */
	public String getValidationMessage(String fieldName) {

		Locator field = page.locator("[name=\'" + fieldName + "\']");
		return (String) field.evaluate("el => el.validationMessage");
	}

	public boolean isFieldValid(String fieldName) {

		Locator field = page.locator("[name=\'" + fieldName + "\']");
		return (Boolean) field.evaluate("el => el.checkValidity()");
	}

	private String getCurrentDateTime() {

		return new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss").format(new Date());
	}
}
