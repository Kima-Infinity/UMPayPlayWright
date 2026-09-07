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
}
