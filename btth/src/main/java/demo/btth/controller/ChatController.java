package demo.btth.controller;

import demo.btth.dto.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatController {
    private final ChatClient chatClient;
    private final PgVectorStore vectorStore;

    @PostMapping
    public String handleChat(@RequestBody ChatRequest request) {
        return chatClient.prompt().user(request.getMessage())
            .advisors(advisorSpec -> advisorSpec.param("chat_memory_conversation_id", request.getConversationId()))
            .advisors(QuestionAnswerAdvisor.builder(vectorStore).build()).call().content();
    }
}
