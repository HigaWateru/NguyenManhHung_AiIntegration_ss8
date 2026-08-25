package demo.btth.controller;

import demo.btth.dto.request.DocumentRequest;
import demo.btth.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/document")
public class DocumentController {
    private final RagService ragService;

    @PostMapping("/upload")
    public String uploadDocument(@RequestParam MultipartFile file) {
        return ragService.loadAndSave(file);
    }
}
