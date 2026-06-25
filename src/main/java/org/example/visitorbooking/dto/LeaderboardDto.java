package org.example.visitorbooking.dto;

public class LeaderboardDto {

    private String guestName;
    private long visitCount;

    public LeaderboardDto(String guestName, long visitCount) {
        this.guestName = guestName;
        this.visitCount = visitCount;
    }

    public String getGuestName() {
        return guestName;
    }

    public long getVisitCount() {
        return visitCount;
    }
}