package com.lms.placement;

import com.lms.auth.AppUser;
import com.lms.auth.AppUserRepository;
import com.lms.error.BadRequestException;
import com.lms.error.NotFoundException;
import com.lms.exam.Exam;
import com.lms.exam.ExamRepository;
import com.lms.exam.ExamScore;
import com.lms.exam.ExamScoreRepository;
import com.lms.group.GroupService;
import com.lms.group.StudentGroup;
import com.lms.group.StudentGroupRepository;
import com.lms.placement.dto.PlacementDtos.ApplyResult;
import com.lms.placement.dto.PlacementDtos.Band;
import com.lms.placement.dto.PlacementDtos.Recommendation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 성적 기반 반편성 — 학생의 평균 시험 성적(백분율)으로 레벨반에 배치.
 * 관리자가 밴드(최소 점수→반)를 정하면 추천(preview) 후 적용(자동 배치). RLS로 테넌트 격리.
 */
@Service
@Transactional
public class PlacementService {

    private final ExamRepository examRepository;
    private final ExamScoreRepository scoreRepository;
    private final StudentGroupRepository groupRepository;
    private final GroupService groupService;
    private final AppUserRepository userRepository;

    public PlacementService(ExamRepository examRepository, ExamScoreRepository scoreRepository,
                            StudentGroupRepository groupRepository, GroupService groupService,
                            AppUserRepository userRepository) {
        this.examRepository = examRepository;
        this.scoreRepository = scoreRepository;
        this.groupRepository = groupRepository;
        this.groupService = groupService;
        this.userRepository = userRepository;
    }

    /** 성적 있는 학생별 평균 백분율을 밴드에 매칭해 배치 추천(미적용). 평균 높은 순 정렬. */
    @Transactional(readOnly = true)
    public List<Recommendation> recommend(List<Band> bands) {
        List<Band> sorted = validateAndSort(bands);
        Map<UUID, String> groupNames = groupRepository.findAllById(
                        bands.stream().map(Band::groupId).toList()).stream()
                .collect(Collectors.toMap(StudentGroup::getId, StudentGroup::getName));

        Map<UUID, Exam> exams = examRepository.findAll().stream()
                .collect(Collectors.toMap(Exam::getId, Function.identity()));

        // 학생별 (백분율 합, 건수) 집계
        Map<String, double[]> agg = new HashMap<>();
        for (ExamScore s : scoreRepository.findAll()) {
            Exam e = exams.get(s.getExamId());
            if (e == null) continue;
            double pct = s.getScore() * 100.0 / e.getMaxScore();
            double[] a = agg.computeIfAbsent(s.getStudentSubject(), k -> new double[2]);
            a[0] += pct;
            a[1]++;
        }

        List<Recommendation> out = new ArrayList<>();
        for (Map.Entry<String, double[]> en : agg.entrySet()) {
            String student = en.getKey();
            int avg = (int) Math.round(en.getValue()[0] / en.getValue()[1]);
            int count = (int) en.getValue()[1];
            Band band = pick(sorted, avg);
            String name = userRepository.findByEmail(student).map(AppUser::getDisplayName).orElse(null);
            // 현재 소속(밴드 반 중 첫 번째)
            UUID curId = null;
            for (Band b : sorted) {
                if (groupService.isMember(b.groupId(), student)) { curId = b.groupId(); break; }
            }
            String curName = curId == null ? null : groupNames.get(curId);
            boolean moved = !java.util.Objects.equals(curId, band.groupId());
            out.add(new Recommendation(student, name, avg, count,
                    band.groupId(), groupNames.get(band.groupId()), curId, curName, moved));
        }
        out.sort(Comparator.comparingInt(Recommendation::avgPercent).reversed());
        return out;
    }

    /** 추천대로 반 배정을 적용. 밴드 반들 중 추천 반에만 남기고 나머지에선 제외(레벨 이동 반영). */
    public ApplyResult apply(List<Band> bands) {
        List<Band> sorted = validateAndSort(bands);
        List<Recommendation> recs = recommend(bands);
        int assigned = 0;
        for (Recommendation r : recs) {
            for (Band b : sorted) {
                boolean isRecommended = b.groupId().equals(r.groupId());
                boolean member = groupService.isMember(b.groupId(), r.studentSubject());
                if (isRecommended && !member) {
                    groupService.addMember(b.groupId(), r.studentSubject());
                    assigned++;
                } else if (!isRecommended && member) {
                    groupService.removeMember(b.groupId(), r.studentSubject());
                }
            }
        }
        return new ApplyResult(assigned, recs.size(), recs);
    }

    private List<Band> validateAndSort(List<Band> bands) {
        if (bands == null || bands.isEmpty()) {
            throw new BadRequestException("반편성 기준(밴드)을 하나 이상 지정하세요");
        }
        for (Band b : bands) {
            if (b.minPercent() < 0 || b.minPercent() > 100) {
                throw new BadRequestException("기준 점수는 0~100 이어야 합니다: " + b.minPercent());
            }
            groupRepository.findById(b.groupId())
                    .orElseThrow(() -> new NotFoundException("반을 찾을 수 없습니다: " + b.groupId()));
        }
        return bands.stream().sorted(Comparator.comparingInt(Band::minPercent).reversed()).toList();
    }

    /** 평균이 도달하는 가장 높은 밴드. 어느 밴드에도 못 미치면 최저 밴드(기초반)로. */
    private Band pick(List<Band> sortedDesc, int avg) {
        for (Band b : sortedDesc) {
            if (avg >= b.minPercent()) return b;
        }
        return sortedDesc.get(sortedDesc.size() - 1);
    }
}
