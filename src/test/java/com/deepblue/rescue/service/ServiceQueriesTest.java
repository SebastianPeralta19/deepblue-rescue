package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Consultas de los tres services (los tests de reglas de negocio están en *ServiceImplTest). */
@ExtendWith(MockitoExtension.class)
class ServiceQueriesTest {

    @Mock
    private AnimalRepository animalRepository;
    @Mock
    private AnimalMapper animalMapper;
    @Mock
    private RescueCaseRepository rescueCaseRepository;
    @Mock
    private RescueCaseMapper rescueCaseMapper;
    @Mock
    private SpecialistRepository specialistRepository;
    @Mock
    private TreatmentRepository treatmentRepository;
    @Mock
    private TreatmentMapper treatmentMapper;

    private static Animal animal() {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-001");
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        Animal animal = new Animal();
        animal.setAnimalCode("AN-001");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.UNKNOWN);
        rescueCase.assignAnimal(animal);
        return animal;
    }

    private static AnimalResponse animalResponse() {
        return new AnimalResponse(1L, "AN-001", "Green Sea Turtle", "Chelonia mydas",
                AnimalSex.UNKNOWN, null, "RES-001", RescueStatus.IN_REHABILITATION);
    }

    // ---------- AnimalService ----------

    @Test
    void animalFindByCodeReturnsDto() {
        AnimalServiceImpl service = new AnimalServiceImpl(animalRepository, animalMapper);
        Animal animal = animal();
        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(animalMapper.toResponse(animal)).thenReturn(animalResponse());

        assertThat(service.findByCode("AN-001")).isEqualTo(animalResponse());
    }

    @Test
    void animalFindByCodeThrowsWhenMissing() {
        AnimalServiceImpl service = new AnimalServiceImpl(animalRepository, animalMapper);
        when(animalRepository.findByAnimalCode("AN-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("AN-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Animal not found: AN-999");
    }

    @Test
    void animalsInRehabilitationQueriesIN_REHABILITATION() {
        AnimalServiceImpl service = new AnimalServiceImpl(animalRepository, animalMapper);
        Animal animal = animal();
        when(animalRepository.findByRescueCaseStatus(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(animal));
        when(animalMapper.toResponse(animal)).thenReturn(animalResponse());

        assertThat(service.findAnimalsInRehabilitation()).containsExactly(animalResponse());
    }

    @Test
    void eligibilityThrowsWhenAnimalMissing() {
        AnimalServiceImpl service = new AnimalServiceImpl(animalRepository, animalMapper);
        when(animalRepository.findByAnimalCode("AN-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.canReceiveTreatment("AN-999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- RescueCaseService ----------

    @Test
    void rescueCasesByStatusAreMapped() {
        RescueCaseServiceImpl service = new RescueCaseServiceImpl(rescueCaseRepository, rescueCaseMapper);
        RescueCase rescueCase = new RescueCase();
        RescueCaseResponse response = new RescueCaseResponse(
                1L, "RES-001", LocalDate.of(2026, 8, 20), "Bahia Concha",
                RescueStatus.IN_REHABILITATION, "DB-CAR", null);
        when(rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(rescueCase));
        when(rescueCaseMapper.toResponse(rescueCase)).thenReturn(response);

        assertThat(service.findByStatus(RescueStatus.IN_REHABILITATION)).containsExactly(response);
    }

    @Test
    void changeStatusWithoutStatusIsRejectedBeforeTouchingTheRepository() {
        RescueCaseServiceImpl service = new RescueCaseServiceImpl(rescueCaseRepository, rescueCaseMapper);

        assertThatThrownBy(() -> service.changeStatus(
                "RES-001", new com.deepblue.rescue.dto.request.ChangeRescueStatusRequest(null)))
                .isInstanceOf(com.deepblue.rescue.exception.BusinessRuleException.class);

        verify(rescueCaseRepository, never()).save(any());
    }

    // ---------- TreatmentService ----------

    @Test
    void treatmentsOfAnimalAreReturnedInChronologicalOrder() {
        TreatmentServiceImpl service = new TreatmentServiceImpl(
                animalRepository, specialistRepository, treatmentRepository, treatmentMapper);
        Animal animal = animal();
        ReflectionTestUtils.setField(animal, "id", 7L);
        Treatment treatment = new Treatment();
        TreatmentResponse response = new TreatmentResponse(
                1L, "AN-001", "SPEC-001", LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.WOUND_CARE, "Cleaning and treatment of flipper injury.");
        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(7L)).thenReturn(List.of(treatment));
        when(treatmentMapper.toResponse(treatment)).thenReturn(response);

        assertThat(service.findByAnimalCode("AN-001")).containsExactly(response);
    }

    @Test
    void treatmentsOfMissingAnimalThrowNotFound() {
        TreatmentServiceImpl service = new TreatmentServiceImpl(
                animalRepository, specialistRepository, treatmentRepository, treatmentMapper);
        when(animalRepository.findByAnimalCode("AN-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByAnimalCode("AN-999"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(treatmentRepository, never()).findByAnimalIdOrderByPerformedAtAsc(any());
    }
}
