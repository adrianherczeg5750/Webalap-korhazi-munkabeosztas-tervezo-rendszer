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
import java.util.Set;

@Path("/api/leave-requests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LeaveRequestResource {

    @Inject
    LeaveRequestRepository leaveRequestRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    WorkRequestRepository workRequestRepository;

    @Inject
    JsonWebToken jwt;

    private User.Assigment getCallerAssigment() {
        User caller = userRepository.findByUsername(jwt.getName());
        if (caller != null && caller.role == User.Role.MANAGER && caller.assigment != User.Assigment.NOT_ASSIGNED) {
            return caller.assigment;
        }
        return null;
    }

    private LeaveRequestResponse toDto(LeaveRequest r) {
        LeaveRequestResponse dto = new LeaveRequestResponse();
        dto.id = r.getId();
        dto.type = r.getType();
        dto.status = r.getStatus();
        dto.startDate = r.getStartDate();
        dto.endDate = r.getEndDate();
        dto.dates = r.getDates();
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
    public Response create(CreateLeaveRequestDTO dto) {

        User employee = userRepository.findById(dto.employeeId);
        if (employee == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Employee not found")
                    .build();
        }

        LeaveRequest request = new LeaveRequest();
        request.setEmployee(employee);
        request.setStartDate(dto.startDate);
        request.setEndDate(dto.endDate);
        request.setType(dto.type);

        leaveRequestRepository.persist(request);

        return Response.status(Response.Status.CREATED).entity(toDto(request)).build();
    }

    @GET
    @Path("/pending")
    @RolesAllowed({"MANAGER"})
    public List<LeaveRequestResponse> getPending() {
        User.Assigment assigment = getCallerAssigment();
        if (assigment != null) {
            return leaveRequestRepository.findPendingByAssigment(assigment).stream().map(this::toDto).toList();
        }
        return leaveRequestRepository
                .list("status", LeaveRequest.LeaveStatus.PENDING)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GET
    @Path("/by-employee/{employeeId}")
    @RolesAllowed({"EMPLOYEE", "MANAGER"})
    public List<LeaveRequestResponse> listByEmployee(@PathParam("employeeId") Long employeeId) {
        return leaveRequestRepository
                .list("employee.id", employeeId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GET
    @Path("/all")
    @RolesAllowed({"MANAGER"})
    public List<LeaveRequestResponse> listAll() {
        User.Assigment assigment = getCallerAssigment();
        if (assigment != null) {
            return leaveRequestRepository.findAllByAssigment(assigment).stream().map(this::toDto).toList();
        }
        return leaveRequestRepository.listAll().stream().map(this::toDto).toList();
    }

    @POST
    @Path("/{id}/approve")
    @Transactional
    @RolesAllowed({"MANAGER"})
    public Response approve(@PathParam("id") Long id, DecisionDTO dto) {

        LeaveRequest request = leaveRequestRepository.findById(id);
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
        List<WorkRequest> conflicts = workRequestRepository.findApprovedByEmployeeOverlapping(
                employeeId, request.getStartDate(), request.getEndDate());
        if (!conflicts.isEmpty()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Ütközés: a dolgozónak már van elfogadott munkavégzési kérelme erre az időszakra.")
                    .build();
        }

        request.approve(manager, dto.note);

        LeaveRequestDecisionResponse resp = new LeaveRequestDecisionResponse();
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

        LeaveRequest request = leaveRequestRepository.findById(id);
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

        LeaveRequestDecisionResponse resp = new LeaveRequestDecisionResponse();
        resp.id = request.getId();
        resp.status = request.getStatus();
        resp.managerNote = request.getManagerNote();
        resp.decidedAt = request.getDecidedAt();
        resp.decidedById = manager.id;
        resp.decidedByUsername = manager.username;
        return Response.ok(resp).build();
    }

    public static class CreateLeaveRequestDTO {
        public Long employeeId;
        public LocalDate startDate;
        public LocalDate endDate;
        public LeaveRequest.LeaveType type;
    }

    public static class DecisionDTO {
        public Long managerId;
        public String note;
    }

    /** Safe response DTO for list endpoints (avoids serializing LAZY relations) */
    public static class LeaveRequestResponse {
        public Long id;

        public Long employeeId;
        public String employeeUsername;

        public LocalDate startDate;
        public LocalDate endDate;
        public Set<LocalDate> dates;

        public LeaveRequest.LeaveType type;
        public LeaveRequest.LeaveStatus status;

        public Instant createdAt;
        public Instant decidedAt;

        public Long decidedById;
        public String decidedByUsername;

        public String managerNote;
    }

    /** Response DTO for approve/reject */
    public static class LeaveRequestDecisionResponse {
        public Long id;
        public LeaveRequest.LeaveStatus status;
        public String managerNote;
        public Instant decidedAt;
        public Long decidedById;
        public String decidedByUsername;
    }
}
