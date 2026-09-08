Feature: Domestic Transfer

  As a UMPay user
  I want the Domestic Transfer area to offer the China routes and price them correctly
  So that a transfer within China is known to open, validate and send

  # WHAT THIS FILE COVERS
  #
  # The /domestic-transfer area and everything reachable from it: the three routes it offers,
  # the two that are under maintenance, and the UnionPay China form in full - what it states,
  # what it refuses, what it converts, and sending one.
  #
  # These scenarios used to live in Transfer.feature, which covers the breadth of all three
  # transfer areas. Depth for one area belongs in its own file, the way GlobalTransfer.feature
  # already holds the depth for global transfers.
  #
  # THE DATA
  #
  # TestData/DomesticTransfer_TestData.xlsx carries the account, the saved receiver a sent
  # transfer pays, the route names and the message the maintenance routes answer with. Amounts
  # are not in it: the form states its own band and the scenarios read it, because the band is
  # the platform's and moves.
  #
  # Every scenario signs in for itself rather than from a Background, and does it from the row
  # its own data lives on - so the row a scenario is driven by is visible in the scenario
  # instead of having to be carried in from the top of the file. The maintenance outline signs
  # in from whichever row its example names, which is the row the route comes from too.
  #
  # WHAT SENDS
  #
  # One scenario, tagged @sends, and it moves real money. Leave it out with
  # -Dcucumber.filter.tags="not @sends". The @negative scenarios prove the refusals without
  # spending anything, which is why each sits beside the send.

  # ------------------------------------------------------------------
  # What the area offers
  # ------------------------------------------------------------------

  @domestic @Domestic_Transfer_TC_005
  Scenario Outline: Domestic Transfer offers exactly the China routes
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Domestic Transfer area
    Then the Domestic Transfer area should offer exactly its own routes

    Examples:
      | excelFileName                  | excelSheetName | row |
      | DomesticTransfer_TestData.xlsx | sheet1         | 1   |

  # The tiles say Maintenance and the click is refused. Asserted from this area rather than from
  # the Transfer hub, because the same route reached from the wrong place would pass and prove
  # nothing about this one.
  @domestic @negative @Domestic_Transfer_TC_006
  Scenario Outline: A payment app route under maintenance refuses to open
    Given I log into the UMPay application with valid email credentials using "<row>" of "<sheet>" of "<file>"
    When I open the Domestic Transfer area
    Then the Domestic Transfer route in "<row>" of "<sheet>" of "<file>" should be marked as under maintenance
    When I take the route in "<row>" of "<sheet>" of "<file>" from Domestic Transfer
    Then Domestic Transfer should refuse it with the message in "<row>" of "<sheet>" of "<file>"

    Examples:
      | file                             | sheet  | row |
      | DomesticTransfer_TestData.xlsx   | sheet1 | 2   |
      | DomesticTransfer_TestData.xlsx   | sheet1 | 3   |

  # Every route in the area is tried, not just the two known to be under maintenance. The point
  # is the ones nobody has thought to name: a route that starts saying Maintenance is caught
  # without anybody adding a scenario for it, and so is one that stops saying it while still
  # refusing to open. Each route either opens or refuses with the message the sheet records.

  # ------------------------------------------------------------------
  # The UnionPay China form
  # ------------------------------------------------------------------

  @domestic @Domestic_Transfer_TC_007
  Scenario Outline: UnionPay China states its limits before anything is entered
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Domestic Transfer area
    And I take the "UnionPay China" route from Domestic Transfer
    Then the UnionPay amount form should be shown
    And the UnionPay form should state "Limit Min"
    And the UnionPay form should state "Limit Max"
    And the UnionPay transfer should not be ready to continue

    Examples:
      | excelFileName                  | excelSheetName | row |
      | DomesticTransfer_TestData.xlsx | sheet1         | 1   |

  # The China route converts, so it has a second box for what the recipient receives. Filling
  # the paying box should fill the receiving one.
  @domestic @Domestic_Transfer_TC_008
  Scenario Outline: UnionPay China works out what the recipient receives
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Domestic Transfer area
    And I take the "UnionPay China" route from Domestic Transfer
    Then the UnionPay amount form should be shown
    And the UnionPay form should offer a converted amount
    When I enter the stated minimum as the UnionPay amount
    Then the converted amount should be worked out

    Examples:
      | excelFileName                  | excelSheetName | row |
      | DomesticTransfer_TestData.xlsx | sheet1         | 1   |

  # Both ends of the band, proved without sending anything.
  @domestic @negative @Domestic_Transfer_TC_004
  Scenario Outline: UnionPay China will not go on with less than its stated minimum
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Domestic Transfer area
    And I take the "UnionPay China" route from Domestic Transfer
    Then the UnionPay amount form should be shown
    When I enter one below the stated minimum as the UnionPay amount
    Then the UnionPay transfer should not be ready to continue

    Examples:
      | excelFileName                  | excelSheetName | row |
      | DomesticTransfer_TestData.xlsx | sheet1         | 1   |

  @domestic @negative @Domestic_Transfer_TC_003
  Scenario Outline: UnionPay China will not go on with more than its stated maximum
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Domestic Transfer area
    And I take the "UnionPay China" route from Domestic Transfer
    Then the UnionPay amount form should be shown
    When I enter one above the stated maximum as the UnionPay amount
    Then the UnionPay transfer should not be ready to continue

    Examples:
      | excelFileName                  | excelSheetName | row |
      | DomesticTransfer_TestData.xlsx | sheet1         | 1   |

  # ------------------------------------------------------------------
  # Sending one
  # ------------------------------------------------------------------

  # This moves real money. The receiver is one the account has already saved, named in the sheet,
  # and the PIN comes from -Dumpay.pin because it is a credential rather than data.
  #
  # It fails at the time of writing, and the failure is the platform's rather than the suite's:
  # Next stays disabled with an amount inside the stated band, the form no longer shows the Card
  # Available Balance line it used to, and the account appears to have no UnionPay card to send
  # from. The scenario is written so that it starts passing the day there is one.
  @domestic @sends @Domestic_Transfer_TC_002
  Scenario Outline: A UnionPay China transfer is sent to a saved receiver
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Domestic Transfer area
    And I take the "UnionPay China" route from Domestic Transfer
    Then the UnionPay amount form should be shown
    When I enter the stated minimum as the UnionPay amount
    Then the UnionPay transfer should be ready to continue
    When I go on to the receiver step
    And I send it to the saved receiver in "<row>" of "<excelSheetName>" of "<excelFileName>", which moves real money
    Then the transfer should go through

    Examples:
      | excelFileName                  | excelSheetName | row |
      | DomesticTransfer_TestData.xlsx | sheet1         | 1   |

  # The other way to say who is being paid: a card typed in with the person it belongs to,
  # their purpose and their source of funds, rather than one the account has saved. They are
  # different forms reaching the same send, and only the saved one was covered.
  #
  # This moves real money too. The receiver is in row 5 of the sheet - the same card
  # GlobalTransfer_TestData uses for a new receiver within China - and the PIN comes from
  # -Dumpay.pin.
  #
  # It fails today for the reason the scenario above it does, and the failure is the platform's:
  # Next stays disabled with an amount inside the stated band, and the form no longer shows the
  # Card Available Balance it used to. Both start passing the day the account has a UnionPay
  # card to send from.
  @domestic @sends @Domestic_Transfer_TC_001
  Scenario Outline: A UnionPay China transfer is sent to a new receiver
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Domestic Transfer area
    And I take the "UnionPay China" route from Domestic Transfer
    Then the UnionPay amount form should be shown
    When I enter the stated minimum as the UnionPay amount
    Then the converted amount should be worked out
    And the UnionPay transfer should be ready to continue
    When I go on to the receiver step
    And I send it to a new receiver in "<row>" of "<excelSheetName>" of "<excelFileName>", which moves real money
    Then the transfer should go through

    Examples:
      | excelFileName                  | excelSheetName | row |
      | DomesticTransfer_TestData.xlsx | sheet1         | 5   |
