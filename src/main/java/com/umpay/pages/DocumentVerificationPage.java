package com.umpay.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.umpay.utility.Wait;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What this account's documents say about it, opened from the profile drawer.
 *
 * A dialog rather than a page: the address does not change, and everything is mounted into
 * {@code #root.dialog}, which sits empty until the drawer opens it. That is what tells the two
 * states apart, and it is why nothing here is found by a URL.
 *
 * The dialog is a reading of what was submitted and accepted, in label and value pairs - the
 * person (name, date of birth), the document (type, number, expiry), the contact details it was
 * verified with, what the account holder does and where they live - and under them the three
 * pictures the verification was granted on: the front of the document, the back, and a selfie
 * holding it.
 *
 * Nothing here can be changed, which is the whole of its safety: there is no form, no upload and
 * nothing to submit. The only thing that can be pressed is Close.
 */
public class DocumentVerificationPage {

	/** Where the application mounts a dialog. It exists on every page, empty until one opens. */
	private static final String DIALOG = "#root\\.dialog";

	/** Where an account that has not been verified is sent to submit a document. */
	private static final String FORM = "/kyc";

	/** What this dialog heads itself with. */
	private static final String HEADING = "Document Verification";

	/** The captions under the pictures, which are captions rather than labels with values. */
	private static final String[] PICTURES =
			{"Front Page", "Back Page", "Selfie with document"};

	private final Page page;

	public DocumentVerificationPage(Page ldriver) {

		this.page = ldriver;
	}

	/** True once the dialog is open and is the one about the documents. */
	public boolean isShowing() {

		try {
			Locator dialog = page.locator(DIALOG);

			if (dialog.count() == 0) {
				return false;
			}

			return dialog.first().innerText().contains(HEADING);
		} catch (Exception notThere) {
			return false;
		}
	}

	/** True once the run is past the login page and inside the application. */
	public boolean waitUntilSignedIn(int timeoutSeconds) {

		return Wait.until(() -> !page.url().contains("/login")
				&& page.locator("nav button").count() > 0, timeoutSeconds);
	}

	/** What the drawer said beside Document Verification when it was last opened. */
	private String verdict = "";

	/**
	 * Opens the dialog from the profile drawer, which is the only way to it.
	 *
	 * The verdict beside the item is read on the way past, while the drawer is still the thing on
	 * the screen. Once the dialog is up it covers the drawer, and going back for the verdict then
	 * means clicking through an overlay that swallows it.
	 */
	public void open() {

		if (isShowing()) {
			return;
		}

		waitUntilSignedIn(30);

		ProfileDrawerPage drawer = new ProfileDrawerPage(page);

		drawer.open();

		try {
			verdict = drawer.documentVerification();
		} catch (Exception itSaidNothing) {
			verdict = "";
		}

		drawer.open(HEADING);

		if (!Wait.until(() -> isShowing() || isShowingTheForm(), 15)) {
			throw new IllegalStateException("The profile drawer led neither to the verified"
					+ " account's details nor to the form that submits a document. The page is at "
					+ page.url());
		}
	}

	/**
	 * True once the form that submits a document is open.
	 *
	 * An account that has been verified is shown what it was verified on, in a dialog. An account
	 * that has not is sent to /kyc instead, to submit something - so which of the two the drawer
	 * leads to is itself the answer to whether the account is verified.
	 */
	public boolean isShowingTheForm() {

		try {
			return page.url().contains(FORM)
					&& page.locator("input[name*='documentId']").count() > 0;
		} catch (Exception notThere) {
			return false;
		}
	}

	/**
	 * What the form asks for, by the names it gives its boxes, with the hidden ones left out.
	 *
	 * The names the application uses are long - documentContent[personalInfo][documentId] - so
	 * only the part in the last brackets is kept, which is the part that says what is wanted.
	 */
	public List<String> askedForOnTheForm() {

		List<String> asked = new ArrayList<>();

		for (Locator field : Wait.all(page.locator("input[name], select[name]"))) {

			try {
				if ("hidden".equals(field.getAttribute("type"))) {
					continue;
				}

				String name = String.valueOf(field.getAttribute("name"));

				int lastOpen = name.lastIndexOf('[');

				if (lastOpen >= 0 && name.endsWith("]")) {
					name = name.substring(lastOpen + 1, name.length() - 1);
				}

				if (!name.isEmpty() && !"null".equals(name)) {
					asked.add(name);
				}
			} catch (Exception reRendered) {
				// A field that redrew from under us is not one to report.
			}
		}

		return asked;
	}

	/** How many pictures the form will take, counted by the boxes that accept a file. */
	public int picturesAskedFor() {

		try {
			return page.locator("input[type='file']").count();
		} catch (Exception notThere) {
			return 0;
		}
	}

	/** What kinds of file the form will take, as it says itself. */
	public String fileKindsAccepted() {

		try {
			Locator upload = page.locator("input[type='file']");

			if (upload.count() == 0) {
				return "";
			}

			return String.valueOf(upload.first().getAttribute("accept"));
		} catch (Exception notThere) {
			return "";
		}
	}

	/** True while the form offers to submit what has been filled in. */
	public boolean offersToSubmit() {

		try {
			return page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
					new Page.GetByRoleOptions().setName("Submit").setExact(true)).count() > 0;
		} catch (Exception notThere) {
			return false;
		}
	}

	/**
	 * What the drawer said about where the account stands - "Verified" and the like.
	 *
	 * Read from the drawer rather than from the dialog, because the drawer is where somebody sees
	 * it without opening anything - and read as the dialog was opened, for the reason given there.
	 */
	public String verdictInTheDrawer() {

		return verdict;
	}

	/** Everything the dialog says, line by line, with the empty lines dropped. */
	private List<String> lines() {

		List<String> said = new ArrayList<>();

		try {
			for (String line : page.locator(DIALOG).first().innerText().split("\n")) {

				if (!line.trim().isEmpty()) {
					said.add(line.trim());
				}
			}
		} catch (Exception unreadable) {
			// Nothing to say about a dialog that is not there.
		}

		return said;
	}

	/**
	 * What the dialog shows, as the labels it uses and the values under them.
	 *
	 * The dialog writes a label and then its value, one after another, and finishes with the
	 * three picture captions, which have no value under them. So the captions are recognised and
	 * skipped rather than being read as labels whose value is the next caption.
	 */
	public Map<String, String> detailsShown() {

		Map<String, String> shown = new LinkedHashMap<>();

		List<String> said = lines();

		for (int at = 0; at < said.size(); at++) {

			String line = said.get(at);

			if (line.equals(HEADING) || line.equalsIgnoreCase("Close modal") || isACaption(line)) {
				continue;
			}

			// A label with nothing after it is a label with no value, and is worth saying so.
			String value = at + 1 < said.size() ? said.get(at + 1) : "";

			if (isACaption(value)) {
				value = "";
			}

			shown.put(line, value);

			at++;
		}

		return shown;
	}

	private boolean isACaption(String line) {

		for (String caption : PICTURES) {

			if (caption.equalsIgnoreCase(line)) {
				return true;
			}
		}

		return false;
	}

	/** The labels the dialog uses, in the order it uses them. */
	public List<String> labelsShown() {

		return new ArrayList<>(detailsShown().keySet());
	}

	/** What the dialog shows under {@code label}, or "" if it shows nothing under it. */
	public String shownUnder(String label) {

		for (Map.Entry<String, String> detail : detailsShown().entrySet()) {

			if (detail.getKey().equalsIgnoreCase(label)) {
				return detail.getValue();
			}
		}

		return "";
	}

	/** The captions the dialog puts under its pictures. */
	public List<String> captionsShown() {

		List<String> captions = new ArrayList<>();

		for (String line : lines()) {

			if (isACaption(line)) {
				captions.add(line);
			}
		}

		return captions;
	}

	/**
	 * The pictures the verification was granted on, named by where they are kept.
	 *
	 * A caption with no picture under it is a document nobody can check, so the two are counted
	 * separately and compared by the step.
	 */
	public List<String> picturesShown() {

		List<String> pictures = new ArrayList<>();

		try {
			for (Locator picture : Wait.all(page.locator(DIALOG + " img"))) {

				String source = String.valueOf(picture.getAttribute("src"));

				if (!source.isEmpty() && !"null".equals(source)) {
					pictures.add(source);
				}
			}
		} catch (Exception unreadable) {
			// As above.
		}

		return pictures;
	}

	/** Closes the dialog. */
	public void close() {

		Locator close = page.locator(DIALOG + " button");

		if (close.count() == 0) {
			throw new IllegalStateException("The dialog offers nothing to close it with");
		}

		try {
			close.first().click();
		} catch (Exception intercepted) {
			close.first().dispatchEvent("click");
		}

		Wait.sleep(2500);
	}

	/** Where the run is - the page the dialog was opened over. */
	public String getCurrentUrl() {

		return page.url();
	}

	/** What the dialog says, on one line, for a message that has to name what was seen. */
	public String text() {

		try {
			return page.locator(DIALOG).first().innerText().replaceAll("\\s+", " ").trim();
		} catch (Exception unreadable) {
			return "";
		}
	}

	/** What the platform said, if it answered with one of its own dialogs. */
	public String refusalShowing() {

		return com.umpay.utility.PlatformRefusal.showing(page);
	}
}
