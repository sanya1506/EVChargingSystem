package com.abcgreen.evcharging.ui.components;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import com.abcgreen.evcharging.ui.UiKit;

/**
 * Generic read-only JTable for a list of objects. Each row is produced by a lambda
 * (Function<T, Object[]>), so one class serves every screen. Supports sorting and coloured status cells.
 */
public class BeanTable<T> extends JTable {

    private static final long serialVersionUID = 1L;
    private final Model<T> model;

    public BeanTable(String[] columns, Function<T, Object[]> rowMapper) {
        this.model = new Model<>(columns, rowMapper);
        setModel(model);
        setRowSorter(new TableRowSorter<>(model));
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFillsViewportHeight(true);
        setAutoCreateRowSorter(false);
        setDefaultRenderer(Object.class, new StatusRenderer());
        getTableHeader().setReorderingAllowed(false);
    }

    public void setItems(List<T> items) {
        model.items = new ArrayList<>(items);
        model.fireTableDataChanged();
    }

    public List<T> getItems() {
        return model.items;
    }

    /** The object in the selected row (taking sorting into account), or null. */
    public T getSelectedItem() {
        int row = getSelectedRow();
        return row < 0 ? null : model.items.get(convertRowIndexToModel(row));
    }

    public JScrollPane scroll() {
        JScrollPane sp = new JScrollPane(this);
        sp.setBorder(javax.swing.BorderFactory.createLineBorder(UiKit.BORDER));
        return sp;
    }

    /** Rows as Object[] (used for CSV export). */
    public List<Object[]> rows() {
        List<Object[]> rows = new ArrayList<>();
        for (T t : model.items) {
            rows.add(model.rowMapper.apply(t));
        }
        return rows;
    }

    public String[] columns() {
        return model.columns;
    }

    private static class Model<T> extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final String[] columns;
        private final Function<T, Object[]> rowMapper;
        private List<T> items = new ArrayList<>();

        Model(String[] columns, Function<T, Object[]> rowMapper) {
            this.columns = columns;
            this.rowMapper = rowMapper;
        }

        @Override
        public int getRowCount() {
            return items.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int row, int col) {
            return rowMapper.apply(items.get(row))[col];
        }

        @Override
        public Class<?> getColumnClass(int col) {
            if (items.isEmpty()) {
                return Object.class;
            }
            Object v = getValueAt(0, col);
            return v instanceof Number ? Number.class : Object.class;
        }
    }

    /** Colours well-known status words (AVAILABLE, FAULTY, PAID...). */
    public static class StatusRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, selected, focus, row, col);
            if (value instanceof Number) {
                setHorizontalAlignment(JLabel.RIGHT);
            } else {
                setHorizontalAlignment(JLabel.LEFT);
            }
            if (!selected) {
                c.setForeground(colorFor(value == null ? "" : value.toString()));
            }
            return c;
        }

        static Color colorFor(String v) {
            switch (v) {
                case "AVAILABLE": case "ACTIVE": case "PAID": case "COMPLETED": case "ALLOCATED": case "PEAK!": return UiKit.PRIMARY_DARK;
                case "OCCUPIED": case "IN_PROGRESS": case "BOOKED": case "WAITING": case "SCHEDULED": return UiKit.BLUE;
                case "FAULTY": case "FAILED": case "BLOCKED": case "CANCELLED": case "PEAK": return UiKit.RED;
                case "MAINTENANCE": case "PENDING": case "NO_SHOW": case "MEDIUM": case "INACTIVE": return UiKit.AMBER;
                default: return UiKit.TEXT;
            }
        }
    }
}
