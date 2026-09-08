package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.umpay.utility.Wait;

import java.util.ArrayList;
import java.util.List;

/**
 * The language the account reads the application in, at /v2/languages.
 *
 * Reached from the profile drawer. Three of them, each a label the page draws around a radio:
 *
 *   English     en
 *   繁體中文     zh-HK
 *   ไทย          th
 *
 * Each is written in its own script, which is the only way somebody who cannot read the current
 * one can find their own. The radio carries the code the application knows it by, so both are
 * available: the name for a message a person reads, the code for saying plainly which of the
 * three the account is on.
 *
 * There is nothing to save. Choosing one takes effect at once and stays chosen afterwards, which
 * is why every scenario that chooses one puts the account back on the language it found it on -
 * every other file in the suite reads the screen in English.
 *
 * WHAT SAYS THE LANGUAGE CHANGED. The tab, rather than the page. The words on the screen are
 * redrawn in places and left alone in others, but the tab is written from the same translations
 * and changes on every page, so it is the honest thing to hold the application to.
 */
public class LanguagesPage {

	/** Where the languages are. */
	private static final String ROUTE = "/v2/languages";

	/** One language: the label the page draws around its radio. */
	private static final String LANGUAGES = "label.ui-choice.default";

	/** The radio inside one of those labels. */
	private static final String RADIO = "input[type='radio']";

	private final Page page;

	public LanguagesPage(Page ldriver) {

		this.page = ldriver;
	}

	/** True once the languages are on the screen. */
	public boolean isShowing() {

		try {
			return page.url().contains(ROUTE) && page.locator(LANGUAGES).count() > 0;
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
	 * Opens the languages.
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
			drawer.open("Languages");

			if (Wait.until(this::isShowing, 10)) {
				return;
			}
		} catch (Exception theDrawerWouldNot) {
			System.out.println("The profile drawer did not lead to the languages: "
					+ theDrawerWouldNot.getMessage());
		}

		openByItsAddress();

		if (!Wait.until(this::isShowing, 20)) {
			throw new IllegalStateException("Neither the profile drawer nor " + ROUTE + " opened"
					+ " the languages. The page is at " + page.url());
		}
	}

	/** Opens the languages again from nothing, which is how a chosen one is proved to have kept. */
	public void openByItsAddress() {

		page.navigate(page.url().replaceAll("(https?://[^/]+).*", "$1") + ROUTE);

		Wait.sleep(3000);

		Wait.until(this::isShowing, 20);
	}

	/** Goes to the home page, to see whether the language followed. */
	public void openTheHomePage() {

		page.navigate(page.url().replaceAll("(https?://[^/]+).*", "$1") + "/");

		Wait.sleep(4000);
	}

	private List<Locator> languages() {

		return Wait.all(page.locator(LANGUAGES));
	}

	/** Every language on offer, each written in its own script. */
	public List<String> languagesOffered() {

		List<String> offered = new ArrayList<>();

		for (Locator language : languages()) {

			try {
				String said = language.innerText().replaceAll("\\s+", " ").trim();

				if (!said.isEmpty()) {
					offered.add(said);
				}
			} catch (Exception reRendered) {
				// A label that redrew from under us is not one to report.
			}
		}

		return offered;
	}

	/** The code the application knows each of them by - en, zh-HK, th. */
	public List<String> codesOffered() {

		List<String> codes = new ArrayList<>();

		for (Locator language : languages()) {

			try {
				codes.add(String.valueOf(language.locator(RADIO).first().getAttribute("value")));
			} catch (Exception reRendered) {
				// As above.
			}
		}

		return codes;
	}

	/** The language the account is on, as the page writes it, or "" if none is chosen. */
	public String languageChosen() {

		for (Locator language : languages()) {

			try {
				if (language.locator(RADIO).first().isChecked()) {
					return language.innerText().replaceAll("\\s+", " ").trim();
				}
			} catch (Exception reRendered) {
				// As above.
			}
		}

		return "";
	}

	/** The code of the language the account is on - en, zh-HK or th. */
	public String codeChosen() {

		for (Locator language : languages()) {

			try {
				Locator radio = language.locator(RADIO).first();

				if (radio.isChecked()) {
					return String.valueOf(radio.getAttribute("value"));
				}
			} catch (Exception reRendered) {
				// As above.
			}
		}

		return "";
	}

	/** How many languages are chosen. One, on a page that is behaving. */
	public int howManyChosen() {

		int chosen = 0;

		for (Locator language : languages()) {

			try {
				if (language.locator(RADIO).first().isChecked()) {
					chosen++;
				}
			} catch (Exception reRendered) {
				// As above.
			}
		}

		return chosen;
	}

	/**
	 * Chooses the language named or coded {@code wanted}.
	 *
	 * Either will do: a scenario about the account being read in Chinese is clearer written as
	 * zh-HK than as characters the person reading the feature file may not have a font for.
	 */
	public void choose(String wanted) {

		for (Locator language : languages()) {

			String name;
			String code;

			try {
				name = language.innerText().replaceAll("\\s+", " ").trim();
				code = String.valueOf(language.locator(RADIO).first().getAttribute("value"));
			} catch (Exception reRendered) {
				continue;
			}

			if (!name.equalsIgnoreCase(wanted) && !code.equalsIgnoreCase(wanted)) {
				continue;
			}

			try {
				language.locator(RADIO).first().check();
			} catch (Exception styledAway) {

				try {
					language.click();
				} catch (Exception intercepted) {
					language.dispatchEvent("click");
				}
			}

			Wait.sleep(4000);
			return;
		}

		throw new IllegalStateException("No language here is called \"" + wanted + "\". The page"
				+ " offers " + languagesOffered() + " (" + codesOffered() + ")");
	}

	/** True while the page offers something to press to keep the choice. */
	public boolean offersToSave() {

		try {
			return page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
					new Page.GetByRoleOptions().setName("Save")).count() > 0;
		} catch (Exception notThere) {
			return false;
		}
	}

	/**
	 * What the interface reads, in whatever language it is in.
	 *
	 * The tab, because it is written from the same translations as the rest and changes on every
	 * page - "UMPay | Languages" in English, "UMPay | 語言" in Chinese.
	 */
	public String interfaceReads() {

		try {
			return page.title();
		} catch (Exception unreadable) {
			return "";
		}
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
