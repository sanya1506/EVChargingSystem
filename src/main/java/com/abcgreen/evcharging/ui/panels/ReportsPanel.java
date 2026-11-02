package com.abcgreen.evcharging.ui.panels;

import java.awt.BorderLayout;
import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.abcgreen.evcharging.model.ReportData;
import com.abcgreen.evcharging.service.ReportService;
import com.abcgreen.evcharging.ui.Refreshable;
import com.abcgreen.evcharging.ui.UiKit;
import com.abcgreen.evcharging.ui.components.ReportView;

/** FR-11 Report generation with CSV export. */
public class ReportsPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private final transient ReportService service = new ReportService();
    private final JComboBox<String> type = new JComboBox<>(ReportService.REPORT_TYPES);
    private final JTextField day = new JTextField(LocalDate.now().toString(), 9);
    private final JTextField month = new JTextField(YearMonth.now().toString(), 7);
    private final ReportView view = new ReportView();
    private final JLabel status = new JLabel(" ");

    public ReportsPanel() {
        setLayout(new BorderLayout());
        JButton generate = UiKit.primaryButton("Generate");
        generate.addActionListener(e -> generate());
        JButton export = UiKit.button("Export CSV");
        export.addActionListener(e -> export());
        type.addActionListener(e -> generate());
        status.setForeground(UiKit.MUTED);
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(UiKit.toolbar(new JLabel("Report"), type, new JLabel("Day (yyyy-mm-dd)"), day, new JLabel("Month (yyyy-mm)"), month, generate, export),
                BorderLayout.NORTH);
        head.add(status, BorderLayout.SOUTH);
        body.add(head, BorderLayout.NORTH);
        body.add(UiKit.card(view.scroll()), BorderLayout.CENTER);
        add(UiKit.page("Reports", "Daily operations, monthly utilisation, demand, revenue and audit reports - exportable to CSV", body));
    }

    @Override
    public void refresh() {
        if (view.getReport() == null) {
            generate();
        }
    }

    private void generate() {
        String t = (String) type.getSelectedItem();
        UiKit.async(this, () -> service.generate(t, LocalDate.parse(day.getText().trim()), YearMonth.parse(month.getText().trim())), (ReportData r) -> {
            view.setReport(r);
            status.setText(r.getTitle() + "  -  " + r.getRows().size() + " rows generated in " + r.getGenerationMillis() + " ms");
        });
    }

    private void export() {
        ReportData r = view.getReport();
        if (r == null) {
            return;
        }
        File f = ReportService.defaultFile(r);
        UiKit.async(this, () -> service.exportCsv(r, f), file -> UiKit.info(this, "Report exported to\n" + file.getAbsolutePath()));
    }
}
