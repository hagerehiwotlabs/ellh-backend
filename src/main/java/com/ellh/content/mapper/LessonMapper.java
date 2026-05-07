package com.ellh.content.mapper;

import com.ellh.content.document.LessonContent;
import com.ellh.content.dto.*;
import com.ellh.content.entity.Exercise;
import com.ellh.content.entity.Language;
import com.ellh.content.entity.Lesson;
import com.ellh.user.entity.CefrLevel;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper — converts between JPA entities, MongoDB documents, and DTOs.
 * Section 4.5.1 — Content Domain (LessonMapper interface).
 *
 * @Mapper(componentModel = "spring"): MapStruct generates a Spring @Component
 * implementation that is injectable via @Autowired / constructor injection.
 *
 * The generated implementation is in target/generated-sources/annotations/
 * after mvn compile.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LessonMapper {

    // ── Language ──────────────────────────────────────────────────────────────

    LanguageResponse toLanguageResponse(Language language);

    List<LanguageResponse> toLanguageResponseList(List<Language> languages);

    // ── Lesson ────────────────────────────────────────────────────────────────

    /**
     * Maps Lesson entity to LessonResponse.
     * content and versionStamp are set manually in LessonService after
     * the MongoDB document is fetched — MapStruct cannot cross-store map.
     */
    @Mapping(source = "language.id",      target = "languageId")
    @Mapping(source = "language.isoCode", target = "languageCode")
    @Mapping(target = "content",          ignore = true)  // set manually
    @Mapping(target = "versionStamp",     ignore = true)  // set from MongoDB doc
    LessonResponse toLessonResponse(Lesson lesson);

    List<LessonResponse> toLessonResponseList(List<Lesson> lessons);

    // ── LessonContent (MongoDB → DTO) ─────────────────────────────────────────

    LessonContentDto toLessonContentDto(LessonContent content);

    // ── Exercise ──────────────────────────────────────────────────────────────

    @Mapping(source = "lesson.id", target = "lessonId")
    ExerciseResponse toExerciseResponse(Exercise exercise);

    List<ExerciseResponse> toExerciseResponseList(List<Exercise> exercises);

    // ── String ↔ CefrLevel ────────────────────────────────────────────────────

    default CefrLevel toCefrLevel(String s) {
        return s == null ? null : CefrLevel.valueOf(s.toUpperCase());
    }
}
