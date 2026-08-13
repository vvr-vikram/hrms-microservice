package com.hrms.payroll.repository;

import com.hrms.payroll.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    List<Payroll> findByEmployeeId(Long employeeId);

    Optional<Payroll> findByEmployeeIdAndYearAndMonth(Long employeeId, Integer year, Integer month);

    boolean existsByEmployeeIdAndYearAndMonth(Long employeeId, Integer year, Integer month);

    @Query("SELECT p FROM Payroll p WHERE p.employeeId = :employeeId ORDER BY p.year DESC, p.month DESC")
    List<Payroll> findLatestPayrollsByEmployee(@Param("employeeId") Long employeeId);
}
