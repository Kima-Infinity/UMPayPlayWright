package com.umpay.utility;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;

/**
 * The platform's own answer about a transaction, read off the dialog it shows.
 *
 * The application can decline a transaction at any point and for any combination. A Malaysian
 * ringgit deposit through the Duitnow channel was answered "This service not available please
 * try again with other methods", and there is nothing about that answer particular to Malaysia,
 * to an e-wallet, or to Duitnow - nor, for that matter, to depositing. Any currency, any
 * channel, any payment method and any flow can be answered the same way, so the check lives
 * here where every flow can use it rather than in the one page that happened to need it first.
 *
 * Reading the answer is what keeps a refusal legible. Without it a run waits out its timeout
 * for a success marker that is never coming and then fails saying nothing about why - or,
 * worse, walks past the dialog and reports a transaction that never happened.
 */
public final class PlatformRefusal {

	/**
	 * The standing notice these dialogs open with, whatever they go on to say.
	 *
	 * Whatever the dialog says about the transaction comes after it, and whatever titles the
	 * dialog - "Deposit", "Withdraw" - comes before, so taking everything up to the end of the
	 * notice puts the sentence that matters at the front of the failure rather than at the end
	 * of a long line.
	 */
	private static final String STANDING_NOTICE =
			"If your operation is still in progress. Avoid refreshing or closing this page.";

	/**
	 * How the platform sounds when something has gone against the transaction.
	 *
	 * A complaint is looked for line by line because the answer does not always arrive in a
	 * dialog of its own: a refused withdraw is answered inside the PIN dialog, which goes on
	 * reading "Please enter your PIN" and adds "This service not available please try again
	 * with other methods" underneath it. Reading the dialog as a whole would either miss that
	 * or quote the prompt along with it.
	 *
	 * Deliberately about tone rather than about any one message. A wording nobody has seen yet
	 * is still likely to say one of these, and a message that says none of them is still caught
	 * when it arrives in a dialog that does nothing else.
	 */
	private static final String[] SOUNDS_LIKE_A_COMPLAINT = {
			"not available", "unavailable", "try again", "failed", "failure", "unsuccessful",
			"unable", "invalid", "incorrect", "insufficient", "not enough", "declin", "reject",
			"not allowed", "not permitted", "denied", "error", "exceed", "expired", "cannot"
	};

	/** What a dialog says when it is reporting an order rather than refusing one. */
	private static final String[] READS_AS_A_RECEIPT = {
			"merchant has accepted", "order no", "order number", "order id",
			"successful", "success", "completed", "processing", "pending"
	};

	private PlatformRefusal() {
		// Static holder; there is nothing to construct.
	}

	/**
	 * Stops the transaction if the platform has something to say about it.
	 *
	 * The message names the transaction and the step it stopped at, so the failure reads as the
	 * platform's decision about a particular transaction rather than as a broken test.
	 *
	 * @param transaction how to name what was being attempted, e.g. "the HKD withdraw to FPS"
	 * @param afterDoing  the step just taken, e.g. "confirming"
	 */
	public static void stopIfRefused(Page page, String transaction, String afterDoing) {

		String refusal = showing(page);

		if (!refusal.isEmpty()) {
			throw new IllegalStateException("The platform would not take " + transaction
					+ ", after " + afterDoing + ": " + refusal);
		}
	}

	/**
	 * Whatever the platform is saying against the transaction, or empty when it is saying
	 * nothing.
	 *
	 * Safe to poll, and meant to be: waiting for a success marker <em>or</em> this is what turns
	 * a refusal from thirty seconds of silence into an immediate failure carrying the reason.
	 * The form's own working dialogs - a picker to choose from, the PIN box, the receipt for an
	 * order that went through - read as silence unless they carry a complaint.
	 */
	public static String showing(Page page) {

		String said = mostTalkativeDialog(page);

		if (said.trim().isEmpty()) {
			return "";
		}

		String complaint = theLineThatComplains(said);

		if (!complaint.isEmpty()) {
			return complaint;
		}

		// Nothing to type into, nothing to choose from and no order to report: a dialog that is
		// only words is the platform talking rather than the form working, whatever the words
		// turn out to be. This is what catches a refusal worded in a way nobody has seen yet.
		if (holdsBoxesToTypeInto(page) || holdsRowsToChooseFrom(page) || readsAsAReceipt(said)) {
			return "";
		}

		return withoutStandingNotice(asOneLine(said));
	}

