package dev.vkazulkin.entity;

public record UpcomingTalkApprovalStatus (UpcomingTalks upcomingTalks, Status status)  {

    public enum Status {
       PENDING,
       NOT_APPROVED,
       APPROVED;
    }

}

