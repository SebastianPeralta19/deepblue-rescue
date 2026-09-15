package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalServiceImplTest {

    @Mock
    private AnimalRepository repository;

    @Mock
    private AnimalMapper mapper;

    @InjectMocks
    private AnimalServiceImpl service;

    private Animal animalWithStatus(RescueStatus status) {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-001");
        rescueCase.setStatus(status);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-001");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.UNKNOWN);
        rescueCase.assignAnimal(animal);

        return animal;
    }

    @ParameterizedTest
    @EnumSource(value = RescueStatus.class, names = {"UNDER_EVALUATION", "IN_REHABILITATION"})
    void shouldReceiveTreatmentWhenStatusIsEligible(RescueStatus status) {
        when(repository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animalWithStatus(status)));

        assertThat(service.canReceiveTreatment("AN-001")).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = RescueStatus.class, names = {"ADMITTED", "READY_FOR_RELEASE", "RELEASED", "CLOSED"})
    void shouldNotReceiveTreatmentWhenStatusIsNotEligible(RescueStatus status) {
        when(repository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animalWithStatus(status)));

        assertThat(service.canReceiveTreatment("AN-001")).isFalse();
    }
}