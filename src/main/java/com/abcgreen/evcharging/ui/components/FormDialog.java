package com.abcgreen.evcharging.ui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.abcgreen.evcharging.ui.UiKit;

/**
 * Simple modal form: add labelled fields, then showDialog(). The onSave action may throw a
 * validation exception - the message is shown and the dialog stays open so the user can fix it.
 */
public class FormDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private final JPanel form = new JPanel(new GridBagLayout());
    private int row;
    private boolean saved;
    private UiKit.Task<Void> onSave;

    public FormDialog(Component parent, String title) {
        super(SwingUtilities.getWindowAncestor(parent), title, ModalityType.APPLICATION_MODAL);
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
    }

    public FormDialog add(String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 4, 5, 8);
        c.gridy = row++;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        field.setPreferredSize(new java.awt.Dimension(280, field.getPreferredSize().height + 4));
        form.add(field, c);
        return this;
    }

    public FormDialog note(String text) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row++;
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        JLabel l = new JLabel("<html><body style='width:380px'>" + text + "</body></html>");
        l.setForeground(UiKit.MUTED);
        l.setFont(UiKit.SMALL);
        form.add(l, c);
        return this;
    }

    public FormDialog onSave(UiKit.Task<Void> action) {
        this.onSave = action;
        return this;
    }

    /** @return true when the form was saved successfully */
    public boolean showDialog() {
        JButton save = UiKit.primaryButton("Save");
        JButton cancel = UiKit.button("Cancel");
        save.addActionListener(e -> {
            try {
                if (onSave != null) {
                    onSave.run();
                }
                saved = true;
                dispose();
            } catch (Exception ex) {
                UiKit.error(this, ex);
            }
        });
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(save);
        getRootPane().setDefaultButton(save);
        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        Window owner = getOwner();
        setLocationRelativeTo(owner);
        setVisible(true);
        return saved;
    }
}
