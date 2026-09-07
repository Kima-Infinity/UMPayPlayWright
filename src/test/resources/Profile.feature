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
  # documents are in. The function names and the pages they open are in the scenarios, because
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

  # The way out asks before it takes it - "Cancel" or "Yes" - and cancelling has to leave the
  # session alone: a confirmation that signs you out either way is not a confirmation.


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
