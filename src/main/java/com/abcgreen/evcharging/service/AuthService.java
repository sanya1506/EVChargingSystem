package com.abcgreen.evcharging.service;

import java.util.List;

import com.abcgreen.evcharging.dao.CustomerDao;
import com.abcgreen.evcharging.dao.Transaction;
import com.abcgreen.evcharging.dao.UserDao;
import com.abcgreen.evcharging.exception.AuthenticationException;
import com.abcgreen.evcharging.exception.DuplicateRecordException;
import com.abcgreen.evcharging.exception.EVChargingException;
import com.abcgreen.evcharging.exception.ValidationException;
import com.abcgreen.evcharging.model.Customer;
import com.abcgreen.evcharging.model.Role;
import com.abcgreen.evcharging.model.User;
import com.abcgreen.evcharging.util.PasswordUtil;
import com.abcgreen.evcharging.util.Validator;

/** FR-01 User authentication, customer self-registration and user management. */
public class AuthService {

    private final UserDao userDao = new UserDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final AuditService audit = new AuditService();
    private final ConfigService config = new ConfigService();

    /**
     * Verifies username/email + password. Accounts are locked after too many failed attempts.
     */
    public User login(String username, String password) throws EVChargingException {
        Validator.required(username, "Username");
        Validator.required(password, "Password");
        User user = userDao.findByUsername(username.trim());
        if (user == null) {
            throw new AuthenticationException("Invalid username or password.");
        }
        if (!user.isActive()) {
            throw new AuthenticationException("This account is locked or disabled. Please contact the administrator.");
        }
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            int max = config.getInt(ConfigService.MAX_FAILED_LOGINS, 5);
            boolean lock = user.getFailedAttempts() + 1 >= max;
            userDao.recordFailedAttempt(user.getUserId(), lock);
            SecurityContext.setRequestUser(user);
            audit.log("LOGIN_FAILED", "USER", user.getUserId(), lock ? "Account locked" : "Wrong password");
            SecurityContext.clearRequestUser();
            throw new AuthenticationException(lock ? "Too many failed attempts. The account has been locked."
                    : "Invalid username or password. Attempts left: " + (max - user.getFailedAttempts() - 1));
        }
        userDao.recordLogin(user.getUserId());
        SecurityContext.setRequestUser(user);
        audit.log("LOGIN", "USER", user.getUserId(), user.getRole().name());
        SecurityContext.clearRequestUser();
        return user;
    }

    /** Web self-registration: creates the user account and the customer profile in one transaction. */
    public Customer registerCustomer(String username, String password, String confirm, String fullName, String email,
                                     String phone, String city, String address) throws EVChargingException {
        String u = Validator.username(username);
        String name = Validator.name(fullName, "Full name");
        String mail = Validator.email(email);
        String mobile = Validator.phone(phone);
        String c = Validator.required(city, "City");
        if (!PasswordUtil.isStrong(password)) {
            throw new ValidationException(PasswordUtil.policyText());
        }
        if (!password.equals(confirm)) {
            throw new ValidationException("Passwords do not match.");
        }
        if (userDao.usernameExists(u)) {
            throw new DuplicateRecordException("Username '" + u + "' is already taken.");
        }
        if (userDao.emailExists(mail) || customerDao.emailExists(mail, 0)) {
            throw new DuplicateRecordException("A customer with this email is already registered.");
        }
        if (customerDao.phoneExists(mobile, 0)) {
            throw new DuplicateRecordException("A customer with this phone number is already registered.");
        }
        return Transaction.execute(() -> {
            User user = new User(u, name, mail, mobile, Role.CUSTOMER);
            user.setPasswordHash(PasswordUtil.hash(password));
            userDao.insert(user);
            Customer customer = new Customer(name, mail, mobile, c, address);
            customer.setUserId(user.getUserId());
            customerDao.insert(customer);
            audit.log("REGISTER", "CUSTOMER", customer.getCustomerId(), name + " registered online");
            return customer;
        });
    }

    public List<User> staffUsers() {
        return userDao.findStaff();
    }

    public void createStaffUser(String username, String password, String fullName, String email, String phone, Role role)
            throws EVChargingException {
        requireAdmin();
        String u = Validator.username(username);
        if (!PasswordUtil.isStrong(password)) {
            throw new ValidationException(PasswordUtil.policyText());
        }
        if (userDao.usernameExists(u)) {
            throw new DuplicateRecordException("Username '" + u + "' already exists.");
        }
        String mail = Validator.email(email);
        if (userDao.emailExists(mail)) {
            throw new DuplicateRecordException("Email already used by another account.");
        }
        User user = new User(u, Validator.name(fullName, "Full name"), mail, phone == null || phone.isBlank() ? null : Validator.phone(phone), role);
        user.setPasswordHash(PasswordUtil.hash(password));
        userDao.insert(user);
        audit.log("CREATE", "USER", user.getUserId(), u + " as " + role);
    }

    public void updateStaffUser(User user) throws EVChargingException {
        requireAdmin();
        Validator.name(user.getFullName(), "Full name");
        user.setEmail(Validator.email(user.getEmail()));
        userDao.update(user);
        audit.log("UPDATE", "USER", user.getUserId(), user.getUsername() + (user.isActive() ? " active" : " disabled"));
    }

    public void resetPassword(int userId, String newPassword) throws EVChargingException {
        if (!PasswordUtil.isStrong(newPassword)) {
            throw new ValidationException(PasswordUtil.policyText());
        }
        User current = SecurityContext.current();
        if (current != null && current.getUserId() != userId) {
            requireAdmin();
        }
        userDao.updatePassword(userId, PasswordUtil.hash(newPassword));
        audit.log("RESET_PASSWORD", "USER", userId, null);
    }

    public void deleteUser(int userId) throws EVChargingException {
        requireAdmin();
        User current = SecurityContext.current();
        if (current != null && current.getUserId() == userId) {
            throw new ValidationException("You cannot delete your own account.");
        }
        userDao.delete(userId);
        audit.log("DELETE", "USER", userId, null);
    }

    /** Role-based access check. */
    public static void requireAdmin() throws AuthenticationException {
        User u = SecurityContext.current();
        if (u == null || !u.getRole().isAdmin()) {
            throw new AuthenticationException("Only an administrator can perform this action.");
        }
    }
}
