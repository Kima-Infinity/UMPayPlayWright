# UMPayPlayWright

The UMPay suite with Playwright driving the browser instead of Selenium.

It is a mirror, not a rewrite. The same eight feature files, the same Excel test data, the
same Cucumber and TestNG runner, the same ExtentReports output, the same captcha OCR and
mailbox helpers. A scenario that fails here fails for the same reason it would fail in
`UMPay`, which is the point: the two suites stay comparable.

`UMPay` itself is untouched.

## Running it

    mvn test                                  the unattended run
    mvn test -Dheadless=true                  without a visible window
    mvn test -Dcucumber.filter.tags="@login"  one area

The first run downloads the browsers Playwright manages for itself. There is no
chromedriver to keep in step with Chrome, so the `Drivers` folder that `UMPay` carries has
no equivalent here.

## What changed, and why

| Selenium | Playwright | Note |
|---|---|---|
| `WebDriver` | `Page` | still called `driver` in `BaseClass`, so the forty files that refer to it did not all need renaming |
| `@FindBy` + `PageFactory` | `Locator` fields built in the constructor | a Locator is lazy in the same way a proxy was: it resolves when used |
| `WebDriverWait` / `ExpectedConditions` | mostly nothing | every action waits for the element to be visible, stable and able to receive the event |
| `wait.until(d -> ...)` | `Wait.until(...)` | conditions about the page rather than one element still need polling |
| `Select` | `selectOption` | |
| `JavascriptExecutor` click | `dispatchEvent("click")` | same effect, no cast |
| `StaleElementReferenceException` | gone | a Locator resolves afresh every time, so there is nothing to go stale |
| a browser per scenario | a context per scenario | same isolation, a fraction of the cost |

## Three traps the port hit, in case they come up again

**An id with a dot in it.** `By.id("locale.dropdown-icon")` takes the id literally. The CSS
shorthand `#locale.dropdown-icon` does not - it reads as *id `locale`, class
`dropdown-icon`* - so the language switcher was silently never clicked. Every id is matched
as `[id='...']` here for that reason.

**`findElements` was covered by an implicit wait.** The Selenium suite set a five second
implicit wait, so a list read a moment too early was quietly retried. `Locator.all()`
answers immediately, so the same call returned nothing and the caller concluded the dialog
was empty. `Wait.all(...)` waits for the first match before listing, which restores the old
behaviour honestly: an empty list now means the page really has none.

**`innerText` on an `<option>` is empty.** Options inside a closed `<select>` are not
rendered, and `innerText` only sees rendered text. Use `textContent()`, and read an option's
value from the attribute rather than with `inputValue()`.

## Where it stands

The full suite has been run against the test environment. **47 of 57 scenarios pass**, and
the ten failures are the same ones `UMPay` reports:

| Failure | Also fails on Selenium? |
|---|---|
| Global Transfer x4 (existing template / new receiver, both routes) | yes - the Receiver Information locators |
| A registered phone number signs in | yes - the number is not bound to an account |
| Reset: unknown address offered a reset | yes - and probably deliberate on the app's part |
| Reset: going back from verification crashes the app | yes |
| Reset: unknown number refused without a message | yes |
| Successful registration | no - the captcha OCR had a bad run of reads and used all ten attempts. Passes on a re-run. |
| Successful Withdraw | no - fixed, see below. Passes on a re-run. |

Both of the two that were not on that list have been re-run since and pass, so the two
suites agree.

Timing, like for like: the login area takes 155s here against 118s under Selenium for one
fewer scenario, and the full suite 1484s against 2178s on the first Playwright run before
the fixes below. A context per scenario rather than a browser per scenario is most of that.

## Three defects the first full run found

Worth writing down, because each is a way the port could silently disagree with the original.

**The 2FA prompt stopped being dismissed.** `isDisplayed()` ran under Selenium's five second
implicit wait, so asking "is the prompt there?" right after a page load quietly waited for
it. Playwright answers immediately and truthfully - not yet - so the check said no prompt,
navigation went ahead, and the dialog then drew a full screen backdrop over everything. Every
click afterwards landed on the overlay. That one defect caused fourteen of the first run's
twenty-four failures. `Wait.appears(...)` is the fix, and the rule is: use it where the
question is *has this arrived*, and plain `isVisible()` where the question is *is it there
right now*.

**Navigation had a thirty second cap.** Playwright's default. The Selenium suite set no page
load timeout at all, so a slow login page simply took as long as it took. One withdraw
scenario failed in its Before hook with every step skipped. Now ninety seconds.

**`findElements` was riding on the same implicit wait.** See `Wait.all(...)` above.
