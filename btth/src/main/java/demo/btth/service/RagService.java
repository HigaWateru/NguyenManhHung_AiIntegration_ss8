package demo.btth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {
    private final VectorStore vectorStore;

    public String loadAndSave(MultipartFile file) {
        try {
            Resource resource = file.getResource();
            TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);

            List<Document> rawDocuments = tikaDocumentReader.get();
            TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder().build();
            List<Document> documents = tokenTextSplitter.split(rawDocuments);

            vectorStore.add(documents);
            return "File uploaded and processed successfully.";
        } catch (Exception e) {
            log.error("Error processing file: {}", e.getMessage(), e);
            return "Error processing file: " + e.getMessage();
        }
    }

    public String searchDocument(String keyword) {
        SearchRequest searchRequest = SearchRequest.builder().query(keyword).topK(3).build();
        return vectorStore.similaritySearch(searchRequest).stream().map(Document::getText)
            .collect(Collectors.joining("\n"));
    }
}
