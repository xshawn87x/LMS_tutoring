package com.lms.placement;

import com.lms.placement.dto.PlacementDtos.ApplyResult;
import com.lms.placement.dto.PlacementDtos.PlacementRequest;
import com.lms.placement.dto.PlacementDtos.Recommendation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 성적 기반 반편성 API. 추천(미적용)/적용 모두 INSTRUCTOR/ADMIN. */
@RestController
public class PlacementController {

    private final PlacementService service;

    public PlacementController(PlacementService service) {
        this.service = service;
    }

    @PostMapping("/api/placement/recommend")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public List<Recommendation> recommend(@RequestBody PlacementRequest req) {
        return service.recommend(req.bands());
    }

    @PostMapping("/api/placement/apply")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public ApplyResult apply(@RequestBody PlacementRequest req) {
        return service.apply(req.bands());
    }
}
