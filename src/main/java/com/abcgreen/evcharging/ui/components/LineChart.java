package com.abcgreen.evcharging.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;

import com.abcgreen.evcharging.ui.UiKit;

/** Line / area chart for time series. */
public class LineChart extends ChartPanel {

    private static final long serialVersionUID = 1L;
    private final Color color;

    public LineChart(String title, Color color) {
        super(title);
        this.color = color;
    }

    @Override
    protected void drawChart(Graphics2D g, int x, int y, int w, int h) {
        double max = max() * 1.1;
        drawGrid(g, x, y, w, h, max);
        int n = values.length;
        if (n < 2) {
            return;
        }
        int[] xs = new int[n];
        int[] ys = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = x + (int) (w * i / (double) (n - 1));
            ys[i] = y + h - (int) (h * values[i] / max);
        }
        Polygon area = new Polygon();
        area.addPoint(xs[0], y + h);
        for (int i = 0; i < n; i++) {
            area.addPoint(xs[i], ys[i]);
        }
        area.addPoint(xs[n - 1], y + h);
        g.setPaint(new GradientPaint(0, y, new Color(color.getRed(), color.getGreen(), color.getBlue(), 90), 0, y + h,
                new Color(color.getRed(), color.getGreen(), color.getBlue(), 10)));
        g.fill(area);
        g.setColor(color);
        g.setStroke(new BasicStroke(2.2f));
        g.drawPolyline(xs, ys, n);
        int every = Math.max(1, n / 6);
        for (int i = 0; i < n; i++) {
            if (i % every == 0 || i == n - 1) {
                g.setColor(UiKit.MUTED);
                g.drawString(labels[i], xs[i] - g.getFontMetrics().stringWidth(labels[i]) / 2, y + h + 14);
            }
        }
    }
}
