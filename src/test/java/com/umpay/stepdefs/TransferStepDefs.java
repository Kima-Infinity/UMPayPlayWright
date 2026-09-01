package com.umpay.stepdefs;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import com.umpay.pages.DomesticTransferPage;
import com.umpay.pages.GlobalPayoutPage;
import com.umpay.pages.GlobalTransferPage;
import com.umpay.pages.HomePage;
import com.umpay.pages.SchoolFeeTransferPage;
import com.umpay.pages.TransferPage;
import com.umpay.pages.UMPayWalletTransferPage;
import com.umpay.pages.UnionPayAmountPage;
import com.umpay.utility.BaseClass;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * The steps Transfer.feature is written from.
 *
 * Separate from GlobalTransferStepDefs because that file drives one thing thoroughly - a
 * UnionPay transfer from end to end, out of a spreadsheet - while these cover the breadth
 * of the module: every area, every route, and the state each form is in before anything is
 * sent. Neither duplicates the other.
 *
 * The pages are built when they are first asked for rather than in a constructor, because
 * Cucumber creates this class per scenario while the browser is opened by the Before hook,
 * so a field initialised too early would capture a page that does not exist yet.
 */
public class TransferStepDefs {

	private HomePage homePage;
	private TransferPage transferPage;
	private DomesticTransferPage domesticTransferPage;
	private GlobalTransferPage globalTransferPage;
	private UMPayWalletTransferPage walletPage;
	private UnionPayAmountPage unionPayPage;
	private GlobalPayoutPage payoutPage;
	private SchoolFeeTransferPage schoolFeePage;

	private HomePage home() {
		if (homePage == null) {
			homePage = new HomePage(BaseClass.driver);
		}
		return homePage;
	}

	private TransferPage transfer() {
		if (transferPage == null) {
			transferPage = new TransferPage(BaseClass.driver);
		}
		return transferPage;
	}

	private DomesticTransferPage domestic() {
		if (domesticTransferPage == null) {
			domesticTransferPage = new DomesticTransferPage(BaseClass.driver);
		}
		return domesticTransferPage;
	}

	private GlobalTransferPage global() {
		if (globalTransferPage == null) {
			globalTransferPage = new GlobalTransferPage(BaseClass.driver);
		}
		return globalTransferPage;
	}

	private UMPayWalletTransferPage wallet() {
		if (walletPage == null) {
			walletPage = new UMPayWalletTransferPage(BaseClass.driver);
		}
		return walletPage;
	}

	private UnionPayAmountPage unionPay() {
		if (unionPayPage == null) {
			unionPayPage = new UnionPayAmountPage(BaseClass.driver);
		}
		return unionPayPage;
	}

	private GlobalPayoutPage payout() {
		if (payoutPage == null) {
			payoutPage = new GlobalPayoutPage(BaseClass.driver);
		}
		return payoutPage;
	}

	private SchoolFeeTransferPage schoolFee() {
		if (schoolFeePage == null) {
			schoolFeePage = new SchoolFeeTransferPage(BaseClass.driver);
		}
		return schoolFeePage;
	}

	// ------------------------------------------------------------------
	// Getting to each transfer area
	// ------------------------------------------------------------------

	@When("I open the Transfer hub")
	public void iOpenTheTransferHub() {

		home().dismissTwoFactorPromptIfShowing();
		home().openTransferHub();

		assertTrue(transfer().isShowing(), "The Transfer hub did not open");

	}

	@When("I open the Domestic Transfer area")
	public void iOpenTheDomesticTransferArea() {

		home().dismissTwoFactorPromptIfShowing();
		home().openDomesticTransfer();

		assertTrue(domestic().isShowing(), "The Domestic Transfer area did not open");

	}

	@When("I open the Global Transfer area")
	public void iOpenTheGlobalTransferArea() {

		home().dismissTwoFactorPromptIfShowing();
		home().openGlobalTransfer();

		assertTrue(global().isShowing(), "The Global Transfer area did not open");

	}

	// ------------------------------------------------------------------
	// What each area offers
	// ------------------------------------------------------------------

	@Then("the Transfer hub should offer the route {string}")
	public void theTransferHubShouldOfferTheRoute(String route) {

		assertTrue(transfer().offersRoute(route),
				"The Transfer hub does not offer the " + route + " route");

	}

