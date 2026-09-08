Feature: Withdraw
  As a UMPay user
  I want to perform a withdraw transaction
  So that I can take money out of my wallet

  # ------------------------------------------------------------------
  # A withdraw from every wallet the account holds
  # ------------------------------------------------------------------
  #
  # One example row per wallet the currency list offers, the same ten the deposits cover. Each
  # has its own limits and its own saved payout accounts, so one row cannot stand in for the
  # rest. What the form stated when these were written:
  #
  #   HKD  100 to 9999999999   HSBC, FPS or USDT-TRC 20
  #   PHP  1000 to 50000       nothing saved
  #   BDT  1000 to 50000       USDT-TRC 20
  #   MXN  20 to 20000         AFIRME
  #   BRL  100 to 5000         nothing saved
  #   VND  1100000 to 100000000  nothing saved
  #   IDR  200000 to 10000000  DANA
  #   THB  551 to 20000        Bangkok Bank Plc. or USDT-TRC 20
  #   MYR  200000 to 20000     the form states a minimum above its own maximum
  #   USD  19.77 to 4943.71    USDT-TRC 20
  #
  # The amount column reads MIN rather than a figure, as the deposits do: each wallet's
  # minimum is its own and some are a conversion of a limit held elsewhere, so a number in the
  # sheet is a number that goes stale. MIN means the smallest the form itself says it will
  # take.
  #
  # The payment account column names one where the row is proving that account in particular,
  # and reads SAVED where it is not - meaning whatever this wallet has saved. Which templates
  # an account holds is a fact about the account rather than about the test, and a wallet with
  # none saved cannot pay out at all, which the run says in those words.
  #
  # The balance is read before anything is filled in. A wallet that cannot cover its own
  # minimum has already decided the outcome, so the run says what the wallet holds and what
  # was asked for rather than filling in a form to be told the same thing more slowly. Four
  # wallets are short at the time of writing - MXN and THB hold nothing, VND and MYR hold less
  # than their minimum - and those failures are the account's state, not the suite's.
  #
  # The HKD row names FPS rather than HSBC. Both are saved and HSBC does go through, but the
  # platform answers FPS with "This service not available please try again with other methods"
  # - its own decision about that channel, which the run reports in the platform's words.
  #
  # Receive Information is asked for twice over, because the form offers two ways to give it
  # and they are different code paths in the application - different pickers, different boxes,
  # different validation. From Template picks a payout account the account already holds. New
  # Account types a fresh one in, and what it asks for is the wallet's own business: Hong
  # Kong's E-Wallet wants an FPS account and payer name, the Philippines and Indonesia want an
  # account name and number under a bank chosen from a list of ninety-odd, Brazil wants a CPF
  # tax number and account number, and the USDT wallets want an address. So the form is asked
  # what it wants rather than told, the same way the deposits ask.
  #
  # Save Template is left alone on the New Account rows. Ticking it would add a payout account
  # to the test account on every run, and the templates it holds are exactly what the From
  # Template rows read - the two halves would slowly rewrite each other's data.
  #
  # These submit real withdraw orders, two per wallet per run, and each needs the account's
  # PIN. Tag them @manual if that is more than an unattended run should be spending.

  @withdraw @fromtemplate @Withdraw_TC_001
  Scenario Outline: Successful Withdraw Transaction using Template
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to Withdraw page
    Then I should be able to initiate a withdraw transaction using "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples: From Template - a payout account the account already holds
      | excelFileName          | excelSheetName | row |
      | Withdraw_TestData.xlsx | sheet1         | 1   |
      | Withdraw_TestData.xlsx | sheet1         | 2   |
      | Withdraw_TestData.xlsx | sheet1         | 3   |
      | Withdraw_TestData.xlsx | sheet1         | 4   |
      | Withdraw_TestData.xlsx | sheet1         | 5   |
      | Withdraw_TestData.xlsx | sheet1         | 6   |
      | Withdraw_TestData.xlsx | sheet1         | 7   |
      | Withdraw_TestData.xlsx | sheet1         | 8   |
      | Withdraw_TestData.xlsx | sheet1         | 9   |
      | Withdraw_TestData.xlsx | sheet1         | 10  |

  @withdraw @newaccount @Withdraw_TC_002
  Scenario Outline: Successful Withdraw Transaction using New Account
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to Withdraw page
    Then I should be able to initiate a withdraw transaction using "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples: New Account - a payout account typed in on the spot
      | excelFileName          | excelSheetName | row |
      | Withdraw_TestData.xlsx | sheet1         | 11  |
      | Withdraw_TestData.xlsx | sheet1         | 12  |
      | Withdraw_TestData.xlsx | sheet1         | 13  |
      | Withdraw_TestData.xlsx | sheet1         | 14  |
      | Withdraw_TestData.xlsx | sheet1         | 15  |
      | Withdraw_TestData.xlsx | sheet1         | 16  |
      | Withdraw_TestData.xlsx | sheet1         | 17  |
      | Withdraw_TestData.xlsx | sheet1         | 18  |
      | Withdraw_TestData.xlsx | sheet1         | 19  |
      | Withdraw_TestData.xlsx | sheet1         | 20  |

  # Nothing is submitted here. The scenarios above each withdraw from one wallet; this is the
  # one that notices a wallet appearing or disappearing from the list they are drawn from.
  @withdraw @Withdraw_TC_003
  Scenario Outline: The withdraw form offers a wallet for every currency the account holds
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to Withdraw page
    Then the withdraw currency list should offer "BDT, BRL, HKD, IDR, MXN, MYR, PHP, THB, USD, VND"

    Examples:
      | excelFileName          | excelSheetName | row |
      | Withdraw_TestData.xlsx | sheet1         | 1   |

  # ------------------------------------------------------------------
  # What the amount box will and will not take
  # ------------------------------------------------------------------
  #
  # The form prints its lower bound - "Limit Min 100 HKD" - and the box carries min and max
  # with type=number, so the browser refuses both ends before the server is troubled. The
  # wording asserted below is the application's own, set on the element rather than left to
  # the browser's stock phrasing.
  #
  # The figures are not written down here. Each wallet has its own band, the bands are the
  # platform's rather than the suite's, and the US dollar one is a conversion that moves - so
  # the run reads what the form states and steps one outside it. A number in the feature file
  # would be measuring a band that has since changed.
  #
  # The MYR row fails, and the failure is the point. That wallet states a minimum of 200000
  # against a maximum of 20000, so one above its maximum - 20001 - is still below its minimum,
  # and the box answers "limit min or above" where every other wallet answers "limit max or
  # below". The band cannot be satisfied by any figure at all, which is why the MYR withdraw
  # form never even draws its payout section. Asserting the wording the other nine give is what
  # makes that visible; loosening it to accept either message would hide it.
  #
  # Nothing here submits. These cost the account nothing and can be run as often as you like.

  @withdraw @limits @Withdraw_TC_004
  Scenario Outline: The amount box holds each wallet to its own stated limits
    Given I log into the UMPay application with valid email credentials using "1" of "sheet1" of "Withdraw_TestData.xlsx"
    When I navigate to Withdraw page
    And I choose the "<currency>" wallet on the Withdraw page
    And I enter one below the stated minimum as the withdraw amount
    Then the withdraw amount should be refused with "Please input an amount of limit min or above!"
    When I enter one above the stated maximum as the withdraw amount
    Then the withdraw amount should be refused with "Please input an amount of limit max or below!"
    When I enter the stated minimum as the withdraw amount
    Then the withdraw amount should be accepted

    Examples:
      | currency |
      | HKD      |
      | PHP      |
      | BDT      |
      | MXN      |
      | BRL      |
      | VND      |
      | IDR      |
      | THB      |
      | MYR      |
      | USD      |

  # ------------------------------------------------------------------
  # What the form will not let a withdraw be
  # ------------------------------------------------------------------
  #
  # Everything above proves a withdraw that should go through. These prove the ones that should
  # not, which is the half a suite tends to be missing: until now not one withdraw case was a
  # negative, so nothing in the file would have noticed the form quietly accepting an amount it
  # ought to refuse.
  #
  # The first three cost the account nothing and can be run as often as you like. Only the last
  # one presses Confirm, and it presses it on a withdraw the wallet cannot cover - so if the
  # platform behaves, nothing moves, and if it does not, the case has found something that
  # matters more than the cost of finding it.
  #
  # WHAT THE FORM DOES TO AN AMOUNT AS IT IS TYPED. The box is type=number and the form works on
  # it key by key, so letters, a zero and a negative never land in it at all - the box is left
  # empty rather than filled with something wrong, and the form then says the field is required.
  # That is why these read the box back rather than only asking whether it is valid: a box that
  # is invalid because it is empty and a box that is invalid because the amount is wrong are the
  # same verdict from two different places, and only one of them is what is being tested.

  @withdraw @negative @Withdraw_TC_005
  Scenario Outline: The amount box refuses anything that is not an amount
    Given I log into the UMPay application with valid email credentials using "1" of "sheet1" of "Withdraw_TestData.xlsx"
    When I navigate to Withdraw page
    And I choose the "HKD" wallet on the Withdraw page
    And I type "<typed>" at the withdraw amount box
    Then the withdraw amount box should be left holding nothing
    And the withdraw amount should be refused with "This field is required"

    Examples: things that are not an amount
      | typed |
      | abcd  |
      | 0     |
      | -100  |

  # A figure finer than the currency's smallest unit cannot be paid out, so the form should not
  # take one. Hong Kong dollars have cents and nothing smaller.
  @withdraw @negative @Withdraw_TC_006
  Scenario Outline: The amount box will not take a figure finer than the currency pays in
    Given I log into the UMPay application with valid email credentials using "1" of "sheet1" of "Withdraw_TestData.xlsx"
    When I navigate to Withdraw page
    And I choose the "HKD" wallet on the Withdraw page
    And I type "<typed>" at the withdraw amount box
    Then the withdraw amount box should be left holding "<kept>"

    Examples:
      | typed   | kept   |
      | 100.555 | 100.55 |

  # A withdraw with nowhere to send the money should not be raisable at all. The form disables
  # Confirm rather than hiding it, so what is asserted is that it cannot be pressed - and then
  # that choosing a payout account is what makes it pressable, since a Confirm that is disabled
  # for some other reason entirely would pass the first half on its own.
  @withdraw @negative @Withdraw_TC_007
  Scenario Outline: A withdraw cannot be confirmed with nowhere to send the money
    Given I log into the UMPay application with valid email credentials using "1" of "sheet1" of "Withdraw_TestData.xlsx"
    When I navigate to Withdraw page
    And I choose the "<currency>" wallet on the Withdraw page
    And I enter the stated minimum as the withdraw amount
    Then the withdraw should not be confirmable
    When I choose the saved payout account "<account>"
    Then the withdraw should be confirmable

    Examples:
      | currency | account |
      | HKD      | HSBC    |

  # The one that presses Confirm. The amount is read from what the wallet actually holds and a
  # thousand added to it, so the figure is inside the band the form states and outside what the
  # account has - which is the only way to ask the platform this question rather than the form.
  #
  # Nothing should move. If the platform raises an order for it anyway, that is worth knowing.
  @withdraw @negative @Withdraw_TC_008
  Scenario Outline: A withdraw for more than the wallet holds is refused
    Given I log into the UMPay application with valid email credentials using "1" of "sheet1" of "Withdraw_TestData.xlsx"
    When I navigate to Withdraw page
    And I choose the "<currency>" wallet on the Withdraw page
    And I enter more than the wallet holds as the withdraw amount
    And I choose the saved payout account "<account>"
    And I confirm the withdraw
    Then the platform should refuse to raise the order

    Examples:
      | currency | account |
      | HKD      | HSBC    |
