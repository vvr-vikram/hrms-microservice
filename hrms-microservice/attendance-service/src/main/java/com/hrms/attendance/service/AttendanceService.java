package com.hrms.attendance.service;

import com.hrms.attendance.dto.AttendanceDTO;
import com.hrms.attendance.dto.EmployeeDTO;
import com.hrms.attendance.entity.Attendance;
import com.hrms.attendance.exception.ResourceNotFoundException;
import com.hrms.attendance.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final RestTemplate restTemplate;

    private static final double STANDARD_WORKING_HOURS = 8.0;

    private EmployeeDTO getEmployeeDetails(Long employeeId) {
        try {
            ResponseEntity<EmployeeDTO> response = restTemplate.getForEntity(
                "http://employee-service/employees/" + employeeId, 
                EmployeeDTO.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Failed to fetch employee details for id {}: {}", employeeId, e.getMessage());
        }
        return null;
    }

    public AttendanceDTO markCheckIn(Long employeeId) {
        log.info("Marking check-in for employee: {}", employeeId);
        
        EmployeeDTO employee = getEmployeeDetails(employeeId);
        if (employee == null) {
            throw new ResourceNotFoundException("Employee not found");
        }
        
        LocalDate today = LocalDate.now();
        
        // Check if attendance already exists for today
        Attendance existingAttendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
            .orElse(null);
        
        if (existingAttendance != null && existingAttendance.getCheckInTime() != null) {
            throw new RuntimeException("Check-in already marked for today");
        }
        
        Attendance attendance;
        if (existingAttendance == null) {
            attendance = new Attendance();
            attendance.setEmployeeId(employeeId);
            attendance.setDate(today);
        } else {
            attendance = existingAttendance;
        }
        
        LocalDateTime checkInTime = LocalDateTime.now();
        attendance.setCheckInTime(checkInTime);
        
        // Determine status based on check-in time
        if (checkInTime.getHour() < 9) {
            attendance.setStatus("PRESENT");
        } else if (checkInTime.getHour() < 10) {
            attendance.setStatus("LATE");
        } else {
            attendance.setStatus("HALF_DAY");
        }
        
        Attendance savedAttendance = attendanceRepository.save(attendance);
        log.info("Check-in recorded for employee: {} at {}", employeeId, checkInTime);
        
        return convertToDTO(savedAttendance, employee.getFullName());
    }

    public AttendanceDTO markCheckOut(Long employeeId) {
        log.info("Marking check-out for employee: {}", employeeId);
        
        EmployeeDTO employee = getEmployeeDetails(employeeId);
        if (employee == null) {
            throw new ResourceNotFoundException("Employee not found");
        }
        
        LocalDate today = LocalDate.now();
        
        Attendance attendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
            .orElseThrow(() -> new RuntimeException("No check-in record found for today"));
        
        if (attendance.getCheckOutTime() != null) {
            throw new RuntimeException("Check-out already marked for today");
        }
        
        LocalDateTime checkOutTime = LocalDateTime.now();
        attendance.setCheckOutTime(checkOutTime);
        
        // Calculate total working hours
        if (attendance.getCheckInTime() != null) {
            Duration duration = Duration.between(attendance.getCheckInTime(), checkOutTime);
            double workingHours = duration.toHours() + duration.toMinutesPart() / 60.0;
            attendance.setTotalWorkingHours(workingHours);
            
            if (workingHours > STANDARD_WORKING_HOURS) {
                attendance.setOvertimeHours(workingHours - STANDARD_WORKING_HOURS);
            }
        }
        
        Attendance savedAttendance = attendanceRepository.save(attendance);
        log.info("Check-out recorded for employee: {} at {}", employeeId, checkOutTime);
        
        return convertToDTO(savedAttendance, employee.getFullName());
    }

    public List<AttendanceDTO> getEmployeeAttendance(Long employeeId, LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching attendance for employee: {} from {} to {}", employeeId, startDate, endDate);
        
        EmployeeDTO employee = getEmployeeDetails(employeeId);
        String employeeName = employee != null ? employee.getFullName() : "Unknown Employee";

        return attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, startDate, endDate)
            .stream()
            .map(entity -> convertToDTO(entity, employeeName))
            .toList();
    }

    public AttendanceDTO updateAttendance(Long attendanceId, AttendanceDTO attendanceDTO) {
        log.info("Updating attendance record: {}", attendanceId);
        
        Attendance attendance = attendanceRepository.findById(attendanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found"));
        
        attendance.setStatus(attendanceDTO.getStatus());
        attendance.setRemarks(attendanceDTO.getRemarks());
        
        if (attendanceDTO.getCheckInTime() != null) {
            attendance.setCheckInTime(attendanceDTO.getCheckInTime());
        }
        
        if (attendanceDTO.getCheckOutTime() != null) {
            attendance.setCheckOutTime(attendanceDTO.getCheckOutTime());
        }
        
        if (attendanceDTO.getTotalWorkingHours() != null) {
            attendance.setTotalWorkingHours(attendanceDTO.getTotalWorkingHours());
        }
        
        if (attendanceDTO.getOvertimeHours() != null) {
            attendance.setOvertimeHours(attendanceDTO.getOvertimeHours());
        }
        
        Attendance updatedAttendance = attendanceRepository.save(attendance);
        
        EmployeeDTO employee = getEmployeeDetails(updatedAttendance.getEmployeeId());
        String employeeName = employee != null ? employee.getFullName() : "Unknown Employee";
        
        return convertToDTO(updatedAttendance, employeeName);
    }

    private AttendanceDTO convertToDTO(Attendance entity, String employeeName) {
        return AttendanceDTO.builder()
            .id(entity.getId())
            .employeeId(entity.getEmployeeId())
            .employeeName(employeeName)
            .date(entity.getDate())
            .checkInTime(entity.getCheckInTime())
            .checkOutTime(entity.getCheckOutTime())
            .totalWorkingHours(entity.getTotalWorkingHours())
            .overtimeHours(entity.getOvertimeHours())
            .status(entity.getStatus())
            .remarks(entity.getRemarks())
            .build();
    }
}
