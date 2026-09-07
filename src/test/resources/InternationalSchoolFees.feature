Feature: International School Fees

  As a UMPay user
  I want to pay a school fee abroad from my wallet
  So that the amount, the fee it carries and the limits on it are known to be right before
  anything is sent

  # WHAT THIS FILE COVERS
  #
  # The International School Fee form at /school-fee: both ways in, what it states before
  # anything is entered, the arithmetic it does once an amount is, both ends of what it will
  # take, and the step it leads to.
  #
  # ONE PAGE
  #
  # Everything here starts on /school-fee, reached from the left navigation. Global Transfer
  # offers a route to the same form, and that route is asserted in GlobalTransfer.feature where
  # it starts - a scenario that begins on another page belongs to that page's file.
  #
  # THE DATA
  #
  # TestData/InternationalSchoolFees_TestData.xlsx carries the account and the minimum the form
  # should state. The amounts the scenarios enter are not in it: they are worked out from what
  # the form itself says - one below its minimum, one above what the wallet holds - because
  # those are the edges wherever they happen to be today.
  #
  # NOTHING HERE IS SENT
  #
  # Every scenario stops at the amount step or the one after it. The flow goes on to ask which
  # country and which school is being paid, and no scenario here answers that, so nothing can
  # be submitted by accident.

  # ------------------------------------------------------------------
  # Reaching the form
  # ------------------------------------------------------------------

  @schoolfee
  Scenario Outline: The sidebar opens the school fee form
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open International School Fees from the sidebar
    Then the school fee form should be shown
    And the school fee form should show the sending wallet and its balance
    And the school fee form should ask for an amount and a remark

    Examples:
      | excelFileName                         | excelSheetName | row |
      | InternationalSchoolFees_TestData.xlsx | sheet1         | 1   |

  # ------------------------------------------------------------------
  # What it states, and what it works out
  # ------------------------------------------------------------------

  # The minimum is stated as the amount box's placeholder rather than as text on the page. The
  # figure the form should state is in the sheet, and the assertion is exact on purpose: it
  # moved from 100 to 10 while this suite was being written, and a change like that should fail
  # somewhere a reader can see rather than pass quietly.
  @schoolfee
  Scenario Outline: The form states the minimum it will take
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open International School Fees from the sidebar
    Then the school fee form should be shown
    And the school fee form should state the minimum recorded in "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName                         | excelSheetName | row |
      | InternationalSchoolFees_TestData.xlsx | sheet1         | 1   |

  # An empty form cannot go on, which is what makes the scenarios below it mean anything: a
  # Next that was always enabled would prove nothing about any amount.
  @schoolfee @negative
  Scenario Outline: An empty form cannot go on
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open International School Fees from the sidebar
    Then the school fee form should be shown
    And the school fee transfer should not be ready to continue

    Examples:
      | excelFileName                         | excelSheetName | row |
      | InternationalSchoolFees_TestData.xlsx | sheet1         | 1   |

  # The arithmetic rather than the figures. A fee that is quoted and then not added, or added
  # twice, is the kind of error a scenario naming three numbers would stop catching the moment
  # the fee changed.
  @schoolfee
  Scenario Outline: The form works out the fee and what the transfer comes to
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open International School Fees from the sidebar
    Then the school fee form should be shown
    When I enter the stated minimum as the school fee amount
    Then the form should work out the fee and what it comes to
    And the school fee transfer should be ready to continue

    Examples:
      | excelFileName                         | excelSheetName | row |
      | InternationalSchoolFees_TestData.xlsx | sheet1         | 3   |

  # ------------------------------------------------------------------
  # What it will not take
  # ------------------------------------------------------------------

  # Less than the minimum is refused, but not in the way anything else in this application
  # refuses. Next stays enabled, the form works out a fee for an amount it will not take, and
  # pressing Next does nothing at all - no message, no movement. Asserted as it is rather than
  # as it ought to be, so that the day the form starts saying why, this scenario fails and gets
  # updated to expect the message.
  @schoolfee @negative @International_School_Fee_TC_003
  Scenario Outline: Less than the stated minimum amount can not be sent
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open International School Fees from the sidebar
    Then the school fee form should be shown
    When I enter one below the stated minimum as the school fee amount
    And I go on from the school fee amount
    Then the form should not go on, and should say nothing about why

    Examples:
      | excelFileName                         | excelSheetName | row |
      | InternationalSchoolFees_TestData.xlsx | sheet1         | 1   |

  # More than the wallet holds is not refused here at all: the form quotes a fee on it and
  # carries it through to choosing a school. Where it is refused - if it is - is somewhere past
  # the step this suite stops at, so what is asserted is what actually happens, and the comment
  # is the record that nothing checked the balance on the way past. The figure is worked out
  # from the balance the form is showing, because that moves every time the suite sends anything.
  @schoolfee @negative
  Scenario Outline: More than the wallet holds is carried past the amount step unchecked
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open International School Fees from the sidebar
    Then the school fee form should be shown
    When I enter more than the wallet holds as the school fee amount
    Then the school fee transfer should be ready to continue
    When I go on from the school fee amount
    Then the form should ask which school is being paid

    Examples:
      | excelFileName                         | excelSheetName | row |
      | InternationalSchoolFees_TestData.xlsx | sheet1         | 1   |

  # ------------------------------------------------------------------
  # Where it leads
  # ------------------------------------------------------------------

  # As far as this suite goes. Next does not send anything - the flow asks which country and
  # which school is being paid next - so going one step further proves the amount step hands on
  # properly without putting a transfer within reach of an accidental click.
  @schoolfee
  Scenario Outline: The amount step leads to choosing who is being paid
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open International School Fees from the sidebar
    Then the school fee form should be shown
    When I enter the stated minimum as the school fee amount
    And I go on from the school fee amount
    Then the form should ask which school is being paid

    Examples:
      | excelFileName                         | excelSheetName | row |
      | InternationalSchoolFees_TestData.xlsx | sheet1         | 3   |

  # ------------------------------------------------------------------
  # Paying a school the account has saved
  # ------------------------------------------------------------------
  #
  # The step after the amount asks which school is being paid, and offers two ways to answer:
  # a country and a form to fill in, or Template - the schools this account has saved. The
  # second is what these cover, because it is the one that carries data across, and data
  # carried across is data that can be carried across wrongly.
  #
  # Still nothing is sent. Choosing a saved school fills the step in and stops there.

  @schoolfee @template
  Scenario Outline: The saved schools are offered
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open International School Fees from the sidebar
    Then the school fee form should be shown
    When I enter the stated minimum as the school fee amount
    And I go on from the school fee amount
    Then the form should ask which school is being paid
    When I open the schools saved on the account
    Then the schools saved on the account should be listed

    Examples:
      | excelFileName                         | excelSheetName | row |
      | InternationalSchoolFees_TestData.xlsx | sheet1         | 4   |

  # Every box is asserted, not just the school's name. A template that carried the school across
  # but lost the student, or filled the student's name and left the identity number empty, would
  # be worse than one that did nothing: the fee would go to the right school for the wrong
  # person, and the form would look perfectly filled in.
  @schoolfee @template
  Scenario Outline: Choosing a saved school fills in the school and the student
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open International School Fees from the sidebar
    Then the school fee form should be shown
    When I enter the stated minimum as the school fee amount
    And I go on from the school fee amount
    Then the form should ask which school is being paid
    When I open the schools saved on the account
    Then the schools saved on the account should be listed
    When I choose the school saved in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the school and the student should be filled in from "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName                         | excelSheetName | row |
      | InternationalSchoolFees_TestData.xlsx | sheet1         | 4   |

  # ------------------------------------------------------------------
  # Paying one, for real
  # ------------------------------------------------------------------
  #
  # The exception to the rule at the top of this file, and it is tagged so it can be left out:
  # this scenario pays a school fee and the money goes. Run it deliberately with
  # -Dcucumber.filter.tags="@sends".
  #
  # It pays the school the account has saved, for the amount the form states as its minimum -
  # the least this route can spend - and it authorises with the PIN from -Dumpay.pin.
  #
  # Worth knowing about the step that sends: the button reads Next, exactly as it does on the
  # amount step before it, and pressing it submits the transfer. There is no summary and no
  # button that says Confirm. That is why the page object's method is called
  # sendFromTheSchoolStep rather than next - so no scenario presses it by accident.

  @schoolfee @sends @International_School_Fee_TC_002
  Scenario Outline: A school fee is paid to a saved school
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open International School Fees from the sidebar
    Then the school fee form should be shown
    When I enter the stated minimum as the school fee amount
    Then the form should work out the fee and what it comes to
    When I go on from the school fee amount
    Then the form should ask which school is being paid
    When I open the schools saved on the account
    And I choose the school saved in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the school and the student should be filled in from "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I send the school fee, which moves real money
    Then the school fee should go through

    Examples:
      | excelFileName                         | excelSheetName | row |
      | InternationalSchoolFees_TestData.xlsx | sheet1         | 4   |

  # ------------------------------------------------------------------
  # Paying a school that has not been saved
  # ------------------------------------------------------------------
  #
  # The other half of the school step, and a different code path from choosing a saved one: the
  # same boxes, filled by hand, with a country chosen rather than one that arrives with a
  # template. An account paying a school for the first time takes this way, and nothing covered
  # it.
  #
  # This sends. The school, the student and the country are in the sheet - row 5 - and the
  # amount is the smallest the form states. The checkbox beside the form is deliberately left
  # alone: ticking it would save this school as a template, and the saved-school scenarios read
  # that list.

  @schoolfee @sends @International_School_Fee_TC_001
  Scenario Outline: A school fee is paid to a school typed in for the first time
    Given I log into the UMPay application with valid email credentials using "<row>" of "<excelSheetName>" of "<excelFileName>"
    When I open International School Fees from the sidebar
    Then the school fee form should be shown
    When I enter the stated minimum as the school fee amount
    Then the form should work out the fee and what it comes to
    When I go on from the school fee amount
    Then the form should ask which school is being paid
    When I enter the school details in "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the school fee should be ready to send
    When I send the school fee, which moves real money
    Then the school fee should go through

    Examples:
      | excelFileName                        | excelSheetName | row |
      | InternationalSchoolFees_TestData.xlsx | Sheet1        | 5   |
