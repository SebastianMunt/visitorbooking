package org.example.visitorbooking.controller;

import org.example.visitorbooking.service.AppUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class SetupController {

    private final AppUserService appUserService;

    public SetupController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping("/setup")
    public String setupPage(Model model) {
        if (appUserService.adminExists()) {
            return "redirect:/login";
        }

        return "setup";
    }

    @PostMapping("/setup")
    public String createAdmin(
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model
    ) {
        try {
            appUserService.createInitialAdmin(password, confirmPassword);
            return "redirect:/login";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return "setup";
        }
    }
}