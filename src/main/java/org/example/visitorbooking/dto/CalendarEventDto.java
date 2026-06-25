package org.example.visitorbooking.dto;

public class CalendarEventDto {

    private String title;
    private String start;
    private String end;
    private String color;

    private String bookingType;
    private String comment;
    private Long days;

    public CalendarEventDto(String title, String start, String end, String color) {
        this.title = title;
        this.start = start;
        this.end = end;
        this.color = color;
    }

    public CalendarEventDto(
            String title,
            String start,
            String end,
            String color,
            String bookingType,
            String comment,
            Long days
    ) {
        this.title = title;
        this.start = start;
        this.end = end;
        this.color = color;
        this.bookingType = bookingType;
        this.comment = comment;
        this.days = days;
    }

    public String getTitle() {
        return title;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }

    public String getColor() {
        return color;
    }

    public String getBookingType() {
        return bookingType;
    }

    public String getComment() {
        return comment;
    }

    public Long getDays() {
        return days;
    }
}