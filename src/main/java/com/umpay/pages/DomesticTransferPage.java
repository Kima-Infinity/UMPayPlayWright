package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;



import java.time.Duration;

public class DomesticTransferPage {

	Page page;
	
		private final Locator unionPayChinaButton;


		private final Locator aliPayButton;


		private final Locator weChatButton;


	public DomesticTransferPage(Page ldriver) {

		this.page = ldriver;
		this.unionPayChinaButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div/a[1]/div/div/div/div/h5/div/span");
		this.aliPayButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div/a[2]/div/div/div/div/h5/div/span");
		this.weChatButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div/a[3]/div/div/div/div/h5/div/span");
	}

	private void clickPaymentMethod(Locator element, String methodName) {

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			System.out.println("Wait interrupted: " + e.getMessage());
		}

		try {
			element.waitFor();
			element.click();
			System.out.println(methodName + " clicked Successfully!");
			Thread.sleep(2000);
		} catch (Exception e) {
			System.out.println(methodName + " click failed: " + e.getMessage());
		}

	}

	public void unionPayChina() {
		clickPaymentMethod(unionPayChinaButton, "UnionPay China");
	}

	public void aliPay() {
		clickPaymentMethod(aliPayButton, "AliPay");
	}

	public void weChat() {
		clickPaymentMethod(weChatButton, "WeChat");
	}

	/*
	 * The methods below are read-only questions about this screen, added for
	 * Transfer.feature. Nothing above them changed: the existing click methods and their
	 * locators are what GlobalTransfer.feature already runs on, and rewriting those to suit
	 * a new feature would be putting a working suite at risk for a tidier file.
	 *
	 * They locate by link text rather than by position in the document, because a route
	 * list is exactly the kind of thing that gains an entry - the absolute paths above
	 * already name a[1] to a[3], and a new route inserted anywhere would silently shift
	 * every one of them onto the wrong tile.
	 */

	/** True once this area's route list is on screen. */
	public boolean isShowing() {

		return isPresent(page.locator("xpath=" + "//a[contains(normalize-space(.),'UnionPay China')]"));

	}

	public boolean offersRoute(String route) {

		return isPresent(page.locator("xpath=" + "//a[contains(normalize-space(.),'" + route + "')]"));

	}

	/** Whether the route carries a Maintenance marker in its own tile. */
	public boolean routeIsUnderMaintenance(String route) {

		return isPresent(page.locator("xpath=" + "//a[contains(normalize-space(.),'" + route + "')]"
				+ "[contains(normalize-space(.),'Maintenance')]"));

	}

	/**
	 * Opens a route by its name.
	 *
	 * Clicked through JavaScript: the tiles are single page application links with no href,
	 * and the app raises overlays that intercept an ordinary click.
	 */
	public void openRoute(String route) {

		Locator link = page.locator("xpath=//a[contains(normalize-space(.),'" + route + "')]").first();

		link.waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));

		link.dispatchEvent("click");

		try {
			Thread.sleep(3000);

		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	private boolean isPresent(Locator locator) {

		try {
			locator.first().waitFor(new Locator.WaitForOptions()
					.setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
			return true;

		} catch (Exception notThere) {
			return false;
		}
	}
}
