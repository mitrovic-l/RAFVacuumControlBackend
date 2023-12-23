package com.raf.usermanagement.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Vacuum {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VacuumStatus status;

    @ManyToOne
    @JoinColumn(name = "addedBy", referencedColumnName = "id")
    private User addedBy;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private LocalDate dateAdded;

    @Column(nullable = false)
    private Integer startCount;

    @Version
    private Integer version = 0; //zbog optimisticLock-ovanja...
    public enum VacuumStatus {
        ON, OFF, DISCHARGING
    }
}
