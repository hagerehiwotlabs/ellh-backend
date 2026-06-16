package com.ellh.sync.controller;

import com.ellh.sync.dto.SyncBatchRequest;
import com.ellh.sync.dto.SyncBatchResponse;
import com.ellh.sync.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SyncBatchResponse> submitSyncBatch(
            @RequestBody SyncBatchRequest request) {
        return ResponseEntity.ok(syncService.processBatch(request));
    }
}
