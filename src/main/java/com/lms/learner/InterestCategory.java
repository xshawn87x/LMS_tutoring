package com.lms.learner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 관심분야 카탈로그 — 전역 공유 참조 테이블(테넌트 무관). 관심분야 선택 UI의 목록. */
@Entity
@Table(name = "interest_category")
@Getter
@NoArgsConstructor
public class InterestCategory {

    @Id
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
