package com.umpay.stepdefs;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import com.umpay.pages.*;
import com.umpay.utility.BaseClass;
import com.umpay.utility.BrowserFactory;
import com.umpay.utility.ExcelDataProvider;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

public class GlobalTransferStepDefs {

    HomePage homePage;
    GlobalTransferPage globalTransferPage;
    ToUnionPayPage toUnionPayPage;
    ReceiverInformationPage receiverInformationPage;
    ExcelDataProvider excel;
    DomesticTransferPage domesticTransferPage;

    @When("I navigate to Global Transfer page")
    public void navigateToGlobalTransfer() throws InterruptedException {

        homePage = new HomePage(BaseClass.driver);
        globalTransferPage = new GlobalTransferPage(BaseClass.driver);
        toUnionPayPage = new ToUnionPayPage(BaseClass.driver);
        receiverInformationPage = new ReceiverInformationPage(BaseClass.driver);

        // homePage.homePage() is deliberately not called: it performs a long run of
        // diagnostic clicks that have nothing to do with reaching Global Transfer and
        // that have caused failures of their own. Dismissing the 2FA prompt and asking
        // for the area is all this step needs.

        Thread.sleep(5000);

        homePage.dismissTwoFactorPromptIfShowing();

        homePage.openGlobalTransfer();
        globalTransferPage.unionPayGlobal();
        System.out.println("Navigated to Global Transfer Page Successfully!");
        BaseClass.logger.pass("Navigated to Global Transfer page");
    }

    @When("I navigate to Domestic Transfer page")
    public void navigateToDomesticTransfer() throws InterruptedException {

        homePage = new HomePage(BaseClass.driver);
        domesticTransferPage = new DomesticTransferPage(BaseClass.driver);
        toUnionPayPage = new ToUnionPayPage(BaseClass.driver);
        receiverInformationPage = new ReceiverInformationPage(BaseClass.driver);

        // See the note in the Global Transfer step: the same diagnostic clicks are
        // skipped here for the same reason.

        Thread.sleep(5000);

        homePage.dismissTwoFactorPromptIfShowing();

        homePage.openDomesticTransfer();

        domesticTransferPage.unionPayChina();

        System.out.println("Navigated to Domestic Transfer Page Successfully!");
        BaseClass.logger.pass("Navigated to Global Transfer page");
    }

    @Then("I should be able to initiate a global transfer for existing template using {string} of {string} of {string}")
    public void initiateGlobalTransferExistingTemplate(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        toUnionPayPage.unionPayTransfer(excel.getStringData(excelSheetName, row, 3));

        receiverInformationPage.transferToExistingReceiver(excel.getStringData(excelSheetName, row, 4), excel.getStringData(excelSheetName, row, 5));

        Assert.assertTrue(receiverInformationPage.lastOrderCompleted(),
                "The global transfer did not complete. The order settled on \""
                        + receiverInformationPage.getLastOrderStatus() + "\"");

        BrowserFactory.quitBrowser(BaseClass.driver);

        System.out.println("Test Completed!");
        BaseClass.logger.pass("Initiated global transfer successfully");
    }

    @Then("I should be able to initiate a global transfer for new receiver account using {string} of {string} of {string}")
    public void initiateGlobalTransferNewReceiver(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);


        toUnionPayPage.unionPayTransfer(excel.getStringData(excelSheetName, row, 3));

        receiverInformationPage.transferToNewReceiver(excel.getStringData(excelSheetName, row, 4),
                excel.getStringData(excelSheetName, row, 6), excel.getStringData(excelSheetName, row, 7),
                excel.getStringData(excelSheetName, row, 8), excel.getStringData(excelSheetName, row, 9),
                excel.getStringData(excelSheetName, row, 10), excel.getStringData(excelSheetName, row, 5));

        Assert.assertTrue(receiverInformationPage.lastOrderCompleted(),
                "The global transfer did not complete. The order settled on \""
                        + receiverInformationPage.getLastOrderStatus() + "\"");

        BrowserFactory.quitBrowser(BaseClass.driver);

        System.out.println("Test Completed!");
        BaseClass.logger.pass("Initiated global transfer successfully");
    }
}
