package com.umpay.stepdefs;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import com.umpay.pages.*;
import com.umpay.utility.BaseClass;
import com.umpay.utility.ExcelDataProvider;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class WithdrawStepDefs {

    HomePage homePage;
    WithdrawPage withdrawPage;
    ExcelDataProvider excel;

    @When("I navigate to Withdraw page")
    public void i_navigate_to_withdraw_page() throws InterruptedException {

        homePage = new HomePage(BaseClass.driver);
        withdrawPage = new WithdrawPage(BaseClass.driver);

        Thread.sleep(3000);

        homePage.dismissTwoFactorPromptIfShowing();

        homePage.openWithdraw();

        System.out.println("Navigated to Withdraw Page Successfully!");
        BaseClass.logger.pass("Navigated to Withdraw page");
    }

    @Then("I should be able to initiate a withdraw transaction using {string} of {string} of {string}")
    public void submitWithdrawTransaction(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        withdrawPage.submitWithdraw(excel.getStringData(excelSheetName, row, 3),
                excel.getStringData(excelSheetName, row, 4),
                excel.getStringData(excelSheetName, row, 5),
                excel.getStringData(excelSheetName, row, 6),
                excel.getStringData(excelSheetName, row, 7),
                excel.getStringData(excelSheetName, row, 8),
                excel.getStringData(excelSheetName, row, 9),
                excel.getStringData(excelSheetName, row, 10),
                excel.getStringData(excelSheetName, row, 11),
                excel.getStringData(excelSheetName, row, 12));

        System.out.println("Test Completed!");
        if (BaseClass.logger != null) {
            BaseClass.logger.pass("Initiated Withdraw transaction successfully");
        }
    }

    @Then("the withdraw order status should be {string}")
    public void withdrawOrderStatusShouldBe(String expectedStatus) {

        String actualStatus = withdrawPage.getSubmittedOrderStatus();

        org.testng.Assert.assertEquals(actualStatus, expectedStatus,
                "Unexpected status on the submitted withdraw order");

        BaseClass.logger.pass("Withdraw order status is " + actualStatus);
    }
}
