package com.hrms.leave.repository;

import com.hrms.leave.entity.LeaveApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<LeaveApplication, Long> {

    List<LeaveApplication> findByEmployeeId(Long employeeId);

    @Query("SELECT l FROM LeaveApplication l WHERE l.employeeId = :employeeId " +
           "AND l.status = :status " +
           "AND l.startDate <= :endDate AND l.endDate >= :startDate")
    List<LeaveApplication> findByEmployeeIdAndStartDateBetweenAndStatus(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") String status);

    @Query("SELECT l FROM LeaveApplication l WHERE l.status = 'PENDING' ORDER BY l.createdAt ASC")
    List<LeaveApplication> findPendingLeavesOrderByDate();

    @Query("SELECT COALESCE(SUM(l.totalDays), 0) FROM LeaveApplication l " +
           "WHERE l.employeeId = :employeeId " +
           "AND l.status = 'APPROVED' " +
           "AND EXTRACT(YEAR FROM l.startDate) = :year")
    Long countApprovedLeavesByEmployeeAndYear(
            @Param("employeeId") Long employeeId, 
            @Param("year") Integer year);
}
