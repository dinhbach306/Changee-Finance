package org.dinhb.microservice.core.ai.config;

import org.dinhb.microservice.core.ai.tools.AccountInfoTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiClientConfig {

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are Changee Finance Assistant, a helpful AI for personal finance.
            You answer questions about the user's accounts and transactions, and explain finance concepts clearly.
            Always answer concisely.
            When the user asks about their accounts or balance, call getMyAccounts — it already knows the caller's identity.
            Never ask the user for their userId; it is injected automatically.
            """;

    @Bean
    ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory, AccountInfoTool accountInfoTool) {
        return builder
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(accountInfoTool)
                .build();
    }
}
