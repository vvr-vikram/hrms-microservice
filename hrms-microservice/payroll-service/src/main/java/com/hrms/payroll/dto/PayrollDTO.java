package com.hrms.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollDTO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private Integer year;
    private Integer month;
    private Double basicSalary;
    private Double houseRentAllowance;
    private Double dearnessAllowance;
    private Double travelAllowance;
    private Double medicalAllowance;
    private Double specialAllowance;
    private Double totalEarnings;
    private Double providentFund;
    private Double professionalTax;
    private Double incomeTax;
    private Double loanDeduction;
    private Double totalDeductions;
    private Double netSalary;
    private Integer totalPresentDays;
    private Integer totalAbsentDays;
    private Integer totalLeaveDays;
    private String paymentStatus;
    private String paymentMode;
    private String remarks;
}
