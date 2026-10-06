package com.deepblue.rescue.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class RescueStatusTest {

    @ParameterizedTest
    @CsvSource({
            "ADMITTED, UNDER_EVALUATION",
            "UNDER_EVALUATION, IN_REHABILITATION",
            "IN_REHABILITATION, READY_FOR_RELEASE",
            "READY_FOR_RELEASE, RELEASED",
            "RELEASED, CLOSED"
    })
    void shouldAllowOnlyTheNextStep(RescueStatus from, RescueStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "ADMITTED, RELEASED",
            "ADMITTED, READY_FOR_RELEASE",
            "IN_REHABILITATION, UNDER_EVALUATION",
            "IN_REHABILITATION, IN_REHABILITATION",
            "READY_FOR_RELEASE, CLOSED"
    })
    void shouldRejectSkippingBackwardsOrSameStatus(RescueStatus from, RescueStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    void closedIsTerminalAndNullTargetIsRejected() {
        for (RescueStatus target : RescueStatus.values()) {
            assertThat(RescueStatus.CLOSED.canTransitionTo(target)).isFalse();
        }
        assertThat(RescueStatus.ADMITTED.canTransitionTo(null)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = RescueStatus.class, names = {"UNDER_EVALUATION", "IN_REHABILITATION"})
    void shouldAllowTreatmentOnlyWhileEvaluatedOrInRehabilitation(RescueStatus status) {
        assertThat(status.allowsTreatment()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = RescueStatus.class,
            names = {"ADMITTED", "READY_FOR_RELEASE", "RELEASED", "CLOSED"})
    void shouldNotAllowTreatmentInOtherStatuses(RescueStatus status) {
        assertThat(status.allowsTreatment()).isFalse();
    }
}
