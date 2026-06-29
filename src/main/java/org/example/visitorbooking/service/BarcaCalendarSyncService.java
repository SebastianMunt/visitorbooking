package org.example.visitorbooking.service;

import org.example.visitorbooking.model.Booking;
import org.example.visitorbooking.model.BookingType;
import org.example.visitorbooking.repository.BookingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class BarcaCalendarSyncService {

    private static final String BARCA_CALENDAR_URL =
            "https://pub.fotmob.com/prod/pub/api/v2/calendar/team/8634.ics";

    private final BookingRepository bookingRepository;
    private final HttpClient httpClient;

    public BarcaCalendarSyncService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Copenhagen")
    public void syncHomeMatchesAutomatically() {
        int createdCount = syncHomeMatches();

        if (createdCount > 0) {
            System.out.println("Automatisk Barça-sync oprettede " + createdCount + " nye kampe.");
        }
    }

    @Transactional
    public int syncHomeMatches() {
        String calendarContent = fetchCalendarContent();
        List<BarcaMatch> matches = parseMatches(calendarContent);

        int createdCount = 0;

        for (BarcaMatch match : matches) {
            if (!isFutureMatch(match)) {
                continue;
            }

            if (!isHomeMatch(match)) {
                continue;
            }

            boolean alreadyExists =
                    bookingRepository.existsByGuestNameAndStartDateAndBookingType(
                            match.cleanSummary(),
                            match.date(),
                            BookingType.FOOTBALL
                    );

            if (alreadyExists) {
                continue;
            }

            Booking booking = new Booking();
            booking.setGuestName(match.cleanSummary());
            booking.setStartDate(match.date());
            booking.setEndDate(match.date());
            booking.setBookingType(BookingType.FOOTBALL);
            booking.setComment(createComment(match));

            bookingRepository.save(booking);
            createdCount++;
        }

        return createdCount;
    }

    private String fetchCalendarContent() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BARCA_CALENDAR_URL))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Kunne ikke hente Barça-kalenderen. Status: " + response.statusCode());
            }

            return response.body();
        } catch (IOException error) {
            throw new IllegalStateException("Kunne ikke hente Barça-kalenderen.", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Synkronisering af Barça-kalenderen blev afbrudt.", error);
        }
    }

    private List<BarcaMatch> parseMatches(String calendarContent) {
        List<String> lines = unfoldIcsLines(calendarContent);
        List<BarcaMatch> matches = new ArrayList<>();

        boolean insideEvent = false;
        boolean insideAlarm = false;

        String summary = null;
        String location = null;
        String description = null;
        String url = null;
        LocalDateTime startDateTime = null;

        for (String line : lines) {
            if (line.equals("BEGIN:VEVENT")) {
                insideEvent = true;
                insideAlarm = false;

                summary = null;
                location = null;
                description = null;
                url = null;
                startDateTime = null;

                continue;
            }

            if (line.equals("END:VEVENT")) {
                if (insideEvent && summary != null && startDateTime != null) {
                    matches.add(new BarcaMatch(
                            cleanMatchTitle(cleanIcsText(summary)),
                            startDateTime,
                            cleanIcsText(location),
                            cleanIcsText(description),
                            cleanIcsText(url)
                    ));
                }

                insideEvent = false;
                insideAlarm = false;
                continue;
            }

            if (!insideEvent) {
                continue;
            }

            if (line.equals("BEGIN:VALARM")) {
                insideAlarm = true;
                continue;
            }

            if (line.equals("END:VALARM")) {
                insideAlarm = false;
                continue;
            }

            if (insideAlarm) {
                continue;
            }

            if (line.startsWith("SUMMARY:")) {
                summary = line.substring("SUMMARY:".length());
                continue;
            }

            if (line.startsWith("LOCATION:")) {
                location = line.substring("LOCATION:".length());
                continue;
            }

            if (line.startsWith("DESCRIPTION:")) {
                description = line.substring("DESCRIPTION:".length());
                continue;
            }

            if (line.startsWith("URL:")) {
                url = line.substring("URL:".length());
                continue;
            }

            if (line.startsWith("DTSTART")) {
                startDateTime = parseIcsDateTime(line);
            }
        }

        return matches;
    }

    private List<String> unfoldIcsLines(String calendarContent) {
        String[] rawLines = calendarContent.replace("\r\n", "\n").split("\n");
        List<String> lines = new ArrayList<>();

        for (String rawLine : rawLines) {
            if ((rawLine.startsWith(" ") || rawLine.startsWith("\t")) && !lines.isEmpty()) {
                int lastIndex = lines.size() - 1;
                lines.set(lastIndex, lines.get(lastIndex) + rawLine.trim());
            } else {
                lines.add(rawLine.trim());
            }
        }

        return lines;
    }

    private LocalDateTime parseIcsDateTime(String line) {
        String value = line.substring(line.indexOf(":") + 1);

        if (value.length() == 8) {
            return LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE).atStartOfDay();
        }

        if (value.endsWith("Z")) {
            return OffsetDateTime.parse(
                            value,
                            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX")
                    )
                    .atZoneSameInstant(ZoneId.of("Europe/Copenhagen"))
                    .toLocalDateTime();
        }

        if (value.contains("T")) {
            return LocalDateTime.parse(
                    value,
                    DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
            );
        }

        return LocalDate.parse(value.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE).atStartOfDay();
    }

    private boolean isFutureMatch(BarcaMatch match) {
        return !match.date().isBefore(LocalDate.now());
    }

    private boolean isHomeMatch(BarcaMatch match) {
        String summary = match.cleanSummary().toLowerCase();
        String location = match.location() == null ? "" : match.location().toLowerCase();

        boolean titleLooksLikeHomeMatch =
                summary.startsWith("barcelona - ") ||
                        summary.startsWith("fc barcelona - ") ||
                        summary.startsWith("barça - ") ||
                        summary.startsWith("barca - ");

        boolean locationLooksLikeHome =
                location.contains("barcelona") ||
                        location.contains("spotify camp nou") ||
                        location.contains("camp nou") ||
                        location.contains("estadi olímpic") ||
                        location.contains("estadi olimpic") ||
                        location.contains("montjuïc") ||
                        location.contains("montjuic") ||
                        location.contains("estadi johan cruyff");

        return titleLooksLikeHomeMatch || locationLooksLikeHome;
    }

    private String createComment(BarcaMatch match) {
        StringBuilder comment = new StringBuilder();

        comment.append(match.cleanSummary());

        if (match.startDateTime() != null) {
            comment.append("\nTidspunkt: ")
                    .append(match.startDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }

        if (match.location() != null && !match.location().isBlank()) {
            comment.append("\nSted: ")
                    .append(match.location());
        }

        String competition = extractCompetition(match.description());

        if (competition != null && !competition.isBlank()) {
            comment.append("\nTurnering: ")
                    .append(competition);
        }

        return comment.toString();
    }

    private String extractCompetition(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        String[] parts = description.split("\n");

        if (parts.length < 2) {
            return null;
        }

        return parts[1].trim();
    }

    private String cleanMatchTitle(String title) {
        if (title == null) {
            return null;
        }

        return title
                .replace("⚽️", "")
                .replace("⚽", "")
                .replaceAll("\\s+\\([^)]*\\)$", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String cleanIcsText(String text) {
        if (text == null) {
            return null;
        }

        return text
                .replace("\\n", "\n")
                .replace("\\,", ",")
                .replace("\\;", ";")
                .replace("\\\\", "\\")
                .trim();
    }

    private record BarcaMatch(
            String cleanSummary,
            LocalDateTime startDateTime,
            String location,
            String description,
            String url
    ) {
        private LocalDate date() {
            return startDateTime.toLocalDate();
        }
    }
}