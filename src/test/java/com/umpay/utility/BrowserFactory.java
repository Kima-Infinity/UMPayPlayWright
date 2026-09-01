package com.umpay.utility;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.util.List;

/**
 * Starts and stops the browser, the Playwright way.
 *
 * Selenium handed the suite one object, a WebDriver, that was both the browser and the
 * page. Playwright separates them, and the separation is worth keeping rather than hiding
 * behind a driver-shaped wrapper:
 *
 *   Playwright      the connection to the driver process. One per run.
 *   Browser         the browser process itself. One per run.
 *   BrowserContext  an isolated profile - its own cookies and storage. Cheap to make.
 *   Page            a tab.
 *
 * The Selenium suite started a whole browser for every scenario and quit it again in the
 * Cucumber hooks. That isolation is worth keeping - a scenario should not inherit the
 * session of whatever ran before it - but paying for a browser launch to get it is not.
 * So the Playwright and Browser are opened once for the run and every scenario gets a fresh
 * BrowserContext instead: same clean cookies and storage, a fraction of the cost.
 *
 * That makes {@link #quitBrowser} a smaller thing than it was - it ends the scenario's
 * context, not the browser - and adds {@link #shutdown}, which the run calls once at the
 * end. Anything that closed the browser mid-run under Selenium therefore ends its own
 * scenario's session here and leaves the run able to continue, which is what it meant to do.
 */
public class BrowserFactory {

	/**
	 * The viewport every run gets.
	 *
	 * Selenium maximised a visible window and asked for 1920x1080 when headless. Playwright
	 * has no window to maximise - the viewport is set outright - so both modes get the same
	 * size and the desktop layout is what the locators always meet. Anything much narrower
	 * drops the site into its mobile layout and the locators stop matching.
	 */
	private static final int VIEWPORT_WIDTH = 1920;

	private static final int VIEWPORT_HEIGHT = 1080;

	/** How long a page load may take before the run gives up on it. */
	private static final double NAVIGATION_TIMEOUT_MILLIS = 90_000;

	private static Playwright playwright;

	private static Browser browser;

	private static BrowserContext context;

	/**
	 * Whether the run should start the browser without a visible window.
	 *
	 * Checked in this order, so a single run can be switched without editing a file:
	 *   -Dheadless=true on the command line   (mvn test -Dheadless=true)
	 *   headless=true in Config/config.properties
	 *   false
	 *
	 * Anything other than "true" (ignoring case) counts as false.
	 */
	public static boolean isHeadless() {

		String override = System.getProperty("headless");

		if (override != null && !override.isBlank()) {
			return Boolean.parseBoolean(override.trim());
		}

		return new ConfigDataProvider().isHeadless();
	}

	/**
	 * Opens the browser at {@code url} and hands back the tab everything else works with.
	 *
	 * There is no implicit wait to set. Selenium needed one because findElement asked the
	 * page a question the instant it was called; every Playwright action waits for the
	 * element to be there, visible, stable and able to receive the event before it acts. The
	 * five second implicit wait that the Selenium suite carried - and that one page had to
	 * switch off to measure absence - has no equivalent here and needs none.
	 */
	public static synchronized Page startBrowser(String url) {

		if (browser == null) {
			playwright = Playwright.create();

			browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
					.setHeadless(isHeadless())
					// Chromium's own default of a small window would put the site in its
					// mobile layout before the viewport below is applied.
					.setArgs(List.of("--window-size=" + VIEWPORT_WIDTH + "," + VIEWPORT_HEIGHT)));
		}

		context = newContext();

		// Playwright caps a navigation at 30 seconds by default. The Selenium suite had no
		// page load timeout at all - the line setting one is commented out in its
		// BrowserFactory - so a slow load simply took as long as it took. A 30 second cap
		// is a behaviour change the port did not intend, and it cost a withdraw scenario a
		// run: the login page took a moment longer than usual and the Before hook failed
		// with every step skipped. Ninety seconds is generous enough to match the old
		// behaviour in practice while still ending a run that is truly stuck.
		context.setDefaultNavigationTimeout(NAVIGATION_TIMEOUT_MILLIS);

		Page page = context.newPage();

		// Responses are events in Playwright, so the listener has to be in place before the
		// first navigation or the calls that set the page up are never seen.
		com.umpay.utility.ApiLog.attach(page);

		page.navigate(url);

		return page;
	}

	/** A fresh, isolated profile: its own cookies, its own storage, nothing carried over. */
	public static BrowserContext newContext() {

		return browser.newContext(new Browser.NewContextOptions()
				.setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT));
	}

	/**
	 * Ends the scenario's session: the tab and the profile behind it.
	 *
	 * Takes the page because that is what the callers hold, and because the Selenium suite
	 * spelled this quitBrowser(driver). The browser itself stays up for the next scenario;
	 * {@link #shutdown} is what finally closes it.
	 */
	public static synchronized void quitBrowser(Page page) {

		if (page != null && !page.isClosed()) {
			page.close();
		}

		closeQuietly(context, "browser context");
		context = null;
	}

	/** Closes the browser and the Playwright connection. Called once, at the end of a run. */
	public static synchronized void shutdown() {

		closeQuietly(context, "browser context");
		context = null;

		closeQuietly(browser, "browser");
		browser = null;

		closeQuietly(playwright, "Playwright");
		playwright = null;
	}

	private static void closeQuietly(AutoCloseable closeable, String what) {

		if (closeable == null) {
			return;
		}

		try {
			closeable.close();
		} catch (Exception cannotClose) {
			System.out.println("Could not close the " + what + ": " + cannotClose.getMessage());
		}
	}
}
