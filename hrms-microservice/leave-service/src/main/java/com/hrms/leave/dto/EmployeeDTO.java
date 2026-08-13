package com.hrms.leave.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {
    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private LocalDate dateOfBirth;
    private String gender;
    private String position;
    private Double baseSalary;
    private LocalDate joiningDate;
    private String role;
    private Boolean isActive;
    private Long departmentId;
    private String departmentName;
    private Long reportingManagerId;
    private String reportingManagerName;

    public String getFullName() {
        if (firstName == null && lastName == null) return "Unknown Employee";
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
}
