package demo.crm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

class RAGRetrievalServiceTest {

    @Test
    void blocksLowSimilarityQuestionWithoutCallingLlm() {
        VectorStore vectorStore = mock(VectorStore.class);
        ChatClient chatClient = mock(ChatClient.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
            scoredDocument("CRM đổi trả trong 30 ngày.", 0.74),
            scoredDocument("CRM hoàn tiền theo điều kiện.", 0.60)));
        RAGRetrievalService service = new RAGRetrievalService(vectorStore, chatClient, 0.75);

        String answer = service.answer("Làm thế nào để học Java?");

        assertEquals(RAGRetrievalService.OUT_OF_SCOPE_MESSAGE, answer);
        verifyNoInteractions(chatClient);
        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        assertEquals(3, requestCaptor.getValue().getTopK());
        assertEquals(0.75, requestCaptor.getValue().getSimilarityThreshold());
    }

    @Test
    void blankQuestionIsRejectedBeforeVectorSearch() {
        VectorStore vectorStore = mock(VectorStore.class);
        ChatClient chatClient = mock(ChatClient.class);
        RAGRetrievalService service = new RAGRetrievalService(vectorStore, chatClient, 0.75);

        assertEquals(RAGRetrievalService.OUT_OF_SCOPE_MESSAGE, service.answer("  "));
        verifyNoInteractions(vectorStore, chatClient);
    }

    private static Document scoredDocument(String text, double score) {
        return Document.builder().text(text).score(score).build();
    }
}
