package demo.crm.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class ChunkingConfigTest {

    @Test
    void registersBothSplittersInSpringContext() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ChunkingConfig.class)) {
            assertEquals(2, context.getBeansOfType(TextSplitter.class).size());
            assertTrue(context.containsBean("tokenBasedTextSplitter"));
            assertTrue(context.containsBean("headerBasedTextSplitter"));
        }
    }

    @Test
    void headerSplitterKeepsMarkdownHeadersWithTheirContent() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ChunkingConfig.class)) {
            TextSplitter splitter = context.getBean("headerBasedTextSplitter", TextSplitter.class);
            List<Document> chunks = splitter.split(new Document(
                "# Chương I\n\nNội dung chương mô tả phạm vi áp dụng, trách nhiệm và quy trình xử lý hồ sơ khách hàng trong toàn bộ hệ thống CRM.\n\n"
                    + "## Điều 1\n\nQuy định khách hàng yêu cầu xác thực mã định danh, lưu lại lịch sử giao dịch và thông báo kết quả xử lý bằng kênh đã đăng ký."));

            assertEquals(2, chunks.size());
            assertTrue(chunks.get(0).getText().startsWith("# Chương I"));
            assertTrue(chunks.get(1).getText().startsWith("## Điều 1"));
        }
    }
}
