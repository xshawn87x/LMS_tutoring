package com.lms.guardian;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GuardianLinkRepository extends JpaRepository<GuardianLink, UUID> {

    List<GuardianLink> findByParentSubject(String parentSubject);

    boolean existsByParentSubjectAndStudentSubject(String parentSubject, String studentSubject);

    // 회원 삭제 시 그 사람이 부모든 자녀든 걸린 연결을 함께 정리한다(고아 연결 방지).
    void deleteByParentSubjectOrStudentSubject(String parentSubject, String studentSubject);
}
