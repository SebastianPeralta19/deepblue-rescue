package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service;

    private Animal buildAnimal(RescueStatus status, LocalDate rescueDate) {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-001");
        rescueCase.setStatus(status);
        rescueCase.setRescueDate(rescueDate);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-001");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.UNKNOWN);
        rescueCase.assignAnimal(animal);

        return animal;
    }

    private Specialist buildSpecialist(boolean active) {
        Specialist specialist = new Specialist();
        specialist.setProfessionalCode("SPEC-001");
        specialist.setFirstName("Elena");
        specialist.setLastName("Vargas");
        specialist.setActive(active);
        return specialist;
    }

    @Test
    void shouldRegisterTreatmentWhenAllRulesPass() {
        Animal animal = buildAnimal(RescueStatus.IN_REHABILITATION, LocalDate.of(2026, 8, 20));
        Specialist specialist = buildSpecialist(true);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001",
                LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper injury."
        );

        TreatmentResponse response = new TreatmentResponse(
                1L, "AN-001", "SPEC-001",
                request.performedAt(), TreatmentType.WOUND_CARE, request.description()
        );

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));
        when(treatmentRepository.save(any(Treatment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Treatment.class))).thenReturn(response);

        TreatmentResponse result = service.register(request);

        assertThat(result).isEqualTo(response);
        verify(treatmentRepository).save(any(Treatment.class));
    }

    @Test
    void shouldThrowWhenAnimalDoesNotExist() {
        when(animalRepository.findByAnimalCode("AN-999")).thenReturn(Optional.empty());

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-999", "SPEC-001", LocalDateTime.now(), TreatmentType.OBSERVATION, "n/a"
        );

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectWhenSpecialistIsInactive() {
        Animal animal = buildAnimal(RescueStatus.IN_REHABILITATION, LocalDate.of(2026, 8, 20));
        Specialist specialist = buildSpecialist(false);

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001",
                LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.OBSERVATION,
                "n/a"
        );

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectWhenRescueCaseIsReleased() {
        Animal animal = buildAnimal(RescueStatus.RELEASED, LocalDate.of(2026, 8, 20));
        Specialist specialist = buildSpecialist(true);

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001",
                LocalDateTime.of(2026, 8, 25, 9, 0),
                TreatmentType.OBSERVATION,
                "n/a"
        );

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectWhenTreatmentDateIsBeforeRescueDate() {
        Animal animal = buildAnimal(RescueStatus.IN_REHABILITATION, LocalDate.of(2026, 8, 20));
        Specialist specialist = buildSpecialist(true);

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001",
                LocalDateTime.of(2026, 8, 15, 9, 0),
                TreatmentType.OBSERVATION,
                "n/a"
        );

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }
}