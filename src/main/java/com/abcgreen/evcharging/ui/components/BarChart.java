package com.abcgreen.evcharging.ui.components;

import java.awt.Color;
import java.awt.Graphics2D;

import com.abcgreen.evcharging.ui.UiKit;

/** Vertical bar chart. Bars at or above 75% of the maximum are highlighted as peak values. */
public class BarChart extends ChartPanel {

    private static final long serialVersionUID = 1L;
    private Color color = UiKit.PRIMARY;
    private Color peakColor = UiKit.AMBER;

    public BarChart(String title) {
        super(title);
    }

    public BarChart colors(Color normal, Color peak) {
        this.color = normal;
        this.peakColor = peak;
        return this;
    }

    @Override
    protected void drawChart(Graphics2D g, int x, int y, int w, int h) {
        double max = max();
        drawGrid(g, x, y, w, h, max);
        int n = values.length;
        double slot = w / (double) n;
        int barW = Math.max(2, (int) (slot * 0.7));
        int labelEvery = n > 16 ? 3 : 1;
        for (int i = 0; i < n; i++) {
            int bh = (int) (h * values[i] / max);
            int bx = x + (int) (i * slot + (slot - barW) / 2);
            g.setColor(values[i] >= 0.75 * max() ? peakColor : color);
            g.fillRoundRect(bx, y + h - bh, barW, bh, 4, 4);
            if (i % labelEvery == 0) {
                g.setColor(UiKit.MUTED);
                String l = labels[i];
                g.drawString(l, bx + barW / 2 - g.getFontMetrics().stringWidth(l) / 2, y + h + 14);
            }
        }
    }
}
