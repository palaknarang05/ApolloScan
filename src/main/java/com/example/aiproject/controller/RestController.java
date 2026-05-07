package com.example.aiproject.controller;


import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.example.aiproject.model.RepoScanRequest;
import com.example.aiproject.request.CheckstyleRequest;
import com.example.aiproject.service.CodeScanService;
import com.example.aiproject.service.CheckstyleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;


import lombok.extern.slf4j.Slf4j;

@Slf4j
@org.springframework.web.bind.annotation.RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping(produces = "application/json")
public class RestController {
    private final CheckstyleService checkstyleService;

    @Autowired
    private CodeScanService CodeScanServiceService;

    public RestController(CheckstyleService checkstyleService) {
        this.checkstyleService = checkstyleService;
    }

    @PostMapping("/get-checkstyle-result")
    public ResponseEntity<Map<String, String>> runCheckstyle(@RequestBody CheckstyleRequest request) {
        try {

            Map<String, String> result = checkstyleService.scanRepository(request.getRepoURL(), request.getGitlabToken());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    @PutMapping("/scan-repo")
    public ResponseEntity<?> scanRepositoryMetadata(@RequestBody RepoScanRequest request) {
        log.info("Received scanRepositoryMetadata request for repositoryPath: {}, modulePath: {}", request.getRepositoryPath(), request.getModulePath());
        String token = request.getToken();
        String repositoryPath = CodeScanServiceService.getRepositoryPath(request.getRepositoryPath());
        String modulePath = request.getModulePath();

        if (token == null) {
            log.warn("Missing token in scanRepository request");
            return ResponseEntity.badRequest().body("Missing token");
        }
        return ResponseEntity.ok(CodeScanServiceService.getRepoMetaData(token, repositoryPath, modulePath));
    }

    @PutMapping("/scan-documents-with-templates")
    public ResponseEntity<?> scanDocumentsWithTemplates(@RequestBody RepoScanRequest request) throws IOException {
        log.info("Received scanDocumentsWithTemplates request for repositoryPath: {}, modulePath: {}", request.getRepositoryPath(), request.getModulePath());
        String token = request.getToken();
        String repositoryPath = CodeScanServiceService.getRepositoryPath(request.getRepositoryPath());
        String modulePath = request.getModulePath();
        return ResponseEntity.ok(CodeScanServiceService.scanRepo(token, repositoryPath, modulePath));
    }

    @PutMapping("get-reports")
    public ResponseEntity<?> getReports(@RequestBody RepoScanRequest request) {
        log.info("Received getReports request for repositoryPath: {}, modulePath: {}", request.getRepositoryPath(), request.getModulePath());
        String repositoryPath = CodeScanServiceService.getRepositoryPath(request.getRepositoryPath());

        return ResponseEntity.ok(CodeScanServiceService.getAllLastScannedRepos(repositoryPath));
    }

    @PutMapping("create-issues")
    public ResponseEntity<?> createIssues(@RequestBody RepoScanRequest request) {
        String token = request.getToken();
        String repositoryPath = CodeScanServiceService.getRepositoryPath(request.getRepositoryPath());
        List<String> ids = request.getId();
        CodeScanServiceService.createIssue(token, repositoryPath, ids);
        return ResponseEntity.ok("Success");
    }

    @PutMapping("/get-codes")
    public ResponseEntity<?> getCodes(@RequestBody RepoScanRequest request) {
        log.info("Received getCodes request for repositoryPath: {}, modulePath: {}", request.getRepositoryPath(), request.getModulePath());
        String token = request.getToken();
        String repositoryPath = CodeScanServiceService.getRepositoryPath(request.getRepositoryPath());
        String modulePath = request.getModulePath();

        return ResponseEntity.ok(CodeScanServiceService.generateIssueCode(token, repositoryPath, modulePath));
    }

    @PutMapping("/get-positives")
    public ResponseEntity<?> getPositivesResponse(@RequestBody RepoScanRequest request) {
        log.info("Received getCodes request for repositoryPath: {}, modulePath: {}", request.getRepositoryPath(), request.getModulePath());
        String token = request.getToken();
        String repositoryPath = CodeScanServiceService.getRepositoryPath(request.getRepositoryPath());
        String modulePath = request.getModulePath();
        return ResponseEntity.ok(CodeScanServiceService.retrievePositives(token, repositoryPath, modulePath));
    }

    @GetMapping("/all-scanned-repo")
    public ResponseEntity<?> getLastScannedRepo() {
        List<Map<String, Object>> getAll = CodeScanServiceService.getAllLastRepoMetaData();
        return ResponseEntity.ok(getAll);
    }

    @PutMapping(value = "/scan-html-report", produces = "text/html")
    public ResponseEntity<?> scanHtmlReport(@RequestBody RepoScanRequest request) throws IOException {
        log.info("Received scanHtmlReport request for repositoryPath: {}, modulePath: {}", request.getRepositoryPath(), request.getModulePath());
        String token = request.getToken();
        String repositoryPath = CodeScanServiceService.getRepositoryPath(request.getRepositoryPath());
        String modulePath = request.getModulePath();
        String htmlReport = CodeScanServiceService.generateHtmlReport(token, repositoryPath, modulePath);
        return ResponseEntity.ok().header("Content-Type", "text/html").body(htmlReport);
    }
}