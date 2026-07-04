package com.lms.placement.dto;

import java.util.List;
import java.util.UUID;

/** 성적 기반 반편성 — 평균 성적 구간(밴드)별로 학생을 레벨반에 배치. */
public final class PlacementDtos {

    private PlacementDtos() {
    }

    /** 밴드: 이 최소 백분율 이상이면 해당 반(groupId)에 배치. (예: 80→상위반, 60→중위반, 0→기초반) */
    public record Band(int minPercent, UUID groupId) {
    }

    public record PlacementRequest(List<Band> bands) {
    }

    /** 한 학생의 배치 추천 결과. current*는 현재 소속(밴드 반 중), moved=반 이동 여부. */
    public record Recommendation(
            String studentSubject, String studentName, int avgPercent, int examCount,
            UUID groupId, String groupName,
            UUID currentGroupId, String currentGroupName, boolean moved) {
    }

    /** 적용 결과 — 새로 배치한 인원 + 대상 학생 수 + 추천 상세. */
    public record ApplyResult(int assigned, int studentsPlaced, List<Recommendation> recommendations) {
    }
}
