package com.hycora.backend.domain.activity.entity;

public class StatusLabel {

    public static String from(String status) {
        return switch (status) {
            case "scheduled" -> "예정";
            case "recruiting" -> "모집중";
            case "ongoing" -> "진행중";
            case "completed" -> "완료";
            default -> throw new IllegalArgumentException("Invalid status: " + status);
        };
    }
}
