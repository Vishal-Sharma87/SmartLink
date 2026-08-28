package com.spring.springboot.smartlink.controllers;

import com.spring.springboot.smartlink.enums.ReportCause;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Server-rendered entry points for the small Thymeleaf product UI.
 * Data mutations and reads continue to use the existing REST controllers.
 */
@Controller
public class UiController {

    @GetMapping({"/", "/home"})
    public String home() {
        return "index";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping({"/auth", "/login", "/signup"})
    public String authentication() {
        return "auth";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/analytics")
    public String analytics() {
        return "analytics";
    }

    @GetMapping({"/report-abuse-page", "/report"})
    public String reportAbuse(Model model) {
        model.addAttribute("reportCauses", ReportCause.values());
        return "report-abuse";
    }

    @GetMapping("/signup/verify")
    public String signupVerification(HttpSession session, Model model) {
        Object pendingSignup = session.getAttribute(AuthController.PENDING_SIGNUP_SESSION_KEY);
        if (pendingSignup == null) return "redirect:/auth?mode=signup";
        model.addAttribute("pendingEmail", ((com.spring.springboot.smartlink.dto.requestDtos.SignupInitiationRequestDto) pendingSignup).getEmail());
        return "signup-verify";
    }
}
