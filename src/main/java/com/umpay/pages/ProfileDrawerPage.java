package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.umpay.utility.Wait;

import java.util.ArrayList;
import java.util.List;

/**
 * The profile drawer, opened from the last control in the top bar and served as
 * {@code #profile-sidebar}.
 *
 * Everything the account holder can reach about themselves lives here: their trade record, the
 * people under them, their wallets and payment accounts, their templates and fee settings, the
 * state of their documents, security, languages, settings, and the way out.
 *
 * Items are found by what they say rather than by id. The older page object addressed them as
 * [id='trade_record'], [id='wallet'] and so on, and not one of those ids is on the page any
 * more - every item now carries no id at all, so a locator built on one silently matches
 * nothing. What the drawer says about itself is the only stable thing to hold on to.
 */
public class ProfileDrawerPage {

	/** The drawer itself. It is mounted when opened rather than hidden in the page. */
	private static final String DRAWER = "xpath=//*[@id='profile-sidebar']";

	/** The item in the drawer that carries the referral code. */
	private static final String REFERRAL_ITEM =
			DRAWER + "//button[contains(normalize-space(.),'Referral Code')]";

	/**
	 * The control beside the referral code that copies it.
	 *
	 * The item around it is not a control at all - it is drawn with cursor-auto and pressing it
	 * does nothing - so the copy has to be addressed on its own rather than through the item.
	 */
	private static final String COPY_REFERRAL =
			REFERRAL_ITEM + "//div[contains(@class,'cursor-pointer')]";

	/**
	 * The top bar's controls, one of which opens the drawer.
	 *
	 * Which one cannot be stated: they carry no id, no label and no text - the profile control
	 * is an avatar, and the bar nests buttons inside buttons, so the last of them is an inner
	 * element of the notifications control rather than the profile. So the drawer is opened by
	 * trying them from the right, which is where an avatar sits, and stopping at the one that
	 * opens it. A control that navigates instead is stepped back from.
	 */
	private final Locator topBarButtons;

	private final Page page;

	public ProfileDrawerPage(Page ldriver) {

		this.page = ldriver;
		this.topBarButtons = page.locator("nav button");
	}

	/** Opens the drawer, if it is not open already. */
	public void open() {

		if (isShowing()) {
			return;
		}

		String startedAt = page.url();

		int count = topBarButtons.count();

		for (int control = count - 1; control >= 0; control--) {

			try {
				topBarButtons.nth(control).click();
			} catch (Exception intercepted) {
				continue;
			}

			if (Wait.until(this::isShowing, 4)) {
				settle();
				return;
			}

			// It went somewhere instead. Step back and try the one before it.
			if (!page.url().equals(startedAt)) {

				page.navigate(startedAt);
				Wait.sleep(3000);

			} else {
				page.keyboard().press("Escape");
				Wait.sleep(500);
			}
		}

		throw new IllegalStateException("None of the " + count + " controls in the top bar opened"
				+ " the profile drawer. The page is at " + page.url());
	}

	/**
	 * Waits for the drawer to finish drawing itself.
	 *
	 * Its items do not all arrive together - the referral code is fetched, and the account's
	 * standing with it - so a list read the moment the panel appears is a short list. Reading it
	 * twice a second apart and stopping when the two agree is what tells a drawer that has
	 * finished from one that is still filling.
	 */
	private void settle() {

		int seen = -1;

		for (int look = 0; look < 12; look++) {

			int now = functionsOffered().size();

			if (now > 0 && now == seen) {
				return;
			}

			seen = now;

			Wait.sleep(1000);
		}
	}

	/**
	 * True once the drawer is on screen.
	 *
	 * Judged by whether its items can be seen rather than by the drawer element itself. The
	 * drawer is a sliding panel whose outer element Playwright does not call visible even when
	 * the panel is open and its items are plainly there - asking about the element alone
	 * reported a closed drawer and sent the run off clicking the rest of the top bar.
	 */
	public boolean isShowing() {

		try {
			if (page.locator(DRAWER).count() == 0) {
				return false;
			}

			for (Locator item : page.locator(DRAWER + "//button").all()) {
				if (item.isVisible()) {
					return true;
				}
			}

		} catch (Exception notThere) {
			return false;
		}

		return false;
	}

