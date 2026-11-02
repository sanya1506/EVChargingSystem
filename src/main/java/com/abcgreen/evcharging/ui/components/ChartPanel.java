package com.abcgreen.evcharging.ui.components;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import com.abcgreen.evcharging.ui.UiKit;

/**
 * Abstract chart drawn with Java 2D (no external chart library).
 * Subclasses implement {@link #drawChart}: template-method pattern + polymorphism.
 */
public abstract class ChartPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    protected String[] labels = new String[0];
    protected double[] values = new double[0];
    private final String title;

    protected ChartPanel(String title) {
        this.title = title;
        setBackground(UiKit.CARD);
        setPreferredSize(new Dimension(420, 260));
    }

    public void setData(String[] labels, double[] values) {
        this.labels = labels;
        this.values = values;
        repaint();
    }

    protected double max() {
        double m = 0;
        for (double v : values) {
            m = Math.max(m, v);
        }
        return m == 0 ? 1 : m;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(UiKit.BORDER);
        g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        g2.setColor(UiKit.TEXT);
        g2.setFont(UiKit.HEADING);
        g2.drawString(title, 14, 24);
        g2.setFont(UiKit.SMALL);
        if (values.length > 0) {
            drawChart(g2, 44, 40, getWidth() - 60, getHeight() - 74);
        } else {
            g2.setColor(UiKit.MUTED);
            g2.drawString("Loading...", 14, 50);
        }
        g2.dispose();
    }

    protected abstract void drawChart(Graphics2D g, int x, int y, int w, int h);

    /** Horizontal grid lines with value labels. */
    protected void drawGrid(Graphics2D g, int x, int y, int w, int h, double max) {
        g.setFont(UiKit.SMALL.deriveFont(Font.PLAIN, 10f));
        for (int i = 0; i <= 4; i++) {
            int gy = y + h - (int) (h * i / 4.0);
            g.setColor(UiKit.BORDER);
            g.drawLine(x, gy, x + w, gy);
            g.setColor(UiKit.MUTED);
            String label = max * i / 4 >= 1000 ? String.format("%.1fk", max * i / 4000) : String.format("%.0f", max * i / 4);
            g.drawString(label, x - g.getFontMetrics().stringWidth(label) - 6, gy + 4);
        }
    }
}
