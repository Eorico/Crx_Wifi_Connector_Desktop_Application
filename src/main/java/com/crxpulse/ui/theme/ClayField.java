package com.crxpulse.ui.theme;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Input decoration. One place turns any Swing input into a pressed clay
 * field, so no screen ever sets a border, a font, or a caret colour
 * itself.
 */
public final class ClayField {

    private ClayField() {
    }

    /**
     * Wraps an input in a pressed (inset) clay shell and strips the
     * input's own chrome so only the clay shape shows.
     */
    public static ClayPanel wrap(JComponent input) {
        ClayPanel shell = new ClayPanel(Theme.RADIUS_SM, Theme.FIELD_FILL, true);
        shell.setLayout(new BorderLayout());
        shell.setContentPadding(8, 12, 8, 12);

        input.setOpaque(false);
        input.setBorder(BorderFactory.createEmptyBorder());
        input.setFont(Theme.plain(14));

        if (input instanceof JTextComponent text) {
            text.setForeground(Theme.TEXT_PRIMARY);
            text.setCaretColor(Theme.PRIMARY);
        }

        shell.add(input, BorderLayout.CENTER);
        return shell;
    }

    /**
     * A combo box flattened to match the clay fields. The stock Windows
     * look-and-feel paints a raised 3D box with its own border, which
     * would fight the clay shape, so the UI delegate is replaced with a
     * borderless one and a plain chevron.
     */
    public static <T> JComboBox<T> flatCombo() {
        JComboBox<T> combo = new JComboBox<>();
        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton arrow = new JButton("▾");
                arrow.setBorder(BorderFactory.createEmptyBorder());
                arrow.setContentAreaFilled(false);
                arrow.setFocusPainted(false);
                arrow.setForeground(Theme.TEXT_SECONDARY);
                arrow.setFont(Theme.bold(12));
                arrow.setPreferredSize(new Dimension(20, 20));
                return arrow;
            }
        });
        combo.setOpaque(false);
        combo.setBorder(BorderFactory.createEmptyBorder());
        combo.setFont(Theme.plain(13));
        combo.setForeground(Theme.TEXT_PRIMARY);
        combo.setBackground(Theme.FIELD_FILL);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                label.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
                label.setFont(Theme.plain(13));

                // index < 0 means this is the closed combo's own display
                // area, not a row in the popup. Painting the "selected"
                // highlight there would put a coloured bar inside the
                // clay field, so it stays transparent.
                if (index < 0) {
                    label.setOpaque(false);
                    label.setForeground(Theme.TEXT_PRIMARY);
                } else {
                    label.setOpaque(true);
                    label.setBackground(selected ? Theme.PRIMARY_LIGHT : Theme.SURFACE);
                    label.setForeground(selected ? Theme.PRIMARY_DARK : Theme.TEXT_PRIMARY);
                }
                return label;
            }
        });
        return combo;
    }

    /** A small uppercase caption, the same one used above every field. */
    public static JLabel label(String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(Theme.bold(11));
        label.setForeground(Theme.TEXT_SECONDARY);
        return label;
    }
}