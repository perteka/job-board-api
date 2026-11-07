package com.jobboard.api.entity.enums;

public enum ApplicationStatus {
    APPLIED,
    REVIEWED,
    HIRED,
    REJECTED;

    public boolean canTransitionTo(ApplicationStatus target) {
        return switch (this) {
            case APPLIED -> target == REVIEWED;
            case REVIEWED -> target == HIRED || target == REJECTED;
            case HIRED, REJECTED -> false;
        };
    }
}
