package com.lms.group.dto;

import com.lms.group.GroupMember;
import com.lms.group.StudentGroup;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class GroupDtos {

    private GroupDtos() {
    }

    public record GroupRequest(@NotBlank String name, String term, LocalDate startDate, LocalDate endDate) {
    }

    public record AddMemberRequest(@NotBlank String studentSubject) {
    }

    public record GroupResponse(
            UUID id, String name, String term, LocalDate startDate, LocalDate endDate,
            int memberCount, OffsetDateTime createdAt) {
        public static GroupResponse of(StudentGroup g, int memberCount) {
            return new GroupResponse(g.getId(), g.getName(), g.getTerm(), g.getStartDate(), g.getEndDate(),
                    memberCount, g.getCreatedAt());
        }
    }

    public record MemberResponse(UUID id, String studentSubject, OffsetDateTime createdAt) {
        public static MemberResponse from(GroupMember m) {
            return new MemberResponse(m.getId(), m.getStudentSubject(), m.getCreatedAt());
        }
    }
}
