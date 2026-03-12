package com.hospital;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

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

    public List<User> findAllActive(){
        return listAll();
    }
}