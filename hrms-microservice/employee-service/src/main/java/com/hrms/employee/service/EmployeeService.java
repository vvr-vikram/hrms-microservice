package com.hrms.employee.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.employee.dto.EmployeeDTO;
import com.hrms.employee.entity.AuditLog;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.exception.DuplicateResourceException;
import com.hrms.employee.exception.ResourceNotFoundException;
import com.hrms.employee.repository.AuditLogRepository;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.employee.validator.EmployeeValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AuditLogRepository auditLogRepository;
    private final EmployeeValidator employeeValidator;
    private final ObjectMapper objectMapper;

    @Transactional
    public EmployeeDTO onboardEmployee(EmployeeDTO employeeDTO) {
        log.info("Onboarding new employee: {}", employeeDTO.getEmail());
        
        // Validate employee data
        employeeValidator.validateForOnboarding(employeeDTO);
        
        // Check for duplicate email and phone
        if (employeeRepository.existsByEmail(employeeDTO.getEmail())) {
            throw new DuplicateResourceException("Employee with email " + employeeDTO.getEmail() + " already exists");
        }
        
        if (employeeRepository.existsByPhone(employeeDTO.getPhone())) {
            throw new DuplicateResourceException("Employee with phone " + employeeDTO.getPhone() + " already exists");
        }
        
        // Generate employee code
        String employeeCode = generateEmployeeCode();
        
        // Set joining date if not provided
        if (employeeDTO.getJoiningDate() == null) {
            employeeDTO.setJoiningDate(LocalDate.now());
        }
        
        Employee employee = convertToEntity(employeeDTO);
        employee.setEmployeeCode(employeeCode);
        employee.setRole(employeeDTO.getRole() != null ? employeeDTO.getRole() : "ROLE_EMPLOYEE");
        employee.setIsActive(true);
        
        if (employeeDTO.getDepartmentId() != null) {
            employee.setDepartmentId(employeeDTO.getDepartmentId());
        }
        
        if (employeeDTO.getReportingManagerId() != null) {
            employee.setReportingManagerId(employeeDTO.getReportingManagerId());
        }
        
        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Employee onboarded successfully with code: {}", employeeCode);
        
        // Create audit log
        createAuditLog(savedEmployee, "CREATE_EMPLOYEE", null, "Employee created");
        
        return convertToDTO(savedEmployee);
    }

    public EmployeeDTO getEmployeeById(Long id) {
        log.debug("Fetching employee with id: {}", id);
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return convertToDTO(employee);
    }

    public Page<EmployeeDTO> getAllEmployees(Pageable pageable) {
        log.debug("Fetching all employees with pagination");
        return employeeRepository.findAll(pageable)
            .map(this::convertToDTO);
    }

    public Page<EmployeeDTO> searchEmployees(String searchTerm, Pageable pageable) {
        log.debug("Searching employees with term: {}", searchTerm);
        return employeeRepository.searchEmployees(searchTerm, pageable)
            .map(this::convertToDTO);
    }

    @Transactional
    public EmployeeDTO updateEmployee(Long id, EmployeeDTO employeeDTO) {
        log.info("Updating employee with id: {}", id);
        
        Employee existingEmployee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        
        String oldState = existingEmployee.toString();
        
        // Update fields
        existingEmployee.setFirstName(employeeDTO.getFirstName());
        existingEmployee.setLastName(employeeDTO.getLastName());
        existingEmployee.setPhone(employeeDTO.getPhone());
        existingEmployee.setAddress(employeeDTO.getAddress());
        existingEmployee.setPosition(employeeDTO.getPosition());
        
        if (employeeDTO.getBaseSalary() != null) {
            existingEmployee.setBaseSalary(BigDecimal.valueOf(employeeDTO.getBaseSalary()));
        }
        
        if (employeeDTO.getJoiningDate() != null) {
            existingEmployee.setJoiningDate(employeeDTO.getJoiningDate());
        }
        
        if (employeeDTO.getDateOfBirth() != null) {
            existingEmployee.setDateOfBirth(employeeDTO.getDateOfBirth());
        }
        
        if (employeeDTO.getRole() != null && !employeeDTO.getRole().isEmpty()) {
            existingEmployee.setRole(employeeDTO.getRole());
        }
        
        if (employeeDTO.getDepartmentId() != null) {
            existingEmployee.setDepartmentId(employeeDTO.getDepartmentId());
        }
        
        if (employeeDTO.getReportingManagerId() != null) {
            existingEmployee.setReportingManagerId(employeeDTO.getReportingManagerId());
        }
        
        Employee updatedEmployee = employeeRepository.save(existingEmployee);
        
        // Create audit log
        createAuditLog(updatedEmployee, "UPDATE_EMPLOYEE", oldState, "Employee updated");
        
        return convertToDTO(updatedEmployee);
    }

    @Transactional
    public void terminateEmployee(Long id, String reason) {
        log.info("Terminating employee with id: {}", id);
        
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        
        String oldState = employee.toString();
        employee.setIsActive(false);
        
        Employee savedEmployee = employeeRepository.save(employee);
        
        createAuditLog(savedEmployee, "TERMINATE_EMPLOYEE", oldState, 
            "Terminated with reason: " + reason);
    }

    public Page<EmployeeDTO> getEmployeesByDepartment(Long departmentId, Pageable pageable) {
        log.debug("Fetching employees for department: {}", departmentId);
        return employeeRepository.findByDepartmentId(departmentId, pageable)
            .map(this::convertToDTO);
    }

    public Page<EmployeeDTO> getActiveEmployees(Pageable pageable) {
        log.debug("Fetching active employees");
        return employeeRepository.findByIsActive(true, pageable)
            .map(this::convertToDTO);
    }

    public long getTotalEmployeesCount() {
        log.debug("Counting total employees");
        return employeeRepository.count();
    }

    public long getActiveEmployeesCount() {
        log.debug("Counting active employees");
        return employeeRepository.countByIsActive(true);
    }

    private String generateEmployeeCode() {
        String code;
        do {
            code = "EMP" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (employeeRepository.existsByEmployeeCode(code));
        return code;
    }

    private Employee convertToEntity(EmployeeDTO dto) {
        Employee employee = new Employee();
        employee.setEmployeeCode(dto.getEmployeeCode());
        employee.setFirstName(dto.getFirstName());
        employee.setLastName(dto.getLastName());
        employee.setEmail(dto.getEmail());
        employee.setPhone(dto.getPhone());
        employee.setAddress(dto.getAddress());
        employee.setPosition(dto.getPosition());
        
        if (dto.getBaseSalary() != null) {
            employee.setBaseSalary(BigDecimal.valueOf(dto.getBaseSalary()));
        }
        
        employee.setJoiningDate(dto.getJoiningDate());
        employee.setDateOfBirth(dto.getDateOfBirth());
        employee.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        
        return employee;
    }

    private EmployeeDTO convertToDTO(Employee entity) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(entity.getId());
        dto.setEmployeeCode(entity.getEmployeeCode());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setAddress(entity.getAddress());
        dto.setPosition(entity.getPosition());
        
        if (entity.getBaseSalary() != null) {
            dto.setBaseSalary(entity.getBaseSalary().doubleValue());
        }
        
        dto.setJoiningDate(entity.getJoiningDate());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setIsActive(entity.getIsActive());
        
        if (entity.getRole() != null) {
            dto.setRole(entity.getRole());
        }
        
        dto.setDepartmentId(entity.getDepartmentId());
        dto.setReportingManagerId(entity.getReportingManagerId());
        
        // Stubs for names, can be loaded or set if needed.
        if (entity.getDepartmentId() != null) {
            dto.setDepartmentName("Department " + entity.getDepartmentId());
        }
        if (entity.getReportingManagerId() != null) {
            dto.setReportingManagerName("Manager " + entity.getReportingManagerId());
        }
        
        return dto;
    }

    private void createAuditLog(Employee employee, String action, String oldValue, String newValue) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .entityName("Employee")
                    .entityId(employee.getId())
                    .action(action)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .performedBy("SYSTEM")
                    .ipAddress("127.0.0.1")
                    .build();
            
            auditLogRepository.save(auditLog);
            log.debug("Audit log created for action: {} on employee: {}", action, employee.getId());
        } catch (Exception e) {
            log.error("Error creating audit log: {}", e.getMessage());
        }
    }
}
