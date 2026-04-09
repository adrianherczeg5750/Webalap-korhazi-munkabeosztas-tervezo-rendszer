package com.hospital;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

import java.time.LocalDate;
import java.util.List;

@Path("/api/shifts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShiftResource {

    @Inject
    ShiftRepository shiftRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    ShiftGenerationService shiftService;

    @Inject
    JsonWebToken jwt;

    private User.Assigment getCallerAssigment() {
        User caller = userRepository.findByUsername(jwt.getName());
        if (caller != null && caller.role == User.Role.MANAGER && caller.assigment != User.Assigment.NOT_ASSIGNED) {
            return caller.assigment;
        }
        return null;
    }

    @GET
    public List<Shift> list() {
        User.Assigment assigment = getCallerAssigment();
        if (assigment != null) {
            return shiftRepository.findByEmployeeAssigment(assigment);
        }
        return shiftRepository.listAll();
    }

    @POST
    @Path("/generate")
    @RolesAllowed({"MANAGER"})
    public Response generate(GenerateRequest req) {
        if (req == null || req.month == null || req.month.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        User caller = userRepository.findByUsername(jwt.getName());
        if (caller == null || caller.assigment == User.Assigment.NOT_ASSIGNED) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Csak beosztott manager generálhat beosztást.")
                    .build();
        }
        int staffPerShift = req.getStaffPerShift();
        if (staffPerShift < 2 || staffPerShift > 4) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("staffPerShift must be between 2 and 4.")
                    .build();
        }
        shiftService.generateForMonth(req.month, caller.assigment, staffPerShift);
        return Response.ok().build();
    }



    public static class GenerateRequest {

        private String month;
        private int staffPerShift = 2;

        public String getMonth() {
            return month;
        }

        public void setMonth(String month) {
            this.month = month;
        }

        public int getStaffPerShift() {
            return staffPerShift;
        }

        public void setStaffPerShift(int staffPerShift) {
            this.staffPerShift = staffPerShift;
        }
    }


    @POST
    @Transactional
    @RolesAllowed({"MANAGER"})
    public Response create(CreateShiftDTO dto) {
        if (dto == null) {
            throw new BadRequestException("Hiányzó request body");
        }
        if (dto.userId == null) {
            throw new BadRequestException("userId kötelező");
        }
        if (dto.date == null) {
            throw new BadRequestException("date kötelező");
        }
        if (dto.shiftType == null) {
            throw new BadRequestException("shiftType kötelező");
        }

        User user = userRepository.findById(dto.userId);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("User not found")
                    .build();
        }

        boolean exists = shiftRepository.count("user = ?1 and date = ?2", user, dto.date) > 0;
        if (exists) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Erre a napra már van műszak rögzítve ennél a felhasználónál")
                    .build();
        }

        Shift shift = new Shift();
        shift.setUser(user);
        shift.setDate(dto.date);
        shift.setShiftType(dto.shiftType);

        shiftRepository.persist(shift);
        return Response.status(Response.Status.CREATED).entity(shift).build();
    }

    public static class CreateShiftDTO {
        public Long userId;
        public LocalDate date;
        public Shift.ShiftType shiftType;
    }
}
