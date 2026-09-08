package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.umpay.utility.Wait;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The two QR pages: the account's own code at /qr-code, and the scanner at /scan-qr.
 *
 * THE ACCOUNT'S OWN CODE. A square drawn as an SVG, with the wallet it is drawn for named above
 * it, an amount and a remark that can be put on it, and a Download. Choosing the wallet opens the
 * account's wallets with their balances.
 *
 * THE SCANNER. A camera frame, and beneath it "OR Upload Photo" for a picture of a code. Where
 * there is no camera - a headless run has none - the page says "Device not supported" and the
 * photo is the only way in, which is the way this suite uses it.
 *
 * HOW THEY ARE OPENED. By their addresses, because nothing in the application leads to them any
 * more: not the top bar at any width, not the sidebar, not the profile drawer, and nothing on the
 * home, transfer, deposit, withdraw, bills, convert, wallet or trade record pages. The suite's
 * older header page object still carries locators for a QR control in the top bar, and none of
 * them matches anything now. A scenario holds the application to that separately; the rest of
 * them would have nothing to test if they could not open the pages at all.
 */
public class QrCodePage {

	/** Where the account's own code is. */
	private static final String CODE = "/qr-code";

	/** Where a code is scanned. */
	private static final String SCANNER = "/scan-qr";

	/** What the account is asked to put on its code. */
	private static final String AMOUNT = "input[name='amount']";

	/** What else it can put on it. */
	private static final String REMARK = "textarea[name='remark']";

	/** Where the application mounts a dialog - here, the wallets to draw the code for. */
	private static final String DIALOG = "#root\\.dialog";

	/** How small a drawing can be and still be the code rather than an icon. */
	private static final int SMALLEST_CODE = 80;

	private final Page page;

	public QrCodePage(Page ldriver) {

		this.page = ldriver;
	}

	/** True once the run is past the login page and inside the application. */
	public boolean waitUntilSignedIn(int timeoutSeconds) {

		return Wait.until(() -> !page.url().contains("/login")
				&& page.locator("nav button").count() > 0, timeoutSeconds);
	}

	private void openByItsAddress(String route) {

		page.navigate(page.url().replaceAll("(https?://[^/]+).*", "$1") + route);

		Wait.sleep(4000);
	}

	/** Opens the account's own code. */
	public void openTheCode() {

		if (isShowingTheCode()) {
			return;
		}

		waitUntilSignedIn(30);

		openByItsAddress(CODE);

		if (!Wait.until(this::isShowingTheCode, 20)) {
			throw new IllegalStateException(CODE + " did not open the account's QR code. The page"
					+ " is at " + page.url());
		}
	}

	/** Opens the scanner. */
	public void openTheScanner() {

		if (isShowingTheScanner()) {
			return;
		}

		waitUntilSignedIn(30);

		openByItsAddress(SCANNER);

		if (!Wait.until(this::isShowingTheScanner, 20)) {
			throw new IllegalStateException(SCANNER + " did not open the scanner. The page is at "
					+ page.url());
		}
	}

	/** True once the account's own code is on the screen. */
	public boolean isShowingTheCode() {

		try {
			return page.url().contains(CODE) && text().contains("QR Code");
		} catch (Exception notThere) {
			return false;
		}
	}

	/** True once the scanner is on the screen. */
	public boolean isShowingTheScanner() {

		try {
			return page.url().contains(SCANNER) && text().contains("Scan QR Code");
		} catch (Exception notThere) {
			return false;
		}
	}

	/**
	 * True while a square large enough to be a QR code is drawn on the page.
	 *
	 * The code is an SVG rather than a picture, so there is no address to check - what can be
	 * checked is that something square and big enough to be scanned is actually drawn, which is
	 * the difference between a page with a code on it and a page with an empty frame.
	 */
	public boolean codeIsDrawn() {

		try {
			for (Locator drawing : Wait.all(page.locator("svg"))) {

				com.microsoft.playwright.options.BoundingBox box = drawing.boundingBox();

				if (box != null && box.width >= SMALLEST_CODE && box.height >= SMALLEST_CODE) {
					return true;
				}
			}
		} catch (Exception unreadable) {
			return false;
		}

		return false;
	}

