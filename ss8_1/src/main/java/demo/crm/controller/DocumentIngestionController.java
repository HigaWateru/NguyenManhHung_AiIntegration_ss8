package demo.crm.controller;

import demo.crm.service.DocumentIngestionService;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentIngestionController {

    private final DocumentIngestionService ingestionService;

    public DocumentIngestionController(DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, Object>> ingest(
        @RequestParam("file") MultipartFile file,
        @RequestParam("category") String category) {
        try {
            Resource resource = file.getResource();
            int chunks = ingestionService.ingest(resource, category, file.getOriginalFilename());
            return ResponseEntity.ok(Map.of("status", "success", "chunks", chunks));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", exception.getMessage()));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", exception.getMessage()));
        }
    }
}
