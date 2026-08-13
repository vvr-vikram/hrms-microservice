package com.hrms.attendance.repository;

import com.hrms.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByEmployeeIdAndDate(Long employeeId, LocalDate date);

    List<Attendance> findByEmployeeIdAndDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.employeeId = :employeeId " +
           "AND a.date BETWEEN :startDate AND :endDate " +
           "AND a.status IN ('PRESENT', 'LATE')")
    Long countPresentDays(@Param("employeeId") Long employeeId, 
                          @Param("startDate") LocalDate startDate, 
                          @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.employeeId = :employeeId " +
           "AND a.date BETWEEN :startDate AND :endDate " +
           "AND a.status = 'LATE'")
    Long countLateDaysByEmployee(@Param("employeeId") Long employeeId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);
}