	/** How big the code is drawn, for a message that has to say what was seen. */
	public String sizeOfTheCode() {

		try {
			for (Locator drawing : Wait.all(page.locator("svg"))) {

				com.microsoft.playwright.options.BoundingBox box = drawing.boundingBox();

				if (box != null && box.width >= SMALLEST_CODE && box.height >= SMALLEST_CODE) {
					return Math.round(box.width) + "x" + Math.round(box.height);
				}
			}
		} catch (Exception unreadable) {
			return "";
		}

		return "";
	}

	/** The wallet the code is drawn for, as the page names it - "HKD Hong Kong". */
	public String walletChosen() {

		try {
			for (Locator button : Wait.all(page.locator("button"))) {

				String said = button.innerText().replaceAll("\\s+", " ").trim();

				// The wallet reads as its code and its country, which nothing else on the page does.
				if (said.matches("[A-Z]{3} .+")) {
					return said;
				}
			}
		} catch (Exception unreadable) {
			return "";
		}

		return "";
	}

	/** True while the page asks for an amount to put on the code. */
	public boolean asksForAnAmount() {

		return page.locator(AMOUNT).count() > 0;
	}

	/** True while the page asks for a remark to put on the code. */
	public boolean asksForARemark() {

		return page.locator(REMARK).count() > 0;
	}

	/** What the amount box invites somebody to do, in its own words. */
	public String amountInvites() {

		try {
			return String.valueOf(page.locator(AMOUNT).first().getAttribute("placeholder"));
		} catch (Exception notThere) {
			return "";
		}
	}

	/** Puts an amount on the code. */
	public void typeTheAmount(String amount) {

		page.locator(AMOUNT).first().fill(amount);

		Wait.sleep(2500);
	}

	/** Puts a remark on the code. */
	public void typeTheRemark(String remark) {

		page.locator(REMARK).first().fill(remark);

		Wait.sleep(2500);
	}

	/** What is in the amount box now. */
	public String amountOnTheCode() {

		try {
			return page.locator(AMOUNT).first().inputValue();
		} catch (Exception notThere) {
			return "";
		}
	}

	/** What is in the remark box now. */
	public String remarkOnTheCode() {

		try {
			return page.locator(REMARK).first().inputValue();
		} catch (Exception notThere) {
			return "";
		}
	}

	/** True while the code can be downloaded. */
	public boolean offersToDownload() {

		try {
			return page.getByRole(AriaRole.BUTTON,
					new Page.GetByRoleOptions().setName("Download")).count() > 0;
		} catch (Exception notThere) {
			return false;
		}
	}

	/** Opens the wallets the code can be drawn for. */
	public void openTheWallets() {

		String wallet = walletChosen();

		if (wallet.isEmpty()) {
			throw new IllegalStateException("The page does not say which wallet the code is drawn"
					+ " for, so there is nothing to open");
		}

		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(wallet)).first().click();

