Feature: Fee Listing

  As a UMPay user
  I want to see what UMPay charges in each currency
  So that I know what a transfer will cost me before I make it

  # WHAT THIS FILE COVERS
  #
  # The fees at /customer/fee, reached from the profile drawer. A chooser of the currencies the
  # platform deals in - BDT, BRL, HKD, IDR, MXN, MYR, PHP, THB, USD, USDT, VND - and, in
  # principle, what it costs to move money in whichever is chosen. The page asks the server for
  # /api/agents/fees as it loads.
  #
  # HOW A CURRENCY IS CHOSEN
  #
  # By the button carrying its name. Each row also holds a radio, and the radio is decorative: it
  # sits behind the row with a negative z-index and carries neither a name nor a value, so it can
  # be neither clicked nor checked - Playwright answers "clicking the checkbox did not change its
  # state". Anything testing this page has to press the button, which is what the scenarios below
  # do.
  #
  # WHY NOTHING HERE IS DANGEROUS
  #
  # A fee schedule is the platform's to set. This page only shows it: there is no form, nothing to
  # submit and nothing that can be changed from here.
  #
  # THE DATA
  #
  # TestData/FeeListing_TestData.xlsx carries the account and the currency to ask the fees for.

  @feelisting @Fee_Listing_TC_001
  Scenario Outline: The profile drawer opens the fee listing
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Fee Listing
    Then the fee listing should be shown

    Examples:
      | excelFileName           | excelSheetName | row |
      | FeeListing_TestData.xlsx | Sheet1        | 1   |

  # A currency missing from here is one nobody can find out the cost of before they send money in
  # it.
  @feelisting @Fee_Listing_TC_002
  Scenario Outline: Every currency the platform deals in can be asked about
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Fee Listing
    Then the currencies offered should be "BDT, BRL, HKD, IDR, MXN, MYR, PHP, THB, USD, USDT, VND"

    Examples:
      | excelFileName           | excelSheetName | row |
      | FeeListing_TestData.xlsx | Sheet1        | 1   |

  # The same currency listed twice would leave somebody pressing one of them and wondering why the
  # answer differs from the other.
  @feelisting @Fee_Listing_TC_003
  Scenario Outline: Each currency is offered exactly once
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Fee Listing
    Then each currency should be offered exactly once

    Examples:
      | excelFileName           | excelSheetName | row |
      | FeeListing_TestData.xlsx | Sheet1        | 1   |

  @feelisting @Fee_Listing_TC_004
  Scenario Outline: The page asks which currency the fees are wanted for
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Fee Listing
    Then it should ask which currency the fees are wanted for

    Examples:
      | excelFileName           | excelSheetName | row |
      | FeeListing_TestData.xlsx | Sheet1        | 1   |

  # Pressing a currency and being shown no sign that it took leaves somebody pressing it again,
  # and again, with nothing to tell them whether the page heard them.
  @feelisting @Fee_Listing_TC_005
  Scenario Outline: The currency that has been chosen is shown as chosen
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Fee Listing
    And I ask for the fees in the currency named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then that currency should be shown as the one chosen

    Examples:
      | excelFileName           | excelSheetName | row |
      | FeeListing_TestData.xlsx | Sheet1        | 2   |

  # The whole point of the page. A fee listing that lists no fees tells nobody what anything costs,
  # and the cost is the one thing somebody comes here to find out.
  @feelisting @Fee_Listing_TC_006
  Scenario Outline: Choosing a currency shows what it costs to move money in it
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Fee Listing
    And I ask for the fees in the currency named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then it should show what that currency costs

    Examples:
      | excelFileName           | excelSheetName | row |
      | FeeListing_TestData.xlsx | Sheet1        | 2   |
