package com.deepblue.rescue.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "medical_records")
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "initial_weight", nullable = false)
    private BigDecimal initialWeight;

    @Column(name = "initial_condition", nullable = false)
    private String initialCondition;

    @Column(name = "injuries", columnDefinition = "text")
    private String injuries;

    @Column(name = "observations", columnDefinition = "text")
    private String observations;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false, unique = true)
    private Animal animal;

    public MedicalRecord() {
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getInitialWeight() {
        return initialWeight;
    }

    public void setInitialWeight(BigDecimal initialWeight) {
        this.initialWeight = initialWeight;
    }

    public String getInitialCondition() {
        return initialCondition;
    }

    public void setInitialCondition(String initialCondition) {
        this.initialCondition = initialCondition;
    }

    public String getInjuries() {
        return injuries;
    }

    public void setInjuries(String injuries) {
        this.injuries = injuries;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public Animal getAnimal() {
        return animal;
    }

    public void setAnimal(Animal animal) {
        this.animal = animal;
    }
}