Feature: Wallet

  As a UMPay user
  I want to see every wallet I hold and what is in each of them
  So that I know what I can spend, what is being held, and which wallet the product will use

  # WHAT THIS FILE COVERS
  #
  # The wallet page at /v2/wallet, reached from the profile drawer. It opens on a chart of
  # everything the account holds split by country, and below it a card for each wallet - its
  # currency, its country, what is available and what is blocked. One wallet is marked as the
  # main one; every other offers to be made it.
  #
  # A card opens that wallet's own page at /v2/wallet/detail/record?currencyCode=HKD, which
  # states the wallet three ways - Total Balance, Available Balance and Locked - and holds two
  # tabs: Record, everything that has moved in this wallet, and Locked, what is being held.
  #
  # WHAT IS WORTH KNOWING ABOUT THE FIGURES
  #
  # Locked is written as a negative - "HK$ -5,544.32" - but the total counts it as money the
  # account still has: 8,369.51 available against -5,544.32 locked makes a total of 13,913.83.
  # So the total is the available balance plus what is locked taken as a size, and that is what
  # is asserted.
  #
  # WHAT THE PAGE DOES NOT DO
  #
  # The countries above the list are a chart's legend and nothing more. One of them even draws a
  # pointer, but clicking changes neither the list nor the address - there is no filtering on
  # this page, and nothing here pretends there is. This was tried against the running platform
  # before being left out rather than assumed away.
  #
  # WHAT IT CHANGES
  #
  # Nothing here moves money. One scenario moves which wallet the account calls its main one -
  # a setting, which the platform changes without asking anything first - and puts it back where
  # it found it before it finishes. It is tagged @changesmain so a run can leave it out.
  #
  # THE DATA
  #
  # TestData/Wallet_TestData.xlsx carries the account, the wallet to open and the wallet to move
  # the main one to.

  # ------------------------------------------------------------------
  # The wallets the account holds
  # ------------------------------------------------------------------

  @wallet @Wallet_TC_001
  Scenario Outline: The profile drawer opens the account's wallets
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Wallet page
    Then the page should list the wallets the account holds

    Examples:
      | excelFileName        | excelSheetName | row |
      | Wallet_TestData.xlsx | Sheet1         | 1   |

  # A balance with no currency beside it is a number nobody can act on, and a wallet that does
  # not say what is blocked hides the part of the money the account cannot actually use.
  @wallet @Wallet_TC_002
  Scenario Outline: Every wallet names its currency, country, balance and what is blocked
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Wallet page
    Then every wallet should name its currency, country, balance and what is blocked

    Examples:
      | excelFileName        | excelSheetName | row |
      | Wallet_TestData.xlsx | Sheet1         | 1   |

  # The main wallet is the one the rest of the product reaches for first, so two of them is a
  # question nobody can answer, and none at all leaves every other flow guessing.
  @wallet @Wallet_TC_003
  Scenario Outline: Exactly one wallet is the main wallet
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Wallet page
    Then exactly one wallet should be the main wallet

    Examples:
      | excelFileName        | excelSheetName | row |
      | Wallet_TestData.xlsx | Sheet1         | 1   |

  # A reader compares a wallet's balance against what is blocked at a glance, so the two have to
  # be in the same money. Held against the card itself rather than a table of symbols in the
  # code, because which symbol the platform picked is its own business.
  @wallet @Wallet_TC_004
  Scenario Outline: Each wallet states its balance and what is blocked in its own currency
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Wallet page
    Then each wallet's figures should be written in its own currency

    Examples:
      | excelFileName        | excelSheetName | row |
      | Wallet_TestData.xlsx | Sheet1         | 1   |

  # A country charted that the account holds no wallet for would be money shown to be somewhere
  # it cannot be.
  @wallet @Wallet_TC_005
  Scenario Outline: The chart only names countries the account holds a wallet for
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Wallet page
    Then every country in the chart should be one the account holds a wallet for

    Examples:
      | excelFileName        | excelSheetName | row |
      | Wallet_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # One wallet on its own
  # ------------------------------------------------------------------

  @wallet @Wallet_TC_006
  Scenario Outline: A wallet opens its own page
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Wallet page
    And I open the wallet named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then that wallet's own page should open
    And the wallet should state its total, what is available and what is locked

    Examples:
      | excelFileName        | excelSheetName | row |
      | Wallet_TestData.xlsx | Sheet1         | 2   |

  # An account holder reads these three numbers to work out what they can spend. Three numbers
  # that do not add up leave them unable to.
  @wallet @Wallet_TC_007
  Scenario Outline: A wallet's total is what is available plus what is locked
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Wallet page
    And I open the wallet named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the wallet's total should be what is available plus what is locked

    Examples:
      | excelFileName        | excelSheetName | row |
      | Wallet_TestData.xlsx | Sheet1         | 2   |

  @wallet @Wallet_TC_008
  Scenario Outline: A wallet keeps what has moved and what is held in two tabs
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Wallet page
    And I open the wallet named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the wallet should offer the tabs "Record, Locked"

    Examples:
      | excelFileName        | excelSheetName | row |
      | Wallet_TestData.xlsx | Sheet1         | 2   |

  # A movement with no date cannot be placed against a statement, and one with no amount says
  # nothing at all.
  @wallet @Wallet_TC_009
  Scenario Outline: The Record tab lists what has moved in that wallet
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Wallet page
    And I open the wallet named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I move to the wallet's "Record" tab
    Then every movement should name what it was, when it happened and how much

    Examples:
      | excelFileName        | excelSheetName | row |
      | Wallet_TestData.xlsx | Sheet1         | 2   |

  # What is being held against the wallet is the difference between what it says it has and what
  # can actually be spent, so it has to be readable in its own right.
  @wallet @Wallet_TC_010
  Scenario Outline: The Locked tab lists what is being held against that wallet
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Wallet page
    And I open the wallet named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I move to the wallet's "Locked" tab
    Then every movement should name what it was, when it happened and how much

    Examples:
      | excelFileName        | excelSheetName | row |
      | Wallet_TestData.xlsx | Sheet1         | 2   |

  @wallet @Wallet_TC_011
  Scenario Outline: A wallet's own page offers to make it the main wallet
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Wallet page
    And I open the wallet named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the wallet should offer to become the main wallet

    Examples:
      | excelFileName        | excelSheetName | row |
      | Wallet_TestData.xlsx | Sheet1         | 2   |

  @wallet @Wallet_TC_012
  Scenario Outline: Going back from a wallet returns to the list
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Wallet page
    And I open the wallet named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then that wallet's own page should open
    When I go back to the wallets
    Then the wallets should be listed again

    Examples:
      | excelFileName        | excelSheetName | row |
      | Wallet_TestData.xlsx | Sheet1         | 2   |

  # ------------------------------------------------------------------
  # Moving the main wallet
  # ------------------------------------------------------------------

  # The one thing this page can change, so it is done for real rather than looked at. The
  # platform asks nothing first, and the scenario puts the main wallet back where it found it -
  # every other flow in this product reaches for the main wallet, and leaving it pointing
  # wherever this run chose would quietly change what those flows do.
  @wallet @changesmain @Wallet_TC_013
  Scenario Outline: The main wallet can be moved to another wallet and put back
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Wallet page
    And I make the wallet named in "<row>" of "<excelSheetName>" of "<excelFileName>" the main one
    Then that wallet should become the main one
    When I put the main wallet back
    Then the wallet that was main should be main again

    Examples:
      | excelFileName        | excelSheetName | row |
      | Wallet_TestData.xlsx | Sheet1         | 3   |
