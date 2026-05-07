package com.example.aiproject.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class CheckstyleService {

    private static final Logger logger = LoggerFactory.getLogger(CheckstyleService.class);

    public Map<String, String> scanRepository(String repositoryURL, String gitlabToken) throws Exception {
        Path tempDir = Files.createTempDirectory("repo");

        logger.info("Cloning repository from url: {} ", repositoryURL);
        // clone repo
        Git.cloneRepository()
                .setURI(repositoryURL)
                .setDirectory(tempDir.toFile())
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider("oauth2", gitlabToken))
                .call();
        logger.info("Cloned successfully: {}", tempDir.toFile().getAbsolutePath());

        // Run checkstyle using mvnw wrapper

        File mvnw = new File(tempDir.toFile(), isWindows() ? "mvnw.cmd" : "mvnw");
        if (!mvnw.exists()) {
            throw new IllegalStateException("mvnw does not exist");
        }
        if (!isWindows()) mvnw.setExecutable(true);

        logger.info("Running maven checkstyle command.....");
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(tempDir.toFile());
        processBuilder.command(
                mvnw.getAbsolutePath(),
                "checkstyle:checkstyle",
                "-Dcheckstyle.config.location=sun_checks.xml",
                "-Dcheckstyle.output.format=xml",
                "-Dcheckstyle.output.file=target/checkstyle-result.xml",
                "-X", "-e", "--batch-mode"
        );

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // consume output to avoid blocking
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("Maven: {}", line);
            }
        }
        boolean completed = process.waitFor(480, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("Checkstyle process timeout");
        }

        logger.info("Checkstyle process completed");

        String repoName = repositoryURL.substring(repositoryURL.lastIndexOf('/') + 1).replace(".git", "");
        String timeStamp = String.valueOf(System.currentTimeMillis());

        storeCheckstyleResults(tempDir.toFile(), repoName, timeStamp);

        FileSystemUtils.deleteRecursively(tempDir.toFile());
        logger.info("Deleted temp cloned directory: {}", tempDir.toFile().getAbsolutePath());

        Map<String, String> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "checkstyle completed. Report saved at: reports/ directory");
        return response;

    }

    private void storeCheckstyleResults(File cloneDir, String repoName, String timeStamp) throws Exception {
        List<Path> resultFiles = Files.walk(cloneDir.toPath())
                .filter(p -> p.getFileName().toString().equals("checkstyle-result.xml"))
                .collect(Collectors.toList());

        if (resultFiles.isEmpty()) {
            throw new IllegalStateException("No checkstyle-result.xml file found in any module");
        }

        File reportsDir = new File("reports");
        Files.createDirectories(reportsDir.toPath());

        if (resultFiles.size() == 1) {
            Path source = resultFiles.get(0);
            File jsonDest = new File(reportsDir, timeStamp + "_" + repoName + ".json");
            convertXmlToJson(source, jsonDest, repoName);
//      Files.copy(source, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
//      logger.info("Saved single module checkstyle report to: {}", dest.getAbsolutePath());
        } else {
            for (Path resultFile : resultFiles) {
                String moduleName = resultFile.getParent().getParent().getFileName().toString();
                File jsonDest = new File(reportsDir, timeStamp + "_" + repoName + "_" + moduleName + ".json");
//        Files.copy(resultFile, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                convertXmlToJson(resultFile, jsonDest, moduleName);
//        logger.info("Saved multiple module checkstyle report to: {}", dest.getAbsolutePath());
            }
        }
    }


    private void convertXmlToJson(Path xmlPath, File jsonOutFile, String moduleName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlPath.toFile());

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("module", moduleName);

        ArrayNode filesArray = mapper.createArrayNode();
        NodeList fileNodes = doc.getElementsByTagName("file");


        for (int i = 0; i < fileNodes.getLength(); i++) {
            Element fileElem = (Element) fileNodes.item(i);
            ObjectNode fileNode = mapper.createObjectNode();
            fileNode.put("file", fileElem.getAttribute("name"));

            ArrayNode errorArray = mapper.createArrayNode();
            NodeList errors = fileElem.getElementsByTagName("error");

            for (int j = 0; j < errors.getLength(); j++) {
                Element errorElem = (Element) errors.item(j);
                ObjectNode error = mapper.createObjectNode();
                error.put("line", errorElem.getAttribute("line"));
                error.put("severity", errorElem.getAttribute("severity"));
                error.put("message", errorElem.getAttribute("message"));
                error.put("source", errorElem.getAttribute("source"));
                errorArray.add(error);
            }
            fileNode.set("errors", errorArray);
            filesArray.add(fileNode);
        }
        root.set("files", filesArray);
        mapper.writerWithDefaultPrettyPrinter().writeValue(jsonOutFile, root);
        logger.info("Converted {} to JSON -> {}", xmlPath.getFileName(), jsonOutFile.getAbsolutePath());
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }


}
