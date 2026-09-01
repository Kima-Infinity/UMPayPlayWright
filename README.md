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

## Running it from Jenkins

`Jenkinsfile` in this repository defines the job, so create a **Pipeline** job with
*Definition: Pipeline script from SCM*, pointing at this repository. Nothing else needs
configuring in the UI.

Set **Branch Specifier** to `*/main`. Jenkins defaults that field to `*/master`, which this
repository does not have, and the failure it gives - "couldn't find remote ref
refs/heads/master" - does not mention the field it came from.

The pipeline uses `checkout scm`, so the branch, the URL and the credentials come from the
job's own SCM configuration and are set in exactly one place.

It runs two ways:

- **On demand** - *Build with Parameters*, where `TAGS` chooses what to run.
- **On a schedule** - `H 2 * * *`, nightly.

### The schedule deliberately does not run everything

Some scenarios spend real things on the test environment: Deposit, Withdraw, Convert and
GlobalTransfer move real money between wallets, Register creates a real account on every
run, and the reset scenarios are rate limited by an endpoint whose block escalates from a
minute to an hour if pushed. A suite that does all of that unattended every night is a
standing cost and a way to trip the rate limiter for everyone.

So the nightly build runs `@login or @transfer` - the areas that read, open forms and assert
without sending anything. Spend the rest on purpose: *Build with Parameters*, and set `TAGS`
to empty for everything, or to `@reset` or `@register` for one area.

`not @manual` is always appended, whatever `TAGS` says. That matters: the runner's own
filter is replaced by a `-D` tag expression, and `@manual` is what keeps the two scenarios
that deliberately lock the shared account out of an unattended run.

### What the agent needs

| Needs | Why |
|---|---|
| JDK 17 and Maven, configured under *Manage Jenkins -> Tools* as `jdk-17` and `maven-3.9.9` | the build. The pipeline's `tools` block refers to them **by those names** - rename them there and the build fails with "No tool named ... found". Without the block, Jenkins inherits only the PATH of the account it runs as, which on Windows is normally LocalSystem and has no Maven |
| Python 3 on the PATH | the captcha OCR. `-Dcaptcha.ocr.python` overrides the absolute path in `config.properties`, which is right for a developer machine and wrong for an agent |
| A `umpay-mail-password` Secret text credential | reading the registration code over IMAP, and emailing the report. `Config/secrets.properties` is not in this repository, so the job supplies it as `UMPAY_MAIL_PASSWORD` |
| A `github-umpay` credential | cloning, if the repository is private |
| Roughly 200MB of disk for the browsers | Playwright downloads them on first use, cached under `PLAYWRIGHT_BROWSERS_PATH` so it happens once per agent rather than once per build |

No browser needs installing and there is no chromedriver to keep in step with Chrome -
Playwright manages both.

### Reading a result

The job publishes `target/cucumber.xml` as JUnit results, so Jenkins shows a test trend and
marks a build with failing scenarios **unstable** rather than **failed**. A **failed** build
is usually the agent rather than the application - missing Maven, missing Python, or a
missing credential.

Ten scenarios are expected to fail today, and they are listed under *Where it stands* above.
Check a new failure against that list before treating it as a regression.
