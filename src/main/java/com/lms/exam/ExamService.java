package com.lms.exam;

import com.lms.auth.AppUser;
import com.lms.auth.AppUserRepository;
import com.lms.error.BadRequestException;
import com.lms.error.NotFoundException;
import com.lms.exam.dto.ExamDtos.ExamRanking;
import com.lms.exam.dto.ExamDtos.ExamRequest;
import com.lms.exam.dto.ExamDtos.ScoreEntry;
import com.lms.exam.dto.ExamDtos.StudentScore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 시험 정의 + 학생별 성적 관리. 관리(시험/성적 입력)는 컨트롤러에서 INSTRUCTOR/ADMIN으로 제한.
 * 조회는 본인 성적(학생)·자녀 성적(학부모, GuardianService 경유). RLS로 테넌트 격리.
 */
@Service
@Transactional
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamScoreRepository scoreRepository;
    private final AppUserRepository userRepository;

    public ExamService(ExamRepository examRepository, ExamScoreRepository scoreRepository,
                       AppUserRepository userRepository) {
        this.examRepository = examRepository;
        this.scoreRepository = scoreRepository;
        this.userRepository = userRepository;
    }

    /** 한 시험 점수 목록에서 특정 점수의 석차(1-based, 동점은 같은 등수) + 상위 백분율. */
    private static int[] rankOf(List<Integer> allScores, int myScore) {
        int total = allScores.size();
        int higher = (int) allScores.stream().filter(s -> s > myScore).count();
        int rank = higher + 1;
        int topPercent = total == 0 ? 0 : (int) Math.ceil(rank * 100.0 / total);
        return new int[] { rank, total, topPercent };
    }

    // --- 시험 ---

    @Transactional(readOnly = true)
    public List<Exam> list() {
        return examRepository.findAllByOrderByExamDateDesc();
    }

    @Transactional(readOnly = true)
    public Exam get(UUID id) {
        return require(id);
    }

    public Exam create(ExamRequest req) {
        validate(req);
        return examRepository.save(new Exam(req.title().trim(), blankToNull(req.subject()),
                req.examDate(), maxScoreOf(req), req.groupId()));
    }

    public Exam update(UUID id, ExamRequest req) {
        validate(req);
        Exam e = require(id);
        e.update(req.title().trim(), blankToNull(req.subject()), req.examDate(), maxScoreOf(req), req.groupId());
        return e;
    }

    public void delete(UUID id) {
        examRepository.delete(require(id));   // exam_score FK CASCADE
    }

    // --- 성적 ---

    /** 한 시험의 학생별 점수 일괄 입력(upsert). 만점 초과·음수는 거부. */
    public List<ExamScore> recordScores(UUID examId, List<ScoreEntry> entries) {
        Exam exam = require(examId);
        return entries.stream().map(en -> {
            String student = en.studentSubject() == null ? "" : en.studentSubject().trim().toLowerCase();
            if (student.isBlank()) {
                throw new BadRequestException("학생 이메일을 입력하세요");
            }
            if (en.score() < 0 || en.score() > exam.getMaxScore()) {
                throw new BadRequestException("점수는 0~" + exam.getMaxScore() + " 범위여야 합니다: " + en.score());
            }
            return scoreRepository.findByExamIdAndStudentSubject(examId, student)
                    .map(existing -> { existing.update(en.score(), en.comment()); return existing; })
                    .orElseGet(() -> scoreRepository.save(new ExamScore(examId, student, en.score(), en.comment())));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<ExamScore> scoresForExam(UUID examId) {
        require(examId);
        return scoreRepository.findByExamId(examId);
    }

    /** 학생 본인/자녀의 성적을 시험 정보·석차와 함께, 시행일 오름차순으로(추이 그래프용). */
    @Transactional(readOnly = true)
    public List<StudentScore> studentScores(String studentSubject) {
        List<ExamScore> scores = scoreRepository.findByStudentSubject(studentSubject.trim().toLowerCase());
        if (scores.isEmpty()) return List.of();
        Map<UUID, Exam> exams = examRepository.findAllById(
                        scores.stream().map(ExamScore::getExamId).distinct().toList()).stream()
                .collect(Collectors.toMap(Exam::getId, Function.identity()));
        // 시험별 전체 점수(석차 계산용)를 한 번씩만 로드
        Map<UUID, List<Integer>> allByExam = new java.util.HashMap<>();
        for (UUID examId : exams.keySet()) {
            allByExam.put(examId, scoreRepository.findByExamId(examId).stream().map(ExamScore::getScore).toList());
        }
        return scores.stream()
                .map(s -> {
                    Exam e = exams.get(s.getExamId());
                    if (e == null) return null;   // 시험이 삭제된 경우(CASCADE로 보통 없음) 방어
                    int percent = (int) Math.round(s.getScore() * 100.0 / e.getMaxScore());
                    int[] r = rankOf(allByExam.getOrDefault(e.getId(), List.of()), s.getScore());
                    return new StudentScore(e.getId(), e.getTitle(), e.getSubject(), e.getExamDate(),
                            s.getScore(), e.getMaxScore(), percent, s.getComment(), r[0], r[1], r[2]);
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(StudentScore::examDate))
                .toList();
    }

    /** 한 시험의 석차표 — 점수 내림차순, 석차·상위 백분율·이름 포함. */
    @Transactional(readOnly = true)
    public List<ExamRanking> ranking(UUID examId) {
        Exam exam = require(examId);
        List<ExamScore> scores = scoreRepository.findByExamId(examId);
        List<Integer> all = scores.stream().map(ExamScore::getScore).toList();
        List<ExamRanking> out = new ArrayList<>();
        for (ExamScore s : scores) {
            int percent = (int) Math.round(s.getScore() * 100.0 / exam.getMaxScore());
            int[] r = rankOf(all, s.getScore());
            String name = userRepository.findByEmail(s.getStudentSubject()).map(AppUser::getDisplayName).orElse(null);
            out.add(new ExamRanking(s.getStudentSubject(), name, s.getScore(), percent, r[0], r[1], r[2]));
        }
        out.sort(Comparator.comparingInt(ExamRanking::score).reversed());
        return out;
    }

    // --- 헬퍼 ---

    private void validate(ExamRequest req) {
        if (req.title() == null || req.title().isBlank()) throw new BadRequestException("시험 제목을 입력하세요");
        if (req.examDate() == null) throw new BadRequestException("시행일을 입력하세요");
        if (req.maxScore() != null && req.maxScore() <= 0) throw new BadRequestException("만점은 1 이상이어야 합니다");
    }

    private int maxScoreOf(ExamRequest req) {
        return req.maxScore() == null ? 100 : req.maxScore();
    }

    private Exam require(UUID id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("시험을 찾을 수 없습니다: " + id));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
