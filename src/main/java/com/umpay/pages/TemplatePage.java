package com.umpay.pages;

import com.umpay.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;



import java.time.Duration;
import java.util.List;

public class TemplatePage {

	Page page;
	
	public TemplatePage(Page ldriver) {

		this.page = ldriver;
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
