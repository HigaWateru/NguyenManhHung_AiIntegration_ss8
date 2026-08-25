package demo.crm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import demo.crm.dto.ChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;

class CrmChatControllerTest {

    @Test
    void returnsServiceUnavailableWhenLlmOrVectorStoreFails() {
        ChatClient chatClient = mock(ChatClient.class);
        when(chatClient.prompt()).thenThrow(new RuntimeException("connection refused"));
        CrmChatController controller = new CrmChatController(chatClient);

        var response = controller.chat(new ChatRequest("Chính sách đổi trả là gì?"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Dịch vụ trợ lý CRM tạm thời không khả dụng. Vui lòng thử lại sau.",
            response.getBody().answer());
    }
}
