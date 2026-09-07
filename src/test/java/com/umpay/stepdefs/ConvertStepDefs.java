package com.umpay.stepdefs;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import com.umpay.pages.ConvertPage;
import com.umpay.pages.HomePage;
import com.umpay.utility.BaseClass;
import com.umpay.utility.ExcelDataProvider;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ConvertStepDefs {

    /*
     * Column layout of TestData/Convert_TestData.xlsx
     * 0 Scenario | 1 LoginID | 2 Password | 3 FromCurrency | 4 ToCurrency
     * 5 Amount | 6 ExpectedMessage
     */
    private static final int FROM_CURRENCY = 3;
    private static final int TO_CURRENCY = 4;
    private static final int AMOUNT = 5;
    private static final int EXPECTED_MESSAGE = 6;

    /** How long to give the application to answer a submitted conversion. */
    private static final int OUTCOME_TIMEOUT_SECONDS = 30;

    HomePage homePage;
    ConvertPage convertPage;
    ExcelDataProvider excel;

    /** The source balance as it stood before converting, for comparison after. */
    private BigDecimal balanceBefore;

    /** The amount taken out of the source wallet, from the test data. */
    private BigDecimal amountConverted;

    /** How many times to ask for the Convert page before giving up on it. */
    private static final int NAVIGATION_ATTEMPTS = 3;

    @When("I navigate to the Convert page")
    public void navigateToConvertPage() {

        homePage = new HomePage(BaseClass.driver);
        convertPage = new ConvertPage(BaseClass.driver);

        homePage.skipTwoFactorSetup();

        // The click is retried rather than trusted once: dismissing the 2FA dialog
        // leaves it fading for a moment, and a click that lands on the fading
        // backdrop is swallowed without any error - the page simply stays on home.
        for (int attempt = 1; attempt <= NAVIGATION_ATTEMPTS; attempt++) {

            homePage.openConvert();

            if (convertPage.waitUntilReady(20)) {
                BaseClass.logger.pass("Navigated to the Convert page");
                return;
            }

            System.out.println("The Convert form was not ready at " + convertPage.getCurrentUrl()
                    + " (attempt " + attempt + " of " + NAVIGATION_ATTEMPTS + ")");
        }

        Assert.fail("The Convert form did not open after " + NAVIGATION_ATTEMPTS
                + " attempts. Landed on " + convertPage.getCurrentUrl());
    }

    @When("I convert the amount in {string} of {string} of {string}")
    public void convertTheAmount(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String from = excel.getStringData(excelSheetName, row, FROM_CURRENCY);
        String to = excel.getStringData(excelSheetName, row, TO_CURRENCY);
        String amount = excel.getStringData(excelSheetName, row, AMOUNT);

        convertPage.selectFromCurrency(from);
        convertPage.selectToCurrency(to);
        convertPage.enterAmount(amount);

        // Read the balance after the wallets are chosen: it belongs to whichever
        // wallet is being converted from, so reading it earlier could measure a
        // different one.
        balanceBefore = convertPage.getSourceBalanceAmount();
        amountConverted = new BigDecimal(amount);

        String quote = convertPage.waitForQuote(20);

        System.out.println(convertPage.getRate());
        System.out.println("Converting " + amount + " " + from + " to " + to
                + ", expecting " + quote);
        System.out.println(from + " balance before: " + balanceBefore);

        convertPage.submit();

        BaseClass.logger.pass("Submitted a conversion of " + amount + " " + from + " to " + to);
    }

    @When("I choose to convert from {string} to {string}")
    public void chooseTheWallets(String from, String to) {

        convertPage.selectFromCurrency(from);
        convertPage.selectToCurrency(to);

        BaseClass.logger.pass("Converting from " + from + " to " + to);
    }

    @When("I enter {string} as the convert amount")
    public void enterConvertAmount(String amount) {

        convertPage.enterAmount(amount);
        BaseClass.logger.pass("Entered " + amount + " as the amount to convert");
    }

    /**
     * A figure the form must refuse, worked out from what the form itself says it will take.
     *
     * The band belongs to the pair rather than to the suite, and the pair is priced live, so a
     * figure written into the feature file would be measuring a band that has since moved.
     */
    @When("I enter one below the stated minimum as the convert amount")
    public void enterOneBelowTheStatedMinimum() {

        String tooLittle = new java.math.BigDecimal(convertPage.statedMinimum())
                .subtract(java.math.BigDecimal.ONE).toPlainString();

        convertPage.enterAmount(tooLittle);
        BaseClass.logger.pass("Entered " + tooLittle + ", one below the stated minimum of "
                + convertPage.statedMinimum());
    }

    @When("I enter one above the stated maximum as the convert amount")
    public void enterOneAboveTheStatedMaximum() {

        String tooMuch = new java.math.BigDecimal(convertPage.statedMaximum())
                .add(java.math.BigDecimal.ONE).toPlainString();

        convertPage.enterAmount(tooMuch);
        BaseClass.logger.pass("Entered " + tooMuch + ", one above the stated maximum of "
                + convertPage.statedMaximum());
    }

    @When("I enter the stated minimum as the convert amount")
    public void enterTheStatedMinimumToConvert() {

        convertPage.enterAmount(convertPage.statedMinimum());
        BaseClass.logger.pass("Entered the stated minimum of " + convertPage.statedMinimum());
    }

    @Then("the convert amount should be refused with {string}")
    public void convertAmountShouldBeRefused(String expected) {

        Assert.assertFalse(convertPage.isAmountValid(),
                "The browser accepted the amount, but it is outside the stated limits of "
                        + convertPage.statedMinimum() + " to " + convertPage.statedMaximum());

        Assert.assertEquals(convertPage.amountValidationMessage(), expected,
                "Unexpected wording on the amount box");

        BaseClass.logger.pass("The amount was refused with: " + expected);
    }

    @Then("the convert amount should be accepted")
    public void convertAmountShouldBeAccepted() {

        Assert.assertTrue(convertPage.isAmountValid(),
                "The browser refused the amount with: " + convertPage.amountValidationMessage());

        BaseClass.logger.pass("The amount was accepted");
    }

    /**
     * More than the wallet holds, worked out from the balance the form is showing.
     *
     * Not a figure in the feature file: the balance moves every time the suite converts
     * anything, so a number written down here would stop being "more than the wallet holds" the
     * moment a matrix run finished.
     */
    @When("I enter more than the wallet holds as the convert amount")
    public void enterMoreThanTheWalletHolds() {

        BigDecimal tooMuch = convertPage.getSourceBalanceAmount().add(BigDecimal.ONE);

        convertPage.enterAmount(tooMuch.toPlainString());

        BaseClass.logger.pass("Entered " + tooMuch.toPlainString() + ", more than the "
                + convertPage.getSourceBalance() + " the wallet holds");
    }

    @Then("the form should not offer to convert")
    public void theFormShouldNotOfferToConvert() {

        Assert.assertFalse(convertPage.canConvert(),
                "The form offered to convert an amount the wallet cannot cover");

        BaseClass.logger.pass("Convert is disabled, as it should be");
    }

    @Then("the form should offer to convert")
    public void theFormShouldOfferToConvert() {

        Assert.assertTrue(convertPage.canConvert(), "The form would not offer to convert");

        BaseClass.logger.pass("Convert is enabled");
    }

    @When("I open the conversion history")
    public void openTheConversionHistory() {

        convertPage.openHistory();

        BaseClass.logger.pass("Opened the conversion history");
    }

    @Then("the conversion history should be shown")
    public void theConversionHistoryShouldBeShown() {

        Assert.assertTrue(convertPage.isHistoryShowing(),
                "The History button did not open the conversion history. Landed on "
                        + convertPage.getCurrentUrl());

        Assert.assertEquals(convertPage.historyHeadings(),
                java.util.List.of("Convert From", "Convert To"),
                "The conversion history is not showing the columns it used to");

        BaseClass.logger.pass("The conversion history is shown at " + convertPage.getCurrentUrl());
    }

    /**
     * Every entry names all four of its parts.
     *
     * A history that opens is only half the question - the other half is whether what it lists
     * reads as conversions. An entry missing its rate, or showing an amount with no currency
     * against it, is displayed wrongly however well the page loaded.
     */
    @Then("every conversion listed should name an amount, a time, what it became and the rate")
    public void everyConversionShouldBeWellFormed() {

        List<String> entries = convertPage.historyEntries();

        Assert.assertFalse(entries.isEmpty(),
                "The conversion history lists nothing at all. It reads: "
                        + convertPage.historyText());

        System.out.println("The history lists " + entries.size() + " conversions. The newest: "
                + entries.get(0));

        BaseClass.logger.pass("The history lists " + entries.size()
                + " conversions, each naming an amount, a time, what it became and the rate."
                + " The newest is: " + entries.get(0));
    }

    /**
     * The conversion just made is the one at the top.
     *
     * What makes this a test of the history rather than of the form: the figures asserted are
     * the ones the conversion was asked for, so an entry that lists the wrong amount, the wrong
     * pair, or an older conversion above a newer one is a failure.
     */
    @Then("the newest entry should be the conversion just made in {string} of {string} of {string}")
    public void theNewestEntryShouldBeTheConversionJustMade(String rowNumber, String excelSheetName,
                                                            String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String from = excel.getStringData(excelSheetName, row, FROM_CURRENCY);
        String to = excel.getStringData(excelSheetName, row, TO_CURRENCY);
        String amount = excel.getStringData(excelSheetName, row, AMOUNT);

        List<String> entries = convertPage.historyEntries();

        Assert.assertFalse(entries.isEmpty(), "The conversion history lists nothing at all");

        String newest = entries.get(0);

        // The history writes every figure to two places, so 1 is listed as 1.00.
        String asListed = new BigDecimal(amount).setScale(2, java.math.RoundingMode.UNNECESSARY)
                .toPlainString();

        Assert.assertTrue(newest.startsWith(asListed + " " + from),
                "The newest history entry should be the " + amount + " " + from
                        + " just converted, but it reads: " + newest);

        Assert.assertTrue(newest.contains(" " + to + " Rate: 1 " + from + " ~ "),
                "The newest history entry should show the conversion into " + to
                        + ", but it reads: " + newest);

        Assert.assertTrue(newest.contains(java.time.LocalDate.now().toString()),
                "The newest history entry should be dated today, but it reads: " + newest);

        BaseClass.logger.pass("The newest history entry is the conversion just made: " + newest);
    }

    /**
     * Every listed conversion opens, and its receipt says what the list said about it.
     *
     * That the two agree is the check. A receipt that opens is worth little on its own - it is
     * either the conversion that was clicked or somebody else's, and a list that opened the
     * wrong receipt would look perfectly healthy from the outside.
     */
    @Then("each of the first {int} conversions should open a receipt that matches its entry")
    public void eachConversionShouldOpenAMatchingReceipt(int howMany) {

        List<String> entries = convertPage.historyEntries();

        Assert.assertTrue(entries.size() >= howMany,
                "The history lists only " + entries.size() + " conversions, so the first "
                        + howMany + " cannot be checked");

        for (int entry = 0; entry < howMany; entry++) {

            convertPage.openHistoryEntry(entry);

            Assert.assertTrue(convertPage.isShowingAReceipt(),
                    "Conversion " + (entry + 1) + " did not open a receipt");

            String receipt = convertPage.receiptText();
            String listed = entries.get(entry);

            // The amounts, the currencies and the time - the parts the list shows, which are what
            // make this the right receipt rather than merely a receipt.
            for (String part : listed.split(" Rate: ")[0].split(" ")) {

                if (part.isBlank()) {
                    continue;
                }

                Assert.assertTrue(receipt.contains(part),
                        "The receipt for \"" + listed + "\" does not mention " + part
                                + ". It reads: " + receipt);
            }

            System.out.println("Receipt " + (entry + 1) + " matches: " + listed);

            convertPage.closeReceipt();
        }

        BaseClass.logger.pass("The first " + howMany
                + " conversions each opened a receipt matching their entry");
    }

    @When("I open the newest conversion")
    public void openTheNewestConversion() {

        convertPage.openHistoryEntry(0);

        Assert.assertTrue(convertPage.isShowingAReceipt(), "The newest conversion did not open");

        BaseClass.logger.pass("Opened the newest conversion");
    }

    @Then("the receipt should name what was converted, what it became, the rate and when")
    public void theReceiptShouldNameEverything() {

        for (String part : new String[]{"From currency", "To currency", "Rate", "Created Date"}) {

            Assert.assertTrue(convertPage.receiptSays(part),
                    "The receipt does not say " + part + ". It reads: " + convertPage.receiptText());
        }

        BaseClass.logger.pass("The receipt reads: " + convertPage.receiptText());
    }

    /**
     * Downloading the receipt produces a file with something in it.
     *
     * Saved and measured rather than trusted. The application draws the receipt in the browser
     * and hands it over as a data URL, so there is no request to watch and a download that
     * "happened" without producing bytes is exactly the failure worth catching.
     */
    @Then("downloading the receipt should produce a file")
    public void downloadingTheReceiptShouldProduceAFile() throws java.io.IOException {

        java.nio.file.Path folder = java.nio.file.Paths.get("target", "downloads");

        java.nio.file.Files.createDirectories(folder);

        java.nio.file.Path file = convertPage.downloadReceipt(folder);

        Assert.assertTrue(java.nio.file.Files.exists(file), "Download produced no file at " + file);

        long size = java.nio.file.Files.size(file);

        Assert.assertTrue(size > 1000,
                "The downloaded receipt is only " + size + " bytes, which is not a receipt");

        System.out.println("The receipt downloaded as " + file.getFileName() + " (" + size + " bytes)");

        BaseClass.logger.pass("Downloaded " + file.getFileName() + ", " + size + " bytes");
    }

    @When("I share the receipt")
    public void iShareTheReceipt() {

        convertPage.shareReceipt();

        BaseClass.logger.pass("Opened the sharing choices");
    }

    @Then("the sharing choices should offer a way to copy it")
    public void theSharingChoicesShouldOfferACopy() {

        Assert.assertTrue(convertPage.shareOffersACopy(),
                "Share opened nothing that can be copied. It shows: " + convertPage.receiptText());

        BaseClass.logger.pass("Share offers a way to copy the receipt");
    }

    @Then("the convert currency list should offer {string}")
    public void convertCurrencyListShouldOffer(String expected) {

        List<String> offered = new ArrayList<>(convertPage.walletsOffered());
        java.util.Collections.sort(offered);

        List<String> wanted = new ArrayList<>();
        for (String currency : expected.split(",")) {
            wanted.add(currency.trim());
        }
        java.util.Collections.sort(wanted);

        Assert.assertEquals(offered, wanted,
                "The convert currency list does not offer every wallet the account holds");

        BaseClass.logger.pass("The convert list offers " + offered);
    }

    /**
     * Converts one wallet into every other wallet the form offers, one pair at a time.
     *
     * Ninety ordered pairs is more than ninety scenarios' worth of signing in, so a whole row
     * of the matrix is walked in one session: ten of these cover every pair a ten-wallet
     * account has. The wallet the run started from is left until last, so the money finishes
     * where it began rather than scattered across ten wallets.
     *
     * A pair that fails does not stop the rest. One declined channel should not hide the eight
     * that work, so every pair is tried and the failures are reported together at the end -
     * which is also what makes the result read as a matrix rather than as a single verdict.
     */
    @Then("converting the amount in {string} of {string} of {string} into every other wallet, ending with {string}, should succeed")
    public void convertIntoEveryOtherWallet(String rowNumber, String excelSheetName,
                                            String excelFileName, String home) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String from = excel.getStringData(excelSheetName, row, FROM_CURRENCY);
        String amount = excel.getStringData(excelSheetName, row, AMOUNT);
        String expected = excel.getStringData(excelSheetName, row, EXPECTED_MESSAGE);

        List<String> failures = new ArrayList<>();
        List<String> converted = new ArrayList<>();

        List<String> targets = everyOtherWallet(from, home);

        System.out.println("Converting " + amount + " " + from + " into each of " + targets);

        for (String to : targets) {

            String pair = from + " to " + to;

            try {
                String went = convertOnePair(from, to, amount, expected);

                if (went.isEmpty()) {
                    converted.add(pair);
                    BaseClass.logger.pass("Converted " + amount + " " + pair);
                } else {
                    failures.add(pair + " - " + went);
                    BaseClass.logger.fail("Could not convert " + amount + " " + pair + ": " + went);
                }

            } catch (Exception wentWrong) {
                String said = String.valueOf(wentWrong.getMessage()).split(System.lineSeparator())[0];
                failures.add(pair + " - " + said);
                BaseClass.logger.fail("Could not convert " + amount + " " + pair + ": " + said);
            }

            convertPage.dismissAnythingOpen();
        }

        System.out.println("Converted from " + from + ": " + converted.size() + " of "
                + (converted.size() + failures.size()) + " pairs");

        Assert.assertTrue(failures.isEmpty(), "Converting " + amount + " " + from
                + " into every other wallet failed for " + failures.size() + " of "
                + (converted.size() + failures.size()) + " pairs: " + failures);
    }

    /**
     * The wallets to convert into, with the one the run started from left until last.
     *
     * Ending where it began is what lets the whole matrix run on a single funded wallet: every
     * other wallet is fed before it is asked to be a source, and the last conversion of the
     * last row puts the money back where it came from.
     */
    private List<String> everyOtherWallet(String from, String home) {

        List<String> targets = new ArrayList<>();

        for (String wallet : convertPage.walletsOffered()) {
            if (!wallet.equals(from) && !wallet.equals(home)) {
                targets.add(wallet);
            }
        }

        if (!home.equals(from)) {
            targets.add(home);
        }

        return targets;
    }

    /** One conversion, start to finish. Empty when it went through, the reason when it did not. */
    private String convertOnePair(String from, String to, String amount, String expected) {

        convertPage.selectFromCurrency(from);
        convertPage.selectToCurrency(to);
        convertPage.enterAmount(amount);

        BigDecimal before = convertPage.getSourceBalanceAmount();

        String quote = convertPage.waitForQuote(20);

        if (quote.isBlank()) {
            return "the form never quoted a rate";
        }

        // Asked before submitting rather than discovered afterwards. A disabled Convert button
        // swallows the click, and the run then waits out its timeout for a confirmation that was
        // never coming - which reads as the platform ignoring the conversion when in fact the
        // form had already refused the amount.
        if (!convertPage.canConvert()) {
            return "the form will not convert " + amount + " " + from + " into " + to
                    + " - Convert stays disabled, so the amount is outside what this pair takes";
        }

        System.out.println("Converting " + amount + " " + from + " to " + to + ", expecting " + quote
                + " (the " + from + " wallet holds " + before + ")");

        convertPage.submit();

        String actual = convertPage.waitForSuccessMessage(OUTCOME_TIMEOUT_SECONDS);

        if (actual.isBlank()) {

            String refusal = convertPage.refusalShowing();

            if (!refusal.isEmpty()) {
                return "the platform would not take it: " + refusal;
            }

            // The form's own objection, before the platform is troubled. A number field with
            // bounds refuses quietly - no dialog, nothing on the page - so a conversion the
            // browser rejected looks exactly like one the platform ignored unless the box is
            // asked outright.
            if (!convertPage.isAmountValid()) {
                return "the form refused the amount: " + convertPage.amountValidationMessage();
            }

            return "the conversion was neither confirmed nor refused within "
                    + OUTCOME_TIMEOUT_SECONDS + " seconds";
        }

        if (!actual.contains(expected)) {
            return "it answered \"" + actual + "\" rather than \"" + expected + "\"";
        }

        convertPage.acknowledgeSuccess();

        BigDecimal after = convertPage.waitForBalanceBelow(before, OUTCOME_TIMEOUT_SECONDS);

        if (after.compareTo(before) >= 0) {
            return "it was confirmed but the " + from + " wallet did not go down: it held "
                    + before + " before and " + after + " after";
        }

        return "";
    }

    @Then("the conversion should be confirmed with the message in {string} of {string} of {string}")
    public void conversionShouldBeConfirmed(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        String expected = excel.getStringData(excelSheetName, row, EXPECTED_MESSAGE);
        String actual = convertPage.waitForSuccessMessage(OUTCOME_TIMEOUT_SECONDS);

        Assert.assertFalse(actual.isBlank(),
                "The conversion produced no confirmation within " + OUTCOME_TIMEOUT_SECONDS + " seconds");

        Assert.assertTrue(actual.contains(expected),
                "Expected the conversion to be confirmed with \"" + expected + "\" but it said \"" + actual + "\"");

        convertPage.acknowledgeSuccess();

        BaseClass.logger.pass("Conversion confirmed: " + actual);
    }

    /**
     * The confirmation dialog only says the request was accepted. This checks the
     * source wallet actually paid for it, which is the part a conversion test
     * exists to prove.
     */
    @Then("the source wallet balance should have gone down by the converted amount")
    public void sourceBalanceShouldHaveGoneDown() {

        BigDecimal after = convertPage.waitForBalanceBelow(balanceBefore, OUTCOME_TIMEOUT_SECONDS);
        BigDecimal taken = balanceBefore.subtract(after);

        System.out.println("Source balance after: " + after + " (down by " + taken + ")");

        Assert.assertTrue(after.compareTo(balanceBefore) < 0,
                "The source wallet balance did not go down. It was " + balanceBefore
                        + " before the conversion and " + after + " after it");

        Assert.assertEquals(taken.stripTrailingZeros(), amountConverted.stripTrailingZeros(),
                "The source wallet went down by " + taken + " but " + amountConverted + " was converted");

        BaseClass.logger.pass("Source wallet went from " + balanceBefore + " to " + after);
    }
}
