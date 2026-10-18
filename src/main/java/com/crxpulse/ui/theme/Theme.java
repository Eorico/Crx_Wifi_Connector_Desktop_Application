package com.crxpulse.ui.theme;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The single source of truth for colour and type.
 *
 * These are the exact hex values from the Flutter app's {@code AppColors},
 * so the desktop tool and the phone app read as one product. Change a hue
 * here and every panel, button, and label follows — nothing else in the
 * codebase hard-codes a colour.
 */
public final class Theme {

    private Theme() {
        // static holder — never instantiated
    }

    // ── palette (mirrors lib/core/theme.dart) ──
    public static final Color PRIMARY        = new Color(0x3D5A3D);
    public static final Color PRIMARY_DARK   = new Color(0x2C4530);
    public static final Color PRIMARY_LIGHT  = new Color(0x7AA87A);
    public static final Color ACCENT         = new Color(0x8B6F47);
    public static final Color BACKGROUND     = new Color(0xF5F5DC);
    public static final Color SURFACE        = new Color(0xFFFFFF);
    public static final Color FIELD_FILL     = new Color(0xFAFAF2);
    public static final Color BORDER         = new Color(0xE5E7EB);
    public static final Color TEXT_PRIMARY   = new Color(0x0F172A);
    public static final Color TEXT_SECONDARY = new Color(0x6B7280);
    public static final Color TEXT_MUTED     = new Color(0x9CA3AF);
    public static final Color SUCCESS        = new Color(0x22C55E);
    public static final Color WARNING        = new Color(0xF59E0B);
    public static final Color DANGER         = new Color(0xEF4444);

    // ── console (mirrors the app's Serial Output screen) ──
    public static final Color CONSOLE_BG     = new Color(0x0D1117);
    public static final Color CONSOLE_PANEL  = new Color(0x161B22);
    public static final Color CONSOLE_GREEN  = new Color(0x3FB950);
    public static final Color CONSOLE_DIM    = new Color(0x6E7681);
    public static final Color CONSOLE_BLUE   = new Color(0x58A6FF);
    public static final Color CONSOLE_AMBER  = new Color(0xD29922);
    public static final Color CONSOLE_RED    = new Color(0xF85149);

    // ── shape ──
    public static final int RADIUS    = 22;
    public static final int RADIUS_SM = 14;

    private static final String UI_FAMILY   = firstAvailable("Segoe UI", "Inter", "Roboto", "DejaVu Sans", "SansSerif");
    private static final String MONO_FAMILY = firstAvailable("Consolas", "JetBrains Mono", "Cascadia Mono", "DejaVu Sans Mono", "Monospaced");

    public static Font bold(int size)  { return new Font(UI_FAMILY, Font.BOLD, size); }
    public static Font plain(int size) { return new Font(UI_FAMILY, Font.PLAIN, size); }
    public static Font mono(int size)  { return new Font(MONO_FAMILY, Font.PLAIN, size); }

    /** Blends {@code c} toward white — used for hover states. */
    public static Color lighten(Color c, double amount) {
        return new Color(
                (int) Math.min(255, c.getRed()   + (255 - c.getRed())   * amount),
                (int) Math.min(255, c.getGreen() + (255 - c.getGreen()) * amount),
                (int) Math.min(255, c.getBlue()  + (255 - c.getBlue())  * amount));
    }

    /**
     * Picks the first font family actually installed, so the app looks
     * right on Windows without shipping a font file, and still renders
     * sensibly elsewhere.
     */
    private static String firstAvailable(String... candidates) {
        Set<String> installed = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String candidate : candidates) {
            if (installed.contains(candidate)) {
                return candidate;
            }
        }
        return candidates[candidates.length - 1];
    }
}