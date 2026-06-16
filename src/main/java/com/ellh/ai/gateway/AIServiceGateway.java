package com.ellh.ai.gateway;

import com.ellh.ai.dto.PronunciationResponse;
import com.ellh.user.entity.User;
import org.springframework.web.multipart.MultipartFile;

/**
 * Outbound AI Service Gateway Interface.
 * Orchestrates calls to Python AI Speech models.
 */
public interface AIServiceGateway {

    /**
     * Transmits a voice recording to the AI model for phonetic validation.
     *
     * @param audio      compressed audio payload
     * @param user       authenticated client User details
     * @param targetWord Ge'ez phrase we are grading
     * @param audioHash  SHA-256 check stamp for deduplication
     */
    PronunciationResponse analyzePronunciation(
            MultipartFile audio,
            User user,
            String targetWord,
            String audioHash) throws Exception;
}
