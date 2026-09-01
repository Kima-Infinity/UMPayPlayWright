package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


import java.time.Duration;

public class UnionPayOrderStatusPage {

	Page page;
	
		private final Locator orderProcessingStatus;


		private final Locator orderLatestStatus;


		private final Locator orderNumber;


		private final Locator transactionType;


		private final Locator status;


		private final Locator currency;


		private final Locator totalAmount;


		private final Locator fee;


		private final Locator requestAmount;


		private final Locator exchangeRate;


		private final Locator receiveCurrency;


		private final Locator receiveAmount;


		private final Locator createdDate;


	public UnionPayOrderStatusPage(Page ldriver) {

		this.page = ldriver;
		this.orderProcessingStatus = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div[2]/div/div[1]/div/div[1]/h5");
		this.orderLatestStatus = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div[2]/div/div/div[1]/div[1]/div/div[1]/h5");
		this.orderNumber = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div[2]/div/div/div[1]/div[2]/div[1]/span[2]");
		this.transactionType = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div[2]/div/div/div[1]/div[2]/div[2]/span[2]");
		this.status = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div[2]/div/div/div[1]/div[2]/div[3]/span[2]");
		this.currency = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div[2]/div/div/div[1]/div[2]/div[4]/span[2]");
		this.totalAmount = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div[2]/div/div/div[1]/div[2]/div[5]/span[2]");
		this.fee = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div[2]/div/div/div[1]/div[2]/div[6]/span[2]");
		this.requestAmount = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div[2]/div/div/div[1]/div[2]/div[7]/span[2]");
		this.exchangeRate = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div[2]/div/div/div[1]/div[2]/div[8]/span[2]");
		this.receiveCurrency = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div[2]/div/div/div[1]/div[2]/div[9]/span[2]");
		this.receiveAmount = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div[2]/div/div/div[1]/div[2]/div[10]/span[2]");
		this.createdDate = page.locator("xpath=//*[@id=\"root\"]/div[1]/div/div[2]/div[4]/div[1]/div/div[2]/div/div/div[1]/div[2]/div[11]/span[2]");
	}



	/**
	 * Waits for the application to settle on a status for the submitted order and
	 * reports it.
	 *
	 * This used to fail the test outright for every status other than "Completed",
	 * from inside the page object, with the message "Order has failed" -
	 * a sentence that named neither the order nor the scenario. The page now reports
	 * what it read and the caller decides whether that is a pass.
	 *
	 * The two minute wait is the application's own: a UnionPay order sits in "Order
	 * Processing" for that long before its final status appears.
	 *
	 * @return the final status text, for example "Completed" or "Order Processing"
	 */
	public String waitForFinalStatus() throws InterruptedException {

		orderProcessingStatus.waitFor();
		System.out.println("Order Status: " + orderProcessingStatus.innerText());

		Thread.sleep(120000);

		orderLatestStatus.waitFor();

		String finalStatus = orderLatestStatus.innerText().trim();
		System.out.println("Final order status: " + finalStatus);

		return finalStatus;

	}

}
