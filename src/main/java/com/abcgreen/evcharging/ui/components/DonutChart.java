package com.abcgreen.evcharging.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import com.abcgreen.evcharging.ui.UiKit;

/** Donut chart with legend (e.g. charger status distribution). */
public class DonutChart extends ChartPanel {

    private static final long serialVersionUID = 1L;
    private Color[] colors = { UiKit.PRIMARY, UiKit.BLUE, UiKit.RED, UiKit.AMBER, UiKit.PURPLE, UiKit.MUTED };

    public DonutChart(String title) {
        super(title);
    }

    public DonutChart colors(Color... colors) {
        this.colors = colors;
        return this;
    }

    @Override
    protected void drawChart(Graphics2D g, int x, int y, int w, int h) {
        double total = 0;
        for (double v : values) {
            total += v;
        }
        int size = Math.min(h, w / 2) - 10;
        int cx = x - 20;
        int cy = y + (h - size) / 2 + 6;
        double angle = 90;
        g.setStroke(new BasicStroke(size / 5f));
        int inset = size / 10;
        for (int i = 0; i < values.length; i++) {
            double sweep = total == 0 ? 0 : 360 * values[i] / total;
            g.setColor(colors[i % colors.length]);
            g.drawArc(cx + inset, cy + inset, size - 2 * inset, size - 2 * inset, (int) Math.round(angle), -(int) Math.ceil(sweep));
            angle -= sweep;
        }
        g.setColor(UiKit.TEXT);
        g.setFont(new Font("Segoe UI", Font.BOLD, 20));
        String t = String.valueOf((int) total);
        g.drawString(t, cx + size / 2 - g.getFontMetrics().stringWidth(t) / 2, cy + size / 2 + 4);
        g.setFont(UiKit.SMALL);
        g.setColor(UiKit.MUTED);
        g.drawString("total", cx + size / 2 - g.getFontMetrics().stringWidth("total") / 2, cy + size / 2 + 18);

        int lx = cx + size + 24;
        int ly = y + 20;
        g.setStroke(new BasicStroke(1));
        for (int i = 0; i < values.length; i++) {
            g.setColor(colors[i % colors.length]);
            g.fillRoundRect(lx, ly + i * 26 - 10, 12, 12, 4, 4);
            g.setColor(UiKit.TEXT);
            g.setFont(UiKit.BODY);
            g.drawString(labels[i] + "  " + (int) values[i] + String.format(" (%.0f%%)", total == 0 ? 0 : values[i] * 100 / total), lx + 20, ly + i * 26);
        }
    }
}
