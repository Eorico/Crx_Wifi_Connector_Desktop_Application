package com.crxpulse.serial;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns the USB serial connection to the ESP32 — and nothing else. It does
 * not know what the bytes mean (that is {@link DeviceProvisioner}) and it
 * does not know about the UI (that is {@link SerialEvents}).
 */
public class SerialPortService {

    /** Must match {@code Serial.begin(115200)} in the firmware. */
    public static final int BAUD_RATE = 115200;

    private final SerialEvents events;
    private SerialPort port;

    public SerialPortService(SerialEvents events) {
        this.events = events;
    }

    /** Every serial device the OS can see right now. */
    public static List<PortInfo> listPorts() {
        List<PortInfo> found = new ArrayList<>();
        for (SerialPort p : SerialPort.getCommPorts()) {
            found.add(new PortInfo(p.getSystemPortName(), p.getDescriptivePortName()));
        }
        return found;
    }

    public boolean isOpen() {
        return port != null && port.isOpen();
    }

    public String currentPortName() {
        return isOpen() ? port.getSystemPortName() : null;
    }

    public void open(String systemPortName) throws IOException {
        close();

        SerialPort candidate = SerialPort.getCommPort(systemPortName);
        candidate.setComPortParameters(BAUD_RATE, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        candidate.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);

        // Most ESP32 boards wire DTR and RTS to EN/BOOT, so asserting them
        // reboots the board — which would throw away whatever we send
        // next. Clearing both before AND after opening keeps it running.
        candidate.clearDTR();
        candidate.clearRTS();

        if (!candidate.openPort()) {
            throw new IOException("Could not open " + systemPortName
                    + ". Close the Arduino IDE's Serial Monitor if it is still open, then try again.");
        }
        candidate.clearDTR();
        candidate.clearRTS();
        candidate.addDataListener(new LineAssembler());

        this.port = candidate;
        events.onStatus("Connected to " + systemPortName + " at " + BAUD_RATE + " baud.", false);
    }

    public void close() {
        if (port == null) {
            return;
        }
        String name = port.getSystemPortName();
        port.removeDataListener();
        if (port.isOpen()) {
            port.closePort();
        }
        port = null;
        events.onStatus("Disconnected from " + name + ".", false);
    }

    /** Sends one line, newline-terminated, UTF-8. */
    public void sendLine(String line) throws IOException {
        if (!isOpen()) {
            throw new IOException("Not connected to a serial port.");
        }
        OutputStream out = port.getOutputStream();
        out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /**
     * jSerialComm can buffer up to a delimiter for us, so there is no
     * reader thread or partial-line handling to get wrong — each callback
     * is exactly one line.
     */
    private final class LineAssembler implements SerialPortMessageListener {

        @Override
        public int getListeningEvents() {
            return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
        }

        @Override
        public byte[] getMessageDelimiter() {
            return new byte[] { (byte) '\n' };
        }

        @Override
        public boolean delimiterIndicatesEndOfMessage() {
            return true;
        }

        @Override
        public void serialEvent(SerialPortEvent event) {
            String raw = new String(event.getReceivedData(), StandardCharsets.UTF_8);
            events.onLine(raw.replace("\r", "").replace("\n", ""));
        }
    }

    /** A port as the user picks it from the dropdown. */
    public record PortInfo(String systemName, String description) {
        @Override
        public String toString() {
            return systemName + "  —  " + description;
        }
    }
}