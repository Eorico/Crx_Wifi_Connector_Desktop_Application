package com.crxpulse.ui;

import com.crxpulse.serial.DeviceProvisioner;
import com.crxpulse.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * The serial console — deliberately the same dark, monospaced, colour-coded
 * look as the Serial Output screen in the mobile app, so the two feel like
 * one product.
 */
public class ConsolePanel extends JPanel implements ConsoleSink {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Above this, the oldest third is dropped so a long session can't eat memory. */
    private static final int MAX_CHARS = 180_000;

    private final JTextPane view = new JTextPane();
    private final StyledDocument document;
    private final JCheckBox autoScroll = new JCheckBox("Auto-scroll", true);

    public ConsolePanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.CONSOLE_BG);

        view.setEditable(false);
        view.setBackground(Theme.CONSOLE_BG);
        view.setFont(Theme.mono(12));
        view.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        document = view.getStyledDocument();

        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(Theme.CONSOLE_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        add(buildToolbar(), BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.CONSOLE_PANEL);
        bar.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 8));

        JLabel title = new JLabel("SERIAL OUTPUT");
        title.setFont(Theme.bold(11));
        title.setForeground(Theme.CONSOLE_DIM);
        bar.add(title, BorderLayout.WEST);

        autoScroll.setBackground(Theme.CONSOLE_PANEL);
        autoScroll.setForeground(Theme.CONSOLE_DIM);
        autoScroll.setFont(Theme.plain(11));
        autoScroll.setFocusPainted(false);

        // Flat, not clay: a clay button on the dark toolbar reads as an
        // empty outlined box, because its light highlight is the only
        // part that shows against near-black.
        JButton copy = toolbarButton("Copy", e -> copyAll());
        JButton clear = toolbarButton("Clear", e -> clear());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        right.setBackground(Theme.CONSOLE_PANEL);
        right.add(autoScroll);
        right.add(Box.createHorizontalStrut(4));
        right.add(copy);
        right.add(clear);
        bar.add(right, BorderLayout.EAST);

        bar.setPreferredSize(new Dimension(10, 54));
        return bar;
    }

    /** A small borderless button for the console's own toolbar. */
    private JButton toolbarButton(String text, java.awt.event.ActionListener action) {
        JButton button = new JButton(text);
        button.setFont(Theme.bold(11));
        button.setForeground(Theme.CONSOLE_DIM);
        button.setBackground(Theme.CONSOLE_PANEL);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        button.setFocusPainted(false);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.addActionListener(action);
        return button;
    }

    /** A line that came from the device. Safe to call from any thread. */
    public void appendDeviceLine(String line) {
        append(line, colourFor(line));
    }

    /** A line from this application, not the device. */
    @Override
    public void appendSystemLine(String line, boolean isError) {
        append("* " + line, isError ? Theme.CONSOLE_RED : Theme.CONSOLE_BLUE);
    }

    /** Echo of something we sent, so the exchange reads as a conversation. */
    @Override
    public void appendSentLine(String line) {
        append("> " + line, Theme.CONSOLE_AMBER);
    }

    private void append(String text, Color colour) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> append(text, colour));
            return;
        }
        try {
            trimIfHuge();
            document.insertString(document.getLength(), LocalTime.now().format(CLOCK) + "  ", styleOf(Theme.CONSOLE_DIM));
            document.insertString(document.getLength(), text + "\n", styleOf(colour));
            if (autoScroll.isSelected()) {
                view.setCaretPosition(document.getLength());
            }
        } catch (BadLocationException ignored) {
            // The document only ever grows at the end; nothing to recover from.
        }
    }

    private void trimIfHuge() throws BadLocationException {
        if (document.getLength() > MAX_CHARS) {
            document.remove(0, MAX_CHARS / 3);
        }
    }

    private AttributeSet styleOf(Color colour) {
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setForeground(style, colour);
        StyleConstants.setFontFamily(style, view.getFont().getFamily());
        StyleConstants.setFontSize(style, 12);
        return style;
    }

    /**
     * Same classification as the mobile console, so a line is the same
     * colour on both screens.
     */
    private static Color colourFor(String line) {
        if (line.startsWith(DeviceProvisioner.REPLY_ERROR)
                || line.contains("Not Connected") || line.contains("failed") || line.contains("disconnected")) {
            return Theme.CONSOLE_RED;
        }
        if (line.startsWith(DeviceProvisioner.REPLY_OK) || line.startsWith(DeviceProvisioner.REPLY_PREFIX)) {
            return Theme.CONSOLE_AMBER;
        }
        if (line.startsWith("[Cloud]") || line.contains("WiFi: connected") || line.startsWith("IP")) {
            return Theme.CONSOLE_BLUE;
        }
        if (line.startsWith("=====")) {
            return Theme.CONSOLE_DIM;
        }
        return Theme.CONSOLE_GREEN;
    }

    public void clear() {
        SwingUtilities.invokeLater(() -> view.setText(""));
    }

    private void copyAll() {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(view.getText()), null);
        appendSystemLine("Console copied to the clipboard.", false);
    }
}