package org.example.visitorbooking.dto;

public class AdminScoreboardDto {

    private final long bookingCount;
    private final long bookedDays;
    private final long guestCount;
    private final String longestVisitText;

    public AdminScoreboardDto(
            long bookingCount,
            long bookedDays,
            long guestCount,
            String longestVisitText
    ) {
        this.bookingCount = bookingCount;
        this.bookedDays = bookedDays;
        this.guestCount = guestCount;
        this.longestVisitText = longestVisitText;
    }

    public long getBookingCount() {
        return bookingCount;
    }

    public long getBookedDays() {
        return bookedDays;
    }

    public long getGuestCount() {
        return guestCount;
    }

    public String getLongestVisitText() {
        return longestVisitText;
    }
}