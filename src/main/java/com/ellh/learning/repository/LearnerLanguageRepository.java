package com.ellh.learning.repository;

import com.ellh.learning.entity.LearnerLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LearnerLanguageRepository extends JpaRepository<LearnerLanguage, Long> {

    /**
     * Find active language progress for a specific user and language ID.
     */
    @Query("SELECT ll FROM LearnerLanguage ll WHERE ll.user.id = :userId AND ll.language.id = :languageId")
    Optional<LearnerLanguage> findByUserIdAndLanguageId(
            @Param("userId") Long userId, 
            @Param("languageId") Long languageId);
}
