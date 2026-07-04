package com.lms.notice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** RLS가 현재 테넌트로 격리한다. */
public interface NoticeRepository extends JpaRepository<Notice, UUID> {

    List<Notice> findByScopeOrderByPinnedDescCreatedAtDesc(NoticeScope scope);

    List<Notice> findByCourseIdOrderByPinnedDescCreatedAtDesc(UUID courseId);
}
