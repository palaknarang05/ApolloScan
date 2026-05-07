package com.example.aiproject.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTreeEntry;
import org.kohsuke.github.GitHub;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GithubAPI {

    private static final Map<String, GitHub> apiCache = new ConcurrentHashMap<>();

    @Value("${github.server.base.url}")
    private String githubBaseUrl;

    private static GitHub getGitHubApi(String serverUrl, String token, String repositoryPath) {
        String key = serverUrl + ":" + repositoryPath;
        return apiCache.computeIfAbsent(key, k -> {
            try{
                if(serverUrl==null || serverUrl.isEmpty() || serverUrl.equals("https://github.com")){
                    return GitHub.connectUsingOAuth(token);
                }
            }
            catch (IOException e){
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    public String getDefaultBranch(String repositoryPath) {
        try {
            GitHub gitHub = getGitHubApi(githubBaseUrl, null, repositoryPath);
            GHRepository repository=gitHub.getRepository(repositoryPath);
            return repository.getDefaultBranch();
        } catch (IOException e) {
            log.error("Error fetching default branch for repo path: {}. Details: ", repositoryPath, e);
            throw new RuntimeException(e);
        }
    }

    public List<dev.langchain4j.data.document.Document> getAllFileContents(
            String serverUrl,
            String token,
            String repositoryPath,
            String modulePath
    ) {
        Map<String, String> fileContents = new ConcurrentHashMap<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(10)) {
            GitHub gitHub = getGitHubApi(serverUrl, token, repositoryPath);
            GHRepository repository = gitHub.getRepository(repositoryPath);

            String branch = getDefaultBranch(repositoryPath);

            List<GHTreeEntry> treeEntries = repository.getTreeRecursive(branch, Integer.MAX_VALUE).getTree();

            List<Future<Void>> futures = new ArrayList<>();

            for (GHTreeEntry entry : treeEntries) {
                if ("blob".equals(entry.getType())) {
                    String path = entry.getPath();

                    if (!modulePath.isEmpty() && !path.startsWith(modulePath)) {
                        continue;
                    }

                    futures.add(executor.submit(() -> {
                        try {
                            GHContent file = repository.getFileContent(path, branch);
                            String content = file.isFile() ? file.getContent() : "";

                            fileContents.put(path, content);
                        } catch (Exception e) {
                            log.error("Error fetching file: {}. Details: ", path, e);
                        }
                        return null;
                    }));
                }
            }

            for (Future<Void> future : futures) {
                future.get();
            }

        } catch (Exception e) {
            log.error("Error fetching all file contents for repo path: {}. Details: ", repositoryPath, e);
            throw new RuntimeException(e);
        }

        return fileContents.entrySet().stream()
                .map(entry -> dev.langchain4j.data.document.Document.from(
                        (entry.getValue() == null || entry.getValue().isBlank()) ? "Blank" : entry.getValue(),
                        new dev.langchain4j.data.document.Metadata(Map.of("fileName", entry.getKey()))
                ))
                .toList();
    }


    public Map<String, Float> calculateRepositoryLanguagesFromLinesOfCode(Map<String, Integer> linesOfCode) {
        int totalLines = linesOfCode.values().stream().mapToInt(Integer::intValue).sum();
        Map<String, Float> languagePercentages = new HashMap<>();
        if (totalLines == 0) return languagePercentages;
        for (Map.Entry<String, Integer> entry : linesOfCode.entrySet()) {
            float percent = (entry.getValue() * 100.0f) / totalLines;
            languagePercentages.put(entry.getKey(), percent);
        }
        return languagePercentages;
    }

    public List<dev.langchain4j.data.document.Document> getPomFileContent(String serverUrl, String token, String repositoryPath, String modulePath) {
        try {
            GitHub gitHub = getGitHubApi(serverUrl, token, repositoryPath);
            GHRepository repository = gitHub.getRepository(repositoryPath);
            String branch = getDefaultBranch(repositoryPath);
            String pomPath = modulePath.isEmpty() ? "pom.xml" : modulePath + "/pom.xml";
            GHContent file = repository.getFileContent(pomPath, branch);
            String pomContent = file.isFile() ? file.getContent() : "";
            dev.langchain4j.data.document.Document doc = dev.langchain4j.data.document.Document.from(pomContent, new dev.langchain4j.data.document.Metadata(Map.of("fileName", "pom.xml")));
            return List.of(doc);
        } catch (org.kohsuke.github.GHFileNotFoundException e) {
            log.warn("pom.xml not found in repo: {}. Skipping pom analysis.", repositoryPath);
            return List.of();
        } catch (IOException e) {
            log.error("Error fetching pom.xml for repo path: {}. Details: ", repositoryPath, e);
            return List.of();
        }
    }

    public Map<String, Object> getPomExecutiveSummary(List<dev.langchain4j.data.document.Document> pomDocuments) {
        Map<String, Object> summary = new HashMap<>();
            if (pomDocuments.isEmpty()) {
                summary.put("groupId", "unknown");
                summary.put("artifactId", "unknown");
                summary.put("version", "unknown");
                summary.put("name", "No pom.xml found");
                return summary;
            }
        for (dev.langchain4j.data.document.Document pomDoc : pomDocuments) {
            try {
                String pomContent = pomDoc.text();
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                org.w3c.dom.Document doc = dBuilder.parse(new InputSource(new StringReader(pomContent)));
                doc.getDocumentElement().normalize();

                summary.put("groupId", doc.getElementsByTagName("groupId").item(0).getTextContent());
                summary.put("artifactId", doc.getElementsByTagName("artifactId").item(0).getTextContent());
                summary.put("version", doc.getElementsByTagName("version").item(0).getTextContent());
                summary.put("name", doc.getElementsByTagName("name").item(0) != null ? doc.getElementsByTagName("name").item(0).getTextContent() : "");
            } catch (Exception e) {
                log.error("Error parsing pom.xml content. Details: ", e);
                throw new RuntimeException(e);
            }
        }
        return summary;
    }

    public String createIssue(String repositoryPath, String title, String description) {
        try {
            GitHub gitHub = getGitHubApi(githubBaseUrl, null, repositoryPath);
            GHRepository repository= gitHub.getRepository(repositoryPath);
            repository.createIssue(title).body(description).create();
        } catch (IOException e) {
            log.error("Error creating issue in repo path: {}. Details: ", repositoryPath, e);
            throw new RuntimeException(e);
        }
        return "Success";
    }

    public String getFileLines(String repositoryPath, String filePath, int startLine, int endLine) throws IOException {
        // Fetch the raw file content from GitHub
        GitHub gitHub = getGitHubApi(githubBaseUrl, null, repositoryPath);
        GHRepository repository = gitHub.getRepository(repositoryPath);
        GHContent file;
        try{
            file=repository.getFileContent(filePath,getDefaultBranch(repositoryPath));
        } catch (FileNotFoundException e) {
            return null;
        }

        String content = new String(Base64.getDecoder().decode(file.getContent()),StandardCharsets.UTF_8);
        String[] lines = content.split("\n");
        // Adjust for 1-based line numbers
        startLine = Math.max(1, startLine);
        endLine = Math.min(lines.length, endLine);
        StringBuilder sb = new StringBuilder();
        for (int i = startLine - 1; i < endLine; i++) {
            sb.append(lines[i]).append(System.lineSeparator());
        }
        return sb.toString();
    }

    public Map<String, Integer> estimateLinesOfCodeByReadingFiles(String serverUrl, String token, String repositoryPath, String modulePath) {
        List<dev.langchain4j.data.document.Document> docs = getAllFileContents(serverUrl, token, repositoryPath, modulePath);
        Map<String, Integer> linesPerLanguage = new HashMap<>();

        for (dev.langchain4j.data.document.Document doc : docs) {
            String fileName = doc.metadata().getString("fileName");
            String content = doc.text();
            int lines = (int) content.lines().count();

            String language = detectLanguageByExtension(fileName);
            linesPerLanguage.merge(language, lines, Integer::sum);
        }
        return linesPerLanguage;
    }

    private String detectLanguageByExtension(String fileName) {
        if (fileName.endsWith(".java")) return "Java";
        if (fileName.endsWith(".kt")) return "Kotlin";
        if (fileName.endsWith(".scala")) return "Scala";
        if (fileName.endsWith(".groovy")) return "Groovy";
        if (fileName.endsWith(".js")) return "JavaScript";
        if (fileName.endsWith(".jsx")) return "JavaScript";
        if (fileName.endsWith(".ts")) return "TypeScript";
        if (fileName.endsWith(".tsx")) return "TypeScript";
        if (fileName.endsWith(".json")) return "JSON";
        if (fileName.endsWith(".xml")) return "XML";
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) return "YAML";
        if (fileName.endsWith(".sh")) return "Shell";
        if (fileName.endsWith(".bat")) return "Batch";
        if (fileName.endsWith(".ps1")) return "PowerShell";
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) return "HTML";
        if (fileName.endsWith(".css")) return "CSS";
        if (fileName.endsWith(".scss")) return "SCSS";
        if (fileName.endsWith(".less")) return "LESS";
        if (fileName.endsWith(".md")) return "Markdown";
        if (fileName.endsWith(".py")) return "Python";
        if (fileName.endsWith(".rb")) return "Ruby";
        if (fileName.endsWith(".php")) return "PHP";
        if (fileName.endsWith(".c")) return "C";
        if (fileName.endsWith(".h")) return "C/C++ Header";
        if (fileName.endsWith(".cpp") || fileName.endsWith(".cc") || fileName.endsWith(".cxx")) return "C++";
        if (fileName.endsWith(".hpp") || fileName.endsWith(".hh") || fileName.endsWith(".hxx")) return "C++ Header";
        if (fileName.endsWith(".go")) return "Go";
        if (fileName.endsWith(".rs")) return "Rust";
        if (fileName.endsWith(".swift")) return "Swift";
        if (fileName.endsWith(".dart")) return "Dart";
        if (fileName.endsWith(".m") || fileName.endsWith(".mm")) return "Objective-C";
        if (fileName.endsWith(".cs")) return "C#";
        if (fileName.endsWith(".vb")) return "VB.NET";
        if (fileName.endsWith(".pl")) return "Perl";
        if (fileName.endsWith(".r")) return "R";
        if (fileName.endsWith(".sql")) return "SQL";
        if (fileName.endsWith(".gradle")) return "Gradle";
        if (fileName.endsWith(".pom") || fileName.endsWith(".pom.xml")) return "Maven";
        if (fileName.endsWith(".make") || fileName.endsWith("Makefile")) return "Makefile";
        if (fileName.endsWith(".dockerfile") || fileName.equalsIgnoreCase("Dockerfile")) return "Dockerfile";
        if (fileName.endsWith(".ini")) return "INI";
        if (fileName.endsWith(".conf")) return "Config";
        if (fileName.endsWith(".properties")) return "Properties";
        if (fileName.endsWith(".env")) return "Env";
        if (fileName.endsWith(".txt")) return "Text";
        if (fileName.endsWith(".log")) return "Log";
        if (fileName.endsWith(".csv")) return "CSV";
        if (fileName.endsWith(".cmd")) return "CMD";
        return "Other";
    }
}
