package com.crxpulse.ui.theme;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Claymorphism, in Java2D.
 *
 * The clay look in the mobile app is a two-tone soft shadow: a diffuse
 * DARK halo to the bottom-right and a soft LIGHT highlight to the
 * top-left, with a large corner radius and no hard border.
 *
 * Java2D has no blur primitive, so the softness is faked by stacking a
 * few rounded rectangles at growing offsets with falling alpha. A
 * "pressed" surface (an input) simply swaps the two directions, which the
 * eye reads as an inset — the same trick the Flutter version uses, since
 * BoxShadow can't do a true inner shadow either.
 *
 * Every clay surface in the app paints through this one method, so the
 * look is defined in exactly one place.
 */
public final class ClayPainter {

    private ClayPainter() {
    }

    /** Space each clay component must reserve around itself for its shadow. */
    public static final int SHADOW_PAD = 9;

    private static final int LAYERS = 6;
    private static final Color DARK  = new Color(0x8A, 0x8A, 0x78);
    private static final Color LIGHT = new Color(0xFF, 0xFF, 0xFF);

    public static void paint(Graphics2D g, int x, int y, int w, int h,
                             int radius, Color fill, boolean pressed) {
        if (w <= 0 || h <= 0) {
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int direction = pressed ? -1 : 1;

        // Far layers are faint, near layers stronger — that falloff is
        // what sells the blur.
        for (int i = LAYERS; i >= 1; i--) {
            int strength = LAYERS + 1 - i;

            g.setColor(withAlpha(DARK, strength * 7));
            g.fill(new RoundRectangle2D.Float(
                    x + (float) direction * i, y + (float) direction * i, w, h, radius, radius));

            g.setColor(withAlpha(LIGHT, strength * 13));
            g.fill(new RoundRectangle2D.Float(
                    x - (float) direction * i, y - (float) direction * i, w, h, radius, radius));
        }

        g.setColor(fill);
        g.fill(new RoundRectangle2D.Float(x, y, w, h, radius, radius));
    }

    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(255, Math.max(0, alpha)));
    }
}