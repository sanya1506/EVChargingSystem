package com.abcgreen.evcharging.ui.panels;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.abcgreen.evcharging.model.QueueEntry;
import com.abcgreen.evcharging.service.QueueProcessor;
import com.abcgreen.evcharging.service.QueueService;
import com.abcgreen.evcharging.service.QueueSimulator;
import com.abcgreen.evcharging.ui.Refreshable;
import com.abcgreen.evcharging.ui.UiKit;
import com.abcgreen.evcharging.ui.components.BeanTable;

/** FR-06 Charging queue management + multithreaded queue simulation. */
public class QueuePanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private final transient QueueService service = new QueueService();

    private final BeanTable<QueueEntry> waiting = new BeanTable<>(
            new String[] { "Queue #", "Position", "Customer", "Vehicle", "Station", "Charger type", "Requested", "Est. wait (min)" },
            q -> new Object[] { q.getQueueId(), q.getPosition(), q.getCustomerName(), q.getVehicleRegNo(), q.getStationName(), q.getChargerType(),
                UiKit.fmt(q.getRequestedAt()), q.getEstimatedWaitMin() });
    private final BeanTable<QueueEntry> recent = new BeanTable<>(
            new String[] { "Queue #", "Customer", "Vehicle", "Station", "Type", "Requested", "Status", "Allocated charger", "Allocated at" },
            q -> new Object[] { q.getQueueId(), q.getCustomerName(), q.getVehicleRegNo(), q.getStationName(), q.getChargerType(),
                UiKit.fmt(q.getRequestedAt()), q.getStatus(), q.getAllocatedChargerCode(), UiKit.fmt(q.getAllocatedAt()) });
    private final JLabel summary = new JLabel();

    public QueuePanel() {
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Live Queue", liveTab());
        tabs.addTab("Thread Simulation", simulationTab());
        add(UiKit.page("Charging Queue", "Customers waiting for a charger. The queue processor thread allocates free chargers automatically (FIFO).", tabs));
        new Timer(10000, e -> {
            if (isShowing()) {
                refresh();
            }
        }).start();
    }

    private JComponent liveTab() {
        JButton process = UiKit.primaryButton("Process Queue Now");
        process.addActionListener(e -> {
            QueueProcessor.wakeUp();
            new Timer(1500, ev -> {
                ((Timer) ev.getSource()).stop();
                refresh();
            }).start();
        });
        JButton cancel = UiKit.dangerButton("Remove from Queue");
        cancel.addActionListener(e -> {
            QueueEntry q = waiting.getSelectedItem();
            if (q != null && UiKit.confirm(this, "Remove " + q.getCustomerName() + " from the queue?")) {
                UiKit.async(this, () -> {
                    service.cancel(q.getQueueId());
                    return null;
                }, x -> refresh());
            }
        });
        JButton reload = UiKit.button("Refresh");
        reload.addActionListener(e -> refresh());
        summary.setFont(UiKit.HEADING);

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(UiKit.toolbar(process, cancel, reload, summary), BorderLayout.NORTH);
        top.add(UiKit.card(waiting.scroll()), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        JLabel h = new JLabel("Queue history (latest 200)");
        h.setFont(UiKit.HEADING);
        bottom.add(h, BorderLayout.NORTH);
        bottom.add(UiKit.card(recent.scroll()), BorderLayout.CENTER);
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 12));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        p.add(top);
        p.add(bottom);
        return p;
    }

    private JComponent simulationTab() {
        JSpinner chargers = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        JSpinner vehicles = new JSpinner(new SpinnerNumberModel(12, 1, 100, 1));
        JSpinner arrival = new JSpinner(new SpinnerNumberModel(400, 50, 5000, 50));
        JSpinner charge = new JSpinner(new SpinnerNumberModel(1500, 100, 10000, 100));
        JTextArea log = new JTextArea();
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        log.setEditable(false);
        JButton run = UiKit.primaryButton("Run Simulation");
        run.addActionListener(e -> {
            run.setEnabled(false);
            log.setText("");
            int c = (Integer) chargers.getValue();
            int v = (Integer) vehicles.getValue();
            int a = (Integer) arrival.getValue();
            int ch = (Integer) charge.getValue();
            Thread t = new Thread(() -> {
                try {
                    new QueueSimulator().run(c, v, a, ch, line -> SwingUtilities.invokeLater(() -> {
                        log.append(line + "\n");
                        log.setCaretPosition(log.getDocument().getLength());
                    }));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    SwingUtilities.invokeLater(() -> run.setEnabled(true));
                }
            }, "simulation-runner");
            t.start();
        });
        JPanel controls = UiKit.toolbar(new JLabel("Chargers"), chargers, new JLabel("Vehicles"), vehicles, new JLabel("Avg arrival (ms)"), arrival,
                new JLabel("Avg charging (ms)"), charge, run);
        JLabel note = new JLabel("<html>Producer thread = vehicles arriving; one consumer thread per charger. Shared java.util.Queue protected with "
                + "synchronized / wait() / notifyAll(). No database changes are made.</html>");
        note.setForeground(UiKit.MUTED);
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(controls, BorderLayout.NORTH);
        head.add(note, BorderLayout.SOUTH);
        p.add(head, BorderLayout.NORTH);
        p.add(UiKit.card(new JScrollPane(log)), BorderLayout.CENTER);
        return p;
    }

    @Override
    public void refresh() {
        UiKit.async(this, () -> new Object[] { service.waiting(), service.recent(200) }, r -> {
            @SuppressWarnings("unchecked")
            java.util.List<QueueEntry> w = (java.util.List<QueueEntry>) r[0];
            @SuppressWarnings("unchecked")
            java.util.List<QueueEntry> h = (java.util.List<QueueEntry>) r[1];
            waiting.setItems(w);
            recent.setItems(h);
            summary.setText("   " + w.size() + " customer(s) waiting");
        });
    }
}
