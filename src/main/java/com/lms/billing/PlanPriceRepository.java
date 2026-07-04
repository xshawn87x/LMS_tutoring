package com.lms.billing;

import com.lms.platform.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanPriceRepository extends JpaRepository<PlanPrice, Plan> {
}
