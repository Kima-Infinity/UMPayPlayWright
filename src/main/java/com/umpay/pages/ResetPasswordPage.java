package com.umpay.pages;

import com.umpay.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import com.umpay.utility.CaptchaSolver;
import com.umpay.utility.FormInput;
import com.umpay.utility.OtpMailReader;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

/**
 * The Forgot Password journey, reached from the login page.
 *
 * The form asks for an email address or a phone number plus a captcha, and answers with a
 * verification step where a new password would be set. Nothing here ever sets one: the suite
 * signs in as a shared account and a completed reset would change the password every other
 * scenario depends on. The scenarios stop at the verification step, which is as far as this
 * page can be exercised without taking the account away from everyone else.
 */
public class ResetPasswordPage {

	/** Test data value asking for the captcha to be read from the image by OCR. */
	public static final String AUTO_CAPTCHA = "AUTO";

	/** Shortest captcha the backend issues; four digits on the test environment. */
	private static final int MIN_CAPTCHA_LENGTH = 4;

	/**
	 * What the form says when the captcha was misread. The only outcome worth another go:
	 * every other refusal is the answer the scenario came for.
	 */
	private static final String CAPTCHA_REJECTED = "captcha code invalid";

	/**
	 * The reset endpoint refuses a source that asks too often, and says so.
	 *
	 * This matters more here than anywhere else in the suite: a feature file full of reset
	 * scenarios is exactly the traffic pattern it guards against, so without waiting the
	 * block out, scenarios would report this message instead of the one they were written
	 * to check - and each would look like a different failure.
	 */
	private static final String RATE_LIMITED = "blocked for 1 minute";

	/**
	 * The block escalates, and the second step is far past anything a test can wait out.
	 *
	 * Keep asking after the minute-long block and the endpoint answers "The source of
	 * request is blocked for 1 hour." Nothing here can recover from that, so it is named
	 * only so a run says plainly that the source is locked out rather than reporting it as
	 * the application giving a wrong answer. If scenarios start failing on this, the
	 * machine has been running the reset scenarios too often - wait it out.
	 */
	private static final String BLOCKED_FOR_AN_HOUR = "blocked for 1 hour";

	/** A minute plus a little, so the retry lands after the block has actually lifted. */
	private static final long BLOCK_MILLIS = 65_000;

	/** How long to wait for the form to answer a submit at all. */
	private static final int OUTCOME_TIMEOUT_SECONDS = 20;

	Page page;
	
	private final CaptchaSolver captchaSolver = new CaptchaSolver();

	private final OtpMailReader otpMailReader = new OtpMailReader();

	/**
	 * How many messages the mailbox held before the reset was asked for.
	 *
	 * Kept so the code that turns up afterwards can be shown to be a new one. The reset code
	 * goes to the account's real address, which already holds codes from earlier runs, so
	 * without this a check that "a code arrived" would pass on a code from last week.
	 */
	private int mailboxMark = -1;

	/** What the form said in answer to the last submit, recorded while it was still on screen. */
	private String lastAnswer = "";

		private final Locator emailTabButton;


		private final Locator phoneTabButton;


		private final Locator emailField;


		private final Locator phoneCountryDropdown;


		private final Locator phoneField;


		private final Locator captchaField;

	/**
	 * The arrow that leaves the verification step and returns to the form.
	 *
	 * Taken as the nearest button before the "Forgot Password" heading, because it carries
	 * no text, no id and only the generic classes the rest of the form uses. That heading
	 * appears on this step alone, and the nearest button before it is the arrow rather than
	 * the language switcher further up the page.
	 */
		private final Locator backArrow;


	// The same shape the registration form uses, and confirmed against this page.
		private final Locator captchaImage;


		private final Locator refreshCaptchaButton;


		private final Locator nextButton;


	// --- The verification step, shown once the form is accepted ---

		private final Locator verificationCodeField;


		private final Locator newPasswordField;


		private final Locator resendCodeControl;


	// --- How a refusal arrives ---

		private final Locator errorBanner;


		private final Locator fieldErrorMessage;


