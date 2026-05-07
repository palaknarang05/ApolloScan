package com.example.aiproject.model;

import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import lombok.Data;

@Data
public class ScanMetrics {
    private String aiCompatibilityScore;
    private String cyclomaticComplexity;
    private String couplingLevel;
    private String dynamicCodeConstructs;
    private String relevantComments;
    private Positive[] positives;
    private Issue[] issues;

    public static ScanMetrics fromJson(JSONObject jsonObject) {
        ScanMetrics scanMetrics = new ScanMetrics();
        scanMetrics.setAiCompatibilityScore(jsonObject.optString("aiCompatibilityScore", null));
        scanMetrics.setCyclomaticComplexity(jsonObject.optString("cyclomaticComplexity", null));
        scanMetrics.setCouplingLevel(jsonObject.optString("couplingLevel", null));
        scanMetrics.setDynamicCodeConstructs(jsonObject.optString("dynamicCodeConstructs", null));
        scanMetrics.setRelevantComments(jsonObject.optString("relevantComments", null));

        JSONArray positivesArray = jsonObject.optJSONArray("positives");
        if (positivesArray != null) {
            Positive[] positives = new Positive[positivesArray.length()];
            for (int i = 0; i < positivesArray.length(); i++) {
                JSONObject posObj = positivesArray.getJSONObject(i);
                Positive positive = new Positive();
                positive.setPositiveId(posObj.optString("positiveId", null));
                positive.setStart(posObj.optString("start", null));
                positive.setEnd(posObj.optString("end", null));
                positive.setConfidence(posObj.optString("confidence", null));
                positive.setDescription(posObj.optString("description", null));
                positives[i] = positive;
            }
            scanMetrics.setPositives(positives);
        }

        JSONArray issuesArray = jsonObject.optJSONArray("issues");
        if (issuesArray != null) {
            Issue[] issues = new Issue[issuesArray.length()];
            for (int i = 0; i < issuesArray.length(); i++) {
                JSONObject issueObj = issuesArray.getJSONObject(i);
                Issue issue = new Issue();
                issue.setIssueId(issueObj.optString("issueId", null));
                issue.setStart(issueObj.optString("start", null));
                issue.setEnd(issueObj.optString("end", null));
                issue.setSeverity(issueObj.optString("severity", null));
                issue.setConfidence(issueObj.optString("confidence", null));
                issue.setDescription(issueObj.optString("description", null));
                issue.setSuggestedFix(issueObj.optString("suggestedFix", null));
                issues[i] = issue;
            }
            scanMetrics.setIssues(issues);
        }
        return scanMetrics;
    }

    @Data
    public static class Positive {
        private String positiveId = UUID.randomUUID().toString();
        private String start;
        private String end;
        private String confidence;
        private String description;
    }

    @Data
    public static class Issue {
        private String issueId = UUID.randomUUID().toString();
        private String start;
        private String end;
        private String severity;
        private String confidence;
        private String description;
        private String suggestedFix;
    }
}