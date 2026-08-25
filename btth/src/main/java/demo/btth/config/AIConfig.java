package demo.btth.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
            .maxMessages(10)
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .build();
    }
    @Bean
    public ChatClient chatClient (ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder
            .defaultSystem("Bạn là chatbot hỗ trợ trả lời câu hỏi có trong database. Hãy cung cấp thông tin về ứng dụng và sản phẩm cùng thông tin liên quan. Tuyệt đối không được đưa ra câu trả lời sai lệch hoặc không có trong database. Nếu không biết câu trả lời, hãy trả lời 'Xin lỗi, tôi không biết câu trả lời cho câu hỏi này.'")
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory()).build())
            .build();
    }
}
