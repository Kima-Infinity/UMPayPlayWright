Feature: Convert
  As a UMPay user
  I want to convert money between my wallets
  So that I can hold a balance in the currency I need

  # Convert moves real money between the test account's wallets, so the amounts in
  # Convert_TestData.xlsx are kept small. The scenario asserts on the source
  # balance falling rather than only on the success dialog: the dialog says the
  # request was accepted, the balance says the money actually moved.

  @convert @smoke
  Scenario Outline: Money is converted between two wallets
    Given I log into the UMPay application with valid credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to the Convert page
    And I convert the amount in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the conversion should be confirmed with the message in "<row>" of "<excelSheetName>" of "<excelFileName>"
    And the source wallet balance should have gone down by the converted amount

    Examples:
      | excelFileName        | excelSheetName | row |
      | Convert_TestData.xlsx | Sheet1        | 1   |
