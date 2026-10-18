package com.crxpulse.serial;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Lists the WiFi networks this PC can see, so the SSID can be picked from
 * a list instead of typed — a mistyped SSID is the single most common
 * reason a board silently fails to join.
 *
 * <p>Best-effort by design: it shells out to the OS, and if that is not
 * available (or the wireless adapter is off) it returns an empty list and
 * the user simply types the name. It never throws.
 */
public final class WifiScanner {

    private WifiScanner() {
    }

    private static final long TIMEOUT_SECONDS = 6;

    public static List<String> scan() {
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("win")) {
                return parseWindows(run("netsh", "wlan", "show", "networks"));
            }
            if (os.contains("mac")) {
                return parseOneNamePerLine(run("/usr/sbin/airport", "-s"));
            }
            return parseOneNamePerLine(run("nmcli", "-t", "-f", "SSID", "device", "wifi"));
        } catch (Exception ignored) {
            // A scan is a convenience, never a requirement.
            return List.of();
        }
    }

    private static List<String> run(String... command) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
        }
        return lines;
    }

    /** netsh prints lines like {@code SSID 3 : MyNetwork}. */
    private static List<String> parseWindows(List<String> lines) {
        Set<String> names = new LinkedHashSet<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("SSID ")) {
                continue;
            }
            int colon = trimmed.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String name = trimmed.substring(colon + 1).trim();
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return new ArrayList<>(names);
    }

    private static List<String> parseOneNamePerLine(List<String> lines) {
        Set<String> names = new LinkedHashSet<>();
        for (String line : lines) {
            String name = line.trim();
            if (!name.isEmpty() && !name.startsWith("SSID")) {
                names.add(name);
            }
        }
        return new ArrayList<>(names);
    }
}