	/** The first line of the dialog that sounds like a complaint, or empty when none does. */
	private static String theLineThatComplains(String said) {

		for (String line : said.split("\\R")) {

			String plainly = line.toLowerCase();

			for (String tone : SOUNDS_LIKE_A_COMPLAINT) {
				if (plainly.contains(tone)) {
					return asOneLine(line);
				}
			}
		}

		return "";
	}

	/**
	 * The words of whichever open dialog has the most to say.
	 *
	 * Several nodes answer to the dialog's id and the outermost is not always the one holding
	 * the words - taking the first gave "Deposit", the dialog's title and nothing else - so the
	 * one with most to say is the one worth reading. Line breaks are kept, because which line
	 * the platform's answer is on is what tells it apart from the dialog's own furniture.
	 */
	private static String mostTalkativeDialog(Page page) {

		String said = "";

		for (Locator dialog : openDialogs(page)) {

			try {
				String text = dialog.innerText().trim();

				if (text.length() > said.length()) {
					said = text;
				}
			} catch (Exception reRendered) {
				// A dialog that re-rendered from under us is not one that is blocking anything.
			}
		}

		return said;
	}

	/** Every dialog on screen, by either of the two shapes this application uses. */
	private static List<Locator> openDialogs(Page page) {

		List<Locator> open = new ArrayList<>();

		for (String shape : new String[]{"xpath=//*[@id='root.dialog']", "div[role='dialog']"}) {
			try {
				for (Locator dialog : page.locator(shape).all()) {
					if (dialog.isVisible()) {
						open.add(dialog);
					}
				}
			} catch (Exception nothingShowing) {
				// Nothing open is nothing to read.
			}
		}

		return open;
	}

	/**
	 * Whether an open dialog wants typing rather than reading.
	 *
	 * The PIN dialog that authorises a withdraw is a dialog like any other, and so is a picker
	 * with a search box. Neither is a message on its own - though either can carry one, which
	 * is why the complaint is looked for first.
	 */
	private static boolean holdsBoxesToTypeInto(Page page) {

		try {
			for (Locator box : page.locator("div[role='dialog'] input, [id='root.dialog'] input").all()) {
				if (box.isVisible()) {
					return true;
				}
			}
		} catch (Exception nothingOpen) {
			return false;
		}

		return false;
	}

	/**
	 * Whether an open dialog is one of the form's own pickers.
	 *
	 * Reading a picker as a message would stop a perfectly good transaction and quote a list of
	 * wallets as the reason.
	 */
	private static boolean holdsRowsToChooseFrom(Page page) {

		try {
			return page.locator("xpath=//*[@id='root.dialog']/div/div[2]/div/div/div/div/div[2]/div").count() > 0
					|| page.locator("xpath=//*[@id='root.dialog']/div/div[2]/div/div/div/div/div/div[3]/div").count() > 0;
		} catch (Exception nothingOpen) {
			return false;
		}
	}

	/** Whether the dialog is reporting an order rather than saying anything against one. */
	private static boolean readsAsAReceipt(String said) {

		String plainly = said.toLowerCase();

		for (String marker : READS_AS_A_RECEIPT) {
			if (plainly.contains(marker)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * The dialog's own words, with the notice it always carries taken off the front.
	 *
	 * A dialog carrying nothing but its standing notice has said nothing, and reads here as
	 * silence. Anything that does not carry the notice is left exactly as it was found, so a
	 * change to the wording costs nothing worse than a longer message.
	 */
	private static String withoutStandingNotice(String said) {

		int notice = said.indexOf(STANDING_NOTICE);

		if (notice < 0) {
			return said;
		}

		return said.substring(notice + STANDING_NOTICE.length()).trim();
	}

	/** One line, so a failure message stays on one line however the dialog was laid out. */
	private static String asOneLine(String said) {

		return said.replaceAll("\\s+", " ").trim();
	}
}
