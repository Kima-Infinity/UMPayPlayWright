Feature: Global Transfer
  As a UMPay user
  I want to perform a global transfer
  So that I can send money internationally

  @Global_Transfer_TC_001
  Scenario Outline: Successful Global Transfer for Outside China existing template
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to Global Transfer page
    Then I should be able to initiate a global transfer for existing template using "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName       | excelSheetName | row |
      | GlobalTransfer_TestData.xlsx | sheet1 | 1  |

  @Global_Transfer_TC_002
  Scenario Outline: Successful Global Transfer for Outside China new Receiver
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to Global Transfer page
    Then I should be able to initiate a global transfer for new receiver account using "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName       | excelSheetName | row |
      | GlobalTransfer_TestData.xlsx | sheet1 | 2  |

  @Global_Transfer_TC_003
  Scenario Outline: Successful Global Transfer for within China existing template
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to Domestic Transfer page
    Then I should be able to initiate a global transfer for existing template using "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName       | excelSheetName | row |
      | GlobalTransfer_TestData.xlsx | sheet1 | 3  |

  @Global_Transfer_TC_004
  Scenario Outline: Successful Global Transfer for within China new Receiver
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to Domestic Transfer page
    Then I should be able to initiate a global transfer for new receiver account using "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName       | excelSheetName | row |
      | GlobalTransfer_TestData.xlsx | sheet1 | 4  |

  # ------------------------------------------------------------------
  # The area itself, and what it offers
  # ------------------------------------------------------------------
  #
  # These came from Transfer.feature, which now covers the Transfer page alone. Depth for the
  # Global Transfer area belongs here, beside the end to end transfers above: the routes it
  # offers, the UnionPay form as this area serves it, and the two payout routes with the
  # negatives that prove their limits without spending anything.
  #
  # The payout accounts these use are in GlobalTransfer_TestData.xlsx, in a column of their own.
  # Rows 5 and 6 name the accounts the two payout routes pay; row 7 is for the scenarios that
  # only look.

  @transfer @Global_Transfer_TC_005
  Scenario Outline: Global Transfer offers the transfer options outside China
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Global Transfer area
    Then the Global Transfer area should offer the route "UMPay Transfer to School Fees"
    And the Global Transfer area should offer the route "UMPay Transfer to Personal Bank Account"
    And the Global Transfer area should offer the route "UnionPay Global"
    And the Global Transfer area should offer the route "UMPay Transfer to USDT"

    Examples:
      | excelFileName                | excelSheetName | row |
      | GlobalTransfer_TestData.xlsx | sheet1                      | 7   |

  # ------------------------------------------------------------------
  # UnionPay Global
  # ------------------------------------------------------------------

  # UnionPay China and everything reachable from Domestic Transfer now live in
  # DomesticTransfer.feature - depth for one area belongs in its own file. What is left here is
  # the Global side of the same screen, which is the same form without the conversion, and
  # asserting the absence is the point.
  @transfer @Global_Transfer_TC_006
  Scenario Outline: UnionPay Global offers the same form without a converted amount
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Global Transfer area
    And I take the "UnionPay Global" route from Global Transfer
    Then the UnionPay amount form should be shown
    And the UnionPay form should state "Limit Min"
    And the UnionPay form should state "Limit Max"
    And the UnionPay form should not offer a converted amount
    And the UnionPay transfer should not be ready to continue

    Examples:
      | excelFileName                | excelSheetName | row |
      | GlobalTransfer_TestData.xlsx | sheet1                      | 7   |

  # ------------------------------------------------------------------
  # The Global Transfer payout routes
  # ------------------------------------------------------------------

  # Both payout routes are the withdraw form in another place: an amount first, and only then a
  # Receive Information section offering From Template and New Account. So they send the same
  # way - amount, saved payout account, Confirm, PIN - and the PIN handling is shared with the
  # withdraw rather than written again.
  #
  # The amount is the smallest the form states, which is a conversion that moves - it read
  # 19.77 USD the day this was written - so it is read off the form rather than written here.

  @transfer @sends @Global_Transfer_TC_007
  Scenario Outline: A personal bank payout is sent
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Global Transfer area
    And I take the "UMPay Transfer to Personal Bank Account" route from Global Transfer
    Then the "To Personal Bank Account" form should be shown
    And the payout form should ask for a currency and an amount
    And the payout form should state "Limit Min"
    And the payout should not be ready to confirm
    When I enter the stated minimum as the payout amount
    And I choose the saved payout account in "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I confirm the payout, which moves real money
    Then the payout should go through

    Examples:
      | excelFileName                | excelSheetName | row |
      | GlobalTransfer_TestData.xlsx | sheet1                      | 6   |

  @transfer @sends @Global_Transfer_TC_008
  Scenario Outline: A USDT payout is sent
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Global Transfer area
    And I take the "UMPay Transfer to USDT" route from Global Transfer
    Then the "To USDT" form should be shown
    And the payout form should ask for a currency and an amount
    And the payout form should state "Limit Min"
    And the payout should not be ready to confirm
    When I enter the stated minimum as the payout amount
    And I choose the saved payout account in "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I confirm the payout, which moves real money
    Then the payout should go through

    Examples:
      | excelFileName                | excelSheetName | row |
      | GlobalTransfer_TestData.xlsx | sheet1                      | 5   |

  # The same two routes, refused. Below the stated minimum the form will not offer to confirm at
  # all, so nothing is sent and nothing has to be undone - which is what makes this the right
  # negative to have beside a scenario that spends money.

  # The same two routes, refused. Below the stated minimum the form will not offer to confirm at
  # all, so nothing is sent and nothing has to be undone - which is what makes this the right
  # negative to have beside a scenario that spends money.

  @transfer @negative @Global_Transfer_TC_009
  Scenario Outline: A payout below the stated minimum cannot be confirmed
    Given I log into the UMPay application with valid email credentials using "1" of "sheet1" of "GlobalTransfer_TestData.xlsx"
    When I open the Global Transfer area
    And I take the "<route>" route from Global Transfer
    Then the "<form>" form should be shown
    When I enter one below the stated minimum as the payout amount
    Then the payout should not be ready to confirm

    Examples:
      | route                                   | form                     |
      | UMPay Transfer to Personal Bank Account | To Personal Bank Account |
      | UMPay Transfer to USDT                  | To USDT                  |

  # School fees have a file of their own - InternationalSchoolFees.feature - which covers the
  # form from both ways in, what it states, the fee it works out, and both ends of what it will
  # take. The route is still asserted above, where this file lists what Global Transfer offers.

  # The other way a payout is refused: inside the band, but more than the wallet holds. Worth
  # having beside the sending scenarios because it is the failure a real user meets most often,
  # and because it costs nothing to prove.
  @transfer @negative @Global_Transfer_TC_010
  Scenario Outline: A payout of more than the wallet holds cannot be confirmed
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Global Transfer area
    And I take the "UMPay Transfer to USDT" route from Global Transfer
    Then the "To USDT" form should be shown
    When I enter more than the wallet holds as the payout amount
    Then the payout should not be ready to confirm

    Examples:
      | excelFileName                | excelSheetName | row |
      | GlobalTransfer_TestData.xlsx | sheet1                      | 7   |

  # The school fee route, asserted where it starts. The form it opens is covered in full by
  # InternationalSchoolFees.feature; what this proves is that Global Transfer still reaches it,
  # which is broken for whoever uses this route however well the sidebar's own entry works.
  @schoolfee @International_School_Fee_TC_012
  Scenario Outline: Global Transfer reaches the same form
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Global Transfer area
    And I take the "UMPay Transfer to School Fees" route from Global Transfer
    Then the school fee form should be shown
    And the school fee form should show the sending wallet and its balance
    And the school fee form should ask for an amount and a remark

    Examples:
      | excelFileName                         | excelSheetName | row |
      | InternationalSchoolFees_TestData.xlsx | sheet1         | 2   |
