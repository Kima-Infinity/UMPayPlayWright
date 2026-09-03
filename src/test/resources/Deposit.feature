Feature: Deposit
  As a UMPay user
  I want to perform a deposit transaction
  So that I can send money internationally

  # ------------------------------------------------------------------
  # A deposit in every currency the account can hold
  # ------------------------------------------------------------------
  #
  # One example row per wallet the currency dropdown offers, and the dropdown is the account's
  # wallets rather than the platform's currency list - so this is a deposit in each of the ten
  # the account holds, from BDT to VND.
  #
  # Every currency has its own channel and its own limits, which is why one row cannot stand in
  # for the rest:
  #
  #   HKD  100 to 999999999   Alipay, E-Wallet, Bank, Cash Deposit or USDT
  #   PHP  11000 to 50000     E-Wallet (QR-PH) or Bank (InstaPay)
  #   BDT  1000 to 50000      USDT, on a network the row names
  #   MXN  20 to 20000        Bank (SPEI)
  #   BRL  100 to 5000        E-Wallet (PIX)
  #   VND  320000 to 10000000 E-Wallet (MOMO or VIETQR)
  #   IDR  220000 to 10000000 E-Wallet (QRIS)
  #   THB  550 to 50000       E-Wallet (PromptPay)
  #   MYR  110 to 1000        E-Wallet (Duitnow, FPX or Touch N Go)
  #
  # The MYR row names FPX rather than Duitnow. Both are offered and Duitnow does go through,
  # but the platform answered one run with "This service not available please try again with
  # other methods" - its own decision about that channel, not a fault in the test - and FPX has
  # not done that. A run that meets it again says so in those words rather than timing out.
  #   USD  19.77 to 4943.71   USDT
  #
  # The amount column reads MIN rather than a figure. Each wallet's minimum is its own, and
  # some are a conversion of a limit held in another currency - the US dollar wallet asked for
  # 19.77 the day these were written - so a number in the sheet is a number that goes stale.
  # MIN means the smallest the form itself says it will take.
  #
  # These submit real deposit orders, one per currency per run. Tag them @manual if that is
  # more than an unattended run should be spending.

  @deposit
  Scenario Outline: Successful Deposit Transaction
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to Deposit page
    Then I should be able to initiate a deposit transaction using "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName         | excelSheetName | row |
      | Deposit_TestData.xlsx | sheet1         | 1   |
      | Deposit_TestData.xlsx | sheet1         | 2   |
      | Deposit_TestData.xlsx | sheet1         | 3   |
      | Deposit_TestData.xlsx | sheet1         | 4   |
      | Deposit_TestData.xlsx | sheet1         | 5   |
      | Deposit_TestData.xlsx | sheet1         | 6   |
      | Deposit_TestData.xlsx | sheet1         | 7   |
      | Deposit_TestData.xlsx | sheet1         | 8   |
      | Deposit_TestData.xlsx | sheet1         | 9   |
      | Deposit_TestData.xlsx | sheet1         | 10  |

  # Nothing is submitted here. The scenarios above each deposit in one currency; this is the
  # one that notices a wallet appearing or disappearing from the list they are drawn from.
  @deposit
  Scenario: The deposit form offers a wallet for every currency the account holds
    Given I log into the UMPay application with valid email credentials using "1" of "sheet1" of "Deposit_TestData.xlsx"
    When I navigate to Deposit page
    Then the deposit currency list should offer "BDT, BRL, HKD, IDR, MXN, MYR, PHP, THB, USD, VND"

  # ------------------------------------------------------------------
  # What the amount box will and will not take
  # ------------------------------------------------------------------
  #
  # The form prints its lower bound - "Limit Min 100 HKD" - and prints no upper one, but the
  # box carries min=100 and max=999999999 with type=number, so the browser refuses both ends
  # before the server is troubled. The wording below is the application's own, set on the
  # element rather than left to the browser's stock phrasing, which is why it is asserted
  # exactly rather than by a fragment.
  #
  # The Confirm button is deliberately not asserted. It stays disabled for a good amount as
  # well as a bad one, because the payment type has not been chosen at this point, so a
  # scenario resting on it would pass whether the limit worked or not.
  #
  # Nothing here submits. These cost the account nothing and can be run as often as you like.

  @deposit @negative
  Scenario: An amount below the minimum is refused
    Given I log into the UMPay application with valid email credentials using "1" of "sheet1" of "Deposit_TestData.xlsx"
    When I navigate to Deposit page
    And I choose the "HKD" wallet on the Deposit page
    And I enter "50" as the deposit amount
    Then the deposit amount should be refused with "Please input an amount of limit min or above!"

  @deposit @negative
  Scenario: An amount above the maximum is refused
    Given I log into the UMPay application with valid email credentials using "1" of "sheet1" of "Deposit_TestData.xlsx"
    When I navigate to Deposit page
    And I choose the "HKD" wallet on the Deposit page
    And I enter "1000000000" as the deposit amount
    Then the deposit amount should be refused with "Please input an amount of limit max or below!"

  # The boundary itself, so the two above are known to be measuring the edge rather than
  # refusing everything.
  @deposit
  Scenario: The minimum itself is accepted
    Given I log into the UMPay application with valid email credentials using "1" of "sheet1" of "Deposit_TestData.xlsx"
    When I navigate to Deposit page
    And I choose the "HKD" wallet on the Deposit page
    And I enter "100" as the deposit amount
    Then the deposit amount should be accepted

  @deposit
  Scenario: The amount box states the limits it enforces
    Given I log into the UMPay application with valid email credentials using "1" of "sheet1" of "Deposit_TestData.xlsx"
    When I navigate to Deposit page
    And I choose the "HKD" wallet on the Deposit page
    Then the deposit form should state a minimum of "100" and a maximum of "999999999"
