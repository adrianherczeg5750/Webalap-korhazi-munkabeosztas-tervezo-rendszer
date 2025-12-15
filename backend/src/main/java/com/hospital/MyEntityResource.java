package com.hospital;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/my-entities")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MyEntityResource {

    @Inject
    UserRepository userRepository;

    @GET
    public List<User> findAll() {
        return userRepository.listAll();
    }
}