package com.abcgreen.evcharging.ui.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.abcgreen.evcharging.model.Payment;
import com.abcgreen.evcharging.service.InvoiceGenerator;
import com.abcgreen.evcharging.ui.UiKit;

/** Shows a charging receipt / invoice and lets the operator save it as .txt or .html. */
public final class ReceiptDialog {

    private ReceiptDialog() {
    }

    public static void show(Component parent, Payment p) {
        InvoiceGenerator gen = new InvoiceGenerator();
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(parent), "Receipt " + p.getInvoiceNo(), JDialog.ModalityType.APPLICATION_MODAL);
        JTextArea text = new JTextArea(gen.toText(p));
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        text.setEditable(false);
        text.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JButton saveTxt = UiKit.button("Save receipt (.txt)");
        JButton saveHtml = UiKit.button("Save invoice (.html)");
        JButton close = UiKit.primaryButton("Close");
        saveTxt.addActionListener(e -> save(d, new File("reports", p.getInvoiceNo() + ".txt"), gen.toText(p)));
        saveHtml.addActionListener(e -> save(d, new File("reports", p.getInvoiceNo() + ".html"), gen.toHtml(p)));
        close.addActionListener(e -> d.dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(saveTxt);
        buttons.add(saveHtml);
        buttons.add(close);
        d.setLayout(new BorderLayout());
        d.add(new JScrollPane(text), BorderLayout.CENTER);
        d.add(buttons, BorderLayout.SOUTH);
        d.setSize(560, 600);
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
    }

    private static void save(Component parent, File file, String content) {
        try {
            file.getAbsoluteFile().getParentFile().mkdirs();
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
            UiKit.info(parent, "Saved to " + file.getAbsolutePath());
        } catch (Exception ex) {
            UiKit.error(parent, ex);
        }
    }
}
