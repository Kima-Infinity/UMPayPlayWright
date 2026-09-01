Feature: Deposit
  As a UMPay user
  I want to perform a deposit transaction
  So that I can send money internationally

  Scenario Outline: Successful Deposit Transaction
    Given I log into the UMPay application with valid credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to Deposit page
    Then I should be able to initiate a deposit transaction using "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName       | excelSheetName | row |
      |Deposit_TestData.xlsx | sheet1 | 1  |

