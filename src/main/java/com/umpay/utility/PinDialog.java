package com.umpay.utility;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;

/**
 * The PIN the application asks for before money leaves the account.
 *
 * Every screen that sends money ends the same way: Confirm does not submit anything on its own,
 * the application answers with a PIN dialog, and the transaction only goes through once that is
 * filled in. The withdraw screen taught this the hard way - no run had ever actually raised a
 * withdraw, because every one of them stopped at this dialog, and because the dialog is
 * aria-modal it then covered the sidebar and blocked whatever step came next.
 *
 * The Global Transfer payouts do the same thing, which is why the handling lives here rather
 * than in one page: the next screen that sends money should not have to learn it again.
 */
public final class PinDialog {

	private PinDialog() {
		// Static holder; there is nothing to construct.
	}

	/**
	 * Fills in the PIN, if the application asks for one.
	 *
	 * Answers whether it did. A screen that never asks is not a failure - not every route is
	 * gated the same way - but a caller that expected one is entitled to know.
	 *
	 * @param what how to name the transaction in a failure, e.g. "the USD payout"
	 */
	public static boolean authoriseIfAsked(Page page, String what) {

		Locator dialog = waitForThePinDialog(page);

		if (dialog == null) {
			System.out.println("No PIN dialog appeared after confirming " + what + ".");
			return false;
		}

		// Only the boxes a person could type into. The dialog carries several inputs, most of
		// them hidden plumbing for the PIN component, and reaching for one of those fails.
		List<Locator> boxes = new ArrayList<>();

		for (Locator input : Wait.all(dialog.locator("input"))) {
			try {
				if (input.isVisible() && input.isEnabled()) {
					boxes.add(input);
				}
			} catch (Exception skip) {
				// A box that cannot even be asked is not one to type into.
			}
		}

		if (boxes.isEmpty()) {
			throw new IllegalStateException(what + " asked for a PIN but none of the dialog's"
					+ " inputs can be typed into. Dialog reads: " + dialog.innerText());
		}

		System.out.println("Authorising " + what + " with the PIN ("
				+ boxes.size() + " box(es) available to type into).");

		String pin = SecurityPin.value();

		// Sent to the first box as one string: a split PIN component advances by itself, and a
		// single box wants the whole code anyway. Only if that leaves the boxes empty is it
		// worth pushing a digit into each one.
		boxes.get(0).click();
		boxes.get(0).fill(pin);

		if (boxes.size() > 1 && String.valueOf(boxes.get(0).inputValue()).isEmpty()) {

			System.out.println("The PIN did not advance across the boxes; filling them one at a time.");

			for (int box = 0; box < boxes.size() && box < pin.length(); box++) {
				boxes.get(box).click();
				boxes.get(box).fill(String.valueOf(pin.charAt(box)));
			}
		}

		return true;
	}

	/** Whether the PIN dialog is still up, which means the transaction has not been authorised. */
	public static boolean isStillShowing(Page page) {

		return theOpenPinDialog(page) != null;
	}

	/** What the PIN dialog is saying, for a failure that has to quote it. */
	public static String says(Page page) {

		Locator dialog = theOpenPinDialog(page);

		try {
			return dialog == null ? "" : dialog.innerText().replaceAll("\\s+", " ").trim();
		} catch (Exception gone) {
			return "";
		}
	}

	/** Waits up to fifteen seconds for the application to ask. */
	private static Locator waitForThePinDialog(Page page) {

		long deadline = System.currentTimeMillis() + 15_000L;

		while (System.currentTimeMillis() < deadline) {

			Locator asking = theOpenPinDialog(page);

			if (asking != null) {
				return asking;
			}

			Wait.sleep(500);
		}

		return null;
	}

	/** The PIN dialog, if one is on screen. */
	private static Locator theOpenPinDialog(Page page) {

		try {
			for (Locator candidate : Wait.all(page.locator("div[role='dialog']"))) {
				if (candidate.isVisible() && candidate.innerText().toUpperCase().contains("PIN")) {
					return candidate;
				}
			}
		} catch (Exception nothingOpen) {
			// Nothing on screen is nothing to fill in.
		}

		return null;
	}
}
