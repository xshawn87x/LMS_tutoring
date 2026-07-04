package com.lms.billing;

import com.lms.feature.Feature;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureAddonPriceRepository extends JpaRepository<FeatureAddonPrice, Feature> {
}
