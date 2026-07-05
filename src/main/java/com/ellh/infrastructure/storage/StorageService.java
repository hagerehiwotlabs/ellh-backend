package com.ellh.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

/**
 * Enterprise Storage interface for Cloud Providers (AWS S3, Supabase, R2).
 * Enforces strict contracts for uploading and purging PII/Audio files.
 */
public interface StorageService {
    
    /**
     * Uploads an AAC audio file to the cloud bucket.
     * @param file The compressed audio payload.
     * @param fileHash SHA-256 hash used as the object key.
     * @return The public or signed CDN URL of the uploaded object.
     */
    String uploadAudio(MultipartFile file, String fileHash) throws IOException;

    /**
     * Hard-deletes an object from the cloud bucket.
     * Called by the GDPR Cleanup Job.
     */
    void deleteAudio(String fileHash);
}
