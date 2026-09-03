Feature: Withdraw
  As a UMPay user
  I want to perform a withdraw transaction
  So that I can take money out of my wallet

  Scenario Outline: Successful Withdraw Transaction
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to Withdraw page
    Then I should be able to initiate a withdraw transaction using "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName         | excelSheetName | row |
      |Withdraw_TestData.xlsx | sheet1 | 1  |