	public ResetPasswordPage(Page ldriver) {

		this.page = ldriver;
		this.emailTabButton = page.locator("xpath=//button[normalize-space()='Email']");
		this.phoneTabButton = page.locator("xpath=//button[normalize-space()='Phone Number']");
		this.emailField = page.locator("[name=\'email\']");
		this.phoneCountryDropdown = page.locator("[name=\'phoneCountry\']");
		this.phoneField = page.locator("[name=\'phone\']");
		this.captchaField = page.locator("[name=\'captcha\']");
		this.backArrow = page.locator(
				"xpath=//h4[normalize-space()='Forgot Password']/preceding::button[1]");
		this.captchaImage = page.locator("xpath=//input[@name='captcha']/ancestor::div[contains(@class,'gap-2')][1]//img");
		this.refreshCaptchaButton = page.locator("xpath=//input[@name='captcha']/ancestor::div[contains(@class,'gap-2')][1]//button");
		this.nextButton = page.locator("xpath=//button[@type='submit' and normalize-space()='Next']");
		this.verificationCodeField = page.locator("[name=\'otpVerifyCode\']");
		this.newPasswordField = page.locator("[name=\'newPassword\']");
		this.resendCodeControl = page.locator("xpath=//*[contains(text(),'Resend Code')]");
		this.errorBanner = page.locator("xpath=//div[contains(@class,'border-error-500')]//p[contains(@class,'text-sm')]");
		this.fieldErrorMessage = page.locator("xpath=//em[contains(@class,'text-error-600')]");
	}

	/**
	 * Loads the reset page on an empty session, for the same reason the login page does:
	 * the browser is shared by the whole suite and may arrive here signed in.
	 */
	public void open(String resetUrl) {

		page.navigate(resetUrl);

		if (isShowing()) {
			return;
		}

		// Best effort. Whether the session can be cleared this way depends on where the
		// browser happens to be when a scenario starts - a page still settling answers
		// "Access is denied for this document" - and failing to clear is not a reason to
		// give up on opening the page, which the fresh navigation below may well fix.
		try {
			page.evaluate("() => { window.localStorage.clear(); window.sessionStorage.clear(); }");
		} catch (Exception cannotClear) {
			System.out.println("Could not clear the browser's storage: " + cannotClear.getMessage());
		}

		try {
			page.context().clearCookies();
		} catch (Exception cannotClear) {
			System.out.println("Could not clear the cookies: " + cannotClear.getMessage());
		}

		page.navigate(resetUrl);
	}

	/** Whether the address-and-captcha form is on screen. */
	public boolean isShowing() {

		try {
			captchaField.waitFor(new Locator.WaitForOptions().setTimeout(10 * 1000));
			return true;
		} catch (com.microsoft.playwright.TimeoutError notThere) {
			return false;
		}
	}

	public void chooseEmailMethod() {

		clickWhenReady(emailTabButton, "Email method");
		emailField.waitFor();
	}

	public void choosePhoneMethod() {

		clickWhenReady(phoneTabButton, "Phone Number method");
		phoneField.waitFor();
	}

	/** Fills the email form and stops, for the checks the page makes without being asked. */
	public void fillEmailForm(String email, String captchaCode) {

		chooseEmailMethod();

		typeInto(emailField, email);
		typeInto(captchaField, literalCaptcha(captchaCode));
	}

	/** Fills the phone form and stops. */
	public void fillPhoneForm(String countryCode, String phoneNumber, String captchaCode) {

		choosePhoneMethod();

		phoneCountryDropdown.selectOption(countryCode);

		typeInto(phoneField, phoneNumber);
		typeInto(captchaField, literalCaptcha(captchaCode));
	}

	/** Fills the email form and sends it, answering the captcha by OCR when asked to. */
	public void requestResetByEmail(String email, String captchaCode) {

		fillEmailForm(email, captchaCode);
		submitAnsweringCaptcha(captchaCode);
	}

