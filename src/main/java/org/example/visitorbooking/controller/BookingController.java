package org.example.visitorbooking.controller;

import org.example.visitorbooking.dto.CalendarEventDto;
import org.example.visitorbooking.dto.GuestHighlightDto;
import org.example.visitorbooking.dto.LeaderboardDto;
import org.example.visitorbooking.model.Booking;
import org.example.visitorbooking.service.BookingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.example.visitorbooking.dto.AdminScoreboardDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;


import java.util.List;

@Controller
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/bookings")
    public String showBookingPage(Model model) {
        if (!model.containsAttribute("booking")) {
            model.addAttribute("booking", new Booking());
        }

        return "bookings";
    }

    @PostMapping("/bookings")
    public String createBooking(
            @ModelAttribute Booking booking,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        try {
            bookingService.createGuestBooking(booking);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Din booking er sendt. Vi tjekker datoerne og vender tilbage til dig."
            );

            return "redirect:/bookings";
        } catch (IllegalArgumentException error) {
            model.addAttribute("errorMessage", error.getMessage());
            model.addAttribute("booking", booking);
            return "bookings";
        }
    }

    @GetMapping("/admin")
    public String showAdminPage(Model model) {
        if (!model.containsAttribute("booking")) {
            model.addAttribute("booking", new Booking());
        }

        model.addAttribute("guestBookings", bookingService.findGuestBookingsSortedByDate());
        model.addAttribute("adminEntries", bookingService.findAdminEntriesSortedByDate());
        model.addAttribute("scoreboard", bookingService.findAdminScoreboard());

        return "admin";
    }

    @PostMapping("/admin/calendar-entry")
    public String createAdminCalendarEntry(@ModelAttribute Booking booking, Model model) {
        try {
            bookingService.createAdminCalendarEntry(booking);
            return "redirect:/admin";
        } catch (IllegalArgumentException error) {
            model.addAttribute("errorMessage", error.getMessage());
            model.addAttribute("booking", booking);
            model.addAttribute("guestBookings", bookingService.findGuestBookingsSortedByDate());
            model.addAttribute("adminEntries", bookingService.findAdminEntriesSortedByDate());
            return "admin";
        }
    }

    @GetMapping("/admin/edit/{id}")
    public String showEditPage(@PathVariable Long id, Model model) {
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
    public String deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return "redirect:/admin";
    }

    @GetMapping("/api/bookings/public")
    @ResponseBody
    public List<CalendarEventDto> getPublicCalendarEvents() {
        return bookingService.findPublicCalendarEvents();
    }

    @GetMapping("/api/bookings/admin")
    @ResponseBody
    public List<CalendarEventDto> getAdminCalendarEvents() {
        return bookingService.findAdminCalendarEvents();
    }

    @GetMapping("/api/bookings/leaderboard")
    @ResponseBody
    public List<LeaderboardDto> getLeaderboard() {
        return bookingService.findGuestLeaderboard();
    }

    @GetMapping("/api/bookings/guest-highlights")
    @ResponseBody
    public GuestHighlightDto getGuestHighlights() {
        return bookingService.findGuestHighlights();
    }

    @GetMapping("/admin/calendar.ics")
    @ResponseBody
    public ResponseEntity<String> downloadCalendar() {
        String calendarContent = bookingService.createIcsCalendar();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=barcelona-bookinger.ics")
                .contentType(MediaType.parseMediaType("text/calendar; charset=utf-8"))
                .body(calendarContent);
    }

}