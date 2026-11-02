package com.abcgreen.evcharging.service;

import com.abcgreen.evcharging.model.User;

/**
 * Remembers who is logged in, so services can write audit entries.
 * Web requests use a per-thread user (set by the AuthFilter); the desktop app uses one global user.
 */
public final class SecurityContext {

    private static volatile User desktopUser;
    private static final ThreadLocal<User> REQUEST_USER = new ThreadLocal<>();

    private SecurityContext() {
    }

    public static void setDesktopUser(User user) {
        desktopUser = user;
    }

    public static void setRequestUser(User user) {
        REQUEST_USER.set(user);
    }

    public static void clearRequestUser() {
        REQUEST_USER.remove();
    }

    public static User current() {
        User u = REQUEST_USER.get();
        return u != null ? u : desktopUser;
    }

    public static String username() {
        User u = current();
        return u == null ? "system" : u.getUsername();
    }
}
