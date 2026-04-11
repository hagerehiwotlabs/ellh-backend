package com.ellh.content.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Generic paged response wrapper for paginated API endpoints.
 * Used by admin feedback list, lesson list, and future paginated endpoints.
 */
@Getter
@Builder
public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
