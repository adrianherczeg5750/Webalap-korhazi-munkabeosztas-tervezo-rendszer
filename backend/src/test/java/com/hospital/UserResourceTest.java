package com.hospital;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@QuarkusTest
public class UserResourceTest {

    @Inject
    UserResource userResource;

    @InjectMock
    UserRepository userRepository;

    private User testUser;
    private UserResource.LoginRequest loginRequest;

    @BeforeEach
    public void setup() {
        testUser = new User();
        testUser.username = "testuser";
        testUser.role = User.Role.EMPLOYEE;
        testUser.assigment = User.Assigment.EMERGENCY;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest("password".getBytes());
            testUser.password = Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        loginRequest = new UserResource.LoginRequest();
        loginRequest.username = testUser.username;
        loginRequest.password = "password";
    }


    @Test
    public void testSaveUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(null);
        Response response = userResource.register(testUser);
        assertEquals(200, response.getStatus());
        verify(userRepository).save(testUser);
    }

    @Test
    public void testSaveUserConflict() {
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        Response response = userResource.register(testUser);
        assertEquals(409, response.getStatus());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testLoginUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        Response response = userResource.login(loginRequest);
        assertEquals(200, response.getStatus());
        UserResource.LoginResponse body = (UserResource.LoginResponse) response.getEntity();
        assertNotNull(body.token);
        verify(userRepository).findByUsername(loginRequest.username);
    }

    @Test
    public void testLoginUserNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(null);
        Response response = userResource.login(loginRequest);
        assertEquals(401, response.getStatus());
        assertNull(response.getEntity());
        verify(userRepository).findByUsername(loginRequest.username);
    }
}
