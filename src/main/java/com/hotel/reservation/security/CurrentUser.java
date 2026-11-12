package com.hotel.reservation.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Stores and reads the signed-in users in the HTTP session.
 * Admin and guest logins are kept under different keys, so they are completely separate.
 */
public final class CurrentUser {

    public static final String ADMIN_KEY = "SIGNED_IN_ADMIN";
    public static final String GUEST_KEY = "SIGNED_IN_GUEST";

    private CurrentUser() {
    }

    public static SessionUser admin(HttpSession session) {
        return session == null ? null : (SessionUser) session.getAttribute(ADMIN_KEY);
    }

    public static SessionUser guest(HttpSession session) {
        return session == null ? null : (SessionUser) session.getAttribute(GUEST_KEY);
    }

    /**
     * Signs a user in. The session ID is changed first, so an ID that an attacker may
     * have planted before login becomes useless (protection against "session fixation").
     */
    public static void signIn(HttpServletRequest request, SessionUser user) {
        request.getSession(true);
        request.changeSessionId();
        request.getSession().setAttribute(user.isAdmin() ? ADMIN_KEY : GUEST_KEY, user);
    }

    public static void signOutAdmin(HttpSession session) {
        if (session != null) {
            session.removeAttribute(ADMIN_KEY);
        }
    }

    public static void signOutGuest(HttpSession session) {
        if (session != null) {
            session.removeAttribute(GUEST_KEY);
        }
    }
}
