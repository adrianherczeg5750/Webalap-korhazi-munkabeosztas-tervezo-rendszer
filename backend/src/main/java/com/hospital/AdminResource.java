package com.hospital;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Path("/api/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN"})
public class AdminResource {

    @Inject
    UserRepository userRepository;

    @Inject
    ShiftRepository shiftRepository;

    @Inject
    LeaveRequestRepository leaveRequestRepository;

    @Inject
    WorkRequestRepository workRequestRepository;

    @GET
    @Path("/users")
    public List<UserDto> listUsers() {
        return userRepository.listAll().stream()
                .map(u -> new UserDto(u.id, u.username, u.role, u.assigment))
                .toList();
    }

    @PUT
    @Path("/users/{id}/role")
    @Transactional
    public Response changeRole(@PathParam("id") Long id, ChangeRoleDto dto) {
        User user = userRepository.findById(id);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
        }
        try {
            user.role = User.Role.valueOf(dto.role);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Érvénytelen szerepkör: " + dto.role)
                    .build();
        }
        if (user.role == User.Role.ADMIN) {
            user.assigment = User.Assigment.NOT_ASSIGNED;
        }
        return Response.ok(new UserDto(user.id, user.username, user.role, user.assigment)).build();
    }

    @PUT
    @Path("/users/{id}/assigment")
    @Transactional
    public Response changeAssigment(@PathParam("id") Long id, ChangeAssigmentDto dto) {
        User user = userRepository.findById(id);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
        }
        try {
            user.assigment = User.Assigment.valueOf(dto.assigment);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Érvénytelen beosztás: " + dto.assigment)
                    .build();
        }
        return Response.ok(new UserDto(user.id, user.username, user.role, user.assigment)).build();
    }

    @DELETE
    @Path("/users/{id}")
    @Transactional
    public Response deleteUser(@PathParam("id") Long id) {
        User user = userRepository.findById(id);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
        }

        long shiftCount = shiftRepository.count("user.id = ?1", id);
        if (shiftCount > 0) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("A felhasználónak beosztott munkája van, nem törölhető.")
                    .build();
        }
        
        List<LeaveRequest> leaveRequests = leaveRequestRepository.list("employee.id", id);
        for (LeaveRequest lr : leaveRequests) {
            leaveRequestRepository.delete(lr);
        }

        workRequestRepository.delete("employee.id", id);
        userRepository.delete(user);

        return Response.noContent().build();
    }

    @DELETE
    @Path("/shifts/month/{month}")
    @Transactional
    public Response deleteShiftsForMonth(@PathParam("month") String month) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(month);
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Érvénytelen hónap formátum. Pl: 2025-03")
                    .build();
        }
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        shiftRepository.deleteByDateBetween(start, end);
        return Response.noContent().build();
    }

    public static class UserDto {
        public Long id;
        public String username;
        public User.Role role;
        public User.Assigment assigment;

        public UserDto(Long id, String username, User.Role role, User.Assigment assigment) {
            this.id = id;
            this.username = username;
            this.role = role;
            this.assigment = assigment;
        }
    }

    public static class ChangeRoleDto {
        public String role;
    }

    public static class ChangeAssigmentDto {
        public String assigment;
    }
}