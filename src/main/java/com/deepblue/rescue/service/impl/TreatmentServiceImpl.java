package com.deepblue.rescue.service.impl;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.TreatmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TreatmentServiceImpl implements TreatmentService {

    private final AnimalRepository animalRepository;
    private final SpecialistRepository specialistRepository;
    private final TreatmentRepository treatmentRepository;
    private final TreatmentMapper mapper;

    public TreatmentServiceImpl(AnimalRepository animalRepository,
                                SpecialistRepository specialistRepository,
                                TreatmentRepository treatmentRepository,
                                TreatmentMapper mapper) {
        this.animalRepository = animalRepository;
        this.specialistRepository = specialistRepository;
        this.treatmentRepository = treatmentRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public TreatmentResponse register(CreateTreatmentRequest request) {
        Animal animal = getAnimal(request.animalCode());
        Specialist specialist = specialistRepository.findByProfessionalCode(request.specialistCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Specialist not found: " + request.specialistCode()));

        if (!specialist.isActive()) {
            throw new BusinessRuleException(
                    "Specialist is not active: " + specialist.getProfessionalCode());
        }

        RescueCase rescueCase = animal.getRescueCase();
        if (rescueCase == null) {
            throw new BusinessRuleException(
                    "Animal has no rescue case: " + animal.getAnimalCode());
        }
        if (rescueCase.getStatus() == RescueStatus.RELEASED) {
            throw new BusinessRuleException("Released animals cannot receive treatments");
        }
        if (!rescueCase.getStatus().allowsTreatment()) {
            throw new BusinessRuleException(
                    "Animal cannot receive treatments while the rescue case is "
                            + rescueCase.getStatus());
        }
        if (rescueCase.getRescueDate() != null
                && request.performedAt().toLocalDate().isBefore(rescueCase.getRescueDate())) {
            throw new BusinessRuleException(
                    "Treatment date cannot be before the rescue date");
        }

        Treatment treatment = new Treatment();
        treatment.setAnimal(animal);
        treatment.setSpecialist(specialist);
        treatment.setPerformedAt(request.performedAt());
        treatment.setType(request.type());
        treatment.setDescription(request.description());

        return mapper.toResponse(treatmentRepository.save(treatment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentResponse> findByAnimalCode(String animalCode) {
        Animal animal = getAnimal(animalCode);
        return treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId())
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    private Animal getAnimal(String animalCode) {
        return animalRepository.findByAnimalCode(animalCode)
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found: " + animalCode));
    }
}
