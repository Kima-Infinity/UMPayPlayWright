Feature: Profile drawer

  As a UMPay user
  I want everything about my own account to be reachable from one place
  So that my records, my wallets, my settings and the way out are where I left them

  # WHAT THIS FILE COVERS
  #
  # The drawer behind the last control in the top bar, served as #profile-sidebar: what it
  # offers, what it says about the account, where each of its functions leads, and logging out.
  #
  # HOW THE ITEMS ARE FOUND
  #
  # By what they say. The older page object addressed them as [id='trade_record'],
  # [id='wallet'] and so on, and not one of those ids is on the page any more - every item now
  # carries no id at all, so those locators matched nothing while looking perfectly reasonable.
  #
  # THE DATA
  #
  # TestData/Profile_TestData.xlsx carries the account, its referral code and the state its
  # documents are in. The referral code is in the sheet because it is this account's own and does
  # not change; what is copied is compared against what the drawer shows rather than against the
  # sheet, since the point is that the two agree with each other. The function names and the pages they open are in the scenarios, because
  # those are what is being asserted rather than what is being fed in.
  #
  # LOGGING OUT
  #
  # Two scenarios, and the order matters: cancelling first, because it proves the confirmation
  # is real, and signing out last, because after it the session is gone.

  @profile @Profile_TC_001
  Scenario Outline: The drawer offers everything the account holder can reach
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the profile drawer
    Then the profile drawer should offer "Trade Record, Referral Code, User List, Commission Listing, Fee Listing, Wallet, Payment, Template, Transfer Fee Setting, Document Verification, Security, Languages, Settings, Logout"

    Examples:
      | excelFileName         | excelSheetName | row |
      | Profile_TestData.xlsx | Sheet1         | 1   |

  @profile @Profile_TC_002
  Scenario Outline: The drawer displays details of account owner
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the profile drawer
    Then the profile drawer should show the account details in "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName         | excelSheetName | row |
      | Profile_TestData.xlsx | Sheet1         | 2   |

  # Each function opens a page of its own. The address and the heading are both asserted: several
  # of these live under /settings and would look alike by address alone, and a heading can be
  # left over from the page before while the new one is still drawing.
  @profile @Profile_TC_003
  Scenario Outline: Each function in the drawer opens its own page
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the profile drawer
    And I open "<function>" from the profile drawer
    Then it should open the "<heading>" page at "<address>"

    Examples:
      | excelFileName         | excelSheetName | row | function             | heading              | address                  |
      | Profile_TestData.xlsx | Sheet1         | 3   | Trade Record         | Trade Record         | /v2/trade-record         |
      | Profile_TestData.xlsx | Sheet1         | 3   | User List            | User List            | /customer                |
      | Profile_TestData.xlsx | Sheet1         | 3   | Commission Listing   | Commission Listing   | /customer/commission     |
      | Profile_TestData.xlsx | Sheet1         | 3   | Fee Listing          | Fee Listing          | /customer/fee            |
      | Profile_TestData.xlsx | Sheet1         | 3   | Wallet               | Wallet               | /v2/wallet               |
      | Profile_TestData.xlsx | Sheet1         | 3   | Payment              | Payment              | /settings-payment        |
      | Profile_TestData.xlsx | Sheet1         | 3   | Template             | Templates            | /v2/template             |
      | Profile_TestData.xlsx | Sheet1         | 3   | Transfer Fee Setting | Transfer Fee Setting | /settings/transfer-fee   |

  # THE REFERRAL CODE
  #
  # The drawer shows this account's own code and offers a control beside it that copies it. The
  # item around that control is not a control at all - it is drawn with cursor-auto and pressing it
  # does nothing - so the copy is addressed on its own. Pressing it closes the drawer, which is why
  # the code is read before the copy rather than after.
  #
  # These carry numbers after the logout case rather than before it. The tag is what identifies a
  # case in the workbook, and renumbering the logout case would break the row already recorded
  # against it.

  # A code shown but not offered for copying would have to be read off the screen and typed out
  # again by hand, and a referral code mistyped is somebody else's commission.
  @profile @Profile_TC_005
  Scenario Outline: Copying the referral code puts it on the clipboard
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the profile drawer
    And I copy my referral code
    Then the referral code should be on the clipboard

    Examples:
      | excelFileName         | excelSheetName | row |
      | Profile_TestData.xlsx | Sheet1         | 2   |

  # Copying leaves nothing on the screen to see, so a notice is the only way somebody knows the
  # press was heard rather than pressing it again and again.
  @profile @Profile_TC_006
  Scenario Outline: The application confirms the referral code was copied
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the profile drawer
    And I copy my referral code
    Then the application should confirm the referral code was copied

    Examples:
      | excelFileName         | excelSheetName | row |
      | Profile_TestData.xlsx | Sheet1         | 2   |

  # A code that changed between readings would quietly break every link and message this account
  # had already sent out, and the commission would go nowhere.
  @profile @Profile_TC_007
  Scenario Outline: The referral code does not change between readings
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the profile drawer
    Then the referral code should read the same each time the drawer is opened

    Examples:
      | excelFileName         | excelSheetName | row |
      | Profile_TestData.xlsx | Sheet1         | 2   |

  # The way out asks before it takes it - "Cancel" or "Yes" - and cancelling has to leave the
  # session alone: a confirmation that signs you out either way is not a confirmation.


  # Cancelling comes first, because it is what proves the confirmation is real: a question that
  # signs you out whichever button is pressed is not a question. This case is described in the
  # notes above and its steps were written, but the scenario itself was never added - so nothing
  # held the Cancel button to doing anything at all.
  @profile @negative @Profile_TC_008
  Scenario Outline: Cancelling the logout leaves the account signed in
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the profile drawer
    And I ask to log out
    Then the application should ask whether I mean it
    When I answer "Cancel"
    Then I should still be signed in

    Examples:
      | excelFileName         | excelSheetName | row |
      | Profile_TestData.xlsx | Sheet1         | 4   |

  @profile @Profile_TC_004
  Scenario Outline: Logging out signs the account out
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the profile drawer
    And I ask to log out
    Then the application should ask whether I mean it
    When I answer "Yes"
    Then I should be signed out

    Examples:
      | excelFileName         | excelSheetName | row |
      | Profile_TestData.xlsx | Sheet1         | 4   |

  # Signing out has to mean the account cannot be read again. A page held in the browser's
  # history was drawn while somebody was signed in, and if stepping back to it shows it again
  # then signing out on a shared machine has done nothing at all.
  #
  # This goes last in the file, as the other logout case does: after it the session is gone.
  @profile @negative @Profile_TC_009
  Scenario Outline: Going back after signing out does not show the account again
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the profile drawer
    And I ask to log out
    Then the application should ask whether I mean it
    When I answer "Yes"
    Then I should be signed out
    When I go back in the browser
    Then the account should not be shown again

    Examples:
      | excelFileName         | excelSheetName | row |
      | Profile_TestData.xlsx | Sheet1         | 4   |

