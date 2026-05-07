package com.example.aiproject.ai;

import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnBooleanProperty("cli-chat.enabled")
public class CLIChat {
    @Autowired
    AIScanInsightsService aiScanInsightsService;

    @PostConstruct
    public void startConversation() {
        new Thread(() -> {
            log.info("===============================Starting CLI chat conversation==============================");
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    log.info("==================================================");
                    log.info("User: ");
                    String userQuery = scanner.nextLine();
                    log.info("==================================================");

                    if ("e".equalsIgnoreCase(userQuery) || "exit".equalsIgnoreCase(userQuery)) {
                        break;
                    }

                    String agentAnswer = aiScanInsightsService.answer(userQuery);
                    log.info("==================================================");
                    log.info("Assistant: " + agentAnswer);
                }
            }
        }).start();
    }
}