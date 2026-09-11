package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@Repository
public interface RescueCaseRepository extends JpaRepository<RescueCase, Long> {

    // Paso 32 — Consulta A: buscar un caso por caseCode
    Optional<RescueCase> findByCaseCode(String caseCode);

    // Paso 32 — Consulta B: buscar todos los casos según status, ordenados por rescueDate ASC
    List<RescueCase> findByStatusOrderByRescueDateAsc(RescueStatus status);

    // Paso 32 — Consulta C / Paso 33 — navegando la relación RescueCase -> rescueCenter -> code
    List<RescueCase> findByRescueCenterCode(String code);

    // Paso 37 — casos posteriores a una fecha, del más reciente al más antiguo
    List<RescueCase> findByRescueDateAfterOrderByRescueDateDesc(LocalDate date);

    // Paso 66 — Consulta 1: ¿existe el caso?
    boolean existsByCaseCode(String caseCode);

}
