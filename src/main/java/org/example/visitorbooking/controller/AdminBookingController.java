package org.example.visitorbooking.controller;

import org.example.visitorbooking.model.Booking;
import org.example.visitorbooking.service.BookingService;
import org.example.visitorbooking.service.BookingStatsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.example.visitorbooking.service.BarcaCalendarSyncService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminBookingController {

    private final BookingService bookingService;
    private final BookingStatsService bookingStatsService;
    private final BarcaCalendarSyncService barcaCalendarSyncService;

    public AdminBookingController(
            BookingService bookingService,
            BookingStatsService bookingStatsService,
            BarcaCalendarSyncService barcaCalendarSyncService
    ) {
        this.bookingService = bookingService;
        this.bookingStatsService = bookingStatsService;
        this.barcaCalendarSyncService = barcaCalendarSyncService;
    }

    @GetMapping("/admin")
    public String showAdminPage(Model model) {
        addAdminPageData(model);

        if (!model.containsAttribute("booking")) {
            model.addAttribute("booking", new Booking());
        }

        return "admin";
    }

    @PostMapping("/admin/calendar-entry")
    public String createAdminCalendarEntry(
            @ModelAttribute Booking booking,
            Model model
    ) {
        try {
            bookingService.createAdminCalendarEntry(booking);
            return "redirect:/admin";
        } catch (IllegalArgumentException error) {
            model.addAttribute("errorMessage", error.getMessage());
            model.addAttribute("booking", booking);
            addAdminPageData(model);

            return "admin";
        }
    }

    @GetMapping("/admin/edit/{id}")
    public String showEditPage(
            @PathVariable Long id,
            Model model
    ) {
        model.addAttribute("booking", bookingService.findBookingById(id));
        return "edit-booking";
    }

    @PostMapping("/admin/edit/{id}")
    public String updateBooking(
            @PathVariable Long id,
            @ModelAttribute Booking booking,
            Model model
    ) {
        try {
            bookingService.updateBooking(id, booking);
            return "redirect:/admin";
        } catch (IllegalArgumentException error) {
            model.addAttribute("errorMessage", error.getMessage());
            model.addAttribute("booking", booking);
            return "edit-booking";
        }
    }

    @PostMapping("/admin/delete/{id}")
    public String deleteBooking(
            @PathVariable Long id,
            @RequestParam(defaultValue = "top") String section
    ) {
        bookingService.deleteBooking(id);

        if (section.equals("bookings")) {
            return "redirect:/admin#bookings-section";
        }

        if (section.equals("adminEntries")) {
            return "redirect:/admin#admin-entries-section";
        }

        return "redirect:/admin";
    }

    private void addAdminPageData(Model model) {
        model.addAttribute("guestBookings", bookingService.findGuestBookingsSortedByDate());
        model.addAttribute("adminEntries", bookingService.findAdminEntriesSortedByDate());
        model.addAttribute("scoreboard", bookingStatsService.findAdminScoreboard());
    }

    @PostMapping("/admin/sync-barca")
    public String syncBarcaMatches(RedirectAttributes redirectAttributes) {
        try {
            int createdCount = barcaCalendarSyncService.syncHomeMatches();

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Barça-kampe synkroniseret. Nye kampe oprettet: " + createdCount
            );
        } catch (IllegalStateException error) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    error.getMessage()
            );
        }

        return "redirect:/admin";
    }

}