package com.lms.guardian;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GuardianLinkRepository extends JpaRepository<GuardianLink, UUID> {

    List<GuardianLink> findByParentSubject(String parentSubject);

    boolean existsByParentSubjectAndStudentSubject(String parentSubject, String studentSubject);
}
