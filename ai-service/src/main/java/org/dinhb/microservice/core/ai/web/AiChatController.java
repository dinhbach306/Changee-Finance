package org.dinhb.microservice.core.ai.web;

import org.dinhb.microservice.core.ai.service.AiChatService;
import org.dinhb.microservice.core.common.security.HeaderNames;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
public class AiChatController {

    private final AiChatService chatService;

    public AiChatController(AiChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@RequestHeader(HeaderNames.X_USER_ID) UUID userId,
                             @RequestBody ChatRequest request) {
        String content = chatService.chat(userId, request.message());
        return new ChatResponse(content);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestHeader(HeaderNames.X_USER_ID) UUID userId,
                               @RequestBody ChatRequest request) {
        return chatService.stream(userId, request.message());
    }

    @GetMapping("/history")
    public List<HistoryEntry> history(@RequestHeader(HeaderNames.X_USER_ID) UUID userId) {
        return chatService.history(userId).stream()
                .map(HistoryEntry::from)
                .toList();
    }

    @DeleteMapping("/history")
    public void clear(@RequestHeader(HeaderNames.X_USER_ID) UUID userId) {
        chatService.clear(userId);
    }

    public record ChatRequest(String message) {}

    public record ChatResponse(String message) {}

    public record HistoryEntry(String role, String content) {
        static HistoryEntry from(Message m) {
            return new HistoryEntry(m.getMessageType().getValue(), m.getText());
        }
    }
}
