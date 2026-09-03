Feature: Global Transfer
  As a UMPay user
  I want to perform a global transfer
  So that I can send money internationally

  Scenario Outline: Successful Global Transfer for Outside China existing template
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to Global Transfer page
    Then I should be able to initiate a global transfer for existing template using "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName       | excelSheetName | row |
      | GlobalTransfer_TestData.xlsx | sheet1 | 1  |

  Scenario Outline: Successful Global Transfer for Outside China new Receiver
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to Global Transfer page
    Then I should be able to initiate a global transfer for new receiver account using "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName       | excelSheetName | row |
      | GlobalTransfer_TestData.xlsx | sheet1 | 2  |

  Scenario Outline: Successful Global Transfer for within China existing template
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to Domestic Transfer page
    Then I should be able to initiate a global transfer for existing template using "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName       | excelSheetName | row |
      | GlobalTransfer_TestData.xlsx | sheet1 | 3  |

  Scenario Outline: Successful Global Transfer for within China new Receiver
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to Domestic Transfer page
    Then I should be able to initiate a global transfer for new receiver account using "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName       | excelSheetName | row |
      | GlobalTransfer_TestData.xlsx | sheet1 | 4  |
