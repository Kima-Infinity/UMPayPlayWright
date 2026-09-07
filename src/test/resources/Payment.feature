Feature: Payment

  As a UMPay user
  I want to keep the accounts I can be paid out to
  So that a withdrawal or a payout has somewhere to go, on terms I chose

  # WHAT THIS FILE COVERS
  #
  # The payment accounts at /settings-payment, reached from the profile drawer. These are the
  # accounts the withdraw and payout flows draw on - when one of those says "Please add your
  # payment account", this is the page it means - so what is listed here decides what the rest of
  # the product can do.
  #
  # An account is shown as a block: the provider across the top, and under it whatever that kind
  # of account is identified by. The shape differs by kind, which is why it is read rather than
  # assumed: a bank carries an Account Name and Number, an e-wallet may carry an email address
  # and a first and last name besides, a fast payment carries a Wallet Name and Number, and a
  # USDT address carries a Wallet Address and sometimes a Remark. Every block offers two things:
  # change it, and remove it.
  #
  # Add Payment opens /settings-payment/create, which asks for a currency and then offers three
  # ways of being paid - E-Wallet, Bank and USDT. Choosing one opens a searchable list of the
  # providers that take it, each naming how long it takes and what it charges.
  #
  # WHAT NOTHING HERE DOES
  #
  # No scenario saves an account and no scenario removes one. The remove control is counted and
  # reported on, never clicked: these accounts are what the Withdraw and Global Transfer
  # scenarios rely on, and one removed here would fail a scenario in another file entirely -
  # the hardest kind of failure to account for. Adding one is walked up to the point of choosing
  # a provider and no further, for the same reason in reverse: this account already carries a
  # dozen, and every run that saved another would leave the list a little worse.
  #
  # THE DATA
  #
  # TestData/Payment_TestData.xlsx carries the account, the way of being paid to choose, and what
  # to search the providers for.

  # ------------------------------------------------------------------
  # What is saved
  # ------------------------------------------------------------------

  @payment @Payment_TC_001
  Scenario Outline: The profile drawer opens the account's payment accounts
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Payment page
    Then the page should list the payment accounts the account can be paid out to

    Examples:
      | excelFileName         | excelSheetName | row |
      | Payment_TestData.xlsx | Sheet1         | 1   |

  # An account saved with an empty field is one a payout would be sent into the dark. Which
  # fields there are depends on the kind of account, so what is asserted is that there are some
  # and that none of them is blank.
  @payment @Payment_TC_002
  Scenario Outline: Every saved account names its provider and what identifies it
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Payment page
    Then every saved account should name its provider and what identifies it

    Examples:
      | excelFileName         | excelSheetName | row |
      | Payment_TestData.xlsx | Sheet1         | 1   |

  # An account whose details are wrong and which cannot be changed is a payout waiting to go
  # astray. Counted rather than clicked - proving the removal by removing one would break the
  # withdraw scenarios that rely on these accounts.
  @payment @Payment_TC_003
  Scenario Outline: Every saved account offers to be changed and removed
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Payment page
    Then every saved account should offer to be changed and removed

    Examples:
      | excelFileName         | excelSheetName | row |
      | Payment_TestData.xlsx | Sheet1         | 1   |

  @payment @Payment_TC_004
  Scenario Outline: A saved account can be opened to be changed
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Payment page
    And I open the first saved account to change it
    Then somewhere to change it should open

    Examples:
      | excelFileName         | excelSheetName | row |
      | Payment_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # Adding one
  # ------------------------------------------------------------------

  @payment @Payment_TC_005
  Scenario Outline: The page offers to add a payment account
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Payment page
    And I start adding a payment account
    Then the form for a new payment account should open

    Examples:
      | excelFileName         | excelSheetName | row |
      | Payment_TestData.xlsx | Sheet1         | 1   |

  # A way of being paid that disappeared would quietly take a whole kind of payout with it, and
  # nobody would notice until somebody went looking for it.
  @payment @Payment_TC_006
  Scenario Outline: The form asks for a currency and offers every way of being paid
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Payment page
    And I start adding a payment account
    Then the form should ask for a currency and offer "E-Wallet, Bank, USDT"

    Examples:
      | excelFileName         | excelSheetName | row |
      | Payment_TestData.xlsx | Sheet1         | 1   |

  # What a payout costs and how long it takes is the whole of what somebody is choosing on. A
  # list of names alone would make the choice arbitrary, and a fee that only appeared later would
  # make it a surprise.
  @payment @Payment_TC_007
  Scenario Outline: Choosing to be paid by bank offers banks with their fees and how long they take
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Payment page
    And I start adding a payment account
    And I choose the way of being paid named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the providers offered should each name how long they take and what they charge

    Examples:
      | excelFileName         | excelSheetName | row |
      | Payment_TestData.xlsx | Sheet1         | 2   |

  @payment @Payment_TC_008
  Scenario Outline: Choosing to be paid by e-wallet offers e-wallets with their fees and how long they take
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Payment page
    And I start adding a payment account
    And I choose the way of being paid named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the providers offered should each name how long they take and what they charge

    Examples:
      | excelFileName         | excelSheetName | row |
      | Payment_TestData.xlsx | Sheet1         | 3   |

  @payment @Payment_TC_009
  Scenario Outline: Choosing to be paid in USDT offers what it can be sent through
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Payment page
    And I start adding a payment account
    And I choose the way of being paid named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the providers offered should each name how long they take and what they charge

    Examples:
      | excelFileName         | excelSheetName | row |
      | Payment_TestData.xlsx | Sheet1         | 4   |

  @payment @Payment_TC_010
  Scenario Outline: The providers can be searched
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Payment page
    And I start adding a payment account
    And I choose the way of being paid named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the providers should be searchable

    Examples:
      | excelFileName         | excelSheetName | row |
      | Payment_TestData.xlsx | Sheet1         | 2   |

  # A search that merely reorders the list, or leaves something else in it, is worse than no
  # search at all, because it is believed.
  @payment @Payment_TC_011
  Scenario Outline: Searching the providers leaves only what was searched for
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Payment page
    And I start adding a payment account
    And I choose the way of being paid named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I search the providers for what is in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then only the providers matching it should be left

    Examples:
      | excelFileName         | excelSheetName | row |
      | Payment_TestData.xlsx | Sheet1         | 2   |

  # Somebody who opens the wrong list has to be able to get out of it without losing the currency
  # they had already chosen.
  @payment @Payment_TC_012
  Scenario Outline: Closing the providers leaves the form as it was
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Payment page
    And I start adding a payment account
    And I choose the way of being paid named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I close what is being asked
    Then the form should still be there

    Examples:
      | excelFileName         | excelSheetName | row |
      | Payment_TestData.xlsx | Sheet1         | 2   |

  @payment @Payment_TC_013
  Scenario Outline: Going back from adding one returns to the saved accounts
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Payment page
    And I start adding a payment account
    Then the form for a new payment account should open
    When I go back to the payment accounts
    Then the payment accounts should be listed again

    Examples:
      | excelFileName         | excelSheetName | row |
      | Payment_TestData.xlsx | Sheet1         | 1   |
