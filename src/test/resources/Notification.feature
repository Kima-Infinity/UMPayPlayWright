Feature: Notification

  As a UMPay user
  I want everything the platform has told me in one place
  So that I can see what has happened to my account, what has been announced, and what was said

  # WHAT THIS FILE COVERS
  #
  # The notification page at /v2/notification, reached from the bell in the top bar. It holds
  # three tabs and nothing else:
  #
  #   Transactions  - a card for every movement the account has made, carrying the kind of
  #                   transaction, its amount, its currency and when it happened
  #   Announcements - what the platform has announced to the account, each with its own message
  #   Messages      - the conversation with customer service
  #
  # WHAT THE PAGE DOES NOT DO, AND SO IS NOT COVERED HERE
  #
  # A card is not a link. Its cursor stays an arrow, it holds no button or anchor of any kind,
  # and clicking one changes neither the page nor the address - so there is no detail to open,
  # and nothing to download or share the way a bill can be. There is no control that marks
  # notifications read either: the whole page offers three tab buttons and no others. Both were
  # tried against the running platform before being left out rather than assumed away.
  #
  # WHAT IT READS, AND WHAT IT CHANGES
  #
  # Everything the rest of this suite has already done - deposits, withdraws, conversions,
  # transfers and school fees all announce themselves here - so these scenarios submit nothing
  # and cost the account nothing. What they do change is read state, and they change it merely
  # by looking: this platform marks a notification read once it has been shown, so the count on
  # the bell falls while the page is scrolled. The last two scenarios hold that behaviour to the
  # parts of it that are the platform's promise rather than an accident of how much of the list
  # this account has already seen - the marks agreeing with the count, and read state surviving
  # a reload. They are tagged @seen so a run that wants something still unread can leave them
  # out.
  #
  # THE DATA
  #
  # TestData/Notification_TestData.xlsx carries the account and the tab to move to. What the
  # tabs are called is asserted from the page rather than fed in, because which tabs exist is
  # what is being checked.

  # ------------------------------------------------------------------
  # The page and its tabs
  # ------------------------------------------------------------------

  @notification @Notification_TC_001
  Scenario Outline: The bell opens the account's notifications
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Notifications page
    Then the notifications should list what the platform has told the account

    Examples:
      | excelFileName              | excelSheetName | row |
      | Notification_TestData.xlsx | Sheet1         | 1   |

  # Which tabs exist decides what an account can be told about, so one appearing or disappearing
  # is worth noticing here rather than discovering when a scenario quietly reads the wrong list.
  @notification @Notification_TC_002
  Scenario Outline: The notifications are kept in three tabs
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Notifications page
    Then the notifications should offer the tabs "Transactions, Announcements, Messages"

    Examples:
      | excelFileName              | excelSheetName | row |
      | Notification_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # Transactions
  # ------------------------------------------------------------------

  # A card missing any of its four parts is a card nobody can act on, and it is the sort of thing
  # a redesign produces without anyone noticing, because the page still looks full.
  @notification @Notification_TC_003
  Scenario Outline: Every transaction notification names its kind, amount, currency and date
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Notifications page
    Then each notification should name its kind, amount, currency and when it happened

    Examples:
      | excelFileName              | excelSheetName | row |
      | Notification_TestData.xlsx | Sheet1         | 1   |

  # The order is the whole use of the list: a reader opens it to see what has just happened, and
  # a list in any other order sends them to the oldest thing the platform ever said.
  @notification @Notification_TC_004
  Scenario Outline: The transaction notifications are listed newest first
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Notifications page
    Then the notifications should be listed newest first

    Examples:
      | excelFileName              | excelSheetName | row |
      | Notification_TestData.xlsx | Sheet1         | 1   |

  # Held against the list itself rather than against a table of symbols kept in the code: what
  # matters is not which symbol the platform picked for a wallet but that it picks the same one
  # every time, since a reader scanning a column of figures reads the symbol and not the code.
  @notification @Notification_TC_005
  Scenario Outline: Each amount is written with the symbol its currency uses
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Notifications page
    Then each amount should be written with the symbol its currency uses

    Examples:
      | excelFileName              | excelSheetName | row |
      | Notification_TestData.xlsx | Sheet1         | 1   |

  # The list grows as it is scrolled rather than paging, so reaching the bottom is what asks for
  # more. An account with hundreds of notifications is unreadable if only the first twenty can
  # ever be reached.
  @notification @Notification_TC_006
  Scenario Outline: Reaching the end of the list brings in older notifications
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Notifications page
    And I scroll to the end of the notifications
    Then more notifications should have been brought in

    Examples:
      | excelFileName              | excelSheetName | row |
      | Notification_TestData.xlsx | Sheet1         | 1   |

  # ------------------------------------------------------------------
  # Announcements and Messages
  # ------------------------------------------------------------------

  @notification @Notification_TC_007
  Scenario Outline: The Announcements tab shows what the platform has announced
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Notifications page
    And I move to the tab named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then every announcement should carry a message and when it arrived

    Examples:
      | excelFileName              | excelSheetName | row |
      | Notification_TestData.xlsx | Sheet1         | 2   |

  @notification @Notification_TC_008
  Scenario Outline: The Messages tab shows the conversation with customer service
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Notifications page
    And I move to the tab named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the conversation with customer service should be shown

    Examples:
      | excelFileName              | excelSheetName | row |
      | Notification_TestData.xlsx | Sheet1         | 3   |

  # A tab that merely highlights itself while leaving the same list underneath is worse than no
  # tab at all, because a reader believes they are looking at something else.
  @notification @Notification_TC_009
  Scenario Outline: Moving to another tab changes what is listed
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Notifications page
    And I move to the tab named in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then what is listed should change

    Examples:
      | excelFileName              | excelSheetName | row |
      | Notification_TestData.xlsx | Sheet1         | 2   |

  # ------------------------------------------------------------------
  # Being read
  # ------------------------------------------------------------------

  # Each card carries a dot the page hides once the card has been read, so the list says which
  # of the notifications on screen are still waiting - and the bell says how many are waiting
  # altogether. Two answers to the same question, and the list can never mark more of them than
  # the bell admits to.
  @notification @seen @Notification_TC_010
  Scenario Outline: The unread count agrees with what the list marks unread
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Notifications page
    And I look through the notifications
    Then the unread count should agree with the marks in the list

    Examples:
      | excelFileName              | excelSheetName | row |
      | Notification_TestData.xlsx | Sheet1         | 4   |

  # Being shown a notification is what marks it read here - there is no control anywhere on the
  # page that does it - so the count may fall while the list is scrolled. How far it falls
  # depends on how much of the list is new to this account, which no scenario can arrange, and
  # each run pushes the unread ones deeper into a list that only grows. So what is asserted is
  # the direction rather than the distance: looking must never make more of them unread, and a
  # count that climbs back after the page is loaded again was never saved on the platform at all
  # - it had only been forgotten in the browser, and the next sign-in would show the whole lot
  # unread over again.
  @notification @seen @Notification_TC_011
  Scenario Outline: What has been read is not forgotten when the page is loaded again
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open the Notifications page
    And I look through the notifications
    Then looking should not have added to the unread count
    When I reload the notifications
    Then what has been seen should still be marked read

    Examples:
      | excelFileName              | excelSheetName | row |
      | Notification_TestData.xlsx | Sheet1         | 4   |
