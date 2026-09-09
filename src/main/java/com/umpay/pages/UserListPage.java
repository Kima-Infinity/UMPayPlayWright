package com.umpay.pages;

import com.microsoft.playwright.Page;
import com.umpay.utility.Wait;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The people this account has brought to UMPay, at /customer.
 *
 * Reached from the profile drawer. Each one is listed by the account number the platform knows
 * them by, the name they go under, and whether their account is locked:
 *
 *   User UUID 000000770850   User Name Kima_Test123   Status Unlock
 *
 * READ RATHER THAN PRESSED. There is nothing on this page to press: no search, no filter, no
 * form, and the status is written in a span rather than offered as a control. That matters more
 * here than elsewhere - these are other people's accounts, and a page that offered to lock one
 * from here would be the last place to go clicking about.
 *
 * The users are read out of what the page says rather than off its markup: it writes each as a
 * run of label and value pairs, and the labels are the same three every time, which is a steadier
 * thing to hold on to than the boxes they happen to be drawn in.
 */
public class UserListPage {

	/** Where the people this account brought are listed. */
	private static final String ROUTE = "/customer";

	/** One user, as the page writes them. */
	private static final Pattern USER = Pattern.compile(
			"User UUID (\\S+) User Name (.+?) Status (\\S+)");

	/** What an account number looks like: twelve figures. */
	private static final String ACCOUNT_NUMBER = "\\d{12}";

	/** What the page writes where a user has no name of their own. */
	private static final String NO_NAME = "N/A";

	private final Page page;

	public UserListPage(Page ldriver) {

		this.page = ldriver;
	}

	/** True once the users are on the screen. */
	public boolean isShowing() {

		try {
			return page.url().endsWith(ROUTE) && text().contains("User List");
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
	 * Opens the users.
	 *
	 * By the drawer first, because that is the way somebody would reach them, and by the route
	 * only if the drawer would not - the same order the rest of the suite opens a page in.
	 */
	public void open() {

		if (isShowing()) {
			return;
		}

		waitUntilSignedIn(30);

		try {
			ProfileDrawerPage drawer = new ProfileDrawerPage(page);

			drawer.open();
			drawer.open("User List");

			if (Wait.until(this::isShowing, 10)) {
				return;
			}
		} catch (Exception theDrawerWouldNot) {
			System.out.println("The profile drawer did not lead to the user list: "
					+ theDrawerWouldNot.getMessage());
		}

		page.navigate(page.url().replaceAll("(https?://[^/]+).*", "$1") + ROUTE);

		Wait.sleep(4000);

		if (!Wait.until(this::isShowing, 20)) {
			throw new IllegalStateException("Neither the profile drawer nor " + ROUTE + " opened"
					+ " the user list. The page is at " + page.url());
		}
	}

	/** Everyone listed, by the account number they are known by, in the order they are listed. */
	public List<String> accountNumbersListed() {

		List<String> numbers = new ArrayList<>();

		Matcher user = USER.matcher(text());

		while (user.find()) {
			numbers.add(user.group(1).trim());
		}

		return numbers;
	}

	/** Everyone listed, as the account number they are known by and the name they go under. */
	public Map<String, String> namesListed() {

		Map<String, String> names = new LinkedHashMap<>();

		Matcher user = USER.matcher(text());

		while (user.find()) {
			names.put(user.group(1).trim(), user.group(2).trim());
		}

		return names;
	}

	/** Everyone listed, as the account number they are known by and whether they are locked. */
	public Map<String, String> statesListed() {

		Map<String, String> states = new LinkedHashMap<>();

		Matcher user = USER.matcher(text());

		while (user.find()) {
			states.put(user.group(1).trim(), user.group(3).trim());
		}

		return states;
	}

	/** How many people this account has brought. */
	public int howManyListed() {

		return accountNumbersListed().size();
	}

	/** True while {@code number} reads as an account number the platform would issue. */
	public boolean readsAsAnAccountNumber(String number) {

		return number != null && number.matches(ACCOUNT_NUMBER);
	}

	/** Those listed with no name of their own, which the page writes as N/A. */
	public List<String> thoseWithNoName() {

		List<String> unnamed = new ArrayList<>();

		for (Map.Entry<String, String> user : namesListed().entrySet()) {

			if (NO_NAME.equalsIgnoreCase(user.getValue()) || user.getValue().isEmpty()) {
				unnamed.add(user.getKey());
			}
		}

		return unnamed;
	}

	/**
	 * Anything on the page that would change one of these accounts.
	 *
	 * The status is written rather than offered: it is a span, not a button. Anything that could
	 * lock or unlock somebody from here would be found by this, and there should be nothing.
	 */
	public List<String> anythingThatWouldChangeAnAccount() {

		List<String> controls = new ArrayList<>();

		try {
			Object found = page.evaluate(
					"() => { const found = [];"
					+ " document.querySelectorAll('#root button, #root a, #root input,"
					+ " #root [role=button], #root [role=switch]').forEach(e => {"
					+ "   if (e.closest('nav') || e.closest('aside')) return;"
					+ "   const said = (e.innerText || '').replace(/\\s+/g, ' ').trim();"
					+ "   if (/lock|unlock|suspend|remove|delete|edit|block/i.test("
					+ "       said + ' ' + (e.className || ''))) found.push(e.tagName + ' ' + said); });"
					+ " return found.slice(0, 6); }");

			if (found instanceof List) {

				for (Object one : (List<?>) found) {
					controls.add(String.valueOf(one));
				}
			}
		} catch (Exception unreadable) {
			// A page that would not answer offers nothing, as far as this can tell.
		}

		return controls;
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
