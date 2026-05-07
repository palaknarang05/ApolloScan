package com.example.aiproject.ai;



import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import static com.example.aiproject.util.Utils.toPath;

@Slf4j
@Component
@ConditionalOnBooleanProperty(value = "ai-test.enabled")
public class AITest {
    @Autowired
    private AIScanInsightsService insightService;


    @PostConstruct
    public void test() {
        var prompt = """
        With the rising adoption of AI-assisted coding tools, enterprises face challenges such as files that AI tools cannot interpret, poorly structured legacy code, and security risks from AI-generated code lacking governance. ApolloScan addresses these issues by scanning repositories to detect files AI tools struggle with, highlighting AI adoption issues, and providing risk insights before onboarding AI-assisted coding tools.
        You are an AI assistant designed to analyse code for software quality metrics. Your task is to analyze and evaluate key metrics based on the source code of all java services. This is crucial for identifying code that is difficult to test and maintain expecially in legacy systems.
        
        Instruction:
        Return your detailed analysis , comments and recommendations as a JSON object.
        """;
//    var result = openAIClient.getChatCompletions(deploymentName, new ChatCompletionsOptions(List.of(newmsg)));
//    log.info("OpenAiChatClient response: {}", result.getChoices().getFirst().getMessage().getContent());
        log.info("OpenAiChatModel response: {}", insightService.answer(prompt));
    }

    //  @PostConstruct
    public void testTemplate() throws IOException {
        String template = """
        With the rising adoption of AI-assisted coding tools, enterprises face challenges such as files that AI tools cannot interpret, poorly structured legacy code, and security risks from AI-generated code lacking governance. ApolloScan addresses these issues by scanning repositories to detect files AI tools struggle with, highlighting AI adoption issues, and providing risk insights before onboarding AI-assisted coding tools.
        You are an AI assistant designed to analyse code for software quality metrics. Your task is to analyze and evaluate key metrics based on the source code of all java services. This is crucial for identifying code that is difficult to test and maintain expecially in legacy systems.
        
        Instruction:
        Return your detailed analysis , comments and recommendations as a JSON object in the following format:
        {{format}}
        """;
        PromptTemplate promptTemplate = PromptTemplate.from(template);
        String format = Files.readString(toPath("templates/dynamic_code_constructs.json"));
        Prompt p = promptTemplate.apply(Collections.singletonMap("format", format));
        log.info("OpenAiChatModel response: {}", insightService.answer(p.toString()));
    }
}