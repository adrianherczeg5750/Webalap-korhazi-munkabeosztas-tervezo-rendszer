package com.hospital;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import jakarta.transaction.Transactional;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserRepository userRepository;

    @POST
    @Path("/register")
    @Transactional
    public Response register(User user) {

        if (userRepository.findByUsername(user.username) != null) {
            return Response.status(Response.Status.CONFLICT).build();
        }

        if (user.role == null) user.role = "USER";

        String hashed;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(user.password.getBytes());
            hashed = Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Hashing error")
                    .build();
        }

        user.password = hashed;
        userRepository.save(user);

        return Response.ok(user).build();
    }

    @POST
    @Path("/login")
    @Transactional
    public Response login(User user) {

        User dbUser = userRepository.findByUsername(user.username);
        if (dbUser == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(user.password.getBytes());
            String hashed = Base64.getEncoder().encodeToString(hashBytes);

            if (!hashed.equals(dbUser.password)) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

        } catch (NoSuchAlgorithmException e) {
            return Response.serverError().build();
        }

        return Response.ok(user).build();
    }

}
