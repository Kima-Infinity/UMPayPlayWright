package com.umpay.pages;

import com.umpay.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import java.time.Duration;

/**
 * The AML/KYC policy a new account is shown before it can use the application.
 *
 * Registration leaves the browser on /v2/term-and-condition with this modal over
 * the home page. The sidebar is already there underneath, so the account exists
 * by this point - the modal gates using it, not creating it.
 *
 * Confirm carries a real disabled attribute until the checkbox is ticked,
 * confirmed against the test environment, so the tick is what ungates it rather
 * than any scrolling of the policy text.
 */
public class TermsAndConditionsPage {

	Page page;
	
	/** The page renders exactly one checkbox, with an id that changes per render. */
		private final Locator agreeCheckbox;


		private final Locator confirmButton;


	/** Dismisses the modal without accepting, which is not what this page is for. */
		private final Locator closeModalButton;


	public TermsAndConditionsPage(Page ldriver) {

		this.page = ldriver;
		this.agreeCheckbox = page.locator("input.input-checkbox");
		this.confirmButton = page.locator("xpath=//button[normalize-space()='Confirm']");
		this.closeModalButton = page.locator("xpath=//button[normalize-space()='Close modal']");
	}

	/** True while the policy modal is up. */
	public boolean isDisplayed() {

		// "Has this screen arrived" rather than "is it on screen this instant" - see
		// Wait.appears on why the difference matters after the port.
		return Wait.appears(confirmButton);
	}

	/** More policies than the application has ever shown, as a runaway guard. */
	private static final int MAX_POLICIES = 6;

	/**
	 * Accepts every policy the application puts up, in order, until none is left.
	 *
	 * A new account is shown more than one: confirming the AML policy replaces it
	 * with Terms and Conditions in the same modal frame, at the same URL, with a
	 * fresh unticked checkbox. Accepting only the first leaves the second sitting
	 * over the home page, which is what a single-shot version of this did.
	 *
	 * Looping rather than accepting a known list keeps this working if the
	 * application adds or removes a policy - the exit condition is the modal being
	 * gone, not a count being reached.
	 */
	public void acceptAllPolicies() {

		for (int policy = 1; policy <= MAX_POLICIES; policy++) {

			if (!isDisplayed()) {
				return;
			}

			// One modal is replaced by the next rather than closing, and for a moment
			// both are in the document. The locators take the first match, which during
			// that overlap is the outgoing modal - already ticked, so ticking looks done
			// while the Confirm being waited on belongs to the incoming one and never
			// becomes clickable. Waiting for an untouched checkbox means everything below
			// is working on a single settled modal.
			if (!waitForFreshPolicy()) {
				throw new IllegalStateException(
						"A policy modal never settled into a state that could be accepted");
			}

			if (!isDisplayed()) {
				return;
			}

			String fingerprint = policyFingerprint();

			acceptOne(policyLabel());

			// The next modal renders into the same frame, so waiting for the frame to
			// disappear would time out on a chain. A re-render is what says the
			// application moved on.
			if (!waitForPolicyToChange(fingerprint)) {
				throw new IllegalStateException(
						"The policy \"" + policyLabel() + "\" was still showing after confirming it");
			}
		}

		throw new IllegalStateException("Still being shown policies after accepting " + MAX_POLICIES
				+ ". The last one was \"" + policyLabel() + "\"");
	}

	/**
	 * Ticks one policy's agreement box and confirms it.
	 *
	 * The click goes through JavascriptExecutor when the normal one does not take:
	 * the input is styled as a peer of its label, so the visible control is not the
	 * element that receives the event.
	 */
	private void acceptOne(String label) {

		agreeCheckbox.waitFor();

		if (!tick()) {
			throw new IllegalStateException("The agreement checkbox would not tick for \"" + label + "\"");
		}

		System.out.println("Agreed to: " + label);

		// Confirm only loses its disabled attribute once the box is ticked, so this
		// waits for the application to catch up rather than clicking into a dead button.
		confirmButton.waitFor();

		confirmButton.click();

		System.out.println("Confirm clicked for: " + label);
	}

