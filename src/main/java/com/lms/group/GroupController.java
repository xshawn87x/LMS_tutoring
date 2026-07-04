package com.lms.group;

import com.lms.group.dto.GroupDtos.AddMemberRequest;
import com.lms.group.dto.GroupDtos.GroupRequest;
import com.lms.group.dto.GroupDtos.GroupResponse;
import com.lms.group.dto.GroupDtos.MemberResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** 반/기수 관리 API. 관리=INSTRUCTOR/ADMIN, 조회도 동일(운영 화면용). */
@RestController
@RequestMapping("/api/groups")
@PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
public class GroupController {

    private final GroupService service;

    public GroupController(GroupService service) {
        this.service = service;
    }

    @GetMapping
    public List<GroupResponse> list() {
        var counts = service.memberCountsByGroup();   // 배치 집계 (N+1 방지)
        return service.list().stream()
                .map(g -> GroupResponse.of(g, counts.getOrDefault(g.getId(), 0)))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(@Valid @RequestBody GroupRequest request) {
        StudentGroup g = service.create(request);
        return GroupResponse.of(g, 0);
    }

    @PutMapping("/{id}")
    public GroupResponse update(@PathVariable UUID id, @Valid @RequestBody GroupRequest request) {
        StudentGroup g = service.update(id, request);
        return GroupResponse.of(g, service.memberCount(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping("/{id}/members")
    public List<MemberResponse> members(@PathVariable UUID id) {
        return service.members(id).stream().map(MemberResponse::from).toList();
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse addMember(@PathVariable UUID id, @Valid @RequestBody AddMemberRequest request) {
        return MemberResponse.from(service.addMember(id, request.studentSubject()));
    }

    @DeleteMapping("/{id}/members/{studentSubject}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID id, @PathVariable String studentSubject) {
        service.removeMember(id, studentSubject);
    }
}
