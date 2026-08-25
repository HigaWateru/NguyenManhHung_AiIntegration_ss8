package demo.crm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;

class DocumentIngestionServiceTest {

    @Test
    void ingestsMarkdownWithDynamicMetadata() {
        VectorStore vectorStore = mock(VectorStore.class);
        DocumentIngestionService service = new DocumentIngestionService(vectorStore);
        AtomicReference<List<Document>> storedDocumentsReference = new AtomicReference<>();
        doAnswer(invocation -> {
            storedDocumentsReference.set(invocation.getArgument(0));
            return null;
        }).when(vectorStore).add(anyList());

        int chunks = service.ingest(new ClassPathResource("test-document.md"), "returns", "test-document.md");

        assertEquals(1, chunks);
        List<Document> storedDocuments = storedDocumentsReference.get();
        assertFalse(storedDocuments.isEmpty());
        assertEquals("returns", storedDocuments.get(0).getMetadata().get("category"));
        assertEquals("test-document.md", storedDocuments.get(0).getMetadata().get("source_file"));
    }

    @Test
    void rejectsMissingResource() {
        VectorStore vectorStore = mock(VectorStore.class);
        DocumentIngestionService service = new DocumentIngestionService(vectorStore);

        assertThrows(IllegalArgumentException.class,
            () -> service.ingest(new ClassPathResource("missing.md"), "returns", "missing.md"));
    }
}