	/**
	 * Ticks the agreement box, escalating until it actually takes.
	 *
	 * The input is a Tailwind {@code peer}: visually replaced by its styled label,
	 * so the thing a user clicks is not the element Selenium is holding. A click on
	 * the input can therefore land somewhere harmless and report success, which is
	 * why each strategy is judged by whether the box ended up ticked rather than by
	 * whether it threw. An earlier version only fell back on an exception and gave
	 * up on the silent case.
	 *
	 * The JavaScript click is last because it is the least like a user, but it is
	 * also the one that reliably reaches a React onChange handler.
	 */
	private boolean tick() {

		if (isTicked()) {
			return true;
		}

		if (clickAndCheck(() -> agreeCheckbox.click(), "the checkbox")) {
			return true;
		}

		if (clickAndCheck(() -> agreeCheckbox.locator("xpath=" + "./following-sibling::*[1]").click(),
				"its label")) {
			return true;
		}

		return clickAndCheck(
				() -> agreeCheckbox.dispatchEvent("click"),
				"JavaScript");
	}

	/** Runs one ticking strategy and gives the application a moment to register it. */
	private boolean clickAndCheck(Runnable strategy, String how) {

		try {
			strategy.run();
		} catch (Exception didNotLand) {
			return false;
		}

		// React re-renders on its own schedule, so the box is not ticked the instant
		// the click returns.
		if (!Wait.until(this::isTicked, 3)) {
			return false;
		}

		System.out.println("Ticked the agreement box by clicking " + how);
		return true;
	}

	/**
	 * Waits until the modal on screen is one that has not been acted on yet - a
	 * visible, unticked checkbox - or until the modals have finished entirely.
	 */
	private boolean waitForFreshPolicy() {

		return Wait.until(() -> !isDisplayed() || (isCheckboxShowing() && !isTicked()), 20);
	}

	private boolean isCheckboxShowing() {

		try {
			return agreeCheckbox.isVisible();
		} catch (Exception notThere) {
			return false;
		}
	}

	private boolean isTicked() {

		try {
			return agreeCheckbox.isChecked();
		} catch (Exception notThere) {
			return false;
		}
	}

	/**
	 * Identifies which policy is on screen.
	 *
	 * The application gives the checkbox a randomly generated id on every render,
	 * so a new id means a new modal. That is the signal the loop turns on, rather
	 * than the wording: the visible label sits outside the input's own subtree and
	 * is not reliably reachable from it, and an empty string compared against an
	 * empty string once made this loop believe nothing had changed.
	 */
	private String policyFingerprint() {

		try {
			String id = agreeCheckbox.getAttribute("id");
			return id == null ? "" : id;
		} catch (Exception notThere) {
			return "";
		}
	}

	/**
	 * The wording beside the checkbox, for the run log only - "I agreed to the aml
	 * policy!" against "I have read and agree to the terms and conditions". The
	 * markup puts it in a sibling of the input rather than around it, so a couple
	 * of shapes are tried before giving up and naming the policy by its id.
	 */
	private String policyLabel() {

		for (String relative : new String[] { "./following-sibling::*[1]", "./..", "./../.." }) {

			try {
				String text = agreeCheckbox.locator("xpath=" + relative).innerText().trim();

				if (!text.isEmpty()) {
					return text.replace("\n", " ");
				}
			} catch (Exception keepLooking) {
				// Shape does not exist on this modal; try the next one.
			}
		}

		return "the policy with checkbox " + policyFingerprint();
	}

	/** Waits for the modal to move on to a different policy, or to close entirely. */
	private boolean waitForPolicyToChange(String fingerprint) {

		return Wait.until(() -> !isDisplayed() || !policyFingerprint().equals(fingerprint), 20);
	}

	/**
	 * Waits for the modal to give way to the application.
	 *
	 * The route stays on /v2/term-and-condition for a moment after Confirm before
	 * the application moves on, so the modal going away is the signal to wait for,
	 * not the URL changing.
	 */
	public boolean waitUntilAccepted(int timeoutSeconds) {

		return Wait.until(() -> !isDisplayed(), timeoutSeconds);
	}
}
