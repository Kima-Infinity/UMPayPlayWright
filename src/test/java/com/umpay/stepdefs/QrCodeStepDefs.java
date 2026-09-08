package com.umpay.stepdefs;

import com.umpay.pages.HomePage;
import com.umpay.pages.QrCodePage;
import com.umpay.utility.BaseClass;
import com.umpay.utility.ExcelDataProvider;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * The account's own QR code, and the scanner that reads somebody else's.
 *
 * <p>Column layout of TestData/QRCode_TestData.xlsx
 * 0 Scenario | 1 LoginID | 2 Password | 3 Amount | 4 Remark
 *
 * <p>Nothing here pays anybody. The code is drawn and read, never used to send anything, and the
 * scanner is only ever handed a picture with no code in it - a screenshot from this suite's own
 * run - so there is no destination for it to act on.
 */
public class QrCodeStepDefs {

    /** Where this suite keeps the pictures its own runs took. */
    private static final String SCREENSHOTS = "Screenshots";

    /** The pages swept for a way to the QR pages. */
    private static final String[] THE_APPLICATION = {"/", "/v2/transfer", "/deposit", "/withdraw",
            "/v2/bills", "/convert", "/v2/wallet", "/trade-record"};

    private QrCodePage qrPage;

    private ExcelDataProvider excel;

    /** What was put on the code, so the step that judges it can say so. */
    private String amountTyped = "";

    private String remarkTyped = "";

    /** The picture the scanner was handed. */
    private String pictureHandedOver = "";

    /** What the scanner said before it was handed anything. */
    private String scannerSaidBefore = "";

    /** What each page swept for a way to the QR pages had to say about them. */
    private Map<String, List<String>> whatEachPageLeadsTo = new LinkedHashMap<>();

    private QrCodePage qr() {

        if (qrPage == null) {
            qrPage = new QrCodePage(BaseClass.driver);
        }

        return qrPage;
    }

    private String fromTheSheet(String rowNumber, String sheetName, String fileName, String columnName) {

        excel = new ExcelDataProvider(fileName, sheetName);

        for (int column = 0; column < 20; column++) {

            String heading;

            try {
                heading = excel.getStringData(sheetName, 0, column);
            } catch (Exception noHeadingHere) {
                continue;
            }

            if (columnName.equalsIgnoreCase(String.valueOf(heading).trim())) {
                try {
                    return excel.getStringData(sheetName, Integer.parseInt(rowNumber), column);
                } catch (Exception blank) {
                    return "";
                }
            }
        }

        throw new IllegalStateException("The sheet " + fileName + " has no column called \""
                + columnName + "\"");
    }

    /** What the platform said, if it answered with a dialog of its own, ready to be appended. */
    private String refused() {

        String said = qr().refusalShowing();

        return said.isEmpty() ? "" : ". The platform said: \"" + said + "\"";
    }

    private void signedInAndSteady() throws InterruptedException {

        HomePage home = new HomePage(BaseClass.driver);

        Thread.sleep(3000);

        home.dismissTwoFactorPromptIfShowing();

        qr().waitUntilSignedIn(30);
    }

    @When("I open the QR code page")
    public void openTheQrCodePage() throws InterruptedException {

        signedInAndSteady();

        qr().openTheCode();

        BaseClass.logger.pass("Opened the QR code page at " + qr().getCurrentUrl());
    }

    @When("I open the scan QR code page")
    public void openTheScanPage() throws InterruptedException {

        signedInAndSteady();

        qr().openTheScanner();

        scannerSaidBefore = qr().text();

        BaseClass.logger.pass("Opened the scanner at " + qr().getCurrentUrl());
    }

    @Then("the account's QR code should be drawn")
    public void theCodeShouldBeDrawn() {

        assertTrue(qr().isShowingTheCode(),
                "The QR code page is not on the screen. The page is at " + qr().getCurrentUrl()
                        + refused());

        assertTrue(qr().codeIsDrawn(),
                "The page opened but no code is drawn on it, so there is nothing for anybody to"
                        + " scan. It reads: " + qr().text());

        System.out.println("The code is drawn " + qr().sizeOfTheCode() + " at "
                + qr().getCurrentUrl());

        BaseClass.logger.pass("The account's QR code is drawn, " + qr().sizeOfTheCode());
    }

