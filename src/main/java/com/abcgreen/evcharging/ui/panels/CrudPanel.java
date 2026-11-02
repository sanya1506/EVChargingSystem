package com.abcgreen.evcharging.ui.panels;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.function.Function;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.abcgreen.evcharging.ui.Refreshable;
import com.abcgreen.evcharging.ui.UiKit;
import com.abcgreen.evcharging.ui.components.BeanTable;
import com.abcgreen.evcharging.util.CsvUtil;

/**
 * Abstract list screen with search, table, Add / Edit / Delete and CSV export.
 * Subclasses supply the data source and the edit form (template method pattern).
 */
public abstract class CrudPanel<T> extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    protected final BeanTable<T> table;
    protected final JTextField search = new JTextField(22);
    protected final JPanel toolbar;
    protected final JLabel count = new JLabel();
    protected final String entityName;

    protected CrudPanel(String title, String subtitle, String entityName, String[] columns, Function<T, Object[]> rowMapper) {
        this.entityName = entityName;
        this.table = new BeanTable<>(columns, rowMapper);
        setLayout(new BorderLayout());
        search.putClientProperty("JTextField.placeholderText", "Search...");
        search.addActionListener(e -> refresh());
        JButton find = UiKit.button("Search");
        find.addActionListener(e -> refresh());
        JButton add = UiKit.primaryButton("+ Add " + entityName);
        add.addActionListener(e -> {
            if (edit(null)) {
                refresh();
            }
        });
        JButton editBtn = UiKit.button("Edit");
        editBtn.addActionListener(e -> editSelected());
        JButton delete = UiKit.dangerButton("Delete");
        delete.addActionListener(e -> deleteSelected());
        JButton export = UiKit.button("Export CSV");
        export.addActionListener(e -> exportCsv());
        count.setForeground(UiKit.MUTED);
        toolbar = UiKit.toolbar(search, find, add, editBtn, delete, export, count);
        if (!canModify()) {
            add.setVisible(false);
            editBtn.setVisible(false);
            delete.setVisible(false);
        }
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && canModify()) {
                    editSelected();
                }
            }
        });
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(toolbar, BorderLayout.NORTH);
        body.add(UiKit.card(table.scroll()), BorderLayout.CENTER);
        add(UiKit.page(title, subtitle, body));
    }

    /** Loads the rows for the current search keyword (runs on a background thread). */
    protected abstract List<T> load(String keyword) throws Exception;

    /** Shows the add / edit form. @param item null for "add" @return true if saved */
    protected abstract boolean edit(T item);

    protected abstract void delete(T item) throws Exception;

    protected boolean canModify() {
        return true;
    }

    @Override
    public void refresh() {
        String k = search.getText();
        UiKit.async(this, () -> load(k), list -> {
            table.setItems(list);
            count.setText(list.size() + " records");
        });
    }

    protected T selected() {
        T item = table.getSelectedItem();
        if (item == null) {
            UiKit.info(this, "Please select a " + entityName.toLowerCase() + " in the table first.");
        }
        return item;
    }

    private void editSelected() {
        T item = selected();
        if (item != null && edit(item)) {
            refresh();
        }
    }

    private void deleteSelected() {
        T item = selected();
        if (item != null && UiKit.confirm(this, "Delete the selected " + entityName.toLowerCase()
                + "? Related records (vehicles, sessions, bills...) are also deleted.")) {
            UiKit.async(this, () -> {
                delete(item);
                return null;
            }, x -> refresh());
        }
    }

    private void exportCsv() {
        File file = new File("reports", entityName.toLowerCase().replace(' ', '_') + "_export_" + System.currentTimeMillis() + ".csv");
        UiKit.async(this, () -> {
            CsvUtil.write(file, table.columns(), table.rows());
            return file;
        }, f -> UiKit.info(this, table.getItems().size() + " rows exported to\n" + f.getAbsolutePath()));
    }
}
