package com.abcgreen.evcharging.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Secure password hashing with PBKDF2-HMAC-SHA256 and a random 16-byte salt.
 * Plain-text passwords are never stored. Stored format: iterations:salt:hash
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return hash(password, salt, ITERATIONS);
    }

    static String hash(String password, byte[] salt, int iterations) {
        byte[] hash = pbkdf2(password.toCharArray(), salt, iterations);
        return iterations + ":" + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        String[] parts = stored.split(":");
        if (parts.length != 3) {
            return false;
        }
        int iterations = Integer.parseInt(parts[0]);
        byte[] salt = Base64.getDecoder().decode(parts[1]);
        byte[] expected = Base64.getDecoder().decode(parts[2]);
        byte[] actual = pbkdf2(password.toCharArray(), salt, iterations);
        // constant-time comparison to avoid timing attacks
        int diff = expected.length ^ actual.length;
        for (int i = 0; i < expected.length && i < actual.length; i++) {
            diff |= expected[i] ^ actual[i];
        }
        return diff == 0;
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            KeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 not available", e);
        }
    }

    /**
     * Password policy: 8+ characters with upper case, lower case, digit and special character.
     * Implemented with a plain loop over the characters (Module I - loops and conditions).
     */
    public static boolean isStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean upper = false, lower = false, digit = false, special = false;
        for (char ch : password.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                upper = true;
            } else if (Character.isLowerCase(ch)) {
                lower = true;
            } else if (Character.isDigit(ch)) {
                digit = true;
            } else {
                special = true;
            }
        }
        return upper && lower && digit && special;
    }

    public static String policyText() {
        return "Password must be at least 8 characters and contain an upper case letter, "
                + "a lower case letter, a digit and a special character.";
    }
}
