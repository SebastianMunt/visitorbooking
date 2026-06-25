package org.example.visitorbooking.service;

import org.example.visitorbooking.dto.AdminScoreboardDto;
import org.example.visitorbooking.dto.GuestHighlightDto;
import org.example.visitorbooking.dto.LeaderboardDto;
import org.example.visitorbooking.model.Booking;
import org.example.visitorbooking.model.BookingType;
import org.example.visitorbooking.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookingStatsService {

    private final BookingRepository bookingRepository;

    public BookingStatsService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
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

    public GuestHighlightDto findGuestHighlights() {
        List<Booking> guestBookings = bookingRepository.findByBookingType(BookingType.GUEST);

        if (guestBookings.isEmpty()) {
            return new GuestHighlightDto(
                    "Ingen har booket det længste besøg endnu.",
                    "Ingen har booket endnu.",
                    "Ingen besøg er planlagt endnu."
            );
        }

        String longestVisitText = guestBookings.stream()
                .max(Comparator.comparingLong(this::calculateBookingDays))
                .map(booking -> booking.getGuestName()
                        + " har det længste besøg med "
                        + calculateBookingDays(booking)
                        + " dage.")
                .orElse("Ingen har booket det længste besøg endnu.");

        String firstBookingText = guestBookings.stream()
                .min(Comparator.comparing(Booking::getId))
                .map(booking -> booking.getGuestName()
                        + " var først til mølle og lavede den første booking.")
                .orElse("Ingen har booket endnu.");

        String firstVisitText = guestBookings.stream()
                .min(Comparator.comparing(Booking::getStartDate))
                .map(booking -> booking.getGuestName()
                        + " bliver den første der kommer forbi.")
                .orElse("Ingen besøg er planlagt endnu.");

        return new GuestHighlightDto(
                longestVisitText,
                firstBookingText,
                firstVisitText
        );
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
                .max(Comparator.comparingLong(this::calculateBookingDays))
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

    private long calculateBookingDays(Booking booking) {
        return ChronoUnit.DAYS.between(
                booking.getStartDate(),
                booking.getEndDate()
        ) + 1;
    }
}