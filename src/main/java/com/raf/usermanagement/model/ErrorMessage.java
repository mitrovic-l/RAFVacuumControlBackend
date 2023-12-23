package com.raf.usermanagement.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Getter
@Setter
public class ErrorMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Integer id;

    @Column
    private LocalDate date;

    @OneToOne
    @JoinColumn(name = "vacuumId", referencedColumnName = "id")
    private Vacuum vacuum;

    @Column(nullable = false)
    private String operation;

    @Column(nullable = false)
    private String message;

}
