package com.umpay.pages;

import com.umpay.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;



import java.time.Duration;
import java.util.List;

/**
 * The Select Template list, reached from the Transfer hub's "UMPay to Existing template" route
 * and served at /v2/template.
 *
 * A saved destination rather than a form: each row names the template and the account it pays,
 * and choosing one opens the transfer form for that account. The rows carry no id and no text
 * that tells them apart from the rest of the page, so they are found by the class the
 * application gives every choosable row - the same one the receiver flow already uses.
 */
public class TemplatePage {

	/** Every choosable row on the page. The application marks them all the same way. */
	private static final String ROWS = ".ui-choice.default";

	/**
	 * The account a saved template pays is the last thing its row says.
	 *
	 * It is not always a card number. Two of this account's templates pay an email address, and a
	 * check that understood only digits called them templates naming no account at all. What they
	 * have in common is the masking - "**********000004", "ash*******@mailinator.com" - which is
	 * the property worth holding the list to.
	 */

	Page page;

	public TemplatePage(Page ldriver) {

		this.page = ldriver;
	}

	/** True once the saved templates are listed. */
	public boolean isShowing() {

		try {
			return page.url().contains("/v2/template")
					&& page.locator("xpath=//*[normalize-space()='Select Template']")
							.first().isVisible();
		} catch (Exception notThere) {
			return false;
		}
	}

	/**
	 * Every saved template, as the list shows it: the name it was saved under and the account
	 * it pays.
	 *
	 * Text rather than rows, so a step can read the list without being able to click it.
	 */
	public java.util.List<String> templatesOffered() {

		java.util.List<String> offered = new java.util.ArrayList<>();

		for (Locator row : Wait.all(page.locator(ROWS))) {
			try {
				offered.add(row.innerText().replaceAll("\\s+", " ").trim());
			} catch (Exception reRendered) {
				// A row that redrew from under us is not one to report.
			}
		}

		return offered;
	}

	/** The account each saved template pays, exactly as the list writes it. */
	public java.util.List<String> accountsOffered() {

		java.util.List<String> accounts = new java.util.ArrayList<>();

		for (String template : templatesOffered()) {

			String[] words = template.trim().split(" ");

			accounts.add(words.length == 0 ? "" : words[words.length - 1]);
		}

		return accounts;
	}

	/**
	 * Chooses the saved template whose row says {@code named}, whatever the row is about.
	 *
	 * The other way of choosing here matches a card number in a particular sub-element, which is
	 * right for payout templates and wrong for school ones: a school row reads "School Test 4"
	 * over "Test School 2 (Anna Smith)", and matching the card path found the school where the
	 * name was wanted and clicked nothing at all. Matching on what the row says covers both.
	 *
	 * The row is what selects, not the text inside it - the name is a heading strip that ignores
	 * clicks - so this walks outwards until the list actually closes.
	 */
	public void choose(String named) {

		for (Locator row : Wait.all(page.locator(ROWS))) {

			String said;

			try {
				said = row.innerText().replaceAll("\\s+", " ").trim();
			} catch (Exception reRendered) {
				continue;
			}

			if (!said.contains(named)) {
				continue;
			}

			System.out.println("Choosing the saved template: " + said);

			Locator clickable = row;

			for (int level = 0; level <= 3; level++) {

				try {
					clickable.click();
				} catch (Exception notClickable) {
					clickable.dispatchEvent("click");
				}

				Wait.sleep(2000);

				if (!isShowing()) {
					return;
				}

				clickable = clickable.locator("xpath=..");
			}

			throw new IllegalStateException("The template list stayed open after choosing \""
					+ named + "\", so nothing was carried into the transfer");
		}

		throw new IllegalStateException("No saved template says \"" + named
				+ "\". The list offers: " + templatesOffered());
	}

