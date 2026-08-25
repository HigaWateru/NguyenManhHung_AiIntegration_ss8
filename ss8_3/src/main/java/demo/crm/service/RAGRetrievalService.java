package demo.crm.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RAGRetrievalService {

    public static final String OUT_OF_SCOPE_MESSAGE =
        "Xin lỗi, thông tin bạn tìm kiếm không nằm trong tài liệu quy chế của chúng tôi.";

    private static final int TOP_K = 3;
    private static final Logger log = LoggerFactory.getLogger(RAGRetrievalService.class);

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final double similarityThreshold;

    public RAGRetrievalService(
        VectorStore vectorStore,
        ChatClient chatClient,
        @Value("${rag.similarity-threshold:0.75}") double similarityThreshold) {
        if (similarityThreshold < 0 || similarityThreshold > 1) {
            throw new IllegalArgumentException("rag.similarity-threshold must be between 0 and 1");
        }
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.similarityThreshold = similarityThreshold;
    }

    public String answer(String question) {
        if (question == null || question.isBlank()) {
            log.warn("Rejected blank RAG question without calling the LLM");
            return OUT_OF_SCOPE_MESSAGE;
        }

        SearchRequest request = SearchRequest.builder()
            .query(question)
            .topK(TOP_K)
            .similarityThreshold(similarityThreshold)
            .build();
        List<Document> candidates = vectorStore.similaritySearch(request);
        List<Document> relevantDocuments = candidates == null ? List.of() : candidates.stream()
            .filter(Objects::nonNull)
            .filter(document -> document.getScore() != null && document.getScore() > similarityThreshold)
            .limit(TOP_K)
            .toList();

        if (relevantDocuments.isEmpty()) {
            log.warn("RAG blocked out-of-scope question: threshold={}, candidates={}",
                similarityThreshold, candidates == null ? 0 : candidates.size());
            return OUT_OF_SCOPE_MESSAGE;
        }

        String context = relevantDocuments.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n\n---\n\n"));
        log.info("RAG accepted question with {} relevant documents", relevantDocuments.size());
        return chatClient.prompt()
            .system("Bạn là trợ lý CRM. Chỉ trả lời dựa trên CONTEXT; nếu không đủ thông tin, hãy nói không biết.")
            .user("CONTEXT:\n" + context + "\n\nQUESTION:\n" + question)
            .call()
            .content();
    }
}
