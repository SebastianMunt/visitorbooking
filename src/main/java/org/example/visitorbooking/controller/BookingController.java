package org.example.visitorbooking.controller;

import org.example.visitorbooking.dto.CalendarEventDto;
import org.example.visitorbooking.model.Booking;
import org.example.visitorbooking.service.BookingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/bookings")
    public String showBookings(Model model) {
        addBookingAttributes(model, new Booking());
        return "bookings";
    }

    @PostMapping("/bookings")
    public String createBooking(@ModelAttribute Booking booking, Model model) {
        try {
            bookingService.createGuestBooking(booking);
            return "redirect:/bookings";
        } catch (IllegalArgumentException exception) {
            addBookingAttributes(model, booking);
            model.addAttribute("error", exception.getMessage());
            return "bookings";
        }
    }

    @GetMapping("/admin")
    public String adminPage(Model model) {
        addBookingAttributes(model, new Booking());
        return "admin";
    }

    @PostMapping("/admin/block")
    public String blockDates(@ModelAttribute Booking booking, Model model) {
        try {
            bookingService.createBlockedBooking(booking);
            return "redirect:/admin";
        } catch (IllegalArgumentException exception) {
            addBookingAttributes(model, booking);
            model.addAttribute("error", exception.getMessage());
            return "admin";
        }
    }

    @PostMapping("/admin/delete/{id}")
    public String deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return "redirect:/admin";
    }

    @GetMapping("/api/bookings/public")
    @ResponseBody
    public List<CalendarEventDto> getPublicBookingsAsJson() {
        return bookingService.findPublicCalendarEvents();
    }

    @GetMapping("/api/bookings/admin")
    @ResponseBody
    public List<CalendarEventDto> getAdminBookingsAsJson() {
        return bookingService.findAdminCalendarEvents();
    }

    private void addBookingAttributes(Model model, Booking booking) {
        model.addAttribute("bookings", bookingService.findAllBookings());
        model.addAttribute("booking", booking);
    }
}