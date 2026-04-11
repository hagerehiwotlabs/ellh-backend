package com.ellh.content.repository;

import com.ellh.content.document.VocabularySet;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Spring Data MongoDB repository for vocabulary_sets collection. */
@Repository
public interface VocabularySetRepository extends MongoRepository<VocabularySet, String> {

    List<VocabularySet> findByLanguageCodeAndCefrLevelAndActiveTrue(
            String languageCode, String cefrLevel);

    List<VocabularySet> findByLanguageCodeAndCategoryAndActiveTrue(
            String languageCode, String category);
}
