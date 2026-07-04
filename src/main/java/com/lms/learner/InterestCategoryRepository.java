package com.lms.learner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterestCategoryRepository extends JpaRepository<InterestCategory, String> {
    List<InterestCategory> findAllByOrderBySortOrderAsc();
}
