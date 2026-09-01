package com.umpay.utility;

import jakarta.mail.Address;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pulls the emailed registration code out of the mailbox over IMAP, so the
 * happy path no longer needs a human reading an inbox.
 *
 * Freshness is the whole problem here. An earlier run submitted a code from the
 * previous day that Chrome had left in the field, and the backend rejected it -
 * so picking "a six digit number from the inbox" is not good enough. The guard
 * is the address itself: every run registers a fresh {@code +timestamp} alias,
 * Gmail delivers it to the same inbox, and only one message can ever be
 * addressed to that alias. Matching on it makes a stale code impossible rather
 * than merely unlikely.
 *
 * Credentials come from the same Gmail app password the report mailer uses, read
 * from UMPAY_MAIL_PASSWORD. Without it this reports itself unconfigured and the
 * caller falls back to a human typing the code.
 */
public class OtpMailReader {

	private static final String CONFIG_PATH = "/Config/config.properties";

	/** How often to look for the message while waiting for it to arrive. */
	private static final long POLL_INTERVAL_MILLIS = 5000;

	/** Only the tail of the inbox is worth scanning; the message is always recent. */
	private static final int MESSAGES_TO_SCAN = 25;

	/** "your code is 123456" and friends, preferred over any loose six digit run. */
	private static final Pattern LABELLED_CODE =
			Pattern.compile("(?i)(?:code|otp|pin)\\D{0,30}?(\\d{6})");

	/** Fallback: a six digit number that is not part of a longer one. */
	private static final Pattern BARE_CODE =
			Pattern.compile("(?<!\\d)(\\d{6})(?!\\d)");

	private final Properties config = new Properties();

	public OtpMailReader() {

		String path = System.getProperty("user.dir") + CONFIG_PATH;

		try (FileInputStream in = new FileInputStream(path)) {
			config.load(in);
		} catch (IOException e) {
			System.out.println("Could not read " + path + " for the IMAP settings: " + e.getMessage());
		}
	}

	/** Whether there is enough to attempt a mailbox read at all. */
	public boolean isConfigured() {

		return Boolean.parseBoolean(get("mail.imap.enabled", "false"))
				&& !get("mail.imap.host", "").isBlank()
				&& !get("mail.from", "").isBlank()
				&& !password().isBlank();
	}

	/**
	 * How many messages the inbox holds right now, as a mark to read forward from.
	 *
	 * Registration does not need this: every run registers a fresh +alias, so the only
	 * message ever addressed to it is that run's own. A password reset has no such luxury -
	 * it goes to the account's real address, which already holds codes from earlier runs.
	 * Taking the count before asking for the reset, and then refusing to look at anything
	 * that was already there, is what makes "a code arrived" mean this code rather than
	 * some code.
	 *
	 * Returns -1 when the mailbox cannot be reached, which {@link #waitForCodeAfter} reads
	 * as "no mark", so a mail problem shows up as no code rather than as a false pass.
	 */
	public int mailboxSize() {

		if (!isConfigured()) {
			return -1;
		}

		Store store = null;

		try {
			store = connect(get("mail.imap.host", "imap.gmail.com"), get("mail.from", ""));

			Folder inbox = store.getFolder("INBOX");
			inbox.open(Folder.READ_ONLY);

			try {
				return inbox.getMessageCount();
			} finally {
				inbox.close(false);
			}
		} catch (Exception cannotRead) {
			System.out.println("Could not measure the mailbox: " + cannotRead.getMessage());
			return -1;
		} finally {
			closeQuietly(store);
		}
	}

	/**
	 * The same wait, but only messages that arrived after the mailbox held {@code mark}
	 * messages count.
	 *
	 * Without the mark this would return the newest matching message the moment it looked,
	 * and on the first poll that is regularly a code from a previous run - which passes
	 * while proving nothing.
	 */
	public String waitForCodeAfter(String recipient, int timeoutSeconds, int mark) {

		if (mark < 0) {
			System.out.println("No mailbox mark was taken, so nothing can be said about"
					+ " whether a new code arrived.");
			return "";
		}

		return waitForCode(recipient, timeoutSeconds, mark);
	}

