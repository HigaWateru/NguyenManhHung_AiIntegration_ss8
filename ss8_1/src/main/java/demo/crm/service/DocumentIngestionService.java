package demo.crm.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);
    private static final int CHUNK_SIZE = 600;
    private static final int MIN_CHUNK_SIZE_CHARS = 120;
    private static final int MAX_TOKENS = 10_000;

    private final VectorStore vectorStore;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Transactional
    public int ingest(Resource resource, String category, String sourceFile) {
        if (resource == null || !resource.exists() || !resource.isReadable()) {
            log.error("CRM document is missing or unreadable: {}", sourceFile);
            throw new IllegalArgumentException("Document does not exist or is not readable: " + sourceFile);
        }

        try {
            MarkdownDocumentReader reader = new MarkdownDocumentReader(resource.getURI().toString());
            List<Document> sourceDocuments = reader.get();
            List<Document> documentsWithMetadata = addMetadata(sourceDocuments, category, sourceFile);

            TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(CHUNK_SIZE)
                .withMinChunkSizeChars(MIN_CHUNK_SIZE_CHARS)
                .withMaxNumChunks(MAX_TOKENS)
                .build();
            List<Document> chunks = splitter.split(documentsWithMetadata);

            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("Markdown document contains no text: " + sourceFile);
            }
            vectorStore.add(chunks);
            log.info("CRM document ingested successfully: sourceFile={}, category={}, chunks={}",
                sourceFile, category, chunks.size());
            return chunks.size();
        } catch (IllegalArgumentException exception) {
            log.error("Invalid CRM document format or content: {}", sourceFile, exception);
            throw exception;
        } catch (Exception exception) {
            log.error("Could not ingest CRM document: {}", sourceFile, exception);
            throw new IllegalStateException("Could not ingest document: " + sourceFile, exception);
        }
    }

    private List<Document> addMetadata(List<Document> documents, String category, String sourceFile) {
        List<Document> result = new ArrayList<>(documents.size());
        for (Document document : documents) {
            Map<String, Object> metadata = new HashMap<>(document.getMetadata());
            metadata.put("category", category);
            metadata.put("source_file", sourceFile);
            result.add(new Document(document.getText(), metadata));
        }
        return result;
    }
}
