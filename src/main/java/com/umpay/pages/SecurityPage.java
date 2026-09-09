package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.umpay.utility.Wait;

import java.util.ArrayList;
import java.util.List;

/**
 * What guards this account, at /securities.
 *
 * Reached from the profile drawer. Three things, each drawn as a row that carries where it goes
 * in a {@code path} attribute of its own:
 *
 *   Login Password      /securities/password    current, new and confirm, then Save
 *   PIN Code            /securities/pin         the same three, four digits each, then Save
 *   2FA Authenticator   /securities/two-factor  says whether it is connected, and offers to
 *                                               remove or reset it
 *
 * WHAT THIS PAGE COSTS TO TEST. Every credential the rest of the suite runs on is set here. A
 * password changed would lock the whole suite out of the account, a PIN changed would fail every
 * transfer that enters one, and an authenticator removed would take the account with it. So this
 * page object can fill the forms and press Save - a form that is never submitted has not been
 * tested - but the values it is given are only ever ones the application cannot accept, and
 * nothing here touches the authenticator's own controls at all.
 */
public class SecurityPage {

	/** Where the three of them are listed. */
	private static final String ROUTE = "/securities";

	/** One of the three: the page hands each row the address it opens. */
	private static final String ROWS = "a[path^='/securities']";

	/** Where changing the password is done. */
	private static final String PASSWORD_FORM = "/securities/password";

	/** Where changing the PIN is done. */
	private static final String PIN_FORM = "/securities/pin";

	/** Where the authenticator is. */
	private static final String AUTHENTICATOR = "/securities/two-factor";

	private final Page page;

	public SecurityPage(Page ldriver) {

		this.page = ldriver;
	}

	/** True once the three things this account is guarded by are listed. */
	public boolean isShowing() {

		try {
			return page.url().endsWith(ROUTE) && page.locator(ROWS).count() > 0;
		} catch (Exception notThere) {
			return false;
		}
	}

	/** True once the run is past the login page and inside the application. */
	public boolean waitUntilSignedIn(int timeoutSeconds) {

		return Wait.until(() -> !page.url().contains("/login")
				&& page.locator("nav button").count() > 0, timeoutSeconds);
	}

	/**
	 * Opens the security page.
	 *
	 * By the drawer first, because that is the way somebody would reach it, and by the route only
	 * if the drawer would not - the same order the rest of the suite opens a page in.
	 */
	public void open() {

		if (isShowing()) {
			return;
		}

		waitUntilSignedIn(30);

		try {
			ProfileDrawerPage drawer = new ProfileDrawerPage(page);

			drawer.open();
			drawer.open("Security");

			if (Wait.until(this::isShowing, 10)) {
				return;
			}
		} catch (Exception theDrawerWouldNot) {
			System.out.println("The profile drawer did not lead to the security page: "
					+ theDrawerWouldNot.getMessage());
		}

		openByItsAddress(ROUTE);

		if (!Wait.until(this::isShowing, 20)) {
			throw new IllegalStateException("Neither the profile drawer nor " + ROUTE + " opened"
					+ " the security page. The page is at " + page.url());
		}
	}

	/** Opens one of this account's security pages by its address. */
	public void openByItsAddress(String route) {

		page.navigate(page.url().replaceAll("(https?://[^/]+).*", "$1") + route);

		Wait.sleep(3000);
	}

	/** Goes back to the three of them. */
	public void openTheList() {

		openByItsAddress(ROUTE);

		Wait.until(this::isShowing, 20);
	}

	private List<Locator> rows() {

		return Wait.all(page.locator(ROWS));
	}

	/** Everything this page guards, in the words it names them in. */
	public List<String> thingsProtected() {

		List<String> guarded = new ArrayList<>();

		for (Locator row : rows()) {

			try {
				String heading = row.locator("h5").count() > 0
						? row.locator("h5").first().innerText()
						: row.innerText();

				guarded.add(heading.replaceAll("\\s+", " ").trim());
			} catch (Exception reRendered) {
				// A row that redrew from under us is not one to report.
			}
		}

		return guarded;
	}

	/**
	 * What the row for {@code named} says about itself besides its name - "Connected" on the
	 * authenticator, and nothing at all on the two that are always there.
	 */
	public String statusOf(String named) {

		for (Locator row : rows()) {

			String said;

			try {
				said = row.innerText().replaceAll("\\s+", " ").trim();
			} catch (Exception reRendered) {
				continue;
			}

			if (said.startsWith(named)) {
				return said.substring(named.length()).trim();
			}
		}

		return "";
	}

