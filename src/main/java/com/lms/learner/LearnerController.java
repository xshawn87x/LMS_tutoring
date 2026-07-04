package com.lms.learner;

import com.lms.learner.dto.LearnerDtos.InterestCategoryResponse;
import com.lms.learner.dto.LearnerDtos.ProfileRequest;
import com.lms.learner.dto.LearnerDtos.ProfileResponse;
import com.lms.learner.dto.LearnerDtos.RecommendationResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 학습자 본인 화면용 API: 관심분야 카탈로그, 내 프로필(역량·관심분야), 맞춤 추천.
 * 학생 식별자(student_id)는 JWT subject에서 가져온다.
 */
@RestController
public class LearnerController {

    private final LearnerProfileService profileService;
    private final RecommendationService recommendationService;
    private final InterestCategoryRepository categoryRepository;

    public LearnerController(LearnerProfileService profileService,
                            RecommendationService recommendationService,
                            InterestCategoryRepository categoryRepository) {
        this.profileService = profileService;
        this.recommendationService = recommendationService;
        this.categoryRepository = categoryRepository;
    }

    /** 관심분야 선택지 (전역 카탈로그) — 인증된 누구나. */
    @GetMapping("/api/interest-categories")
    public List<InterestCategoryResponse> categories() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(InterestCategoryResponse::from).toList();
    }

    @GetMapping("/api/me/profile")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return profileService.getProfile(jwt.getSubject());
    }

    @PutMapping("/api/me/profile")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ProfileResponse saveProfile(@Valid @RequestBody ProfileRequest request,
                                       @AuthenticationPrincipal Jwt jwt) {
        return profileService.saveProfile(jwt.getSubject(), request);
    }

    @GetMapping("/api/recommendations")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public List<RecommendationResponse> recommendations(@AuthenticationPrincipal Jwt jwt) {
        return recommendationService.recommend(jwt.getSubject());
    }
}