	/**
	 * Waits for the message addressed to {@code recipient} and returns its six
	 * digit code, or an empty string if it never arrived or the mailbox could not
	 * be read. An empty return means "ask a human instead", never "fail the run".
	 *
	 * Safe without a mark for registration, whose +alias can only ever have one
	 * message addressed to it. Anything reading a code sent to a real address wants
	 * {@link #waitForCodeAfter} instead.
	 *
	 * @param recipient      the +alias the form registered
	 * @param timeoutSeconds how long to keep looking
	 */
	public String waitForCode(String recipient, int timeoutSeconds) {

		return waitForCode(recipient, timeoutSeconds, 0);
	}

	private String waitForCode(String recipient, int timeoutSeconds, int mark) {

		if (!isConfigured()) {
			System.out.println("IMAP is not configured - password came from " + MailCredentials.source());
			return "";
		}

		String host = get("mail.imap.host", "imap.gmail.com");
		String user = get("mail.from", "");

		System.out.println("Watching " + user + " over IMAP for the code sent to " + recipient
				+ " (credential from " + MailCredentials.source() + ")");

		Store store = null;

		try {
			store = connect(host, user);

			long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

			while (System.currentTimeMillis() < deadline) {

				String code = findCode(store, recipient, mark);

				if (!code.isEmpty()) {
					System.out.println("Verification code read from the mailbox: " + code);
					return code;
				}

				Thread.sleep(POLL_INTERVAL_MILLIS);
			}

			System.out.println("No message for " + recipient + " arrived within " + timeoutSeconds + " seconds");
			return "";

		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			return "";
		} catch (Exception cannotRead) {
			System.out.println("Could not read the mailbox over IMAP: " + cannotRead.getMessage());
			return "";
		} finally {
			closeQuietly(store);
		}
	}

	/**
	 * Connects with the first configured password Gmail actually accepts.
	 *
	 * Trying only the highest-precedence credential is what made this fail in
	 * practice: a revoked app password sitting in UMPAY_MAIL_PASSWORD shadowed a
	 * working one in secrets.properties, so the run reported "Invalid credentials"
	 * with a usable credential on the same machine. Authentication is the one test
	 * that settles which is real, so it is used as the tie-breaker.
	 *
	 * Only the source is ever logged, never the password.
	 */
	private Store connect(String host, String user) throws MessagingException {

		Properties props = new Properties();
		props.put("mail.store.protocol", "imaps");
		props.put("mail.imaps.host", host);
		props.put("mail.imaps.port", get("mail.imap.port", "993"));
		props.put("mail.imaps.ssl.enable", "true");
		props.put("mail.imaps.connectiontimeout", "20000");
		props.put("mail.imaps.timeout", "20000");

		MessagingException lastFailure = null;

		for (Map.Entry<String, String> candidate : MailCredentials.candidates().entrySet()) {

			Store store = Session.getInstance(props).getStore("imaps");

			try {
				store.connect(host, user, candidate.getValue());
				System.out.println("Mailbox opened with the credential from " + candidate.getKey());
				return store;

			} catch (MessagingException rejected) {
				System.out.println("The credential from " + candidate.getKey()
						+ " was refused: " + rejected.getMessage());
				closeQuietly(store);
				lastFailure = rejected;
			}
		}

		throw lastFailure == null
				? new MessagingException("No mailbox password is configured anywhere")
				: lastFailure;
	}

