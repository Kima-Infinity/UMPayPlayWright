Feature: Settings

  As a UMPay user
  I want to set how the application treats me
  So that it logs me out when I walk away and reaches me where I actually am

  # WHAT THIS FILE COVERS
  #
  # The settings at /settings, reached from the profile drawer. Four things, each a row carrying
  # the address it opens:
  #
  #   Auto Logout     /settings/logout         whether to be logged out when idle, after how many
  #                                            minutes, and whether an order in flight stops it
  #   About UMPay     /settings/about          the terms, and where UMPay can be followed
  #   Email           /settings/email          a new address, verified by a code sent to it
  #   Phone Number    /settings/phone-number   a new number, verified the same way
  #
  # WHAT NOTHING HERE DOES, AND WHY
  #
  # The email on this account is what the whole suite signs in with and reads its mail at, the
  # phone number is what it verifies with, and an auto logout of a few minutes would throw a run
  # out in the middle of a transfer.
  #
  # So the forms are opened, filled and pressed for real - a form that is never submitted has not
  # been tested - but nothing here saves an auto logout, and nothing here submits an address or a
  # number the application could accept. What is submitted is nothing at all, or an address that
  # is not an address: both can only be refused.
  #
  # Neither change could complete unseen in any case. Each ends in a code sent to the new address
  # or number, and no scenario here goes near that code - so the furthest any of this reaches is
  # the form itself.
  #
  # THE DATA
  #
  # TestData/Settings_TestData.xlsx carries the account and, row by row, what to put in the boxes.
  # EMPTY means the box is left empty.

  # ------------------------------------------------------------------
  # What can be set
  # ------------------------------------------------------------------

  @settings @Settings_TC_001
  Scenario Outline: The profile drawer opens the settings
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Settings page
    Then the settings should be shown

    Examples:
      | excelFileName          | excelSheetName | row |
      | Settings_TestData.xlsx | Sheet1         | 1   |

  @settings @Settings_TC_002
  Scenario Outline: The page offers everything that can be set
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Settings page
    Then the settings offered should be "Auto Logout, About UMPay, Email, Phone Number"

    Examples:
      | excelFileName          | excelSheetName | row |
      | Settings_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # Being logged out when idle
  # ------------------------------------------------------------------

  # An account that stays signed in on a shared machine is somebody else's money to spend.
  @settings @Settings_TC_003
  Scenario Outline: Auto Logout says how long the account may sit idle
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Settings page
    And I open "Auto Logout" from the settings
    Then the auto logout should say how long the account may sit idle

    Examples:
      | excelFileName          | excelSheetName | row |
      | Settings_TestData.xlsx | Sheet1         | 1   |

  # Being logged out in the middle of paying a school is worse than being logged out at all, so
  # the exception is worth having and worth checking is still offered.
  @settings @Settings_TC_004
  Scenario Outline: Auto Logout offers to leave an order in flight alone
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Settings page
    And I open "Auto Logout" from the settings
    Then it should offer to leave an order in flight alone

    Examples:
      | excelFileName          | excelSheetName | row |
      | Settings_TestData.xlsx | Sheet1         | 1   |

  # The minutes are the whole of the setting: a stepper that does not step leaves everybody on
  # whatever the account was given.
  @settings @Settings_TC_005
  Scenario Outline: The minutes can be put up and down
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Settings page
    And I open "Auto Logout" from the settings
    And I put the minutes up
    Then the minutes should have gone up
    When I put the minutes down
    Then the minutes should be back where they started

    Examples:
      | excelFileName          | excelSheetName | row |
      | Settings_TestData.xlsx | Sheet1         | 1   |

  # Nothing is saved here on purpose, so this is also the check that nothing was: an auto logout
  # changed by accident would throw a later run out in the middle of a transfer.
  @settings @Settings_TC_006
  Scenario Outline: Leaving the auto logout without saving leaves it as it was
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Settings page
    And I open "Auto Logout" from the settings
    And I put the minutes up
    And I leave the auto logout without saving
    Then the auto logout should be as it was

    Examples:
      | excelFileName          | excelSheetName | row |
      | Settings_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # What UMPay says about itself
  # ------------------------------------------------------------------

  # The terms are the one thing on this page somebody may need in an argument about their money.
  @settings @Settings_TC_007
  Scenario Outline: About UMPay carries the terms and where UMPay can be followed
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Settings page
    And I open "About UMPay" from the settings
    Then it should offer "Terms and Conditions, Facebook, Instagram, X, Youtube"

    Examples:
      | excelFileName          | excelSheetName | row |
      | Settings_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # The address and the number
  # ------------------------------------------------------------------

  @settings @Settings_TC_008
  Scenario Outline: Changing the email asks for the new address and a captcha
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Settings page
    And I open "Email" from the settings
    Then the form should ask for "email, captcha"

    Examples:
      | excelFileName          | excelSheetName | row |
      | Settings_TestData.xlsx | Sheet1         | 1   |

  # An empty form that went through would begin moving this account to no address at all.
  @settings @Settings_TC_009
  Scenario Outline: Changing the email is refused when nothing is filled in
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Settings page
    And I open "Email" from the settings
    And I fill the email form from "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I go on from the form
    Then the form should refuse to go on

    Examples:
      | excelFileName          | excelSheetName | row |
      | Settings_TestData.xlsx | Sheet1         | 2   |

  # A code sent to something that is not an address is a code nobody receives, and an account
  # halfway to an address that cannot be reached.
  @settings @Settings_TC_010
  Scenario Outline: Changing the email is refused when the address is not an address
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Settings page
    And I open "Email" from the settings
    And I fill the email form from "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I go on from the form
    Then the form should refuse to go on

    Examples:
      | excelFileName          | excelSheetName | row |
      | Settings_TestData.xlsx | Sheet1         | 3   |

  @settings @Settings_TC_011
  Scenario Outline: Changing the phone number asks for a country, a number and a captcha
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Settings page
    And I open "Phone Number" from the settings
    Then the form should ask for "phoneCountry, phone, captcha"

    Examples:
      | excelFileName          | excelSheetName | row |
      | Settings_TestData.xlsx | Sheet1         | 1   |

  @settings @Settings_TC_012
  Scenario Outline: Changing the phone number is refused when nothing is filled in
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Settings page
    And I open "Phone Number" from the settings
    And I fill the phone form from "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I go on from the form
    Then the form should refuse to go on

    Examples:
      | excelFileName          | excelSheetName | row |
      | Settings_TestData.xlsx | Sheet1         | 2   |

  @settings @Settings_TC_013
  Scenario Outline: Going back from a setting returns to the settings
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Settings page
    And I open "About UMPay" from the settings
    When I go back to the settings
    Then the settings should be listed again

    Examples:
      | excelFileName          | excelSheetName | row |
      | Settings_TestData.xlsx | Sheet1         | 1   |
