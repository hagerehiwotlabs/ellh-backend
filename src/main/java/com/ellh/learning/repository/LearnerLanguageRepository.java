package com.ellh.learning.repository;

import com.ellh.learning.entity.LearnerLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LearnerLanguageRepository extends JpaRepository<LearnerLanguage, Long> {
    // Inherits standard CRUD methods like .save(), .findById() automatically!
}
