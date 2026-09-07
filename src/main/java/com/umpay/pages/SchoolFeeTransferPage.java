package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import java.time.Duration;

/**
 * The school fee transfer form at /school-fee.
 *
 * Reached from Global Transfer as "UMPay Transfer to School Fees", and also from the left
 * navigation as "International School Fees" - the same form under two names, which is why
 * a scenario naming either should land here.
 *
 * It is the simplest of the transfer forms: a source wallet, an amount whose placeholder
 * states the minimum, and a remark.
 *
 * NOTHING HERE SENDS THE TRANSFER.
 */
public class SchoolFeeTransferPage {

	private final Page page;
		private final Locator heading;


		private final Locator amountField;


		private final Locator remarkField;


		private final Locator sourceWalletButton;


		private final Locator nextButton;


	public SchoolFeeTransferPage(Page ldriver) {

		this.page = ldriver;
		this.heading = page.locator("xpath=//*[normalize-space(text())='International School Fee']");
		this.amountField = page.locator("[name=\'amount\']");
		this.remarkField = page.locator("[name=\'remark\']");
		this.sourceWalletButton = page.locator("xpath=//button[contains(.,'Available Balance')]");
		this.nextButton = page.locator("xpath=//button[normalize-space()='Next']");
	}

	public boolean isShowing() {

		return isPresent(page.locator("xpath=" + "//*[normalize-space(text())='International School Fee']"));

	}

	public boolean hasAmountField() {

		return isPresent(page.locator("[name=\'amount\']"));

	}

	public boolean hasRemarkField() {

		return isPresent(page.locator("[name=\'remark\']"));

	}

	public boolean showsSourceWalletBalance() {

		return isPresent(page.locator("xpath=" + "//button[contains(.,'Available Balance')]"));

	}

	/**
	 * The minimum the form states, taken from the amount box's placeholder.
	 *
	 * Returned rather than asserted here so the scenario can name the figure it expects and
	 * a change to it fails somewhere a reader can see.
	 */
	public String statedMinimum() {

		amountField.waitFor();

		// Waited for, not read once. The placeholder arrives with the wallet's own limits a
		// moment after the form draws, and a reading taken before that comes back empty - which
		// is not "this form states no minimum" but "nobody has been told yet".
		com.umpay.utility.Wait.until(() -> amountField.getAttribute("placeholder") != null, 15);

		String stated = amountField.getAttribute("placeholder");

		if (stated == null) {
			return "";
		}

		// "Minimum: 10.00" is how the box says it; the figure is what a caller wants.
		java.util.regex.Matcher figure =
				java.util.regex.Pattern.compile("([0-9][0-9,]*(?:\\.[0-9]+)?)").matcher(stated);

		return figure.find() ? figure.group(1).replace(",", "") : stated;
	}

	/** What the wallet paying the fee holds, as the form states it. */
	public String availableBalance() {

		try {
			return sourceWalletButton.first().innerText().replaceAll("\\s+", " ").trim();
		} catch (Exception notThere) {
			return "";
		}
	}

	/**
	 * What the form works out once an amount is entered: the amount, the fee, and the total.
	 *
	 * The three arrive together and only after an amount is entered, so they are read together.
	 * Empty where the form has not worked them out yet.
	 */
	public String feeBreakdown() {

		try {
			return page.locator("xpath=//*[contains(normalize-space(.),'You Will Pay')][last()]")
					.first().innerText().replaceAll("\\s+", " ").trim();
		} catch (Exception notWorkedOut) {
			return "";
		}
	}

	/** One figure out of that breakdown - "Fee", "Transaction Amount", "You Will Pay". */
	public String breakdownFigure(String named) {

		java.util.regex.Matcher figure = java.util.regex.Pattern
				.compile(java.util.regex.Pattern.quote(named) + " ([0-9][0-9,]*(?:\\.[0-9]+)?)")
				.matcher(feeBreakdown());

		return figure.find() ? figure.group(1).replace(",", "") : "";
	}

	/**
	 * Goes on from the amount to the step that asks which school is being paid.
	 *
	 * Next does not send anything: the flow asks for the country and the school afterwards, so
	 * this is safe in a way a Confirm would not be.
	 */
	public void next() {

		nextButton.first().click();

		settle();
	}

	/**
	 * True once the form has moved on to choosing who is being paid.
	 *
	 * On screen, not merely in the document. The country list is in the page before the step is
	 * reached, so a presence check answered yes while the form was still asking for an amount -
	 * which made a form that had refused to go anywhere look like one that had gone on.
	 */
	public boolean asksWhoIsBeingPaid() {

		try {
			for (Locator sign : com.umpay.utility.Wait.all(page.locator(
					"xpath=//*[normalize-space(text())='Template']"
					+ " | //*[normalize-space(text())='AFGHANISTAN (AF)']"))) {

				if (sign.isVisible()) {
					return true;
				}
			}
		} catch (Exception notThere) {
			return false;
		}

		return false;
	}

	/**
	 * The school step's own boxes, by the names the form gives them.
	 *
	 * Read rather than typed into by this suite: what fills them is a saved template, and the
	 * point of the scenario is that choosing one carries the school and the student across.
	 */
	private static final String SCHOOL_NAME = "schoolPaymentInfo[schoolInfo][name]";

	private static final String SCHOOL_CODE = "schoolPaymentInfo[schoolInfo][code]";

	private static final String SCHOOL_ADDRESS = "schoolPaymentInfo[schoolInfo][address]";

