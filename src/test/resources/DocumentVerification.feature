Feature: Document Verification

  As a UMPay user
  I want to see what my account is verified on
  So that I know which document is standing behind everything I am allowed to do

  # WHAT THIS FILE COVERS
  #
  # The document verification the profile drawer opens. It is a dialog rather than a page - the
  # address does not change - mounted into #root.dialog, which sits empty until the drawer opens
  # it. That is what tells the two states apart.
  #
  # The dialog is a reading of what was submitted and accepted, in labels and values:
  #
  #   who the account belongs to    First Name, Last Name, Date of Birth
  #   the document it stands on     Document Type, National ID, National ID Expiry Date
  #   where it was verified from    Country of Issue, Address
  #   how they can be reached       Phone Number, Email
  #   what they do                  Position, Occupation, Annual Income
  #
  # and under them the three pictures the verification was granted on - the front of the document,
  # the back, and a selfie holding it.
  #
  # The drawer itself carries the verdict, which is where somebody sees it without opening
  # anything.
  #
  # WHY NOTHING HERE IS DANGEROUS
  #
  # There is nothing on this dialog to change: no form, no upload, nothing to submit. The only
  # thing that can be pressed is Close. What these documents say decides what the whole suite is
  # allowed to do, and nothing here can alter it.
  #
  # THE DATA
  #
  # TestData/DocumentVerification_TestData.xlsx carries the account, and the expected details are
  # not written into it on purpose: the scenarios hold the dialog to being complete and
  # consistent rather than to one particular person's name, so they still mean something on an
  # account other than this one.

  @documentverification @Document_Verification_TC_001
  Scenario Outline: The profile drawer opens the account's document verification
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Document Verification
    Then the document verification should be shown

    Examples:
      | excelFileName                      | excelSheetName | row |
      | DocumentVerification_TestData.xlsx | Sheet1         | 1   |

  # The drawer is where somebody finds out where they stand without opening anything, and an
  # account that cannot tell whether it is verified is one that cannot tell why it is refused.
  @documentverification @Document_Verification_TC_002
  Scenario Outline: The drawer says where the account stands
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Document Verification
    Then the drawer should say where the account stands

    Examples:
      | excelFileName                      | excelSheetName | row |
      | DocumentVerification_TestData.xlsx | Sheet1         | 1   |

  @documentverification @Document_Verification_TC_003
  Scenario Outline: The dialog names the person the account belongs to
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Document Verification
    Then it should carry "First Name, Last Name, Date of Birth"

    Examples:
      | excelFileName                      | excelSheetName | row |
      | DocumentVerification_TestData.xlsx | Sheet1         | 1   |

  @documentverification @Document_Verification_TC_004
  Scenario Outline: The dialog names the document the account is verified on
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Document Verification
    Then it should carry "Document Type, National ID, National ID Expiry Date"

    Examples:
      | excelFileName                      | excelSheetName | row |
      | DocumentVerification_TestData.xlsx | Sheet1         | 1   |

  # The address and the number here are what a refusal would be explained to, so they are worth
  # being sure of.
  @documentverification @Document_Verification_TC_005
  Scenario Outline: The dialog carries the contact details it was verified with
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Document Verification
    Then it should carry "Phone Number, Email"

    Examples:
      | excelFileName                      | excelSheetName | row |
      | DocumentVerification_TestData.xlsx | Sheet1         | 1   |

  @documentverification @Document_Verification_TC_006
  Scenario Outline: The dialog carries what the account holder does and where they live
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Document Verification
    Then it should carry "Country of Issue, Address, Position, Occupation, Annual Income"

    Examples:
      | excelFileName                      | excelSheetName | row |
      | DocumentVerification_TestData.xlsx | Sheet1         | 1   |

  # A label with nothing under it is a detail the account was verified without, and nobody
  # reading the dialog can tell which of the two it is.
  @documentverification @Document_Verification_TC_007
  Scenario Outline: Nothing the dialog shows is blank
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Document Verification
    Then nothing it shows should be blank

    Examples:
      | excelFileName                      | excelSheetName | row |
      | DocumentVerification_TestData.xlsx | Sheet1         | 1   |

  # A caption with no picture under it is a document nobody can check.
  @documentverification @Document_Verification_TC_008
  Scenario Outline: All three pictures the verification was granted on are there
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Document Verification
    Then all three pictures should be there, one under each caption

    Examples:
      | excelFileName                      | excelSheetName | row |
      | DocumentVerification_TestData.xlsx | Sheet1         | 1   |

  # An account verified on a document that has since expired is verified on nothing: the whole
  # point of the expiry date is that the document stops standing for anything after it.
  @documentverification @Document_Verification_TC_009
  Scenario Outline: The document the account is verified on has not expired
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Document Verification
    Then the document it is verified on should not have expired

    Examples:
      | excelFileName                      | excelSheetName | row |
      | DocumentVerification_TestData.xlsx | Sheet1         | 1   |

  # It is a dialog over whatever the account was looking at, so closing it has to give that back
  # rather than leave somebody on a page they never asked for.
  @documentverification @Document_Verification_TC_010
  Scenario Outline: The dialog closes and gives back the page it was opened over
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Document Verification
    And I close the document verification
    Then it should be gone, and the page underneath should still be there

    Examples:
      | excelFileName                      | excelSheetName | row |
      | DocumentVerification_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # An account that has not been verified
  # ------------------------------------------------------------------

  # The other side of everything above. A new account has submitted nothing, so the drawer sends
  # it to the form rather than to a reading of what it was verified on - and until something is
  # submitted and accepted, it is not verified.
  #
  # Row 2 is an account this suite registered through the ordinary registration flow and never
  # verified. It is kept in the sheet rather than registered afresh each run: registering takes a
  # captcha, a mailbox and two minutes, and none of that is what this scenario is about.
  @documentverification @Document_Verification_TC_011
  Scenario Outline: A new account has a document still to submit, and is not verified
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Document Verification
    Then the account should still have a document to submit
    And the form should ask for a document and its pictures

    Examples:
      | excelFileName                      | excelSheetName | row |
      | DocumentVerification_TestData.xlsx | Sheet1         | 2   |
