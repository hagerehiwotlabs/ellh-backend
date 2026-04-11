package com.ellh.content.repository;

import com.ellh.content.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Section 4.5.2.3 — no dedicated index needed beyond UNIQUE on iso_code and name.
 * Language list is small (3 rows in v1.0) and fully cached in Redis.
 */
@Repository
public interface LanguageRepository extends JpaRepository<Language, Long> {

    List<Language> findByActiveTrue();

    Optional<Language> findByIsoCode(String isoCode);

    boolean existsByIsoCode(String isoCode);
}
