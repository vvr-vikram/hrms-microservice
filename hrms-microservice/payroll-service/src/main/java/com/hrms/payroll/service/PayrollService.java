package com.hrms.payroll.service;

import com.hrms.payroll.dto.PayrollDTO;
import com.hrms.payroll.dto.EmployeeDTO;
import com.hrms.payroll.dto.AttendanceDTO;
import com.hrms.payroll.dto.LeaveDTO;
import com.hrms.payroll.entity.Payroll;
import com.hrms.payroll.exception.ResourceNotFoundException;
import com.hrms.payroll.repository.PayrollRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PayrollService {

    private final PayrollRepository payrollRepository;
    private final RestTemplate restTemplate;

    private static final BigDecimal HRA_PERCENTAGE = BigDecimal.valueOf(0.25);
    private static final BigDecimal DA_PERCENTAGE = BigDecimal.valueOf(0.10);
    private static final BigDecimal TA_PERCENTAGE = BigDecimal.valueOf(0.05);
    private static final BigDecimal MA_PERCENTAGE = BigDecimal.valueOf(0.03);
    private static final BigDecimal PF_PERCENTAGE = BigDecimal.valueOf(0.12);
    private static final BigDecimal PT_AMOUNT = BigDecimal.valueOf(200.0);
    private static final BigDecimal LATE_DEDUCTION_RATE = BigDecimal.valueOf(50.0);
    private static final BigDecimal BONUS_PERCENTAGE_HIGH = BigDecimal.valueOf(0.15);
    private static final BigDecimal BONUS_PERCENTAGE_MEDIUM = BigDecimal.valueOf(0.10);
    private static final BigDecimal BONUS_PERCENTAGE_LOW = BigDecimal.valueOf(0.05);

   @CircuitBreaker(name = "employeeServiceCB", fallbackMethod = "getEmployeeDetailsFallback")
   @Bulkhead(name = "employeeServiceBH")
   private EmployeeDTO getEmployeeDetails(Long employeeId) {
       ResponseEntity<EmployeeDTO> response = restTemplate.getForEntity(
           "http://employee-service/employees/" + employeeId, 
           EmployeeDTO.class
       );
       return response.getBody();
   }
   
   private EmployeeDTO getEmployeeDetailsFallback(Long employeeId, Throwable throwable) {
	   log.error("circuit opened for fallback triggered for employee ID:{}.Error:{}",
			   employeeId,throwable.getMessage());
	   
		return EmployeeDTO.builder()
				.id(employeeId)
				.firstName("User temporarily")
				.lastName("unavailable")
				.employeeCode("N/A")
				.baseSalary(0.0)
				.build();
   }

    private List<AttendanceDTO> getAttendanceHistory(Long employeeId, LocalDate startDate, LocalDate endDate) {
        try {
            String url = String.format("http://attendance-service/api/attendance/employee/%d?startDate=%s&endDate=%s", 
                employeeId, startDate.toString(), endDate.toString());
            ResponseEntity<AttendanceDTO[]> response = restTemplate.getForEntity(url, AttendanceDTO[].class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Arrays.asList(response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to fetch attendance history for employee {}: {}", employeeId, e.getMessage());
        }
        return List.of();
    }

    private List<LeaveDTO> getLeaveHistory(Long employeeId) {
        try {
            String url = "http://leave-service/api/leaves/employee/" + employeeId;
            ResponseEntity<LeaveDTO[]> response = restTemplate.getForEntity(url, LeaveDTO[].class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Arrays.asList(response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to fetch leave history for employee {}: {}", employeeId, e.getMessage());
        }
        return List.of();
    }

    public PayrollDTO generatePayroll(Long employeeId, int year, int month) {
        log.info("Generating payroll for employee: {} for {}-{}", employeeId, year, month);
        
        EmployeeDTO employee = getEmployeeDetails(employeeId);
        if (employee == null) {
            throw new ResourceNotFoundException("Employee not found");
        }
        
        // Check if payroll already exists
        if (payrollRepository.existsByEmployeeIdAndYearAndMonth(employeeId, year, month)) {
            throw new RuntimeException("Payroll already generated for this month");
        }
        
        // Calculate dates for the month
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();
        long totalWorkingDays = endDate.getDayOfMonth();
        
        // Fetch Attendance history from attendance-service
        List<AttendanceDTO> attendances = getAttendanceHistory(employeeId, startDate, endDate);
        
        long presentDays = attendances.stream()
            .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()) || "LATE".equalsIgnoreCase(a.getStatus()))
            .count();
            
        long lateDays = attendances.stream()
            .filter(a -> "LATE".equalsIgnoreCase(a.getStatus()))
            .count();
            
        double totalOvertimeHours = attendances.stream()
            .filter(a -> a.getOvertimeHours() != null)
            .mapToDouble(AttendanceDTO::getOvertimeHours)
            .sum();
            
        long absentDays = totalWorkingDays - presentDays;
        
        // Fetch Leave history from leave-service
        List<LeaveDTO> leaves = getLeaveHistory(employeeId);
        long leaveDays = leaves.stream()
            .filter(l -> "APPROVED".equalsIgnoreCase(l.getStatus()) && l.getStartDate() != null && l.getStartDate().getYear() == year)
            .mapToLong(LeaveDTO::getTotalDays)
            .sum();
        
        // Calculate earnings
        BigDecimal basicSalary = BigDecimal.valueOf(employee.getBaseSalary());
        BigDecimal houseRentAllowance = basicSalary.multiply(HRA_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal dearnessAllowance = basicSalary.multiply(DA_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal travelAllowance = basicSalary.multiply(TA_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal medicalAllowance = basicSalary.multiply(MA_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal specialAllowance = calculateSpecialAllowance(basicSalary, presentDays, totalWorkingDays);
        
        BigDecimal totalEarnings = basicSalary
            .add(houseRentAllowance)
            .add(dearnessAllowance)
            .add(travelAllowance)
            .add(medicalAllowance)
            .add(specialAllowance);
        
        // Calculate deductions
        BigDecimal providentFund = basicSalary.multiply(PF_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal professionalTax = PT_AMOUNT;
        BigDecimal incomeTax = calculateIncomeTax(basicSalary);
        BigDecimal loanDeduction = BigDecimal.ZERO; // Placeholder
        BigDecimal lateDeduction = LATE_DEDUCTION_RATE.multiply(BigDecimal.valueOf(lateDays));
        
        BigDecimal totalDeductions = providentFund
            .add(professionalTax)
            .add(incomeTax)
            .add(loanDeduction)
            .add(lateDeduction);
        
        // Calculate net salary
        BigDecimal netSalary = totalEarnings.subtract(totalDeductions);
        
        // Create payroll record
        Payroll payroll = Payroll.builder()
            .employeeId(employeeId)
            .year(year)
            .month(month)
            .basicSalary(basicSalary)
            .houseRentAllowance(houseRentAllowance)
            .dearnessAllowance(dearnessAllowance)
            .travelAllowance(travelAllowance)
            .medicalAllowance(medicalAllowance)
            .specialAllowance(specialAllowance)
            .totalEarnings(totalEarnings)
            .providentFund(providentFund)
            .professionalTax(professionalTax)
            .incomeTax(incomeTax)
            .loanDeduction(loanDeduction)
            .totalDeductions(totalDeductions)
            .netSalary(netSalary)
            .totalPresentDays((int) presentDays)
            .totalAbsentDays((int) absentDays)
            .totalLeaveDays((int) leaveDays)
            .totalOvertimeHours(totalOvertimeHours)
            .generatedAt(LocalDateTime.now())
            .generatedBy("SYSTEM")
            .paymentStatus("PENDING")
            .build();
        
        Payroll savedPayroll = payrollRepository.save(payroll);
        log.info("Payroll generated successfully for employee: {}", employeeId);
        
        return convertToDTO(savedPayroll, employee.getFullName(), employee.getEmployeeCode());
    }

    public List<PayrollDTO> getEmployeePayrolls(Long employeeId) {
        log.debug("Fetching payrolls for employee: {}", employeeId);
        
        EmployeeDTO employee = getEmployeeDetails(employeeId);
        String employeeName = employee != null ? employee.getFullName() : "Unknown Employee";
        String employeeCode = employee != null ? employee.getEmployeeCode() : "N/A";

        return payrollRepository.findByEmployeeId(employeeId)
            .stream()
            .map(entity -> convertToDTO(entity, employeeName, employeeCode))
            .toList();
    }

    public PayrollDTO getPayrollByMonth(Long employeeId, int year, int month) {
        Payroll payroll = payrollRepository.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
            .orElseThrow(() -> new ResourceNotFoundException("Payroll not found for the specified month"));
            
        EmployeeDTO employee = getEmployeeDetails(employeeId);
        String employeeName = employee != null ? employee.getFullName() : "Unknown Employee";
        String employeeCode = employee != null ? employee.getEmployeeCode() : "N/A";

        return convertToDTO(payroll, employeeName, employeeCode);
    }

    public PayrollDTO updatePaymentStatus(Long payrollId, String status, String approvedBy) {
        log.info("Updating payment status for payroll: {} to {}", payrollId, status);
        
        Payroll payroll = payrollRepository.findById(payrollId)
            .orElseThrow(() -> new ResourceNotFoundException("Payroll not found"));
        
        payroll.setPaymentStatus(status);
        if ("APPROVED".equals(status)) {
            payroll.setApprovedBy(approvedBy);
            payroll.setApprovedAt(LocalDateTime.now());
            payroll.setPaymentDate(LocalDate.now());
        }
        
        Payroll updatedPayroll = payrollRepository.save(payroll);
        
        EmployeeDTO employee = getEmployeeDetails(updatedPayroll.getEmployeeId());
        String employeeName = employee != null ? employee.getFullName() : "Unknown Employee";
        String employeeCode = employee != null ? employee.getEmployeeCode() : "N/A";

        return convertToDTO(updatedPayroll, employeeName, employeeCode);
    }

    private BigDecimal calculateSpecialAllowance(BigDecimal basicSalary, long presentDays, long totalWorkingDays) {
        if (totalWorkingDays == 0) {
            return BigDecimal.ZERO;
        }
        
        double attendancePercentage = (double) presentDays / totalWorkingDays;
        BigDecimal bonusPercentage;
        
        if (attendancePercentage >= 0.95) {
            bonusPercentage = BONUS_PERCENTAGE_HIGH;
        } else if (attendancePercentage >= 0.90) {
            bonusPercentage = BONUS_PERCENTAGE_MEDIUM;
        } else if (attendancePercentage >= 0.85) {
            bonusPercentage = BONUS_PERCENTAGE_LOW;
        } else {
            bonusPercentage = BigDecimal.ZERO;
        }
        
        return basicSalary
            .multiply(bonusPercentage)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateIncomeTax(BigDecimal basicSalary) {
        BigDecimal annualSalary = basicSalary.multiply(BigDecimal.valueOf(12));
        double annualSalaryValue = annualSalary.doubleValue();
        double tax = 0;
        
        if (annualSalaryValue <= 250000) {
            tax = 0;
        } else if (annualSalaryValue <= 500000) {
            tax = (annualSalaryValue - 250000) * 0.05;
        } else if (annualSalaryValue <= 1000000) {
            tax = 12500 + (annualSalaryValue - 500000) * 0.20;
        } else {
            tax = 112500 + (annualSalaryValue - 1000000) * 0.30;
        }
        
        return BigDecimal.valueOf(tax / 12).setScale(2, RoundingMode.HALF_UP);
    }

    private PayrollDTO convertToDTO(Payroll entity, String employeeName, String employeeCode) {
        return PayrollDTO.builder()
            .id(entity.getId())
            .employeeId(entity.getEmployeeId())
            .employeeName(employeeName)
            .employeeCode(employeeCode)
            .year(entity.getYear())
            .month(entity.getMonth())
            .basicSalary(entity.getBasicSalary() != null ? entity.getBasicSalary().doubleValue() : 0.0)
            .houseRentAllowance(entity.getHouseRentAllowance() != null ? entity.getHouseRentAllowance().doubleValue() : 0.0)
            .dearnessAllowance(entity.getDearnessAllowance() != null ? entity.getDearnessAllowance().doubleValue() : 0.0)
            .travelAllowance(entity.getTravelAllowance() != null ? entity.getTravelAllowance().doubleValue() : 0.0)
            .medicalAllowance(entity.getMedicalAllowance() != null ? entity.getMedicalAllowance().doubleValue() : 0.0)
            .specialAllowance(entity.getSpecialAllowance() != null ? entity.getSpecialAllowance().doubleValue() : 0.0)
            .totalEarnings(entity.getTotalEarnings() != null ? entity.getTotalEarnings().doubleValue() : 0.0)
            .providentFund(entity.getProvidentFund() != null ? entity.getProvidentFund().doubleValue() : 0.0)
            .professionalTax(entity.getProfessionalTax() != null ? entity.getProfessionalTax().doubleValue() : 0.0)
            .incomeTax(entity.getIncomeTax() != null ? entity.getIncomeTax().doubleValue() : 0.0)
            .loanDeduction(entity.getLoanDeduction() != null ? entity.getLoanDeduction().doubleValue() : 0.0)
            .totalDeductions(entity.getTotalDeductions() != null ? entity.getTotalDeductions().doubleValue() : 0.0)
            .netSalary(entity.getNetSalary() != null ? entity.getNetSalary().doubleValue() : 0.0)
            .totalPresentDays(entity.getTotalPresentDays())
            .totalAbsentDays(entity.getTotalAbsentDays())
            .totalLeaveDays(entity.getTotalLeaveDays())
            .paymentMode(entity.getPaymentMethod())
            .remarks(entity.getRemarks())
            .paymentStatus(entity.getPaymentStatus())
            .build();
    }
}
