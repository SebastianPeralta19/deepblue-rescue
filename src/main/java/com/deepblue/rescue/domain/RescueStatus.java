package com.deepblue.rescue.domain;

public enum RescueStatus {
    ADMITTED,
    UNDER_EVALUATION,
    IN_REHABILITATION,
    READY_FOR_RELEASE,
    RELEASED,
    CLOSED;

    /** Un animal solo puede recibir tratamientos mientras se evalúa o rehabilita. */
    public boolean allowsTreatment() {
        return this == UNDER_EVALUATION || this == IN_REHABILITATION;
    }

    /**
     * Ciclo de vida lineal: cada estado solo puede avanzar al siguiente.
     * ADMITTED -> UNDER_EVALUATION -> IN_REHABILITATION -> READY_FOR_RELEASE -> RELEASED -> CLOSED
     */
    public boolean canTransitionTo(RescueStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case ADMITTED -> target == UNDER_EVALUATION;
            case UNDER_EVALUATION -> target == IN_REHABILITATION;
            case IN_REHABILITATION -> target == READY_FOR_RELEASE;
            case READY_FOR_RELEASE -> target == RELEASED;
            case RELEASED -> target == CLOSED;
            case CLOSED -> false;
        };
    }
}
