package com.abcgreen.evcharging;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.abcgreen.evcharging.exception.ValidationException;
import com.abcgreen.evcharging.util.Validator;

class ValidatorTest {

    @Test
    @DisplayName("TC-VAL-01 Valid email is normalised to lower case")
    void email() throws ValidationException {
        assertEquals("aarav.sharma@gmail.com", Validator.email("  Aarav.Sharma@Gmail.com "));
    }

    @ParameterizedTest
    @ValueSource(strings = { "plainaddress", "a@b", "@gmail.com", "x@y.c", "" })
    @DisplayName("TC-VAL-02 Invalid emails are rejected")
    void badEmail(String email) {
        assertThrows(ValidationException.class, () -> Validator.email(email));
    }

    @Test
    @DisplayName("TC-VAL-03 Indian mobile numbers (+91 / spaces allowed)")
    void phone() throws ValidationException {
        assertEquals("9876543210", Validator.phone("+91 98765 43210"));
        assertThrows(ValidationException.class, () -> Validator.phone("12345"));
        assertThrows(ValidationException.class, () -> Validator.phone("5876543210"));
    }

    @Test
    @DisplayName("TC-VAL-04 Vehicle registration number normalised and validated")
    void registration() throws ValidationException {
        assertEquals("KA01AB1234", Validator.vehicleRegistration("ka-01-ab-1234"));
        assertEquals("DL3CAB1234", Validator.vehicleRegistration("DL 3C AB 1234"));
        assertEquals("22BH1234AA", Validator.vehicleRegistration("22BH1234AA"));
        assertThrows(ValidationException.class, () -> Validator.vehicleRegistration("XYZ"));
        assertThrows(ValidationException.class, () -> Validator.vehicleRegistration(null));
    }

    @Test
    @DisplayName("TC-VAL-05 Numeric range validation")
    void numbers() throws ValidationException {
        assertEquals(42, Validator.intInRange("42", "SOC", 0, 100));
        assertThrows(ValidationException.class, () -> Validator.intInRange("120", "SOC", 0, 100));
        assertThrows(ValidationException.class, () -> Validator.positiveNumber("-5", "Power"));
        assertThrows(ValidationException.class, () -> Validator.positiveNumber("abc", "Power"));
    }

    @Test
    @DisplayName("TC-VAL-06 HTML output is escaped (XSS prevention)")
    void html() {
        assertEquals("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;", Validator.html("<script>alert('x')</script>"));
        assertEquals("", Validator.html(null));
    }
}
