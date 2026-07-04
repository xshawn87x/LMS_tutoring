package com.lms.placement;

import com.lms.error.BadRequestException;
import com.lms.error.NotFoundException;
import com.lms.exam.ExamService;
import com.lms.exam.dto.ExamDtos.ExamRequest;
import com.lms.exam.dto.ExamDtos.ScoreEntry;
import com.lms.group.GroupService;
import com.lms.group.StudentGroup;
import com.lms.group.dto.GroupDtos.GroupRequest;
import com.lms.placement.dto.PlacementDtos.Band;
import com.lms.placement.dto.PlacementDtos.Recommendation;
import com.lms.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 성적 기반 반편성 — 평균 성적으로 레벨반 추천/적용. */
@SpringBootTest
@Testcontainers
class PlacementTest {

    private static final UUID TENANT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("lms").withUsername("lms_owner").withPassword("lms_owner_pw");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "lms_app");
        registry.add("spring.datasource.password", () -> "lms_app_pw");
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired PlacementService placementService;
    @Autowired ExamService examService;
    @Autowired GroupService groupService;

    private Recommendation recOf(List<Recommendation> recs, String subject) {
        return recs.stream().filter(r -> r.studentSubject().equals(subject)).findFirst().orElseThrow();
    }

    @Test
    void 평균_성적으로_레벨반을_추천한다() {
        TenantContext.set(TENANT_A);
        StudentGroup top = groupService.create(new GroupRequest("상위반", null, null, null));
        StudentGroup base = groupService.create(new GroupRequest("기초반", null, null, null));
        long n = System.nanoTime();
        String hi = "hi_" + n + "@acme", lo = "lo_" + n + "@acme";

        UUID exam = examService.create(new ExamRequest("모의고사", "수학", LocalDate.of(2026, 3, 1), 100, null)).getId();
        examService.recordScores(exam, List.of(new ScoreEntry(hi, 90, null), new ScoreEntry(lo, 50, null)));

        List<Band> bands = List.of(new Band(80, top.getId()), new Band(0, base.getId()));
        List<Recommendation> recs = placementService.recommend(bands);

        assertThat(recOf(recs, hi).avgPercent()).isEqualTo(90);
        assertThat(recOf(recs, hi).groupId()).isEqualTo(top.getId());
        assertThat(recOf(recs, lo).groupId()).isEqualTo(base.getId());
    }

    @Test
    void 적용하면_학생이_반에_배정된다() {
        TenantContext.set(TENANT_A);
        StudentGroup top = groupService.create(new GroupRequest("A반", null, null, null));
        StudentGroup base = groupService.create(new GroupRequest("B반", null, null, null));
        long n = System.nanoTime();
        String hi = "ah_" + n + "@acme", lo = "al_" + n + "@acme";

        UUID exam = examService.create(new ExamRequest("중간고사", "영어", LocalDate.of(2026, 4, 1), 100, null)).getId();
        examService.recordScores(exam, List.of(new ScoreEntry(hi, 88, null), new ScoreEntry(lo, 40, null)));

        List<Band> bands = List.of(new Band(70, top.getId()), new Band(0, base.getId()));
        placementService.apply(bands);

        assertThat(groupService.isMember(top.getId(), hi)).isTrue();
        assertThat(groupService.isMember(base.getId(), lo)).isTrue();
        assertThat(groupService.isMember(top.getId(), lo)).isFalse();   // 저성적은 상위반에 없음
    }

    @Test
    void 없는_반이나_빈_밴드는_거부된다() {
        TenantContext.set(TENANT_A);
        assertThatThrownBy(() -> placementService.recommend(List.of(new Band(50, UUID.randomUUID()))))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> placementService.recommend(List.of()))
                .isInstanceOf(BadRequestException.class);
    }
}
