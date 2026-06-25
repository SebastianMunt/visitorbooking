package org.example.visitorbooking.service;

import org.example.visitorbooking.dto.CalendarEventDto;
import org.example.visitorbooking.model.Booking;
import org.example.visitorbooking.model.BookingType;
import org.example.visitorbooking.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public List<Booking> findAllBookings() {
        return bookingRepository.findAll();
    }

    public List<CalendarEventDto> findPublicCalendarEvents() {
        return bookingRepository.findAll()
                .stream()
                .map(this::convertToPublicCalendarEventDto)
                .toList();
    }

    public List<CalendarEventDto> findAdminCalendarEvents() {
        return bookingRepository.findAll()
                .stream()
                .map(this::convertToAdminCalendarEventDto)
                .toList();
    }

    public void createGuestBooking(Booking booking) {
        validateDates(booking, "Afrejsedato kan ikke være før ankomstdato.");
        validateNoOverlap(booking, "De valgte datoer er allerede booket eller blokeret.");

        booking.setBookingType(BookingType.GUEST);
        bookingRepository.save(booking);
    }

    public void createBlockedBooking(Booking booking) {
        validateDates(booking, "Slutdato kan ikke være før startdato.");
        validateNoOverlap(booking, "Datoerne overlapper med en eksisterende booking eller blokering.");

        booking.setGuestName("Blokeret");
        booking.setBookingType(BookingType.BLOCKED);
        bookingRepository.save(booking);
    }

    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }

    private CalendarEventDto convertToPublicCalendarEventDto(Booking booking) {
        String title = booking.getBookingType() == BookingType.BLOCKED
                ? "Blokeret"
                : "Optaget";

        String color = booking.getBookingType() == BookingType.BLOCKED
                ? "#777777"
                : "#d9534f";

        LocalDate fullCalendarEndDate = booking.getEndDate().plusDays(1);

        return new CalendarEventDto(
                title,
                booking.getStartDate().toString(),
                fullCalendarEndDate.toString(),
                color
        );
    }

    private CalendarEventDto convertToAdminCalendarEventDto(Booking booking) {
        String title = booking.getBookingType() == BookingType.BLOCKED
                ? "Blokeret"
                : booking.getGuestName();

        String color = booking.getBookingType() == BookingType.BLOCKED
                ? "#777777"
                : "#d9534f";

        LocalDate fullCalendarEndDate = booking.getEndDate().plusDays(1);

        return new CalendarEventDto(
                title,
                booking.getStartDate().toString(),
                fullCalendarEndDate.toString(),
                color
        );
    }

    private void validateDates(Booking booking, String errorMessage) {
        if (booking.getStartDate() == null || booking.getEndDate() == null) {
            throw new IllegalArgumentException("Du skal vælge både startdato og slutdato.");
        }

        if (booking.getEndDate().isBefore(booking.getStartDate())) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private void validateNoOverlap(Booking booking, String errorMessage) {
        List<Booking> overlappingBookings =
                bookingRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        booking.getEndDate(),
                        booking.getStartDate()
                );

        if (!overlappingBookings.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}