package com.abcgreen.evcharging.model;

/**
 * Abstract base class for people in the system (User accounts and Customers).
 * Demonstrates abstraction, encapsulation and inheritance.
 */
public abstract class Person {

    private String fullName;
    private String email;
    private String phone;

    protected Person() {
    }

    protected Person(String fullName, String email, String phone) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    /** Each subclass describes what kind of person it represents. */
    public abstract String getDisplayRole();

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    @Override
    public String toString() {
        return fullName + " (" + getDisplayRole() + ")";
    }
}
