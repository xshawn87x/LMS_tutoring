package com.lms.learner;

import com.lms.course.Course;
import com.lms.course.CourseRepository;
import com.lms.curation.ContentInsight;
import com.lms.curation.ContentInsightRepository;
import com.lms.enrollment.Enrollment;
import com.lms.enrollment.EnrollmentRepository;
import com.lms.feature.Feature;
import com.lms.feature.FeatureService;
import com.lms.learner.dto.LearnerDtos.RecommendationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 규칙 기반 추천 (Phase 1, AI 없음).
 * 학습자의 관심분야·역량 ↔ 과정의 분야·난이도를 매칭해 점수화하고, 이미 수강한 과정은 제외한다.
 * 각 추천에는 "왜 추천됐는지"(reason)를 함께 담는다.
 */
@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private static final int MAX_RESULTS = 6;

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LearnerInterestRepository interestRepository;
    private final LearnerSkillRepository skillRepository;
    private final InterestCategoryRepository categoryRepository;
    private final ContentInsightRepository insightRepository;
    private final FeatureService featureService;

    public RecommendationService(CourseRepository courseRepository,
                                 EnrollmentRepository enrollmentRepository,
                                 LearnerInterestRepository interestRepository,
                                 LearnerSkillRepository skillRepository,
                                 InterestCategoryRepository categoryRepository,
                                 ContentInsightRepository insightRepository,
                                 FeatureService featureService) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.interestRepository = interestRepository;
        this.skillRepository = skillRepository;
        this.categoryRepository = categoryRepository;
        this.insightRepository = insightRepository;
        this.featureService = featureService;
    }

    public List<RecommendationResponse> recommend(String studentId) {
        featureService.requireEnabled(Feature.RECOMMENDATIONS);

        Set<String> interests = interestRepository.findByStudentId(studentId).stream()
                .map(LearnerInterest::getCategoryCode).collect(Collectors.toSet());
        Map<String, Integer> skillByCategory = skillRepository.findByStudentId(studentId).stream()
                .collect(Collectors.toMap(LearnerSkill::getCategoryCode, LearnerSkill::getLevel, (a, b) -> a));
        Set<UUID> enrolled = enrollmentRepository.findByStudentIdOrderByEnrolledAtDesc(studentId).stream()
                .map(Enrollment::getCourseId).collect(Collectors.toSet());
        Map<String, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(InterestCategory::getCode, InterestCategory::getName));
        // 콘텐츠 분석 태그 (AI_CURATION으로 분석된 과정에 한해 존재). RLS로 현재 테넌트만.
        Map<UUID, List<String>> tagsByCourse = insightRepository.findAll().stream()
                .collect(Collectors.toMap(ContentInsight::getCourseId, ContentInsight::getTags, (a, b) -> a));

        List<Course> allCourses = courseRepository.findAll();
        // 행동 신호 1: 인기도 — 테넌트 전체 수강 수
        Map<UUID, Long> popularity = enrollmentRepository.findAll().stream()
                .collect(Collectors.groupingBy(Enrollment::getCourseId, Collectors.counting()));
        // 행동 신호 2: 학습자가 이미 수강한 과정들의 분야 (이어가기 신호)
        Map<UUID, String> categoryByCourse = allCourses.stream()
                .filter(c -> c.getCategoryCode() != null)
                .collect(Collectors.toMap(Course::getId, Course::getCategoryCode, (a, b) -> a));
        Set<String> engagedCategories = enrolled.stream()
                .map(categoryByCourse::get).filter(c -> c != null).collect(Collectors.toSet());

        List<Scored> scored = new ArrayList<>();
        for (Course course : allCourses) {
            if (enrolled.contains(course.getId())) {
                continue; // 이미 수강한 과정 제외
            }
            scored.add(score(course, interests, skillByCategory, categoryNames,
                    tagsByCourse.getOrDefault(course.getId(), List.of()),
                    popularity.getOrDefault(course.getId(), 0L), engagedCategories));
        }

        scored.sort((a, b) -> b.score() - a.score());
        return scored.stream()
                .limit(MAX_RESULTS)
                .map(s -> new RecommendationResponse(
                        s.course().getId(), s.course().getTitle(), s.course().getCategoryCode(),
                        s.course().getLevel(), s.score(), s.reason()))
                .toList();
    }

    private Scored score(Course course, Set<String> interests,
                         Map<String, Integer> skillByCategory, Map<String, String> categoryNames,
                         List<String> contentTags, long popularity, Set<String> engagedCategories) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        String category = course.getCategoryCode();

        if (category != null && interests.contains(category)) {
            score += 10;
            reasons.add("관심분야 '" + categoryNames.getOrDefault(category, category) + "'");
        }

        // 행동 신호: 내가 수강한 분야와 같은 과정 (이어가기)
        if (category != null && engagedCategories.contains(category) && !interests.contains(category)) {
            score += 3;
            reasons.add("수강 이력과 같은 분야");
        }

        // 행동 신호: 인기 과정 (테넌트 전체 수강 수, 최대 +3)
        if (popularity > 0) {
            score += (int) Math.min(3, popularity);
            if (popularity >= 3) {
                reasons.add("인기 과정");
            }
        }

        // AI 콘텐츠 분석 태그가 관심분야와 겹치면 가산 (관심분야 코드를 소문자 태그와 비교)
        if (!contentTags.isEmpty() && !interests.isEmpty()) {
            Set<String> interestTags = interests.stream().map(String::toLowerCase).collect(Collectors.toSet());
            List<String> matched = contentTags.stream().filter(interestTags::contains).distinct().toList();
            if (!matched.isEmpty()) {
                score += Math.min(9, matched.size() * 3);
                reasons.add("콘텐츠 태그 일치: " + String.join(", ", matched));
            }
        }

        if (course.getLevel() != null && category != null && skillByCategory.containsKey(category)) {
            int diff = course.getLevel() - skillByCategory.get(category);
            if (diff <= 0) {
                score += 2;
                reasons.add("현재 수준에서 무리 없음");
            } else if (diff == 1) {
                score += 5;
                reasons.add("한 단계 도전하기 좋음");
            } else {
                score -= 3;
                reasons.add("다소 어려울 수 있음");
            }
        }

        String reason = reasons.isEmpty() ? "새로운 과정" : String.join(" · ", reasons);
        return new Scored(course, score, reason);
    }

    private record Scored(Course course, int score, String reason) {
    }
}
