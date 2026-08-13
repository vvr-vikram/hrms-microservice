package com.hrms.attendance.controller;

import com.hrms.attendance.dto.AttendanceDTO;
import com.hrms.attendance.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Attendance Management", description = "APIs for managing attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/checkin/{employeeId}")
    @Operation(summary = "Mark check-in for employee")
    public ResponseEntity<AttendanceDTO> markCheckIn(@PathVariable Long employeeId) {
        log.info("POST /api/attendance/checkin/{} - Marking check-in", employeeId);
        AttendanceDTO attendance = attendanceService.markCheckIn(employeeId);
        return ResponseEntity.ok(attendance);
    }

    @PostMapping("/checkout/{employeeId}")
    @Operation(summary = "Mark check-out for employee")
    public ResponseEntity<AttendanceDTO> markCheckOut(@PathVariable Long employeeId) {
        log.info("POST /api/attendance/checkout/{} - Marking check-out", employeeId);
        AttendanceDTO attendance = attendanceService.markCheckOut(employeeId);
        return ResponseEntity.ok(attendance);
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get employee attendance by date range")
    public ResponseEntity<List<AttendanceDTO>> getEmployeeAttendance(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/attendance/employee/{} - Fetching attendance from {} to {}", 
                employeeId, startDate, endDate);
        List<AttendanceDTO> attendance = attendanceService.getEmployeeAttendance(employeeId, startDate, endDate);
        return ResponseEntity.ok(attendance);
    }

    @PutMapping("/{attendanceId}")
    @Operation(summary = "Update attendance record")
    public ResponseEntity<AttendanceDTO> updateAttendance(
            @PathVariable Long attendanceId,
            @RequestBody AttendanceDTO attendanceDTO) {
        log.info("PUT /api/attendance/{} - Updating attendance record", attendanceId);
        AttendanceDTO updatedAttendance = attendanceService.updateAttendance(attendanceId, attendanceDTO);
        return ResponseEntity.ok(updatedAttendance);
    }
}
