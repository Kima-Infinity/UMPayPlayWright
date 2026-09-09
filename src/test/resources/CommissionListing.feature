Feature: Commission Listing

  As a UMPay user who has brought other people to the platform
  I want to see what I have earned in commission and what has been paid out
  So that I can tell what I am owed from what I have already been given

  # WHAT THIS FILE COVERS
  #
  # The commissions at /customer/commission, reached from the profile drawer. A list of what the
  # account has been credited for, each entry a card naming:
  #
  #   where it came from   Transfer, Payment Gateway, Deposit, Withdraw
  #   whether it is settled  an Unsettled or Settled badge
  #   Reference No:        the number the platform knows it by
  #   Amount:              the commission, in the currency it was earned in
  #   Created Date:        when it was credited
  #
  # Pressing one opens a Commission View Detail carrying the same in full - reference, currency,
  # order type, commission fee and date - and offering to download or share it.
  #
  # THE FILTERS
  #
  # A wallet to narrow to, a Type of All, Unsettled or Settled, and a start and end date. Confirm
  # applies them and Reset clears them. A range with nothing in it answers "No data", which is
  # worth holding the page to: an empty list that says nothing cannot be told from one that failed
  # to load.
  #
  # WHY NOTHING HERE IS DANGEROUS
  #
  # Commission is credited by the platform. This page only shows it - there is no form, nothing to
  # submit and nothing to change. The filters are put back with Reset before the scenarios that
  # use them end, so the page is left as it was found.
  #
  # THE DATA
  #
  # TestData/CommissionListing_TestData.xlsx carries the account, the type to ask for, and a date
  # range chosen to hold nothing.

  # ------------------------------------------------------------------
  # What is listed
  # ------------------------------------------------------------------

  @commission @Commission_Listing_TC_001
  Scenario Outline: The profile drawer opens the commission listing
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Commission Listing
    Then the commissions should be listed

    Examples:
      | excelFileName                   | excelSheetName | row |
      | CommissionListing_TestData.xlsx | Sheet1         | 1   |

  # A commission with no reference cannot be asked about, and one with no date cannot be placed
  # against anything the account did.
  @commission @Commission_Listing_TC_002
  Scenario Outline: Every commission carries a reference, an amount and a date
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Commission Listing
    Then every commission should carry a reference, an amount and a date

    Examples:
      | excelFileName                   | excelSheetName | row |
      | CommissionListing_TestData.xlsx | Sheet1         | 1   |

  # An amount with no currency on it is a number nobody can bank.
  @commission @Commission_Listing_TC_003
  Scenario Outline: Every amount is stated in the currency it was earned in
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Commission Listing
    Then every amount should be stated in a currency

    Examples:
      | excelFileName                   | excelSheetName | row |
      | CommissionListing_TestData.xlsx | Sheet1         | 1   |

  # Where a commission came from is what lets somebody check it against their own record of the
  # transaction that earned it.
  @commission @Commission_Listing_TC_004
  Scenario Outline: Every commission says what earned it and whether it is settled
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Commission Listing
    Then every commission should say what earned it and whether it is settled

    Examples:
      | excelFileName                   | excelSheetName | row |
      | CommissionListing_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # Asking for part of it
  # ------------------------------------------------------------------

  @commission @Commission_Listing_TC_005
  Scenario Outline: The listing offers to be narrowed to settled or unsettled
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Commission Listing
    Then the types offered should be "All, Unsettled, Settled"

    Examples:
      | excelFileName                   | excelSheetName | row |
      | CommissionListing_TestData.xlsx | Sheet1         | 1   |

  # What is still owed is the thing somebody comes to this page to find out.
  @commission @Commission_Listing_TC_006
  Scenario Outline: Asking for unsettled commissions leaves only unsettled ones
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Commission Listing
    And I ask for the commissions named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then only commissions of that kind should be listed
    When I clear the filters
    Then the whole listing should come back

    Examples:
      | excelFileName                   | excelSheetName | row |
      | CommissionListing_TestData.xlsx | Sheet1         | 2   |

  @commission @Commission_Listing_TC_007
  Scenario Outline: Asking for settled commissions leaves only settled ones
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Commission Listing
    And I ask for the commissions named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then only commissions of that kind should be listed
    When I clear the filters
    Then the whole listing should come back

    Examples:
      | excelFileName                   | excelSheetName | row |
      | CommissionListing_TestData.xlsx | Sheet1         | 3   |

  # An empty page that says nothing cannot be told from one that failed to load, and somebody
  # would be left wondering whether they had earned nothing or whether the page was broken.
  @commission @Commission_Listing_TC_008
  Scenario Outline: A date range with nothing in it says so
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Commission Listing
    And I ask for the commissions between the dates in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then it should say there is nothing to show
    When I clear the filters
    Then the whole listing should come back

    Examples:
      | excelFileName                   | excelSheetName | row |
      | CommissionListing_TestData.xlsx | Sheet1         | 4   |

  # Commission is earned in whichever currency the transaction was in, so the account has to be
  # able to look at one wallet at a time.
  @commission @Commission_Listing_TC_009
  Scenario Outline: The commissions can be narrowed to one of the account's wallets
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Commission Listing
    And I open the wallets the commissions can be narrowed to
    Then every wallet the account holds should be offered to narrow by

    Examples:
      | excelFileName                   | excelSheetName | row |
      | CommissionListing_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # One commission on its own
  # ------------------------------------------------------------------

  # The reference number is what somebody quotes when they ask why a commission is what it is, so
  # a listing that cannot produce one in full is a listing nobody can act on.
  @commission @Commission_Listing_TC_010
  Scenario Outline: A commission opens its own detail
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Commission Listing
    And I open the first commission
    Then its detail should carry "Reference No, Currency, Order Type, Commission Fee, Created Date"

    Examples:
      | excelFileName                   | excelSheetName | row |
      | CommissionListing_TestData.xlsx | Sheet1         | 1   |

  @commission @Commission_Listing_TC_011
  Scenario Outline: A commission's detail offers to be downloaded and shared
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Commission Listing
    And I open the first commission
    Then the detail should offer "Download, Share"

    Examples:
      | excelFileName                   | excelSheetName | row |
      | CommissionListing_TestData.xlsx | Sheet1         | 1   |

  @commission @Commission_Listing_TC_012
  Scenario Outline: Closing a commission's detail gives back the listing
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Commission Listing
    And I open the first commission
    And I close the detail
    Then the commissions should be listed again

    Examples:
      | excelFileName                   | excelSheetName | row |
      | CommissionListing_TestData.xlsx | Sheet1         | 1   |
