package org.example.visitorbooking.controller;

import org.example.visitorbooking.service.IcsCalendarService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CalendarExportController {

    private final IcsCalendarService icsCalendarService;

    public CalendarExportController(IcsCalendarService icsCalendarService) {
        this.icsCalendarService = icsCalendarService;
    }

    @GetMapping("/admin/calendar.ics")
    public ResponseEntity<String> downloadCalendar() {
        String calendarContent = icsCalendarService.createIcsCalendar();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=barcelona-bookinger.ics")
                .contentType(MediaType.parseMediaType("text/calendar; charset=utf-8"))
                .body(calendarContent);
    }
}