package com.example.aiproject.ai;

import static com.example.aiproject.util.Utils.glob;
import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocumentsRecursively;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "ollama.chat-model")
public class AIInitializer {

    private String endpoint;

    private boolean logEnabled;
    @Value("${test-repo.root.path}")
    private String testRepoRootPath;
    private String model;


    @Bean(name = "chatModel", defaultCandidate = true)
    public OllamaChatModel getAIChatModel() {
        log.info("Initializing Ollama AI Chat Model connection with Endpoint: {}, Deployment Name: {}", endpoint, model);
        return OllamaChatModel.builder()
                .baseUrl(endpoint)
                .modelName(model)
                .temperature(1.2)
                .topP(0.9)
                .maxRetries(5)
                .logRequests(logEnabled)
                .logResponses(logEnabled)
                .build();
    }

    @Bean(name = "jsonChatModel")
    public OllamaChatModel getJsonAIChatModel() {
        log.info("Initializing Ollama AI Chat Model connection with JSON schema with Endpoint: {}, Deployment Name: {}", endpoint, model);
        return OllamaChatModel.builder()
                .baseUrl(endpoint)
                .modelName(model)
                .temperature(1.2)
                .topP(0.9)
                .maxRetries(5)
                .responseFormat(new ResponseFormat.Builder().type(ResponseFormatType.JSON).jsonSchema(getJsonSchema()).build())
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    @ConditionalOnBooleanProperties({
            @ConditionalOnBooleanProperty(name = "ai-test.enabled"),
            @ConditionalOnBooleanProperty(name = "cli-chat.enabled")
    })
    public AIScanInsightsService getTestInsightsService(@Autowired OllamaChatModel chatModel) {
        return AiServices.builder(AIScanInsightsService.class)
                .chatModel(chatModel)
//        .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
//        .contentRetriever(createTestContentRetriever())
                .build();
    }

    public AIScanInsightsService createInsightService(OllamaChatModel chatModel, List<Document> documents) {
        log.info("Creating new Insights Service");
        return AiServices.builder(AIScanInsightsService.class)
                .chatModel(chatModel)
//        .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
//        .contentRetriever(createContentRetriever(documents))
                .build();
    }

    public ScanMetricsService createMetricsService(OllamaChatModel chatModel, List<Document> documents) {
        log.info("Creating new JSON based Metrics Service");
        return AiServices.builder(ScanMetricsService.class)
                .chatModel(chatModel)
//        .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
//        .contentRetriever(createContentRetriever(documents))
                .build();
    }

    private ContentRetriever createTestContentRetriever() {
        log.info("Creating new Testing ContentRetriever with test repo path: {}", testRepoRootPath);
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
        EmbeddingStore<TextSegment> embeddingStore = embed(Path.of(testRepoRootPath), glob("**.java"), embeddingModel);

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(70)
//        .dynamicMaxResults(query -> 3)
                .minScore(0.3)
//        .dynamicMinScore(query -> 0.75)
//        .filter(metadataKey("userId").isEqualTo("12345"))
//        .dynamicFilter(query -> {
//          String userId = getUserId(query.metadata().chatMemoryId());
//          return metadataKey("userId").isEqualTo(userId);
//        })
                .build();
    }

    private ContentRetriever createContentRetriever(List<Document> documents) {
        log.info("Creating new ContentRetriever");
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
        EmbeddingStore<TextSegment> embeddingStore = embed(documents, embeddingModel);
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(70)
//        .dynamicMaxResults(query -> 3)
                .minScore(0.3)
//        .dynamicMinScore(query -> 0.75)
//        .filter(metadataKey("userId").isEqualTo("12345"))
//        .dynamicFilter(query -> {
//          String userId = getUserId(query.metadata().chatMemoryId());
//          return metadataKey("userId").isEqualTo(userId);
//        })
                .build();
    }

    private EmbeddingStore<TextSegment> embed(List<Document> documents, EmbeddingModel embeddingModel) {
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);

        documents.parallelStream().forEach(document -> {
            List<TextSegment> segments = splitter.split(document);
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            embeddingStore.addAll(embeddings, segments);
        });

        return embeddingStore;

    }

    private EmbeddingStore<TextSegment> embed(Path dirPath, PathMatcher glob, EmbeddingModel embeddingModel) {
        DocumentParser documentParser = new TextDocumentParser();
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        List<Document> documents = loadDocumentsRecursively(dirPath, glob, documentParser);

        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
        documents.parallelStream().forEach(document -> {
            List<TextSegment> segments = splitter.split(document);
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            embeddingStore.addAll(embeddings, segments);
        });

        return embeddingStore;
    }

    private JsonSchema getJsonSchema() {
        return JsonSchema.builder()
                .name("Metrics")
                .rootElement(JsonObjectSchema.builder()
                        .addStringProperty("aiCompatibilityScore")
                        .addStringProperty("cyclomaticComplexity")
                        .addStringProperty("couplingLevel")
                        .addStringProperty("dynamicCodeConstructs")
                        .addStringProperty("relevantComments")
                        .addProperty("positives", JsonArraySchema.builder()
                                .items(JsonObjectSchema.builder()
                                        .addStringProperty("start")
                                        .addStringProperty("end")
                                        .addStringProperty("confidence")
                                        .addStringProperty("description")
                                        .required("start", "end", "confidence", "description")
                                        .build())
                                .build())
                        .addProperty("issues", JsonArraySchema.builder().items(JsonObjectSchema.builder()
                                        .addStringProperty("start")
                                        .addStringProperty("end")
                                        .addStringProperty("severity")
                                        .addStringProperty("confidence")
                                        .addStringProperty("description")
                                        .addStringProperty("suggestedFix")
                                        .required("start", "end", "severity", "confidence", "description", "suggestedFix")
                                        .build())
                                .build())
                        .required("aiCompatibilityScore", "cyclomaticComplexity", "couplingLevel", "dynamicCodeConstructs", "relevantComments", "positives", "issues")
                        .build())
                .build();
    }
}
