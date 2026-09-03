Feature: UMPay end to end journey

  As a UMPay user
  I want to register, move money through every flow, and sign out
  So that one run proves the whole product hangs together rather than each part alone

  # WHY THIS EXISTS ALONGSIDE THE PER-FEATURE FILES
  #
  # Register.feature, Deposit.feature and the rest each prove one flow, and each signs in
  # for itself, so they say nothing about whether the flows work in succession against one
  # session. This scenario is deliberately one long scenario for that reason: everything
  # after the first step runs on the state the previous step left behind.
  #
  # That is also its weakness. A single scenario stops at the first failure, so a broken
  # Deposit hides Withdraw, Transfer and Convert. Keep the per-feature files for pinpointing
  # a flow; use this one to prove the journey.
  #
  # ORDERING NOTES
  #
  # - The sign out after registration is not decoration. Registration finishes signed in as
  #   the account it just created, while the login step fills a login form and expects one on
  #   screen. Without signing out first, Login would look for a form that is not there.
  # - Only the first flow signs in. The per-feature files each begin with a login step because
  #   they run standalone; repeating that here would try to log in while already logged in.
  #   The money flows below use their navigate steps instead.
  # - Sheet names are copied from each feature exactly. Convert's is "Sheet1" with a capital
  #   S while every other file uses "sheet1", and the lookup is case sensitive.
  #
  # WHAT IT COSTS TO RUN
  #
  # A real account is created on the test environment, a real verification email is read out
  # of the mailbox, and real money moves - Convert asserts the source balance actually falls.
  # Test environment only.
  #
  # Run it on its own:  mvn test -Dcucumber.filter.tags="@e2e"

  @e2e
  Scenario: A new account registers, signs in, moves money through every flow and signs out

    # 1 - Register: captcha read by OCR, the emailed code read over IMAP
    Given I am on the UMPay registration page
    When I register with an email address using "1" of "sheet1" of "Register_TestData.xlsx"
    And I enter the verification code sent to the email address
    Then the registration should be accepted
    When I accept the policies shown to a new account
    And I set a PIN security code
    And I skip the two factor authentication prompt
    Then I should land on the UMPay home page
    And I should see the account wallets on the home page

    # Hand the session back so the login step has a login form to fill
    And I should be able to successfully log out

    # 2 - Login
    When I log into the UMPay application with valid email credentials using "1" of "sheet1" of "Login_TestData.xlsx"
    Then I check and validate all the homepage contents

    # 3 - Deposit
    When I navigate to Deposit page
    Then I should be able to initiate a deposit transaction using "1" of "sheet1" of "Deposit_TestData.xlsx"

    # 4 - Withdraw
    When I navigate to Withdraw page
    Then I should be able to initiate a withdraw transaction using "1" of "sheet1" of "Withdraw_TestData.xlsx"

    # 5 - Convert
    When I navigate to the Convert page
    And I convert the amount in "1" of "Sheet1" of "Convert_TestData.xlsx"
    Then the conversion should be confirmed with the message in "1" of "Sheet1" of "Convert_TestData.xlsx"
    And the source wallet balance should have gone down by the converted amount

    # 6 - Logout
    Then I should be able to successfully log out