	/** Fills the phone form and sends it, answering the captcha by OCR when asked to. */
	public void requestResetByPhone(String countryCode, String phoneNumber, String captchaCode) {

		fillPhoneForm(countryCode, phoneNumber, captchaCode);
		submitAnsweringCaptcha(captchaCode);
	}

	/**
	 * The same, with the country named by its dialling code.
	 *
	 * The registered number lives in Login_TestData's login sheet, written "855 96443322",
	 * and is read from there rather than copied into this sheet as well - one number in one
	 * place, so changing the account's phone number does not mean remembering to change it
	 * twice.
	 */
	public void requestResetByPhoneDiallingCode(String diallingCode, String phoneNumber,
												String captchaCode) {

		choosePhoneMethod();

		selectCountryByDiallingCode(diallingCode);

		typeInto(phoneField, phoneNumber);
		typeInto(captchaField, literalCaptcha(captchaCode));

		submitAnsweringCaptcha(captchaCode);
	}

	/**
	 * Picks the country whose label carries {@code diallingCode}.
	 *
	 * Failing loudly matters: a code that is not on the list would otherwise leave the form
	 * on whatever country it defaulted to, and the number would be reported as belonging to
	 * nobody - which would say nothing about the number.
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

	public void submit() {

		lastAnswer = "";

		clickWhenReady(nextButton, "Next button");

		lastAnswer = awaitAnswer();
	}

	/** The phone number as the field holds it, which is not always what was typed. */
	public String phoneAsTyped() {

		String value = phoneField.inputValue();

		return value == null ? "" : value;
	}

	public String validationMessageFor(String fieldName) {

		Locator field = page.locator("[name=\'" + fieldName + "\']");

		return (String) field.evaluate("el => el.validationMessage");
	}

	public boolean isFieldValid(String fieldName) {

		Locator field = page.locator("[name=\'" + fieldName + "\']");

		return (Boolean) field.evaluate("el => el.checkValidity()");
	}

	/**
	 * The banner the form answered the last submit with, or empty if it did not.
	 *
	 * The answer is the one recorded when the submit happened rather than a fresh look at
	 * the page. That distinction cost a whole run to find: the banner removes itself after
	 * a few seconds, and a step that waited for it a second time - after the page object
	 * had already waited once to decide whether to retry the captcha - kept arriving to
	 * find it gone, and reported "nothing was said about it" for four different scenarios
	 * that the application had in fact answered correctly.
	 */
	public String errorMessage() {

		if (!lastAnswer.isEmpty()) {
			return lastAnswer;
		}

		try {
			errorBanner.waitFor(new Locator.WaitForOptions().setTimeout(OUTCOME_TIMEOUT_SECONDS * 1000));

			return errorBanner.innerText().trim();
		} catch (Exception noBanner) {
			return "";
		}
	}

	/** The inline complaint printed under a box, such as letters in the phone field. */
	public String fieldError() {

		try {
			fieldErrorMessage.waitFor();

			return fieldErrorMessage.innerText().trim();
		} catch (Exception noMessage) {
			return "";
		}
	}

	/** The captcha picture as a data URI, so a caller can tell one image from another. */
	public String captchaSource() {

		try {
			String source = captchaImage.getAttribute("src");
			return source == null ? "" : source;
		} catch (Exception notThere) {
			return "";
		}
	}

	/**
	 * Asks for a new captcha and waits for the picture to actually change.
	 *
	 * Returning on the click is not enough: the new image arrives afterwards, so a caller
	 * comparing straight away would be handed the picture it already had.
	 */
	public void refreshCaptcha() {

		String before = captchaSource();

		clickWhenReady(refreshCaptchaButton, "Refresh captcha button");

		if (!Wait.until(() -> {
						String now = captchaSource();
						return !now.isEmpty() && !now.equals(before);}, 10)) {
			System.out.println("The captcha image did not change after asking for a new one.");
		}
	}

	/** Whether the form has moved on to where a new password would be set. */
	public boolean isVerificationStepShowing() {

		return isShowing(verificationCodeField) || isShowing(newPasswordField);
	}

