package com.deepblue.rescue;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@Transactional
public class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired
    private RescueCenterRepository rescueCenterRepository;
    @Autowired
    private RescueCaseRepository rescueCaseRepository;
    @Autowired
    private AnimalRepository animalRepository;
    @Autowired
    private SpecialistRepository specialistRepository;
    @Autowired
    private ExpertiseRepository expertiseRepository;
    @Autowired
    private TreatmentRepository treatmentRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Paso 48 — Test de métodos heredados
    @Test
    void shouldSaveAndFindRescueCenterUsingInheritedMethods() {
        RescueCenter center = new RescueCenter();
        center.setCode("DB-CAR");
        center.setName("DeepBlue Caribbean Center");
        center.setCity("Santa Marta");

        RescueCenter saved = rescueCenterRepository.save(center);

        assertTrue(rescueCenterRepository.existsById(saved.getId()));
        assertEquals(1, rescueCenterRepository.count());

        Optional<RescueCenter> found = rescueCenterRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("DB-CAR", found.get().getCode());
    }

    // Paso 49 — Test relación 1:N
    @Test
    void shouldAssociateTwoCasesToTheSameCenter() {
        RescueCenter center = new RescueCenter();
        center.setCode("DB-1N");
        center.setName("DeepBlue Test Center");
        center.setCity("Barranquilla");
        rescueCenterRepository.save(center);

        RescueCase case1 = buildCase("RES-1N-001", RescueStatus.ADMITTED, LocalDate.of(2026, 1, 10));
        RescueCase case2 = buildCase("RES-1N-002", RescueStatus.ADMITTED, LocalDate.of(2026, 1, 15));

        center.addCase(case1);
        center.addCase(case2);

        rescueCaseRepository.save(case1);
        rescueCaseRepository.save(case2);

        List<RescueCase> cases = rescueCaseRepository.findByRescueCenterCode("DB-1N");

        assertThat(cases).hasSize(2);
        assertThat(cases).allMatch(c -> c.getRescueCenter().getCode().equals("DB-1N"));
    }

    // Paso 50 — Test RescueCase 1:1 Animal
    @Test
    void shouldAssignAnimalToRescueCase() {
        RescueCase rescueCase = buildCase("RES-2026-001", RescueStatus.ADMITTED, LocalDate.of(2026, 2, 1));
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-001");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.UNKNOWN);

        rescueCase.assignAnimal(animal);
        animalRepository.save(animal);
        rescueCaseRepository.saveAndFlush(rescueCase);

        RescueCase persistedCase = rescueCaseRepository.findByCaseCode("RES-2026-001").orElseThrow();
        Animal persistedAnimal = animalRepository.findByAnimalCode("AN-2026-001").orElseThrow();

        assertEquals(persistedAnimal.getAnimalCode(), persistedCase.getAnimal().getAnimalCode());
        assertEquals(persistedCase.getCaseCode(), persistedAnimal.getRescueCase().getCaseCode());
    }

    // Paso 51 — Test Animal 1:1 MedicalRecord
    @Test
    void shouldPersistAnimalWithMedicalRecordUsingCascade() {
        RescueCase rescueCase = buildCase("RES-2026-002", RescueStatus.UNDER_EVALUATION, LocalDate.of(2026, 2, 5));
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-002");
        animal.setCommonName("Loggerhead Turtle");
        animal.setScientificName("Caretta caretta");
        animal.setSex(AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);

        MedicalRecord record = new MedicalRecord();
        record.setInitialWeight(new BigDecimal("28.40"));
        record.setInitialCondition("STABLE");
        record.setInjuries("Left front flipper injury");
        animal.assignMedicalRecord(record);

        animalRepository.saveAndFlush(animal);

        assertNotNull(animal.getId());
        assertNotNull(animal.getMedicalRecord().getId());
    }

    // Paso 52 — Test N:M
    @Test
    void shouldAssociateSpecialistWithTwoExpertiseAreas() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist();
        elena.setProfessionalCode("SP-001");
        elena.setFirstName("Elena");
        elena.setLastName("Vargas");
        elena.setEmail("elena.vargas@deepblue.org");
        elena.setActive(true);

        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);

        specialistRepository.saveAndFlush(elena);

        Specialist persisted = specialistRepository.findById(elena.getId()).orElseThrow();
        assertEquals(2, persisted.getExpertiseAreas().size());
    }

    // Paso 53 — Test Query Method simple
    @Test
    void shouldFindCasesByStatus() {
        RescueCenter center = new RescueCenter();
        center.setCode("DB-QM1");
        center.setName("DeepBlue QM Center");
        center.setCity("Cartagena");
        rescueCenterRepository.save(center);

        RescueCase case1 = buildCase("RES-001", RescueStatus.IN_REHABILITATION, LocalDate.of(2026, 3, 1));
        RescueCase case2 = buildCase("RES-002", RescueStatus.READY_FOR_RELEASE, LocalDate.of(2026, 3, 2));
        RescueCase case3 = buildCase("RES-003", RescueStatus.IN_REHABILITATION, LocalDate.of(2026, 3, 3));

        center.addCase(case1);
        center.addCase(case2);
        center.addCase(case3);
        rescueCaseRepository.saveAll(List.of(case1, case2, case3));

        List<RescueCase> result = rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);

        assertThat(result).hasSize(2);
    }

    // Paso 54 — Test Query Method navegando relaciones
    @Test
    void shouldFindAnimalsBelongingToASpecificCenter() {
        RescueCenter centerCar = new RescueCenter();
        centerCar.setCode("DB-CAR-QM");
        centerCar.setName("DeepBlue Caribbean QM");
        centerCar.setCity("Santa Marta");
        rescueCenterRepository.save(centerCar);

        RescueCenter centerPac = new RescueCenter();
        centerPac.setCode("DB-PAC-QM");
        centerPac.setName("DeepBlue Pacific QM");
        centerPac.setCity("Buenaventura");
        rescueCenterRepository.save(centerPac);

        RescueCase caseCar = buildCase("RES-CAR-QM", RescueStatus.ADMITTED, LocalDate.of(2026, 3, 10));
        centerCar.addCase(caseCar);
        rescueCaseRepository.save(caseCar);

        RescueCase casePac = buildCase("RES-PAC-QM", RescueStatus.ADMITTED, LocalDate.of(2026, 3, 11));
        centerPac.addCase(casePac);
        rescueCaseRepository.save(casePac);

        Animal animalCar = new Animal();
        animalCar.setAnimalCode("AN-CAR-QM");
        animalCar.setCommonName("Hawksbill Turtle");
        animalCar.setScientificName("Eretmochelys imbricata");
        animalCar.setSex(AnimalSex.MALE);
        caseCar.assignAnimal(animalCar);
        animalRepository.save(animalCar);

        Animal animalPac = new Animal();
        animalPac.setAnimalCode("AN-PAC-QM");
        animalPac.setCommonName("Olive Ridley Turtle");
        animalPac.setScientificName("Lepidochelys olivacea");
        animalPac.setSex(AnimalSex.FEMALE);
        casePac.assignAnimal(animalPac);
        animalRepository.save(animalPac);

        List<Animal> result = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR-QM");

        assertThat(result).extracting(Animal::getAnimalCode).containsExactly("AN-CAR-QM");
    }

    // Paso 55 — Test JPQL de especialistas
    @Test
    void shouldFindActiveSpecialistsByExpertise() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();
        Expertise marineMammals = expertiseRepository.findByNameIgnoreCase("Marine Mammals").orElseThrow();
        Expertise marineBirds = expertiseRepository.findByNameIgnoreCase("Marine Birds").orElseThrow();

        Specialist elena = newActiveSpecialist("SP-ELENA", "Elena", "Vargas", "elena@deepblue.org");
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);

        Specialist mateo = newActiveSpecialist("SP-MATEO", "Mateo", "Rios", "mateo@deepblue.org");
        mateo.addExpertise(marineMammals);
        mateo.addExpertise(rehabilitation);

        Specialist sofia = newActiveSpecialist("SP-SOFIA", "Sofia", "Cruz", "sofia@deepblue.org");
        sofia.addExpertise(marineBirds);
        sofia.addExpertise(trauma);

        specialistRepository.saveAll(List.of(elena, mateo, sofia));

        List<Specialist> result = specialistRepository.findActiveByExpertise("Trauma");

        assertThat(result).extracting(Specialist::getFirstName).containsExactlyInAnyOrder("Elena", "Sofia");
    }

    // Paso 56 y 57 — Tratamientos + Query Method
    @Test
    void shouldFindTreatmentsOfAnimalOrderedChronologically() {
        RescueCase rescueCase = buildCase("RES-TR-001", RescueStatus.IN_REHABILITATION, LocalDate.of(2026, 4, 1));
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-TR-001");
        animal.setCommonName("Kemp's Ridley Turtle");
        animal.setScientificName("Lepidochelys kempii");
        animal.setSex(AnimalSex.UNKNOWN);
        rescueCase.assignAnimal(animal);
        animalRepository.save(animal);

        Specialist elena = newActiveSpecialist("SP-TR-ELENA", "Elena", "Vargas", "elena.tr@deepblue.org");
        Specialist mateo = newActiveSpecialist("SP-TR-MATEO", "Mateo", "Rios", "mateo.tr@deepblue.org");
        specialistRepository.saveAll(List.of(elena, mateo));

        Treatment t1 = newTreatment(animal, elena, LocalDateTime.of(2026, 4, 2, 9, 0), TreatmentType.WOUND_CARE);
        Treatment t2 = newTreatment(animal, elena, LocalDateTime.of(2026, 4, 3, 9, 0), TreatmentType.HYDRATION);
        Treatment t3 = newTreatment(animal, mateo, LocalDateTime.of(2026, 4, 4, 9, 0), TreatmentType.OBSERVATION);

        treatmentRepository.saveAll(List.of(t1, t2, t3));

        List<Treatment> result = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());

        assertThat(result).extracting(Treatment::getType)
                .containsExactly(TreatmentType.WOUND_CARE, TreatmentType.HYDRATION, TreatmentType.OBSERVATION);
    }

    // Paso 58 — Test JPQL por intervalo
    @Test
    void shouldFindTreatmentsWithinDateRange() {
        RescueCase rescueCase = buildCase("RES-TR-002", RescueStatus.IN_REHABILITATION, LocalDate.of(2026, 7, 1));
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-TR-002");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.MALE);
        rescueCase.assignAnimal(animal);
        animalRepository.save(animal);

        Specialist specialist = newActiveSpecialist("SP-TR-002", "Ana", "Marin", "ana.tr@deepblue.org");
        specialistRepository.save(specialist);

        Treatment early = newTreatment(animal, specialist, LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.OBSERVATION);
        Treatment inRange = newTreatment(animal, specialist, LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.MEDICATION);
        Treatment late = newTreatment(animal, specialist, LocalDateTime.of(2026, 8, 20, 10, 0), TreatmentType.OBSERVATION);

        treatmentRepository.saveAll(List.of(early, inRange, late));

        List<Treatment> result = treatmentRepository.findByPerformedAtBetween(
                LocalDateTime.of(2026, 8, 5, 0, 0),
                LocalDateTime.of(2026, 8, 15, 0, 0));

        assertThat(result).hasSize(1);
        assertEquals(LocalDateTime.of(2026, 8, 10, 10, 0), result.get(0).getPerformedAt());
    }

    // Paso 59 — Probar UNIQUE
    @Test
    void shouldFailWhenSavingDuplicateAnimalCode() {
        RescueCase case1 = buildCase("RES-UK-001", RescueStatus.ADMITTED, LocalDate.of(2026, 5, 1));
        rescueCaseRepository.save(case1);
        Animal animal1 = new Animal();
        animal1.setAnimalCode("AN-100");
        animal1.setCommonName("Green Sea Turtle");
        animal1.setScientificName("Chelonia mydas");
        animal1.setSex(AnimalSex.UNKNOWN);
        case1.assignAnimal(animal1);
        animalRepository.saveAndFlush(animal1);

        RescueCase case2 = buildCase("RES-UK-002", RescueStatus.ADMITTED, LocalDate.of(2026, 5, 2));
        rescueCaseRepository.save(case2);
        Animal animal2 = new Animal();
        animal2.setAnimalCode("AN-100");
        animal2.setCommonName("Loggerhead Turtle");
        animal2.setScientificName("Caretta caretta");
        animal2.setSex(AnimalSex.UNKNOWN);
        case2.assignAnimal(animal2);

        assertThrows(DataIntegrityViolationException.class,
                () -> animalRepository.saveAndFlush(animal2));
    }

    // Paso 60 — Probar FK
    @Test
    void shouldEnforceForeignKeyConstraintAtDatabaseLevel() {
        assertThrows(DataAccessException.class, () ->
                jdbcTemplate.update(
                        "insert into treatments (animal_id, specialist_id, performed_at, type) " +
                                "values (?, ?, ?, ?)",
                        999999L, 999999L, LocalDateTime.now(), "OBSERVATION"));
    }

    // Paso 61 — Probar CHECK
    @Test
    void shouldEnforceCheckConstraintOnStatusColumn() {
        RescueCenter center = new RescueCenter();
        center.setCode("DB-CHK");
        center.setName("DeepBlue Check Center");
        center.setCity("Riohacha");
        RescueCenter saved = rescueCenterRepository.saveAndFlush(center);

        assertThrows(DataAccessException.class, () ->
                jdbcTemplate.update(
                        "insert into rescue_cases (case_code, rescue_date, rescue_location, status, rescue_center_id) " +
                                "values (?, ?, ?, ?, ?)",
                        "RES-CHK-001", LocalDate.of(2026, 6, 1), "Riohacha", "INVALID_STATUS", saved.getId()));
    }

    private RescueCase buildCase(String caseCode, RescueStatus status, LocalDate date) {
        // 1. Crear y guardar un centro por defecto para evitar nulos en la FK
        RescueCenter center = new RescueCenter();
        center.setCode("CTR-" + caseCode);
        center.setName("Center for " + caseCode);
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        // 2. Construir el caso y asignarle el centro
        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode(caseCode);
        rescueCase.setRescueDate(date);
        rescueCase.setRescueLocation("Bahía Concha");
        rescueCase.setStatus(status);
        rescueCase.setRescueCenter(center); // <-- Asignación de la FK obligatoria

        return rescueCase;
    }

    private Specialist newActiveSpecialist(String code, String firstName, String lastName, String email) {
        Specialist specialist = new Specialist();
        specialist.setProfessionalCode(code);
        specialist.setFirstName(firstName);
        specialist.setLastName(lastName);
        specialist.setEmail(email);
        specialist.setActive(true);
        return specialist;
    }

    private Treatment newTreatment(Animal animal, Specialist specialist, LocalDateTime performedAt, TreatmentType type) {
        Treatment treatment = new Treatment();
        treatment.setAnimal(animal);
        treatment.setSpecialist(specialist);
        treatment.setPerformedAt(performedAt);
        treatment.setType(type);
        return treatment;
    }

    // Reto integrador — Paso 65: persistir el escenario completo
    @Test
    void shouldPersistAndQueryIntegratorChallengeScenario() {
        RescueCenter center = new RescueCenter();
        center.setCode("DB-CAR");
        center.setName("DeepBlue Caribbean");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = buildCase("RES-2026-100", RescueStatus.IN_REHABILITATION, LocalDate.of(2026, 8, 18));
        rescueCase.setRescueLocation("Bahía Concha");
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-100");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);

        MedicalRecord record = new MedicalRecord();
        record.setInitialWeight(new BigDecimal("27.80"));
        record.setInitialCondition("STABLE");
        record.setInjuries("Injury caused by fishing net");
        record.setObservations("Possible plastic ingestion");
        animal.assignMedicalRecord(record);

        animalRepository.saveAndFlush(animal);

        Expertise marineReptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow();
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = newActiveSpecialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org");
        elena.addExpertise(marineReptiles);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);
        specialistRepository.save(elena);

        Treatment treatment1 = newTreatment(animal, elena, LocalDateTime.of(2026, 8, 19, 9, 0), TreatmentType.WOUND_CARE);
        treatment1.setDescription("Cleaning of left front flipper");

        Treatment treatment2 = newTreatment(animal, elena, LocalDateTime.of(2026, 8, 20, 9, 0), TreatmentType.HYDRATION);
        treatment2.setDescription("Subcutaneous fluid therapy");

        treatmentRepository.saveAll(List.of(treatment1, treatment2));

        // Paso 66 — Consulta 1: ¿existe el caso?
        assertTrue(rescueCaseRepository.existsByCaseCode("RES-2026-100"));

        // Consulta 2: casos IN_REHABILITATION
        assertThat(rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION))
                .extracting(RescueCase::getCaseCode)
                .contains("RES-2026-100");

        // Consulta 3: animales de DB-CAR
        assertThat(animalRepository.findByRescueCaseRescueCenterCode("DB-CAR"))
                .extracting(Animal::getAnimalCode)
                .contains("AN-2026-100");

        // Consulta 4: nombre común que contenga "turtle"
        assertThat(animalRepository.findByCommonNameContainingIgnoreCase("turtle"))
                .extracting(Animal::getAnimalCode)
                .contains("AN-2026-100");

        // Consulta 5: especialistas con experiencia Trauma
        assertThat(specialistRepository.findActiveByExpertise("Trauma"))
                .extracting(Specialist::getProfessionalCode)
                .contains("SPEC-001");

        // Consulta 6: tratamientos de AN-2026-100 en orden cronológico
        assertThat(treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId()))
                .extracting(Treatment::getType)
                .containsExactly(TreatmentType.WOUND_CARE, TreatmentType.HYDRATION);

        // Consulta 7: tratamientos por especialistas con experiencia Rehabilitation
        assertThat(treatmentRepository.findBySpecialistExpertise("Rehabilitation"))
                .contains(treatment1, treatment2);

        // Consulta 8: tratamientos entre dos fechas
        assertThat(treatmentRepository.findByPerformedAtBetween(
                LocalDateTime.of(2026, 8, 18, 0, 0),
                LocalDateTime.of(2026, 8, 21, 0, 0)))
                .hasSize(2);
    }

    // Reto sin guía — animales en rehabilitación tratados por especialista con experiencia en Trauma
    @Test
    void shouldFindAnimalsInRehabilitationTreatedBySpecialistWithTraumaExpertise() {
        RescueCase rescueCase = buildCase("RES-NOGUIDE-001", RescueStatus.IN_REHABILITATION, LocalDate.of(2026, 9, 1));
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-NOGUIDE-001");
        animal.setCommonName("Hawksbill Turtle");
        animal.setScientificName("Eretmochelys imbricata");
        animal.setSex(AnimalSex.MALE);
        rescueCase.assignAnimal(animal);
        animalRepository.save(animal);

        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Specialist specialist = newActiveSpecialist("SP-NOGUIDE", "Carlos", "Diaz", "carlos.noguide@deepblue.org");
        specialist.addExpertise(trauma);
        specialistRepository.save(specialist);

        Treatment t1 = newTreatment(animal, specialist, LocalDateTime.of(2026, 9, 2, 10, 0), TreatmentType.WOUND_CARE);
        Treatment t2 = newTreatment(animal, specialist, LocalDateTime.of(2026, 9, 3, 10, 0), TreatmentType.OBSERVATION);
        treatmentRepository.saveAll(List.of(t1, t2));

        List<Animal> result = animalRepository.findInRehabilitationTreatedBySpecialistWithExpertise(
                RescueStatus.IN_REHABILITATION, "trauma");

        assertThat(result).extracting(Animal::getAnimalCode).containsExactly("AN-NOGUIDE-001");
    }
}