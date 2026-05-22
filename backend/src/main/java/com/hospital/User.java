package com.hospital;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String username;

    @JsonIgnore
    @Column(nullable = false)
    public String password;

    public enum Role {
        ADMIN,
        MANAGER,
        EMPLOYEE
    }

    public enum Assigment {
        NOT_ASSIGNED,
        EMERGENCY,
        INPATIENT,
        OUTPATIENT,
        DAY_CARE,
        NURSING
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Assigment assigment;


    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }


    public Role getRole() {
        return role;
    }

    public Assigment getAssigment() {
        return assigment;
    }
}