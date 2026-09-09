Feature: Home

  As a UMPay account holder
  I want the screen I land on to show what I hold and where I can go
  So that I can see the state of my account and start whatever I came to do

  # WHAT THIS FILE COVERS
  #
  # The screen every account lands on after signing in. It is the first thing anybody sees and
  # was the last thing in the application with no feature file of its own - the page object had
  # been written for the end-to-end journey and nothing held it to anything.
  #
  # WHAT THE PAGE IS MADE OF
  #
  #   Total in HKD          what the whole account comes to, in the main wallet's currency
  #   Total Blocked Amount  what is held against the account across every wallet
  #   Wallets               three cards to begin with, the rest behind Show More
  #   The sidebar           the nine flows a transaction can be started from
  #
  # THE AMOUNTS ARRIVE HIDDEN
  #
  # Every figure on the page is written ******** until it is asked for. That is worth holding the
  # page to in both directions: hidden when it arrives, because somebody opening their account in
  # an office does not necessarily want what they hold on the screen behind them, and readable
  # once the eye is pressed, because a page that hides them and then will not show them is no
  # more use than one that never had them.
  #
  # THE DATA
  #
  # TestData/Home_TestData.xlsx carries the account. What the account holds is not written into
  # it - balances move every time the suite sends anything, and the withdraw and transfer cases
  # send a great deal - so what is held to is the shape of the page rather than the figures on it.

  @home @Home_TC_001
  Scenario Outline: Signing in lands on the account's own home page
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I am on the home page
    Then the home page should show the account's total and its wallets

    Examples:
      | excelFileName      | excelSheetName | row |
      | Home_TestData.xlsx | Sheet1         | 1   |

  # Hidden when it arrives, and readable when asked for. A page that does only the first half is
  # hiding the account from its owner.
  @home @Home_TC_002
  Scenario Outline: The amounts arrive hidden and the eye reveals them
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I am on the home page
    Then the amounts should be hidden
    When I reveal the amounts
    Then the amounts should be readable

    Examples:
      | excelFileName      | excelSheetName | row |
      | Home_TestData.xlsx | Sheet1         | 1   |

  # The home page shows three wallets and keeps the rest back. Somebody who cannot reach the
  # other seven cannot see most of what they own.
  @home @Home_TC_003
  Scenario Outline: Show More brings out every wallet the account holds
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I am on the home page
    Then the home page should offer to show more wallets
    When I show more wallets
    Then more wallets should be listed than before

    Examples:
      | excelFileName      | excelSheetName | row |
      | Home_TestData.xlsx | Sheet1         | 1   |

  # A card missing its currency cannot be told from another, and one missing its balance is the
  # one thing somebody came to the page to read.
  @home @Home_TC_004
  Scenario Outline: Every wallet card names its currency, country, balance and what is held
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I am on the home page
    And I reveal the amounts
    And I show more wallets
    Then every wallet card should name its currency, country, balance and what is held

    Examples:
      | excelFileName      | excelSheetName | row |
      | Home_TestData.xlsx | Sheet1         | 1   |

  # The main wallet is what the transfer, convert and withdraw screens open on, so two of them
  # would make those screens start somewhere nobody chose.
  @home @Home_TC_005
  Scenario Outline: Exactly one wallet is the main wallet and the rest offer to become it
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I am on the home page
    And I show more wallets
    Then exactly one wallet on the home page should be the main wallet
    And every other wallet should offer to become the main one

    Examples:
      | excelFileName      | excelSheetName | row |
      | Home_TestData.xlsx | Sheet1         | 1   |

  # The sidebar is the only way into most of the application. A flow missing from it is a flow
  # nobody can start.
  @home @Home_TC_006
  Scenario Outline: The sidebar offers every flow the account can start
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I am on the home page
    Then the sidebar should offer "Home, International School Fees, Deposit, Withdraw, Domestic Transfer, Global Transfer, Transfer, Bills, Convert"

    Examples:
      | excelFileName      | excelSheetName | row |
      | Home_TestData.xlsx | Sheet1         | 1   |

  # What is held against an account is a quantity of money set aside, not a debt. Written with a
  # minus in front of it, it reads as though the account is owed that money rather than kept from
  # spending it, and the total then reads "Total Blocked Amount: HK$ -10,684.58".
  @home @negative @Home_TC_007
  Scenario Outline: What is held against a wallet is never shown as a negative amount
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I am on the home page
    And I reveal the amounts
    And I show more wallets
    Then no wallet should show what is held against it as a negative amount

    Examples:
      | excelFileName      | excelSheetName | row |
      | Home_TestData.xlsx | Sheet1         | 1   |

  # Somebody whose account is not behaving is on this page when they need help.
  @home @Home_TC_008
  Scenario Outline: Customer Service can be reached from the home page
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I am on the home page
    Then Customer Service should be offered on the home page

    Examples:
      | excelFileName      | excelSheetName | row |
      | Home_TestData.xlsx | Sheet1         | 1   |
