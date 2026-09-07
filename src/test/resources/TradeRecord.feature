Feature: Trade Record

  As a UMPay user
  I want every order I have raised in one place
  So that I can see what became of each of them and go to any one to act on it

  # WHAT THIS FILE COVERS
  #
  # The trade record at /v2/trade-record, reached from the profile drawer. Where the Bills page
  # lists money that moved and the notification page lists what the platform said about it, this
  # lists the orders themselves - paid, unpaid, cancelled, failed and completed alike.
  #
  # Three tabs, each with a card of its own shape:
  #
  #   Deposit/Withdraw         - kind, how it ended, Number, Currency, Amount, and where a
  #                              conversion happened, Received Amount and Exchange rate
  #   International Transfer   - Number, Currency, Total Amount, Fee, Amount, Received Currency,
  #                              Approximate Received and Created Date
  #   International School Fee - Order Number, Total Amount, Currency and Created Date
  #
  # The list grows as it is scrolled rather than paging, twenty orders at a time. There is no
  # filter, no search and no date range anywhere on the page - which is worth knowing, because
  # every other listing in this product has them.
  #
  # WHAT IT READS, AND WHAT IT CHANGES
  #
  # Nothing here raises an order or pays one. The scenario that opens an order stops at the
  # order's own page and comes back: an unpaid deposit opens the page where it would be paid,
  # and paying it is not this file's business.
  #
  # THE DATA
  #
  # TestData/TradeRecord_TestData.xlsx carries the account and the tab to move to. What the tabs
  # are called is asserted from the page rather than fed in, because which tabs exist is what is
  # being checked.

  # ------------------------------------------------------------------
  # The record and its tabs
  # ------------------------------------------------------------------

  @traderecord @Trade_Record_TC_001
  Scenario Outline: The profile drawer opens the account's trade record
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Trade Record page
    Then the trade record should list the orders the account has raised

    Examples:
      | excelFileName             | excelSheetName | row |
      | TradeRecord_TestData.xlsx | Sheet1         | 1   |

  # Which kinds of order the page will show is what decides what can be looked up here at all, so
  # a tab appearing or disappearing is worth noticing rather than discovering when a scenario
  # quietly reads the wrong list.
  @traderecord @Trade_Record_TC_002
  Scenario Outline: The trade record is kept in three tabs
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Trade Record page
    Then the trade record should offer the tabs "Deposit/Withdraw, International Transfer, International School Fee"

    Examples:
      | excelFileName             | excelSheetName | row |
      | TradeRecord_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # Deposits and withdrawals
  # ------------------------------------------------------------------

  # An order with no number cannot be looked up by anybody - not by the account holder and not by
  # customer service - and an order with no status is one nobody can act on.
  @traderecord @Trade_Record_TC_003
  Scenario Outline: Every order names its number, currency, amount and how it ended
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Trade Record page
    Then every order should name its number, currency, amount and how it ended

    Examples:
      | excelFileName             | excelSheetName | row |
      | TradeRecord_TestData.xlsx | Sheet1         | 1   |

  # The number is what identifies an order to everybody who has to talk about it. Two orders
  # carrying the same one is the worst kind of listing bug, because both of them look right.
  @traderecord @Trade_Record_TC_004
  Scenario Outline: No two orders share a number
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Trade Record page
    Then no two orders should share a number

    Examples:
      | excelFileName             | excelSheetName | row |
      | TradeRecord_TestData.xlsx | Sheet1         | 1   |

  # A received amount with no rate cannot be checked by the reader, and a rate with nothing
  # received is a rate applied to nothing. Either half alone is worse than neither.
  @traderecord @Trade_Record_TC_005
  Scenario Outline: A converted order shows both what was received and the rate
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Trade Record page
    Then any converted order should show both what was received and the rate

    Examples:
      | excelFileName             | excelSheetName | row |
      | TradeRecord_TestData.xlsx | Sheet1         | 1   |

  # The list grows as it is scrolled rather than paging, so reaching the bottom is what asks for
  # the next of them. An account with hundreds of orders is unreadable if only twenty can ever be
  # reached.
  @traderecord @Trade_Record_TC_006
  Scenario Outline: Reaching the end of the record brings in older orders
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Trade Record page
    And I scroll to the end of the trade record
    Then more orders should have been brought in

    Examples:
      | excelFileName             | excelSheetName | row |
      | TradeRecord_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # International transfers and school fees
  # ------------------------------------------------------------------

  # The card shows what was taken altogether, what the platform kept and what was sent, so the
  # three can be held against each other. A total that is not the fee plus the amount means
  # somebody is being charged something the card does not name.
  @traderecord @Trade_Record_TC_007
  Scenario Outline: An international transfer is charged its fee plus its amount
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Trade Record page
    And I move to the trade record tab named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then every international transfer should add up to its total

    Examples:
      | excelFileName             | excelSheetName | row |
      | TradeRecord_TestData.xlsx | Sheet1         | 2   |

  @traderecord @Trade_Record_TC_008
  Scenario Outline: A school fee order names its order number, total and when it was created
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Trade Record page
    And I move to the trade record tab named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then every school fee order should name its order number, total and when it was created

    Examples:
      | excelFileName             | excelSheetName | row |
      | TradeRecord_TestData.xlsx | Sheet1         | 3   |

  # A tab that merely highlights itself while leaving the same list underneath is worse than no
  # tab at all, because a reader believes they are looking at something else.
  @traderecord @Trade_Record_TC_009
  Scenario Outline: Moving to another tab changes what the record lists
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Trade Record page
    And I move to the trade record tab named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then what the trade record lists should change

    Examples:
      | excelFileName             | excelSheetName | row |
      | TradeRecord_TestData.xlsx | Sheet1         | 2   |

  # ------------------------------------------------------------------
  # Going from the record to the order
  # ------------------------------------------------------------------

  # The whole use of a record is being able to act on what is in it. Unlike the notification
  # page's cards, these are links: one opens the order's own page, at an address naming the order
  # and where it was opened from.
  @traderecord @Trade_Record_TC_010
  Scenario Outline: An order in the record opens the order itself
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Trade Record page
    And I open the newest order
    Then the order's own page should open

    Examples:
      | excelFileName             | excelSheetName | row |
      | TradeRecord_TestData.xlsx | Sheet1         | 4   |

  # The address an order opens at names where it was opened from, so the way back has to work -
  # a reader who follows an order and cannot return has lost their place in a list they were
  # part way through.
  @traderecord @Trade_Record_TC_011
  Scenario Outline: Going back from an order returns to the trade record
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Trade Record page
    And I open the newest order
    Then the order's own page should open
    When I go back to the trade record
    Then the trade record should be listing again

    Examples:
      | excelFileName             | excelSheetName | row |
      | TradeRecord_TestData.xlsx | Sheet1         | 4   |
