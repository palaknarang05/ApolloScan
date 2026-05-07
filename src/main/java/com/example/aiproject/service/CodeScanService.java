package com.example.aiproject.service;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import com.example.aiproject.ai.AIInitializer;
import com.example.aiproject.ai.AIScanInsightsService;
import com.example.aiproject.ai.ScanMetricsService;
import com.example.aiproject.core.GithubAPI;
import com.example.aiproject.model.ScanMetrics;
import com.example.aiproject.model.ScanMetricsResponse;
import com.example.aiproject.util.MetricsCalculator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;


import dev.langchain4j.data.document.Document;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CodeScanService {
    private final GithubAPI githubAPI;
    private final AIInitializer aiInitializer;
    private final List<Map<String, Object>> lastRepoMetaData = new ArrayList<>();
    private final Map<String, List<ScanMetricsResponse>> lastScannedRepos = new ConcurrentHashMap<>();
    @Value("${github.server.base.url}")
    private String githubBaseUrl;

    public CodeScanService(GithubAPI githubAPI, AIInitializer aiInitializer) {
        this.githubAPI = githubAPI;
        this.aiInitializer = aiInitializer;
        log.info("CodeScanService initialized");
    }

    private static Predicate<Document> isInterestedFileType() {
        return doc -> {
            String fileName = doc.metadata().getString("fileName");
            return fileName.endsWith(".java") && !fileName.contains("test");
//          || fileName.endsWith(".py")
//          || fileName.endsWith(".js")
//          || fileName.endsWith(".ts");
        };
    }

    public Map<String, Object> getRepoMetaData(String token, String repositoryPath, String modulePath) {

        log.info("getRepoMetaData called for repositoryPath: {}, modulePath: {}", repositoryPath, modulePath);

        if (repositoryPath != null && repositoryPath.startsWith(githubBaseUrl)) {
            repositoryPath = repositoryPath.substring(githubBaseUrl.length()).replaceFirst("/+", "");
        }

        // 📊 Lines of code
        Map<String, Integer> linesOfCode =
                githubAPI.estimateLinesOfCodeByReadingFiles(githubBaseUrl, token, repositoryPath, modulePath);

        List<Map<String, Object>> linesOfCodeList =
                linesOfCode.entrySet()
                        .stream()
                        .map(entry -> Map.of("label", entry.getKey(), "value", (Object) entry.getValue()))
                        .toList();

        // 🌐 Languages
        Map<String, Float> languages =
                githubAPI.calculateRepositoryLanguagesFromLinesOfCode(linesOfCode);

        List<Map<String, Object>> languagesList =
                languages.entrySet()
                        .stream()
                        .map(entry -> Map.of("name", entry.getKey(), "value", (Object) entry.getValue()))
                        .toList();

        // 📦 POM handling
        List<Document> pomContent =
                githubAPI.getPomFileContent(githubBaseUrl, token, repositoryPath, modulePath);

        Map<String, Object> pomMetadata =
                githubAPI.getPomExecutiveSummary(pomContent);

        // ✅ FIX: Safe LLM call
        String executiveSummary;

        if (pomContent == null || pomContent.isEmpty()) {
            executiveSummary = "No pom.xml found in this repository...";
        } else {
            try {
                PromptTemplate promptTemplate =
                        PromptTemplate.from(buildPomExplanationPrompt(pomContent));

                Prompt prompt =
                        promptTemplate.apply(Collections.singletonMap("format", pomContent));

                AIScanInsightsService aiScanInsightsService =
                        aiInitializer.createInsightService(
                                aiInitializer.getAIChatModel(), pomContent
                        );

                executiveSummary = aiScanInsightsService.answer(prompt.toString());

            } catch (Exception e) {
                log.error("Error generating executive summary from AI", e);
                executiveSummary = "Failed to generate summary.";
            }
        }

        // 📈 Metrics
        List<ScanMetricsResponse> existingResults =
                lastScannedRepos.getOrDefault(repositoryPath, List.of());

        List<Integer> overallCompatibilityScores =
                MetricsCalculator.overallCompatibilityScores(existingResults);

        List<Integer> cycloMaticComplexityScores =
                MetricsCalculator.cyclomaticComplexityScores(existingResults);

        List<Integer> checkStyleIssueCounts =
                MetricsCalculator.checkStyleIssueCounts(existingResults);

        List<Integer> incompatibleFileCounts =
                MetricsCalculator.incompatibleFileCounts(existingResults);

        // 📦 Final response
        Map<String, Object> result = Map.of(
                "languages", languagesList,
                "pomMetadata", pomMetadata,
                "executiveSummary", executiveSummary,
                "linesOfCode", linesOfCodeList,
                "overallCompatibilityScores", overallCompatibilityScores,
                "cyclomaticComplexityScores", cycloMaticComplexityScores,
                "checkStyleIssueCounts", checkStyleIssueCounts,
                "incompatibleFileCounts", incompatibleFileCounts
        );

        if (lastRepoMetaData.size() > 1000) {
            lastRepoMetaData.clear();
        }

        lastRepoMetaData.add(result);

        return result;
    }

    public List<Map<String, Object>> getAllLastRepoMetaData() {
        return lastRepoMetaData;
    }

    public List<ScanMetricsResponse> scanRepo(String token, String repositoryPath, String modulePath) throws IOException {
        log.info("scanRepo called for repositoryPath: {}, modulePath: {}", repositoryPath, modulePath);
        if (repositoryPath != null && repositoryPath.startsWith(githubBaseUrl)) {
            repositoryPath = repositoryPath.substring(githubBaseUrl.length()).replaceFirst("/+", "");
        }
        List<Document> allFileContents = githubAPI.getAllFileContents(githubBaseUrl, token, repositoryPath, modulePath).stream().filter(isInterestedFileType()).toList();
        int totalFiles = allFileContents.size();
        ScanMetricsService scanMetricsService = aiInitializer.createMetricsService(aiInitializer.getJsonAIChatModel(), allFileContents);
        log.info("aiScanInsightsService Initialized with model: {}", aiInitializer.getAIChatModel());

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources;
        try {
            resources = resolver.getResources("classpath:/templates/*");
        } catch (IOException e) {
            log.error("Error loading template resources", e);
            throw e;
        }

        List<String> templateContents = new ArrayList<>();
        for (Resource resource : resources) {
            try (InputStream is = resource.getInputStream()) {
                templateContents.add(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                log.error("Error reading template file: {}", resource.getFilename(), e);
                throw e;
            }
        }

        int totalTemplates = templateContents.size();
        AtomicInteger processedFiles = new AtomicInteger();

        String scanPath = repositoryPath + "/" + modulePath;

        List<ScanMetricsResponse> results = templateContents.stream().flatMap(templateContent -> allFileContents.stream().map(doc -> {
            try {
                String template = getDefaultJavaTemplate(doc);
                PromptTemplate promptTemplate = PromptTemplate.from(template);
                Prompt prompt = promptTemplate.apply(Collections.singletonMap("format", templateContent));
                ScanMetrics response = scanMetricsService.answer(prompt.toString());
                processedFiles.getAndIncrement();
                log.info("SCANSTATUS: repo: {}, totalFiles: {}, currentFilesProcessed: {}, progress: {}%", scanPath, totalFiles * totalTemplates, processedFiles.get(), processedFiles.get() * 100 / (totalFiles * totalTemplates));
                return new ScanMetricsResponse(null, doc.metadata().getString("fileName"), response);
            } catch (Exception e) {
                log.error("Error processing document: {}", doc.metadata().getString("fileName"), e);
                return null;
            }
        })).filter(Objects::nonNull).toList();

        lastScannedRepos.put(repositoryPath, results);

        return results;

    }

    public List<ScanMetricsResponse> getAllLastScannedRepos(String repositoryPath) {
        if (lastScannedRepos.isEmpty()) {
            try {
                return loadJsonListFromResources();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return lastScannedRepos.getOrDefault(repositoryPath,Collections.emptyList());
    }


    private String buildPomExplanationPrompt(List<Document> pomContent) {
        return """
        You are a senior software architect. Given the following Maven pom.xml content, provide a detailed explanation covering:
        - The overall purpose and functionality of the project
        - The main technologies, frameworks, and plugins used
        - Key dependencies and their roles
        - Notable configuration details (such as build, profiles, properties)
        - Any unique or advanced features present in the pom.xml
        
        Write your explanation in clear, markup language suitable for an experienced developer or architect.
        
        pom.xml content:
        ```xml
        %s
        ```
        """.formatted(pomContent);
    }

    public String getDefaultJavaTemplate(Document doc) {
        return String.format("""
        With the rising adoption of AI-assisted coding tools, enterprises face challenges such as files that AI tools cannot interpret, poorly structured legacy code, and security risks from AI-generated code lacking governance.
        You are an AI assistant designed to analyze code for compatibility with ai code generation tools such as github copilot. Your task is to analyze and evaluate key metrics based on the source code below:
        ```java
        %s
        ```
        Instruction:
        Return analysis in the below JSON format without any special characters
        {{format}}
        """, doc.text());
    }

    public String getRepositoryPath(String path) {
        if (path != null && path.startsWith(githubBaseUrl)) {
            return path.substring(githubBaseUrl.length()).replaceFirst("/+", "");
        }
        return path;
    }

    public List<ScanMetricsResponse> loadJsonListFromResources() throws IOException {
        ClassPathResource resource = new ClassPathResource("reports.json");
        Path path = resource.getFile().toPath();
        String content = Files.readString(path);
        JSONArray jsonArray = new JSONArray(content);
        List<ScanMetricsResponse> result = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            ScanMetrics metrics = ScanMetrics.fromJson(jsonObject.getJSONObject("response"));
            String fileNameValue = jsonObject.optString("fileName", "unknown");
            String id = jsonObject.optString("id");
            result.add(new ScanMetricsResponse(id, fileNameValue, metrics));
        }
        return result;
    }

    public String createIssue(String token, String repositoryPath, List<String> ids) {
        List<ScanMetricsResponse> scanMetricsResponses = null;
        if (!lastScannedRepos.isEmpty()) {
            scanMetricsResponses = lastScannedRepos.get(repositoryPath);

        } else {
            try {
                scanMetricsResponses = loadJsonListFromResources();
            } catch (IOException e) {
                log.error("Error loading json list from resources: {}", e.getMessage());
            }
        }
       if(scanMetricsResponses==null){
       log.warn("No scan metrics available for repository: {}", repositoryPath);
       return "No scan data found for the repository.";
       }

        scanMetricsResponses.forEach(scanMetricsResponse ->
        {
            ids.forEach(id -> {
                log.info("Creating issue for file: {}", id);
                ScanMetrics scanMetrics = scanMetricsResponse.getResponse();
                Arrays.stream(scanMetrics.getIssues()).toList().forEach(issue -> {
                    if (issue.getIssueId().equalsIgnoreCase(id)) {
                        try {
                            String codes = githubAPI.getFileLines(repositoryPath, scanMetricsResponse.getFileName(), Integer.parseInt(issue.getStart()), Integer.parseInt(issue.getEnd()));
                            githubAPI.createIssue(repositoryPath, issue.getDescription(), getIssueTemplate(issue, scanMetricsResponse.getFileName(), codes));
                        } catch (IOException e) {

                            log.error("Issue creation failed: {}", e.getMessage());
                        }
                    }
                });

            });
        });
        return "Issue creation process completed IDs:"+ ids;
    }

    private String getIssueTemplate(ScanMetrics.Issue issue, String fileName, String codes) {
        return String.format("""
        ## Description
        
        %s
        
        ```java
        %s
        ```
        
        _File reference: %s_
        
        **Severity: `%s`**
        
        **Confidence: `%s`**
        
        ## Suggested Fix
        
        %s
        
        _Generated from **CodeScanService AI Scan**. Please check for correctness._
        """, issue.getDescription(), codes, fileName, issue.getSeverity(), issue.getConfidence(), issue.getSuggestedFix());

    }

    public String generateIssueCode(String token, String repositoryPath, String issueId) {
        List<ScanMetricsResponse> scanMetricsResponses = new ArrayList<>();
        if (!lastScannedRepos.isEmpty()) {
            scanMetricsResponses = lastScannedRepos.get(repositoryPath);
        } else {
            try {
                scanMetricsResponses = loadJsonListFromResources();
            } catch (IOException e) {
                log.error("Error loading json list from resources: {}", e.getMessage());
            }
        }
        AtomicReference<String> issueJson = new AtomicReference<>("");
        scanMetricsResponses.forEach(scanMetricsResponse -> {
            Arrays.stream(scanMetricsResponse.getResponse().getIssues()).toList().forEach(issue -> {
                if (issue.getIssueId().equalsIgnoreCase(issueId)) {
                    log.info("Generating issue code for file: {}, issueId: {}", scanMetricsResponse.getFileName(), issueId);
                    try {
                        String codes = githubAPI.getFileLines(repositoryPath, scanMetricsResponse.getFileName(), Integer.parseInt(issue.getStart()), Integer.parseInt(issue.getEnd()));
                        JSONObject json = new JSONObject();
                        json.put("description", issue.getDescription());
                        json.put("code", codes);
                        json.put("fileName", scanMetricsResponse.getFileName());
                        json.put("severity", issue.getSeverity());
                        json.put("confidence", issue.getConfidence());
                        json.put("suggestedFix", issue.getSuggestedFix());
                        issueJson.set(json.toString());
                    } catch (IOException e) {
                        log.error("Error fetching file lines: {}", e.getMessage());
                    }
                }
            });
        });

        return issueJson.get();
    }

    public String retrievePositives(String token, String repositoryPath, String positiveId) {
        List<ScanMetricsResponse> scanMetricsResponses = new ArrayList<>();
        if (!lastScannedRepos.isEmpty()) {
            scanMetricsResponses = lastScannedRepos.get(repositoryPath);
        } else {
            try {
                scanMetricsResponses = loadJsonListFromResources();
            } catch (IOException e) {
                log.error("Error loading json list from resources: {}", e.getMessage());
            }
        }
        AtomicReference<String> positiveJson = new AtomicReference<>("");

        scanMetricsResponses.forEach(scanMetricsResponse -> {
            Arrays.stream(scanMetricsResponse.getResponse().getPositives()).toList().forEach(positive -> {
                if (positive.getPositiveId().equalsIgnoreCase(positiveId)) {
                    log.info("Generating for file: {}, positiveId: {}", scanMetricsResponse.getFileName(), positiveId);
                    try {
                        String codes = githubAPI.getFileLines(repositoryPath, scanMetricsResponse.getFileName(), Integer.parseInt(positive.getStart()), Integer.parseInt(positive.getEnd()));
                        JSONObject json = new JSONObject();
                        json.put("description", positive.getDescription());
                        json.put("code", codes);
                        json.put("fileName", scanMetricsResponse.getFileName());
                        json.put("positiveId", positiveId);
                        positiveJson.set(json.toString());
                    } catch (IOException e) {
                        log.error("Error fetching file lines: {}", e.getMessage());
                    }
                }
            });
        });
        return positiveJson.get();
    }

    public String generateHtmlReport(String token, String repositoryPath, String modulePath) throws IOException {
        log.info("generateHtmlReport called for repositoryPath: {}, modulePath: {}", repositoryPath, modulePath);
        List<ScanMetricsResponse> scanResults = scanRepo(token, repositoryPath, modulePath);

        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<html><head><style>")
                .append("body { background: #101624; color: #e0e6f0; font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 0; }")
                .append(".container { max-width: 1100px; margin: 32px auto; background: #181f2f; border-radius: 12px; box-shadow: 0 4px 24px #0008; padding: 32px; }")
                .append("h1 { color: #7ec7ff; text-align: center; margin-bottom: 32px; }")
                .append("table { border-collapse: separate; border-spacing: 0; width: 100%; background: #232b3e; border-radius: 8px; overflow: hidden; }")
                .append("th, td { border: 1px solid #2d3953; padding: 12px 10px; text-align: center; }")
                .append("th { background: #1a2233; color: #7ec7ff; font-weight: 600; }")
                .append("tr.group-row td { background: #101624; color: #7ec7ff; font-size: 1.08em; font-weight: 600; text-align: left; border-top: 2px solid #2d3953; }")
                .append("tr.metric-row:hover { background: #27304a; }")
                .append("td { vertical-align: middle; }")
                .append(".section-title { color: #7ec7ff; font-size: 1.1em; font-weight: 600; text-align: left; padding-top: 18px; }")
                .append("</style></head><body><div class='container'>");

        htmlBuilder.append("<h1>ApolloScan Report</h1>");
        htmlBuilder.append("<table>");
        htmlBuilder.append("<tr><th style='width:20%'>File Name</th><th>Metric</th><th>Value</th></tr>");

        String lastFileName = null;
        for (ScanMetricsResponse response : scanResults) {
            String fileName = response.getFileName();
            ScanMetrics metrics = response.getResponse();
            if (!fileName.equals(lastFileName)) {
                htmlBuilder.append("<tr class='group-row'><td colspan='3'>")
                        .append(fileName)
                        .append("</td></tr>");
                lastFileName = fileName;
            }
            // AI Compatibility Score
            htmlBuilder.append("<tr class='metric-row'>")
                    .append("<td></td><td>AI Compatibility Score</td><td>")
                    .append(metrics.getAiCompatibilityScore() != null ? metrics.getAiCompatibilityScore() : "-")
                    .append("</td></tr>");
            // Cyclomatic Complexity
            htmlBuilder.append("<tr class='metric-row'>")
                    .append("<td></td><td>Cyclomatic Complexity</td><td>")
                    .append(metrics.getCyclomaticComplexity() != null ? metrics.getCyclomaticComplexity() : "-")
                    .append("</td></tr>");
            // Coupling Level
            htmlBuilder.append("<tr class='metric-row'>")
                    .append("<td></td><td>Coupling Level</td><td>")
                    .append(metrics.getCouplingLevel() != null ? metrics.getCouplingLevel() : "-")
                    .append("</td></tr>");
            // Dynamic Code Constructs
            htmlBuilder.append("<tr class='metric-row'>")
                    .append("<td></td><td>Dynamic Code Constructs</td><td>")
                    .append(metrics.getDynamicCodeConstructs() != null ? metrics.getDynamicCodeConstructs() : "-")
                    .append("</td></tr>");
            // Relevant Comments
            htmlBuilder.append("<tr class='metric-row'>")
                    .append("<td></td><td>Relevant Comments</td><td>")
                    .append(metrics.getRelevantComments() != null ? metrics.getRelevantComments() : "-")
                    .append("</td></tr>");
            // Positives
            if (metrics.getPositives() != null && metrics.getPositives().length > 0) {
                htmlBuilder.append("<tr><td></td><td colspan='3' class='section-title'>Positives</td></tr>");
                for (ScanMetrics.Positive pos : metrics.getPositives()) {
                    htmlBuilder.append("<tr class='metric-row'>")
                            .append("<td></td><td>Description</td><td>").append(pos.getDescription() != null ? pos.getDescription() : "-").append("</td></tr>")
                            .append("<tr class='metric-row'>")
                            .append("<td></td><td>Confidence</td><td>").append(pos.getConfidence() != null ? pos.getConfidence() : "-").append("</td></tr>");
                }
            }
            // Issues
            if (metrics.getIssues() != null && metrics.getIssues().length > 0) {
                htmlBuilder.append("<tr><td></td><td colspan='3' class='section-title'>Issues</td></tr>");
                for (ScanMetrics.Issue issue : metrics.getIssues()) {
                    htmlBuilder.append("<tr class='metric-row'>")
                            .append("<td></td><td>Description</td><td>").append(issue.getDescription() != null ? issue.getDescription() : "-").append("</td></tr>")
                            .append("<tr class='metric-row'>")
                            .append("<td></td><td>Severity</td><td>").append(issue.getSeverity() != null ? issue.getSeverity() : "-").append("</td></tr>")
                            .append("<tr class='metric-row'>")
                            .append("<td></td><td>Confidence</td><td>").append(issue.getConfidence() != null ? issue.getConfidence() : "-").append("</td></tr>");
                }
            }
        }

        htmlBuilder.append("</table>");
        htmlBuilder.append("</div></body></html>");

        return htmlBuilder.toString();
    }

}