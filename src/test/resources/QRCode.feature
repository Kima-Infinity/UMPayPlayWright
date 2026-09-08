Feature: QR Code

  As a UMPay user
  I want a code that pays me and a way to scan one that pays somebody else
  So that money can be sent across a table without anybody typing an account number

  # WHAT THIS FILE COVERS
  #
  # The two QR pages:
  #
  #   /qr-code   the account's own code, drawn for one of its wallets, with an amount and a
  #              remark that can be put on it, and a Download
  #   /scan-qr   the scanner: a camera frame, and under it "OR Upload Photo" for a picture of a
  #              code
  #
  # HOW THEY ARE OPENED, AND WHY THAT IS ITSELF A TEST CASE
  #
  # By their addresses, because nothing in the application leads to them any more. The top bar was
  # swept at desktop and at phone width, and so were the sidebar, the profile drawer and the home,
  # transfer, deposit, withdraw, bills, convert, wallet and trade record pages: none of them
  # mentions either address. The suite's own older header page object still carries locators for a
  # QR control in the top bar, and not one of them matches anything now, so this is something the
  # application used to offer. The last scenario holds it to that; the rest open the pages
  # directly, because otherwise there would be nothing to test at all.
  #
  # WHAT NOTHING HERE DOES
  #
  # Nothing is paid. The code is read and drawn, never used to send anything, and the scanner is
  # only ever handed a picture with no code in it - a screenshot from this suite's own run - so
  # there is no destination for it to act on.
  #
  # A headless run has no camera, so the scanner says "Device not supported" and the photo is the
  # only way in. That is not a fault: it is the state the scanner is meant to handle, and the
  # scenarios below hold it to handling it.
  #
  # THE DATA
  #
  # TestData/QRCode_TestData.xlsx carries the account, the amount and the remark to put on the
  # code.

  # ------------------------------------------------------------------
  # The account's own code
  # ------------------------------------------------------------------

  @qrcode @QR_Code_TC_001
  Scenario Outline: The QR code page draws the account's code
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the QR code page
    Then the account's QR code should be drawn

    Examples:
      | excelFileName        | excelSheetName | row |
      | QRCode_TestData.xlsx | Sheet1         | 1   |

  # A code that does not say which money it collects is one somebody scans and then argues about.
  @qrcode @QR_Code_TC_002
  Scenario Outline: The code says which wallet it collects into
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the QR code page
    Then it should say which wallet the code collects into

    Examples:
      | excelFileName        | excelSheetName | row |
      | QRCode_TestData.xlsx | Sheet1         | 1   |

  @qrcode @QR_Code_TC_003
  Scenario Outline: The code can be given an amount and a remark
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the QR code page
    Then it should ask for an amount and a remark to put on the code

    Examples:
      | excelFileName        | excelSheetName | row |
      | QRCode_TestData.xlsx | Sheet1         | 1   |

  # The code is redrawn from what is typed, so it has to still be there afterwards: a page that
  # loses the code when an amount is entered is one nobody can be paid from.
  @qrcode @QR_Code_TC_004
  Scenario Outline: The code is still drawn once an amount and a remark are on it
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the QR code page
    And I put the amount and the remark from "<row>" of "<excelSheetName>" of "<excelFileName>" on the code
    Then the code should still be drawn, carrying what was typed

    Examples:
      | excelFileName        | excelSheetName | row |
      | QRCode_TestData.xlsx | Sheet1         | 2   |

  # An account with ten wallets that can only be paid into one of them is nine wallets short.
  @qrcode @QR_Code_TC_005
  Scenario Outline: The code can be drawn for any of the account's wallets
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the QR code page
    And I open the wallets the code can be drawn for
    Then every wallet the account holds should be offered

    Examples:
      | excelFileName        | excelSheetName | row |
      | QRCode_TestData.xlsx | Sheet1         | 1   |

  # A code that cannot leave the screen cannot be printed, pinned up or sent to anybody.
  @qrcode @QR_Code_TC_006
  Scenario Outline: The code can be downloaded
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the QR code page
    Then it should offer to download the code

    Examples:
      | excelFileName        | excelSheetName | row |
      | QRCode_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # Scanning somebody else's
  # ------------------------------------------------------------------

  @qrcode @QR_Code_TC_007
  Scenario Outline: The scan page asks for a code to be lined up with the frame
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the scan QR code page
    Then it should ask for a code to be lined up with the frame

    Examples:
      | excelFileName        | excelSheetName | row |
      | QRCode_TestData.xlsx | Sheet1         | 1   |

  # Somebody on a machine with no camera - or who has refused it one - still has to be able to
  # scan a code they have been sent as a picture.
  @qrcode @QR_Code_TC_008
  Scenario Outline: A photo can be uploaded where there is no camera to use
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the scan QR code page
    Then it should offer a photo to be uploaded instead

    Examples:
      | excelFileName        | excelSheetName | row |
      | QRCode_TestData.xlsx | Sheet1         | 1   |

  # The picture handed over is a screenshot from this suite's own run: a real picture with no code
  # in it, so there is nothing for the scanner to act on and nobody to pay.
  @qrcode @QR_Code_TC_009
  Scenario Outline: A picture with no code in it is not acted on
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the scan QR code page
    And I hand it a picture with no code in it
    Then it should not act on it

    Examples:
      | excelFileName        | excelSheetName | row |
      | QRCode_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # Getting to any of it
  # ------------------------------------------------------------------

  # Both pages answer their addresses and work, but nothing in the application leads to either -
  # not the top bar at either width, not the sidebar, not the profile drawer, and nothing on the
  # eight pages this scenario sweeps. A feature nobody can reach is a feature nobody has.
  @qrcode @QR_Code_TC_010
  Scenario Outline: The QR pages can be reached from the application
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I look through the application for a way to the QR pages
    Then something should lead to them

    Examples:
      | excelFileName        | excelSheetName | row |
      | QRCode_TestData.xlsx | Sheet1         | 1   |
