package com.hrms.leave.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_balance", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"employee_id", "year"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "annual_leave_balance")
    @Builder.Default
    private Double annualLeaveBalance = 0.0;

    @Column(name = "casual_leave_balance")
    @Builder.Default
    private Double casualLeaveBalance = 0.0;

    @Column(name = "sick_leave_balance")
    @Builder.Default
    private Double sickLeaveBalance = 0.0;

    @Column(name = "total_leave_taken")
    @Builder.Default
    private Double totalLeaveTaken = 0.0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