    @Then("it should say which wallet the code collects into")
    public void itShouldSayWhichWallet() {

        String wallet = qr().walletChosen();

        assertFalse(wallet.isEmpty(),
                "The page does not say which wallet the code collects into, so whoever scans it"
                        + " cannot know which money they are sending. It reads: " + qr().text());

        System.out.println("The code collects into " + wallet);

        BaseClass.logger.pass("The code collects into " + wallet);
    }

    @Then("it should ask for an amount and a remark to put on the code")
    public void itShouldAskForBoth() {

        assertTrue(qr().asksForAnAmount(),
                "The page offers no amount to put on the code, so every scan would have to be"
                        + " typed in by hand at the other end");

        assertTrue(qr().asksForARemark(),
                "The page offers no remark to put on the code, so nothing can say what a payment"
                        + " was for");

        System.out.println("The code takes an amount (" + qr().amountInvites() + ") and a remark");

        BaseClass.logger.pass("The code takes an amount and a remark");
    }

    @When("I put the amount and the remark from {string} of {string} of {string} on the code")
    public void putTheAmountAndRemarkOn(String rowNumber, String sheetName, String fileName) {

        amountTyped = fromTheSheet(rowNumber, sheetName, fileName, "Amount");
        remarkTyped = fromTheSheet(rowNumber, sheetName, fileName, "Remark");

        assertFalse(amountTyped.trim().isEmpty(),
                "Row " + rowNumber + " of " + fileName + " names no amount to put on the code");

        qr().typeTheAmount(amountTyped);
        qr().typeTheRemark(remarkTyped);

        BaseClass.logger.pass("Put " + amountTyped + " and \"" + remarkTyped + "\" on the code");
    }

    @Then("the code should still be drawn, carrying what was typed")
    public void theCodeShouldStillBeDrawn() {

        assertTrue(qr().codeIsDrawn(),
                "The code went off the page once an amount was put on it, so nobody could be paid"
                        + " from it: " + amountTyped + ", \"" + remarkTyped + "\"" + refused());

        assertEquals(qr().amountOnTheCode(), amountTyped,
                "The amount on the code reads " + qr().amountOnTheCode() + " rather than the "
                        + amountTyped + " that was typed");

        assertEquals(qr().remarkOnTheCode(), remarkTyped,
                "The remark on the code reads \"" + qr().remarkOnTheCode() + "\" rather than the \""
                        + remarkTyped + "\" that was typed");

        System.out.println("The code is still drawn " + qr().sizeOfTheCode() + " with "
                + amountTyped + " and \"" + remarkTyped + "\" on it");

        BaseClass.logger.pass("The code is still drawn with " + amountTyped + " and \""
                + remarkTyped + "\" on it");
    }

    @When("I open the wallets the code can be drawn for")
    public void openTheWallets() {

        qr().openTheWallets();

        BaseClass.logger.pass("Opened the wallets the code can be drawn for");
    }

    @Then("every wallet the account holds should be offered")
    public void everyWalletShouldBeOffered() {

        assertTrue(qr().isShowingTheWallets(),
                "The wallets did not open, so the code can only ever collect into the one it"
                        + " opened on. The page is at " + qr().getCurrentUrl() + refused());

        List<String> wallets = qr().walletsOffered();

        assertFalse(wallets.isEmpty(),
                "The wallets opened but list nothing at all");

        assertTrue(wallets.size() > 1,
                "Only " + wallets + " is offered, so an account with more than one wallet can only"
                        + " be paid into that one");

        System.out.println("The code can be drawn for " + wallets.size() + " wallets: " + wallets);

        BaseClass.logger.pass("The code can be drawn for " + wallets.size() + " wallets: "
                + wallets);

        qr().closeTheWallets();
    }

    @Then("it should offer to download the code")
    public void itShouldOfferToDownload() {

        assertTrue(qr().offersToDownload(),
                "The page offers no way to download the code, so it cannot be printed, pinned up"
                        + " or sent to anybody. It reads: " + qr().text());

        System.out.println("The code can be downloaded");

        BaseClass.logger.pass("The code can be downloaded");
    }

    @Then("it should ask for a code to be lined up with the frame")
    public void itShouldAskToAlignACode() {

        assertTrue(qr().isShowingTheScanner(),
                "The scanner is not on the screen. The page is at " + qr().getCurrentUrl()
                        + refused());

        assertTrue(qr().isAskingToAlignACode(),
                "The scanner says nothing about lining a code up with its frame, so there is"
                        + " nothing telling anybody what to do with it. It reads: " + qr().text());

        System.out.println("The scanner asks for a code to be lined up with the frame"
                + (qr().whatItSaysAboutTheCamera().isEmpty() ? ""
                        : ", and says \"" + qr().whatItSaysAboutTheCamera() + "\" of this machine"));

        BaseClass.logger.pass("The scanner asks for a code to be lined up with the frame");
    }

