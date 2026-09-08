Feature: Languages

  As a UMPay user
  I want to read the application in my own language
  So that I am not moving money about in words I only half understand

  # WHAT THIS FILE COVERS
  #
  # The languages at /v2/languages, reached from the profile drawer. Three of them, each a label
  # the page draws around a radio, and each written in its own script:
  #
  #   English     en
  #   繁體中文     zh-HK
  #   ไทย          th
  #
  # There is nothing to save. Choosing one takes effect at once and is still chosen the next time
  # the page is opened.
  #
  # The scenarios name the languages by their codes rather than by their characters, so that this
  # file reads the same on a machine with no font for them.
  #
  # WHAT SAYS THE LANGUAGE CHANGED
  #
  # The tab, rather than the page. The words on the screen are redrawn in some places and left
  # alone in others, but the tab is written from the same translations and changes on every page -
  # "UMPay | Languages" in English against "UMPay | 語言" in Chinese - so it is the honest thing to
  # hold the application to.
  #
  # WHAT THIS COSTS TO TEST
  #
  # The language is one setting for the whole account, and every other file in this suite reads
  # the screen in English. So every scenario that chooses a language reads what the account was on
  # before it touched anything and puts it back before it ends. The account finishes each scenario
  # in the language it started it.
  #
  # Not to be confused with Login.feature, which covers the switcher on the login page - the one
  # somebody uses before they have an account to set this on.
  #
  # THE DATA
  #
  # TestData/Languages_TestData.xlsx carries the account and, row by row, the language that
  # scenario is about.

  # ------------------------------------------------------------------
  # What is on offer
  # ------------------------------------------------------------------

  @languages @Languages_TC_001
  Scenario Outline: The profile drawer opens the languages
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Languages page
    Then the languages should be shown

    Examples:
      | excelFileName           | excelSheetName | row |
      | Languages_TestData.xlsx | Sheet1         | 1   |

  # A language quietly dropped would strand everybody who reads in it, and they are the least
  # likely to be able to report it.
  @languages @Languages_TC_002
  Scenario Outline: The page offers every language the account can be read in
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Languages page
    Then the languages offered should be "en, zh-HK, th"

    Examples:
      | excelFileName           | excelSheetName | row |
      | Languages_TestData.xlsx | Sheet1         | 1   |

  # A list of languages written entirely in English is no use to the one person who needs it: the
  # one who cannot read English. Each has to be written in its own script.
  @languages @Languages_TC_003
  Scenario Outline: Every language is written in its own script
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Languages page
    Then every language should be named in its own script

    Examples:
      | excelFileName           | excelSheetName | row |
      | Languages_TestData.xlsx | Sheet1         | 1   |

  @languages @Languages_TC_004
  Scenario Outline: Exactly one language is chosen, and the page says which
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Languages page
    Then exactly one language should be chosen

    Examples:
      | excelFileName           | excelSheetName | row |
      | Languages_TestData.xlsx | Sheet1         | 1   |

  # Nothing to press, which is worth stating: a page with a Save nobody notices is a page whose
  # setting silently does not apply.
  @languages @Languages_TC_005
  Scenario Outline: The language is chosen without anything to save
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Languages page
    Then the page should offer nothing to save

    Examples:
      | excelFileName           | excelSheetName | row |
      | Languages_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # Reading the application in each of them
  # ------------------------------------------------------------------

  @languages @Languages_TC_006
  Scenario Outline: The application can be read in Chinese
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Languages page
    And I put the language on the one named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the interface should come back in that language
    When I put the language back the way it was
    Then the interface should be back in the language it was in

    Examples:
      | excelFileName           | excelSheetName | row |
      | Languages_TestData.xlsx | Sheet1         | 2   |

  @languages @Languages_TC_007
  Scenario Outline: The application can be read in Thai
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Languages page
    And I put the language on the one named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the interface should come back in that language
    When I put the language back the way it was
    Then the interface should be back in the language it was in

    Examples:
      | excelFileName           | excelSheetName | row |
      | Languages_TestData.xlsx | Sheet1         | 3   |

  # Choosing two of three would leave the application with no way to know which was meant.
  @languages @Languages_TC_008
  Scenario Outline: Choosing a language leaves only that language chosen
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Languages page
    And I put the language on the one named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then only that language should be chosen
    When I put the language back the way it was
    Then the interface should be back in the language it was in

    Examples:
      | excelFileName           | excelSheetName | row |
      | Languages_TestData.xlsx | Sheet1         | 2   |

  # A language that had to be chosen again on every visit would be worse than none at all.
  @languages @Languages_TC_009
  Scenario Outline: A chosen language is still chosen when the page is opened again
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Languages page
    And I put the language on the one named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then it should still be chosen when the page is opened again
    When I put the language back the way it was
    Then the interface should be back in the language it was in

    Examples:
      | excelFileName           | excelSheetName | row |
      | Languages_TestData.xlsx | Sheet1         | 2   |

  # The language belongs to the account, not to the page it was chosen on.
  @languages @Languages_TC_010
  Scenario Outline: The language follows the account onto another page
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Languages page
    And I put the language on the one named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I go to the home page
    Then the interface should still be in that language
    When I put the language back the way it was
    Then the interface should be back in the language it was in

    Examples:
      | excelFileName           | excelSheetName | row |
      | Languages_TestData.xlsx | Sheet1         | 2   |