	/** Whether the verification step offers to send the code again. */
	public boolean offersResendCode() {

		try {
			resendCodeControl.waitFor(new Locator.WaitForOptions().setTimeout(10 * 1000));
			return true;
		} catch (Exception notOffered) {
			return false;
		}
	}

	/** Records where the mailbox has got to, before anything is asked of the form. */
	public void noteMailboxPosition() {

		mailboxMark = otpMailReader.mailboxSize();

		System.out.println("Mailbox held " + mailboxMark + " messages before the reset was asked for");
	}

	/**
	 * The verification code that arrived for {@code address} after the mailbox was noted,
	 * or an empty string if none did.
	 *
	 * Returning empty rather than throwing keeps the judgement in the step, where the
	 * scenario's own words can say what a missing code means.
	 */
	public String verificationCodeSentTo(String address, int timeoutSeconds) {

		return otpMailReader.waitForCodeAfter(address, timeoutSeconds, mailboxMark);
	}

	/** Whether the mailbox can be read at all, so a step can say why it learned nothing. */
	public boolean canReadTheMailbox() {

		return otpMailReader.isConfigured();
	}

	/**
	 * Leaves the verification step the way a user would, by the arrow on the step itself.
	 *
	 * It used to call page.goBack, which is the browser's back button, and that does not
	 * work here: the reset flow never changes its address. The form and the verification
	 * step are both /forgot-password/email, so there is no history entry between them for
	 * the browser to go back to, and pressing back leaves the flow altogether. The step
	 * carries its own arrow beside the "Forgot Password" heading, which is what a user
	 * would actually press and what does return to the form - both confirmed by hand
	 * against the live page.
	 */
	public void goBack() {

		if (backArrow.count() > 0) {
			backArrow.first().click();
			return;
		}

		// Nothing to press. The browser's back is the wrong instrument here, but it is
		// better than doing nothing at all and leaving the caller to wonder.
		System.out.println("The verification step had no back arrow; using the browser's back.");
		page.goBack();
	}

	public String getCurrentUrl() {

		return page.url();
	}

	/**
	 * Sends the form, reading the captcha from its image when the test data says AUTO.
	 *
	 * A misread captcha is not a result: it is the only answer worth trying again for, so
	 * it takes a fresh image and another reading. Every other outcome - the verification
	 * step, an unknown number, a rate limited source - is what the scenario came to see and
	 * is left alone for the steps to assert on.
	 */
	private void submitAnsweringCaptcha(String captchaCode) {

		boolean readByOcr = AUTO_CAPTCHA.equalsIgnoreCase(captchaCode) && captchaSolver.isEnabled();

		if (!readByOcr) {
			submitWaitingOutAnyBlock();
			return;
		}

		int attempts = captchaSolver.getAttempts();

		for (int attempt = 1; attempt <= attempts; attempt++) {

			String reading = captchaSolver.solve(saveCaptchaImage(), MIN_CAPTCHA_LENGTH);

			if (reading.isBlank()) {
				System.out.println("Nothing usable from this captcha image. Taking a fresh one.");
				refreshCaptcha();
				continue;
			}

			typeInto(captchaField, reading);
			System.out.println("Captcha entered from OCR: " + reading);

			submitWaitingOutAnyBlock();

			if (!lastAnswerWasCaptchaRejection()) {
				return;
			}

			System.out.println("The captcha was misread on attempt " + attempt + " of " + attempts
					+ ". Taking a fresh one.");
			refreshCaptcha();
		}

		System.out.println("OCR did not get the captcha in " + attempts + " attempts.");
	}

	/**
	 * Sends the form, and if the endpoint says the source is blocked, waits the block out
	 * and sends it once more.
	 */
	private void submitWaitingOutAnyBlock() {

		submit();

		if (answerContains(BLOCKED_FOR_AN_HOUR)) {
			System.out.println("The reset endpoint has blocked this source for an hour. Nothing"
					+ " can be learned about the application until it lifts; the scenario will"
					+ " report what the endpoint said.");
			return;
		}

		if (!answerContains(RATE_LIMITED)) {
			return;
		}

		System.out.println("The reset endpoint has blocked this source. Waiting "
				+ (BLOCK_MILLIS / 1000) + " seconds for it to lift.");

		sleep(BLOCK_MILLIS);

		submit();
	}

