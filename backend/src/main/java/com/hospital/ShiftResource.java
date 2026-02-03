package com.hospital;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/shifts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShiftResource {

    @Inject
    ShiftRepository shiftRepository;

    @Inject
    UserRepository userRepository;

    @GET
    public List<Shift> list() {
        return shiftRepository.listAll();
    }

    @POST
    @Transactional
    public Shift create(Shift shift) {
        if (shift.getStartAtDate() == null || shift.getEndAtDate() == null) {
            throw new BadRequestException("Kezdés és befejezés kötelező");
        }

        if (shift.getEndAtDate().isBefore(shift.getStartAtDate())) {
            throw new BadRequestException("A befejezés nem lehet korábban, mint a kezdés");
        }
        User user = userRepository.findById(1L);
        shift.setUser(user);

        shiftRepository.persist(shift);
        return shift;
    }
}
