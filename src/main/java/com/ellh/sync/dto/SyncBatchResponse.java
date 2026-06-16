package com.ellh.sync.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SyncBatchResponse {
    private int processedCount;
    private int skippedCount;
    private int conflictCount;
    private List<Long> achievementsUnlocked;
}
