package org.example.visitorbooking.controller;

import org.example.visitorbooking.dto.ChangePasswordDto;
import org.example.visitorbooking.service.AppUserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AdminController {

    private final AppUserService appUserService;

    public AdminController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping("/admin/change-password")
    public String changePasswordPage(Model model) {
        model.addAttribute("changePasswordDto", new ChangePasswordDto());
        return "change-password";
    }

    @PostMapping("/admin/change-password")
    public String changePassword(
            @ModelAttribute ChangePasswordDto changePasswordDto,
            Authentication authentication,
            Model model
    ) {
        try {
            appUserService.changePassword(authentication.getName(), changePasswordDto);
            model.addAttribute("changePasswordDto", new ChangePasswordDto());
            model.addAttribute("success", "Kodeordet er ændret.");
            return "change-password";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("changePasswordDto", changePasswordDto);
            model.addAttribute("error", exception.getMessage());
            return "change-password";
        }
    }
}