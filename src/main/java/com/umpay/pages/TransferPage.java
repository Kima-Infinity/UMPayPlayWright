package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import java.time.Duration;
import java.util.List;

/**
 * The Transfer hub at /v2/transfer.
 *
 * Transfer is a menu rather than a form: six routes, each leading somewhere different.
 * There is no amount box on this screen at all, so a scenario that wants a form has to
 * step into a route first.
 *
 * Two of the six - Alipay and Wechat - are labelled Maintenance and answer with a warning
 * dialog instead of opening. That is a real product state and this page can read it, so the
 * suite notices when the services come back rather than somebody finding out by accident.
 */
public class TransferPage {

	private final Page page;
	public static final String TEMPLATE = "UMPay to Existing template";
	public static final String UMPAY_WALLET = "UMPay to UMPay Wallet";
	public static final String UNIONPAY_CHINA = "UnionPay China";
	public static final String UNIONPAY_GLOBAL = "UnionPay Global";
	public static final String ALIPAY = "Transfer to Alipay";
	public static final String WECHAT = "Transfer to Wechat";

	/** Every route the hub offers, in the order the page lists them. */
	public static final List<String> ROUTES = List.of(
			TEMPLATE, UMPAY_WALLET, UNIONPAY_CHINA, UNIONPAY_GLOBAL, ALIPAY, WECHAT);

	/** The dialog a route under maintenance raises. */
		private final Locator warningTitle;


		private final Locator warningOkButton;


	public TransferPage(Page ldriver) {

		this.page = ldriver;
		this.warningTitle = page.locator("xpath=//*[normalize-space(text())='Warning']");
		this.warningOkButton = page.locator("xpath=//button[normalize-space()='Ok']");
	}

	public boolean isShowing() {

		return isPresent(page.locator("xpath=" + "//a[contains(normalize-space(.),'" + UMPAY_WALLET + "')]"));

	}

	public boolean offersRoute(String route) {

		return isPresent(routeLink(route));

	}

	/** Whether the route is marked as being under maintenance in its own tile. */
	public boolean routeIsUnderMaintenance(String route) {

		return isPresent(page.locator("xpath=" + "//a[contains(normalize-space(.),'" + route + "')]"
				+ "[contains(normalize-space(.),'Maintenance')]"));

	}

	public void openRoute(String route) {

		Locator link = routeLink(route).first();

		link.waitFor(new Locator.WaitForOptions()
				.setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
		click(link);

	}

	public boolean showsUnavailableWarning() {

		return isPresent(page.locator("xpath=" + "//*[normalize-space(text())='Warning']"));

	}

	public boolean warningSays(String message) {

		return isPresent(page.locator("xpath=" + "//*[contains(normalize-space(text()),\"" + message + "\")]"));

	}

	/** Closes the warning so the next step is not looking at a page behind a dialog. */
	public void dismissWarning() {

		try {
			warningOkButton.waitFor();
			click(warningOkButton);

		} catch (Exception noDialog) {
			System.out.println("There was no warning dialog to dismiss.");
		}
	}

	/**
	 * The tile for a named route.
	 *
	 * A Locator rather than Selenium's By: it is still just a description of where to look,
	 * resolved when it is used rather than when it is made, so it can be handed around and
	 * asked about exactly the way the By was.
	 */
	private Locator routeLink(String route) {

		return page.locator("xpath=//a[contains(normalize-space(.),'" + route + "')]");

	}

	/**
	 * Clicked through JavaScript rather than with a plain click.
	 *
	 * The route tiles are single page application links with no href, and the app raises
	 * overlays - the two factor prompt after signing in is the usual one - that sit over
	 * them. An ordinary click on a covered tile fails with ElementClickInterceptedException,
	 * which was every route on the first exploration run.
	 */
	private void click(Locator element) {

		element.dispatchEvent("click");

		sleep();

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

	/** The app animates between routes; reading the next screen too early finds the last one. */
	private void sleep() {

		try {
			Thread.sleep(3000);

		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}
}
