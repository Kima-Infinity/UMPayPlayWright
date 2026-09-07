Feature: Convert
  As a UMPay user
  I want to convert money between my wallets
  So that I can hold a balance in the currency I need

  # Convert moves real money between the test account's wallets, so the amounts in
  # Convert_TestData.xlsx are kept small. The scenario asserts on the source
  # balance falling rather than only on the success dialog: the dialog says the
  # request was accepted, the balance says the money actually moved.

  @convert @smoke @Convert_TC_001
  Scenario Outline: Money is converted between two wallets
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to the Convert page
    And I convert the amount in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the conversion should be confirmed with the message in "<row>" of "<excelSheetName>" of "<excelFileName>"
    And the source wallet balance should have gone down by the converted amount

    Examples:
      | excelFileName        | excelSheetName | row |
      | Convert_TestData.xlsx | Sheet1        | 1   |

  # ------------------------------------------------------------------
  # Every pair of wallets, in both directions
  # ------------------------------------------------------------------
  #
  # Ten wallets make ninety ordered pairs, and a conversion is not symmetric - HKD to THB and
  # THB to HKD are different rates, different rounding and, as the deposits and withdraws have
  # shown, sometimes different answers from the platform. Each row below walks one wallet into
  # all nine others, so ten rows cover the whole matrix.
  #
  # They are walked rather than run independently, because ninety scenarios would mean ninety
  # sign-ins for ninety conversions. A row is one session and nine conversions.
  #
  # The order of the rows is what makes the matrix affordable. HKD goes first because it is the
  # wallet that holds money; every other wallet is fed by that row before it is asked to be a
  # source of its own, which is why MXN and THB - both of them empty - come last. Every row
  # leaves HKD until last as well, so the money ends where it started rather than scattered
  # across ten wallets.
  #
  # The amounts are small and per-wallet, because a unit means different things in different
  # currencies: one Malaysian ringgit is worth converting, one Vietnamese dong is not. Some had
  # to be raised after a first run: the form simply will not convert a figure that would arrive
  # as almost nothing at the far end, and it says so by leaving Convert disabled rather than by
  # refusing out loud.
  #
  # A pair that fails does not stop the rest. Every pair is tried and the failures are listed
  # together, so one declined channel is reported as one rather than hiding the eight that
  # work.
  #
  # BDT is not among the rows. The convert form offers nine of the account's ten wallets and
  # BDT is the one it leaves out - it can be deposited and withdrawn but not converted. That is
  # said once, in the scenario below this one, rather than nine times over as a row of the
  # matrix that could never pass. If the form ever offers it, that scenario starts failing and
  # the row can be put back; sheet row 4 is still there waiting for it.
  #
  # This moves real money seventy-two times per run and takes the best part of an hour. Run it on
  # its own with -Dcucumber.filter.tags="@allpairs", or tag it @manual if that is more than an
  # unattended run should be spending.

  @convert @allpairs @Convert_TC_002
  Scenario Outline: Every wallet converts into every other wallet
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to the Convert page
    Then converting the amount in "<row>" of "<excelSheetName>" of "<excelFileName>" into every other wallet, ending with "HKD", should succeed

    Examples:
      | excelFileName         | excelSheetName | row |
      | Convert_TestData.xlsx | Sheet1         | 2   |
      | Convert_TestData.xlsx | Sheet1         | 3   |
      | Convert_TestData.xlsx | Sheet1         | 5   |
      | Convert_TestData.xlsx | Sheet1         | 6   |
      | Convert_TestData.xlsx | Sheet1         | 7   |
      | Convert_TestData.xlsx | Sheet1         | 8   |
      | Convert_TestData.xlsx | Sheet1         | 9   |
      | Convert_TestData.xlsx | Sheet1         | 10  |
      | Convert_TestData.xlsx | Sheet1         | 11  |

  # Nothing is converted here. The matrix above walks the wallets the form offers; this is the
  # one that says which wallets it ought to offer, and it fails today because BDT is missing.
  @convert @Convert_TC_003
  Scenario Outline: The convert form offers a wallet for every currency the account holds
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to the Convert page
    Then the convert currency list should offer "BDT, BRL, HKD, IDR, MXN, MYR, PHP, THB, USD, VND"

    Examples:
      | excelFileName         | excelSheetName | row |
      | Convert_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # What the amount box will and will not take
  # ------------------------------------------------------------------
  #
  # One pair is enough here: HKD to PHP, the pair the single conversion above uses. The band
  # belongs to the pair rather than to the form as a whole, and the matrix above is what covers
  # the rest of them.
  #
  # Two different things stop a conversion, and both are worth a scenario:
  #
  #   Outside the band  the box itself refuses, in the application's own wording, and the
  #                     figure never reaches the platform.
  #   More than held    the box is perfectly happy - the amount is inside the band - and it is
  #                     Convert that stays disabled. Reading only the box would call this
  #                     valid and then wait out a timeout wondering why nothing happened.
  #
  # The figures are not written down. The bounds arrive with the pair's live rate and the
  # balance moves every time the suite converts anything, so the run reads what the form says
  # and steps one outside it.
  #
  # Nothing here submits. These cost the account nothing and can be run as often as you like.

  @convert @limits @Convert_TC_004
  Scenario Outline: The amount box holds a conversion to the pair's stated limits
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to the Convert page
    And I choose to convert from "HKD" to "PHP"
    And I enter one below the stated minimum as the convert amount
    Then the convert amount should be refused with "Please input an amount of limit min or above!"
    When I enter one above the stated maximum as the convert amount
    Then the convert amount should be refused with "Please input an amount of limit max or below!"
    When I enter the stated minimum as the convert amount
    Then the convert amount should be accepted
    And the form should offer to convert

    Examples:
      | excelFileName         | excelSheetName | row |
      | Convert_TestData.xlsx | Sheet1         | 1   |

  @convert @limits
  Scenario Outline: More than the wallet holds cannot be converted
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to the Convert page
    And I choose to convert from "HKD" to "PHP"
    And I enter more than the wallet holds as the convert amount
    Then the convert amount should be accepted
    But the form should not offer to convert

    Examples:
      | excelFileName         | excelSheetName | row |
      | Convert_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # The conversion history
  # ------------------------------------------------------------------
  #
  # History is a button on the conversion form and a page of its own at
  # /v2/exchange-rate-history. It is not a table - no columns to address, no rows - so what is
  # read is the shape it repeats for every conversion: what went in, when, what came out, and
  # the rate it went at.
  #
  #   20.00 THB   2026-09-04 08:22:20   37.61 PHP   Rate: 1 THB ~ 1.88 PHP
  #
  # Two questions, and the second is the one that matters: the history opening is not the same
  # as the history being right. So one scenario reads what is already there, and the other
  # converts something and looks for it at the top.

  @convert @history @Convert_TC_005
  Scenario Outline: The conversion history opens from the Convert page
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to the Convert page
    And I open the conversion history
    Then the conversion history should be shown
    And every conversion listed should name an amount, a time, what it became and the rate

    Examples:
      | excelFileName         | excelSheetName | row |
      | Convert_TestData.xlsx | Sheet1         | 1   |

  @convert @history @Convert_TC_006
  Scenario Outline: A latest conversion appears at the top of the history
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to the Convert page
    And I convert the amount in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the conversion should be confirmed with the message in "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the conversion history
    Then the conversion history should be shown
    And the newest entry should be the conversion just made in "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName         | excelSheetName | row |
      | Convert_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # A conversion's own receipt
  # ------------------------------------------------------------------
  #
  # Every entry in the history opens a receipt of its own, carrying Download and Share. Three
  # things are worth proving and they are separate: that each entry opens, that what opens
  # belongs to the entry that was clicked, and that the two things the receipt offers work.
  #
  # The second matters most. A receipt that opens is worth little on its own - it is either the
  # conversion that was clicked or somebody else's, and a list that opened the wrong receipt
  # would look perfectly healthy from the outside.
  #
  # Nothing here converts anything. These read what earlier scenarios and earlier runs left
  # behind, so they cost the account nothing.

  @convert @history @Convert_TC_007
  Scenario Outline: Each conversion receipt in the history can be opened
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to the Convert page
    And I open the conversion history
    Then the conversion history should be shown
    And each of the first 3 conversions should open a receipt that matches its entry

    Examples:
      | excelFileName         | excelSheetName | row |
      | Convert_TestData.xlsx | Sheet1         | 1   |

  @convert @history @Convert_TC_008
  Scenario Outline: A receipt displays what was converted, what it became, the rate and when
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to the Convert page
    And I open the conversion history
    And I open the newest conversion
    Then the receipt should name what was converted, what it became, the rate and when

    Examples:
      | excelFileName         | excelSheetName | row |
      | Convert_TestData.xlsx | Sheet1         | 1   |

  # The receipt is drawn in the browser and handed over as a data URL rather than fetched from
  # the server, so there is no request to watch: a file arriving with something in it is the
  # only evidence the button did anything at all.
  @convert @history @Convert_TC_009
  Scenario Outline: A receipt can be downloaded
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to the Convert page
    And I open the conversion history
    And I open the newest conversion
    Then downloading the receipt should produce a file

    Examples:
      | excelFileName         | excelSheetName | row |
      | Convert_TestData.xlsx | Sheet1         | 1   |

  @convert @history @Convert_TC_010
  Scenario Outline: A receipt can be shared
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I navigate to the Convert page
    And I open the conversion history
    And I open the newest conversion
    When I share the receipt
    Then the sharing choices should offer a way to copy it

    Examples:
      | excelFileName         | excelSheetName | row |
      | Convert_TestData.xlsx | Sheet1         | 1   |
