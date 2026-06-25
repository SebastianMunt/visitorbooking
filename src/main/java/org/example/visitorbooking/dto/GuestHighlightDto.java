package org.example.visitorbooking.dto;

public class GuestHighlightDto {

    private String longestVisitText;
    private String firstBookingText;
    private String firstVisitText;

    public GuestHighlightDto(String longestVisitText, String firstBookingText, String firstVisitText) {
        this.longestVisitText = longestVisitText;
        this.firstBookingText = firstBookingText;
        this.firstVisitText = firstVisitText;
    }

    public String getLongestVisitText() {
        return longestVisitText;
    }

    public String getFirstBookingText() {
        return firstBookingText;
    }

    public String getFirstVisitText() {
        return firstVisitText;
    }
}