package org.example.visitorbooking.repository;

import org.example.visitorbooking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDate endDate,
            LocalDate startDate
    );
}