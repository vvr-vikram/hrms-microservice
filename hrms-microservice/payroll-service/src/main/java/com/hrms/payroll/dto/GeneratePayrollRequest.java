package com.hrms.payroll.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class GeneratePayrollRequest {
	
	@NotNull(message = "Employee Id cannot be null")
	@Positive(message = "Employee Id must be positive number")
	private Long employeeId;
	
	@Min(value = 2000, message = "Year must be 200 or greater")
	@Max(value = 2100, message = "Year must be 2100 or less")
	private int year;
	
	@Min(value = 1, message = "Month must be between 1 and 12")
	@Max(value = 12, message = "Month must be between 1 and 12")
	private int month;

}
