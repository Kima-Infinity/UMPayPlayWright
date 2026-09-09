Feature: Security

  As a UMPay user
  I want to see and change what guards my account
  So that my password, my PIN and my authenticator are mine to control

  # WHAT THIS FILE COVERS
  #
  # The security page at /securities, reached from the profile drawer. Three things, each a row
  # carrying the address it opens:
  #
  #   Login Password      /securities/password    current, new and confirm, then Save
  #   PIN Code            /securities/pin         the same three, four digits each, then Save
  #   2FA Authenticator   /securities/two-factor  says whether it is connected, and offers to
  #                                               remove or reset it
  #
  # WHAT NOTHING HERE DOES, AND WHY
  #
  # Every credential the rest of the suite runs on is set on this page. A password changed here
  # would lock every other file out of this account, a PIN changed would fail every transfer that
  # enters one, and an authenticator removed would take the account with it.
  #
  # So the forms are filled in and Save is pressed for real - a form that is never submitted has
  # not been tested - but the values are only ever ones the application cannot accept, or cannot
  # accept to any effect:
  #
  #   nothing filled in at all;
  #   a new value left empty while the rest is filled;
  #   a new PIN mistyped in the confirm box;
  #   a new PIN equal to the one already set, so that being accepted changes nothing.
  #
  # Nothing on the authenticator page is pressed at all - it is read.
  #
  # WHAT IS DELIBERATELY NOT COVERED
  #
  # A wrong current password is never submitted: a run of those could lock the account the whole
  # suite signs in with. The sequential-PIN half of the page's rule is not covered either, because
  # proving it would mean asking for a PIN the rest of the suite does not know - and the scenario
  # below that submits a repeated digit already shows what becomes of that rule.
  #
  # THE DATA
  #
  # TestData/Security_TestData.xlsx carries the account and, row by row, what to put in the three
  # boxes. EMPTY means the box is left empty.

  # ------------------------------------------------------------------
  # What guards the account
  # ------------------------------------------------------------------

  @security @Security_TC_001
  Scenario Outline: The profile drawer opens what guards the account
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Security page
    Then the page should list what guards the account

    Examples:
      | excelFileName          | excelSheetName | row |
      | Security_TestData.xlsx | Sheet1         | 1   |

  # One of these quietly missing would leave a way into the account that the account holder can
  # no longer see, let alone change.
  @security @Security_TC_002
  Scenario Outline: The page offers everything the account is guarded by
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Security page
    Then what guards the account should be "Login Password, PIN Code, 2FA Authenticator"

    Examples:
      | excelFileName          | excelSheetName | row |
      | Security_TestData.xlsx | Sheet1         | 1   |

  # Whether a second factor is on is the single most important thing this page can tell somebody,
  # and the only place the account holder can find it out.
  @security @Security_TC_003
  Scenario Outline: The page says whether the authenticator is connected
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Security page
    Then the authenticator should say whether it is connected

    Examples:
      | excelFileName          | excelSheetName | row |
      | Security_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # The login password
  # ------------------------------------------------------------------

  @security @Security_TC_004
  Scenario Outline: Login Password opens the form that changes it
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Security page
    And I open "Login Password" from the security page
    Then the form that changes the login password should open

    Examples:
      | excelFileName          | excelSheetName | row |
      | Security_TestData.xlsx | Sheet1         | 1   |

  # A password typed in the clear is readable by whoever is behind the person typing it, which is
  # the one thing a page like this exists to prevent.
  @security @Security_TC_005
  Scenario Outline: Everything the password form asks for is hidden as it is typed
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Security page
    And I open "Login Password" from the security page
    Then everything it asks for should be hidden as it is typed

    Examples:
      | excelFileName          | excelSheetName | row |
      | Security_TestData.xlsx | Sheet1         | 1   |

  # An empty form that saves is an account whose password has become nothing at all.
  @security @Security_TC_006
  Scenario Outline: A password change with nothing filled in is refused
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Security page
    And I open "Login Password" from the security page
    And I fill the password form from "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I save the change
    Then the change should be refused

    Examples:
      | excelFileName          | excelSheetName | row |
      | Security_TestData.xlsx | Sheet1         | 2   |

  @security @Security_TC_007
  Scenario Outline: A password change with no new password is refused
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Security page
    And I open "Login Password" from the security page
    And I fill the password form from "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I save the change
    Then the change should be refused

    Examples:
      | excelFileName          | excelSheetName | row |
      | Security_TestData.xlsx | Sheet1         | 3   |

  # ------------------------------------------------------------------
  # The PIN
  # ------------------------------------------------------------------

  @security @Security_TC_008
  Scenario Outline: PIN Code opens the form that changes it, and says what a PIN may be
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Security page
    And I open "PIN Code" from the security page
    Then the form that changes the PIN should open, saying what a PIN may be

    Examples:
      | excelFileName          | excelSheetName | row |
      | Security_TestData.xlsx | Sheet1         | 1   |

  @security @Security_TC_009
  Scenario Outline: A PIN change with nothing filled in is refused
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Security page
    And I open "PIN Code" from the security page
    And I fill the PIN form from "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I save the change
    Then the change should be refused

    Examples:
      | excelFileName          | excelSheetName | row |
      | Security_TestData.xlsx | Sheet1         | 4   |

  # Mistyping the new PIN twice over is how somebody locks themselves out of their own money, so
  # the confirm box is the thing standing between them and that.
  @security @Security_TC_010
  Scenario Outline: A PIN change whose confirmation does not match is refused
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Security page
    And I open "PIN Code" from the security page
    And I fill the PIN form from "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I save the change
    Then the change should be refused

    Examples:
      | excelFileName          | excelSheetName | row |
      | Security_TestData.xlsx | Sheet1         | 5   |

  # The page states its own rule - a PIN may not be all the same digits - and this holds it to it.
  # The PIN submitted is the one the account already has, so whichever way the application
  # answers, the account is left on the PIN it started with.
  @security @Security_TC_011
  Scenario Outline: A PIN made of one digit repeated is refused, as the page requires
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Security page
    And I open "PIN Code" from the security page
    And I fill the PIN form from "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I save the change
    Then the PIN should be refused, as the page requires

    Examples:
      | excelFileName          | excelSheetName | row |
      | Security_TestData.xlsx | Sheet1         | 6   |

  # ------------------------------------------------------------------
  # The authenticator, and finding the way back
  # ------------------------------------------------------------------

  # Read and never pressed: removing this account's authenticator would take the account with it.
  @security @Security_TC_012
  Scenario Outline: The authenticator says it is connected and offers to remove or reset it
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Security page
    And I open "2FA Authenticator" from the security page
    Then the authenticator should be connected and offer to be removed or reset

    Examples:
      | excelFileName          | excelSheetName | row |
      | Security_TestData.xlsx | Sheet1         | 1   |

  @security @Security_TC_013
  Scenario Outline: Going back from a change form returns to what guards the account
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Security page
    And I open "Login Password" from the security page
    Then the form that changes the login password should open
    When I go back to what guards the account
    Then what guards the account should be listed again

    Examples:
      | excelFileName          | excelSheetName | row |
      | Security_TestData.xlsx | Sheet1         | 1   |
