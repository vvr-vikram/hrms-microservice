package com.hrms.employee.repository;

import com.hrms.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmployeeCode(String employeeCode);

    Page<Employee> findByDepartmentId(Long departmentId, Pageable pageable);
    
    Page<Employee> findByIsActive(Boolean isActive, Pageable pageable);
    
    long countByIsActive(Boolean isActive);

    List<Employee> findByDepartmentId(Long departmentId);

    List<Employee> findByRole(String role);

    List<Employee> findByIsActive(Boolean isActive);

    @Query("SELECT e FROM Employee e WHERE e.joiningDate BETWEEN :startDate AND :endDate")
    List<Employee> findEmployeesJoinedBetween(@Param("startDate") LocalDate startDate, 
                                               @Param("endDate") LocalDate endDate);

    @Query("SELECT e FROM Employee e WHERE " +
           "LOWER(e.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Employee> searchEmployees(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.departmentId = :departmentId AND e.isActive = true")
    Long countActiveEmployeesByDepartment(@Param("departmentId") Long departmentId);
}
