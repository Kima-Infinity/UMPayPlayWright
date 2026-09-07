package com.umpay.stepdefs;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import com.umpay.pages.DomesticTransferPage;
import com.umpay.pages.GlobalPayoutPage;
import com.umpay.pages.GlobalTransferPage;
import com.umpay.pages.HomePage;
import com.umpay.pages.ReceiverInformationPage;
import com.umpay.pages.SchoolFeeTransferPage;
import com.umpay.pages.TemplatePage;
import com.umpay.pages.TransferPage;
import com.umpay.pages.UMPayWalletTransferPage;
import com.umpay.pages.UnionPayAmountPage;
import com.umpay.utility.BaseClass;
import com.umpay.utility.ExcelDataProvider;
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

	private ReceiverInformationPage receiverInformationPage;

	private ReceiverInformationPage receiverInformation() {

		if (receiverInformationPage == null) {
			receiverInformationPage = new ReceiverInformationPage(BaseClass.driver);
		}

		return receiverInformationPage;
	}

	/*
	 * Column layout of TestData/Transfer_TestData.xlsx
	 * 0 Scenario | 1 LoginID | 2 Password | 3 RecipientPhone | 4 ReceiverCardNumber
	 * 5 TemplateAccount | 6 PayoutAccount
	 *
	 * Every row carries the login, because the Background signs in from this sheet too. The rest
	 * of a row is whatever its scenario needs and blank elsewhere: a wallet transfer needs a
	 * phone number, a UnionPay transfer needs a saved receiver, a payout needs a payout account.
	 */
	private static final int RECIPIENT_PHONE = 3;

	private static final int RECEIVER_CARD_NUMBER = 4;

	private static final int TEMPLATE_ACCOUNT = 5;

	private static final int PAYOUT_ACCOUNT = 6;

	private static final int SCHOOL_FEE_MINIMUM = 7;

	/*
	 * Column layout of TestData/DomesticTransfer_TestData.xlsx
	 * 0 Scenario | 1 LoginID | 2 Password | 3 ReceiverCardNumber | 4 Route | 5 ExpectedMessage
	 *
	 * The receiver column is the same shape as the transfer sheet's, so the send step reads
	 * either file without knowing which it was given.
	 */
	private static final int ROUTE = 4;

	private static final int EXPECTED_MESSAGE = 5;

	private ExcelDataProvider excel;

	/** One value out of a sheet, by row and column. */
	private String fromTheSheet(String rowNumber, String sheetName, String fileName, int column) {

		excel = new ExcelDataProvider(fileName, sheetName);

		return excel.getStringData(sheetName, Integer.parseInt(rowNumber), column);
	}

	/**
	 * One value out of a sheet, by row and the name of its column.
	 *
	 * Column numbers are fine while a step only ever reads one sheet, and a trap the moment it
	 * reads two: the transfer sheet's sixth column is a payout account and the global transfer
	 * sheet's is a first name, so a step that moved between them would read a name and try to
	 * pay it. Asking for the column by the name in the header row travels.
	 */
	private String fromTheSheet(String rowNumber, String sheetName, String fileName, String columnName) {

		excel = new ExcelDataProvider(fileName, sheetName);

		for (int column = 0; column < 30; column++) {

			String heading;

			try {
				heading = excel.getStringData(sheetName, 0, column);
			} catch (Exception noHeadingHere) {
				// A column with no heading is a gap, not the end: a sheet that grew a new column
				// on the right can have blanks between it and the ones it started with.
				continue;
			}

			if (columnName.equalsIgnoreCase(String.valueOf(heading).trim())) {

				try {
					return excel.getStringData(sheetName, Integer.parseInt(rowNumber), column);
				} catch (Exception blank) {
					// A row that leaves this column empty has nothing to give, which is not the
					// same as the sheet not having the column.
					return "";
				}
			}
		}

		throw new IllegalStateException("The sheet " + fileName + " has no column called \""
				+ columnName + "\"");
	}

	/**
	 * Every route an area marks as under maintenance is confirmed to refuse, and to say so.
	 *
	 * A sweep rather than a scenario per route, because the point is the ones nobody has thought
	 * to name: a route that starts saying Maintenance is covered the day it does, without
	 * anybody adding a scenario for it.
	 *
	 * Only the marked ones are opened. The routes that work have scenarios of their own that go
	 * further than opening them, and clicking into each of those here would mean navigating back
	 * to the area six times over for no assertion the suite does not already make.
	 *
	 * A marked route that opens anyway is the failure worth catching: it means the tile is
	 * telling users a service is down while letting them start using it.
	 */
	@Then("every route in the {string} area should open unless it is under maintenance, refused with {string}")
	public void everyRouteShouldOpenUnlessUnderMaintenance(String area, String expected) {

		java.util.List<String> refused = new java.util.ArrayList<>();
		java.util.List<String> working = new java.util.ArrayList<>();
		java.util.List<String> wrong = new java.util.ArrayList<>();

		for (String route : routesOf(area)) {

			if (!isMarkedUnderMaintenance(area, route)) {
				working.add(route);
				continue;
			}

			openRouteIn(area, route);

			if (!showsWarningIn(area)) {
				wrong.add(route + " is marked as under maintenance but opened without a word");
			} else if (!warningSaysIn(area, expected)) {
				wrong.add(route + " refused, but not with \"" + expected + "\"");
			} else {
				refused.add(route);
			}

			dismissWarningIn(area);
		}

		System.out.println(area + ": " + refused.size() + " routes under maintenance " + refused
				+ ", " + working.size() + " not marked " + working);

		assertTrue(wrong.isEmpty(), "In the " + area + " area: " + wrong);

		BaseClass.logger.pass(area + " marks " + refused.size() + " routes as under maintenance "
				+ refused + ", and every one of them refuses to open with \"" + expected + "\"."
				+ " The other " + working.size() + " are not marked: " + working);
	}

	/** The routes an area offers, whichever area it is. */
	private java.util.List<String> routesOf(String area) {

		switch (area) {
			case "Transfer hub":
				return com.umpay.pages.TransferPage.ROUTES;
			case "Domestic Transfer":
				return com.umpay.pages.DomesticTransferPage.ROUTES;
			default:
				return com.umpay.pages.GlobalTransferPage.ROUTES;
		}
	}

	private boolean isMarkedUnderMaintenance(String area, String route) {

		switch (area) {
			case "Transfer hub":
				return transfer().routeIsUnderMaintenance(route);
			case "Domestic Transfer":
				return domestic().routeIsUnderMaintenance(route);
			default:
				return global().routeIsUnderMaintenance(route);
		}
	}

	private void openRouteIn(String area, String route) {

		switch (area) {
			case "Transfer hub" -> transfer().openRoute(route);
			case "Domestic Transfer" -> domestic().openRoute(route);
			default -> global().openRoute(route);
		}
	}

	private boolean showsWarningIn(String area) {

		switch (area) {
			case "Transfer hub":
				return transfer().showsUnavailableWarning();
			case "Domestic Transfer":
				return domestic().showsUnavailableWarning();
			default:
				return global().showsUnavailableWarning();
		}
	}

	private boolean warningSaysIn(String area, String message) {

		switch (area) {
			case "Transfer hub":
				return transfer().warningSays(message);
			case "Domestic Transfer":
				return domestic().warningSays(message);
			default:
				return global().warningSays(message);
		}
	}

	private void dismissWarningIn(String area) {

		switch (area) {
			case "Transfer hub" -> transfer().dismissWarning();
			case "Domestic Transfer" -> domestic().dismissWarning();
			default -> global().dismissWarning();
		}
	}

	/** Whether the area is still on screen, which means the route did not open. */
	private boolean stillShowing(String area) {

		switch (area) {
			case "Transfer hub":
				return transfer().isShowing();
			case "Domestic Transfer":
				return domestic().isShowing();
			default:
				return global().isShowing();
		}
	}

	@When("I open International School Fees from the sidebar")
	public void openInternationalSchoolFees() {

		HomePage home = new HomePage(BaseClass.driver);

		for (int attempt = 1; attempt <= 3; attempt++) {

			home.dismissTwoFactorPromptIfShowing();
			home.openInternationalSchoolFees();

			if (com.umpay.utility.Wait.until(() -> schoolFee().isShowing(), 15)) {
				BaseClass.logger.pass("Opened International School Fees from the sidebar");
				return;
			}

			System.out.println("The school fee form was not ready (attempt " + attempt + " of 3)");
		}

		org.testng.Assert.fail("International School Fees did not open from the sidebar");
	}

	private static final int SCHOOL_FEE_STATED_MINIMUM = 3;

	@When("I enter {string} as the school fee amount")
	public void enterAsTheSchoolFeeAmount(String amount) {

		schoolFee().enterAmount(amount);

		BaseClass.logger.pass("Entered " + amount);
	}

	/**
	 * The form would not go on, and said nothing about why.
	 *
	 * Both halves are asserted because both are true and only one of them is obvious: an amount
	 * below the stated minimum leaves Next enabled and even quotes a fee for it, and pressing it
	 * simply does nothing. The user is given no reason at all, which is worth recording as the
	 * behaviour rather than glossing as "the form refuses".
	 */
	@Then("the form should not go on, and should say nothing about why")
	public void theFormShouldNotGoOnSilently() {

		assertTrue(schoolFee().stillAsksForTheAmount(),
				"The form went on with an amount below the minimum it states");

		assertTrue(com.umpay.utility.PlatformRefusal.showing(BaseClass.driver).isEmpty(),
				"The form said something after all - worth updating this scenario for");

		BaseClass.logger.pass("The form stayed on the amount step and said nothing about why");
	}

	private static final int SCHOOL_TEMPLATE_NAME = 5;

	private static final int SCHOOL_NAME = 6;

	private static final int SCHOOL_CODE = 7;

	private static final int SCHOOL_ADDRESS = 8;

	private static final int STUDENT_NAME = 9;

	private static final int STUDENT_ID = 10;

	@When("I open the schools saved on the account")
	public void openTheSavedSchools() {

		schoolFee().openSavedSchools();

		BaseClass.logger.pass("Opened the saved schools");
	}

	@Then("the schools saved on the account should be listed")
	public void theSavedSchoolsShouldBeListed() {

		com.umpay.pages.TemplatePage templates = new com.umpay.pages.TemplatePage(BaseClass.driver);

		assertTrue(templates.isShowing(), "The Template button did not open the saved schools");

		assertFalse(templates.templatesOffered().isEmpty(),
				"The saved school list opened but offers nothing to pay");

		System.out.println("The account has saved: " + templates.templatesOffered());

		BaseClass.logger.pass("The account has " + templates.templatesOffered().size()
				+ " saved school(s): " + templates.templatesOffered());
	}

	@When("I choose the school saved in {string} of {string} of {string}")
	public void chooseTheSavedSchool(String row, String sheetName, String fileName) {

		String name = fromTheSheet(row, sheetName, fileName, "TemplateName");

		new com.umpay.pages.TemplatePage(BaseClass.driver).choose(name);

		BaseClass.logger.pass("Chose the saved school " + name);
	}

	/**
	 * Choosing a saved school fills the step in, which is the whole point of saving one.
	 *
	 * All five boxes are asserted rather than just the school's name: a template that carried
	 * the school across but lost the student, or filled the student's name and left the
	 * identity number empty, would be worse than one that did nothing at all - the transfer
	 * would go to the right school for the wrong person.
	 */
	@Then("the school and the student should be filled in from {string} of {string} of {string}")
	public void theSchoolAndStudentShouldBeFilledIn(String row, String sheetName, String fileName) {

		assertTrue(schoolFee().asksForTheSchool(),
				"Choosing a saved school did not return to the school step");

		// The boxes fill one after another rather than together, so what is waited for is the
		// step being finished with - not the first sign of it.
		schoolFee().waitUntilTheSchoolIsFilledIn(15);

		assertEquals(schoolFee().schoolName(),
				fromTheSheet(row, sheetName, fileName, "SchoolName"), "School name");

		assertEquals(schoolFee().schoolCode(),
				fromTheSheet(row, sheetName, fileName, "SchoolCode"), "School code");

		assertEquals(schoolFee().schoolAddress(),
				fromTheSheet(row, sheetName, fileName, "SchoolAddress"), "School address");

		assertEquals(schoolFee().studentName(),
				fromTheSheet(row, sheetName, fileName, "StudentName"), "Student name");

		assertEquals(schoolFee().studentId(),
				fromTheSheet(row, sheetName, fileName, "StudentId"), "Student ID");

		BaseClass.logger.pass("The saved school carried across: " + schoolFee().schoolName()
				+ " (" + schoolFee().schoolCode() + "), " + schoolFee().schoolAddress()
				+ ", for " + schoolFee().studentName() + " (" + schoolFee().studentId() + ")");
	}

	/**
	 * Sends the school fee. This moves real money.
	 *
	 * The school step's Next submits the transfer - the application answers with the PIN dialog -
	 * so authorising is part of the same step. Stopping between the two would leave a transfer
	 * submitted and unauthorised, which is a state nothing in this suite could clean up.
	 */
	@When("I send the school fee, which moves real money")
	public void sendTheSchoolFee() {

		schoolFee().sendFromTheSchoolStep();

		com.umpay.utility.PinDialog.authoriseIfAsked(BaseClass.driver, "the school fee");

		BaseClass.logger.pass("Sent the school fee and authorised it with the PIN");
	}

	/**
	 * The school fee went through, or the run says what stopped it.
	 *
	 * Three ways it can end and all three are waited for: the platform says something, the PIN
	 * dialog stays up because it was not accepted, or the flow moves on. Waiting only for the
	 * good one turns either of the others into a timeout that explains nothing.
	 */
	@Then("the school fee should go through")
	public void theSchoolFeeShouldGoThrough() {

		Page page = BaseClass.driver;

		com.umpay.utility.Wait.until(() ->
				!com.umpay.utility.PlatformRefusal.showing(page).isEmpty()
						|| !com.umpay.utility.PinDialog.isStillShowing(page), 40);

		String refusal = com.umpay.utility.PlatformRefusal.showing(page);

		assertTrue(refusal.isEmpty(), "The platform would not take the school fee: " + refusal);

		assertFalse(com.umpay.utility.PinDialog.isStillShowing(page),
				"The school fee was not authorised - the PIN dialog is still up and reads: "
						+ com.umpay.utility.PinDialog.says(page));

		System.out.println("The school fee was accepted. The page is at " + page.url());

		BaseClass.logger.pass("The school fee went through");
	}

	/**
	 * Types a school in rather than choosing one that was saved.
	 *
	 * The other half of the school step, and a different code path: the same boxes are filled,
	 * but by hand and with a country that has to be chosen rather than one that arrives with a
	 * template. An account paying a school for the first time takes this way, and nothing
	 * covered it.
	 */
	@When("I enter the school details in {string} of {string} of {string}")
	public void enterTheSchoolDetails(String row, String sheetName, String fileName) {

		schoolFee().chooseCountry(fromTheSheet(row, sheetName, fileName, "CountryCode"));

		schoolFee().enterSchoolDetails(
				fromTheSheet(row, sheetName, fileName, "SchoolName"),
				fromTheSheet(row, sheetName, fileName, "SchoolCode"),
				fromTheSheet(row, sheetName, fileName, "SchoolAddress"),
				fromTheSheet(row, sheetName, fileName, "StudentName"),
				fromTheSheet(row, sheetName, fileName, "StudentId"));

		BaseClass.logger.pass("Typed in "
				+ fromTheSheet(row, sheetName, fileName, "SchoolName") + " for "
				+ fromTheSheet(row, sheetName, fileName, "StudentName"));
	}

	@Then("the school fee should be ready to send")
	public void theSchoolFeeShouldBeReadyToSend() {

		assertTrue(schoolFee().canSend(),
				"The form will not send with the school and the student typed in");

		BaseClass.logger.pass("The form is ready to send");
	}

	@When("I enter the stated minimum as the school fee amount")
	public void enterTheStatedMinimumAsTheSchoolFeeAmount() {

		String minimum = schoolFee().statedMinimum();

		assertFalse(minimum.isEmpty(), "The school fee form states no minimum");

		schoolFee().enterAmount(minimum);

		BaseClass.logger.pass("Entered the stated minimum of " + minimum);
	}

	@When("I enter one below the stated minimum as the school fee amount")
	public void enterOneBelowTheStatedMinimumAsTheSchoolFeeAmount() {

		String minimum = schoolFee().statedMinimum();

		assertFalse(minimum.isEmpty(), "The school fee form states no minimum to measure against");

		String tooLittle = new java.math.BigDecimal(minimum)
				.subtract(java.math.BigDecimal.ONE).toPlainString();

		schoolFee().enterAmount(tooLittle);

		BaseClass.logger.pass("Entered " + tooLittle + ", one below the stated minimum of " + minimum);
	}

	@When("I enter more than the wallet holds as the school fee amount")
	public void enterMoreThanTheWalletHoldsAsTheSchoolFeeAmount() {

		String balance = schoolFee().availableBalance();

		java.util.regex.Matcher figure =
				java.util.regex.Pattern.compile("([0-9][0-9,]*(?:[.][0-9]+)?)").matcher(balance);

		assertTrue(figure.find(), "The form does not say what the wallet holds: " + balance);

		String tooMuch = new java.math.BigDecimal(figure.group(1).replace(",", ""))
				.add(java.math.BigDecimal.ONE).toPlainString();

		schoolFee().enterAmount(tooMuch);

		BaseClass.logger.pass("Entered " + tooMuch + ", more than the " + balance);
	}

	@Then("the school fee form should state the minimum recorded in {string} of {string} of {string}")
	public void theSchoolFeeFormShouldStateTheRecordedMinimum(String row, String sheetName,
															  String fileName) {

		String expected = fromTheSheet(row, sheetName, fileName, "Minimum");

		assertEquals(schoolFee().statedMinimum(), expected,
				"The school fee form states a minimum of " + schoolFee().statedMinimum()
						+ " rather than " + expected);

		BaseClass.logger.pass("The form states a minimum of " + expected);
	}

	@Then("the school fee transfer should not be ready to continue")
	public void theSchoolFeeTransferShouldNotBeReady() {

		assertTrue(schoolFee().hasNextButton(), "The school fee form has no Next button at all");

		assertFalse(schoolFee().canGoNext(),
				"The school fee form offered to continue with an amount it should refuse");

		BaseClass.logger.pass("Next is disabled, as it should be");
	}

	@Then("the school fee transfer should be ready to continue")
	public void theSchoolFeeTransferShouldBeReady() {

		assertTrue(schoolFee().canGoNext(),
				"Next never became enabled with an amount the form should accept");

		BaseClass.logger.pass("Next is enabled");
	}

	/**
	 * The fee is worked out, and the total is the amount plus it.
	 *
	 * The arithmetic is asserted rather than the figures: a fee that is quoted and then not
	 * added, or added twice, is the kind of error a scenario naming three numbers would miss the
	 * moment the fee changed.
	 */
	@Then("the form should work out the fee and what it comes to")
	public void theFormShouldWorkOutTheFee() {

		String amount = schoolFee().breakdownFigure("Transaction Amount");
		String fee = schoolFee().breakdownFigure("Fee");
		String total = schoolFee().breakdownFigure("You Will Pay");

		assertFalse(amount.isEmpty(), "The form did not state the transaction amount. It reads: "
				+ schoolFee().feeBreakdown());
		assertFalse(fee.isEmpty(), "The form did not state a fee. It reads: "
				+ schoolFee().feeBreakdown());
		assertFalse(total.isEmpty(), "The form did not state what it comes to. It reads: "
				+ schoolFee().feeBreakdown());

		assertEquals(new java.math.BigDecimal(total),
				new java.math.BigDecimal(amount).add(new java.math.BigDecimal(fee)),
				"The form quotes " + amount + " plus a fee of " + fee + " but says it comes to "
						+ total);

		BaseClass.logger.pass(amount + " plus a fee of " + fee + " comes to " + total);
	}

	@When("I go on from the school fee amount")
	public void goOnFromTheSchoolFeeAmount() {

		schoolFee().next();

		BaseClass.logger.pass("Went on from the amount");
	}

	@Then("the form should ask which school is being paid")
	public void theFormShouldAskWhichSchoolIsBeingPaid() {

		assertTrue(schoolFee().asksWhoIsBeingPaid(),
				"Next did not lead to the step that asks who is being paid");

		BaseClass.logger.pass("The form asks which school is being paid");
	}

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

	@Then("the Transfer page should be at {string}")
	public void theTransferPageShouldBeAt(String address) {

		assertTrue(BaseClass.driver.url().contains(address),
				"The Transfer page should be at " + address + " but the run is at "
						+ BaseClass.driver.url());

		BaseClass.logger.pass("The Transfer page is at " + BaseClass.driver.url());
	}

	@Then("the Domestic Transfer area should offer exactly its own routes")
	public void theDomesticAreaShouldOfferExactlyItsOwnRoutes() {

		assertEquals(domestic().routesOffered(),
				java.util.List.of("UnionPay China", "Transfer to Alipay", "Transfer to Wechat"),
				"The Domestic Transfer area does not offer what it used to");

		BaseClass.logger.pass("Domestic Transfer offers " + domestic().routesOffered());
	}

	@Then("the Domestic Transfer route in {string} of {string} of {string} should be marked as under maintenance")
	public void theDomesticRouteShouldBeMarkedUnderMaintenance(String row, String sheetName,
															   String fileName) {

		String route = fromTheSheet(row, sheetName, fileName, "Route");

		assertTrue(domestic().routeIsUnderMaintenance(route),
				"The " + route + " route is not marked as under maintenance");

		BaseClass.logger.pass(route + " is marked as under maintenance");
	}

	@When("I take the route in {string} of {string} of {string} from Domestic Transfer")
	public void iTakeTheRouteFromDomesticTransfer(String row, String sheetName, String fileName) {

		String route = fromTheSheet(row, sheetName, fileName, "Route");

		domestic().openRoute(route);

		BaseClass.logger.pass("Took the " + route + " route from Domestic Transfer");
	}

	/**
	 * The area refuses to open a route, and says why.
	 *
	 * Both halves matter. A route that stopped saying Maintenance while still refusing, or said
	 * it while quietly opening, would be wrong in a way only one of the two checks would catch.
	 */
	@Then("Domestic Transfer should refuse it with the message in {string} of {string} of {string}")
	public void domesticTransferShouldRefuseIt(String row, String sheetName, String fileName) {

		String expected = fromTheSheet(row, sheetName, fileName, "ExpectedMessage");

		assertTrue(domestic().showsUnavailableWarning(),
				"A route under maintenance opened without any warning");

		assertTrue(domestic().warningSays(expected),
				"The warning did not say \"" + expected + "\"");

		domestic().dismissWarning();

		BaseClass.logger.pass("Domestic Transfer refused it with: " + expected);
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

	@Then("the saved templates should be listed")
	public void theSavedTemplatesShouldBeListed() {

		TemplatePage templatePage = new TemplatePage(BaseClass.driver);

		assertTrue(templatePage.isShowing(),
				"The Existing template route did not open the saved templates");

		assertFalse(templatePage.templatesOffered().isEmpty(),
				"The template list opened but offers nothing to transfer to");

		System.out.println("The account has " + templatePage.templatesOffered().size()
				+ " saved templates: " + templatePage.accountsOffered());

		BaseClass.logger.pass("The template list offers "
				+ templatePage.templatesOffered().size() + " saved destinations");
	}

	/**
	 * Every saved destination is masked.
	 *
	 * A list of the accounts this user pays is worth showing; the account numbers themselves are
	 * not, and a list that stopped masking them would be showing every one of them in full.
	 */
	@Then("every saved template should mask the account it pays")
	public void everySavedTemplateShouldMaskTheAccountItPays() {

		TemplatePage templatePage = new TemplatePage(BaseClass.driver);

		java.util.List<String> shown = templatePage.accountsOffered();

		for (String account : shown) {

			assertFalse(account.isEmpty(),
					"A saved template names no account at all. The list reads: "
							+ templatePage.templatesOffered());

			assertTrue(account.contains("*"),
					"A saved template shows the account it pays without masking it: " + account);
		}

		BaseClass.logger.pass("All " + shown.size() + " saved accounts are masked: " + shown);
	}

	@When("I choose the saved template in {string} of {string} of {string}")
	public void chooseTheSavedTemplate(String row, String sheetName, String fileName) {

		String ending = fromTheSheet(row, sheetName, fileName, "TemplateAccount");

		new TemplatePage(BaseClass.driver).selectTemplate(ending);

		BaseClass.logger.pass("Chose the saved template paying the account ending " + ending);
	}

	@Then("the transfer should be going to the account in {string} of {string} of {string}")
	public void theTransferShouldBeGoingTo(String row, String sheetName, String fileName) {

		String ending = fromTheSheet(row, sheetName, fileName, "TemplateAccount");


		UnionPayAmountPage unionPay = new UnionPayAmountPage(BaseClass.driver);

		assertTrue(unionPay.shows("To UnionPay Account"),
				"The form does not name the account the transfer is going to");

		assertTrue(unionPay.shows(ending),
				"The form does not name the account ending " + ending
						+ ", so the template chosen is not the one it opened");

		BaseClass.logger.pass("The transfer is going to the saved account ending " + ending);
	}

	/**
	 * The form that sends is the one this route opens.
	 *
	 * Asserted, and then left alone. Transfer is enabled here from the moment the form opens,
	 * with no amount entered and the card balance reading zero - unlike the deposit, withdraw and
	 * convert forms, whose submit stays disabled until they have what they need. Whether pressing
	 * it would be refused or would send is not something this suite finds out: pressing it moves
	 * real money and no test can undo it, so the state is recorded here rather than tested.
	 */
	@Then("the form that sends should be shown")
	public void theFormThatSendsShouldBeShown() {

		UnionPayAmountPage unionPay = new UnionPayAmountPage(BaseClass.driver);

		assertTrue(unionPay.hasTransferButton(),
				"This form has no Transfer button, so it is not the one that sends");

		System.out.println("The saved template route opens the form that sends."
				+ " Transfer is " + (unionPay.canTransfer() ? "enabled" : "disabled")
				+ " with nothing entered.");

		BaseClass.logger.pass("The saved template route opens the form that sends, and Transfer is "
				+ (unionPay.canTransfer() ? "enabled" : "disabled") + " with nothing entered");
	}

	@When("I enter the stated minimum as the UnionPay amount")
	public void enterTheStatedMinimumAsTheUnionPayAmount() {

		UnionPayAmountPage unionPay = new UnionPayAmountPage(BaseClass.driver);

		String minimum = unionPay.statedMinimum();

		assertFalse(minimum.isEmpty(), "The form states no minimum to send");

		unionPay.enterAmountToPay(minimum);

		BaseClass.logger.pass("Entered the stated minimum of " + minimum);
	}

	/**
	 * Sends the transfer. This moves real money.
	 *
	 * The only step in this file that sends anything, which is why it says so in its own name.
	 */
	@When("I send the transfer, which moves real money")
	public void sendTheTransfer() {

		new UnionPayAmountPage(BaseClass.driver).send();

		BaseClass.logger.pass("Pressed Transfer");
	}

	/**
	 * The transfer went through, or the run says what the platform answered instead.
	 *
	 * The screen is no help here. A refused transfer draws no dialog and no message: the page
	 * returns to the hub looking exactly as it would if the money had gone. So the assertion
	 * rests on the call the page made, which is the only place the platform's answer appears.
	 */
	@Then("the transfer should be accepted")
	public void theTransferShouldBeAccepted() {

		UnionPayAmountPage unionPay = new UnionPayAmountPage(BaseClass.driver);

		com.umpay.utility.Wait.until(
				() -> unionPay.hasLeftTheForm() || !unionPay.answerFromTheApi().isEmpty(), 30);

		String answer = unionPay.answerFromTheApi();

		assertTrue(answer.isEmpty(),
				"The transfer was not accepted, and the application said nothing on screen about it."
						+ " The platform answered: " + answer);

		assertTrue(unionPay.hasLeftTheForm(),
				"The transfer was neither accepted nor refused - the form is still showing");

		BaseClass.logger.pass("The transfer was accepted");
	}

	@When("I enter one above the stated maximum as the UnionPay amount")
	public void enterOneAboveTheStatedUnionPayMaximum() {

		UnionPayAmountPage unionPay = new UnionPayAmountPage(BaseClass.driver);

		String maximum = unionPay.statedMaximum();

		assertFalse(maximum.isEmpty(), "The form states no maximum to measure against");

		String tooMuch = new java.math.BigDecimal(maximum)
				.add(java.math.BigDecimal.ONE).toPlainString();

		unionPay.enterAmountToPay(tooMuch);

		BaseClass.logger.pass("Entered " + tooMuch + ", one above the stated maximum of " + maximum);
	}

	@When("I enter one below the stated minimum as the UnionPay amount")
	public void enterOneBelowTheStatedUnionPayMinimum() {

		UnionPayAmountPage unionPay = new UnionPayAmountPage(BaseClass.driver);

		String minimum = unionPay.statedMinimum();

		assertFalse(minimum.isEmpty(), "The form states no minimum to measure against");

		String tooLittle = new java.math.BigDecimal(minimum)
				.subtract(java.math.BigDecimal.ONE).toPlainString();

		unionPay.enterAmountToPay(tooLittle);

		BaseClass.logger.pass("Entered " + tooLittle + ", one below the stated minimum of " + minimum);
	}

	@When("I enter the stated minimum as the payout amount")
	public void enterTheStatedMinimumAsThePayoutAmount() {

		String minimum = payout().statedMinimum();

		assertFalse(minimum.isEmpty(), "The payout form states no minimum");

		payout().enterAmount(minimum);

		BaseClass.logger.pass("Entered the stated minimum of " + minimum
				+ ", against " + payout().availableBalance());
	}

	@When("I enter one below the stated minimum as the payout amount")
	public void enterOneBelowTheStatedMinimumAsThePayoutAmount() {

		String minimum = payout().statedMinimum();

		assertFalse(minimum.isEmpty(), "The payout form states no minimum to measure against");

		String tooLittle = new java.math.BigDecimal(minimum)
				.subtract(java.math.BigDecimal.ONE).toPlainString();

		payout().enterAmount(tooLittle);

		BaseClass.logger.pass("Entered " + tooLittle + ", one below the stated minimum of " + minimum);
	}

	/**
	 * More than the wallet holds, worked out from the balance the form is showing.
	 *
	 * Not a figure written down: the balance moves every time this suite sends anything, and a
	 * number here would stop meaning "more than the wallet holds" the moment it did.
	 */
	@When("I enter more than the wallet holds as the payout amount")
	public void enterMoreThanTheWalletHoldsAsThePayoutAmount() {

		String balance = payout().availableBalance();

		java.util.regex.Matcher figure =
				java.util.regex.Pattern.compile("([0-9][0-9,]*(?:[.][0-9]+)?)").matcher(balance);

		assertTrue(figure.find(), "The payout form does not say what the wallet holds: " + balance);

		String tooMuch = new java.math.BigDecimal(figure.group(1).replace(",", ""))
				.add(java.math.BigDecimal.ONE).toPlainString();

		payout().enterAmount(tooMuch);

		BaseClass.logger.pass("Entered " + tooMuch + ", more than the " + balance);
	}

	@When("I choose the saved payout account in {string} of {string} of {string}")
	public void chooseTheSavedPayoutAccount(String row, String sheetName, String fileName) {

		String name = fromTheSheet(row, sheetName, fileName, "PayoutAccount");

		payout().chooseSavedPaymentAccount(name);

		BaseClass.logger.pass("Chose the saved payout account " + name);
	}

	/**
	 * Sends the payout. This moves real money.
	 *
	 * Confirm does not send on its own - the application answers with a PIN dialog and the money
	 * only leaves once that is filled in - so authorising is part of the same step. Stopping
	 * between the two would leave a transfer nobody had confirmed and nobody had cancelled.
	 */
	@When("I confirm the payout, which moves real money")
	public void confirmThePayout() {

		payout().confirmAndSend();

		com.umpay.utility.PinDialog.authoriseIfAsked(BaseClass.driver, "the payout");

		BaseClass.logger.pass("Confirmed the payout and authorised it with the PIN");
	}

	/**
	 * The payout went through, or the run says what stopped it.
	 *
	 * Three ways it can end and all three are waited for: the platform says something, the PIN
	 * dialog stays up because it was not accepted, or the form gives way to a receipt. Waiting
	 * only for the good one turns any of the others into a timeout that explains nothing.
	 */
	@Then("the payout should go through")
	public void thePayoutShouldGoThrough() {

		Page page = BaseClass.driver;

		com.umpay.utility.Wait.until(() ->
				!com.umpay.utility.PlatformRefusal.showing(page).isEmpty()
						|| !com.umpay.utility.PinDialog.isStillShowing(page), 30);

		String refusal = com.umpay.utility.PlatformRefusal.showing(page);

		assertTrue(refusal.isEmpty(), "The platform would not take the payout: " + refusal);

		assertFalse(com.umpay.utility.PinDialog.isStillShowing(page),
				"The payout was not authorised - the PIN dialog is still up and reads: "
						+ com.umpay.utility.PinDialog.says(page));

		System.out.println("The payout was accepted. The page is at " + page.url());

		BaseClass.logger.pass("The payout went through");
	}

	@When("I go on to the receiver step")
	public void goOnToTheReceiverStep() {

		UnionPayAmountPage unionPay = new UnionPayAmountPage(BaseClass.driver);

		assertTrue(unionPay.canGoNext(),
				"The form will not go on to the receiver step, so nothing can be sent");

		unionPay.next();

		BaseClass.logger.pass("Went on to the receiver step");
	}

	/**
	 * Sends the transfer to a receiver the account has already saved. This moves real money.
	 *
	 * The whole send in one step - choose the receiver, press Transfer, authorise with the PIN -
	 * because it is one action from the user's point of view and because stopping between its
	 * halves leaves an authorised transfer that nothing has confirmed.
	 */
	@When("I send it to the saved receiver in {string} of {string} of {string}, which moves real money")
	public void sendItToTheSavedReceiver(String row, String sheetName, String fileName) {

		String cardNumber = fromTheSheet(row, sheetName, fileName, "ReceiverCardNumber");

		receiverInformation().transferToExistingReceiver(cardNumber,
				com.umpay.utility.SecurityPin.value());

		BaseClass.logger.pass("Sent the transfer to the saved receiver ending " + cardNumber);
	}

	/**
	 * Sends the transfer to a receiver being named for the first time. This moves real money.
	 *
	 * The other half of the receiver step: an account can pay a card it has saved, or type one
	 * in with the person it belongs to, their purpose and their source of funds. They are
	 * different forms reaching the same send, and only the saved one was covered here.
	 */
	@When("I send it to a new receiver in {string} of {string} of {string}, which moves real money")
	public void sendItToANewReceiver(String row, String sheetName, String fileName) {

		receiverInformation().transferToNewReceiver(
				fromTheSheet(row, sheetName, fileName, "ReceiverAccountNumber"),
				fromTheSheet(row, sheetName, fileName, "FirstName"),
				fromTheSheet(row, sheetName, fileName, "SurName"),
				fromTheSheet(row, sheetName, fileName, "Purpose"),
				fromTheSheet(row, sheetName, fileName, "SourceOfFund"),
				fromTheSheet(row, sheetName, fileName, "Address"),
				com.umpay.utility.SecurityPin.value());

		BaseClass.logger.pass("Sent the transfer to a new receiver: "
				+ fromTheSheet(row, sheetName, fileName, "FirstName") + " "
				+ fromTheSheet(row, sheetName, fileName, "SurName") + ", card ending "
				+ fromTheSheet(row, sheetName, fileName, "ReceiverAccountNumber"));
	}

	/**
	 * The order the transfer raised finished, rather than failing or sitting in processing.
	 *
	 * A transfer that was accepted and then failed is not a transfer that went through, so the
	 * status it settled on is what is asserted rather than the absence of an error on the way.
	 */
	@Then("the transfer should go through")
	public void theTransferShouldGoThrough() {

		String status = receiverInformation().getLastOrderStatus();

		assertTrue(receiverInformation().lastOrderCompleted(),
				"The transfer did not go through. The order settled on \"" + status + "\"");

		BaseClass.logger.pass("The transfer went through. The order reads: " + status);
	}

	@When("I name the recipient by {string}")
	public void nameTheRecipientBy(String how) {

		wallet().nameTheRecipientBy(how);

		BaseClass.logger.pass("Naming the recipient by " + how);
	}

	/**
	 * Says who is being paid, taking the identifier the scenario's row holds.
	 *
	 * Which column that is depends on how the recipient is being named - a wallet ID, an email
	 * address or a phone number - so the step is told the way and looks the column up by name.
	 */
	@When("I give the recipient's {string} from {string} of {string} of {string}")
	public void giveTheRecipient(String how, String row, String sheetName, String fileName) {

		String column = switch (how.toUpperCase()) {
			case "ID" -> "RecipientId";
			case "EMAIL" -> "RecipientEmail";
			default -> "RecipientPhone";
		};

		String identifier = fromTheSheet(row, sheetName, fileName, column);

		assertFalse(identifier.isEmpty(),
				"Row " + row + " of " + fileName + " has no " + column + " to give");

		if ("MOBILE".equalsIgnoreCase(how)) {

			// A mobile is two things: the dialling code, chosen from a list of two hundred
			// countries, and the number. The sheet writes them the way a person would.
			String[] halves = identifier.trim().split("\s+", 2);

			assertTrue(halves.length == 2, "The number in row " + row + " should be written as a"
					+ " dialling code and a number, like \"855 965321152\", but reads \""
					+ identifier + "\"");

			wallet().nameTheRecipientByMobile(halves[0], halves[1]);

		} else {
			wallet().enterRecipient(identifier);
		}

		BaseClass.logger.pass("Gave " + identifier + " as the recipient's " + how);
	}

	@Then("the form should find the recipient")
	public void theFormShouldFindTheRecipient() {

		com.umpay.utility.Wait.until(() -> wallet().hasFoundTheRecipient(), 20);

		assertTrue(wallet().hasFoundTheRecipient(),
				"The form did not find the recipient: the amount box is still locked"
						+ (wallet().showsRecipientNotFound() ? " and it says the user does not exist" : ""));

		assertTrue(wallet().remarkFieldIsEnabled(),
				"The recipient was found but the remark box is still locked");

		BaseClass.logger.pass("The form found the recipient and opened the amount and remark");
	}

	@When("I enter the amount in {string} of {string} of {string} to send")
	public void enterTheAmountToSend(String row, String sheetName, String fileName) {

		String amount = fromTheSheet(row, sheetName, fileName, "Amount");

		assertFalse(amount.isEmpty(), "Row " + row + " of " + fileName + " has no Amount to send");

		wallet().enterAmount(amount);

		BaseClass.logger.pass("Entered " + amount + " to send");
	}

	@Then("the wallet transfer should be ready to continue")
	public void theWalletTransferShouldBeReadyToContinue() {

		com.umpay.utility.Wait.until(() -> wallet().canGoNext(), 15);

		assertTrue(wallet().canGoNext(),
				"The wallet form will not go on with a recipient found and an amount entered");

		BaseClass.logger.pass("The wallet transfer is ready to continue");
	}

	/**
	 * Sends the wallet transfer. This moves real money to another account.
	 *
	 * Next opens the summary the application asks for before it sends, and the PIN authorises
	 * it. Both are done here because between them the transfer is neither sent nor abandoned.
	 */
	@When("I send the wallet transfer, which moves real money")
	public void sendTheWalletTransfer() {

		wallet().next();

		com.umpay.utility.PinDialog.authoriseIfAsked(BaseClass.driver, "the wallet transfer");

		BaseClass.logger.pass("Sent the wallet transfer and authorised it with the PIN");
	}

	@Then("the wallet transfer should go through")
	public void theWalletTransferShouldGoThrough() {

		Page page = BaseClass.driver;

		com.umpay.utility.Wait.until(() ->
				!com.umpay.utility.PlatformRefusal.showing(page).isEmpty()
						|| !com.umpay.utility.PinDialog.isStillShowing(page), 40);

		String refusal = com.umpay.utility.PlatformRefusal.showing(page);

		assertTrue(refusal.isEmpty(), "The platform would not take the wallet transfer: " + refusal);

		assertFalse(com.umpay.utility.PinDialog.isStillShowing(page),
				"The wallet transfer was not authorised - the PIN dialog is still up and reads: "
						+ com.umpay.utility.PinDialog.says(page));

		System.out.println("The wallet transfer was accepted. The page is at " + page.url());

		BaseClass.logger.pass("The wallet transfer went through");
	}

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

	@When("I give the recipient's phone number in {string} of {string} of {string}")
	public void giveTheRecipientsPhoneNumber(String row, String sheetName, String fileName) {

		String phone = fromTheSheet(row, sheetName, fileName, "RecipientPhone");

		wallet().enterRecipientPhone(phone);

		BaseClass.logger.pass("Gave " + phone + " as the recipient's phone number");
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

	/**
	 * The form will go on to the receiver step.
	 *
	 * The failure says what was true at the time, because "a button stayed grey" is not
	 * something anybody can act on. What this route needs beyond a valid amount is a UnionPay
	 * card: the form used to show a Card Available Balance line and no longer does on this
	 * account, and the send it guards is refused at the platform's balance check.
	 */
	@Then("the UnionPay transfer should be ready to continue")
	public void theUnionPayTransferShouldBeReady() {

		assertTrue(unionPay().canGoNext(),
				"Next stayed disabled with an amount inside the stated band."
						+ " The form " + (unionPay().shows("Card Available Balance") ? "shows" : "no longer shows")
						+ " a Card Available Balance, and "
						+ (unionPay().amountToReceive().isEmpty()
								? "this route does not convert"
								: "the converted amount reads \"" + unionPay().amountToReceive() + "\""));

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

	/**
	 * The school fee form's minimum, as the sheet records it.
	 *
	 * The figure is the platform's and it moves - it was 100 when these scenarios were written
	 * and reads 10 today - so it lives in the transfer sheet rather than in the scenario. The
	 * assertion is still exact on purpose: a minimum that changes should fail somewhere a reader
	 * can see, and then be updated in one place.
	 */
	@Then("the school fee form should state the minimum in {string} of {string} of {string}")
	public void theSchoolFeeFormShouldStateTheMinimum(String row, String sheetName, String fileName) {

		String expected = fromTheSheet(row, sheetName, fileName, "SchoolFeeMinimum");

		assertEquals(schoolFee().statedMinimum(), expected,
				"The school fee form states a minimum of " + schoolFee().statedMinimum()
						+ " rather than " + expected);
	}

	/**
	 * Says in the feature file where these scenarios stop.
	 *
	 * No step and no page object method presses Next or Confirm on any of these forms,
	 * because a sent transfer moves real money on the test environment and cannot be undone
	 * by a test. The end to end UnionPay flows that do submit live in GlobalTransfer.feature,
	 * where that is the deliberate subject of the test.
	 */
}
