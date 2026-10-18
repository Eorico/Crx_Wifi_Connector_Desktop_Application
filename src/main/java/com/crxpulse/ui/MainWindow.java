package com.crxpulse.ui;

import com.crxpulse.serial.DeviceProvisioner;
import com.crxpulse.serial.SerialEvents;
import com.crxpulse.serial.SerialPortService;
import com.crxpulse.ui.theme.ClayPanel;
import com.crxpulse.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The application window — it wires the pieces together and is the only
 * class that bridges the serial layer onto the Swing event thread.
 */
public class MainWindow extends JFrame implements SerialEvents {

    private static final String APP_NAME = "CRX PULSE CONNECTOR";
    private static final String TAGLINE = "ESP32 WiFi provisioning  ·  CRX-Pulse";

    private final ConsolePanel console = new ConsolePanel();
    private final SerialPortService serial = new SerialPortService(this);
    private final ConnectPanel form;

    public MainWindow() {
        super(APP_NAME);
        this.form = new ConnectPanel(serial, new DeviceProvisioner(serial), console);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(940, 640));
        setSize(new Dimension(1080, 870));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        setContentPane(root);

        // Releasing the port matters: leaving it open blocks the Arduino
        // IDE from uploading until the app is killed.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                serial.close();
            }
        });

        console.appendSystemLine(APP_NAME + " ready. Plug the ESP32 in by USB, pick its port, then Connect.", false);
    }

    /** The dark green bar, the same one the phone app wears. */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY_DARK);
        header.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(APP_NAME);
        title.setFont(Theme.bold(20));
        title.setForeground(java.awt.Color.WHITE);

        JLabel tagline = new JLabel(TAGLINE);
        tagline.setFont(Theme.plain(12));
        tagline.setForeground(Theme.PRIMARY_LIGHT);

        titles.add(title);
        titles.add(tagline);
        header.add(titles, BorderLayout.WEST);
        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(10, 0));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(8, 12, 14, 14));

        // The form can be taller than a laptop screen, so it scrolls.
        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        formScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.getVerticalScrollBar().setUnitIncrement(18);
        formScroll.setPreferredSize(new Dimension(455, 10));
        body.add(formScroll, BorderLayout.WEST);

        // The console sits in its own clay panel so it reads as one card.
        // A small inset keeps the clay panel's rounded edge visible around
        // the console, which paints square.
        ClayPanel consoleCard = new ClayPanel(Theme.RADIUS_SM, Theme.CONSOLE_BG, false);
        consoleCard.setLayout(new BorderLayout());
        consoleCard.setContentPadding(7);
        consoleCard.add(console, BorderLayout.CENTER);
        body.add(consoleCard, BorderLayout.CENTER);

        return body;
    }

    // ===================================================================
    // SerialEvents — called from jSerialComm's thread, NOT the EDT
    // ===================================================================

    @Override
    public void onLine(String line) {
        // ConsolePanel.append already re-dispatches onto the EDT.
        console.appendDeviceLine(line);
    }

    @Override
    public void onStatus(String message, boolean isError) {
        SwingUtilities.invokeLater(() -> {
            console.appendSystemLine(message, isError);
            form.setStatus(message, isError);
        });
    }
}