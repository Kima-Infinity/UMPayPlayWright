Feature: Transfer Fee Setting

  As a UMPay user
  I want to say once and for all who pays the fee on my transfers
  So that I am not asked the same question on every transfer I make

  # WHAT THIS FILE COVERS
  #
  # The transfer fee setting at /settings/transfer-fee, reached from the profile drawer. One
  # setting for the whole account, offered as three ways of settling the fee and a Save that
  # only comes alive once one of them has been changed:
  #
  #   Fee will be selected by User             the sender is asked, transfer by transfer
  #   Fee will always be paid by other party
  #   Fee will always be paid by me
  #
  # Each way is a label around a radio, and the radio carries the word the application knows the
  # setting by - user, sender, receiver - which is what a failure message names when the words on
  # the screen would be ambiguous.
  #
  # WHAT SAVING HERE COSTS
  #
  # This is the one setting in the suite that the transfer flows read: put the account on "always
  # paid by me" and the transfer form stops asking who pays, and the Transfer scenarios that
  # answer that question would fail in another file entirely. It is still saved for real here,
  # because a setting page that is never saved is a setting page that has not been tested - so
  # every scenario that saves a change reads what the account was on before it touched anything,
  # and puts it back before it ends. The account finishes each scenario the way it started it.
  #
  # THE DATA
  #
  # TestData/TransferFeeSetting_TestData.xlsx carries the account and, row by row, the way of
  # settling the fee that scenario is about - so that all three ways are covered rather than one
  # standing in for the others.

  # ------------------------------------------------------------------
  # What the page offers
  # ------------------------------------------------------------------

  @transferfee @Transfer_Fee_Setting_TC_001
  Scenario Outline: The profile drawer opens the transfer fee setting
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer Fee Setting page
    Then the transfer fee setting should be shown

    Examples:
      | excelFileName                    | excelSheetName | row |
      | TransferFeeSetting_TestData.xlsx | Sheet1         | 1   |

  # A way that quietly disappeared would take a whole way of settling a fee with it, and the
  # first anybody would know of it is a transfer that charged the wrong person.
  @transferfee @Transfer_Fee_Setting_TC_002
  Scenario Outline: The page offers every way the fee can be settled
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer Fee Setting page
    Then the ways of settling the fee should be "Fee will be selected by User, Fee will always be paid by other party, Fee will always be paid by me"

    Examples:
      | excelFileName                    | excelSheetName | row |
      | TransferFeeSetting_TestData.xlsx | Sheet1         | 1   |

  # A setting with nothing chosen is a question the account has never answered, and a setting
  # with two chosen is one nobody can act on.
  @transferfee @Transfer_Fee_Setting_TC_003
  Scenario Outline: Exactly one way is chosen, and the page says which
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer Fee Setting page
    Then exactly one way of settling the fee should be chosen

    Examples:
      | excelFileName                    | excelSheetName | row |
      | TransferFeeSetting_TestData.xlsx | Sheet1         | 1   |

  # Save being alive on a page nobody has touched invites somebody to press it and wonder what
  # they just changed.
  @transferfee @Transfer_Fee_Setting_TC_004
  Scenario Outline: There is nothing to save until something is changed
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer Fee Setting page
    Then the page should not offer to save anything

    Examples:
      | excelFileName                    | excelSheetName | row |
      | TransferFeeSetting_TestData.xlsx | Sheet1         | 1   |

  @transferfee @Transfer_Fee_Setting_TC_005
  Scenario Outline: Choosing another way offers to save it
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer Fee Setting page
    And I choose the way of settling the fee named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the page should offer to save it

    Examples:
      | excelFileName                    | excelSheetName | row |
      | TransferFeeSetting_TestData.xlsx | Sheet1         | 2   |

  # Three ways of settling one fee, of which two would contradict the third.
  @transferfee @Transfer_Fee_Setting_TC_006
  Scenario Outline: Choosing a way leaves only that way chosen
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer Fee Setting page
    And I choose the way of settling the fee named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then only that way should be chosen

    Examples:
      | excelFileName                    | excelSheetName | row |
      | TransferFeeSetting_TestData.xlsx | Sheet1         | 2   |

  # ------------------------------------------------------------------
  # Saving it, one way at a time, and putting it back
  # ------------------------------------------------------------------

  @transferfee @Transfer_Fee_Setting_TC_007
  Scenario Outline: The fee can be set to always be paid by me, and it stays
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer Fee Setting page
    And I put the fee setting on the way named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I save the fee setting
    Then the fee setting should still be that way when the page is opened again
    When I put the fee setting back the way it was
    Then the fee setting should be what it was before

    Examples:
      | excelFileName                    | excelSheetName | row |
      | TransferFeeSetting_TestData.xlsx | Sheet1         | 2   |

  @transferfee @Transfer_Fee_Setting_TC_008
  Scenario Outline: The fee can be set to always be paid by the other party, and it stays
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer Fee Setting page
    And I put the fee setting on the way named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I save the fee setting
    Then the fee setting should still be that way when the page is opened again
    When I put the fee setting back the way it was
    Then the fee setting should be what it was before

    Examples:
      | excelFileName                    | excelSheetName | row |
      | TransferFeeSetting_TestData.xlsx | Sheet1         | 3   |

  # The way the account is already on, which still has to be arrived at rather than assumed: the
  # setting is moved off it and back, so that saving it is a real save and not a page that was
  # never touched.
  @transferfee @Transfer_Fee_Setting_TC_009
  Scenario Outline: The fee can be set to be chosen transfer by transfer, and it stays
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer Fee Setting page
    And I put the fee setting on the way named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I save the fee setting
    Then the fee setting should still be that way when the page is opened again
    When I put the fee setting back the way it was
    Then the fee setting should be what it was before

    Examples:
      | excelFileName                    | excelSheetName | row |
      | TransferFeeSetting_TestData.xlsx | Sheet1         | 4   |

  # A choice that survives leaving the page without being saved is a setting changed by accident.
  @transferfee @Transfer_Fee_Setting_TC_010
  Scenario Outline: Leaving without saving leaves the setting as it was
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Transfer Fee Setting page
    And I choose the way of settling the fee named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I leave the page without saving
    Then the fee setting should be what it was before

    Examples:
      | excelFileName                    | excelSheetName | row |
      | TransferFeeSetting_TestData.xlsx | Sheet1         | 3   |