	private static final String STUDENT_NAME = "schoolPaymentInfo[studentInfo][name]";

	private static final String STUDENT_ID = "schoolPaymentInfo[studentInfo][identityNumber]";

	/** Opens the list of schools this account has saved. */
	public void openSavedSchools() {

		page.locator("xpath=//button[normalize-space()='Template']").first().click();

		settle();
	}

	/** True once the step is asking for the school and the student. */
	public boolean asksForTheSchool() {

		return isPresent(page.locator("xpath=//input[@name=\"schoolPaymentInfo[schoolInfo][name]\"]"));
	}

	public String schoolName() {

		return valueOf(SCHOOL_NAME);
	}

	public String schoolCode() {

		return valueOf(SCHOOL_CODE);
	}

	public String schoolAddress() {

		return valueOf(SCHOOL_ADDRESS);
	}

	public String studentName() {

		return valueOf(STUDENT_NAME);
	}

	public String studentId() {

		return valueOf(STUDENT_ID);
	}

	/**
	 * Chooses the school's country, which the form asks for as a code.
	 *
	 * A select of two hundred and two countries, holding codes rather than names - KH, not
	 * CAMBODIA - so the sheet keeps the code and this sets it outright.
	 */
	public void chooseCountry(String code) {

		page.locator("[name='schoolPaymentInfo[countryCode]']").first()
				.selectOption(code);

		settle();
	}

	/**
	 * Types the school and the student in, rather than taking them from a saved template.
	 *
	 * The other half of this step: an account can pay a school it has saved, or one it is
	 * naming for the first time, and they fill the same boxes by different means. Nothing here
	 * touches the checkbox beside the form - ticking it would save this school as a template and
	 * the saved-school scenarios read that list.
	 */
	public void enterSchoolDetails(String schoolName, String schoolCode, String schoolAddress,
								   String studentName, String studentId) {

		fill("schoolPaymentInfo[schoolInfo][name]", schoolName);
		fill("schoolPaymentInfo[schoolInfo][code]", schoolCode);
		fill("schoolPaymentInfo[schoolInfo][address]", schoolAddress);
		fill("schoolPaymentInfo[studentInfo][name]", studentName);
		fill("schoolPaymentInfo[studentInfo][identityNumber]", studentId);

		settle();
	}

	/** Types into one of the school step's boxes, whether it is an input or a textarea. */
	private void fill(String name, String value) {

		if (value == null || value.isBlank()) {
			return;
		}

		page.locator("xpath=//input[@name=\"" + name + "\"]"
				+ " | //textarea[@name=\"" + name + "\"]").first().fill(value);
	}

	/** Whether the form will send what has been typed in. */
	public boolean canSend() {

		try {
			return page.locator("xpath=//button[normalize-space()='Next']").first().isEnabled();
		} catch (Exception notThere) {
			return false;
		}
	}

	/**
	 * Waits for a chosen template to finish filling the step in.
	 *
	 * The boxes do not all fill at once. Asserting the moment the step reappears caught it
	 * half-filled - the school's name and code across, the address still empty - which reads as
	 * a template that loses the address rather than one that had not finished. Answers whether
	 * everything arrived, so a template that really does leave a box empty still fails.
	 */
	public boolean waitUntilTheSchoolIsFilledIn(int timeoutSeconds) {

		return com.umpay.utility.Wait.until(() ->
				!schoolName().isEmpty()
						&& !schoolCode().isEmpty()
						&& !schoolAddress().isEmpty()
						&& !studentName().isEmpty()
						&& !studentId().isEmpty(), timeoutSeconds);
	}

	/**
	 * Whatever one of the school step's boxes is holding.
	 *
	 * Inputs and textareas both, because they are not all the same element: the address is a
	 * textarea while the rest are inputs, and looking only at inputs reported an address the
	 * form was showing perfectly well as empty - which read as a saved school that loses its
	 * address.
	 */
	private String valueOf(String name) {

		try {
			return page.locator("xpath=//input[@name=\"" + name + "\"]"
					+ " | //textarea[@name=\"" + name + "\"]").first().inputValue();

		} catch (Exception notThere) {
			return "";
		}
	}

	/**
	 * Sends the school fee, from the step that names the school.
	 *
	 * The button reads Next here as it does on the amount step, but it is not the same thing:
	 * pressing it submits the transfer and the application answers with the PIN dialog. Named
	 * for what it does rather than for what it says, so no scenario presses it by accident.
	 */
	public void sendFromTheSchoolStep() {

		page.locator("xpath=//button[normalize-space()='Next']").first().click();

		settle();
	}

	/** True while the form is still asking for the amount. */
	public boolean stillAsksForTheAmount() {

		return hasAmountField() && !asksWhoIsBeingPaid();
	}

	public void enterAmount(String amount) {

		amountField.waitFor();
		amountField.clear();
		amountField.fill(amount);

		settle();

	}

	public boolean hasNextButton() {

		return isPresent(page.locator("xpath=" + "//button[normalize-space()='Next']"));

	}

	public boolean canGoNext() {

		try {
			return nextButton.isEnabled();

		} catch (Exception notThere) {
			return false;
		}
	}

	private boolean isPresent(Locator locator) {

		try {
			locator.first().waitFor(new Locator.WaitForOptions()
					.setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
			return true;

		} catch (Exception notThere) {
			return false;
		}
	}

	private void settle() {

		try {
			Thread.sleep(2000);

		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}
}
