package com.umpay.pages;

import com.umpay.utility.Wait;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import com.umpay.utility.FormInput;

import java.time.Duration;
import java.util.List;

public class DepositPage {

	Page page;
	
		private final Locator currencyButton;


		private final Locator amountField;


		private final Locator paymentTypeButton;


		private final Locator paymentNameButton;


		private final Locator fpdPayerNameField;


		private final Locator fpsAccountField;


		private final Locator accountNameField;


		private final Locator accountNumberField;


		private final Locator walletNameField;


		private final Locator walletNumberField;


		private final Locator orderDetailsField;


		private final Locator confirmButton;


		private final Locator submittedOrderStatus;


	public DepositPage(Page ldriver) {

		this.page = ldriver;
		this.currencyButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/form/div/div[1]/div[1]/div[1]/button");
		this.amountField = page.locator("[id=\'amount\']");
		this.paymentTypeButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/form/div/div[2]/div/div/div/div/div/div/button");
		this.paymentNameButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/form/div/div[2]/div/div[1]/div/div[2]/div/div/button");
		this.fpdPayerNameField = page.locator("[id=\'FPSReceiverName\']");
		this.fpsAccountField = page.locator("[id=\'account\']");
		this.accountNameField = page.locator("[id=\'accountName\']");
		this.accountNumberField = page.locator("[id=\'accountNumber\']");
		this.walletNameField = page.locator("[id=\'walletName\']");
		this.walletNumberField = page.locator("[id=\'walletNumber\']");
		this.orderDetailsField = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/form/div/div[3]/div/div");
		this.confirmButton = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/form/button");
		this.submittedOrderStatus = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div/div/div/div[1]/h5");
	}

	public void submitDeposit(String amount, String currency, String paymentType, String paymentName,
							  String fpsAccount, String fpsPayerName,
							  String accountName, String accountNumber,
							  String walletName, String walletNumber) {

		Locator selectedPaymentType = null;
		Locator selectedPaymentName = null;

		currencyButton.waitFor();
		try {
			currencyButton.waitFor();
			currencyButton.click();
		} catch (Exception e) {
			currencyButton.dispatchEvent("click");
		}

		List<Locator> currencies = Wait.all(page.locator("xpath=//*[@id=\"root.dialog\"]/div/div[2]/div/div/div/div/div[2]/div"));
		System.out.println("Number of currencies found: " + currencies.size());

        for (int i = 0; i < currencies.size(); i++) {
			Locator element = currencies.get(i);
            try {
                Locator walletCurrency = element.locator("xpath=" + ".//div[1]/div[2]/p");
                Locator balance = element.locator("xpath=" + ".//div[2]/div/p[2]");

                System.out.println("Balance details for " + walletCurrency.innerText() + ":");
                System.out.println("Wallet Balance: " + balance.innerText());
                System.out.println();

                if (walletCurrency.innerText().equals(currency) && !balance.innerText().equals("0.00")) {
                    System.out.println("Clicking on wallet currency: " + walletCurrency.innerText());
                    element.waitFor();
                    try {
                        element.click();
                    } catch (Exception e) {
                        element.dispatchEvent("click");
                    }
                    break;
                }
            } catch (Exception e) {
                System.out.println("Error locating sub-elements: " + e.getMessage());
            }
        }

		// Same controlled-input race as the withdraw amount; this one has simply been lucky.
		FormInput.type(amountField, amount, "deposit amount");

		paymentTypeButton.waitFor();
		try {
			paymentTypeButton.click();
		} catch (Exception e) {
			paymentTypeButton.dispatchEvent("click");
		}

		List<Locator> elements = Wait.all(page.locator("xpath=//*[@id=\"root.dialog\"]/div/div[2]/div/div/div/div/div[2]/div"));

		for (int i = 0; i < elements.size(); i++) {
			Locator element = elements.get(i);
			try {
				selectedPaymentType = element.locator("xpath=" + ".//div[1]/div[2]/span");

				//System.out.println("Payment Type found: " + selectedPaymentType.innerText());

				if (selectedPaymentType.innerText().equals(paymentType)) {
					System.out.println("Clicking on Payment Type: " + selectedPaymentType.innerText());
					element.waitFor();
					try {
						element.click();
					} catch (Exception clickEx) {
						element.dispatchEvent("click");
					}
					Thread.sleep(2000);
					break;
				}
			} catch (Exception e) {
				System.out.println("Error locating Payment Type sub-elements for index " + i + ": " + e.getMessage());
			}
		}

		paymentNameButton.waitFor();

		if (paymentNameButton.isEnabled()) {
			try {
				paymentNameButton.click();
			} catch (Exception e) {
				paymentNameButton.dispatchEvent("click");
			}

			List<Locator> elements2 = Wait.all(page.locator("xpath=//*[@id=\"root.dialog\"]/div/div[2]/div/div/div/div/div/div[3]/div"));

			for (int j = 1; j <= elements2.size(); j++) {
				try {
					selectedPaymentName = page.locator("xpath=//*[@id=\"root.dialog\"]/div/div[2]/div/div/div/div/div/div[3]/div[" + j + "]/div[1]/p");

					System.out.println("Payment Name is: " + selectedPaymentName.innerText());

					if (selectedPaymentName.innerText().equals(paymentName)) {
						System.out.println("Clicking on Payment Name: " + selectedPaymentName.innerText());
						selectedPaymentName.waitFor();
						try {
							selectedPaymentName.click();
						} catch (Exception clickEx) {
							selectedPaymentName.dispatchEvent("click");
						}
						break;
					}
				} catch (Exception e) {
					System.out.println("Error locating sub-elements: " + e.getMessage());
				}
			}
		}
		else{
			System.out.println("Payment Name is already selected");
		}

		switch (paymentType) {
                case "E-Wallet" -> {
                    fpsAccountField.fill(fpsAccount);
                    fpdPayerNameField.fill(fpsPayerName);
                    clickConfirmButton();
                }
                case "Bank Transfer", "Cash Deposit" -> {
                    accountNameField.fill(accountName);
                    accountNumberField.fill(accountNumber);
                    clickConfirmButton();
                }
                case "Alipay" -> {
                    walletNameField.fill(walletName);
                    walletNumberField.fill(walletNumber);
                    clickConfirmButton();
                }
                case "USDT" -> {
                    clickConfirmButton();
                }
                default -> System.out.println("Payment Type not found");
            }
		submittedOrderStatus.waitFor();
		System.out.println("Submitted Order Status: " + submittedOrderStatus.innerText());


	}

	private void clickConfirmButton() {
		orderDetailsField.waitFor();
		try {
			confirmButton.waitFor();
			confirmButton.click();
			Thread.sleep(5000);
		} catch (Exception e) {
			confirmButton.dispatchEvent("click");
		}
	}



}
