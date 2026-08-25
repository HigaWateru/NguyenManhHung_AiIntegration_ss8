package demo.btth.dto.request;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter @Builder
public class DocumentRequest {
    private MultipartFile file;
}
