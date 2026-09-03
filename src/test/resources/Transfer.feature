Feature: UMPay transfers

  As a UMPay user
  I want to reach every way the application can move money out of my wallet
  So that each transfer route is known to open, validate and price correctly

  # TRANSFER IS THREE AREAS, NOT ONE PAGE
  #
  # The left navigation has three separate transfer entries, and they overlap rather than
  # divide neatly. Between them they offer nine routes:
  #
  #   Transfer          /v2/transfer         Existing template, UMPay Wallet, UnionPay China,
  #                                          UnionPay Global, Alipay, Wechat
  #   Domestic Transfer /domestic-transfer   UnionPay China, Alipay, Wechat
  #   Global Transfer   /global-transfer     School Fees, Personal Bank Account,
  #                                          UnionPay Global, USDT
  #
  # UnionPay China is reachable from two of them and UnionPay Global from two, which is why
  # the scenarios name the area they start from - the same form opened from the wrong place
  # would still pass and prove nothing about the route that was meant to be tested.
  #
  # WHAT THIS FILE COVERS, AND WHAT GlobalTransfer.feature COVERS
  #
  # GlobalTransfer.feature drives one thing thoroughly: a UnionPay transfer from end to end,
  # out of a spreadsheet, through the receiver and template steps to a submitted order.
  #
  # This file covers the breadth instead - every area, every route, and the state each form
  # is in before anything is sent. Nothing here repeats what that file already does.
  #
  # NOTHING HERE IS SENT
  #
  # Every scenario stops on a form and never presses Next or Confirm. Sending moves real
  # money on the test environment and no test can undo it. This is enforced rather than
  # trusted: no step and no page object method presses either button, so a later scenario
  # cannot send one by accident.
  #
  # THE TWO ROUTES UNDER MAINTENANCE ARE TESTED, NOT SKIPPED
  #
  # Alipay and Wechat are labelled Maintenance and answer a click with "The service is
  # currently unavailable. Please try again later." That is a real product state, and
  # asserting it means this suite is what notices when the services come back.

  Background:
    Given I log into the UMPay application with valid email credentials using "1" of "sheet1" of "Login_TestData.xlsx"

  # ------------------------------------------------------------------
  # What each area offers
  # ------------------------------------------------------------------

  @transfer
  Scenario: The Transfer hub offers every route
    When I open the Transfer hub
    Then the Transfer hub should offer the route "UMPay to Existing template"
    And the Transfer hub should offer the route "UMPay to UMPay Wallet"
    And the Transfer hub should offer the route "UnionPay China"
    And the Transfer hub should offer the route "UnionPay Global"
    And the Transfer hub should offer the route "Transfer to Alipay"
    And the Transfer hub should offer the route "Transfer to Wechat"

  @transfer
  Scenario: Domestic Transfer offers the China routes
    When I open the Domestic Transfer area
    Then the Domestic Transfer area should offer the route "UnionPay China"
    And the Domestic Transfer area should offer the route "Transfer to Alipay"
    And the Domestic Transfer area should offer the route "Transfer to Wechat"

  @transfer
  Scenario: Global Transfer offers the international routes
    When I open the Global Transfer area
    Then the Global Transfer area should offer the route "UMPay Transfer to School Fees"
    And the Global Transfer area should offer the route "UMPay Transfer to Personal Bank Account"
    And the Global Transfer area should offer the route "UnionPay Global"
    And the Global Transfer area should offer the route "UMPay Transfer to USDT"

  # ------------------------------------------------------------------
  # The routes under maintenance
  # ------------------------------------------------------------------

  # The tiles say Maintenance and the click is refused. Both halves are asserted: a route
  # that stopped saying Maintenance while still refusing, or said it while quietly opening,
  # would be wrong in a way that only one of these two checks would catch.
  @transfer
  Scenario Outline: A payment app route under maintenance refuses to open
    When I open the Transfer hub
    Then the route "<route>" should be marked as under maintenance
    When I take the "<route>" route from the Transfer hub
    Then the app should say the service is unavailable

    Examples:
      | route              |
      | Transfer to Alipay |
      | Transfer to Wechat |

  # ------------------------------------------------------------------
  # UMPay to UMPay Wallet
  # ------------------------------------------------------------------

  # The web form identifies the recipient by phone number, where the Android app asks for
  # the wallet UUID - the same feature reached two different ways.
  @transfer
  Scenario: The wallet transfer form asks for everything a wallet transfer needs
    When I open the Transfer hub
    And I take the "UMPay to UMPay Wallet" route from the Transfer hub
    Then the UMPay wallet transfer form should be shown
    And the wallet form should show the sending wallet and its balance
    And the wallet form should ask for the recipient's phone number
    And the wallet form should ask for an amount and a remark

  # Next is on the page from the moment the form opens but carries disabled="true", so this
  # tests the validation rather than the layout - asserting the button merely exists would
  # pass on a completely empty form.
  @transfer
  Scenario: A wallet transfer cannot go forward from an empty form
    When I open the Transfer hub
    And I take the "UMPay to UMPay Wallet" route from the Transfer hub
    Then the UMPay wallet transfer form should be shown
    And the wallet transfer should not be ready to continue
    And the transfer is deliberately not sent

  # The form's main rule, and the reason there is no happy path scenario below it.
  #
  # The amount and remark boxes open carrying disabled="true" and stay locked until the
  # phone number resolves to a real UMPay account: you cannot name an amount until the
  # application knows who is receiving it. A number belonging to nobody is answered with
  # "User does not exist" and the boxes stay shut.
  #
  # Completing this form therefore needs the phone number of a real account on the test
  # environment, which is test data this suite does not have. When one is added to
  # Login_TestData.xlsx, the scenario to add here is: give that number, enter an amount,
  # and assert Next becomes enabled - the page object already has the methods for it.
  @transfer
  Scenario: A wallet transfer is refused when nobody holds the number
    When I open the Transfer hub
    And I take the "UMPay to UMPay Wallet" route from the Transfer hub
    Then the UMPay wallet transfer form should be shown
    And the wallet form should keep the amount and remark locked
    When I give "97300000" as the recipient's phone number
    Then the form should say the recipient does not exist
    And the wallet form should keep the amount and remark locked
    And the wallet transfer should not be ready to continue

  # ------------------------------------------------------------------
  # UnionPay, from both the areas that offer it
  # ------------------------------------------------------------------

  @transfer
  Scenario: UnionPay China states its limits before anything is entered
    When I open the Domestic Transfer area
    And I take the "UnionPay China" route from Domestic Transfer
    Then the UnionPay amount form should be shown
    And the UnionPay form should state "Card Available Balance"
    And the UnionPay form should state "Limit Min"
    And the UnionPay form should state "Limit Max"
    And the UnionPay transfer should not be ready to continue

  # The China route converts, so it has a second box for what the recipient receives. Filling
  # the paying box should fill the receiving one and open the way to the next step.
  @transfer
  Scenario: UnionPay China converts the amount and lets the transfer continue
    When I open the Domestic Transfer area
    And I take the "UnionPay China" route from Domestic Transfer
    Then the UnionPay amount form should be shown
    And the UnionPay form should offer a converted amount
    When I enter "50" as the UnionPay amount
    Then the converted amount should be worked out
    And the UnionPay transfer should be ready to continue
    And the transfer is deliberately not sent

  # Global is the same screen without the conversion, and asserting the absence is the point.
  @transfer
  Scenario: UnionPay Global offers the same form without a converted amount
    When I open the Global Transfer area
    And I take the "UnionPay Global" route from Global Transfer
    Then the UnionPay amount form should be shown
    And the UnionPay form should state "Limit Min"
    And the UnionPay form should state "Limit Max"
    And the UnionPay form should not offer a converted amount
    And the UnionPay transfer should not be ready to continue

  # ------------------------------------------------------------------
  # The Global Transfer payout routes
  # ------------------------------------------------------------------

  @transfer
  Scenario: A personal bank payout asks for a currency and an amount
    When I open the Global Transfer area
    And I take the "UMPay Transfer to Personal Bank Account" route from Global Transfer
    Then the "To Personal Bank Account" form should be shown
    And the payout form should ask for a currency and an amount
    And the payout form should state "Limit Min"
    And the payout should not be ready to confirm
    And the transfer is deliberately not sent

  @transfer
  Scenario: A USDT payout asks for a currency and an amount
    When I open the Global Transfer area
    And I take the "UMPay Transfer to USDT" route from Global Transfer
    Then the "To USDT" form should be shown
    And the payout form should ask for a currency and an amount
    And the payout form should state "Limit Min"
    And the payout should not be ready to confirm
    And the transfer is deliberately not sent

  # ------------------------------------------------------------------
  # School fees
  # ------------------------------------------------------------------

  # The same form the left navigation calls International School Fees. Its minimum is stated
  # as the amount box's placeholder rather than as text on the page, so the scenario names
  # the figure and a change to it fails somewhere a reader can see.
  @transfer
  Scenario: The school fee transfer form states its minimum
    When I open the Global Transfer area
    And I take the "UMPay Transfer to School Fees" route from Global Transfer
    Then the school fee form should be shown
    And the school fee form should show the sending wallet and its balance
    And the school fee form should ask for an amount and a remark
    And the school fee form should state a minimum of "100"
