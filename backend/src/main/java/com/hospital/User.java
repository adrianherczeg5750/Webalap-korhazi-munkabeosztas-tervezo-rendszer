package com.hospital;

import jakarta.persistence.*;
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String username;

    @Column(nullable = false)
    public String password;

    public enum Role {
        ADMIN,
        MANAGER,
        EMPLOYEE
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Role role;


    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }


    public Role getRole() {
        return role;
    }
}