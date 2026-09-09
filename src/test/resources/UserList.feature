Feature: User List

  As a UMPay user who has brought other people to the platform
  I want to see who they are and whether their accounts are in good standing
  So that I know who my commission is coming from and who is having trouble

  # WHAT THIS FILE COVERS
  #
  # The people this account has brought to UMPay, at /customer, reached from the profile drawer.
  # Each is listed by three things and nothing else:
  #
  #   User UUID    the account number the platform knows them by
  #   User Name    the name they go under, or N/A where they have not set one
  #   Status       whether their account is locked
  #
  # WHAT IS NOT ON THE PAGE, AND IS CHECKED FOR
  #
  # Nothing to press. No search, no filter, no form, and the status is written in a span rather
  # than offered as a control. That is worth holding the page to rather than simply observing:
  # these are other people's accounts, and anything here that could lock or unlock one of them
  # would be a way to reach into somebody else's money from a page meant only to be read.
  #
  # THE DATA
  #
  # TestData/UserList_TestData.xlsx carries the account. Nothing about the people listed is
  # written into it: who this account has brought will change, and a scenario naming one of them
  # would go stale the moment somebody else signs up. What is held to is the shape of what is
  # shown, which does not.

  @userlist @User_List_TC_001
  Scenario Outline: The profile drawer opens the user list
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the User List
    Then the people this account has brought should be listed

    Examples:
      | excelFileName          | excelSheetName | row |
      | UserList_TestData.xlsx | Sheet1         | 1   |

  # A user listed without the number the platform knows them by cannot be asked about, and one
  # listed without a status cannot be told from an account that has been suspended.
  @userlist @User_List_TC_002
  Scenario Outline: Every user is listed with a number, a name and a status
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the User List
    Then every user should be listed with a number, a name and a status

    Examples:
      | excelFileName          | excelSheetName | row |
      | UserList_TestData.xlsx | Sheet1         | 1   |

  # The account number is what somebody quotes to support, so it has to be the number the platform
  # actually issues rather than a truncation or a placeholder.
  @userlist @User_List_TC_003
  Scenario Outline: Every user is listed under a proper account number
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the User List
    Then every account number should read as one the platform issues

    Examples:
      | excelFileName          | excelSheetName | row |
      | UserList_TestData.xlsx | Sheet1         | 1   |

  # The same person listed twice would have their commission counted twice by anybody reading
  # this page.
  @userlist @User_List_TC_004
  Scenario Outline: Nobody is listed twice
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the User List
    Then nobody should be listed twice

    Examples:
      | excelFileName          | excelSheetName | row |
      | UserList_TestData.xlsx | Sheet1         | 1   |

  # A status the page invents is worse than none: somebody would act on a word nobody defined.
  @userlist @User_List_TC_005
  Scenario Outline: Every status is one the platform uses
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the User List
    Then every status should be one of "Unlock, Lock, Locked"

    Examples:
      | excelFileName          | excelSheetName | row |
      | UserList_TestData.xlsx | Sheet1         | 1   |

  # These are other people's accounts. A page meant to be read should offer no way to change one.
  @userlist @User_List_TC_006
  Scenario Outline: Nothing on the page can change somebody else's account
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the User List
    Then there should be nothing here that changes somebody else's account

    Examples:
      | excelFileName          | excelSheetName | row |
      | UserList_TestData.xlsx | Sheet1         | 1   |
