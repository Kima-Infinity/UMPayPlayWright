package com.umpay.pages;

import com.umpay.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import java.time.Duration;

/**
 * The PIN a new account must set before it can reach the home page.
 *
 * This is the last gate of registration and the easiest one to miss: accepting
 * the policies does not land on home, it lands here, at /setup-pin. The field
 * takes exactly four digits - maxlength="4" with a pattern of [0-9]{4}.
 */
public class SetupPinPage {

	/** More screens than the flow has ever shown, as a runaway guard. */
	private static final int MAX_SCREENS = 4;

	Page page;
	
		private final Locator pinField;


		private final Locator nextButton;


	public SetupPinPage(Page ldriver) {

		this.page = ldriver;
		this.pinField = page.locator("[name=\'pin\']");
		this.nextButton = page.locator("xpath=//button[normalize-space()='Next']");
	}

	/** True while a PIN is being asked for. */
	public boolean isDisplayed() {

		// "Has this screen arrived" rather than "is it on screen this instant" - see
		// Wait.appears on why the difference matters after the port.
		return Wait.appears(pinField);
	}

	/**
	 * Sets the PIN, answering a confirmation screen with the same digits if one
	 * follows.
	 *
	 * Whether the application asks twice is not assumed either way: each pass fills
	 * whatever PIN field is on screen and submits it, and the loop ends when the
	 * application stops asking. That works for one screen or two without the test
	 * having to know which.
	 */
	public void setPin(String pin) {

		for (int screen = 1; screen <= MAX_SCREENS; screen++) {

			if (!isDisplayed()) {
				return;
			}

			pinField.waitFor();

			String before = fingerprint();

			pinField.clear();
			pinField.fill(pin);

			System.out.println("PIN entered on screen " + screen);

			nextButton.waitFor();
			nextButton.click();

			if (!waitForScreenToChange(before)) {
				throw new IllegalStateException("The PIN screen did not move on after submitting the PIN. "
						+ "It may have been rejected - check the Screenshots folder.");
			}
		}

		throw new IllegalStateException("Still being asked for a PIN after " + MAX_SCREENS + " screens");
	}

	/**
	 * What screen is on show. The placeholder is what distinguishes setting the PIN
	 * from confirming it, and the URL covers the field going away entirely.
	 */
	private String fingerprint() {

		try {
			return pinField.getAttribute("placeholder") + "|" + page.url();
		} catch (Exception notThere) {
			return "|" + page.url();
		}
	}

	private boolean waitForScreenToChange(String from) {

		return Wait.until(() -> !isDisplayed() || !fingerprint().equals(from) || isFieldEmpty(), 20);
	}

	/** A cleared field is the other way the application signals a second screen. */
	private boolean isFieldEmpty() {

		try {
			String value = pinField.inputValue();
			return value == null || value.isEmpty();
		} catch (Exception notThere) {
			return false;
		}
	}

	/** Waits for the PIN step to be finished with. */
	public boolean waitUntilDone(int timeoutSeconds) {

		return Wait.until(() -> !isDisplayed(), timeoutSeconds);
	}
}