    /**
     * A machine with no camera is still offered a way in.
     *
     * This run has none, and the page says so - which is exactly the case the photo exists for.
     */
    @Then("it should offer a photo to be uploaded instead")
    public void itShouldOfferAPhoto() {

        assertTrue(qr().offersToUploadAPhoto(),
                "The scanner offers no way to hand it a picture, so a machine without a camera -"
                        + " which this one is - has no way to scan anything. It reads: "
                        + qr().text());

        System.out.println("The scanner takes a photo (" + qr().picturesAccepted() + ")"
                + (qr().whatItSaysAboutTheCamera().isEmpty() ? ""
                        : ", having said \"" + qr().whatItSaysAboutTheCamera() + "\""));

        BaseClass.logger.pass("The scanner offers a photo to be uploaded, taking "
                + qr().picturesAccepted());
    }

    @When("I hand it a picture with no code in it")
    public void handItAPictureWithNoCode() {

        File[] pictures = new File(SCREENSHOTS).listFiles(
                (where, name) -> name.toLowerCase().endsWith(".png"));

        assertTrue(pictures != null && pictures.length > 0,
                "There is no picture in " + SCREENSHOTS + " to hand the scanner");

        File picture = pictures[pictures.length - 1];

        pictureHandedOver = picture.getName();

        qr().uploadThePicture(Path.of(picture.getAbsolutePath()));

        BaseClass.logger.pass("Handed the scanner " + pictureHandedOver
                + ", a screenshot from this suite's own run");
    }

    /**
     * A picture with nothing in it starts nothing.
     *
     * The scanner is one press away from paying somebody, so the thing worth being sure of is
     * that a picture it cannot read leaves the run exactly where it was.
     */
    @Then("it should not act on it")
    public void itShouldNotActOnIt() {

        assertTrue(qr().isShowingTheScanner(),
                "The scanner was handed " + pictureHandedOver + ", which has no code in it, and"
                        + " the run left the scanner for " + qr().getCurrentUrl()
                        + ", so something was read out of it" + refused());

        String saysNow = qr().text();

        System.out.println("The scanner stayed where it was after " + pictureHandedOver
                + (saysNow.equals(scannerSaidBefore)
                        ? ", saying nothing at all about it"
                        : ", and now reads: " + saysNow));

        BaseClass.logger.pass("The scanner did not act on " + pictureHandedOver
                + (saysNow.equals(scannerSaidBefore)
                        ? ", and said nothing at all about it"
                        : ", and answered: " + saysNow));
    }

    @When("I look through the application for a way to the QR pages")
    public void lookForAWayToThem() throws InterruptedException {

        signedInAndSteady();

        whatEachPageLeadsTo = new LinkedHashMap<>();

        for (String where : THE_APPLICATION) {

            qr().openTheApplicationPage(where);

            whatEachPageLeadsTo.put(where, qr().anythingLeadingToTheQrPages());
        }

        BaseClass.logger.pass("Looked through " + THE_APPLICATION.length + " pages of the"
                + " application for a way to the QR pages");
    }

    /**
     * Something in the application leads to the QR pages.
     *
     * Both pages answer their addresses and work. What this holds the application to is that
     * somebody who has not been given the address can still get to them.
     */
    @Then("something should lead to them")
    public void somethingShouldLeadToThem() {

        List<String> found = new ArrayList<>();

        for (Map.Entry<String, List<String>> page : whatEachPageLeadsTo.entrySet()) {

            for (String lead : page.getValue()) {
                found.add(page.getKey() + ": " + lead);
            }
        }

        assertFalse(found.isEmpty(),
                "Nothing in the application leads to /qr-code or /scan-qr. Swept: "
                        + whatEachPageLeadsTo.keySet() + ", as well as the top bar, the sidebar and"
                        + " the profile drawer. Both pages answer their addresses and work, so"
                        + " this is a feature that is there and cannot be reached");

        System.out.println("The QR pages are led to from " + found);

        BaseClass.logger.pass("The QR pages are led to from " + found);
    }
}
