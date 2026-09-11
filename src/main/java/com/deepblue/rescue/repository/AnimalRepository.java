package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {

    // Paso 34 — Consulta A: buscar animal por animalCode
    Optional<Animal> findByAnimalCode(String animalCode);

    // Paso 34 — Consulta B: commonName que contenga texto, ignorando mayúsculas/minúsculas
    List<Animal> findByCommonNameContainingIgnoreCase(String commonName);

    // Paso 35 — Animal -> rescueCase -> status
    List<Animal> findByRescueCaseStatus(RescueStatus status);

    // Paso 36 — Animal -> RescueCase -> RescueCenter -> code
    List<Animal> findByRescueCaseRescueCenterCode(String centerCode);

    // Reto sin guía — animales en determinado status con al menos un tratamiento
// realizado por un especialista con determinada expertise
    @Query("""
    select distinct a
    from Animal a
    join a.rescueCase c
    join a.treatments t
    join t.specialist s
    join s.expertiseAreas e
    where c.status = :status
    and lower(e.name) = lower(:expertiseName)
    """)
    List<Animal> findInRehabilitationTreatedBySpecialistWithExpertise(
            @Param("status") RescueStatus status,
            @Param("expertiseName") String expertiseName);

}
