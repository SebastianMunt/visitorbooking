package org.example.visitorbooking.dto;

public class CalendarEventDto {

    private String title;
    private String start;
    private String end;
    private String color;

    public CalendarEventDto() {
    }

    public CalendarEventDto(String title, String start, String end, String color) {
        this.title = title;
        this.start = start;
        this.end = end;
        this.color = color;
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
}