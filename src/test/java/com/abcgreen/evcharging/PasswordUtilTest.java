package com.abcgreen.evcharging;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.abcgreen.evcharging.util.PasswordUtil;

class PasswordUtilTest {

    @Test
    @DisplayName("TC-AUTH-01 Hash verifies with the correct password only")
    void hashAndVerify() {
        String hash = PasswordUtil.hash("Admin@123");
        assertTrue(PasswordUtil.verify("Admin@123", hash));
        assertFalse(PasswordUtil.verify("admin@123", hash));
        assertFalse(PasswordUtil.verify("", hash));
    }

    @Test
    @DisplayName("TC-AUTH-02 Same password gives different hashes (random salt)")
    void saltIsRandom() {
        assertNotEquals(PasswordUtil.hash("Secret@1"), PasswordUtil.hash("Secret@1"));
    }

    @Test
    @DisplayName("TC-AUTH-03 Plain text is never stored")
    void notPlainText() {
        assertFalse(PasswordUtil.hash("Customer@123").contains("Customer@123"));
    }

    @Test
    @DisplayName("TC-AUTH-04 Password strength policy")
    void strength() {
        assertTrue(PasswordUtil.isStrong("Customer@123"));
        assertFalse(PasswordUtil.isStrong("short1!"));
        assertFalse(PasswordUtil.isStrong("alllowercase1!"));
        assertFalse(PasswordUtil.isStrong("NoDigits!!"));
        assertFalse(PasswordUtil.isStrong("NoSpecial123"));
        assertFalse(PasswordUtil.isStrong(null));
    }

    @Test
    @DisplayName("TC-AUTH-05 Malformed stored hash is rejected")
    void malformed() {
        assertFalse(PasswordUtil.verify("x", "not-a-hash"));
        assertFalse(PasswordUtil.verify(null, null));
    }
}
