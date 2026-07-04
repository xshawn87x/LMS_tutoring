package com.lms.tuition;

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

/** 수강생의 수강료 결제 내역. */
@Entity
@Table(name = "student_payment")
@Getter
@NoArgsConstructor
public class StudentPayment extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_subject", nullable = false)
    private String studentSubject;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private String status;   // PENDING | PAID | REFUNDED

    @Column
    private String method;

    @Column(name = "payment_ref")
    private String paymentRef;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "refunded_at")
    private OffsetDateTime refundedAt;

    public StudentPayment(String studentSubject, UUID courseId, int amount, String method, String paymentRef) {
        this.studentSubject = studentSubject;
        this.courseId = courseId;
        this.amount = amount;
        this.method = method;
        this.paymentRef = paymentRef;
        this.status = "PAID";
        this.createdAt = OffsetDateTime.now();
    }

    public void refund() {
        this.status = "REFUNDED";
        this.refundedAt = OffsetDateTime.now();
    }
}
