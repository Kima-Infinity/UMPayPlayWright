package com.umpay.utility;

import com.microsoft.playwright.Locator;

/**
 * Types into a field and checks the value both lands and stays.
 *
 * Most of what the Selenium version of this class did is now the driver's job. There,
 * sendKeys fired keystrokes at whatever was under the locator at that instant, so the class
 * had to retry a value that never landed, clear with a select-all because WebElement.clear()
 * did not always raise the event React listens for, and wait for the element to be usable
 * itself. Playwright's fill() waits for the field to be visible, enabled and editable, sets
 * the value and raises the input event the framework needs - so none of that is left.
 *
 * What is left is the half that was never about the driver. The withdraw amount typed
 * correctly and was then reset to the field's own default a moment later, when choosing the
 * currency re-rendered the form. That is the application changing its mind after a
 * successful edit, and no amount of driver cleverness sees it: the only way to know is to
 * look again once the re-render has had its chance. Hence this class still exists, and still
 * fails here naming the field and both values rather than letting the run stumble into a
 * timeout on an unrelated element that says nothing about the cause.
 */
public final class FormInput {

    /** One retry is enough in practice; the race is with the first render, not a slow page. */
    private static final int ATTEMPTS = 3;

    /** Long enough for the re-render that follows a selection to have happened. */
    private static final int SETTLE_MILLIS = 900;

    private FormInput() {
        // Static holder; there is nothing to construct.
    }

    /**
     * Clears the field and types {@code value}, retrying until the field reads it back.
     *
     * @throws IllegalStateException if the field will not hold the value
     */
    public static void type(Locator field, String value, String fieldName) {

        String actual = "";

        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {

            field.fill(value);

            actual = read(field);

            if (value.equals(actual)) {

                // Landing is not the same as sticking. Confirm the value survives whatever
                // the form does next.
                sleep();
                actual = read(field);

                if (value.equals(actual)) {
                    return;
                }

                System.out.println("The " + fieldName + " field took \"" + value
                        + "\" and then reverted to \"" + actual + "\" - something re-rendered the"
                        + " form. Retyping (attempt " + attempt + " of " + ATTEMPTS + ").");
                continue;
            }

            System.out.println("The " + fieldName + " field took \"" + actual + "\" instead of \""
                    + value + "\" on attempt " + attempt + " of " + ATTEMPTS + "; retyping.");

            sleep();
        }

        throw new IllegalStateException("The " + fieldName + " field would not accept \"" + value
                + "\" - it reads \"" + actual + "\" after " + ATTEMPTS + " attempts. The value is"
                + " correct in the test data, so this is the field dropping input rather than a"
                + " data problem.");
    }

    private static String read(Locator field) {

        String value = field.inputValue();

        return value == null ? "" : value.trim();
    }

    private static void sleep() {

        try {
            Thread.sleep(SETTLE_MILLIS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