	public void selectTemplate(String targetCardNumber) {
		try {
			page.locator(".ui-choice.default").first()
					.waitFor(new Locator.WaitForOptions()
							.setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
			List<Locator> elements = Wait.all(page.locator(".ui-choice.default"));
			for (Locator element : elements) {
				try {
					// The rows load as the list is walked, so each is waited for in turn.
					element.waitFor();

					Locator cardNumber = element.locator("xpath=" + ".//div[1]/div/div/div[2]/p");

					System.out.println("Card Number Found: " + cardNumber.innerText());

					if (cardNumber.innerText().contains(targetCardNumber)) {
						element.waitFor();
						
						try {
							element.click();
						} catch (Exception clickException) {
							System.out.println("Regular click failed, trying a forced click or a raised event: " + clickException.getMessage());
							try {
								// Selenium moved the mouse to the element first; Playwright's
								// force skips the same actionability checks that a covered
								// element fails, which is what that was for.
								element.click(new Locator.ClickOptions().setForce(true));
							} catch (Exception forcedException) {
								element.dispatchEvent("click");
							}
						}
						
						System.out.println("Clicked card number: " + targetCardNumber);
						return;
					}
				// The Selenium version caught StaleElementReferenceException here and
				// started the list again. A Locator resolves afresh every time it is
				// used, so there is nothing to go stale and nothing to restart.
				} catch (Exception e) {
					System.out.println("Error locating sub-elements: " + e.getMessage());
				}
			}
		} catch (Exception e) {
			System.out.println("Error in selectTemplate: " + e.getMessage());
		}
	}

	// ------------------------------------------------------------------
	// The Templates page, as the profile drawer opens it
	// ------------------------------------------------------------------
	//
	// The same route serves two pages. Reached from the Transfer hub it is a picker headed
	// "Select Template" whose rows are .ui-choice.default and whose only purpose is to be chosen
	// from - that is what everything above this line is about. Reached from the profile drawer it
	// is the account's own list of saved templates, headed "Templates", offering to add one; its
	// cards are not choices and carry none of that class. So the two are told apart by what only
	// the second one has: an Add Template button.

	/** Where the saved templates are listed. */
	private static final String LIST = "/v2/template";

	/** Where adding one starts - the ways a template can be saved for. */
	private static final String ROUTES = "/v2/template/option";

	/** One saved template, by the card the page draws around it. */
	private static final String CARDS = "xpath=//div[contains(@class,'p-6')]"
			+ "[contains(@class,'shadow')][contains(@class,'rounded')]";

	/**
	 * The heading a way of sending money is offered under.
	 *
	 * The ways are not cards - none of them carries the card's classes, which is why they are
	 * found by their heading instead. A heading is also what a saved template's name sits in, so
	 * these are only ever read while the picker is open.
	 */
	private static final String HEADINGS = "xpath=//h5";

	/** What the page says on a way it will not open. */
	private static final String CLOSED = "Maintenance";

	/** The name a card carries, as a title of its own because the card truncates it. */
	private static final String NAME = "p[title]";

	/** What the card says under the name: the account that template pays. */
	private static final String ACCOUNT = "p.text-gray-500";

	/** What whatever opened calls the box it wants the template named in. */
	private static final String TEMPLATE_NAME =
			"xpath=//input[@name='template-name' or @name='name']";

	/** True once the account's own saved templates are listed. */
	public boolean isShowingTheList() {

		try {
			return page.url().contains(LIST)
					&& !page.url().contains(ROUTES)
					&& offersToAdd();
		} catch (Exception notThere) {
			return false;
		}
	}

	/** True while the ways a template can be saved for are the thing on offer. */
	public boolean isShowingTheRoutes() {

		try {
			return page.url().contains(ROUTES) && !routesOffered().isEmpty();
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
	 * Opens the account's saved templates.
	 *
	 * By the drawer first, because that is the way somebody would reach them, and by the route
	 * only if the drawer would not - the same order the rest of the suite opens a page in.
	 */
	public void openTheList() {

		if (isShowingTheList()) {
			return;
		}

		waitUntilSignedIn(30);

		try {
			ProfileDrawerPage drawer = new ProfileDrawerPage(page);

			drawer.open();
			drawer.open("Template");

			if (Wait.until(this::isShowingTheList, 10)) {
				return;
			}
		} catch (Exception theDrawerWouldNot) {
			System.out.println("The profile drawer did not lead to the saved templates: "
					+ theDrawerWouldNot.getMessage());
		}

		page.navigate(page.url().replaceAll("(https?://[^/]+).*", "$1") + LIST);

		Wait.sleep(3000);

		if (!Wait.until(this::isShowingTheList, 20)) {
			throw new IllegalStateException("Neither the profile drawer nor " + LIST + " opened"
					+ " the saved templates. The page is at " + page.url());
		}
	}

	/** Every saved template, by the card it is drawn in. */
	private java.util.List<Locator> cards() {

		return Wait.all(page.locator(CARDS));
	}

	/**
	 * Reads from inside a card without waiting for something that is not there.
	 *
	 * Counted before it is read: asking a locator that matches nothing for its text waits the
	 * full timeout first, and a page of ten cards would spend five minutes finding that out.
	 */
	private String insideOf(Locator card, String selector) {

		Locator inside = card.locator(selector);

		if (inside.count() == 0) {
			return "";
		}

		try {
			return inside.first().innerText().replaceAll("\\s+", " ").trim();
		} catch (Exception reRendered) {
			return "";
		}
	}

	/** Every saved template, as its card reads. */
	public java.util.List<String> savedTemplates() {

		java.util.List<String> found = new java.util.ArrayList<>();

		for (Locator card : cards()) {
			try {
				found.add(card.innerText().replaceAll("\\s+", " ").trim());
			} catch (Exception reRendered) {
				// A card that redrew from under us is not one to report.
			}
		}

		return found;
	}

	/** How many templates the account has saved. */
	public int templatesSaved() {

		return cards().size();
	}

	/** The name each saved template was given, in the order the page lists them. */
	public java.util.List<String> namesSaved() {

		java.util.List<String> names = new java.util.ArrayList<>();

		for (Locator card : cards()) {
			names.add(insideOf(card, NAME));
		}

		return names;
	}

	/** The account each saved template pays, masked as the card writes it. */
	public java.util.List<String> accountsPaid() {

		java.util.List<String> accounts = new java.util.ArrayList<>();

		for (Locator card : cards()) {
			accounts.add(insideOf(card, ACCOUNT));
		}

		return accounts;
	}

	/**
	 * The picture on each saved template, named by the file it comes from.
	 *
	 * A template is saved for one way of sending money and its card carries that way's icon -
	 * global-transfer for a UnionPay Global one, domestic-transfer for a China one, the account
	 * holder's own initial for a wallet - so the file name is how a step can tell them apart
	 * without opening any of them.
	 */
	public java.util.List<String> iconsSaved() {

		java.util.List<String> icons = new java.util.ArrayList<>();

		for (Locator card : cards()) {

			Locator picture = card.locator("img");

			if (picture.count() == 0) {
				icons.add("");
				continue;
			}

			try {
				String source = String.valueOf(picture.first().getAttribute("src"));
				icons.add(source.substring(source.lastIndexOf('/') + 1));
			} catch (Exception reRendered) {
				icons.add("");
			}
		}

		return icons;
	}

	/** How many ways each saved template offers to be acted on - the pencil and the bin. */
	public java.util.List<Integer> controlsOffered() {

		java.util.List<Integer> controls = new java.util.ArrayList<>();

		for (Locator card : cards()) {
			controls.add(card.locator("button").count());
		}

		return controls;
	}

	/** True while the page offers to add a template. */
	public boolean offersToAdd() {

		try {
			return page.getByRole(AriaRole.BUTTON,
					new Page.GetByRoleOptions().setName("Add Template")).count() > 0;
		} catch (Exception notThere) {
			return false;
		}
	}

	/** Starts adding one, which is to say opens the ways it could be saved for. */
	public void addTemplate() {

		Locator add = page.getByRole(AriaRole.BUTTON,
				new Page.GetByRoleOptions().setName("Add Template")).first();

		try {
			add.click();
		} catch (Exception intercepted) {
			add.dispatchEvent("click");
		}

		Wait.sleep(3000);
	}

	/**
	 * Opens the first saved template to be changed, by its pencil.
	 *
	 * The card itself does nothing when it is clicked - only the two controls on its right act -
	 * so the pencil is the only way in. It is the first of the two. The second is the bin, and
	 * nothing in this suite touches it: these are the templates the Transfer scenarios choose
	 * from by name.
	 */
	public void changeTheFirstTemplate() {

		java.util.List<Locator> saved = cards();

		if (saved.isEmpty()) {
			throw new IllegalStateException("There are no saved templates to change. The page is"
					+ " at " + page.url());
		}

		Locator pencil = saved.get(0).locator("button").first();

		try {
			pencil.click();
		} catch (Exception intercepted) {
			pencil.dispatchEvent("click");
		}

		Wait.sleep(4000);
	}

	/**
	 * True once a saved template has been opened to be changed.
	 *
	 * What opens is the form of whatever way that template was saved for, carrying the template
	 * on its address - the UnionPay one opens receiver-info with update-template and the
	 * template's id - so the address is what says a template is being changed rather than a
	 * transfer being made.
	 */
	public boolean isChangingATemplate() {

		String at = page.url();

		return at.contains("update-template") || at.contains("template-id");
	}

	/**
	 * What the page calls itself, taken from the tab rather than from the page.
	 *
	 * Both of these pages head themselves "Templates" in the same kind of heading the ways are
	 * offered in, so the page's own name would otherwise read as a seventh way of sending
	 * money. The tab says "UMPay | Templates", and the half after the bar is the name to leave
	 * out.
	 */
	private String pageName() {

		try {
			String tab = page.title();

			int bar = tab.lastIndexOf('|');

			return bar < 0 ? tab.trim() : tab.substring(bar + 1).trim();
		} catch (Exception unreadable) {
			return "";
		}
	}

	/** Every way of sending money a template can be saved for, as the picker names them. */
	public java.util.List<String> routesOffered() {

		java.util.List<String> routes = new java.util.ArrayList<>();

		String itself = pageName();

		for (Locator heading : Wait.all(page.locator(HEADINGS))) {

			try {
				String said = heading.innerText().replaceAll("\\s+", " ").trim();

				if (!said.isEmpty() && !said.equals(itself)) {
					routes.add(said);
				}
			} catch (Exception reRendered) {
				// A heading that redrew from under us is not one to report.
			}
		}

		return routes;
	}

	/**
	 * The ways the picker marks as closed rather than opens.
	 *
	 * The word sits beside the heading rather than in it, so this steps outwards from each
	 * heading until it finds it - stopping as soon as the surroundings have grown large enough
	 * to hold a second way, because at that point the word would belong to something else.
	 */
	public java.util.List<String> routesUnderMaintenance() {

		java.util.List<String> closed = new java.util.ArrayList<>();

		String itself = pageName();

		for (Locator heading : Wait.all(page.locator(HEADINGS))) {

			String said;

			try {
				said = heading.innerText().replaceAll("\\s+", " ").trim();
			} catch (Exception reRendered) {
				continue;
			}

			if (said.isEmpty() || said.equals(itself)) {
				continue;
			}

			Locator around = heading;

			for (int level = 0; level < 4; level++) {

				around = around.locator("xpath=..");

				String block;

				try {
					block = around.innerText().replaceAll("\\s+", " ").trim();
				} catch (Exception reRendered) {
					break;
				}

				if (block.length() > said.length() + 40) {
					break;
				}

				if (block.contains(CLOSED)) {
					closed.add(said);
					break;
				}
			}
		}

		return closed;
	}

	/**
	 * Chooses the way of sending money named {@code named}.
	 *
	 * The heading is what selects - clicking the words themselves opens the form - but a way the
	 * page has closed answers no click at all, so this walks outwards and then gives up quietly.
	 * Whether anything opened is for the step to say, because for one of these ways the right
	 * answer is that nothing did.
	 */
	public void chooseRoute(String named) {

		Locator heading = page.locator(HEADINGS + "[normalize-space()='" + named + "']");

		if (heading.count() == 0) {
			throw new IllegalStateException("No way offered says \"" + named + "\". The page"
					+ " offers: " + routesOffered());
		}

		Locator clickable = heading.first();

		for (int level = 0; level <= 3; level++) {

			try {
				clickable.click(new Locator.ClickOptions().setTimeout(5000));
			} catch (Exception notClickable) {
				try {
					clickable.dispatchEvent("click");
				} catch (Exception norThat) {
					// A way under maintenance answers nothing. That is the page's answer.
				}
			}

			Wait.sleep(2500);

			if (!isShowingTheRoutes()) {
				return;
			}

			clickable = clickable.locator("xpath=..");
		}
	}

	/** True once somewhere to save a new template has opened. */
	public boolean isSomewhereToSaveOne() {

		return !page.url().contains(ROUTES) && page.locator("input").count() > 0;
	}

	/** True while whatever opened asks what the template should be saved as. */
	public boolean asksForATemplateName() {

		try {
			return page.locator(TEMPLATE_NAME).count() > 0;
		} catch (Exception notThere) {
			return false;
		}
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

	/**
	 * What the platform said, if it answered with one of its own dialogs.
	 *
	 * Nothing on this page moves money, so there is nothing here for the platform to refuse -
	 * but it can answer any page the same way, and a step that fails saying only where it landed
	 * hides the answer that was on the screen at the time.
	 */
	public String refusalShowing() {

		return com.umpay.utility.PlatformRefusal.showing(page);
	}
}
