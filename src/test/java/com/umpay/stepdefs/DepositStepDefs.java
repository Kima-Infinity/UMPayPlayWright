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

public class DepositStepDefs {

    HomePage homePage;
    DepositPage depositPage;
    ExcelDataProvider excel;

    @When("I navigate to Deposit page")
    public void i_navigate_to_deposit_page() throws InterruptedException {

        homePage = new HomePage(BaseClass.driver);
        depositPage = new DepositPage(BaseClass.driver);

        Thread.sleep(3000);

        homePage.dismissTwoFactorPromptIfShowing();

        homePage.openDeposit();

        System.out.println("Navigated to Deposit Page Successfully!");
        BaseClass.logger.pass("Navigated to Deposit page");
    }


    @When("I choose the {string} wallet on the Deposit page")
    public void chooseDepositCurrency(String currency) {

        depositPage.chooseCurrency(currency);

        BaseClass.logger.pass("Chose the " + currency + " wallet");
    }

    @When("I enter {string} as the deposit amount")
    public void enterDepositAmount(String amount) {

        depositPage.enterAmount(amount);

        BaseClass.logger.pass("Entered " + amount + " as the deposit amount");
    }

    @Then("the deposit amount should be refused with {string}")
    public void depositAmountShouldBeRefused(String expected) {

        org.testng.Assert.assertFalse(depositPage.isAmountValid(),
                "The browser accepted the amount, but it is outside the stated limits");

        org.testng.Assert.assertEquals(depositPage.amountValidationMessage(), expected,
                "Unexpected wording on the amount box");

        BaseClass.logger.pass("The amount was refused with: " + expected);
    }

    @Then("the deposit amount should be accepted")
    public void depositAmountShouldBeAccepted() {

        org.testng.Assert.assertTrue(depositPage.isAmountValid(),
                "The browser refused the amount with: " + depositPage.amountValidationMessage());

        BaseClass.logger.pass("The amount was accepted");
    }

    @Then("the deposit form should state a minimum of {string} and a maximum of {string}")
    public void depositFormShouldStateLimits(String minimum, String maximum) {

        org.testng.Assert.assertEquals(depositPage.statedMinimum(), minimum,
                "The amount box does not enforce the expected minimum");

        org.testng.Assert.assertEquals(depositPage.statedMaximum(), maximum,
                "The amount box does not enforce the expected maximum");

        BaseClass.logger.pass("The amount box enforces " + minimum + " to " + maximum);
    }

    @Then("the deposit currency list should offer {string}")
    public void depositCurrencyListShouldOffer(String expected) {

        java.util.List<String> offered = new java.util.ArrayList<>(depositPage.walletBalances().keySet());
        java.util.Collections.sort(offered);

        java.util.List<String> wanted = new java.util.ArrayList<>();
        for (String currency : expected.split(",")) {
            wanted.add(currency.trim());
        }
        java.util.Collections.sort(wanted);

        org.testng.Assert.assertEquals(offered, wanted,
                "The deposit currency list does not offer what it used to");

        BaseClass.logger.pass("The deposit list offers " + offered);
    }

    @Then("I should be able to initiate a deposit transaction using {string} of {string} of {string}")
    public void submitDepositTransaction(String rowNumber, String excelSheetName, String excelFileName) {

        int row = Integer.parseInt(rowNumber);
        excel = new ExcelDataProvider(excelFileName, excelSheetName);

        depositPage.submitDeposit(excel.getStringData(excelSheetName, row, 3),
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
            BaseClass.logger.pass("Initiated Deposit transaction successfully");
        }
    }
}
