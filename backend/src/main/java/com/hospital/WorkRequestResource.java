package com.hospital;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.jwt.JsonWebToken;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Path("/api/work-requests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkRequestResource {

    @Inject
    WorkRequestRepository workRequestRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    LeaveRequestRepository leaveRequestRepository;

    @Inject
    JsonWebToken jwt;

    private User.Assigment getCallerAssigment() {
        User caller = userRepository.findByUsername(jwt.getName());
        if (caller != null && caller.role == User.Role.MANAGER && caller.assigment != User.Assigment.NOT_ASSIGNED) {
            return caller.assigment;
        }
        return null;
    }

    private WorkRequestResponse toDto(WorkRequest r) {
        WorkRequestResponse dto = new WorkRequestResponse();
        dto.id = r.getId();
        dto.workType = r.getWorkType();
        dto.shiftType = r.getShiftType();
        dto.status = r.getStatus();
        dto.startDate = r.getStartDate();
        dto.endDate = r.getEndDate();
        dto.createdAt = r.getCreatedAt();
        dto.decidedAt = r.getDecidedAt();
        dto.managerNote = r.getManagerNote();

        if (r.getEmployee() != null) {
            dto.employeeId = r.getEmployee().id;
            dto.employeeUsername = r.getEmployee().username;
        }
        if (r.getDecidedBy() != null) {
            dto.decidedById = r.getDecidedBy().id;
            dto.decidedByUsername = r.getDecidedBy().username;
        }

        return dto;
    }

    @POST
    @Path("/create-request")
    @Transactional
    @RolesAllowed({"EMPLOYEE"})
    public Response create(CreateWorkRequestDTO dto) {
        User employee = userRepository.findById(dto.employeeId);
        if (employee == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Employee not found")
                    .build();
        }

        Shift.ShiftType shiftType;
        try {
            shiftType = Shift.ShiftType.valueOf(dto.role);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Érvénytelen műszak típus: " + dto.role)
                    .build();
        }

        WorkRequest request = new WorkRequest();
        request.setEmployee(employee);
        request.setStartDate(dto.startDate);
        request.setEndDate(dto.endDate != null ? dto.endDate : dto.startDate);
        request.setWorkType(dto.type);
        request.setShiftType(shiftType);

        workRequestRepository.persist(request);

        return Response.status(Response.Status.CREATED).entity(toDto(request)).build();
    }

    @GET
    @Path("/by-employee/{employeeId}")
    @RolesAllowed({"EMPLOYEE", "MANAGER"})
    public List<WorkRequestResponse> listByEmployee(@PathParam("employeeId") Long employeeId) {
        return workRequestRepository
                .list("employee.id", employeeId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GET
    @Path("/all")
    @RolesAllowed({"MANAGER"})
    public List<WorkRequestResponse> listAll() {
        User.Assigment assigment = getCallerAssigment();
        if (assigment != null) {
            return workRequestRepository.findAllByAssigment(assigment).stream().map(this::toDto).toList();
        }
        return workRequestRepository.listAll().stream().map(this::toDto).toList();
    }

    @POST
    @Path("/{id}/approve")
    @Transactional
    @RolesAllowed({"MANAGER"})
    public Response approve(@PathParam("id") Long id, DecisionDTO dto) {
        WorkRequest request = workRequestRepository.findById(id);
        if (request == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        User manager = userRepository.findById(dto.managerId);
        if (manager == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Manager not found")
                    .build();
        }

        Long employeeId = request.getEmployee().getId();
        List<LeaveRequest> conflicts = leaveRequestRepository.findApprovedByEmployeeOverlapping(
                employeeId, request.getStartDate(), request.getEndDate());
        if (!conflicts.isEmpty()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Ütközés: a dolgozónak már van elfogadott szabadság kérelme erre az időszakra.")
                    .build();
        }

        request.approve(manager, dto.note);

        WorkRequestDecisionResponse resp = new WorkRequestDecisionResponse();
        resp.id = request.getId();
        resp.status = request.getStatus();
        resp.managerNote = request.getManagerNote();
        resp.decidedAt = request.getDecidedAt();
        resp.decidedById = manager.id;
        resp.decidedByUsername = manager.username;
        return Response.ok(resp).build();
    }

    @POST
    @Path("/{id}/reject")
    @Transactional
    @RolesAllowed({"MANAGER"})
    public Response reject(@PathParam("id") Long id, DecisionDTO dto) {
        WorkRequest request = workRequestRepository.findById(id);
        if (request == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        User manager = userRepository.findById(dto.managerId);
        if (manager == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Manager not found")
                    .build();
        }

        request.reject(manager, dto.note);

        WorkRequestDecisionResponse resp = new WorkRequestDecisionResponse();
        resp.id = request.getId();
        resp.status = request.getStatus();
        resp.managerNote = request.getManagerNote();
        resp.decidedAt = request.getDecidedAt();
        resp.decidedById = manager.id;
        resp.decidedByUsername = manager.username;
        return Response.ok(resp).build();
    }

    public static class CreateWorkRequestDTO {
        public Long employeeId;
        public LocalDate startDate;
        public LocalDate endDate;
        public WorkRequest.WorkType type;
        public String role;
    }

    public static class DecisionDTO {
        public Long managerId;
        public String note;
    }

    public static class WorkRequestResponse {
        public Long id;
        public Long employeeId;
        public String employeeUsername;
        public LocalDate startDate;
        public LocalDate endDate;
        public WorkRequest.WorkType workType;
        public Shift.ShiftType shiftType;
        public WorkRequest.WorkStatus status;
        public Instant createdAt;
        public Instant decidedAt;
        public Long decidedById;
        public String decidedByUsername;
        public String managerNote;
    }

    public static class WorkRequestDecisionResponse {
        public Long id;
        public WorkRequest.WorkStatus status;
        public String managerNote;
        public Instant decidedAt;
        public Long decidedById;
        public String decidedByUsername;
    }
}