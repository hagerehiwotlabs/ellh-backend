package com.ellh.content.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A supported Ethiopian language and its defining properties.
 * Section 4.5.1 — Content Domain; Section 4.5.2.2 — languages table.
 *
 * Design Goal b: adding a new Ethiopian language requires ONLY inserting a new
 * row in this table — no code change required. The language-agnostic content
 * schema in MongoDB stores all content keyed by ISO 639-3 code.
 *
 * Design Goal a: iso_code (amh|tir|orm) is used in ALL API responses wherever
 * a language is referenced.
 */
@Entity
@Table(name = "languages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Language {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name; // English display name

    /** ISO 639-3 code — used in ALL API responses (Design Goal a). */
    @Column(name = "iso_code", nullable = false, unique = true, length = 3)
    private String isoCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "script_type", nullable = false, length = 20)
    private ScriptType scriptType;

    /** Language name in its own script (e.g. አማርኛ for Amharic). */
    @Column(name = "native_name", nullable = false, length = 100)
    private String nativeName;

    /** False for languages in development — not served to learners. */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "total_speakers")
    private Long totalSpeakers;

    @Column(name = "flag_icon", length = 255)
    private String flagIcon;

    @Column(columnDefinition = "TEXT")
    private String description;
}
