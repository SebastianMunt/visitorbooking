package org.example.visitorbooking.controller;

import org.example.visitorbooking.dto.CalendarEventDto;
import org.example.visitorbooking.dto.GuestHighlightDto;
import org.example.visitorbooking.dto.LeaderboardDto;
import org.example.visitorbooking.service.BookingStatsService;
import org.example.visitorbooking.service.CalendarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BookingApiController {

    private final CalendarService calendarService;
    private final BookingStatsService bookingStatsService;

    public BookingApiController(
            CalendarService calendarService,
            BookingStatsService bookingStatsService
    ) {
        this.calendarService = calendarService;
        this.bookingStatsService = bookingStatsService;
    }

    @GetMapping("/api/bookings/public")
    public List<CalendarEventDto> getPublicCalendarEvents() {
        return calendarService.findPublicCalendarEvents();
    }

    @GetMapping("/api/bookings/admin")
    public List<CalendarEventDto> getAdminCalendarEvents() {
        return calendarService.findAdminCalendarEvents();
    }

    @GetMapping("/api/bookings/leaderboard")
    public List<LeaderboardDto> getLeaderboard() {
        return bookingStatsService.findGuestLeaderboard();
    }

    @GetMapping("/api/bookings/guest-highlights")
    public GuestHighlightDto getGuestHighlights() {
        return bookingStatsService.findGuestHighlights();
    }
}