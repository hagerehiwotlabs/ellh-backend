package com.ellh.user.repository;

import com.ellh.user.entity.DiagnosticAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiagnosticAssessmentRepository extends JpaRepository<DiagnosticAssessment, Long> {

    Optional<DiagnosticAssessment> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
