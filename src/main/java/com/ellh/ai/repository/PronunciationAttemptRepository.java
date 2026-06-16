package com.ellh.ai.repository;

import com.ellh.ai.entity.PronunciationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PronunciationAttemptRepository extends JpaRepository<PronunciationAttempt, Long> {
    List<PronunciationAttempt> findByUserId(Long userId);
}
