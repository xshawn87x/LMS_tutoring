package com.lms.group;

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

/** 반 소속 학생. */
@Entity
@Table(name = "group_member")
@Getter
@NoArgsConstructor
public class GroupMember extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "student_subject", nullable = false)
    private String studentSubject;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public GroupMember(UUID groupId, String studentSubject) {
        this.groupId = groupId;
        this.studentSubject = studentSubject;
        this.createdAt = OffsetDateTime.now();
    }
}
