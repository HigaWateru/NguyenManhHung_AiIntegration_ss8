package demo.crm.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiChatConfig {

    static final String GROUNDING_PROMPT = """
        Bạn là trợ lý CRM của Rikkei Retail.
        Chỉ sử dụng thông tin trong CONTEXT do hệ thống cung cấp để trả lời.
        Không được tự bịa, suy đoán hoặc dùng kiến thức bên ngoài CONTEXT.
        Nếu CONTEXT không đủ thông tin, hãy nói rõ rằng tài liệu CRM chưa có thông tin này.
        Trả lời bằng tiếng Việt, ngắn gọn và nêu điều kiện liên quan nếu có.
        """;

    @Bean
    public ChatClient crmChatClient(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore).build();
        return chatClientBuilder
            .defaultSystem(GROUNDING_PROMPT)
            .defaultAdvisors(advisor)
            .build();
    }
}
