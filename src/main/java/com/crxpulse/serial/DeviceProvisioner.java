package com.crxpulse.serial;

import com.crxpulse.model.WifiCredentials;
import com.crxpulse.constants.WifiProtocol;

import java.io.IOException;

/**
 * The provisioning protocol — the one class that knows the wire format.
 * If the firmware's command names ever change, this file is the only one
 * that changes with it.
 *
 * <pre>
 *   PC    -> ESP32    &gt;SSID:&lt;ssid&gt;
 *   PC    -> ESP32    &gt;PASS:&lt;password&gt;
 *   PC    -> ESP32    &gt;SAVE
 *   ESP32 -> PC       &lt;ACK:SSID     / &lt;ACK:PASS
 *   ESP32 -> PC       &lt;OK:SAVED     / &lt;ERR:&lt;reason&gt;
 * </pre>
 *
 * Everything after the colon is the value, all the way to the newline, so
 * an SSID or password may contain spaces, colons, or any other character
 * except a line break. That is why the values are not delimiter-packed
 * into a single line.
 */
public class DeviceProvisioner {
    /**
     * The ESP32 parses one line per loop pass, so the lines are spaced
     * out slightly rather than fired back to back.
     */
    private static final long INTER_LINE_GAP_MS = 150;

    private final SerialPortService serial;

    public DeviceProvisioner(SerialPortService serial) {
        this.serial = serial;
    }

    /**
     * Sends the credentials and asks the device to persist them.
     *
     * <p>Blocking — call it off the Swing event thread.
     */
    public void provision(WifiCredentials credentials) throws IOException, InterruptedException {
        serial.sendLine(WifiProtocol.CMD_SSID + credentials.ssid());
        Thread.sleep(INTER_LINE_GAP_MS);
        serial.sendLine(WifiProtocol.CMD_PASS + credentials.password());
        Thread.sleep(INTER_LINE_GAP_MS);
        serial.sendLine(WifiProtocol.CMD_SAVE);
    }

    public void requestStatus() throws IOException {
        serial.sendLine(WifiProtocol.CMD_STATUS);
    }

    public void clearStoredCredentials() throws IOException {
        serial.sendLine(WifiProtocol.CMD_CLEAR);
    }

    public void reboot() throws IOException {
        serial.sendLine(WifiProtocol.CMD_REBOOT);
    }
}