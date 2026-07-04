package com.lms.attendance;

import com.lms.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 출석 기록 (반+학생+날짜 단위, 유니크). */
@Entity
@Table(name = "attendance")
@Getter
@NoArgsConstructor
public class Attendance extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "student_subject", nullable = false)
    private String studentSubject;

    @Column(name = "att_date", nullable = false)
    private LocalDate attDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    @Column
    private String note;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Attendance(UUID groupId, String studentSubject, LocalDate attDate, AttendanceStatus status, String note) {
        this.groupId = groupId;
        this.studentSubject = studentSubject;
        this.attDate = attDate;
        this.status = status;
        this.note = note;
        this.createdAt = OffsetDateTime.now();
    }

    public void update(AttendanceStatus status, String note) {
        this.status = status;
        this.note = note;
    }
}
