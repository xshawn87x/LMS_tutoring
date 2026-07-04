package com.lms.tuition;

import com.lms.course.Course;
import com.lms.course.CourseService;
import com.lms.error.BadRequestException;
import com.lms.error.ConflictException;
import com.lms.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 수강료 결제. 실 PG(토스/아임포트/Stripe) 연동은 스텁이며, 결제는 즉시 PAID로 처리한다.
 * 운영에선 결제 승인 콜백/웹훅으로 상태를 전환하는 실제 PG 어댑터로 교체한다.
 */
@Service
@Transactional
public class StudentPaymentService {

    private final StudentPaymentRepository repository;
    private final CourseService courseService;

    public StudentPaymentService(StudentPaymentRepository repository, CourseService courseService) {
        this.repository = repository;
        this.courseService = courseService;
    }

    /** 수강료 결제(모의 PG). 무료(0원)면 결제 대상 아님. */
    public StudentPayment pay(String student, UUID courseId) {
        Course course = courseService.require(courseId);
        if (course.getTuitionFee() <= 0) {
            throw new BadRequestException("무료 강의는 결제가 필요 없습니다");
        }
        // 이중 결제 방지: 이미 결제(환불 안 됨)한 강의는 다시 결제 불가.
        if (repository.existsByStudentSubjectAndCourseIdAndStatus(student, courseId, "PAID")) {
            throw new ConflictException("이미 결제한 강의입니다");
        }
        String ref = "mock_pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return repository.save(new StudentPayment(student, courseId, course.getTuitionFee(), "MOCK", ref));
    }

    @Transactional(readOnly = true)
    public List<StudentPayment> listMine(String student) {
        return repository.findByStudentSubjectOrderByCreatedAtDesc(student);
    }

    @Transactional(readOnly = true)
    public List<StudentPayment> listAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public StudentPayment refund(UUID id) {
        StudentPayment p = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("결제 내역을 찾을 수 없습니다: " + id));
        if ("REFUNDED".equals(p.getStatus())) {
            throw new BadRequestException("이미 환불된 결제입니다");
        }
        p.refund();
        return p;
    }
}
