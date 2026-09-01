Feature: UMPay Registration
  As a new UMPay user
  I want to register an account with an email address or a phone number
  So that I can log into the application

  # The registration form is guarded by an image captcha. Rows whose CaptchaCode is
  # AUTO have it read by OCR; a row can also take the code from the test data or
  # from -Dumpay.captcha=<code> on a captcha bypassed environment. Scenarios still
  # tagged @manual are ones nobody has yet confirmed run start to finish without a
  # person watching, and the runner leaves them out of the unattended run.

  Background:
    Given I am on the UMPay registration page

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

  @register @smoke
  Scenario Outline: Successful registration with a new email address
    When I register with an email address using "<row>" of "<excelSheetName>" of "<excelFileName>"
    And I enter the verification code sent to the email address
    Then the registration should be accepted
    When I accept the policies shown to a new account
    And I set a PIN security code
    And I skip the two factor authentication prompt
    Then I should land on the UMPay home page
    And I should see the account wallets on the home page

    Examples:
      | excelFileName          | excelSheetName | row |
      | Register_TestData.xlsx | sheet1         | 1   |

  @register @manual
  Scenario Outline: Successful registration with a phone number
    When I register with a phone number using "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the registration should be accepted

    Examples:
      | excelFileName          | excelSheetName | row |
      | Register_TestData.xlsx | sheet1         | 5   |

  @register @negative @manual
  Scenario Outline: Registration is rejected for an email address that is already in use
    When I register with an email address using "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the registration should be rejected with the message in "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName          | excelSheetName | row |
      | Register_TestData.xlsx | sheet1         | 2   |

  @register @negative
  Scenario Outline: Registration is rejected when the captcha code is wrong
    When I register with an email address using "<row>" of "<excelSheetName>" of "<excelFileName>"
    Then the registration should be rejected with the message in "<row>" of "<excelSheetName>" of "<excelFileName>"

    Examples:
      | excelFileName          | excelSheetName | row |
      | Register_TestData.xlsx | sheet1         | 4   |

  @register @negative
  Scenario Outline: Registration is blocked when the password is shorter than six characters
    When I fill the registration form using "<row>" of "<excelSheetName>" of "<excelFileName>" without submitting it
    Then the browser should reject the "password" field with the message "Must be at least 6 characters alphanumeric!"

    Examples:
      | excelFileName          | excelSheetName | row |
      | Register_TestData.xlsx | sheet1         | 3   |

  @register
  Scenario: Existing users can reach the login page from the registration page
    Then I should be able to go to the login page from the registration page
