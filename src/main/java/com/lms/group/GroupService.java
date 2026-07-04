package com.lms.group;

import com.lms.error.ConflictException;
import com.lms.error.NotFoundException;
import com.lms.group.dto.GroupDtos.GroupRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 반/기수 + 소속 학생 관리. RLS로 테넌트 격리. 관리 권한은 컨트롤러(INSTRUCTOR/ADMIN). */
@Service
@Transactional
public class GroupService {

    private final StudentGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;

    public GroupService(StudentGroupRepository groupRepository, GroupMemberRepository memberRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentGroup> list() {
        return groupRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public StudentGroup get(UUID id) {
        return require(id);
    }

    public StudentGroup create(GroupRequest req) {
        return groupRepository.save(new StudentGroup(req.name(), req.term(), req.startDate(), req.endDate()));
    }

    public StudentGroup update(UUID id, GroupRequest req) {
        StudentGroup g = require(id);
        g.update(req.name(), req.term(), req.startDate(), req.endDate());
        return g;
    }

    public void delete(UUID id) {
        groupRepository.delete(require(id));   // members/attendance FK CASCADE
    }

    @Transactional(readOnly = true)
    public int memberCount(UUID groupId) {
        return memberRepository.findByGroupId(groupId).size();
    }

    /** 반별 인원수 배치 집계 (N+1 방지). RLS로 현재 테넌트 범위. */
    @Transactional(readOnly = true)
    public Map<UUID, Integer> memberCountsByGroup() {
        Map<UUID, Integer> counts = new HashMap<>();
        for (GroupMember gm : memberRepository.findAll()) {
            counts.merge(gm.getGroupId(), 1, Integer::sum);
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public List<GroupMember> members(UUID groupId) {
        require(groupId);
        return memberRepository.findByGroupId(groupId);
    }

    @Transactional(readOnly = true)
    public boolean isMember(UUID groupId, String studentSubject) {
        return memberRepository.existsByGroupIdAndStudentSubject(groupId, studentSubject.trim().toLowerCase());
    }

    public GroupMember addMember(UUID groupId, String studentSubject) {
        require(groupId);
        String s = studentSubject.trim().toLowerCase();
        if (memberRepository.existsByGroupIdAndStudentSubject(groupId, s)) {
            throw new ConflictException("이미 반에 소속된 학생입니다");
        }
        return memberRepository.save(new GroupMember(groupId, s));
    }

    public void removeMember(UUID groupId, String studentSubject) {
        GroupMember m = memberRepository.findByGroupIdAndStudentSubject(groupId, studentSubject.trim().toLowerCase())
                .orElseThrow(() -> new NotFoundException("반 소속 학생이 아닙니다"));
        memberRepository.delete(m);
    }

    private StudentGroup require(UUID id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("반을 찾을 수 없습니다: " + id));
    }
}
