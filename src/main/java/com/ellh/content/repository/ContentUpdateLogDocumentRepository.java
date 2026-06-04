package com.ellh.content.repository;

import com.ellh.content.document.ContentUpdateLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentUpdateLogDocumentRepository
        extends MongoRepository<ContentUpdateLogDocument, String>,
                ContentUpdateLogDocumentRepositoryCustom { 

    List<ContentUpdateLogDocument> findByContentIdOrderByChangedAtDesc(String contentId);

    List<ContentUpdateLogDocument> findByChangedByOrderByChangedAtDesc(Long changedBy);

    // Remove the void logGdprPurge(...) declaration - it's now in the custom interface
}
