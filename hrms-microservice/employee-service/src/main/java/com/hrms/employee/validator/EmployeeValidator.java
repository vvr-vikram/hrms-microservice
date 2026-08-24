package com.hrms.employee.validator;

import com.hrms.employee.dto.EmployeeDTO;
import com.hrms.employee.exception.DuplicateEmployeeException;
import com.hrms.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmployeeValidator {

    private final EmployeeRepository employeeRepository;

    public void validateForOnboarding(EmployeeDTO employeeDTO) {
        if (employeeDTO.getDepartmentId() == null || employeeDTO.getDepartmentId() < 1 || employeeDTO.getDepartmentId() > 4) {
            throw new IllegalArgumentException("Invalid departmentId. Allowed values are: 1 (Engineering), 2 (HR), 3 (Finance), 4 (Sales).");
        }

        if (employeeRepository.existsByEmail(employeeDTO.getEmail())) {
            throw new DuplicateEmployeeException("Employee with email " + employeeDTO.getEmail() + " already exists");
        }
        
        if (employeeDTO.getPhone() != null && employeeRepository.existsByPhone(employeeDTO.getPhone())) {
            throw new DuplicateEmployeeException("Employee with phone " + employeeDTO.getPhone() + " already exists");
        }
    }
}