		Wait.sleep(3000);
	}

	/**
	 * The wallets offered to draw the code for, by their currency and country.
	 *
	 * Read out of the dialog's words rather than off its lines: it writes the currency, the
	 * country and the balance together - "HKD Hong Kong HK$8156.51" - and puts its line breaks
	 * wherever the layout happens to fall, so what is looked for is a three letter currency
	 * followed by the name of a place.
	 */
	public List<String> walletsOffered() {

		List<String> wallets = new ArrayList<>();

		try {
			String said = page.locator(DIALOG).first().innerText().replaceAll("\\s+", " ");

			java.util.regex.Matcher wallet = java.util.regex.Pattern
					.compile("([A-Z]{3}) ([A-Z][a-z]+(?: [A-Z][a-z]+)*)")
					.matcher(said);

			while (wallet.find()) {

				String found = wallet.group(1) + " " + wallet.group(2);

				if (!wallets.contains(found)) {
					wallets.add(found);
				}
			}
		} catch (Exception unreadable) {
			// Nothing to say about a dialog that is not there.
		}

		return wallets;
	}

	/** True while the wallets are on the screen. */
	public boolean isShowingTheWallets() {

		try {
			return page.locator(DIALOG).first().innerText().contains("Select Wallet");
		} catch (Exception notThere) {
			return false;
		}
	}

	/** Closes the wallets without choosing one. */
	public void closeTheWallets() {

		Locator close = page.locator(DIALOG + " button");

		if (close.count() > 0) {

			try {
				close.first().click();
			} catch (Exception intercepted) {
				close.first().dispatchEvent("click");
			}
		}

		Wait.sleep(2000);
	}

	// ------------------------------------------------------------------
	// The scanner
	// ------------------------------------------------------------------

	/** True while the scanner offers a picture to be uploaded instead of using a camera. */
	public boolean offersToUploadAPhoto() {

		return page.locator("input[type='file']").count() > 0 && text().contains("Upload Photo");
	}

	/** What kinds of picture the scanner will take, as it says itself. */
	public String picturesAccepted() {

		try {
			return String.valueOf(page.locator("input[type='file']").first().getAttribute("accept"));
		} catch (Exception notThere) {
			return "";
		}
	}

	/** True while the scanner is drawing a camera frame to line a code up with. */
	public boolean isAskingToAlignACode() {

		return text().contains("Align frame with QR Code");
	}

	/**
	 * What the scanner says about the camera it has been given.
	 *
	 * A run without one is told so - "Device not supported" - which is worth reading rather than
	 * assuming, because it is the reason the photo is the way in.
	 */
	public String whatItSaysAboutTheCamera() {

		return text().contains("Device not supported") ? "Device not supported" : "";
	}

	/** Hands the scanner a picture. */
	public void uploadThePicture(Path picture) {

		page.locator("input[type='file']").first().setInputFiles(picture);

		Wait.sleep(6000);
	}

	// ------------------------------------------------------------------

	/**
	 * Anything on the page as it stands that would lead somebody to either QR page.
	 *
	 * Looked for in what the page says and in every attribute of everything on it, so a link, a
	 * button carrying the address, or a menu item naming it would all be found.
	 */
	public List<String> anythingLeadingToTheQrPages() {

		List<String> leads = new ArrayList<>();

		try {
			Object found = page.evaluate(
					"() => { const found = [];"
					+ " document.querySelectorAll('*').forEach(e => {"
					+ "   const attrs = Array.from(e.attributes || []).map(a => a.value).join(' ');"
					+ "   const text = e.children.length === 0 ? (e.innerText || '') : '';"
					+ "   if (/qr-code|scan-qr|scan qr|my qr/i.test(attrs + ' ' + text))"
					+ "     found.push((e.tagName + ' ' + text).replace(/\\s+/g, ' ').trim()"
					+ "       .slice(0, 60)); });"
					+ " return found.slice(0, 6); }");

			if (found instanceof List) {

				for (Object one : (List<?>) found) {
					leads.add(String.valueOf(one));
				}
			}
		} catch (Exception unreadable) {
			// A page that would not answer leads nowhere, as far as this can tell.
		}

		return leads;
	}

	/** Opens one of the application's pages, to look for a way to the QR pages from it. */
	public void openTheApplicationPage(String route) {

		openByItsAddress(route);
	}

	/** Where the run is. */
	public String getCurrentUrl() {

		return page.url();
	}

	/** What the page says, on one line, for a message that has to name what was seen. */
	public String text() {

		try {
			return page.locator("xpath=//*[@id='root']").first()
					.innerText().replaceAll("\\s+", " ").trim();
		} catch (Exception unreadable) {
			return "";
		}
	}

	/** What the platform said, if it answered with one of its own dialogs. */
	public String refusalShowing() {

		return com.umpay.utility.PlatformRefusal.showing(page);
	}
}
