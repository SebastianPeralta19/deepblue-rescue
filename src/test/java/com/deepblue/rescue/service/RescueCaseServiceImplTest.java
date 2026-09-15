package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RescueCaseServiceImplTest {

    @Mock
    private RescueCaseRepository repository;

    @Mock
    private RescueCaseMapper mapper;

    @InjectMocks
    private RescueCaseServiceImpl service;

    @Test
    void shouldFindRescueCaseByCode() {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-001");
        rescueCase.setStatus(RescueStatus.ADMITTED);
        rescueCase.setRescueDate(LocalDate.of(2026, 1, 1));

        RescueCaseResponse response = new RescueCaseResponse(
                1L, "RES-001", LocalDate.of(2026, 1, 1), "Bahía Concha",
                RescueStatus.ADMITTED, "DB-CAR", null
        );

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        RescueCaseResponse result = service.findByCode("RES-001");

        assertThat(result).isEqualTo(response);
        verify(repository).findByCaseCode("RES-001");
        verify(mapper).toResponse(rescueCase);
    }

    @Test
    void shouldThrowWhenRescueCaseDoesNotExist() {
        when(repository.findByCaseCode("RES-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("RES-999"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldChangeStatusOnValidTransition() {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-002");
        rescueCase.setStatus(RescueStatus.ADMITTED);

        RescueCaseResponse response = new RescueCaseResponse(
                2L, "RES-002", null, null, RescueStatus.UNDER_EVALUATION, null, null
        );

        when(repository.findByCaseCode("RES-002")).thenReturn(Optional.of(rescueCase));
        when(repository.save(rescueCase)).thenReturn(rescueCase);
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        RescueCaseResponse result = service.changeStatus(
                "RES-002",
                new ChangeRescueStatusRequest(RescueStatus.UNDER_EVALUATION)
        );

        assertThat(result).isEqualTo(response);
        assertThat(rescueCase.getStatus()).isEqualTo(RescueStatus.UNDER_EVALUATION);
        verify(repository).save(rescueCase);
    }

    @Test
    void shouldRejectInvalidTransition() {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-003");
        rescueCase.setStatus(RescueStatus.ADMITTED);

        when(repository.findByCaseCode("RES-003")).thenReturn(Optional.of(rescueCase));

        assertThatThrownBy(() -> service.changeStatus(
                "RES-003",
                new ChangeRescueStatusRequest(RescueStatus.READY_FOR_RELEASE)
        )).isInstanceOf(BusinessRuleException.class);

        verify(repository, never()).save(any());
    }
}