	/**
	 * Everything the drawer offers, in the order it lists them.
	 *
	 * Blank entries are left out: the drawer's own furniture - the avatar, the dividers between
	 * groups - are buttons too, and they are not functions anybody can be offered.
	 */
	public List<String> functionsOffered() {

		List<String> offered = new ArrayList<>();

		for (Locator item : Wait.all(page.locator(DRAWER + "//button"))) {

			try {
				if (!item.isVisible()) {
					continue;
				}

				String said = item.innerText().replaceAll("\\s+", " ").trim();

				if (!said.isEmpty() && !offered.contains(said)) {
					offered.add(said);
				}

			} catch (Exception reDrawn) {
				// An item that redrew from under us is not one to report.
			}
		}

		return offered;
	}

	/** Opens one of the drawer's functions by the name it goes by. */
	public void open(String function) {

		open();

		Locator item = page.locator(DRAWER + "//button[contains(normalize-space(.),\"" + function + "\")]");

		try {
			item.first().waitFor();
		} catch (Exception notOffered) {
			throw new IllegalStateException("The profile drawer offers no \"" + function
					+ "\". It offers: " + functionsOffered());
		}

		try {
			item.first().click();
		} catch (Exception intercepted) {
			item.first().dispatchEvent("click");
		}

		Wait.sleep(3000);
	}

	/**
	 * What the drawer says about the account itself, rather than what it offers to open.
	 *
	 * The status, the referral code and the state of the account's documents are shown in the
	 * drawer and nowhere else, so they are read from it.
	 */
	public String says(String startingWith) {

		for (String said : functionsOffered()) {
			if (said.startsWith(startingWith)) {
				return said;
			}
		}

		return "";
	}

	/**
	 * The referral code the drawer shows, without the words in front of it.
	 *
	 * Read from the item itself if the list of what the drawer offers comes back short. The items
	 * arrive one at a time and the referral code is fetched rather than drawn with the rest, so a
	 * list read while the drawer is still filling can be missing the very item being asked for.
	 */
	public String referralCode() {

		String said = says("Referral Code");

		if (said.isEmpty()) {

			try {
				said = page.locator(REFERRAL_ITEM).first().innerText()
						.replaceAll("\s+", " ").trim();
			} catch (Exception stillDrawing) {
				return "";
			}
		}

		return said.replace("Referral Code:", "").trim();
	}

	/**
	 * True while the drawer offers a way to copy the referral code.
	 *
	 * A code shown but not offered for copying would have to be read off the screen and typed out
	 * again by hand, and a referral code mistyped is somebody else's commission.
	 */
	public boolean canCopyTheReferralCode() {

		try {
			return page.locator(COPY_REFERRAL).count() > 0
					&& page.locator(COPY_REFERRAL).first().isVisible();
		} catch (Exception notThere) {
			return false;
		}
	}

	/**
	 * Copies the referral code, by the control beside it.
	 *
	 * The drawer closes as it is pressed, so whatever is wanted from the drawer itself has to be
	 * read before this is called rather than after.
	 */
	public void copyTheReferralCode() {

		allowTheClipboard();

		Locator control = page.locator(COPY_REFERRAL).first();

		try {
			control.click(new Locator.ClickOptions().setTimeout(8000));
		} catch (Exception wouldNotTakeIt) {

			try {
				control.dispatchEvent("click");
			} catch (Exception norThat) {
				throw new IllegalStateException("The referral code could not be copied: "
						+ norThat.getMessage());
			}
		}

		Wait.sleep(2500);
	}

	/**
	 * What is on the clipboard, or "" while nothing is.
	 *
	 * Asked for with a limit of its own. The clipboard is read through a promise that never
	 * settles at all while the window is not the one in front, so a plain read hangs the run
	 * rather than failing it - which is what it did here before this was guarded.
	 */
	public String whatWasCopied() {

		allowTheClipboard();

		try {
			Object copied = page.evaluate(
					"() => Promise.race(["
					+ " navigator.clipboard.readText().catch(() => ''),"
					+ " new Promise(answer => setTimeout(() => answer(''), 5000))])");

			return copied == null ? "" : String.valueOf(copied).trim();

		} catch (Exception unreadable) {
			return "";
		}
	}

