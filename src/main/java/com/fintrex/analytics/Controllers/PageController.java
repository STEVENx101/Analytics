package com.fintrex.analytics.Controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    /* ===== Public entry ===== */
    @GetMapping("/")
    public String root(HttpSession session) {

        Object user = session.getAttribute("user");
        Object role = session.getAttribute("role2");
        Object type = session.getAttribute("Tname");

        if (user != null && "CBS".equals(role)) {
            return "redirect:/cbsdash";
        }

        if (user != null && "HR User".equals(type)) {
            return "redirect:/hrDash";
        }

        if (user != null) {
            return "redirect:/index";
        }

        return "redirect:/login";
    }

    /* ===== Auth pages (views only; no auth logic here) ===== */
    @GetMapping("/login")
    public String login(HttpSession session) {
        if (session.getAttribute("user") != null) {
            return "redirect:/index";
        }
        return "login";
    }

    // If you use an SSO front-end callback page, keep this
    @GetMapping("/login-callback")
    public String loginCallback() {
        return "login-callback";
    }

    /* ===== Protected home ===== */
    @GetMapping("/index")
    public String indexPage(HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        return "index";
    }

    
}
