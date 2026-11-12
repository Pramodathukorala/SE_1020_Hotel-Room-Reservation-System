package com.hotel.reservation.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Runs BEFORE every controller method (see WebConfig) and protects the private pages:
 * <ul>
 *   <li>/admin/** needs a signed-in administrator (except the admin login page itself),</li>
 *   <li>/my-bookings/** and /account/** need a signed-in guest.</li>
 * </ul>
 * Anyone else is redirected to the matching login page, which sends them back afterwards.
 */
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (path.startsWith("/admin") && !path.startsWith("/admin/login")) {
            if (CurrentUser.admin(request.getSession(false)) == null) {
                response.sendRedirect(loginUrl(request, "/admin/login", path));
                return false;
            }
        } else if (path.startsWith("/my-bookings") || path.startsWith("/account")) {
            if (CurrentUser.guest(request.getSession(false)) == null) {
                response.sendRedirect(loginUrl(request, "/login", path));
                return false;
            }
        }
        return true;
    }

    /** Only GET pages are remembered as the place to return to after logging in. */
    private static String loginUrl(HttpServletRequest request, String loginPage, String path) {
        UriComponentsBuilder url = UriComponentsBuilder.fromPath(request.getContextPath() + loginPage);
        if ("GET".equals(request.getMethod())) {
            url.queryParam("next", path);
        }
        return url.encode().build().toUriString();
    }
}
