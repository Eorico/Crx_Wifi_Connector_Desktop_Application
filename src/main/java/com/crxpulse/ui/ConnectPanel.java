package com.crxpulse.ui;

import com.crxpulse.model.WifiCredentials;
import com.crxpulse.constants.WifiProtocol;
import com.crxpulse.serial.DeviceProvisioner;
import com.crxpulse.serial.SerialPortService;
import com.crxpulse.serial.WifiScanner;
import com.crxpulse.ui.theme.ClayButton;
import com.crxpulse.ui.theme.ClayField;
import com.crxpulse.ui.theme.ClayPanel;
import com.crxpulse.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import java.util.concurrent.ExecutionException;

 
public class ConnectPanel extends JPanel {

    private final SerialPortService serial;
    private final DeviceProvisioner provisioner;
    private final ConsoleSink console;

    private final JComboBox<SerialPortService.PortInfo> portCombo = ClayField.flatCombo();
    private final JTextField ssidField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JCheckBox showPassword = new JCheckBox("Show password");

    private final ClayButton connectButton = ClayButton.secondary("Connect");
    private final ClayButton refreshButton = ClayButton.secondary("Refresh");
    private final ClayButton scanButton = ClayButton.secondary("Scan WiFi");
    private final ClayButton saveButton = ClayButton.primary("Save to ESP32");
    private final ClayButton statusButton = ClayButton.secondary("Status");
    private final ClayButton clearButton = ClayButton.secondary("Erase WiFi");
    private final ClayButton rebootButton = ClayButton.danger("Reboot");

    private final JLabel statusLabel = new JLabel(" ");

    private final char defaultEchoChar = passwordField.getEchoChar();

    public ConnectPanel(SerialPortService serial, DeviceProvisioner provisioner, ConsoleSink console) {
        this.serial = serial;
        this.provisioner = provisioner;
        this.console = console;

        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(2, 8, 6, 8));

        add(buildBoardCard());
        add(Box.createVerticalStrut(4));
        add(buildCredentialsCard());
        add(Box.createVerticalStrut(4));
        add(buildToolsCard());
        add(Box.createVerticalGlue());

