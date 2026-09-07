Feature: Bills

  As a UMPay user
  I want every movement my account has made in one list
  So that I can find a transaction, narrow the list down to it, and read what it was

  # WHAT THIS FILE COVERS
  #
  # The Bills page at /v2/bill: the ledger itself, the three ways it can be narrowed down - by
  # kind of transaction, by how much moved, by when - what it says when nothing matches, and the
  # detail an entry opens, with the receipt it can download and share.
  #
  # WHAT IT READS
  #
  # Everything the rest of this suite has already done. Deposits, withdraws, conversions,
  # transfers and school fees all land here, so these scenarios have plenty to read and submit
  # nothing themselves - they cost the account nothing and can be run as often as you like.
  #
  # THE DATA
  #
  # TestData/Bills_TestData.xlsx carries the account, the kind of transaction to filter by, an
  # amount band and a stretch of days with nothing in it. The dates a scenario needs to match
  # are not in the sheet: a date written down stops matching the day after it is written, so the
  # day filter reads the newest transaction and narrows to that.
  #
  # The kinds are asserted in the scenario rather than fed in from the sheet, because which
  # kinds exist is what is being checked.

  # ------------------------------------------------------------------
  # The ledger
  # ------------------------------------------------------------------

  @bills @Bills_TC_001
  Scenario Outline: The Bills page lists the account's transactions
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Bills page
    Then the ledger should list the account's transactions

    Examples:
      | excelFileName       | excelSheetName | row |
      | Bills_TestData.xlsx | Sheet1         | 1   |

  # Every kind the platform can put in the ledger. A kind appearing or disappearing changes what
  # the account can be asked about, and is worth noticing here rather than discovering when a
  # filter quietly returns nothing.
  @bills @Bills_TC_002
  Scenario Outline: The ledger can be narrowed to any kind of transaction
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Bills page
    Then the ledger should offer to filter by "All, Transfer, Convert, Deposit, Withdraw, Transfer To Card, Card Withdraw, Alipay Transfer, Direct Deposit, Direct Withdraw, Unionpay Transfer, Voucher Fee, International School Fee, Pay, UnionPay Card Topup"

    Examples:
      | excelFileName       | excelSheetName | row |
      | Bills_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # Narrowing it down
  # ------------------------------------------------------------------

  # Filtering by a kind has to leave only that kind. A filter that merely reorders the list, or
  # that leaves one transaction of another kind in it, is worse than no filter at all - it is
  # believed.
  @bills @Bills_TC_003
  Scenario Outline: Filtering by a kind of transaction leaves only that kind
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Bills page
    And I filter the ledger by the type in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then every transaction listed should be a "Convert"

    Examples:
      | excelFileName       | excelSheetName | row |
      | Bills_TestData.xlsx | Sheet1         | 2   |

  # The figures are read without their currency symbols, which is how the page filters them: on
  # the number rather than on what it is worth, so a hundred pesos and a hundred dollars are
  # both inside a band that ends at a hundred.
  @bills @Bills_TC_004
  Scenario Outline: Filtering by amount leaves only what falls inside the band
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Bills page
    And I filter the ledger by the amounts in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then every amount listed should be between "1" and "100"

    Examples:
      | excelFileName       | excelSheetName | row |
      | Bills_TestData.xlsx | Sheet1         | 3   |

  # The day is taken from the ledger rather than from the sheet. A date written down stops
  # matching anything the day after it is written, and a scenario that quietly matches nothing
  # would pass for the wrong reason.
  @bills @Bills_TC_005
  Scenario Outline: Filtering by day leaves only that day
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Bills page
    And I filter the ledger to the day of its newest transaction
    Then every transaction listed should be from that day

    Examples:
      | excelFileName       | excelSheetName | row |
      | Bills_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # When nothing matches, and getting back
  # ------------------------------------------------------------------

  @bills @negative @Bills_TC_006
  Scenario Outline: A stretch of days with nothing in it says so
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Bills page
    And I filter the ledger by the dates in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the ledger should say it has nothing to show

    Examples:
      | excelFileName       | excelSheetName | row |
      | Bills_TestData.xlsx | Sheet1         | 4   |

  # Reset has to undo a filter, not merely clear the boxes. Proved from the emptiest state there
  # is - a stretch of days with nothing in it - so a Reset that cleared the form without asking
  # the ledger again would leave the page empty and fail here.
  @bills @Bills_TC_007
  Scenario Outline: Reset brings the whole ledger back
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Bills page
    And I filter the ledger by the dates in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the ledger should say it has nothing to show
    When I reset the filters
    Then the ledger should list the account's transactions again

    Examples:
      | excelFileName       | excelSheetName | row |
      | Bills_TestData.xlsx | Sheet1         | 4   |

  # ------------------------------------------------------------------
  # One transaction on its own
  # ------------------------------------------------------------------

  # An entry opens a detail carrying the transaction's number, what went in and what came out,
  # the rate where there was one, and the date. The number is what makes it this transaction's
  # detail rather than merely a detail.
  @bills @Bills_TC_008
  Scenario Outline: A transaction opens its own detail
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Bills page
    And I open the newest transaction
    Then the transaction detail should name the transaction, when it happened and what moved

    Examples:
      | excelFileName       | excelSheetName | row |
      | Bills_TestData.xlsx | Sheet1         | 5   |

  # The receipt is drawn in the browser and handed over as a data URL rather than fetched from
  # the server, so a file arriving with something in it is the only evidence the button did
  # anything at all.
  @bills @Bills_TC_009
  Scenario Outline: A transaction's receipt can be downloaded
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Bills page
    And I open the newest transaction
    Then downloading the transaction should produce a file

    Examples:
      | excelFileName       | excelSheetName | row |
      | Bills_TestData.xlsx | Sheet1         | 5   |

  @bills @Bills_TC_010
  Scenario Outline: A transaction can be shared
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Bills page
    And I open the newest transaction
    When I share the transaction
    Then the sharing choices should offer a way to copy the transaction

    Examples:
      | excelFileName       | excelSheetName | row |
      | Bills_TestData.xlsx | Sheet1         | 5   |
