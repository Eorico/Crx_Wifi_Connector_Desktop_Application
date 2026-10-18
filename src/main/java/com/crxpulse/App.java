package com.crxpulse;

import com.crxpulse.ui.MainWindow;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * CRX PULSE CONNECTOR — entry point.
 *
 * Sends the WiFi name and password to the ESP32 over USB so the firmware
 * no longer has to carry them hard-coded.
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        // The native look-and-feel only shows through on the few stock
        // widgets (scroll bars, dialogs, the combo popup); every surface
        // the user actually looks at is clay-painted by this app.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // The cross-platform default is fine.
        }

        // Swing is single-threaded: every window must be built on the
        // event dispatch thread.
        SwingUtilities.invokeLater(() -> {
            try {
                new MainWindow().setVisible(true);
            } catch (Throwable fatal) {
                JOptionPane.showMessageDialog(null,
                        "CRX Pulse Connector could not start:\n\n" + fatal,
                        "Startup failed", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}