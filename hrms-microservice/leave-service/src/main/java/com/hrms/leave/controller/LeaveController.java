package com.hrms.leave.controller;

import com.hrms.leave.dto.LeaveDTO;
import com.hrms.leave.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Leave Management", description = "APIs for managing leave applications")
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping("/apply")
    @Operation(summary = "Apply for leave")
    public ResponseEntity<LeaveDTO> applyForLeave(@Valid @RequestBody LeaveDTO leaveDTO) {
        log.info("POST /api/leaves/apply - Applying for leave for employee: {}", leaveDTO.getEmployeeId());
        LeaveDTO leave = leaveService.applyForLeave(leaveDTO);
        return new ResponseEntity<>(leave, HttpStatus.CREATED);
    }

    @PutMapping("/{leaveId}/approve")
    @Operation(summary = "Approve leave application")
    public ResponseEntity<LeaveDTO> approveLeave(
            @PathVariable Long leaveId,
            @RequestParam String approvedBy) {
        log.info("PUT /api/leaves/{}/approve - Approving leave by: {}", leaveId, approvedBy);
        LeaveDTO leave = leaveService.approveLeave(leaveId, approvedBy);
        return ResponseEntity.ok(leave);
    }

    @PutMapping("/{leaveId}/reject")
    @Operation(summary = "Reject leave application")
    public ResponseEntity<LeaveDTO> rejectLeave(
            @PathVariable Long leaveId,
            @RequestParam String rejectedBy,
            @RequestParam String reason) {
        log.info("PUT /api/leaves/{}/reject - Rejecting leave by: {}, reason: {}", leaveId, rejectedBy, reason);
        LeaveDTO leave = leaveService.rejectLeave(leaveId, rejectedBy, reason);
        return ResponseEntity.ok(leave);
    }

    @PutMapping("/{leaveId}/cancel")
    @Operation(summary = "Cancel leave application")
    public ResponseEntity<LeaveDTO> cancelLeave(@PathVariable Long leaveId) {
        log.info("PUT /api/leaves/{}/cancel - Cancelling leave", leaveId);
        LeaveDTO leave = leaveService.cancelLeave(leaveId);
        return ResponseEntity.ok(leave);
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get employee leaves")
    public ResponseEntity<List<LeaveDTO>> getEmployeeLeaves(@PathVariable Long employeeId) {
        log.info("GET /api/leaves/employee/{} - Fetching employee leaves", employeeId);
        List<LeaveDTO> leaves = leaveService.getEmployeeLeaves(employeeId);
        return ResponseEntity.ok(leaves);
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending leave requests")
    public ResponseEntity<List<LeaveDTO>> getPendingLeaves() {
        log.info("GET /api/leaves/pending - Fetching pending leaves");
        List<LeaveDTO> leaves = leaveService.getPendingLeaves();
        return ResponseEntity.ok(leaves);
    }
}