	private boolean lastAnswerWasCaptchaRejection() {

		return answerContains(CAPTCHA_REJECTED);
	}

	private boolean answerContains(String fragment) {

		return lastAnswer.toLowerCase().contains(fragment.toLowerCase());
	}

	/**
	 * Waits for the form to answer a submit, and returns what it said.
	 *
	 * Three things count as an answer: the verification step appearing, a banner, or an
	 * inline complaint under a field. Only the banner carries text, so the other two return
	 * empty - the steps read those off the page directly, because unlike the banner they
	 * stay put.
	 *
	 * The implicit wait is dropped for the duration: on this screen absence is usually what
	 * is being measured, and paying five seconds for every look would make each captcha
	 * attempt cost far more than it needs to.
	 */
	private String awaitAnswer() {


		try {
			long deadline = System.currentTimeMillis() + (OUTCOME_TIMEOUT_SECONDS * 1000L);

			while (System.currentTimeMillis() < deadline) {

				// Read inside the guard: the banner can be replaced between being found
				// and being read, and a stale element here would end the wait with an
				// exception rather than an answer.
				try {
					if (isShowing(errorBanner)) {
						String said = errorBanner.innerText().trim();
						if (!said.isEmpty()) {
							return said;
						}
					}
				} catch (Exception replacedMidRead) {
					// Try again on the next turn of the loop.
				}

				if (isVerificationStepShowing() || isShowing(fieldErrorMessage)) {
					return "";
				}

				sleep(200);
			}

			return "";

		} finally {
		}
	}

	/**
	 * The captcha is a base64 JPEG embedded in the page, so it can be written to disk
	 * without a second request and read by OCR.
	 */
	private String saveCaptchaImage() {

		String captchaPath = System.getProperty("user.dir") + "/Screenshots/"
				+ new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss").format(new Date()) + "reset-captcha.jpg";

		try {
			captchaImage.waitFor();

			String source = captchaImage.getAttribute("src");
			byte[] image = Base64.getDecoder().decode(source.substring(source.indexOf(",") + 1));

			File target = new File(captchaPath);
			target.getParentFile().mkdirs();

			try (FileOutputStream out = new FileOutputStream(target)) {
				out.write(image);
			}
		} catch (Exception cannotSave) {
			System.out.println("Not able to save the captcha image: " + cannotSave.getMessage());
		}

		return captchaPath;
	}

	/** AUTO is an instruction, not something to type; anything else goes in as it stands. */
	private String literalCaptcha(String captchaCode) {

		return AUTO_CAPTCHA.equalsIgnoreCase(captchaCode) ? "" : captchaCode;
	}

	private boolean isShowing(Locator element) {

		try {
			return element.isVisible();
		} catch (Exception notThere) {
			return false;
		}
	}

	/**
	 * Types into a field and checks the value stays there.
	 *
	 * Typing plainly is not enough on this form. Choosing the country or the phone number
	 * re-renders it, and a captcha typed before that settles is wiped without any error -
	 * the box looks filled, the form sends nothing, and the server answers "Captcha is
	 * required" for a code that was typed correctly. FormInput retypes until the value
	 * survives the next render, which is the problem it was written for.
	 */
	private void typeInto(Locator field, String value) {

		field.waitFor();

		if (value.isEmpty()) {
			field.clear();
			return;
		}

		FormInput.type(field, value, "reset form field");
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

	/** Selenium exceptions carry a page of diagnostics; the first line is the reason. */
	private String firstLine(Exception thrown) {

		String message = thrown.getMessage();

		if (message == null) {
			return thrown.getClass().getSimpleName();
		}

		int newline = message.indexOf('\n');

		return newline < 0 ? message : message.substring(0, newline).trim();
	}

	private void sleep(long millis) {

		try {
			Thread.sleep(millis);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}
}
