package org.example.visitorbooking.repository;

import org.example.visitorbooking.model.Booking;
import org.example.visitorbooking.model.BookingType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.time.LocalDate;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDate endDate,
            LocalDate startDate
    );

    List<Booking> findByBookingTypeInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Collection<BookingType> bookingTypes,
            LocalDate endDate,
            LocalDate startDate
    );

    List<Booking> findByBookingType(BookingType bookingType);

    boolean existsByGuestNameAndStartDateAndBookingType(
            String guestName,
            LocalDate startDate,
            BookingType bookingType
    );

}