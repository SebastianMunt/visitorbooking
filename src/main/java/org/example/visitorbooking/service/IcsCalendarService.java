package org.example.visitorbooking.service;

import org.example.visitorbooking.model.Booking;
import org.example.visitorbooking.model.BookingType;
import org.example.visitorbooking.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class IcsCalendarService {

    private final BookingRepository bookingRepository;

    public IcsCalendarService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public String createIcsCalendar() {
        List<Booking> bookings = bookingRepository.findAll();

        StringBuilder ics = new StringBuilder();

        ics.append("BEGIN:VCALENDAR\r\n");
        ics.append("VERSION:2.0\r\n");
        ics.append("PRODID:-//Visitorbooking//Barcelona Calendar//DA\r\n");
        ics.append("CALSCALE:GREGORIAN\r\n");

        for (Booking booking : bookings) {
            String title = getIcsTitle(booking);
            String description = booking.getComment() == null ? "" : booking.getComment();

            ics.append("BEGIN:VEVENT\r\n");
            ics.append("UID:booking-").append(booking.getId()).append("@visitorbooking\r\n");
            ics.append("DTSTAMP:").append(formatIcsTimestamp()).append("\r\n");
            ics.append("DTSTART;VALUE=DATE:").append(formatIcsDate(booking.getStartDate())).append("\r\n");
            ics.append("DTEND;VALUE=DATE:").append(formatIcsDate(booking.getEndDate().plusDays(1))).append("\r\n");
            ics.append("SUMMARY:").append(escapeIcsText(title)).append("\r\n");

            if (!description.isBlank()) {
                ics.append("DESCRIPTION:").append(escapeIcsText(description)).append("\r\n");
            }

            ics.append("END:VEVENT\r\n");
        }

        ics.append("END:VCALENDAR\r\n");

        return ics.toString();
    }

    private String getIcsTitle(Booking booking) {
        if (booking.getBookingType() == BookingType.EVENT) {
            return booking.getGuestName();
        }

        if (booking.getBookingType() == BookingType.BLOCKED) {
            return "Blokeret";
        }

        return "Besøg: " + booking.getGuestName();
    }

    private String formatIcsDate(LocalDate date) {
        return date.toString().replace("-", "");
    }

    private String formatIcsTimestamp() {
        return ZonedDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
    }

    private String escapeIcsText(String text) {
        return text
                .replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(";", "\\;")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}