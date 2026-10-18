package com.crxpulse.ui.theme;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;

/**
 * A raised (or pressed) clay surface that lays out children normally.
 *
 * It paints its own rounded shape plus shadow, so it must be
 * non-opaque and must keep {@link ClayPainter#SHADOW_PAD} of empty space
 * on every side. Content padding is added on top of that reserved space
 * via {@link #setContentPadding}, so callers never have to know the
 * shadow exists.
 */
public class ClayPanel extends JPanel {

    private final int radius;
    private final Color fill;
    private final boolean pressed;

    public ClayPanel() {
        this(Theme.RADIUS, Theme.SURFACE, false);
    }

    public ClayPanel(int radius, Color fill, boolean pressed) {
        this.radius = radius;
        this.fill = fill;
        this.pressed = pressed;
        setOpaque(false);
        setContentPadding(0, 0, 0, 0);
    }

    public ClayPanel(LayoutManager layout) {
        this();
        setLayout(layout);
    }

    /** Padding INSIDE the clay shape, in addition to the shadow margin. */
    public final void setContentPadding(int top, int left, int bottom, int right) {
        int p = ClayPainter.SHADOW_PAD;
        setBorder(BorderFactory.createEmptyBorder(p + top, p + left, p + bottom, p + right));
    }

    public final void setContentPadding(int all) {
        setContentPadding(all, all, all, all);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int p = ClayPainter.SHADOW_PAD;
            ClayPainter.paint(g2, p, p, getWidth() - 2 * p, getHeight() - 2 * p, radius, fill, pressed);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}