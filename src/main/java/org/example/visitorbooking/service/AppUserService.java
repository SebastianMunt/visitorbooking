package org.example.visitorbooking.service;

import org.example.visitorbooking.dto.ChangePasswordDto;
import org.example.visitorbooking.model.AppUser;
import org.example.visitorbooking.model.Role;
import org.example.visitorbooking.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean adminExists() {
        return appUserRepository.findByUsername("admin").isPresent();
    }

    public void createInitialAdmin(String password, String confirmPassword) {
        if (adminExists()) {
            throw new IllegalArgumentException("Admin-brugeren findes allerede.");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Du skal skrive et kodeord.");
        }

        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Kodeordene matcher ikke.");
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException("Kodeordet skal være mindst 8 tegn.");
        }

        AppUser admin = new AppUser(
                "admin",
                passwordEncoder.encode(password),
                Role.ADMIN
        );

        appUserRepository.save(admin);
    }

    public void changePassword(String username, ChangePasswordDto changePasswordDto) {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Bruger findes ikke."));

        if (!passwordEncoder.matches(changePasswordDto.getCurrentPassword(), appUser.getPassword())) {
            throw new IllegalArgumentException("Nuværende kodeord er forkert.");
        }

        if (!changePasswordDto.getNewPassword().equals(changePasswordDto.getConfirmPassword())) {
            throw new IllegalArgumentException("Det nye kodeord og gentagelsen matcher ikke.");
        }

        if (changePasswordDto.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("Det nye kodeord skal være mindst 8 tegn.");
        }

        appUser.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
        appUserRepository.save(appUser);
    }
}