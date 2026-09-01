package com.umpay.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The one place the mailbox password comes from.
 *
 * Two features need it and used to look it up separately: reading the
 * registration code over IMAP, and sending the run's report over SMTP. They now
 * agree, so a machine that can fetch the code can also send the report.
 *
 * It is looked for in three places, most explicit first:
 *
 * <ol>
 *   <li>{@code -Dumpay.mail.password=...} on the command line, for a one-off run
 *       or a CI job that injects it as a secret</li>
 *   <li>the {@code UMPAY_MAIL_PASSWORD} environment variable</li>
 *   <li>{@code Config/secrets.properties}, key {@code mail.password}</li>
 * </ol>
 *
 * The file exists because environment variables are awkward to rely on locally:
 * on Windows a variable set now does not reach an already running terminal or
 * IDE, so a local run keeps finding nothing until everything is restarted. The
 * file is read fresh each time and needs no restart.
 *
 * It must never be committed. It is listed in .gitignore, and
 * secrets.properties.example is the template that is committed in its place.
 * Nothing here ever prints the password itself, only where it was found.
 */
public class MailCredentials {

	public static final String SYSTEM_PROPERTY = "umpay.mail.password";
	public static final String ENVIRONMENT_VARIABLE = "UMPAY_MAIL_PASSWORD";
	public static final String SECRETS_FILE = "/Config/secrets.properties";
	public static final String SECRETS_KEY = "mail.password";

	private MailCredentials() {
		// Static holder; there is nothing to construct.
	}

	/**
	 * The app password with Google's display spaces removed, or an empty string
	 * when it is not configured anywhere. An empty result is not an error: the
	 * callers fall back to a human reading the code, and skip the report mail.
	 */
	public static String password() {

		String fromProperty = clean(System.getProperty(SYSTEM_PROPERTY));

		if (!fromProperty.isEmpty()) {
			return fromProperty;
		}

		String fromEnvironment = clean(System.getenv(ENVIRONMENT_VARIABLE));

		if (!fromEnvironment.isEmpty()) {
			return fromEnvironment;
		}

		return clean(fromSecretsFile());
	}

	public static boolean isConfigured() {

		return !password().isEmpty();
	}

	/**
	 * Every configured password, best first, keyed by where it came from.
	 *
	 * Precedence alone is not enough, because the most explicit source is not always
	 * the working one. A revoked app password left behind in UMPAY_MAIL_PASSWORD
	 * silently shadowed a perfectly good one in secrets.properties, and the run
	 * failed with "Invalid credentials" while a usable credential sat unused on the
	 * same machine. Callers that can tell whether a credential worked - the IMAP
	 * reader can, it either authenticates or it does not - should walk this in order
	 * rather than take {@link #password()} as the only answer.
	 *
	 * Duplicates are dropped so the same value is never tried twice, which matters
	 * because a failed Gmail login is slow and counts against the account.
	 *
	 * @return source description to password, in the order they should be tried
	 */
	public static Map<String, String> candidates() {

		Map<String, String> found = new LinkedHashMap<>();

		add(found, "the " + SYSTEM_PROPERTY + " system property", System.getProperty(SYSTEM_PROPERTY));
		add(found, "the " + ENVIRONMENT_VARIABLE + " environment variable", System.getenv(ENVIRONMENT_VARIABLE));
		add(found, SECRETS_FILE.substring(1), fromSecretsFile());

		return found;
	}

	private static void add(Map<String, String> found, String source, String rawValue) {

		String value = clean(rawValue);

		if (!value.isEmpty() && !found.containsValue(value)) {
			found.put(source, value);
		}
	}

	/**
	 * Where the password was found, for the run log. Says the source only - never
	 * the value - so a log or a report attachment can be shared safely.
	 */
	public static String source() {

		if (!clean(System.getProperty(SYSTEM_PROPERTY)).isEmpty()) {
			return "the " + SYSTEM_PROPERTY + " system property";
		}

		if (!clean(System.getenv(ENVIRONMENT_VARIABLE)).isEmpty()) {
			return "the " + ENVIRONMENT_VARIABLE + " environment variable";
		}

		if (!clean(fromSecretsFile()).isEmpty()) {
			return SECRETS_FILE.substring(1);
		}

		return "nowhere - set " + ENVIRONMENT_VARIABLE + " or create " + SECRETS_FILE.substring(1)
				+ " from secrets.properties.example";
	}

	private static String fromSecretsFile() {

		File secrets = new File(System.getProperty("user.dir") + SECRETS_FILE);

		if (!secrets.isFile()) {
			return "";
		}

		Properties properties = new Properties();

		try (FileInputStream in = new FileInputStream(secrets)) {
			properties.load(in);
		} catch (IOException unreadable) {
			System.out.println("Could not read " + secrets + ": " + unreadable.getMessage());
			return "";
		}

		return properties.getProperty(SECRETS_KEY, "");
	}

	/** Google shows app passwords in groups of four; the spaces are not part of it. */
	private static String clean(String value) {

		return value == null ? "" : value.replace(" ", "").trim();
	}
}
