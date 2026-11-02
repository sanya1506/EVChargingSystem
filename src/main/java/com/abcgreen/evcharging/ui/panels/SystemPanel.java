package com.abcgreen.evcharging.ui.panels;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import com.abcgreen.evcharging.service.AuditService;
import com.abcgreen.evcharging.service.BackupService;
import com.abcgreen.evcharging.service.ConfigService;
import com.abcgreen.evcharging.ui.Refreshable;
import com.abcgreen.evcharging.ui.UiKit;
import com.abcgreen.evcharging.ui.components.ReportView;

/** FR-12 Database management (backup / restore), system configuration and audit log (admin only). */
public class SystemPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private final transient BackupService backup = new BackupService();
    private final transient ConfigService config = new ConfigService();
    private final transient AuditService audit = new AuditService();
    private final DefaultTableModel configModel = new DefaultTableModel(new String[] { "Setting", "Value", "Description" }, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int r, int c) {
            return c == 1;
        }
    };
    private final ReportView auditView = new ReportView();

    public SystemPanel() {
        setLayout(new BorderLayout());
        JButton backupBtn = UiKit.primaryButton("Backup Database Now");
        backupBtn.addActionListener(e -> doBackup());
        JButton restoreBtn = UiKit.dangerButton("Restore from Backup...");
        restoreBtn.addActionListener(e -> doRestore());
        JLabel note = new JLabel("<html>Backups are written to the <b>backups</b> folder as SQL files (pure JDBC, no mysqldump needed). "
                + "Restore replaces ALL data in one transaction.</html>");
        note.setForeground(UiKit.MUTED);
        JPanel backupCard = new JPanel(new BorderLayout(0, 8));
        backupCard.setOpaque(false);
        JLabel bt = new JLabel("Backup & Restore");
        bt.setFont(UiKit.HEADING);
        backupCard.add(bt, BorderLayout.NORTH);
        backupCard.add(UiKit.toolbar(backupBtn, restoreBtn), BorderLayout.CENTER);
        backupCard.add(note, BorderLayout.SOUTH);

        JTable configTable = new JTable(configModel);
        configTable.getColumnModel().getColumn(2).setPreferredWidth(380);
        JButton saveConfig = UiKit.primaryButton("Save Settings");
        saveConfig.addActionListener(e -> {
            if (configTable.isEditing()) {
                configTable.getCellEditor().stopCellEditing();
            }
            UiKit.async(this, () -> {
                for (int r = 0; r < configModel.getRowCount(); r++) {
                    config.set((String) configModel.getValueAt(r, 0), String.valueOf(configModel.getValueAt(r, 1)));
                }
                return null;
            }, x -> UiKit.info(this, "Settings saved."));
        });
        JPanel configCard = new JPanel(new BorderLayout(0, 8));
        configCard.setOpaque(false);
        JLabel ct = new JLabel("System Configuration");
        ct.setFont(UiKit.HEADING);
        configCard.add(ct, BorderLayout.NORTH);
        configCard.add(new JScrollPane(configTable), BorderLayout.CENTER);
        configCard.add(UiKit.toolbar(saveConfig), BorderLayout.SOUTH);

        JPanel left = new JPanel(new BorderLayout(0, 12));
        left.setOpaque(false);
        left.add(UiKit.card(backupCard), BorderLayout.NORTH);
        left.add(UiKit.card(configCard), BorderLayout.CENTER);

        JPanel auditCard = new JPanel(new BorderLayout(0, 8));
        auditCard.setOpaque(false);
        JLabel at = new JLabel("Audit / Transaction Log (latest 500)");
        at.setFont(UiKit.HEADING);
        auditCard.add(at, BorderLayout.NORTH);
        auditCard.add(auditView.scroll(), BorderLayout.CENTER);

        JPanel grid = new JPanel(new GridLayout(1, 2, 12, 0));
        grid.setOpaque(false);
        grid.add(left);
        grid.add(UiKit.card(auditCard));
        grid.setBorder(BorderFactory.createEmptyBorder());
        add(UiKit.page("System & Backup", "Database backup / restore, system settings and the audit trail", grid));
    }

    @Override
    public void refresh() {
        UiKit.async(this, () -> new Object[] { config.all(), audit.recent(500) }, r -> {
            configModel.setRowCount(0);
            @SuppressWarnings("unchecked")
            Map<String, String[]> map = (Map<String, String[]>) r[0];
            map.forEach((k, v) -> configModel.addRow(new Object[] { k, v[0], v[1] }));
            auditView.setReport((com.abcgreen.evcharging.model.ReportData) r[1]);
        });
    }

    private void doBackup() {
        File f = BackupService.defaultBackupFile();
        UiKit.async(this, () -> backup.backup(f), rows -> {
            UiKit.info(this, "Backup completed: " + rows + " rows written to\n" + f.getAbsolutePath());
            refresh();
        });
    }

    private void doRestore() {
        JFileChooser chooser = new JFileChooser(new File("backups"));
        chooser.setFileFilter(new FileNameExtensionFilter("SQL backup files", "sql"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File f = chooser.getSelectedFile();
        if (!UiKit.confirm(this, "Restore " + f.getName() + "? ALL current data will be replaced by the backup.")) {
            return;
        }
        UiKit.async(this, () -> backup.restore(f), n -> {
            UiKit.info(this, "Database restored (" + n + " statements executed).");
            refresh();
        });
    }
}
