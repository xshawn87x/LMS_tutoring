package com.lms.guardian;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GuardianLinkRepository extends JpaRepository<GuardianLink, UUID> {

    List<GuardianLink> findByParentSubject(String parentSubject);

    // 한 학생에 연결된 학부모들(리포트 발송 대상 찾기).
    List<GuardianLink> findByStudentSubject(String studentSubject);

    boolean existsByParentSubjectAndStudentSubject(String parentSubject, String studentSubject);

    // 회원 삭제 시 그 사람이 부모든 자녀든 걸린 연결을 함께 정리한다(고아 연결 방지).
    void deleteByParentSubjectOrStudentSubject(String parentSubject, String studentSubject);
}
