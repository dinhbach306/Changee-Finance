package org.dinhb.microservice.core.ai.service;

import org.dinhb.microservice.core.ai.tools.AccountInfoTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public AiChatService(ChatClient chatClient, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    public String chat(UUID userId, String message) {
        return chatClient.prompt()
                .user(message)
                .toolContext(Map.of(AccountInfoTool.USER_ID_KEY, userId.toString()))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId.toString()))
                .call()
                .content();
    }

    public Flux<String> stream(UUID userId, String message) {
        return chatClient.prompt()
                .user(message)
                .toolContext(Map.of(AccountInfoTool.USER_ID_KEY, userId.toString()))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId.toString()))
                .stream()
                .content();
    }

    public List<Message> history(UUID userId) {
        return chatMemory.get(userId.toString());
    }

    public void clear(UUID userId) {
        chatMemory.clear(userId.toString());
    }
}
