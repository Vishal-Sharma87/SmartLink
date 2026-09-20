package com.spring.springboot.smartlink.controllers;

import com.spring.springboot.smartlink.user.authentication.dtos.SignupInitiateDto;
import com.spring.springboot.smartlink.configurations.ApplicationConfigs;
import com.spring.springboot.smartlink.enums.ReportCause;
import com.spring.springboot.smartlink.user.authentication.services.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Server-rendered entry points for the small Thymeleaf product UI.
 * Data mutations and reads continue to use the existing REST controllers.
 */
@Controller
public class UiController {

    private final ApplicationConfigs applicationConfigs;

    public UiController(ApplicationConfigs applicationConfigs) {
        this.applicationConfigs = applicationConfigs;
    }

    @ModelAttribute
    public void addFrontendConfiguration(Model model) {
        model.addAttribute("companyDomain", applicationConfigs.domain());
        model.addAttribute("companyEndpoint", applicationConfigs.shortUrlPrefix());
    }

    @GetMapping({ "/", "/home" })
    public String home() {
        return "index";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/auth")
    public String authentication() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/shorten")
    public String shorten() {
        return "shorten";
    }

    @GetMapping("/links")
    public String links() {
        return "links";
    }

    @GetMapping("/analytics")
    public String analytics() {
        return "analytics";
    }

    @GetMapping({ "/report-abuse-page", "/report" })
    public String reportAbuse(Model model) {
        model.addAttribute("reportCauses", ReportCause.values());
        return "report-abuse";
    }

    @GetMapping("/signup/verify")
    public String signupVerification(HttpSession session, Model model) {
        Object pendingSignup = session.getAttribute(AuthService.PENDING_SIGNUP_SESSION_KEY);
        if (pendingSignup == null)
            return "redirect:/signup";
        model.addAttribute("pendingEmail", ((SignupInitiateDto) pendingSignup).getEmail());
        return "signup-verify";
    }
}
