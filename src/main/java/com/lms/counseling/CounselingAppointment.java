package com.lms.counseling;

import com.lms.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 상담 예약 (학생/학부모 신청 → 관리자 확정/취소). */
@Entity
@Table(name = "counseling_appointment")
@Getter
@NoArgsConstructor
public class CounselingAppointment extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_subject", nullable = false)
    private String studentSubject;

    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    @Column(name = "preferred_at")
    private OffsetDateTime preferredAt;

    @Column(nullable = false)
    private String status;   // REQUESTED | CONFIRMED | CANCELLED

    @Column
    private String memo;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public CounselingAppointment(String studentSubject, String requestedBy, OffsetDateTime preferredAt, String memo) {
        this.studentSubject = studentSubject;
        this.requestedBy = requestedBy;
        this.preferredAt = preferredAt;
        this.memo = memo;
        this.status = "REQUESTED";
        this.createdAt = OffsetDateTime.now();
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
