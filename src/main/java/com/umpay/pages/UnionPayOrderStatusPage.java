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
	 * Waits for the application to settle on a status for the submitted order and reports it.
	 *
	 * Read from the page's own words rather than from a path through it. Every locator in this
	 * class walks eleven positional divs from #root, and the first of them stopped matching: a
	 * transfer that had been submitted, authorised with a PIN and answered without an error still
	 * failed here, waiting thirty seconds for a heading that was on the screen the whole time.
	 * A status is a status wherever the layout puts it.
	 *
	 * The long wait is the application's own: a UnionPay order sits in "Order Processing" for
	 * around two minutes before its final status appears. This polls rather than sleeping through
	 * it, so an order that settles sooner does not hold the run up.
	 *
	 * @return the status the order settled on, for example "Completed" or "Order Processing"
	 */
	public String waitForFinalStatus() throws InterruptedException {

		com.umpay.utility.Wait.until(() -> !statusNow().isEmpty(), 60);

		String first = statusNow();

		if (first.isEmpty()) {
			throw new IllegalStateException("The order was submitted but the page never showed a"
					+ " status for it. It reads: " + whatThePageSays());
		}

		System.out.println("Order Status: " + first);

		// Settled means no longer processing. Polled so an order that finishes in twenty seconds
		// does not cost the run two minutes.
		com.umpay.utility.Wait.until(() -> {
			String now = statusNow();
			return !now.isEmpty() && !now.toLowerCase().contains("processing");
		}, 150);

		String finalStatus = statusNow();

		System.out.println("Final order status: " + finalStatus);

		return finalStatus;
	}

	/**
	 * The status the order is showing right now, wherever the page puts it.
	 *
	 * Two ways, because the page words it two ways: a labelled row reading "Status" beside the
	 * value, and a heading over the order while it is still being processed.
	 */
	private String statusNow() {

		try {
			Locator labelled = page.locator(
					"xpath=//*[normalize-space(text())='Status']/following::*[1]");

			if (labelled.count() > 0 && labelled.first().isVisible()) {

				String said = labelled.first().innerText().replaceAll("\\s+", " ").trim();

				if (!said.isEmpty()) {
					return said;
				}
			}
		} catch (Exception notLabelled) {
			// Not shown as a labelled row on this screen.
		}

		try {
			for (Locator heading : com.umpay.utility.Wait.all(page.locator("xpath=//h5"))) {

				if (!heading.isVisible()) {
					continue;
				}

				String said = heading.innerText().replaceAll("\\s+", " ").trim();

				if (said.toLowerCase().contains("processing") || said.toLowerCase().contains("complet")
						|| said.toLowerCase().contains("fail") || said.toLowerCase().contains("cancel")
						|| said.toLowerCase().contains("accept") || said.toLowerCase().contains("pending")) {

					return said;
				}
			}
		} catch (Exception nothingThere) {
			return "";
		}

		return "";
	}

	/** What the page is saying, for a failure that has to quote it. */
	private String whatThePageSays() {

		try {
			String all = page.locator("xpath=//*[@id='root']").first()
					.innerText().replaceAll("\\s+", " ").trim();

			return all.length() > 600 ? all.substring(all.length() - 600) : all;

		} catch (Exception unreadable) {
			return "nothing that can be read";
		}
	}
}
