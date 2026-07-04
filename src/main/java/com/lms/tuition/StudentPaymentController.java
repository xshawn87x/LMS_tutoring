package com.lms.tuition;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** 수강료 결제 API. 결제/내역=학생 본인, 전체/환불=ADMIN. */
@RestController
public class StudentPaymentController {

    private final StudentPaymentService service;

    public StudentPaymentController(StudentPaymentService service) {
        this.service = service;
    }

    @PostMapping("/api/courses/{courseId}/pay")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public PaymentView pay(@PathVariable UUID courseId, @AuthenticationPrincipal Jwt jwt) {
        return PaymentView.from(service.pay(jwt.getSubject(), courseId));
    }

    @GetMapping("/api/me/payments")
    public List<PaymentView> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.listMine(jwt.getSubject()).stream().map(PaymentView::from).toList();
    }

    @GetMapping("/api/admin/payments")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PaymentView> all() {
        return service.listAll().stream().map(PaymentView::from).toList();
    }

    @PostMapping("/api/admin/payments/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentView refund(@PathVariable UUID id) {
        return PaymentView.from(service.refund(id));
    }

    public record PaymentView(UUID id, String studentSubject, UUID courseId, int amount,
                              String status, String method, String paymentRef,
                              OffsetDateTime createdAt, OffsetDateTime refundedAt) {
        static PaymentView from(StudentPayment p) {
            return new PaymentView(p.getId(), p.getStudentSubject(), p.getCourseId(), p.getAmount(),
                    p.getStatus(), p.getMethod(), p.getPaymentRef(), p.getCreatedAt(), p.getRefundedAt());
        }
    }
}
