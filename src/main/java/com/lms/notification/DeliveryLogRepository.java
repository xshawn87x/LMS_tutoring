package com.lms.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, UUID> {
    List<DeliveryLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
