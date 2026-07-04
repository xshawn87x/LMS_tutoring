package com.lms.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {
    List<GroupMember> findByGroupId(UUID groupId);

    List<GroupMember> findByStudentSubject(String studentSubject);

    Optional<GroupMember> findByGroupIdAndStudentSubject(UUID groupId, String studentSubject);

    boolean existsByGroupIdAndStudentSubject(UUID groupId, String studentSubject);
}
