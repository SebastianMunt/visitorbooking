package org.example.visitorbooking.service;

import org.example.visitorbooking.dto.CalendarEventDto;
import org.example.visitorbooking.dto.LeaderboardDto;
import org.example.visitorbooking.model.Booking;
import org.example.visitorbooking.model.BookingType;
import org.example.visitorbooking.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public List<Booking> findAllBookings() {
        return bookingRepository.findAll();
    }

    public List<Booking> findGuestBookingsSortedByDate() {
        return bookingRepository.findAll()
                .stream()
                .filter(booking -> booking.getBookingType() == BookingType.GUEST)
                .sorted(Comparator.comparing(Booking::getStartDate))
                .toList();
    }

    public List<Booking> findAdminEntriesSortedByDate() {
        return bookingRepository.findAll()
                .stream()
                .filter(booking ->
                        booking.getBookingType() == BookingType.BLOCKED ||
                                booking.getBookingType() == BookingType.EVENT
                )
                .sorted(Comparator.comparing(Booking::getStartDate))
                .toList();
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

    public void createGuestBooking(Booking booking) {
        validateDates(booking, "Afrejsedato kan ikke være før ankomstdato.");
        validateGuestName(booking);
        validateNoOverlap(booking);

        booking.setBookingType(BookingType.GUEST);
        bookingRepository.save(booking);
    }

    public void createBlockedBooking(Booking booking) {
        validateDates(booking, "Slutdato kan ikke være før startdato.");
        validateNoOverlap(booking);

        booking.setGuestName("Blokeret");
        booking.setBookingType(BookingType.BLOCKED);
        bookingRepository.save(booking);
    }

    public void createEventBooking(Booking booking) {
        validateDates(booking, "Slutdato kan ikke være før startdato.");

        if (booking.getGuestName() == null || booking.getGuestName().isBlank()) {
            throw new IllegalArgumentException("Begivenheden skal have en titel.");
        }

        booking.setBookingType(BookingType.EVENT);
        bookingRepository.save(booking);
    }

    public void createAdminCalendarEntry(Booking booking) {
        if (booking.getBookingType() == null) {
            throw new IllegalArgumentException("Du skal vælge en type.");
        }

        if (booking.getBookingType() == BookingType.BLOCKED) {
            createBlockedBooking(booking);
            return;
        }

        if (booking.getBookingType() == BookingType.EVENT) {
            createEventBooking(booking);
            return;
        }

        throw new IllegalArgumentException("Admin kan kun oprette blokeringer eller begivenheder her.");
    }

    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }

    public List<LeaderboardDto> findGuestLeaderboard() {
        return bookingRepository.findByBookingType(BookingType.GUEST)
                .stream()
                .collect(Collectors.groupingBy(
                        Booking::getGuestName,
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .map(entry -> new LeaderboardDto(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(LeaderboardDto::getVisitCount).reversed())
                .limit(5)
                .toList();
    }

    private void validateDates(Booking booking, String errorMessage) {
        if (booking.getStartDate() == null || booking.getEndDate() == null) {
            throw new IllegalArgumentException("Du skal vælge både startdato og slutdato.");
        }

        if (booking.getEndDate().isBefore(booking.getStartDate())) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private void validateGuestName(Booking booking) {
        if (booking.getGuestName() == null || booking.getGuestName().isBlank()) {
            throw new IllegalArgumentException("Du skal skrive dit navn.");
        }
    }

    private void validateNoOverlap(Booking booking) {
        List<BookingType> blockingTypes = List.of(
                BookingType.GUEST,
                BookingType.BLOCKED
        );

        List<Booking> overlappingBookings =
                bookingRepository.findByBookingTypeInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        blockingTypes,
                        booking.getEndDate(),
                        booking.getStartDate()
                );

        if (!overlappingBookings.isEmpty()) {
            throw new IllegalArgumentException("Datoerne overlapper med en eksisterende booking eller blokering.");
        }
    }

    private CalendarEventDto convertToPublicCalendarEvent(Booking booking) {
        if (booking.getBookingType() == BookingType.EVENT) {
            return new CalendarEventDto(
                    booking.getGuestName(),
                    booking.getStartDate().toString(),
                    booking.getEndDate().plusDays(1).toString(),
                    "#28a745"
            );
        }

        if (booking.getBookingType() == BookingType.BLOCKED) {
            return new CalendarEventDto(
                    "Blokeret",
                    booking.getStartDate().toString(),
                    booking.getEndDate().plusDays(1).toString(),
                    "#777777"
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
        if (booking.getBookingType() == BookingType.EVENT) {
            return new CalendarEventDto(
                    "Event: " + booking.getGuestName(),
                    booking.getStartDate().toString(),
                    booking.getEndDate().plusDays(1).toString(),
                    "#28a745"
            );
        }

        if (booking.getBookingType() == BookingType.BLOCKED) {
            return new CalendarEventDto(
                    "Blokeret",
                    booking.getStartDate().toString(),
                    booking.getEndDate().plusDays(1).toString(),
                    "#777777"
            );
        }

        return new CalendarEventDto(
                booking.getGuestName(),
                booking.getStartDate().toString(),
                booking.getEndDate().plusDays(1).toString(),
                "#d9534f"
        );
    }
}