	@Then("the Domestic Transfer area should offer the route {string}")
	public void theDomesticAreaShouldOfferTheRoute(String route) {

		assertTrue(domestic().offersRoute(route),
				"Domestic Transfer does not offer the " + route + " route");

	}

	@Then("the Global Transfer area should offer the route {string}")
	public void theGlobalAreaShouldOfferTheRoute(String route) {

		assertTrue(global().offersRoute(route),
				"Global Transfer does not offer the " + route + " route");

	}

	@Then("the route {string} should be marked as under maintenance")
	public void theRouteShouldBeMarkedUnderMaintenance(String route) {

		assertTrue(transfer().routeIsUnderMaintenance(route),
				"The " + route + " route is not marked as under maintenance");

	}

	// ------------------------------------------------------------------
	// Taking a route
	// ------------------------------------------------------------------

	@When("I take the {string} route from the Transfer hub")
	public void iTakeTheRouteFromTheHub(String route) {

		transfer().openRoute(route);

	}

	@When("I take the {string} route from Domestic Transfer")
	public void iTakeTheRouteFromDomestic(String route) {

		domestic().openRoute(route);

	}

	@When("I take the {string} route from Global Transfer")
	public void iTakeTheRouteFromGlobal(String route) {

		global().openRoute(route);

	}

	@Then("the app should say the service is unavailable")
	public void theAppShouldSayTheServiceIsUnavailable() {

		assertTrue(transfer().showsUnavailableWarning(),
				"A route under maintenance opened without any warning");

		assertTrue(transfer().warningSays("The service is currently unavailable"),
				"The warning did not say the service is unavailable");

		// Cleared here so the scenario does not hand the next step a page behind a dialog.
		transfer().dismissWarning();

	}

	// ------------------------------------------------------------------
	// The wallet to wallet form
	// ------------------------------------------------------------------

	@Then("the UMPay wallet transfer form should be shown")
	public void theWalletFormShouldBeShown() {

		assertTrue(wallet().isShowing(), "The wallet transfer form did not open");

	}

	@Then("the wallet form should ask for the recipient's phone number")
	public void theWalletFormShouldAskForThePhone() {

		assertTrue(wallet().hasRecipientPhoneField(),
				"The wallet form has no country and phone number fields");

	}

	@Then("the wallet form should ask for an amount and a remark")
	public void theWalletFormShouldAskForAmountAndRemark() {

		assertTrue(wallet().hasAmountField(), "The wallet form has no amount field");
		assertTrue(wallet().hasRemarkField(), "The wallet form has no remark field");

	}

	@Then("the wallet form should show the sending wallet and its balance")
	public void theWalletFormShouldShowTheSendingWallet() {

		assertTrue(wallet().showsSourceWalletBalance(),
				"The wallet form does not show the sending wallet's balance");

	}

	@Then("the wallet transfer should not be ready to continue")
	public void theWalletTransferShouldNotBeReady() {

		assertTrue(wallet().hasNextButton(), "The wallet form has no Next button at all");

		assertFalse(wallet().canGoNext(),
				"Next is already enabled on an empty wallet transfer form");

	}

	@Then("the wallet form should keep the amount and remark locked")
	public void theWalletFormShouldKeepTheAmountLocked() {

		assertFalse(wallet().amountFieldIsEnabled(),
				"The amount box is open before the application knows who is receiving");

		assertFalse(wallet().remarkFieldIsEnabled(),
				"The remark box is open before the application knows who is receiving");

	}

	@When("I give {string} as the recipient's phone number")
	public void iGiveTheRecipientsPhoneNumber(String phone) {

		wallet().enterRecipientPhone(phone);

	}

	@Then("the form should say the recipient does not exist")
	public void theFormShouldSayTheRecipientDoesNotExist() {

		assertTrue(wallet().showsRecipientNotFound(),
				"An unknown phone number was accepted without any complaint");

	}

	// ------------------------------------------------------------------
	// The UnionPay amount step
	// ------------------------------------------------------------------

	@Then("the UnionPay amount form should be shown")
	public void theUnionPayFormShouldBeShown() {

		assertTrue(unionPay().isShowing(), "The UnionPay amount form did not open");

	}

	@Then("the UnionPay form should state {string}")
	public void theUnionPayFormShouldState(String label) {

		assertTrue(unionPay().shows(label), "The UnionPay form does not show " + label);

	}