	/** Opens the row named {@code named}. */
	public void openThe(String named) {

		for (Locator row : rows()) {

			String said;

			try {
				said = row.innerText().replaceAll("\\s+", " ").trim();
			} catch (Exception reRendered) {
				continue;
			}

			if (!said.startsWith(named)) {
				continue;
			}

			try {
				row.click();
			} catch (Exception intercepted) {
				row.dispatchEvent("click");
			}

			Wait.sleep(3500);
			return;
		}

		throw new IllegalStateException("The security page offers nothing called \"" + named
				+ "\". It offers: " + thingsProtected());
	}

	/** True once the form that changes the login password is open. */
	public boolean isShowingThePasswordForm() {

		return page.url().contains(PASSWORD_FORM) && fieldsAsked().size() == 3;
	}

	/** True once the form that changes the PIN is open. */
	public boolean isShowingThePinForm() {

		return page.url().contains(PIN_FORM) && fieldsAsked().size() == 3;
	}

	/** True once the authenticator page is open. */
	public boolean isShowingTheAuthenticator() {

		return page.url().contains(AUTHENTICATOR);
	}

	/** What the open form asks for, by the names it gives its boxes. */
	public List<String> fieldsAsked() {

		List<String> asked = new ArrayList<>();

		for (Locator field : Wait.all(page.locator("input[name]"))) {

			try {
				asked.add(String.valueOf(field.getAttribute("name")));
			} catch (Exception reRendered) {
				// As above.
			}
		}

		return asked;
	}

	/**
	 * True while every box on the open form hides what is typed into it.
	 *
	 * A password or a PIN typed in the clear is readable by whoever is behind the person typing
	 * it, which is the one thing a page like this exists to prevent.
	 */
	public boolean everythingAskedIsHidden() {

		List<Locator> fields = Wait.all(page.locator("input[name]"));

		if (fields.isEmpty()) {
			return false;
		}

		for (Locator field : fields) {

			try {
				if (!"password".equals(field.getAttribute("type"))) {
					return false;
				}
			} catch (Exception reRendered) {
				return false;
			}
		}

		return true;
	}

	/** How many characters the box called {@code named} will take, or "" if it does not say. */
	public String lengthAllowedIn(String named) {

		try {
			Locator field = page.locator("input[name='" + named + "']");

			if (field.count() == 0) {
				return "";
			}

			return String.valueOf(field.first().getAttribute("maxlength"));
		} catch (Exception notThere) {
			return "";
		}
	}

	/** Types {@code value} into the box called {@code named}, or empties it. */
	public void typeInto(String named, String value) {

		Locator field = page.locator("input[name='" + named + "']");

		if (field.count() == 0) {
			throw new IllegalStateException("This form has no box called \"" + named
					+ "\". It asks for: " + fieldsAsked());
		}

		field.first().fill(value);

		Wait.sleep(500);
	}

	/** Presses Save. */
	public void save() {

		Locator save = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save"))
				.first();

		try {
			save.click();
		} catch (Exception intercepted) {
			save.dispatchEvent("click");
		}

		Wait.sleep(4000);
	}

	/**
	 * What the page says about a PIN it will accept.
	 *
	 * Read rather than assumed, because it is the promise the page makes and the thing a
	 * scenario about a refused PIN is holding the page to.
	 */
	public String ruleStated() {

		try {
			Locator rule = page.locator("xpath=//*[contains(text(),'PIN code can not be')]");

			if (rule.count() == 0) {
				return "";
			}

			return rule.first().innerText().replaceAll("\\s+", " ").trim();
		} catch (Exception notThere) {
			return "";
		}
	}

	/**
	 * True once the page has said the change was made.
	 *
	 * The application answers a change it accepted with "Updated successfully" and then leaves
	 * the form, so either is enough to know that something was saved.
	 */
	public boolean saidItSaved() {

		try {
			return text().contains("Updated successfully");
		} catch (Exception unreadable) {
			return false;
		}
	}

	/** What the authenticator page offers to do, in the words on its buttons. */
	public List<String> authenticatorOffers() {

		List<String> offered = new ArrayList<>();

		for (Locator button : Wait.all(page.locator("button"))) {

			String said;

			try {
				said = button.innerText().replaceAll("\\s+", " ").trim();
			} catch (Exception reRendered) {
				continue;
			}

			if (said.contains("Google Authenticator") || said.contains("Google 2FA")) {
				offered.add(said);
			}
		}

		return offered;
	}

	/** Where the run is. */
	public String getCurrentUrl() {

		return page.url();
	}

	/** Steps back the way the browser would. */
	public void goBack() {

		page.goBack();

		Wait.sleep(3000);
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
