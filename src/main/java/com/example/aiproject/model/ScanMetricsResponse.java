package com.example.aiproject.model;

import java.util.UUID;

import lombok.Data;

@Data
public class ScanMetricsResponse {
    private String id;
    private String fileName;
    private ScanMetrics response;

    public ScanMetricsResponse(String id, String fileName, ScanMetrics response) {
        this.id = (id != null) ? id : UUID.randomUUID().toString();
        this.fileName = fileName;
        this.response = response;
    }
}