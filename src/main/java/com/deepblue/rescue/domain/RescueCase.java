package com.deepblue.rescue.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "rescue_cases")
public class RescueCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_code", nullable = false, unique = true)
    private String caseCode;

    @Column(name = "rescue_date", nullable = false)
    private LocalDate rescueDate;

    @Column(name = "rescue_location", nullable = false)
    private String rescueLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RescueStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescue_center_id", nullable = false)
    private RescueCenter rescueCenter;

    @OneToOne(
            mappedBy = "rescueCase",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Animal animal;

    public RescueCase() {
    }

    public void assignAnimal(Animal animal) {
        if (animal == null) {
            if (this.animal != null) {
                this.animal.setRescueCase(null);
            }
        } else {
            animal.setRescueCase(this);
        }
        this.animal = animal;
    }

    public Long getId() {
        return id;
    }

    public String getCaseCode() {
        return caseCode;
    }

    public void setCaseCode(String caseCode) {
        this.caseCode = caseCode;
    }

    public LocalDate getRescueDate() {
        return rescueDate;
    }

    public void setRescueDate(LocalDate rescueDate) {
        this.rescueDate = rescueDate;
    }

    public String getRescueLocation() {
        return rescueLocation;
    }

    public void setRescueLocation(String rescueLocation) {
        this.rescueLocation = rescueLocation;
    }

    public RescueStatus getStatus() {
        return status;
    }

    public void setStatus(RescueStatus status) {
        this.status = status;
    }

    public RescueCenter getRescueCenter() {
        return rescueCenter;
    }

    public void setRescueCenter(RescueCenter rescueCenter) {
        this.rescueCenter = rescueCenter;
    }

    public Animal getAnimal() {
        return animal;
    }
}