package com.lms.learner;

import com.lms.feature.Feature;
import com.lms.feature.FeatureService;
import com.lms.learner.dto.LearnerDtos.ProfileRequest;
import com.lms.learner.dto.LearnerDtos.ProfileResponse;
import com.lms.learner.dto.LearnerDtos.SkillView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LearnerProfileService {

    private final LearnerProfileRepository profileRepository;
    private final LearnerInterestRepository interestRepository;
    private final LearnerSkillRepository skillRepository;
    private final FeatureService featureService;

    public LearnerProfileService(LearnerProfileRepository profileRepository,
                                 LearnerInterestRepository interestRepository,
                                 LearnerSkillRepository skillRepository,
                                 FeatureService featureService) {
        this.profileRepository = profileRepository;
        this.interestRepository = interestRepository;
        this.skillRepository = skillRepository;
        this.featureService = featureService;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String studentId) {
        featureService.requireEnabled(Feature.DIAGNOSIS);
        boolean onboarded = profileRepository.findByStudentId(studentId)
                .map(LearnerProfile::isOnboarded).orElse(false);
        List<String> interests = interestRepository.findByStudentId(studentId).stream()
                .map(LearnerInterest::getCategoryCode).toList();
        List<SkillView> skills = skillRepository.findByStudentId(studentId).stream()
                .map(s -> new SkillView(s.getCategoryCode(), s.getLevel())).toList();
        return new ProfileResponse(onboarded, interests, skills);
    }

    /** 관심분야·역량을 통째로 교체 저장하고 온보딩 완료 처리. */
    public ProfileResponse saveProfile(String studentId, ProfileRequest request) {
        featureService.requireEnabled(Feature.DIAGNOSIS);

        LearnerProfile profile = profileRepository.findByStudentId(studentId)
                .orElseGet(() -> new LearnerProfile(studentId));
        profile.markOnboarded();
        profileRepository.save(profile);

        // 통째 교체. flush()로 DELETE를 즉시 실행해야 한다 —
        // Hibernate는 flush 시 INSERT를 DELETE보다 먼저 처리하므로, flush 없이 재삽입하면
        // 기존 행과 (tenant_id, student_id, category_code) 유니크 제약이 충돌한다(재저장 시 500).
        interestRepository.deleteByStudentId(studentId);
        skillRepository.deleteByStudentId(studentId);
        interestRepository.flush();
        skillRepository.flush();
        request.interests().stream().distinct()
                .forEach(code -> interestRepository.save(new LearnerInterest(studentId, code)));
        request.skills()
                .forEach(s -> skillRepository.save(new LearnerSkill(studentId, s.categoryCode(), s.level())));

        return getProfile(studentId);
    }
}
