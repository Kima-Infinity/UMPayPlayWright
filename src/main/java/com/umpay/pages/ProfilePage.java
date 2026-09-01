package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;



import java.time.Duration;

public class ProfilePage {

	Page page;
		LoginPage loginPage;

		private final Locator editProfileButton;


		private final Locator tradeRecordButton;


		private final Locator referralCodeButton;


		private final Locator userListButton;


		private final Locator CommissionListButton;


		private final Locator feeListButton;


		private final Locator walletButton;


		private final Locator paymentButton;


		private final Locator templateButton;


		private final Locator transferFeeSettingButton;


		private final Locator documentVerificationButton;


		private final Locator securityButton;


		private final Locator languagesButton;


		private final Locator settingsButton;


		private final Locator logoutButton;


		private final Locator logOutConfirmPopUp;


		private final Locator cancelLogOutButton;


		private final Locator confconfirmLogoutButton;



	public ProfilePage(Page ldriver) {

		this.page = ldriver;
		this.editProfileButton = page.locator("xpath=//*[@id=\"profile-sidebar\"]/div/aside/div[1]/div/div[1]/div");
		this.tradeRecordButton = page.locator("[id=\'trade_record\']");
		this.referralCodeButton = page.locator("xpath=//*[@id=\"profile-sidebar\"]/div/aside/div[2]/ul/li[2]/button");
		this.userListButton = page.locator("[id=\'user_list\']");
		this.CommissionListButton = page.locator("[id=\'commission_listing\']");
		this.feeListButton = page.locator("[id=\'fee_listing\']");
		this.walletButton = page.locator("[id=\'wallet\']");
		this.paymentButton = page.locator("[id=\'payment\']");
		this.templateButton = page.locator("[id=\'template\']");
		this.transferFeeSettingButton = page.locator("[id=\'transfer_fee_setting\']");
		this.documentVerificationButton = page.locator("xpath=//*[@id=\"profile-sidebar\"]/div/aside/div[2]/ul/li[12]/button");
		this.securityButton = page.locator("[id=\'transfer_fee_setting\']");
		this.languagesButton = page.locator("[id=\'languages\']");
		this.settingsButton = page.locator("[id=\'settings\']");
		this.logoutButton = page.locator("[id=\'logout\']");
		this.logOutConfirmPopUp = page.locator("xpath=//*[@id=\"root.dialog\"]/div/div[2]/div/div/div/div/div/div[2]");
		this.cancelLogOutButton = page.locator("xpath=//*[@id=\"root.dialog\"]/div/div[2]/div/div/div/div/div/div[2]/div/button[1]");
		this.confconfirmLogoutButton = page.locator("xpath=//*[@id=\"root.dialog\"]/div/div[2]/div/div/div/div/div/div[2]/div/button[2]");
		this.loginPage = new LoginPage(page);
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

	public void editProfile() {

		clickPaymentMethod(editProfileButton, "Edit Profile");
	}
	public void tradeRecord() {
		clickPaymentMethod(tradeRecordButton, "Trade Record");
	}
	public void referralCode() {
		clickPaymentMethod(referralCodeButton, "Referral Code");
	}
	public void userList() {
		clickPaymentMethod(userListButton, "User List");
	}
	public void CommissionList() {
		clickPaymentMethod(CommissionListButton, "Commission List");
	}
	public void feeList() {
		clickPaymentMethod(feeListButton, "Fee List");
	}
	public void wallet() {
		clickPaymentMethod(walletButton, "Wallet");
	}
	public void payment() {
		clickPaymentMethod(paymentButton, "Payment");
	}
	public void template() {
		clickPaymentMethod(templateButton, "Template");
	}
	public void transferFeeSetting() {
		clickPaymentMethod(transferFeeSettingButton, "Transfer Fee Setting");
	}
	public void documentVerification() {
		clickPaymentMethod(documentVerificationButton, "Document Verification");
	}
	public void security() {
		clickPaymentMethod(securityButton, "Security");
	}
	public void languages() {
		clickPaymentMethod(languagesButton, "Languages");
	}
	public void settings() {
		clickPaymentMethod(settingsButton, "Settings");
	}

	public void logout() {
		clickPaymentMethod(logoutButton, "Logout");
		logOutConfirmPopUp.waitFor();
		confconfirmLogoutButton.click();

		loginPage.waitUntilReady();
	}
}
