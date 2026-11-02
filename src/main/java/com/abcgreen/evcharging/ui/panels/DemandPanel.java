package com.abcgreen.evcharging.ui.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.time.LocalDate;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.abcgreen.evcharging.model.ChargingStation;
import com.abcgreen.evcharging.model.ReportData;
import com.abcgreen.evcharging.service.DemandService;
import com.abcgreen.evcharging.service.ReportService;
import com.abcgreen.evcharging.service.StationService;
import com.abcgreen.evcharging.ui.Refreshable;
import com.abcgreen.evcharging.ui.UiKit;
import com.abcgreen.evcharging.ui.components.BarChart;
import com.abcgreen.evcharging.ui.components.ReportView;

/** FR-08 Demand data management: peak hours, daily / monthly demand, historical records, CSV import/export. */
public class DemandPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private final transient DemandService demand = new DemandService();
    private final transient StationService stations = new StationService();
    private final BarChart peakChart = new BarChart("Sessions by hour of day (last 30 days) - peak hours highlighted");
    private final JLabel peakLabel = new JLabel();
    private final ReportView dailyView = new ReportView();
    private final ReportView monthlyView = new ReportView();
    private final ReportView peakView = new ReportView();
    private final ReportView recordsView = new ReportView();
    private final JComboBox<Object> station = new JComboBox<>();
    private final JTextField from = new JTextField(LocalDate.now().minusDays(7).toString(), 9);
    private final JTextField to = new JTextField(LocalDate.now().toString(), 9);
    private final JTabbedPane tabs = new JTabbedPane();

    public DemandPanel() {
        setLayout(new BorderLayout());
        peakChart.setPreferredSize(new Dimension(800, 250));
        peakLabel.setForeground(UiKit.MUTED);
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setOpaque(false);
        top.add(peakChart, BorderLayout.CENTER);
        top.add(peakLabel, BorderLayout.SOUTH);

        tabs.addTab("Peak Hours", wrap(peakView, null));
        tabs.addTab("Daily Demand", wrap(dailyView, null));
        tabs.addTab("Monthly Demand", wrap(monthlyView, null));
        tabs.addTab("Historical Records", recordsTab());

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);
        body.add(tabs, BorderLayout.CENTER);
        add(UiKit.page("Demand Analytics", "Historical charging demand per station and hour - daily, monthly and peak-hour analysis", body));
    }

    private JComponent wrap(ReportView view, JComponent toolbar) {
        JButton export = UiKit.button("Export CSV");
        export.addActionListener(e -> export(view));
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        p.add(toolbar != null ? toolbar : UiKit.toolbar(export), BorderLayout.NORTH);
        p.add(UiKit.card(view.scroll()), BorderLayout.CENTER);
        return p;
    }

    private JComponent recordsTab() {
        JButton load = UiKit.primaryButton("Load");
        load.addActionListener(e -> loadRecords());
        JButton importCsv = UiKit.button("Import CSV...");
        importCsv.addActionListener(e -> importCsv());
        JButton export = UiKit.button("Export CSV");
        export.addActionListener(e -> export(recordsView));
        return wrap(recordsView, UiKit.toolbar(new JLabel("Station"), station, new JLabel("From"), from, new JLabel("To"), to, load, importCsv, export));
    }

    @Override
    public void refresh() {
        UiKit.async(this, () -> new Object[] { demand.peakHours(30), demand.daily(60), demand.monthly(), stations.findAll() }, r -> {
            ReportData peak = (ReportData) r[0];
            peakView.setReport(peak);
            dailyView.setReport((ReportData) r[1]);
            monthlyView.setReport((ReportData) r[2]);
            String[] labels = new String[24];
            double[] values = new double[24];
            int peakHour = 0;
            for (int h = 0; h < 24; h++) {
                labels[h] = String.valueOf(h);
                values[h] = ((Number) peak.getRows().get(h)[1]).doubleValue();
                if (values[h] > values[peakHour]) {
                    peakHour = h;
                }
            }
            peakChart.setData(labels, values);
            peakLabel.setText("Busiest hour: " + peakHour + ":00-" + peakHour + ":59 (orange bars = peak hours, at least 75% of the busiest hour)");
            if (station.getItemCount() == 0) {
                station.addItem("All stations");
                @SuppressWarnings("unchecked")
                java.util.List<ChargingStation> list = (java.util.List<ChargingStation>) r[3];
                list.forEach(station::addItem);
                loadRecords();
            }
        });
    }

    private void loadRecords() {
        Object sel = station.getSelectedItem();
        Integer stationId = sel instanceof ChargingStation ? ((ChargingStation) sel).getStationId() : null;
        UiKit.async(this, () -> demand.records(stationId, LocalDate.parse(from.getText().trim()), LocalDate.parse(to.getText().trim())), recordsView::setReport);
    }

    private void importCsv() {
        JFileChooser chooser = new JFileChooser(new File("."));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        chooser.setDialogTitle("Import demand CSV (" + String.join(",", DemandService.CSV_HEADER) + ")");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            UiKit.async(this, () -> demand.importCsv(f), msg -> {
                UiKit.info(this, msg);
                loadRecords();
            });
        }
    }

    private void export(ReportView view) {
        ReportData data = view.getReport();
        if (data == null) {
            return;
        }
        File f = ReportService.defaultFile(data);
        UiKit.async(this, () -> {
            demand.exportCsv(data, f);
            return f;
        }, file -> UiKit.info(this, data.getRows().size() + " rows exported to\n" + file.getAbsolutePath()));
    }
}
