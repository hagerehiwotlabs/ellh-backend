package com.ellh.content.entity;

/**
 * Writing system for each Ethiopian language.
 * Section 4.5.2.2 — languages.script_type column.
 * Android uses this to select the correct font and keyboard layout.
 * GEEZ_FIDEL → Noto Serif Ethiopic; LATIN_QUBEE → Noto Sans (Afaan Oromo).
 */
public enum ScriptType {
    GEEZ_FIDEL,
    LATIN_QUBEE
}
