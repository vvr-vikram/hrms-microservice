package com.hrms.employee.controller;

import com.hrms.employee.dto.EmployeeDTO;
import com.hrms.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Employee Management", description = "APIs for managing employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping("/onboard")
    @Operation(summary = "Onboard a new employee")
    public ResponseEntity<EmployeeDTO> onboardEmployee(@Valid @RequestBody EmployeeDTO employeeDTO) {
        log.info("POST /employees/onboard - Onboarding new employee");
        EmployeeDTO createdEmployee = employeeService.onboardEmployee(employeeDTO);
        return new ResponseEntity<>(createdEmployee, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee by ID")
    public ResponseEntity<EmployeeDTO> getEmployeeById(@PathVariable Long id) {
        log.info("GET /employees/{} - Fetching employee", id);
        EmployeeDTO employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(employee);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update employee details")
    public ResponseEntity<EmployeeDTO> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeDTO employeeDTO) {
        log.info("PUT /employees/{} - Updating employee", id);
        EmployeeDTO updatedEmployee = employeeService.updateEmployee(id, employeeDTO);
        return ResponseEntity.ok(updatedEmployee);
    }

    @GetMapping
    @Operation(summary = "Get all employees with pagination")
    public ResponseEntity<Page<EmployeeDTO>> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        log.info("GET /employees - Fetching all employees with pagination");
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<EmployeeDTO> employees = employeeService.getAllEmployees(pageable);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/search")
    @Operation(summary = "Search employees by term")
    public ResponseEntity<Page<EmployeeDTO>> searchEmployees(
            @RequestParam String term,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /employees/search - Searching employees with term: {}", term);
        Pageable pageable = PageRequest.of(page, size);
        Page<EmployeeDTO> employees = employeeService.searchEmployees(term, pageable);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/department/{departmentId}")
    @Operation(summary = "Get employees by department")
    public ResponseEntity<Page<EmployeeDTO>> getEmployeesByDepartment(
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /employees/department/{} - Fetching employees by department", departmentId);
        Pageable pageable = PageRequest.of(page, size);
        Page<EmployeeDTO> employees = employeeService.getEmployeesByDepartment(departmentId, pageable);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active employees")
    public ResponseEntity<Page<EmployeeDTO>> getActiveEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /employees/active - Fetching active employees");
        Pageable pageable = PageRequest.of(page, size);
        Page<EmployeeDTO> employees = employeeService.getActiveEmployees(pageable);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/count")
    @Operation(summary = "Get total employees count")
    public ResponseEntity<Long> getTotalEmployeesCount() {
        log.info("GET /employees/count - Fetching total employees count");
        long count = employeeService.getTotalEmployeesCount();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/active")
    @Operation(summary = "Get active employees count")
    public ResponseEntity<Long> getActiveEmployeesCount() {
        log.info("GET /employees/count/active - Fetching active employees count");
        long count = employeeService.getActiveEmployeesCount();
        return ResponseEntity.ok(count);
    }

    @DeleteMapping("/{id}/terminate")
    @Operation(summary = "Terminate employee")
    public ResponseEntity<Void> terminateEmployee(
            @PathVariable Long id,
            @RequestParam String reason) {
        log.info("DELETE /employees/{}/terminate - Terminating employee with reason: {}", id, reason);
        employeeService.terminateEmployee(id, reason);
        return ResponseEntity.noContent().build();
    }
}
