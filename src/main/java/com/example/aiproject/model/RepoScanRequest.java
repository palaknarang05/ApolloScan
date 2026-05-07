package com.example.aiproject.model;

import java.util.List;

import lombok.Data;

@Data
public class RepoScanRequest {
    private String token;
    private String repositoryPath;
    //Optional
    private String modulePath = "";
    private List<String> id;
}