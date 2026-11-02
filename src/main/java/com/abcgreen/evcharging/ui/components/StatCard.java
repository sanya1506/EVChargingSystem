package com.abcgreen.evcharging.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.abcgreen.evcharging.ui.UiKit;

/** Dashboard KPI tile: label, big value and a sub-text, with a coloured accent bar. */
public class StatCard extends JPanel {

    private static final long serialVersionUID = 1L;
    private final JLabel value = new JLabel("-");
    private final JLabel sub = new JLabel(" ");
    private final Color accent;

    public StatCard(String label, Color accent) {
        super(new BorderLayout(0, 2));
        this.accent = accent;
        setBackground(UiKit.CARD);
        setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 12));
        JLabel l = new JLabel(label.toUpperCase());
        l.setFont(UiKit.SMALL.deriveFont(Font.BOLD));
        l.setForeground(UiKit.MUTED);
        value.setFont(new Font("Segoe UI", Font.BOLD, 24));
        value.setForeground(UiKit.TEXT);
        sub.setFont(UiKit.SMALL);
        sub.setForeground(UiKit.MUTED);
        add(l, BorderLayout.NORTH);
        add(value, BorderLayout.CENTER);
        add(sub, BorderLayout.SOUTH);
    }

    public void set(String v, String subText) {
        value.setText(v);
        sub.setText(subText == null ? " " : subText);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(UiKit.BORDER);
        g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        g2.setColor(accent);
        g2.fillRect(0, 0, 5, getHeight());
        g2.dispose();
    }
}