	/** Lets the run read and write the clipboard, which a browser will not do unasked. */
	private void allowTheClipboard() {

		try {
			page.context().grantPermissions(
					java.util.Arrays.asList("clipboard-read", "clipboard-write"));
		} catch (Exception theBrowserWouldNot) {
			// A browser that will not grant it will refuse the read, which is reported there.
		}
	}

	/** What the application said back, in the notice it raises for itself. */
	public String noticeShowing() {

		try {
			Object said = page.evaluate(
					"() => Array.from(document.querySelectorAll('[class*=Toast],[class*=toast]'))"
					+ ".map(e => (e.innerText || '').replace(/\s+/g, ' ').trim())"
					+ ".filter(s => s).filter((s, i, a) => a.indexOf(s) === i).join(' | ')");

			return said == null ? "" : String.valueOf(said).trim();

		} catch (Exception unreadable) {
			return "";
		}
	}

	/** Closes the drawer, so that it can be opened again and read a second time. */
	public void close() {

		try {
			page.keyboard().press("Escape");
			Wait.sleep(1000);
		} catch (Exception itWouldNot) {
			// A drawer that will not take Escape is closed by opening something else.
		}

		if (isShowing()) {

			try {
				page.mouse().click(5, 5);
				Wait.sleep(1000);
			} catch (Exception norThat) {
				// Left open; opening it again is harmless.
			}
		}
	}

	/** Whether the drawer reports the account's documents as verified. */
	public String documentVerification() {

		return says("Document Verification").replace("Document Verification", "").trim();
	}

	/** The address the page is at, for a step that has to say where a function led. */
	public String getCurrentUrl() {

		return page.url();
	}

	/** The heading of whatever the last function opened. */
	public String headingShown() {

		try {
			return page.locator("xpath=//h1 | //h4 | //h5").first()
					.innerText().replaceAll("\\s+", " ").trim();
		} catch (Exception noHeading) {
			return "";
		}
	}

	/**
	 * Asks to log out, which the application answers with a confirmation rather than by doing
	 * it.
	 *
	 * Separated from confirming on purpose: a scenario that only wants to prove the drawer asks
	 * before signing anybody out can stop here, and one that wants to be signed out says so.
	 */
	public void askToLogOut() {

		open("Logout");
	}

	/**
	 * Whether the application is asking whether logging out is meant.
	 *
	 * Any dialog carrying a choice counts. Looking for a button labelled Confirm in particular
	 * was a guess about wording, and a wrong one reads as "the application signed the account
	 * out without asking" - which would be a serious finding to report on the strength of a
	 * guess.
	 */
	public boolean asksToConfirmLoggingOut() {

		return theQuestionsButtons().size() >= 2;
	}

	/** What the question is offering, by the words on its buttons. */
	public java.util.List<String> theQuestionsButtons() {

		java.util.List<String> choices = new ArrayList<>();

		try {
			for (Locator button : page.locator("xpath=//*[@id='root.dialog']//button").all()) {

				if (!button.isVisible()) {
					continue;
				}

				String said = button.innerText().replaceAll("\s+", " ").trim();

				if (!said.isEmpty()) {
					choices.add(said);
				}
			}
		} catch (Exception notAsking) {
			return choices;
		}

		return choices;
	}

	/** Everything the question says, for a failure that has to quote it. */
	public String whatTheQuestionSays() {

		try {
			return page.locator("xpath=//*[@id='root.dialog']").first()
					.innerText().replaceAll("\s+", " ").trim();
		} catch (Exception notAsking) {
			return "";
		}
	}

	/** Answers that confirmation, either way. */
	public void answerTheLogOutQuestion(String answer) {

		Locator button = page.locator("xpath=//*[@id='root.dialog']//button[normalize-space()=\""
				+ answer + "\"]");

		try {
			button.first().click();
		} catch (Exception intercepted) {
			button.first().dispatchEvent("click");
		}

		Wait.sleep(4000);
	}

	/** Whether the run has been signed out, which the login form is the proof of. */
	public boolean isSignedOut() {

		try {
			return page.url().contains("/login")
					|| page.locator("xpath=//button[normalize-space()='Login']").first().isVisible();
		} catch (Exception notThere) {
			return false;
		}
	}
}
