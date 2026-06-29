package org.example.visitorbooking.service;

import org.example.visitorbooking.model.Booking;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String mailTo;

    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public EmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.to}") String mailTo
    ) {
        this.mailSender = mailSender;
        this.mailTo = mailTo;
    }

    public void sendNewBookingEmail(Booking booking) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(mailTo);
        message.setSubject("Ny booking på hossebogemma.dk");

        message.setText("""
                Der er lavet en ny booking på hossebogemma.dk.

                Navn:
                %s

                Datoer:
                %s til %s

                Kommentar:
                %s

                Gå til admin:
                https://hossebogemma.dk/admin
                """.formatted(
                booking.getGuestName(),
                booking.getStartDate().format(dateFormatter),
                booking.getEndDate().format(dateFormatter),
                getCommentText(booking)
        ));

        mailSender.send(message);
    }

    private String getCommentText(Booking booking) {
        if (booking.getComment() == null || booking.getComment().isBlank()) {
            return "Ingen kommentar";
        }

        return booking.getComment();
    }
}