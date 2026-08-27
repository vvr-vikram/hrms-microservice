package com.hrms.payroll.controller;

import com.hrms.payroll.dto.GeneratePayrollRequest;
import jakarta.validation.Valid;
import com.hrms.payroll.dto.PayrollDTO;
import com.hrms.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payroll Management", description = "APIs for managing payroll")
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping("/generate")
    @Operation(summary = "Generate payroll for employee")
    public ResponseEntity<PayrollDTO> generatePayroll(
            @Valid @RequestBody GeneratePayrollRequest request) {
        log.info("POST /api/payroll/generate - Generating payroll for employee: {}, {}-{}", 
                request.getEmployeeId(), request.getYear(), request.getMonth());
        PayrollDTO payroll = payrollService.generatePayroll(request.getEmployeeId(), request.getYear(), request.getMonth());
        return new ResponseEntity<>(payroll, HttpStatus.CREATED);
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get employee payroll history")
    public ResponseEntity<List<PayrollDTO>> getEmployeePayrolls(@PathVariable Long employeeId) {
        log.info("GET /api/payroll/employee/{} - Fetching payroll history", employeeId);
        List<PayrollDTO> payrolls = payrollService.getEmployeePayrolls(employeeId);
        return ResponseEntity.ok(payrolls);
    }

    @GetMapping("/employee/{employeeId}/{year}/{month}")
    @Operation(summary = "Get specific month payroll")
    public ResponseEntity<PayrollDTO> getPayrollByMonth(
            @PathVariable Long employeeId,
            @PathVariable int year,
            @PathVariable int month) {
        log.info("GET /api/payroll/employee/{}/{}/{} - Fetching specific payroll", employeeId, year, month);
        PayrollDTO payroll = payrollService.getPayrollByMonth(employeeId, year, month);
        return ResponseEntity.ok(payroll);
    }

    @PutMapping("/{payrollId}/payment-status")
    @Operation(summary = "Update payment status")
    public ResponseEntity<PayrollDTO> updatePaymentStatus(
            @PathVariable Long payrollId,
            @RequestParam String status,
            @RequestParam String approvedBy) {
        log.info("PUT /api/payroll/{}/payment-status - Updating status to: {} by: {}", 
                payrollId, status, approvedBy);
        PayrollDTO payroll = payrollService.updatePaymentStatus(payrollId, status, approvedBy);
        return ResponseEntity.ok(payroll);
    }
}
