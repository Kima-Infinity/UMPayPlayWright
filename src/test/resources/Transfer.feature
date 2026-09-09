Feature: UMPay transfers

  As a UMPay user
  I want to reach every way the application can move money out of my wallet
  So that each transfer route is known to open, validate and price correctly

  # THE TRANSFER PAGE, AND ONLY IT
  #
  # This file covers /v2/transfer - the hub the left navigation calls Transfer - and the forms
  # its own tiles open. It offers six routes:
  #
  #   UMPay to Existing template   a payout account the account already holds
  #   UMPay to UMPay Wallet        another UMPay account, by phone number
  #   UnionPay China               the converting UnionPay form
  #   UnionPay Global              the same form without the conversion
  #   Transfer to Alipay           under maintenance
  #   Transfer to Wechat           under maintenance
  #
  # The other two transfer areas have files of their own, because a route reached from the
  # wrong place would pass and prove nothing about the one that was meant to be tested:
  #
  #   DomesticTransfer.feature     /domestic-transfer and the UnionPay China form in full
  #   GlobalTransfer.feature       /global-transfer, its payout routes, and UnionPay end to end
  #   InternationalSchoolFees.feature   the school fee form, from both ways in
  #
  # THE DATA
  #
  # TestData/Transfer_TestData.xlsx carries the account and the handful of values these
  # scenarios need: the number nobody holds and the saved template a transfer pays. Route names
  # and the messages the application shows stay in the scenarios, because those are what is
  # being asserted rather than what is being fed in.
  #
  # Every scenario signs in for itself rather than from a Background, and does it from the row
  # its own data lives on - so the row a scenario is driven by is visible in the scenario
  # instead of having to be carried in from the top of the file.
  #
  # WHAT SENDS AND WHAT DOES NOT
  #
  # Scenarios tagged @sends press the button and the money goes. They are tagged so a run can
  # leave them out - mvn test -Dcucumber.filter.tags="not @sends" - and each of them sends the
  # smallest amount the form states it will take, read off the form rather than written here.
  #
  # Everything else stops on a form. The @negative scenarios are the cheapest half of that: a
  # figure below the stated minimum never reaches a send at all, so a refusal can be proved
  # without spending anything, and each route that sends has one beside it.
  #
  # A transfer that is sent has to arrive. Every sending scenario asserts the outcome the
  # application reports - an order that settles, a PIN that is accepted, a platform that says
  # nothing against it - because a form that clears itself proves only that the button worked.
  #
  # THE TWO ROUTES UNDER MAINTENANCE ARE TESTED, NOT SKIPPED
  #
  # Alipay and Wechat are labelled Maintenance and answer a click with "The service is
  # currently unavailable. Please try again later." That is a real product state, and
  # asserting it means this suite is what notices when the services come back.

  # ------------------------------------------------------------------
  # What each area offers
  # ------------------------------------------------------------------

  @transfer @Transfer_TC_001
  Scenario Outline: The Transfer hub offers all different types of transfer options
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer hub
    Then the Transfer page should be at "/v2/transfer"
    And the Transfer hub should offer the route "UMPay to Existing template"
    And the Transfer hub should offer the route "UMPay to UMPay Wallet"
    And the Transfer hub should offer the route "UnionPay China"
    And the Transfer hub should offer the route "UnionPay Global"
    And the Transfer hub should offer the route "Transfer to Alipay"
    And the Transfer hub should offer the route "Transfer to Wechat"

    Examples:
      | excelFileName          | excelSheetName | row |
      | Transfer_TestData.xlsx | sheet1         | 1   |

  # ------------------------------------------------------------------
  # The routes under maintenance
  # ------------------------------------------------------------------

  # The tiles say Maintenance and the click is refused. Both halves are asserted: a route
  # that stopped saying Maintenance while still refusing, or said it while quietly opening,
  # would be wrong in a way that only one of these two checks would catch.
  @transfer @Transfer_TC_002
  Scenario Outline: A payment option under maintenance should not open
    Given I log into the UMPay application with valid email credentials using "1" of "sheet1" of "Transfer_TestData.xlsx"
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



  # Next is on the page from the moment the form opens but carries disabled="true", so this
  # tests the validation rather than the layout - asserting the button merely exists would
  # pass on a completely empty form.
  @transfer @Transfer_TC_003
  Scenario Outline: A wallet transfer cannot go forward from an empty form
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer hub
    And I take the "UMPay to UMPay Wallet" route from the Transfer hub
    Then the UMPay wallet transfer form should be shown
    And the wallet transfer should not be ready to continue

    Examples:
      | excelFileName          | excelSheetName | row |
      | Transfer_TestData.xlsx | sheet1         | 1   |

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
  @transfer @Transfer_TC_004
  Scenario Outline: A wallet transfer is refused when nobody holds the mobile number
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer hub
    And I take the "UMPay to UMPay Wallet" route from the Transfer hub
    Then the UMPay wallet transfer form should be shown
    And the wallet form should keep the amount and remark locked
    When I give the recipient's phone number in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the form should say the recipient does not exist
    And the wallet form should keep the amount and remark locked
    And the wallet transfer should not be ready to continue

    Examples:
      | excelFileName          | excelSheetName | row |
      | Transfer_TestData.xlsx | sheet1         | 1   |

  # The same paths, refused. An identifier nobody holds has to leave the form shut: the amount
  # box opening for a recipient the application could not find would mean money could be aimed
  # at nobody.
  #
  # One scenario each rather than a single outline over both. They are different lookups
  # answering the same way today, and if one of them ever stops refusing it should be that
  # scenario that fails and says so by name.

  @transfer @negative @Transfer_TC_005
  Scenario Outline: A recipient nobody holds is refused when named by ID
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer hub
    And I take the "UMPay to UMPay Wallet" route from the Transfer hub
    Then the UMPay wallet transfer form should be shown
    When I name the recipient by "ID"
    And I give the recipient's "ID" from "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the form should say the recipient does not exist
    And the wallet form should keep the amount and remark locked
    And the wallet transfer should not be ready to continue

    Examples:
      | excelFileName          | excelSheetName | row |
      | Transfer_TestData.xlsx | sheet1         | 10  |

  @transfer @negative @Transfer_TC_006
  Scenario Outline: A recipient nobody holds is refused when named by Email
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer hub
    And I take the "UMPay to UMPay Wallet" route from the Transfer hub
    Then the UMPay wallet transfer form should be shown
    When I name the recipient by "EMAIL"
    And I give the recipient's "EMAIL" from "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the form should say the recipient does not exist
    And the wallet form should keep the amount and remark locked
    And the wallet transfer should not be ready to continue

    Examples:
      | excelFileName          | excelSheetName | row |
      | Transfer_TestData.xlsx | sheet1         | 11  |

  # The route's happy path, which was not possible at all before: the amount and remark stay
  # locked until the recipient resolves to a real account, and the suite had nobody to name.
  #
  # One scenario for each way of naming them rather than a single outline over the three. They
  # are different lookups reaching the same send, and a failure should name the one that broke -
  # a transfer that stops working by email while the other two are fine is a specific fault, and
  # a shared outline would report it as a row of a table.
  #
  # Each sends the amount its row holds, small on purpose: this moves money to somebody else's
  # account. A mobile number is two things - a dialling code and the number - and the sheet
  # writes them together the way a person would.

  @transfer @sends @Transfer_TC_007
  Scenario Outline: A wallet transfer is sent to a recipient named by ID
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer hub
    And I take the "UMPay to UMPay Wallet" route from the Transfer hub
    Then the UMPay wallet transfer form should be shown
    And the wallet form should keep the amount and remark locked
    When I name the recipient by "ID"
    And I give the recipient's "ID" from "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the form should find the recipient
    When I enter the amount in "<row>" of "<excelSheetName>" of "<excelFileName>" to send
    Then the wallet transfer should be ready to continue
    When I send the wallet transfer, which moves real money
    Then the wallet transfer should go through

    Examples:
      | excelFileName          | excelSheetName | row |
      | Transfer_TestData.xlsx | sheet1         | 8  |

  @transfer @sends @Transfer_TC_008
  Scenario Outline: A wallet transfer is sent to a recipient named by Email
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer hub
    And I take the "UMPay to UMPay Wallet" route from the Transfer hub
    Then the UMPay wallet transfer form should be shown
    And the wallet form should keep the amount and remark locked
    When I name the recipient by "EMAIL"
    And I give the recipient's "EMAIL" from "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the form should find the recipient
    When I enter the amount in "<row>" of "<excelSheetName>" of "<excelFileName>" to send
    Then the wallet transfer should be ready to continue
    When I send the wallet transfer, which moves real money
    Then the wallet transfer should go through

    Examples:
      | excelFileName          | excelSheetName | row |
      | Transfer_TestData.xlsx | sheet1         | 9  |

  @transfer @sends @Transfer_TC_009
  Scenario Outline: A wallet transfer is sent to a recipient named by Mobile
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer hub
    And I take the "UMPay to UMPay Wallet" route from the Transfer hub
    Then the UMPay wallet transfer form should be shown
    And the wallet form should keep the amount and remark locked
    When I name the recipient by "MOBILE"
    And I give the recipient's "MOBILE" from "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the form should find the recipient
    When I enter the amount in "<row>" of "<excelSheetName>" of "<excelFileName>" to send
    Then the wallet transfer should be ready to continue
    When I send the wallet transfer, which moves real money
    Then the wallet transfer should go through

    Examples:
      | excelFileName          | excelSheetName | row |
      | Transfer_TestData.xlsx | sheet1         | 12  |

  # ------------------------------------------------------------------
  # UnionPay, from this page
  # ------------------------------------------------------------------
  #
  # Both UnionPay routes are reachable from somewhere else as well - China from Domestic
  # Transfer, Global from Global Transfer - and the forms are covered there. What these prove is
  # what this page is responsible for: that its tiles open the right form and that a transfer
  # started here goes through. A route that opened the converting form where it should not would
  # pass every scenario in those files and still be wrong on this page.
  #
  # Both send. The amount is the smallest the form states, the receiver is one the account has
  # already saved - China pays the card in row 2, Global the one in row 7 - and the PIN comes
  # from -Dumpay.pin because it is a credential rather than data.
  #
  # The converted amount is asserted on the way past rather than in a scenario of its own: it is
  # the one thing that tells these two routes apart, and it is worth knowing that the transfer
  # that went through was priced by the form that should have priced it.

  @transfer @sends @Transfer_TC_010
  Scenario Outline: A UnionPay China transfer from the Transfer page converts and is sent
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer hub
    And I take the "UnionPay China" route from the Transfer hub
    Then the UnionPay amount form should be shown
    And the UnionPay form should state "Limit Min"
    And the UnionPay form should state "Limit Max"
    And the UnionPay form should offer a converted amount
    When I enter the stated minimum as the UnionPay amount
    Then the converted amount should be worked out
    And the UnionPay transfer should be ready to continue
    When I go on to the receiver step
    And I send it to the saved receiver in "<row>" of "<excelSheetName>" of "<excelFileName>", which moves real money
    Then the transfer should go through

    Examples:
      | excelFileName          | excelSheetName | row |
      | Transfer_TestData.xlsx | sheet1         | 2   |

  # The same screen without the conversion, and the absence is asserted before anything is sent:
  # this route pays out in the currency it is funded in, so a converted amount box appearing here
  # would mean the wrong form had opened and the transfer would be priced by the wrong one.
  @transfer @sends @Transfer_TC_011
  Scenario Outline: A UnionPay Global transfer from the Transfer page does not convert and is sent
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer hub
    And I take the "UnionPay Global" route from the Transfer hub
    Then the UnionPay amount form should be shown
    And the UnionPay form should state "Limit Min"
    And the UnionPay form should state "Limit Max"
    And the UnionPay form should not offer a converted amount
    When I enter the stated minimum as the UnionPay amount
    Then the UnionPay transfer should be ready to continue
    When I go on to the receiver step
    And I send it to the saved receiver in "<row>" of "<excelSheetName>" of "<excelFileName>", which moves real money
    Then the transfer should go through

    Examples:
      | excelFileName          | excelSheetName | row |
      | Transfer_TestData.xlsx | sheet1         | 7   |

  # ------------------------------------------------------------------
  # UMPay to Existing template
  # ------------------------------------------------------------------
  #
  # The route the hub offers first, and the only one that starts from what the account already
  # knows rather than from an empty form. It opens /v2/template - a Select Template list of the
  # destinations saved on the account - and choosing one opens the UnionPay transfer form for
  # that account, with the destination already settled.
  #
  # Two things are asserted about the list itself. That it offers something at all, because a
  # route that opens an empty list is a route that cannot be used. And that every destination is
  # masked: showing which accounts this user pays is the point of the list, showing them in full
  # would not be. Two of them pay an email address rather than a card number, so what is held to
  # is the masking rather than the shape.
  #
  # This route ends where the others do not. Domestic and Global Transfer open a form carrying
  # Next and go on to a summary; choosing a saved template settles the destination and opens the
  # form that sends, whose button reads Transfer. It is read and never pressed.
  #
  # Worth knowing, and deliberately not asserted either way: Transfer is enabled the moment the
  # form opens, with no amount entered and the card balance reading US$ 0. Every other form in
  # this application keeps its submit disabled until it has what it needs. Whether pressing this
  # one would be refused or would send is not something a test can ask - pressing it moves real
  # money - so the run reports the state and stops there.
  #
  # Nothing is sent here either. Choosing a template opens the amount form and stops.

  @transfer @Transfer_TC_012
  Scenario Outline: The Existing template route lists the destinations saved on the account
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer hub
    And I take the "UMPay to Existing template" route from the Transfer hub
    Then the saved templates should be listed
    And every saved template should mask the account it pays

    Examples:
      | excelFileName          | excelSheetName | row |
      | Transfer_TestData.xlsx | sheet1         | 3   |

  # ------------------------------------------------------------------
  # Sending one, for real
  # ------------------------------------------------------------------
  #
  # The exception to the rule at the top of this file, and it is tagged so it can be left out:
  # this scenario presses Transfer and the money goes. Every other scenario here stops on a
  # form. Run it deliberately with -Dcucumber.filter.tags="@sends".
  #
  # The amount is the smallest the form says it will take, read off its own "Limit Min" line
  # rather than written down here, because the band belongs to the route and the currency.
  #
  # The outcome is not read off the screen, because this route puts nothing there. A refused
  # transfer draws no dialog, prints no message, and returns to the hub looking exactly as it
  # would if the money had gone - so a test trusting the screen would call a refusal a success.
  # What the platform actually answers is in the call the page made, and that is what is
  # asserted.
  #
  # At the time of writing it fails, and the failure is a real one: the platform answers
  # POST /api/orders/global-transfers/check-balance with 400 and "This service not available
  # please try again with other methods" - the same refusal the deposits and withdraws meet -
  # against a card whose available balance reads US$ 0. The application tells the user none of
  # this, which is worth more attention than the refusal itself.

  @transfer @sends @Transfer_TC_013
  Scenario Outline: A transfer to a saved template is sent
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer hub
    And I take the "UMPay to Existing template" route from the Transfer hub
    Then the saved templates should be listed
    When I choose the saved template in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the UnionPay amount form should be shown
    And the transfer should be going to the account in "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I enter the stated minimum as the UnionPay amount
    And I send the transfer, which moves real money
    Then the transfer should be accepted

    Examples:
      | excelFileName          | excelSheetName | row |
      | Transfer_TestData.xlsx | sheet1         | 3   |

  # ------------------------------------------------------------------
  # Sending to yourself
  # ------------------------------------------------------------------
  #
  # Naming your own account as the recipient. Every payment product refuses this and nothing here
  # had ever asked whether UMPay does - the recipient scenarios above all name somebody who does
  # not exist, which is a different question entirely: this one names somebody who very much does.
  #
  # Nothing is sent. The scenario stops at whether the form will let the transfer be built at
  # all, which is where the refusal belongs - money that has gone out and come back to the same
  # wallet has still been through the fee and the ledger.

  @transfer @negative @Transfer_TC_014
  Scenario Outline: A transfer aimed at the account making it is refused
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer hub
    And I take the "UMPay to UMPay Wallet" route from the Transfer hub
    Then the UMPay wallet transfer form should be shown
    When I name the recipient by "EMAIL"
    And I give the recipient's "EMAIL" from "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the wallet transfer should not be ready to continue

    Examples:
      | excelFileName          | excelSheetName | row |
      | Transfer_TestData.xlsx | sheet1         | 13  |
