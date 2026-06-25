package org.example.visitorbooking.service;

import org.example.visitorbooking.model.Booking;
import org.example.visitorbooking.model.BookingType;
import org.example.visitorbooking.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
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

    public Booking findBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kunne ikke finde booking med id: " + id));
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
        validateEventTitle(booking);

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

    public void updateBooking(Long id, Booking updatedBooking) {
        Booking existingBooking = findBookingById(id);

        if (updatedBooking.getBookingType() == null) {
            throw new IllegalArgumentException("Du skal vælge en type.");
        }

        validateDates(updatedBooking, "Slutdato kan ikke være før startdato.");

        if (updatedBooking.getBookingType() == BookingType.GUEST) {
            validateGuestName(updatedBooking);
            validateNoOverlapForUpdate(id, updatedBooking);
        }

        if (updatedBooking.getBookingType() == BookingType.BLOCKED) {
            validateNoOverlapForUpdate(id, updatedBooking);

            if (updatedBooking.getGuestName() == null || updatedBooking.getGuestName().isBlank()) {
                updatedBooking.setGuestName("Blokeret");
            }
        }

        if (updatedBooking.getBookingType() == BookingType.EVENT) {
            validateEventTitle(updatedBooking);
        }

        existingBooking.setBookingType(updatedBooking.getBookingType());
        existingBooking.setGuestName(updatedBooking.getGuestName());
        existingBooking.setStartDate(updatedBooking.getStartDate());
        existingBooking.setEndDate(updatedBooking.getEndDate());
        existingBooking.setComment(updatedBooking.getComment());

        bookingRepository.save(existingBooking);
    }

    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
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

    private void validateEventTitle(Booking booking) {
        if (booking.getGuestName() == null || booking.getGuestName().isBlank()) {
            throw new IllegalArgumentException("Begivenheden skal have en titel.");
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

    private void validateNoOverlapForUpdate(Long currentBookingId, Booking booking) {
        List<BookingType> blockingTypes = List.of(
                BookingType.GUEST,
                BookingType.BLOCKED
        );

        List<Booking> overlappingBookings =
                bookingRepository.findByBookingTypeInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                blockingTypes,
                                booking.getEndDate(),
                                booking.getStartDate()
                        )
                        .stream()
                        .filter(existingBooking -> !existingBooking.getId().equals(currentBookingId))
                        .toList();

        if (!overlappingBookings.isEmpty()) {
            throw new IllegalArgumentException("Datoerne overlapper med en eksisterende booking eller blokering.");
        }
    }
}