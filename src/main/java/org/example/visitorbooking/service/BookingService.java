package org.example.visitorbooking.service;

import org.example.visitorbooking.dto.CalendarEventDto;
import org.example.visitorbooking.dto.LeaderboardDto;
import org.example.visitorbooking.model.Booking;
import org.example.visitorbooking.model.BookingType;
import org.example.visitorbooking.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.example.visitorbooking.dto.GuestHighlightDto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import org.example.visitorbooking.dto.AdminScoreboardDto;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

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

    public GuestHighlightDto findGuestHighlights() {
        List<Booking> guestBookings = bookingRepository.findByBookingType(BookingType.GUEST);

        if (guestBookings.isEmpty()) {
            return new GuestHighlightDto(
                    "Ingen har booket det længste besøg endnu.",
                    "Ingen har booket endnu.",
                    "Ingen besøg er planlagt endnu."
            );
        }

        Optional<Booking> longestVisit = guestBookings.stream()
                .max(Comparator.comparingLong(this::calculateBookingDays));

        Optional<Booking> firstBooking = guestBookings.stream()
                .min(Comparator.comparing(Booking::getId));

        Optional<Booking> firstVisit = guestBookings.stream()
                .min(Comparator.comparing(Booking::getStartDate));

        String longestVisitText = longestVisit
                .map(booking -> booking.getGuestName()
                        + " har det længste besøg med "
                        + calculateBookingDays(booking)
                        + " dage.")
                .orElse("Ingen har booket det længste besøg endnu.");

        String firstBookingText = firstBooking
                .map(booking -> booking.getGuestName()
                        + " var først til mølle og lavede den første booking.")
                .orElse("Ingen har booket endnu.");

        String firstVisitText = firstVisit
                .map(booking -> booking.getGuestName()
                        + " bliver den første der kommer forbi ")
                .orElse("Ingen besøg er planlagt endnu.");

        return new GuestHighlightDto(
                longestVisitText,
                firstBookingText,
                firstVisitText
        );
    }

    private long calculateBookingDays(Booking booking) {
        return ChronoUnit.DAYS.between(
                booking.getStartDate(),
                booking.getEndDate()
        ) + 1;
    }

    private String formatDanishDate(java.time.LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "d. MMMM yyyy",
                Locale.forLanguageTag("da-DK")
        );

        return date.format(formatter);
    }

    public Booking findBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kunne ikke finde booking med id: " + id));
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
            if (updatedBooking.getGuestName() == null || updatedBooking.getGuestName().isBlank()) {
                throw new IllegalArgumentException("Begivenheden skal have en titel.");
            }
        }

        existingBooking.setBookingType(updatedBooking.getBookingType());
        existingBooking.setGuestName(updatedBooking.getGuestName());
        existingBooking.setStartDate(updatedBooking.getStartDate());
        existingBooking.setEndDate(updatedBooking.getEndDate());
        existingBooking.setComment(updatedBooking.getComment());

        bookingRepository.save(existingBooking);
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

    public AdminScoreboardDto findAdminScoreboard() {
        List<Booking> guestBookings = bookingRepository.findByBookingType(BookingType.GUEST);

        long bookingCount = guestBookings.size();

        long bookedDays = guestBookings.stream()
                .mapToLong(this::calculateBookingDays)
                .sum();

        Set<String> uniqueGuests = guestBookings.stream()
                .map(Booking::getGuestName)
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.trim().toLowerCase())
                .collect(Collectors.toSet());

        long guestCount = uniqueGuests.size();

        String longestVisitText = guestBookings.stream()
                .max((a, b) -> Long.compare(calculateBookingDays(a), calculateBookingDays(b)))
                .map(booking -> booking.getGuestName()
                        + " har det længste besøg på "
                        + calculateBookingDays(booking)
                        + " dage")
                .orElse("Der er ingen bookinger endnu");

        return new AdminScoreboardDto(
                bookingCount,
                bookedDays,
                guestCount,
                longestVisitText
        );
    }

    public String createIcsCalendar() {
        List<Booking> bookings = findAllBookings();

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
        return java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
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