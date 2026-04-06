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

import io.smallrye.jwt.build.Jwt;
import java.time.Duration;
import java.util.Set;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserRepository userRepository;

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class LoginResponse {
        public String token;
        public long expiresInSeconds;
        public String username;
        public String role;
        public Long id;
        public String assigment;

        public LoginResponse(String token, long expiresInSeconds, String username, String role, Long id, String assigment) {
            this.token = token;
            this.expiresInSeconds = expiresInSeconds;
            this.username = username;
            this.role = role;
            this.id = id;
            this.assigment = assigment;
        }
    }

    @POST
    @Path("/register")
    @Transactional
    public Response register(User user) {

        if (userRepository.findByUsername(user.username) != null) {
            return Response.status(Response.Status.CONFLICT).build();
        }

        if (user.role == null) {
            user.role = User.Role.EMPLOYEE;
        }


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
        user.assigment = User.Assigment.NOT_ASSIGNED;
        userRepository.save(user);

        return Response.ok(user).build();
    }

    @POST
    @Path("/login")
    @Transactional
    public Response login(LoginRequest user) {

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

        long expiresInSeconds = 3 * 60 * 60;
        String token = Jwt.issuer("hospital-app")
                .upn(dbUser.username)
                .groups(Set.of(dbUser.role.name()))
                .expiresIn(Duration.ofSeconds(expiresInSeconds))
                .sign();

        return Response.ok(new LoginResponse(token, expiresInSeconds, dbUser.username, dbUser.role.name(), dbUser.id, dbUser.assigment.name())).build();
    }

}
