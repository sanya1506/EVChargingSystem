package com.abcgreen.evcharging.ui.components;

import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.abcgreen.evcharging.model.ReportData;
import com.abcgreen.evcharging.ui.UiKit;

/** JTable that displays any ReportData (sortable, read-only). */
public class ReportView extends JTable {

    private static final long serialVersionUID = 1L;
    private ReportData report;

    public ReportView() {
        setAutoCreateRowSorter(true);
        setFillsViewportHeight(true);
        setDefaultRenderer(Object.class, new BeanTable.StatusRenderer());
        getTableHeader().setReorderingAllowed(false);
    }

    public void setReport(ReportData data) {
        this.report = data;
        DefaultTableModel model = new DefaultTableModel(data.getColumns(), 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                List<Object[]> rows = data.getRows();
                return !rows.isEmpty() && rows.get(0)[c] instanceof Number ? Number.class : Object.class;
            }
        };
        for (Object[] row : data.getRows()) {
            model.addRow(row);
        }
        setModel(model);
    }

    public ReportData getReport() {
        return report;
    }

    public JScrollPane scroll() {
        JScrollPane sp = new JScrollPane(this);
        sp.setBorder(javax.swing.BorderFactory.createLineBorder(UiKit.BORDER));
        return sp;
    }
}
