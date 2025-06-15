package dev.iamandicip.chatgpt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
class ChatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatClient.Builder chatClientBuilder;

    @MockitoBean
    private ChatClient chatClient;

    @MockitoBean
    private ChatMemory chatMemory;

    @MockitoBean
    private ChatClient.ChatClientRequestSpec chatClientRequestSpec;

    @MockitoBean
    private ChatClient.CallResponseSpec callResponseSpec;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.defaultAdvisors(any(MessageChatMemoryAdvisor.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        
        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.user(any(String.class))).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Mock AI response for testing");
    }

    @Test
    void testChatPageLoads() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andReturn();
        
        String content = result.getResponse().getContentAsString();
        
        // Verify the page contains our chat form structure
        assertTrue(content.contains("response-container"), "Page should contain response container");
        assertTrue(content.contains("chatForm"), "Page should contain chat form");
        assertTrue(content.contains("x-data=\"chatForm()\""), "Page should contain Alpine.js chat component");
        assertTrue(content.contains("hx-post=\"/api/chat\""), "Page should contain HTMX post to chat API");
    }

    @Test
    void testChatApiResponseStructure() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/chat")
                .param("message", "Hello integration test")
                .param("thinkingId", "thinking-123")
                .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andReturn();
        
        String responseContent = result.getResponse().getContentAsString();
        
        // Verify response contains only the AI response fragment (not user message)
        assertTrue(responseContent.contains("<p"), "Response should contain paragraph element");
        assertTrue(responseContent.contains("Mock AI response for testing"), "Response should contain AI response text");
        assertTrue(responseContent.contains("class=\"mt-4 h-full overflow-auto\""), "Response should have correct CSS classes");
        
        // Verify response does NOT contain user message structure
        assertFalse(responseContent.contains("bg-slate-100"), "Response should NOT contain user message styling");
        assertFalse(responseContent.contains("Hello integration test"), "Response should NOT contain user message text");
        
        // Verify it's a fragment, not a full page
        assertFalse(responseContent.contains("<!doctype html>"), "Response should be fragment, not full page");
        assertFalse(responseContent.contains("<html"), "Response should be fragment, not full page");
    }

    @Test
    void testChatApiWithEmptyMessage() throws Exception {
        // Test what happens when message is empty
        when(callResponseSpec.content()).thenReturn("Please provide a message.");
        
        MvcResult result = mockMvc.perform(post("/api/chat")
                .param("message", "")
                .param("thinkingId", "thinking-456")
                .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andReturn();
        
        String responseContent = result.getResponse().getContentAsString();
        assertTrue(responseContent.contains("Please provide a message."), "Should handle empty message gracefully");
    }

    @Test
    void testMultipleChatMessages() throws Exception {
        // First message
        when(callResponseSpec.content()).thenReturn("First AI response");
        
        MvcResult result1 = mockMvc.perform(post("/api/chat")
                .param("message", "First message")
                .param("thinkingId", "thinking-111")
                .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andReturn();
        
        String response1 = result1.getResponse().getContentAsString();
        assertTrue(response1.contains("First AI response"));
        
        // Second message
        when(callResponseSpec.content()).thenReturn("Second AI response");
        
        MvcResult result2 = mockMvc.perform(post("/api/chat")
                .param("message", "Second message")
                .param("thinkingId", "thinking-222")
                .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andReturn();
        
        String response2 = result2.getResponse().getContentAsString();
        assertTrue(response2.contains("Second AI response"));
        
        // Verify responses are independent fragments
        assertNotEquals(response1, response2, "Each response should be unique");
    }

    @Test
    void testResponseFragmentStructure() throws Exception {
        when(callResponseSpec.content()).thenReturn("Test response with special chars: <>&\"'");
        
        MvcResult result = mockMvc.perform(post("/api/chat")
                .param("message", "Test special characters")
                .param("thinkingId", "thinking-789")
                .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andReturn();
        
        String responseContent = result.getResponse().getContentAsString();
        
        // Verify the exact structure expected by HTMX
        assertTrue(responseContent.startsWith("<p"), "Response should start with paragraph tag");
        assertTrue(responseContent.endsWith("</p>"), "Response should end with paragraph tag");
        assertTrue(responseContent.contains("class=\"mt-4 h-full overflow-auto\""), "Should have expected CSS classes");
        
        // Verify content is properly escaped/handled
        assertTrue(responseContent.contains("Test response with special chars: <>&\"'"), "Should handle special characters");
    }

    @Test
    void testHTMXHeaders() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/chat")
                .param("message", "Test HTMX headers")
                .param("thinkingId", "thinking-999")
                .header("HX-Request", "true")
                .header("HX-Target", "thinking-123456"))
                .andExpect(status().isOk())
                .andReturn();
        
        // Verify the response is appropriate for HTMX
        String contentType = result.getResponse().getContentType();
        assertTrue(contentType.contains("text/html"), "Content type should be HTML for HTMX");
    }
}