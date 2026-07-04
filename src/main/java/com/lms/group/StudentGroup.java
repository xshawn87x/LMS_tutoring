package com.lms.group;

import com.lms.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 반/기수(수강생 그룹). */
@Entity
@Table(name = "student_group")
@Getter
@NoArgsConstructor
public class StudentGroup extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column
    private String term;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public StudentGroup(String name, String term, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.term = term;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = OffsetDateTime.now();
    }

    public void update(String name, String term, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.term = term;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
