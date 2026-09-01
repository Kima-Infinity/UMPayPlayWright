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


	public HeaderPage(Page ldriver) {

		this.page = ldriver;
		this.qrButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[3]/nav/div/div/div[2]/div/button[1]/div/div/button");
		this.myQrCodeButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[3]/nav/div/div/div[2]/div/button[1]/div/div/div/div/button[1]");
		this.scanQrCodeButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[3]/nav/div/div/div[2]/div/button[1]/div/div/div/div/button[2]");
		this.notificationButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[3]/nav/div/div/div[2]/div/button[2]");
		this.profileButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[3]/nav/div/div/div[2]/div/button[4]");
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


}