	/**
	 * Scans the newest messages for one addressed to the alias.
	 *
	 * The folder is reopened on every pass on purpose: an IMAP folder held open
	 * reports the message count it had when it was opened, so a folder opened
	 * before the mail arrived would never see it and this would poll forever.
	 */
	private String findCode(Store store, String recipient, int mark)
			throws MessagingException, IOException {

		Folder inbox = store.getFolder("INBOX");

		try {
			inbox.open(Folder.READ_ONLY);

			int total = inbox.getMessageCount();
			int oldest = Math.max(1, total - MESSAGES_TO_SCAN + 1);

			// Anything at or below the mark was in the mailbox before the code was asked
			// for, so it cannot be the code that was asked for.
			oldest = Math.max(oldest, mark + 1);

			for (int index = total; index >= oldest; index--) {

				Message message = inbox.getMessage(index);

				if (!addressedTo(message, recipient)) {
					continue;
				}

				String code = codeIn(message);

				if (!code.isEmpty()) {
					return code;
				}
			}

			return "";

		} finally {
			try {
				if (inbox.isOpen()) {
					inbox.close(false);
				}
			} catch (MessagingException ignored) {
				// Nothing useful to do; the store is closed by the caller.
			}
		}
	}

	/**
	 * Whether the message was sent to this alias.
	 *
	 * Gmail rewrites the visible To header in some deliveries, so the original
	 * alias is looked for across the delivery headers as well rather than trusting
	 * any single one.
	 */
	private boolean addressedTo(Message message, String recipient) throws MessagingException {

		String wanted = recipient.toLowerCase();

		Address[] recipients = message.getAllRecipients();

		if (recipients != null) {
			for (Address address : recipients) {
				if (address.toString().toLowerCase().contains(wanted)) {
					return true;
				}
			}
		}

		for (String header : new String[] { "Delivered-To", "X-Original-To", "To", "X-Forwarded-To" }) {

			String[] values = message.getHeader(header);

			if (values == null) {
				continue;
			}

			for (String value : values) {
				if (value != null && value.toLowerCase().contains(wanted)) {
					return true;
				}
			}
		}

		return false;
	}

	/** The six digit code from the subject or body, preferring a labelled one. */
	private String codeIn(Message message) throws MessagingException, IOException {

		String subject = message.getSubject();
		String body = textOf(message);
		String all = (subject == null ? "" : subject) + "\n" + body;

		Matcher labelled = LABELLED_CODE.matcher(all);

		if (labelled.find()) {
			return labelled.group(1);
		}

		Matcher bare = BARE_CODE.matcher(all);

		return bare.find() ? bare.group(1) : "";
	}

	/** Flattens a message to text, walking multipart bodies and stripping any markup. */
	private String textOf(Part part) throws MessagingException, IOException {

		Object content;

		try {
			content = part.getContent();
		} catch (IOException unreadable) {
			return "";
		}

		if (content instanceof String) {
			return part.isMimeType("text/html") ? stripMarkup((String) content) : (String) content;
		}

		if (content instanceof Multipart) {

			Multipart parts = (Multipart) content;
			StringBuilder text = new StringBuilder();

			for (int i = 0; i < parts.getCount(); i++) {
				text.append(textOf(parts.getBodyPart(i))).append('\n');
			}

			return text.toString();
		}

		return "";
	}

	/** Tags out, entities to spaces - enough to find a code, not to render anything. */
	private String stripMarkup(String html) {

		return html.replaceAll("(?s)<(script|style).*?</\\1>", " ")
				.replaceAll("<[^>]+>", " ")
				.replaceAll("&nbsp;|&#160;", " ");
	}

	private void closeQuietly(Store store) {

		if (store == null) {
			return;
		}

		try {
			store.close();
		} catch (MessagingException ignored) {
			// Closing a mailbox that is already gone is not worth reporting.
		}
	}

	/** Shared with the report mailer so one credential serves both. */
	private String password() {

		return MailCredentials.password();
	}

	private String get(String key, String fallback) {

		String value = config.getProperty(key);
		return (value == null || value.isBlank()) ? fallback : value.trim();
	}
}
