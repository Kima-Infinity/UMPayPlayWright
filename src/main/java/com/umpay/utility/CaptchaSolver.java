package com.umpay.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Reads the registration captcha with ddddocr, an offline ONNX model that lives
 * in the sibling Captcha project.
 *
 * The model runs as a short lived Python process rather than inside the JVM:
 * ddddocr has no Java port, and shelling out keeps 85MB of model weights out of
 * this repository. Nothing leaves the machine, so this works offline and costs
 * nothing per call.
 *
 * Recognition is unreliable by nature. On a sample of eight real captchas kept
 * from earlier runs, ddddocr read some exactly, dropped digits on half of them,
 * and once returned a confident but wrong "7095" for an image that plainly read
 * 1095. A length check catches the dropped digit cases; nothing catches the
 * confident misreads. Callers must therefore submit the answer and, when the
 * backend rejects it, refresh the image and ask again - see the retry loop in
 * RegisterPage. Treat a single answer as a guess, never as the code.
 */
public class CaptchaSolver {

	private static final String CONFIG_PATH = "/Config/config.properties";

	/** Long enough for a cold start: each call reloads the 54MB model from disk. */
	private static final int PROCESS_TIMEOUT_SECONDS = 120;

	private final Properties config = new Properties();

	public CaptchaSolver() {

		String path = System.getProperty("user.dir") + CONFIG_PATH;

		try (FileInputStream in = new FileInputStream(path)) {
			config.load(in);
		} catch (IOException e) {
			System.out.println("Could not read " + path + " for the captcha OCR settings: " + e.getMessage());
		}
	}

	/**
	 * Whether the OCR path is switched on. Off by default, so a machine without
	 * Python installed falls back to typing the code by hand rather than failing.
	 */
	public boolean isEnabled() {

		return Boolean.parseBoolean(get("captcha.ocr.enabled", "false"))
				&& !get("captcha.ocr.python", "").isBlank()
				&& !get("captcha.ocr.script", "").isBlank();
	}

	/**
	 * How many images to work through before giving up. Each attempt is a fresh
	 * captcha, so the odds compound: at roughly even money per image, six attempts
	 * clear a captcha the overwhelming majority of the time.
	 */
	public int getAttempts() {

		try {
			return Integer.parseInt(get("captcha.ocr.attempts", "6").trim());
		} catch (NumberFormatException notANumber) {
			return 6;
		}
	}

	/**
	 * Reads the image and returns the digits, or an empty string when OCR is off,
	 * misconfigured, failed, or produced something that is not a code of the
	 * expected length. An empty return is normal and means "ask a human instead".
	 *
	 * @param imagePath      the captcha JPEG written by RegisterPage
	 * @param expectedLength digits the backend issues; a different count is a misread
	 */
	public String solve(String imagePath, int expectedLength) {

		if (!isEnabled()) {
			return "";
		}

		String python = get("captcha.ocr.python", "");
		String script = get("captcha.ocr.script", "");

		if (!new File(python).isFile()) {
			System.out.println("Captcha OCR is enabled but the Python executable is missing: " + python);
			return "";
		}

		if (!new File(script).isFile()) {
			System.out.println("Captcha OCR is enabled but the ocr.py script is missing: " + script);
			return "";
		}

		if (!new File(imagePath).isFile()) {
			System.out.println("Captcha OCR has no image to read at: " + imagePath);
			return "";
		}

		List<String> command = new ArrayList<>();
		command.add(python);
		command.add(script);
		command.add(imagePath);
		command.add("--quiet");
		// tesseract is not installed, so the fallback would only cost an import attempt.
		command.add("--no-fallback");

		String ranges = get("captcha.ocr.ranges", "0");
		if (!ranges.isBlank()) {
			command.add("--ranges");
			command.add(ranges);
		}

		String reading = run(command);

		if (reading.isEmpty()) {
			return "";
		}

		if (reading.length() != expectedLength) {
			System.out.println("Captcha OCR read \"" + reading + "\", which is not "
					+ expectedLength + " characters - discarding it as a misread");
			return "";
		}

		return reading;
	}

	/** Runs the OCR process and returns its trimmed stdout, or an empty string on any failure. */
	private String run(List<String> command) {

		Process process = null;

		try {
			long startedAt = System.currentTimeMillis();

			process = new ProcessBuilder(command).start();

			String output = read(process.getInputStream());
			String errors = read(process.getErrorStream());

			if (!process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				System.out.println("Captcha OCR did not finish within " + PROCESS_TIMEOUT_SECONDS + " seconds");
				return "";
			}

			long tookMillis = System.currentTimeMillis() - startedAt;

			if (process.exitValue() != 0) {
				System.out.println("Captcha OCR failed with exit code " + process.exitValue()
						+ (errors.isBlank() ? "" : ": " + firstLine(errors)));
				return "";
			}

			String reading = output.trim();
			System.out.println("Captcha OCR read \"" + reading + "\" in " + tookMillis + "ms");

			return reading;

		} catch (IOException cannotStart) {
			System.out.println("Could not start the captcha OCR process: " + cannotStart.getMessage());
			return "";
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			if (process != null) {
				process.destroyForcibly();
			}
			return "";
		}
	}

	private String read(java.io.InputStream stream) throws IOException {

		StringBuilder text = new StringBuilder();

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream, StandardCharsets.UTF_8))) {

			String line;
			while ((line = reader.readLine()) != null) {
				text.append(line).append(System.lineSeparator());
			}
		}

		return text.toString();
	}

	/** ddddocr prints a long remediation banner on failure; the first line carries the reason. */
	private String firstLine(String text) {

		String[] lines = text.trim().split("\\R");
		return lines.length == 0 ? "" : lines[0];
	}

	/**
	 * A captcha setting, from the command line first and the config file second.
	 *
	 * The command line matters for CI. config.properties names an absolute path to the
	 * Python that runs the OCR, which is right for the machine it was written on and wrong
	 * for every build agent. Without an override a job would have to edit a committed file
	 * to run at all, so -Dcaptcha.ocr.python=... wins over the file the same way
	 * -Dheadless=true already does.
	 */
	private String get(String key, String fallback) {

		String override = System.getProperty(key);

		if (override != null && !override.isBlank()) {
			return override.trim();
		}

		String value = config.getProperty(key);
		return (value == null || value.isBlank()) ? fallback : value.trim();
	}
}
