package com.abcgreen.evcharging.ui.panels;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.abcgreen.evcharging.model.Role;
import com.abcgreen.evcharging.model.User;
import com.abcgreen.evcharging.service.AuthService;
import com.abcgreen.evcharging.ui.UiKit;
import com.abcgreen.evcharging.ui.components.FormDialog;
import com.abcgreen.evcharging.util.PasswordUtil;

/** Administration: manage staff user accounts and roles (admin only). */
public class UsersPanel extends CrudPanel<User> {

    private static final long serialVersionUID = 1L;
    private final transient AuthService auth = new AuthService();

    public UsersPanel() {
        super("User Management", "Administrator and operator accounts with role-based access. Passwords are stored as salted PBKDF2 hashes.", "User",
                new String[] { "ID", "Username", "Full name", "Email", "Phone", "Role", "Active", "Failed logins", "Last login" },
                u -> new Object[] { u.getUserId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole(),
                    u.isActive() ? "ACTIVE" : "BLOCKED", u.getFailedAttempts(), UiKit.fmt(u.getLastLogin()) });
        JButton reset = UiKit.button("Reset Password");
        reset.addActionListener(e -> resetPassword());
        toolbar.add(reset, 5);
    }

    @Override
    protected List<User> load(String keyword) {
        String k = keyword == null ? "" : keyword.toLowerCase();
        return auth.staffUsers().stream()
                .filter(u -> u.getUsername().toLowerCase().contains(k) || u.getFullName().toLowerCase().contains(k))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    protected boolean edit(User existing) {
        JTextField username = new JTextField(existing == null ? "" : existing.getUsername());
        username.setEditable(existing == null);
        JTextField name = new JTextField(existing == null ? "" : existing.getFullName());
        JTextField email = new JTextField(existing == null ? "" : existing.getEmail());
        JTextField phone = new JTextField(existing == null ? "" : existing.getPhone());
        JComboBox<Role> role = new JComboBox<>(new Role[] { Role.OPERATOR, Role.ADMIN });
        JCheckBox active = new JCheckBox("Account active (unlocks a locked account)", existing == null || existing.isActive());
        JPasswordField password = new JPasswordField();
        FormDialog f = new FormDialog(this, existing == null ? "Create user" : "Edit user " + existing.getUsername())
                .add("Username *", username).add("Full name *", name).add("Email *", email).add("Phone", phone).add("Role", role);
        if (existing == null) {
            f.add("Password *", password).note(PasswordUtil.policyText());
        } else {
            role.setSelectedItem(existing.getRole());
            f.add("Status", active);
        }
        return f.onSave(() -> {
            if (existing == null) {
                auth.createStaffUser(username.getText(), new String(password.getPassword()), name.getText(), email.getText(), phone.getText(),
                        (Role) role.getSelectedItem());
            } else {
                existing.setFullName(name.getText());
                existing.setEmail(email.getText());
                existing.setPhone(phone.getText());
                existing.setRole((Role) role.getSelectedItem());
                existing.setActive(active.isSelected());
                auth.updateStaffUser(existing);
            }
            return null;
        }).showDialog();
    }

    private void resetPassword() {
        User u = selected();
        if (u == null) {
            return;
        }
        JPasswordField pf = new JPasswordField();
        if (JOptionPane.showConfirmDialog(this, new Object[] { "New password for " + u.getUsername() + ":", pf, PasswordUtil.policyText() },
                "Reset password", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            UiKit.async(this, () -> {
                auth.resetPassword(u.getUserId(), new String(pf.getPassword()));
                return null;
            }, x -> UiKit.info(this, "Password changed."));
        }
    }

    @Override
    protected void delete(User u) throws Exception {
        auth.deleteUser(u.getUserId());
    }
}
