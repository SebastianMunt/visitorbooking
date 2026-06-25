package org.example.visitorbooking.controller;

import org.example.visitorbooking.model.Booking;
import org.example.visitorbooking.service.BookingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PublicBookingController {

    private final BookingService bookingService;

    public PublicBookingController(BookingService bookingService) {
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
}