package com.umpay.utility;

/**
 * The security code that authorises money leaving the account.
 *
 * Kept out of the feature files and the spreadsheets on purpose: it is a credential rather than
 * test data, and a scenario reads better saying "with the account's PIN" than quoting four
 * digits. Override it for a run with -Dumpay.pin=...
 */
public final class SecurityPin {

	private SecurityPin() {
		// Static holder; there is nothing to construct.
	}

	/** The PIN this run should authorise with. */
	public static String value() {

		return System.getProperty("umpay.pin", "1111");
	}
}
