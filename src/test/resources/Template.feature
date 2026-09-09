Feature: Template

  As a UMPay user
  I want to keep the destinations I send money to
  So that a transfer I make often is a name I choose rather than an account I retype

  # WHAT THIS FILE COVERS
  #
  # The saved templates at /v2/template, reached from the profile drawer. A template is a
  # destination the account holder has already named: a card number, a wallet, a school. The
  # transfer flows draw on these - the Transfer hub's "UMPay to Existing template" route offers
  # this same list - so what is saved here decides what those flows can send without retyping.
  #
  # A template is shown as a card: the icon of the way it was saved for, the name it was given,
  # and under it the account it pays, masked - "**********000004" for a card,
  # "ash*******@mailinator.com" for a wallet. Every card offers two things: a pencil to change it
  # and a bin to remove it. The card itself does nothing when clicked, which is the reason the
  # scenario that opens one goes through the pencil.
  #
  # Add Template opens /v2/template/option, which offers the ways a template can be saved for -
  # a UMPay wallet, UnionPay China, UnionPay Global, school fees, and two that the page marks
  # Maintenance rather than opens.
  #
  # WHAT NOTHING HERE DOES
  #
  # No scenario saves a template and no scenario removes one. The bin is counted and reported on,
  # never clicked: these are the templates the Transfer scenarios choose from by name, and one
  # removed here would fail a scenario in another file entirely - the hardest kind of failure to
  # account for. Adding one is walked as far as the form and no further, for the same reason in
  # reverse: this account already carries ten, and every run that saved another would leave the
  # list a little longer and a little less like the one the other files expect.
  #
  # THE DATA
  #
  # TestData/Template_TestData.xlsx carries the account and, row by row, the way of sending money
  # whose template is being started - so that adding one is covered for every way the page
  # offers, including the ones it will not open.

  # ------------------------------------------------------------------
  # What is saved
  # ------------------------------------------------------------------

  @templates @Template_TC_001
  Scenario Outline: The profile drawer opens the account's saved templates
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Template page
    Then the page should list the templates the account has saved

    Examples:
      | excelFileName          | excelSheetName | row |
      | Template_TestData.xlsx | Sheet1         | 1   |

  # A template saved with no name is one nobody can pick out of the list, and one with no account
  # under it is a destination that has lost its destination. Both are read from the card, because
  # both are all the list gives anybody to choose on.
  @templates @Template_TC_002
  Scenario Outline: Every saved template names itself and the account it pays
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Template page
    Then every saved template should carry a name and the account it pays

    Examples:
      | excelFileName          | excelSheetName | row |
      | Template_TestData.xlsx | Sheet1         | 1   |

  # The icon is the only thing on the card that says which way of sending money the template was
  # saved for. Two templates can name the same card number and pay it by different routes, so a
  # card with no icon leaves them indistinguishable until one is opened.
  @templates @Template_TC_003
  Scenario Outline: Every saved template shows the way of sending it was saved for
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Template page
    Then every saved template should show the way of sending it was saved for

    Examples:
      | excelFileName          | excelSheetName | row |
      | Template_TestData.xlsx | Sheet1         | 1   |

  # Counted rather than clicked. A template whose account has changed and which cannot be changed
  # is a transfer waiting to go astray - but proving the bin by emptying it would take a template
  # the Transfer scenarios choose by name.
  @templates @Template_TC_004
  Scenario Outline: Every saved template offers to be changed and removed
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Template page
    Then every saved template should offer to be changed and removed

    Examples:
      | excelFileName          | excelSheetName | row |
      | Template_TestData.xlsx | Sheet1         | 1   |

  # The card does nothing when it is clicked, so the pencil is the only way in.
  @templates @Template_TC_005
  Scenario Outline: A saved template can be opened to be changed
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Template page
    And I open the first saved template to change it
    Then somewhere to change that template should open

    Examples:
      | excelFileName          | excelSheetName | row |
      | Template_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # Adding one
  # ------------------------------------------------------------------

  @templates @Template_TC_006
  Scenario Outline: The page offers to add a template
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Template page
    And I start adding a template
    Then the ways a template can be saved for should open

    Examples:
      | excelFileName          | excelSheetName | row |
      | Template_TestData.xlsx | Sheet1         | 1   |

  # A way that quietly disappeared would take a whole kind of saved destination with it, and
  # nobody would notice until somebody went looking for the template they used to have.
  @templates @Template_TC_007
  Scenario Outline: Adding one offers every way a template can be saved for
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Template page
    And I start adding a template
    Then the ways offered should be "UMPay to UMPay Wallet, UnionPay China, UnionPay Global, UMPay Transfer to School Fees, Transfer to Alipay, Transfer to Wechat"

    Examples:
      | excelFileName          | excelSheetName | row |
      | Template_TestData.xlsx | Sheet1         | 1   |

  # A way that is closed and does not say so is worse than one that is missing: somebody chooses
  # it, nothing happens, and there is nothing on the page to explain why.
  @templates @Template_TC_008
  Scenario Outline: The ways that are closed say so
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Template page
    And I start adding a template
    Then the ways "Transfer to Alipay, Transfer to Wechat" should be marked as under maintenance

    Examples:
      | excelFileName          | excelSheetName | row |
      | Template_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # Every way it can be saved for, one row of the sheet each
  # ------------------------------------------------------------------

  @templates @Template_TC_009
  Scenario Outline: A template can be started for a UMPay wallet
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Template page
    And I start adding a template
    And I choose the way named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then somewhere to save that template should open

    Examples:
      | excelFileName          | excelSheetName | row |
      | Template_TestData.xlsx | Sheet1         | 2   |

  @templates @Template_TC_010
  Scenario Outline: A template can be started for UnionPay China
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Template page
    And I start adding a template
    And I choose the way named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then somewhere to save that template should open

    Examples:
      | excelFileName          | excelSheetName | row |
      | Template_TestData.xlsx | Sheet1         | 3   |

  @templates @Template_TC_011
  Scenario Outline: A template can be started for UnionPay Global
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Template page
    And I start adding a template
    And I choose the way named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then somewhere to save that template should open

    Examples:
      | excelFileName          | excelSheetName | row |
      | Template_TestData.xlsx | Sheet1         | 4   |

  @templates @Template_TC_012
  Scenario Outline: A template can be started for school fees
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Template page
    And I start adding a template
    And I choose the way named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then somewhere to save that template should open

    Examples:
      | excelFileName          | excelSheetName | row |
      | Template_TestData.xlsx | Sheet1         | 5   |

  # The other side of TC_008: the page says the way is closed, and choosing it leaves the run
  # where it was rather than half-way into a form it cannot finish.
  @templates @Template_TC_013
  Scenario Outline: A way that is under maintenance does not open
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Template page
    And I start adding a template
    And I choose the way named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the ways a template can be saved for should still be offered

    Examples:
      | excelFileName          | excelSheetName | row |
      | Template_TestData.xlsx | Sheet1         | 6   |

  # Somebody who opened the ways by mistake has to be able to get back to what they already have
  # saved, with all of it still there.
  @templates @Template_TC_014
  Scenario Outline: Going back from the ways returns to the saved templates
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Template page
    And I start adding a template
    Then the ways a template can be saved for should open
    When I go back to the saved templates
    Then the saved templates should be listed again

    Examples:
      | excelFileName          | excelSheetName | row |
      | Template_TestData.xlsx | Sheet1         | 1   |
