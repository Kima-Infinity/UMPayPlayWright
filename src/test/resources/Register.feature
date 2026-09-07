Feature: UMPay Registration
  As a new UMPay user
  I want to register an account with an email address or a phone number
  So that I can log into the application

  # The registration form is guarded by an image captcha. Rows whose CaptchaCode is
  # AUTO have it read by OCR; a row can also take the code from the test data or
  # from -Dumpay.captcha=<code> on a captcha bypassed environment. Scenarios still
  # tagged @manual are ones nobody has yet confirmed run start to finish without a
  # person watching, and the runner leaves them out of the unattended run.

  # The full journey from an empty form to a usable account: the captcha is read
  # by OCR, the six digit code is read out of the mailbox over IMAP, the two
  # policies that greet every new account are accepted, the PIN they are then
  # asked for is set, and the 2FA prompt waiting on the home page is skipped. Only
  # then is the account usable, which is why the scenario ends by reading the
  # wallets rather than by checking a URL.
  #
  # This runs unattended and is tagged accordingly. It does need captcha.ocr.enabled
  # and mail.imap.enabled to be on: with either switched off the same steps still
  # work, but they pause for a person to type the code, which in an unattended run
  # means waiting out captcha.manual.timeout and then failing.
  #
  # Every run registers a real account on the test environment.

  @register @smoke @email @Register_TC_001
  Scenario Outline: Successful registration with a new email address
    Given I am on the UMPay "<page>" page
    When I register with an email address using "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I enter the verification code sent to the email address
    Then the registration should be accepted
    When I accept the policies shown to a new account
    And I set a PIN security code
    And I skip the two factor authentication prompt
    Then I should land on the UMPay home page
    And I should see the account wallets on the home page

    Examples:
      | excelFileName          | excelSheetName | row | page         |
      | Register_TestData.xlsx | sheet1         | 1   | registration |

  # A phone registration ends at the verification step rather than at a usable account.
  # The code is sent by SMS and nothing in the suite can read one, where the email flow
  # reads its code out of the mailbox over IMAP. Reaching the step the application only
  # shows once it has accepted the form and sent a code is what this can honestly verify.
  @register @phone @Register_TC_002
  Scenario Outline: Successful registration with a phone number
    Given I am on the UMPay "<page>" page
    When I register with a phone number using "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the registration should reach the phone verification step

    Examples:
      | excelFileName          | excelSheetName | row | page         |
      | Register_TestData.xlsx | sheet1         | 5   | registration |

  @register @negative @email @Register_TC_003
  Scenario Outline: Registration is rejected for an email address that is already in use
    Given I am on the UMPay "<page>" page
    When I register with an email address using "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the registration should be rejected with the message in "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName          | excelSheetName | row | page         |
      | Register_TestData.xlsx | sheet1         | 2   | registration |

  @register @negative @phone @Register_TC_004
  Scenario Outline: Registration is rejected for phone number that is already in use
    Given I am on the UMPay "<page>" page
    When I register with a phone number using "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the registration should be rejected with the message in "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName          | excelSheetName | row | page         |
      | Register_TestData.xlsx | sheet1         | 6   | registration |

  @register @negative @Register_TC_005
  Scenario Outline: Registration is rejected when the captcha code is wrong
    Given I am on the UMPay "<page>" page
    When I register with an email address using "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the registration should be rejected with the message in "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName          | excelSheetName | row | page         |
      | Register_TestData.xlsx | sheet1         | 4   | registration |

  @register @negative @Register_TC_006
  Scenario Outline: Registration is blocked when the password is shorter than six characters
    Given I am on the UMPay "<page>" page
    When I fill the registration form using "<row>" of "<excelSheetName>" of "<excelFileName>" without submitting it
    Then the browser should reject the "password" field with the message "Must be at least 6 characters alphanumeric!"

    Examples:
      | excelFileName          | excelSheetName | row | page         |
      | Register_TestData.xlsx | sheet1         | 3   | registration |

  @register
  Scenario Outline: Existing users can navigate to the login page from the registration page
    Given I am on the UMPay "<page>" page
    Then I should be able to go to the login page from the registration page

    Examples:
      | page         |
      | registration |
