package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;



import java.time.Duration;

public class HeaderPage {

	Page page;
	
		private final Locator qrButton;


		private final Locator myQrCodeButton;


		private final Locator scanQrCodeButton;


		private final Locator notificationButton;


		private final Locator profileButton;

	/**
	 * The hamburger that collapses and expands the left navigation.
	 *
	 * By its own class rather than by position. It is the first button in the bar, so an
	 * index would work today, but the class says what the element is for and the index
	 * only says where it happens to sit.
	 */
		private final Locator sidebarToggle;


	public HeaderPage(Page ldriver) {

		this.page = ldriver;
		this.qrButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[3]/nav/div/div/div[2]/div/button[1]/div/div/button");
		this.myQrCodeButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[3]/nav/div/div/div[2]/div/button[1]/div/div/div/div/button[1]");
		this.scanQrCodeButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[3]/nav/div/div/div[2]/div/button[1]/div/div/div/div/button[2]");
		this.notificationButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[3]/nav/div/div/div[2]/div/button[2]");
		this.profileButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[3]/nav/div/div/div[2]/div/button[4]");
		this.sidebarToggle = page.locator("button.sidebar-toggle");
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

	public void qr() {
		clickPaymentMethod(qrButton, "QR");
	}

	public void myQrCode() {
		clickPaymentMethod(myQrCodeButton, "My QR Code");
	}

	public void scanQrCode() {
		clickPaymentMethod(scanQrCodeButton, "Scan QR Code");
	}

	public void notification() {
		clickPaymentMethod(notificationButton, "Notification Button");
	}

	public void profile() {
		clickPaymentMethod(profileButton, "Profile Button");
	}



	/** Collapses or expands the left navigation, whichever it is not already. */
	public void toggleSidebar() {

		clickPaymentMethod(sidebarToggle, "Sidebar toggle");

	}

	/**
	 * Whether the left navigation is showing its entries.
	 *
	 * Asked of a navigation entry rather than of the sidebar itself, because collapsing
	 * narrows the sidebar rather than removing it - the container is present either way and
	 * only the entries stop being visible.
	 */
	public boolean isSidebarExpanded() {

		return page.locator("[id='home']").isVisible();

	}
}
