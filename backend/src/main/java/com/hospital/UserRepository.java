package com.hospital;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    public User findByUsername(String username) {
        return find("username", username).firstResult();
    }

    public User findById(Long id) {
        return find("id", id).firstResult();
    }

    public void save(User user) {
        persist(user);
    }
}