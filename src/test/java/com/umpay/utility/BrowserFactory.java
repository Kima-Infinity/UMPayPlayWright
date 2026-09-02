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
	 * The size a headless run gets.
	 *
	 * Headless has no window to maximise and no screen to measure, and a build agent must
	 * render the same page every time or a scenario can pass on a desk and fail in CI for a
	 * reason nobody can see. So headless is always this, whatever the machine underneath it
	 * happens to have. It is also what a headed run falls back to if the screen cannot be
	 * measured for the warning below.
	 */
	private static final int HEADLESS_WIDTH = 1920;

	private static final int HEADLESS_HEIGHT = 1080;

	/**
	 * The width below which the site stops being the one the suite was written against.
	 *
	 * Somewhere around here it changes to its mobile layout and the locators stop matching.
	 * A maximised window is whatever the screen is, so this cannot be enforced - but a run
	 * on a screen narrower than this is worth a word before the failures start arriving,
	 * because the cause is the machine rather than the application.
	 */
	private static final int NARROW_SCREEN_WIDTH = 1280;

	/** Whether the narrow-screen warning has already been given this run. */
	private static boolean warned;

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
	 * How the browser window is asked for.
	 *
	 * A headed run is maximised, so it fills the screen it is actually on and nothing hangs
	 * off the edge. That matters on a scaled display: a 1920x1080 panel at 125% scaling
	 * offers applications 1536x864, so the fixed 1920 this used to ask for was wider than
	 * the screen had to give and the remainder spilled onto whatever monitor sat beside it.
	 * Maximising asks the window manager for the answer instead of working it out here, and
	 * the window manager is never wrong about its own screen.
	 *
	 * Headless has no window to maximise, so there the size is stated outright.
	 */
	private static List<String> launchArgs() {

		return isHeadless()
				? List.of("--window-size=" + HEADLESS_WIDTH + "," + HEADLESS_HEIGHT)
				: List.of("--start-maximized");
	}

	/**
	 * Says so when the screen is too narrow for the layout the suite expects.
	 *
	 * Once per run, and only a warning: the run is still worth attempting and the machine is
	 * not something the suite can change. But a locator that has always matched suddenly not
	 * matching is a confusing thing to debug, and "this screen is narrower than the desktop
	 * layout needs" is the sentence that saves the hour.
	 */
	private static synchronized void warnIfScreenIsNarrow() {

		if (warned || isHeadless()) {
			return;
		}

		warned = true;

		int width = screenWidth();

		if (width < NARROW_SCREEN_WIDTH) {
			System.out.println("This screen is " + width + " logical pixels wide, under the "
					+ NARROW_SCREEN_WIDTH + " the desktop layout needs. The site may render "
					+ "its mobile layout, in which case locators will not match.");
		}
	}

	/**
	 * The primary screen's width in logical pixels - the unit Chromium sizes windows in.
	 *
	 * The default screen device rather than getMaximumWindowBounds, which is documented to
	 * be allowed to return the whole virtual desktop on a multi-screen machine, and would
	 * therefore call a narrow laptop wide as soon as a second monitor was plugged in.
	 */
	private static int screenWidth() {

		try {
			if (java.awt.GraphicsEnvironment.isHeadless()) {
				return HEADLESS_WIDTH;
			}

			return java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
					.getDefaultScreenDevice().getDefaultConfiguration().getBounds().width;

		} catch (Throwable noScreen) {
			// A machine with no display, or one that will not say. Assume it is fine rather
			// than warn about something that was never measured.
			return HEADLESS_WIDTH;
		}
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
					.setArgs(launchArgs()));
		}

		warnIfScreenIsNarrow();

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

	/**
	 * A fresh, isolated profile: its own cookies, its own storage, nothing carried over.
	 *
	 * A headed run gets no viewport at all, which is what lets the page fill the maximised
	 * window. Playwright's default is to fix the viewport at 1280x720 and leave the rest of
	 * the window blank, so the setting has to be turned off rather than merely left alone -
	 * setViewportSize(null) is how that is spelled.
	 */
	public static BrowserContext newContext() {

		Browser.NewContextOptions options = new Browser.NewContextOptions();

		if (isHeadless()) {
			options.setViewportSize(HEADLESS_WIDTH, HEADLESS_HEIGHT);
		} else {
			options.setViewportSize(null);
		}

		return browser.newContext(options);
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
