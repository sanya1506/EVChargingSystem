package com.abcgreen.evcharging.model;

/** User roles used for role-based access control. */
public enum Role {
    ADMIN("Administrator"),
    OPERATOR("Station Operator"),
    CUSTOMER("Customer");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Only administrators may manage users, back up / restore the database and change settings. */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /** Admins and operators may use the desktop operations console. */
    public boolean isStaff() {
        return this == ADMIN || this == OPERATOR;
    }
}
