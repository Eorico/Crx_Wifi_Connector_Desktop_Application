package com.crxpulse.ui.theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * A clay button. Three ready-made flavours cover the whole app, which
 * keeps button styling out of the screens:
 *
 *   primary   — filled dark green, for the one main action
 *   secondary — filled white, for supporting actions
 *   danger    — filled red, for destructive actions
 */
public class ClayButton extends JButton {

    private final Color baseFill;
    private final Color baseText;

    public static ClayButton primary(String text) {
        return new ClayButton(text, Theme.PRIMARY, Color.WHITE);
    }

    public static ClayButton secondary(String text) {
        return new ClayButton(text, Theme.SURFACE, Theme.PRIMARY_DARK);
    }

    public static ClayButton danger(String text) {
        return new ClayButton(text, Theme.DANGER, Color.WHITE);
    }

    public ClayButton(String text, Color fill, Color textColor) {
        super(text);
        this.baseFill = fill;
        this.baseText = textColor;

        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFont(Theme.bold(13));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        int p = ClayPainter.SHADOW_PAD;
        setBorder(BorderFactory.createEmptyBorder(p + 8, p + 12, p + 8, p + 12));
    }

    /** Asked for by the label renderer — avoids repainting to recolour text. */
    @Override
    public Color getForeground() {
        if (baseText == null) {
            return super.getForeground();   // still inside the superclass constructor
        }
        return isEnabled() ? baseText : Theme.TEXT_MUTED;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int p = ClayPainter.SHADOW_PAD;
            ClayPainter.paint(g2, p, p, getWidth() - 2 * p, getHeight() - 2 * p,
                    Theme.RADIUS_SM, currentFill(), getModel().isPressed());
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }

    private Color currentFill() {
        if (!isEnabled()) {
            return Theme.BORDER;
        }
        if (getModel().isPressed()) {
            return baseFill.darker();
        }
        if (getModel().isRollover()) {
            return Theme.lighten(baseFill, 0.12);
        }
        return baseFill;
    }
}