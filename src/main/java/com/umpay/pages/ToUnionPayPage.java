package com.umpay.pages;

import com.umpay.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;



import java.time.Duration;
import java.util.List;

public class ToUnionPayPage {

	Page page;
	
		private final Locator fromWalletID;


		private final Locator currencyButton;


		private final Locator sendAmountField;


		private final Locator orderDetails;


		private final Locator receiveAmountField;


		private final Locator remarkField;


		private final Locator nextButton;


	public ToUnionPayPage(Page ldriver) {

		this.page = ldriver;
		this.fromWalletID = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/form/div[1]/div/div/div/div/p[2]");
		this.currencyButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/form/div[1]/div/div/div/button");
		this.sendAmountField = page.locator("[id=\'amount-pay\']");
		this.orderDetails = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/form/div[4]/span");
		this.receiveAmountField = page.locator("[id=\'amount-pay\']");
		this.remarkField = page.locator("[id=\'remark\']");
		this.nextButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/form/button");
	}

	public void unionPayTransfer(String currency) {

		try {

			fromWalletID.waitFor();
			System.out.println("From Wallet: " + fromWalletID.innerText());

			currencyButton.waitFor();
			currencyButton.click();

			List<Locator> elements = Wait.all(page.locator("xpath=//*[@id=\"root.dialog\"]/div/div[2]/div/div/div/div/div/div[2]/div/div"));

			for (int i = 1; i < elements.size(); i++) {
				Locator element = elements.get(i);
				try {
					Locator walletCurrency = element.locator("xpath=" + ".//div[1]/div[2]/p[1]");
					Locator balance = element.locator("xpath=" + ".//div[2]/div/p");

					System.out.println("Balance details for " + walletCurrency.innerText() + ":");
					System.out.println("Wallet Balance: " + balance.innerText());
					System.out.println();

					if (walletCurrency.innerText().equals(currency) && !balance.innerText().equals("0.00")) {
						System.out.println("Clicking on wallet currency: " + walletCurrency.innerText());
						walletCurrency.waitFor();
						walletCurrency.click();
						break;
					}
				} catch (Exception e) {
					System.out.println("Error locating sub-elements: " + e.getMessage());
				}
			}

			sendAmountField.waitFor();
			sendAmountField.fill("10");

			orderDetails.waitFor();
			remarkField.waitFor();
			remarkField.fill("Testing");

			nextButton.waitFor();
			nextButton.click();

			Thread.sleep(2000);

		} catch (Exception e) {
			System.out.println("Union Pay Global operation failed: " + e.getMessage());
		}

	}
}
