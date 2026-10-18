package com.crxpulse.ui;

/**
 * The narrow view of the console that the form needs.
 *
 * The form panel depends on this instead of on {@link ConsolePanel}
 * itself, so it cannot reach into the console's widgets and the console
 * could be swapped for a log file or a test double without touching the
 * form.
 */
public interface ConsoleSink {

    /** A message from this application. */
    void appendSystemLine(String line, boolean isError);

    /** An echo of a command sent to the device. */
    void appendSentLine(String line);
}