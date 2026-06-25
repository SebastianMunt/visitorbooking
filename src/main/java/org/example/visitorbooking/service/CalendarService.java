package org.example.visitorbooking.service;

import org.example.visitorbooking.dto.CalendarEventDto;
import org.example.visitorbooking.model.Booking;
import org.example.visitorbooking.model.BookingType;
import org.example.visitorbooking.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class CalendarService {

    private final BookingRepository bookingRepository;

    public CalendarService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public List<CalendarEventDto> findPublicCalendarEvents() {
        return bookingRepository.findAll()
                .stream()
                .map(this::convertToPublicCalendarEvent)
                .toList();
    }

    public List<CalendarEventDto> findAdminCalendarEvents() {
        return bookingRepository.findAll()
                .stream()
                .map(this::convertToAdminCalendarEvent)
                .toList();
    }

    private CalendarEventDto convertToPublicCalendarEvent(Booking booking) {
        if (booking.getBookingType() == BookingType.EVENT) {
            return new CalendarEventDto(
                    booking.getGuestName(),
                    booking.getStartDate().toString(),
                    booking.getEndDate().plusDays(1).toString(),
                    "#28a745",
                    "Event",
                    booking.getComment(),
                    calculateBookingDays(booking)
            );
        }

        if (booking.getBookingType() == BookingType.BLOCKED) {
            return new CalendarEventDto(
                    "Blokeret",
                    booking.getStartDate().toString(),
                    booking.getEndDate().plusDays(1).toString(),
                    "#777777",
                    "Blokering",
                    booking.getComment(),
                    calculateBookingDays(booking)
            );
        }

        return new CalendarEventDto(
                "Optaget",
                booking.getStartDate().toString(),
                booking.getEndDate().plusDays(1).toString(),
                "#d9534f"
        );
    }

    private CalendarEventDto convertToAdminCalendarEvent(Booking booking) {
        String title;
        String color;
        String typeText;

        if (booking.getBookingType() == BookingType.EVENT) {
            title = booking.getGuestName();
            color = "#28a745";
            typeText = "Event";
        } else if (booking.getBookingType() == BookingType.BLOCKED) {
            title = "Blokeret";
            color = "#777777";
            typeText = "Blokering";
        } else {
            title = booking.getGuestName();
            color = "#d9534f";
            typeText = "Booking";
        }

        return new CalendarEventDto(
                title,
                booking.getStartDate().toString(),
                booking.getEndDate().plusDays(1).toString(),
                color,
                typeText,
                booking.getComment(),
                calculateBookingDays(booking)
        );
    }

    private long calculateBookingDays(Booking booking) {
        return ChronoUnit.DAYS.between(
                booking.getStartDate(),
                booking.getEndDate()
        ) + 1;
    }
}