package com.abcgreen.evcharging.util;

import java.util.regex.Pattern;

import com.abcgreen.evcharging.exception.ValidationException;

/**
 * Input validation helpers (Module I - Strings, operators, conditional statements).
 * Each check throws a ValidationException with a user-friendly message.
 */
public final class Validator {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,}$");
    /** Indian mobile number: 10 digits starting with 6-9. */
    private static final Pattern PHONE = Pattern.compile("^[6-9]\\d{9}$");
    /** Indian vehicle registration e.g. KA01AB1234, DL3CAB1234, 22BH1234AA (spaces/hyphens removed). */
    private static final Pattern VEHICLE_REG = Pattern.compile("^([A-Z]{2}\\d{1,2}[A-Z]{0,3}\\d{4}|\\d{2}BH\\d{4}[A-Z]{1,2})$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z][A-Za-z0-9._]{3,49}$");
    private static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z .'-]{1,99}$");

    private Validator() {
    }

    public static String required(String value, String field) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field + " is required.");
        }
        return value.trim();
    }

    public static String name(String value, String field) throws ValidationException {
        String v = required(value, field);
        if (!NAME.matcher(v).matches()) {
            throw new ValidationException(field + " may contain letters, spaces, dots and hyphens only.");
        }
        return v;
    }

    public static String email(String value) throws ValidationException {
        String v = required(value, "Email").toLowerCase();
        if (!EMAIL.matcher(v).matches()) {
            throw new ValidationException("Please enter a valid email address.");
        }
        return v;
    }

    public static String phone(String value) throws ValidationException {
        String v = required(value, "Phone").replaceAll("[\\s-]", "");
        if (v.startsWith("+91")) {
            v = v.substring(3);
        }
        if (!PHONE.matcher(v).matches()) {
            throw new ValidationException("Phone must be a valid 10-digit Indian mobile number.");
        }
        return v;
    }

    public static String username(String value) throws ValidationException {
        String v = required(value, "Username");
        if (!USERNAME.matcher(v).matches()) {
            throw new ValidationException("Username must be 4-50 characters, start with a letter and contain only letters, digits, '.' or '_'.");
        }
        return v;
    }

    /** Normalises (upper case, no spaces or hyphens) and validates a registration number. */
    public static String vehicleRegistration(String value) throws ValidationException {
        String v = required(value, "Registration number").toUpperCase().replaceAll("[\\s-]", "");
        if (!VEHICLE_REG.matcher(v).matches()) {
            throw new ValidationException("Invalid registration number. Example: KA01AB1234");
        }
        return v;
    }

    public static double positiveNumber(String value, String field) throws ValidationException {
        String v = required(value, field);
        try {
            double d = Double.parseDouble(v);
            if (d <= 0) {
                throw new ValidationException(field + " must be greater than zero.");
            }
            return d;
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a number.");
        }
    }

    public static int intInRange(String value, String field, int min, int max) throws ValidationException {
        String v = required(value, field);
        try {
            int n = Integer.parseInt(v);
            if (n < min || n > max) {
                throw new ValidationException(field + " must be between " + min + " and " + max + ".");
            }
            return n;
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a whole number.");
        }
    }

    /** Escapes text for safe output inside HTML (prevents XSS in JSP pages). */
    public static String html(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '&': sb.append("&amp;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
