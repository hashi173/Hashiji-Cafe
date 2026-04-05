package com.coffeeshop.entity;

public enum JobType {
    FULL_TIME("Full-time"),
    PART_TIME("Part-time"),
    INTERNSHIP("Internship");

    private final String displayName;

    JobType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