        wireActions();
        refreshPorts();
        reflectConnectionState();
    }

    // ===================================================================
    // CARDS
    // ===================================================================

    private JComponent buildBoardCard() {
        ClayPanel card = new ClayPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setContentPadding(12);

        card.add(heading("1. Board"));
        card.add(Box.createVerticalStrut(10));
        card.add(stretch(ClayField.label("USB serial port")));
        card.add(Box.createVerticalStrut(5));
        card.add(stretch(ClayField.wrap(portCombo)));
        card.add(Box.createVerticalStrut(6));
        card.add(stretch(buttonRow(refreshButton, connectButton)));

        return limitHeight(card);
    }

    private JComponent buildCredentialsCard() {
        ClayPanel card = new ClayPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setContentPadding(12);

        card.add(heading("2. WiFi"));
        card.add(Box.createVerticalStrut(10));

        card.add(stretch(ClayField.label("Network name (SSID)")));
        card.add(Box.createVerticalStrut(5));
        card.add(stretch(ClayField.wrap(ssidField)));
        card.add(Box.createVerticalStrut(6));
        card.add(stretch(buttonRow(scanButton)));
        card.add(Box.createVerticalStrut(12));

        card.add(stretch(ClayField.label("Password")));
        card.add(Box.createVerticalStrut(5));
        card.add(stretch(ClayField.wrap(passwordField)));
        card.add(Box.createVerticalStrut(4));

        showPassword.setOpaque(false);
        showPassword.setFont(Theme.plain(12));
        showPassword.setForeground(Theme.TEXT_SECONDARY);
        showPassword.setFocusPainted(false);
        card.add(stretch(showPassword));

        JLabel hint = new JLabel("The ESP32 only joins 2.4 GHz networks.");
        hint.setFont(Theme.plain(11));
        hint.setForeground(Theme.TEXT_MUTED);
        card.add(Box.createVerticalStrut(2));
        card.add(stretch(hint));

        card.add(Box.createVerticalStrut(12));
        card.add(stretch(saveButton));

        statusLabel.setFont(Theme.bold(12));
        statusLabel.setForeground(Theme.TEXT_SECONDARY);
        card.add(Box.createVerticalStrut(8));
        card.add(stretch(statusLabel));

        return limitHeight(card);
    }

    private JComponent buildToolsCard() {
        ClayPanel card = new ClayPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setContentPadding(12);

        card.add(heading("3. Checks"));
        card.add(Box.createVerticalStrut(4));

        JLabel caption = new JLabel("Status reads back what the board has saved.");
        caption.setFont(Theme.plain(11));
        caption.setForeground(Theme.TEXT_MUTED);
        card.add(stretch(caption));

        card.add(Box.createVerticalStrut(8));
        card.add(stretch(buttonRow(statusButton, clearButton, rebootButton)));

        return limitHeight(card);
    }

    // ===================================================================
    // ACTIONS
    // ===================================================================

    private void wireActions() {
        refreshButton.addActionListener(e -> refreshPorts());
        connectButton.addActionListener(e -> toggleConnection());
        scanButton.addActionListener(e -> scanForNetworks());
        saveButton.addActionListener(e -> sendCredentials());
        showPassword.addActionListener(e ->
                passwordField.setEchoChar(showPassword.isSelected() ? (char) 0 : defaultEchoChar));

        statusButton.addActionListener(e -> sendSimpleCommand(
                WifiProtocol.CMD_STATUS, provisioner::requestStatus));
        clearButton.addActionListener(e -> {
            if (confirm("Erase the WiFi name and password saved on the board?")) {
                sendSimpleCommand(WifiProtocol.CMD_CLEAR, provisioner::clearStoredCredentials);
            }
        });
        rebootButton.addActionListener(e -> sendSimpleCommand(
                WifiProtocol.CMD_REBOOT, provisioner::reboot));
    }

    private void refreshPorts() {
        SerialPortService.PortInfo previous = (SerialPortService.PortInfo) portCombo.getSelectedItem();
        portCombo.removeAllItems();

        List<SerialPortService.PortInfo> ports = SerialPortService.listPorts();
        for (SerialPortService.PortInfo port : ports) {
            portCombo.addItem(port);
        }

        if (ports.isEmpty()) {
            setStatus("No serial ports found. Plug the ESP32 in by USB.", true);
            return;
        }
        if (previous != null) {
            // Keep the user's choice across a refresh when it is still there.
            for (int i = 0; i < portCombo.getItemCount(); i++) {
                if (portCombo.getItemAt(i).systemName().equals(previous.systemName())) {
                    portCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        setStatus(ports.size() + " port(s) found.", false);
    }

    private void toggleConnection() {
        if (serial.isOpen()) {
            serial.close();
            reflectConnectionState();
            return;
        }

        SerialPortService.PortInfo selected = (SerialPortService.PortInfo) portCombo.getSelectedItem();
        if (selected == null) {
            setStatus("Choose a port first.", true);
            return;
        }

        connectButton.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                serial.open(selected.systemName());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("Connected to " + selected.systemName() + ".", false);
                } catch (InterruptedException | ExecutionException ex) {
                    setStatus(rootMessage(ex), true);
                    console.appendSystemLine(rootMessage(ex), true);
                }
                reflectConnectionState();
            }
        }.execute();
    }

    private void scanForNetworks() {
        scanButton.setEnabled(false);
        setStatus("Scanning for networks…", false);

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() {
                return WifiScanner.scan();
            }

            @Override
            protected void done() {
                scanButton.setEnabled(true);
                List<String> networks;
                try {
                    networks = get();
                } catch (InterruptedException | ExecutionException ex) {
                    networks = List.of();
                }
                if (networks.isEmpty()) {
                    setStatus("No networks found — type the name instead.", true);
                    return;
                }
                Object choice = JOptionPane.showInputDialog(ConnectPanel.this,
                        "Pick the network the ESP32 should join:", "Networks in range",
                        JOptionPane.PLAIN_MESSAGE, null, networks.toArray(), networks.get(0));
                if (choice != null) {
                    ssidField.setText(choice.toString());
                    setStatus("SSID set from the scan.", false);
                }
            }
        }.execute();
    }

    private void sendCredentials() {
        final WifiCredentials credentials;
        try {
            credentials = WifiCredentials.of(ssidField.getText(), new String(passwordField.getPassword()));
        } catch (IllegalArgumentException invalid) {
            setStatus(invalid.getMessage(), true);
            return;
        }
        if (!serial.isOpen()) {
            setStatus("Connect to the board's port first.", true);
            return;
        }
        if (credentials.isOpenNetwork()
                && !confirm("No password entered. Send this as an open network?")) {
            return;
        }

        saveButton.setEnabled(false);
        setStatus("Sending…", false);
        console.appendSentLine(WifiProtocol.CMD_SSID + credentials.ssid());
        console.appendSentLine(WifiProtocol.CMD_PASS + "**********");
        console.appendSentLine(WifiProtocol.CMD_SAVE);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                provisioner.provision(credentials);
                return null;
            }

            @Override
            protected void done() {
                saveButton.setEnabled(true);
                try {
                    get();
                    setStatus("Sent. Watch for <OK:SAVED in the console.", false);
                } catch (InterruptedException | ExecutionException ex) {
                    setStatus(rootMessage(ex), true);
                    console.appendSystemLine(rootMessage(ex), true);
                }
            }
        }.execute();
    }

    /** Shared by Status / Clear / Reboot — they differ only in the call. */
    private void sendSimpleCommand(String echo, ThrowingCommand command) {
        if (!serial.isOpen()) {
            setStatus("Connect to the board's port first.", true);
            return;
        }
        console.appendSentLine(echo);
        try {
            command.run();
        } catch (Exception ex) {
            setStatus(rootMessage(ex), true);
            console.appendSystemLine(rootMessage(ex), true);
        }
    }

    /** Lets the three one-line commands be passed as method references. */
    @FunctionalInterface
    private interface ThrowingCommand {
        void run() throws Exception;
    }

    // ===================================================================
    // STATE + SMALL HELPERS
    // ===================================================================

    private void reflectConnectionState() {
        boolean open = serial.isOpen();
        connectButton.setText(open ? "Disconnect" : "Connect");
        connectButton.setEnabled(true);
        portCombo.setEnabled(!open);
        refreshButton.setEnabled(!open);
        saveButton.setEnabled(open);
        statusButton.setEnabled(open);
        clearButton.setEnabled(open);
        rebootButton.setEnabled(open);
    }

    public void setStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setForeground(isError ? Theme.DANGER : Theme.TEXT_SECONDARY);
    }

    private boolean confirm(String question) {
        return JOptionPane.showConfirmDialog(this, question, "CRX Pulse Connector",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
    }

    /** Unwraps SwingWorker's ExecutionException so the user sees the real cause. */
    private static String rootMessage(Throwable error) {
        Throwable cause = error instanceof ExecutionException && error.getCause() != null
                ? error.getCause() : error;
        String message = cause.getMessage();
        return (message == null || message.isBlank()) ? cause.getClass().getSimpleName() : message;
    }

    private static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.bold(15));
        label.setForeground(Theme.TEXT_PRIMARY);
        return label;
    }

    private static JPanel buttonRow(JComponent... buttons) {
        JPanel row = new JPanel(new GridLayout(1, buttons.length, 6, 0));
        row.setOpaque(false);
        for (JComponent button : buttons) {
            row.add(button);
        }
        return row;
    }

    /** BoxLayout respects alignment, so every child declares left-aligned. */
    private static <T extends JComponent> T stretch(T component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        return component;
    }

    /** Stops a card from being stretched to fill leftover vertical space. */
    private static JComponent limitHeight(JComponent card) {
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
    }
}