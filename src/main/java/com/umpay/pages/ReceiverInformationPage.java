package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;



import java.time.Duration;

public class ReceiverInformationPage {

	Page page;
	
		private final Locator templateButton;


		private final Locator accountNumberField;


		private final Locator firstNameField;


		private final Locator chineseFirstNameField;


		private final Locator surNameField;


		private final Locator chineseSurNameField;


		private final Locator purposeField;


		private final Locator sourceOfFundField;


		private final Locator addressField;


		private final Locator transferButton;


		private final Locator pinPopUp;


		private final Locator pinField;



		private final Locator submitTransferError;



	UnionPayOrderStatusPage unionPayOrderStatusPage;

	/**
	 * The status of the order this page last submitted, held so the step that asked
	 * for the transfer can assert on the outcome in the language of its scenario.
	 */
	private String lastOrderStatus;



	public ReceiverInformationPage(Page ldriver) {

		this.page = ldriver;
		this.templateButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[3]/div/div/div[3]/button");
		this.accountNumberField = page.locator("[id=\'account-number\']");
		this.firstNameField = page.locator("[id=\'firstName\']");
		this.chineseFirstNameField = page.locator("[id=\'firstNameCn\']");
		this.surNameField = page.locator("[id=\'surname\']");
		this.chineseSurNameField = page.locator("[id=\'surnameCn\']");
		this.purposeField = page.locator("[id=\'purpose\']");
		this.sourceOfFundField = page.locator("[id=\'sourceOfFund\']");
		this.addressField = page.locator("[id=\'address\']");
		this.transferButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/form/button");
		this.pinPopUp = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div/div[2]/div/div");
		this.pinField = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div/div[2]/div/div/div/div/div/div/div/div/div/input");
		this.submitTransferError = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div/div[2]/div/div/div/div/div/div/em");
		this.unionPayOrderStatusPage = new UnionPayOrderStatusPage(page);
	}

	public void transferToNewReceiver(String accountNumber, String firstName, String surName, String purpose, String sourceOfFund,String address, String pin) {

        try {
            accountNumberField.waitFor();
            accountNumberField.fill(accountNumber);

			firstNameField.waitFor();
			firstNameField.fill(firstName);

			surNameField.waitFor();
			surNameField.fill(surName);

			purposeField.waitFor();
			Locator purposeDropDown = purposeField;
			purposeDropDown.selectOption(new com.microsoft.playwright.options.SelectOption().setLabel(purpose));

			sourceOfFundField.waitFor();
			Locator sourceOfFunddropDown = sourceOfFundField;
			sourceOfFunddropDown.selectOption(new com.microsoft.playwright.options.SelectOption().setLabel(sourceOfFund));

			addressField.waitFor();
			addressField.fill(address);

			transferButton.waitFor();
			transferButton.click();

			pinPopUp.waitFor();
			pinField.fill(pin);

			Thread.sleep(5000);
			// Wait for error message briefly, but proceed if it doesn't appear
			try {
				submitTransferError.waitFor(new Locator.WaitForOptions().setTimeout(5 * 1000));
				if(submitTransferError.isVisible()){
					throw new RuntimeException("Transfer failed due to error: " + submitTransferError.innerText());
				}
			} catch (com.microsoft.playwright.TimeoutError e) {
				// Element not found or not displayed within 5 seconds, continue with order status check
				System.out.println("No transfer error detected, proceeding to order status check.");
			}

			Thread.sleep(5000);
			lastOrderStatus = unionPayOrderStatusPage.waitForFinalStatus();


        } catch (Exception e) {
            throw new RuntimeException(e);
        }


		}
	public void transferToExistingReceiver(String cardNumber, String pin){

        try {
            templateButton.waitFor();
            templateButton.click();

			TemplatePage templatePage = new TemplatePage(page);
			templatePage.selectTemplate(cardNumber);

			transferButton.waitFor();
			transferButton.click();

			pinPopUp.waitFor();
			pinField.fill(pin);

			Thread.sleep(5000);

			// Wait for error message briefly, but proceed if it doesn't appear
			try {
				submitTransferError.waitFor(new Locator.WaitForOptions().setTimeout(5 * 1000));
				if(submitTransferError.isVisible()){
					throw new RuntimeException("Transfer failed due to error: " + submitTransferError.innerText());
				}
			} catch (com.microsoft.playwright.TimeoutError e) {
				// Element not found or not displayed within 5 seconds, continue with order status check
				System.out.println("No transfer error detected, proceeding to order status check.");
			}

				Thread.sleep(5000);
				lastOrderStatus = unionPayOrderStatusPage.waitForFinalStatus();


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }



	/**
	 * The status the last submitted order settled on, or an empty string when no
	 * transfer has been submitted through this page.
	 */
	public String getLastOrderStatus() {

		return lastOrderStatus == null ? "" : lastOrderStatus;

	}

	/** Whether that order finished, as opposed to failing, cancelling or still processing. */
	public boolean lastOrderCompleted() {

		return getLastOrderStatus().contains("Completed");

	}

}
