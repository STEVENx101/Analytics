package com.fintrex.analytics.Controllers;

import com.fintrex.analytics.Repository.UserRepo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.springframework.http.HttpStatus;

@Controller
public class AuthController {

    @Value("${app.auth.server:https://auth.fintrexfinance.com:2083}")
    private String authServer;

    @Value("${app.postlogin.redirect:/index}")
    private String defaultRedirect;

    @Value("${app.default.role:IT User}")
    private String defaultRole;

    private String absoluteUrl(HttpServletRequest req, String pathAndQuery) {
        String scheme = req.getScheme();
        String host = req.getServerName();
        int port = req.getServerPort();
        String ctx = req.getContextPath();
        boolean isStd = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        if (!pathAndQuery.startsWith("/")) {
            pathAndQuery = "/" + pathAndQuery;
        }
        return scheme + "://" + host + (isStd ? "" : ":" + port) + ctx + pathAndQuery;
    }

    private static String urlEncode(String v) {
        try {
            return java.net.URLEncoder.encode(v, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return v;
        }
    }

    @GetMapping("/extend-session")
    public ResponseEntity<String> extendSession(HttpSession session) {
        // Just touching session keeps it alive
        session.setAttribute("lastAccess", System.currentTimeMillis());
        return ResponseEntity.ok("Session Extended");
    }

    // ========= SSO (Option B) =========
    @GetMapping("/auth")
    public String beginSso(@RequestParam(value = "redirect", required = false) String redirect,
            HttpServletRequest request) {
        String target = (redirect == null || redirect.isBlank()) ? defaultRedirect : redirect;
        String clientLoginWithRedirect = absoluteUrl(request, "/login?redirect=" + urlEncode(target));
        String authLogin = authServer + "/auth/login?client_redirect_uri=" + urlEncode(clientLoginWithRedirect);
        return "redirect:" + authLogin;
        
    }

    @GetMapping("/sso-logout")
    public String ssoLogout(@RequestParam(value = "redirect", required = false) String redirect,
            HttpServletRequest request,
            HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        String backToLogin = absoluteUrl(request, (redirect == null || redirect.isBlank()) ? "/login" : redirect);
        return "redirect:" + authServer + "/auth/sso-logout?client_redirect_uri=" + urlEncode(backToLogin);
    }

    @GetMapping("/logout")
    public void logout(HttpSession session, HttpServletResponse resp) throws Exception {
        if (session != null) {
            session.invalidate();
        }
        resp.sendRedirect("login");
    }
    
    
    @RestController
    @RequestMapping("/api")
    public static class Api {

        private final UserRepo userRepo;

        public Api(UserRepo userRepo) {
            this.userRepo = userRepo;
        }

        public static class UserInfo {

            public String sub;
            public String name;
            public String email;
        }

        @PostMapping("/login-callback")
        public ResponseEntity<?> createLocalSession(@RequestBody UserInfo user, HttpSession session) {
            if (user == null || user.sub == null || user.sub.isBlank()) {
                return ResponseEntity.badRequest().body("invalid user");
            }

            String email = safe(user.email);
            String name = safe(user.name);
            String loginFromEmail = deriveLogin(email);
            String login = !email.isBlank() ? email : (!name.isBlank() ? name : user.sub);

            Map<String, Object> dbUser = null;
            try {

                if (dbUser == null && !email.isBlank()) {
                    dbUser = userRepo.getUserByUsernameAndPassword(email);
                    System.out.println("email: " + email);
                }

                if (dbUser == null && !loginFromEmail.isBlank()) {
                    dbUser = userRepo.getUserByUsernameAndPassword(loginFromEmail);
                    System.out.println("loginFromEmail: " + loginFromEmail);
                }

                if (dbUser == null && !name.isBlank()) {
                    dbUser = userRepo.getUserByUsernameAndPassword(name);
                    System.out.println("name: " + name);
                }

            } catch (Exception e) {
                e.printStackTrace(); // don’t ignore silently
            }

            if (dbUser == null) {
                session.invalidate();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("User is not authorized for this application");
            }

            session.setAttribute("user", login);
            session.setAttribute("email", email);
            session.setAttribute("jwtSub", user.sub);

            session.setAttribute("type", dbUser.get("user_type_id"));
            session.setAttribute("eid", dbUser.get("employee_id"));
            session.setAttribute("uid", dbUser.get("user_id"));
            session.setAttribute("uid_original", dbUser.get("user_id"));
            session.setAttribute("username", nvl(dbUser.get("callname"), name.isBlank() ? login : name));
            session.setAttribute("role", dbUser.get("role"));
            session.setAttribute("department", dbUser.get("department"));
            session.setAttribute("tcmt", dbUser.get("commentToggle"));
            session.setAttribute("internal", dbUser.get("internalcomment"));
            session.setAttribute("assign", dbUser.get("assign"));
            session.setAttribute("statChange", dbUser.get("status_change"));
            session.setAttribute("Tname", nvl(dbUser.get("Tname"), "IT User"));
            session.setAttribute("appSup", dbUser.get("app_support"));

            if (dbUser.containsKey("role2")) {
                session.setAttribute("role2", dbUser.get("role2"));
            }

            return ResponseEntity.ok().build();
        }

        @GetMapping("/me")
        public ResponseEntity<?> localMe(HttpSession session) {
            Object user = session.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(401).body("Not logged in");
            }
            return ResponseEntity.ok(user);
        }

        @GetMapping("/logout")
        public ResponseEntity<?> apiLogout(HttpSession session,
                HttpServletRequest req,
                HttpServletResponse resp) {
            if (session != null) {
                session.invalidate();
            }

            // Expire JSESSIONID for "/" and context path
            killCookie(resp, "JSESSIONID", "/", req.isSecure());
            String ctx = req.getContextPath();
            if (ctx != null && !ctx.isBlank() && !"/".equals(ctx)) {
                killCookie(resp, "JSESSIONID", ctx, req.isSecure());
            }
            return ResponseEntity.ok().build();
        }

        // ---------- helpers ----------
        private static String safe(String v) {
            return v == null ? "" : v.trim();
        }

        private static String nvl(Object v, String def) {
            return v == null ? def : String.valueOf(v);
        }
        
        

        /**
         * derive "janudav" from "Janudav@fintrexfinance.com" or keep as
         * lower-case if already short
         */
        private static String deriveLogin(String v) {
            if (v == null) {
                return "";
            }
            v = v.trim();
            int at = v.indexOf('@');
            if (at > 0) {
                v = v.substring(0, at);
            }
            return v.toLowerCase();
        }

        private static void killCookie(HttpServletResponse resp, String name, String path, boolean secure) {
            Cookie c = new Cookie(name, "");
            c.setHttpOnly(true);
            c.setSecure(secure);
            c.setPath(path);
            c.setMaxAge(0);
            resp.addCookie(c);
        }
    }
}