	@Then("the UnionPay form should offer a converted amount")
	public void theUnionPayFormShouldOfferAConvertedAmount() {

		assertTrue(unionPay().hasAmountToReceiveField(),
				"The converting route has no box for the amount received");

	}

	@Then("the UnionPay form should not offer a converted amount")
	public void theUnionPayFormShouldNotOfferAConvertedAmount() {

		assertFalse(unionPay().hasAmountToReceiveField(),
				"The Global route shows an amount received box, which it is not expected to have");

	}

	@Then("the UnionPay transfer should not be ready to continue")
	public void theUnionPayTransferShouldNotBeReady() {

		assertTrue(unionPay().hasNextButton(), "The UnionPay form has no Next button at all");

		assertFalse(unionPay().canGoNext(),
				"Next is already enabled on an empty UnionPay form");

	}

	@When("I enter {string} as the UnionPay amount")
	public void iEnterTheUnionPayAmount(String amount) {

		unionPay().enterAmountToPay(amount);

	}

	@Then("the UnionPay transfer should be ready to continue")
	public void theUnionPayTransferShouldBeReady() {

		assertTrue(unionPay().canGoNext(),
				"Next never became enabled after a valid amount was entered");

	}

	@Then("the converted amount should be worked out")
	public void theConvertedAmountShouldBeWorkedOut() {

		String received = unionPay().amountToReceive();

		System.out.println("The form converted the amount to: " + received);

		assertTrue(received != null && !received.trim().isEmpty(),
				"The amount received was left empty after an amount was entered");

	}

	// ------------------------------------------------------------------
	// The Global Transfer payout routes
	// ------------------------------------------------------------------

	@Then("the {string} form should be shown")
	public void thePayoutFormShouldBeShown(String heading) {

		assertTrue(payout().isShowing(heading), "The " + heading + " form did not open");

	}

	@Then("the payout form should ask for a currency and an amount")
	public void thePayoutFormShouldAskForCurrencyAndAmount() {

		assertTrue(payout().hasCurrencyField(), "The payout form has no currency field");
		assertTrue(payout().hasAmountField(), "The payout form has no amount field");

	}

	@Then("the payout form should state {string}")
	public void thePayoutFormShouldState(String label) {

		assertTrue(payout().shows(label), "The payout form does not show " + label);

	}

	@Then("the payout should not be ready to confirm")
	public void thePayoutShouldNotBeReadyToConfirm() {

		assertTrue(payout().hasConfirmButton(), "The payout form has no Confirm button at all");

		assertFalse(payout().canConfirm(),
				"Confirm is already enabled on an empty payout form");

	}

	// ------------------------------------------------------------------
	// The school fee form
	// ------------------------------------------------------------------

	@Then("the school fee form should be shown")
	public void theSchoolFeeFormShouldBeShown() {

		assertTrue(schoolFee().isShowing(), "The school fee form did not open");

	}

	@Then("the school fee form should ask for an amount and a remark")
	public void theSchoolFeeFormShouldAskForAmountAndRemark() {

		assertTrue(schoolFee().hasAmountField(), "The school fee form has no amount field");
		assertTrue(schoolFee().hasRemarkField(), "The school fee form has no remark field");

	}

	@Then("the school fee form should show the sending wallet and its balance")
	public void theSchoolFeeFormShouldShowTheWallet() {

		assertTrue(schoolFee().showsSourceWalletBalance(),
				"The school fee form does not show the sending wallet's balance");

	}

	@Then("the school fee form should state a minimum of {string}")
	public void theSchoolFeeFormShouldStateAMinimumOf(String minimum) {

		String stated = schoolFee().statedMinimum();

		assertEquals(stated, minimum,
				"The school fee form states a minimum of " + stated + " rather than " + minimum);

	}

	/**
	 * Says in the feature file where these scenarios stop.
	 *
	 * No step and no page object method presses Next or Confirm on any of these forms,
	 * because a sent transfer moves real money on the test environment and cannot be undone
	 * by a test. The end to end UnionPay flows that do submit live in GlobalTransfer.feature,
	 * where that is the deliberate subject of the test.
	 */
	@Then("the transfer is deliberately not sent")
	public void theTransferIsDeliberatelyNotSent() {

		System.out.println("Stopping on a completed transfer form: sending would move real"
				+ " money and cannot be undone by a test.");

	}
}
