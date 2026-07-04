package com.lms.curation;

import com.lms.course.Course;
import com.lms.course.CourseRepository;
import com.lms.curation.ContentAnalyzer.AnalyzeInput;
import com.lms.curation.ContentAnalyzer.ContentAnalysis;
import com.lms.billing.UsageService;
import com.lms.error.NotFoundException;
import com.lms.feature.Feature;
import com.lms.feature.FeatureService;
import com.lms.lesson.Lesson;
import com.lms.lesson.LessonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 과정 콘텐츠 분석/조회. AI_CURATION 기능 플래그로 게이팅.
 * 분석기는 Claude(키 설정 시) 우선, 없으면 휴리스틱을 사용한다.
 * 분석 1회는 사용량 과금 대상 → {@link UsageService}로 계량한다.
 */
@Service
@Transactional
public class ContentInsightService {

    private final ContentInsightRepository insightRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final FeatureService featureService;
    private final UsageService usageService;
    private final ContentAnalyzer analyzer;

    public ContentInsightService(ContentInsightRepository insightRepository,
                                 CourseRepository courseRepository,
                                 LessonRepository lessonRepository,
                                 FeatureService featureService,
                                 UsageService usageService,
                                 List<ContentAnalyzer> analyzers) {
        this.insightRepository = insightRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.featureService = featureService;
        this.usageService = usageService;
        this.analyzer = pickAnalyzer(analyzers);
    }

    /** 사용 가능한 비휴리스틱 분석기(예: Claude, API 키 있음)가 있으면 우선, 없으면 휴리스틱. */
    private static ContentAnalyzer pickAnalyzer(List<ContentAnalyzer> analyzers) {
        return analyzers.stream()
                .filter(a -> !"HEURISTIC".equals(a.name()) && a.isAvailable())
                .findFirst()
                .orElseGet(() -> analyzers.stream()
                        .filter(a -> "HEURISTIC".equals(a.name()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("등록된 ContentAnalyzer가 없습니다")));
    }

    public ContentInsight analyze(UUID courseId) {
        featureService.requireEnabled(Feature.AI_CURATION);
        usageService.record(Feature.AI_CURATION);   // 분석 1회 = 사용량 과금 단위
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("과정을 찾을 수 없습니다: " + courseId));
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderNoAsc(courseId);

        ContentAnalysis result = analyzer.analyze(new AnalyzeInput(
                course.getTitle(),
                course.getDescription(),
                course.getCategoryCode(),
                course.getLevel(),
                lessons.stream().map(Lesson::getTitle).toList(),
                lessons.stream().map(Lesson::getContent).toList()
        ));

        ContentInsight insight = insightRepository.findByCourseId(courseId)
                .orElseGet(() -> new ContentInsight(courseId));
        insight.apply(result.tags(), result.difficulty(), result.summary(), result.estMinutes(), analyzer.name());
        return insightRepository.save(insight);
    }

    @Transactional(readOnly = true)
    public Optional<ContentInsight> get(UUID courseId) {
        featureService.requireEnabled(Feature.AI_CURATION);
        return insightRepository.findByCourseId(courseId);
    }
}
