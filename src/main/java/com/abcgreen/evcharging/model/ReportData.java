package com.abcgreen.evcharging.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic tabular report: a title, column headers and rows.
 * The same object is shown in a Swing JTable, on a JSP page and exported to CSV.
 */
public class ReportData {

    private final String title;
    private final String[] columns;
    private final List<Object[]> rows = new ArrayList<>();
    private long generationMillis;

    public ReportData(String title, String... columns) {
        this.title = title;
        this.columns = columns;
    }

    public void addRow(Object... values) {
        rows.add(values);
    }

    public String getTitle() { return title; }
    public String[] getColumns() { return columns; }
    public List<Object[]> getRows() { return rows; }
    public long getGenerationMillis() { return generationMillis; }
    public void setGenerationMillis(long generationMillis) { this.generationMillis = generationMillis; }
}
