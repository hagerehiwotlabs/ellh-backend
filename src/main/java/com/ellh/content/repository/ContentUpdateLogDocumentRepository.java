package com.ellh.content.repository;

import com.ellh.content.document.ContentUpdateLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** MongoDB repository for the content_update_logs collection. */
@Repository
public interface ContentUpdateLogDocumentRepository
        extends MongoRepository<ContentUpdateLogDocument, String> {

    List<ContentUpdateLogDocument> findByContentIdOrderByChangedAtDesc(String contentId);

    List<ContentUpdateLogDocument> findByChangedByOrderByChangedAtDesc(Long changedBy);
}
