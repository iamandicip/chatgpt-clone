package dev.iamandicip.chatgpt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.FragmentsRendering;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class ChatGptControllerTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatMemory chatMemory;

    @Mock
    private Model model;

    @Mock
    private ChatClient.ChatClientRequestSpec chatClientRequestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private ChatGptController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.defaultAdvisors(any(Advisor.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        controller = new ChatGptController(chatClientBuilder, chatMemory);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testHome() {
        String result = controller.home();
        assertEquals("index", result);
    }

    @Test
    void testHomeEndpoint() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void testGenerate() {
        String testMessage = "Hello, world!";
        String mockResponse = "Hello! How can I help you today?";

        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.user(testMessage)).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(mockResponse);

        View result = controller.generate(testMessage, "thinking-123", model);

        assertNotNull(result);
        verify(model).addAttribute("response", mockResponse);
        verify(model).addAttribute("message", testMessage);
        verify(chatClient).prompt();
        verify(chatClientRequestSpec).user(testMessage);
        verify(chatClientRequestSpec).call();
        verify(callResponseSpec).content();
    }

    @Test
    void testGenerateWithEmptyMessage() {
        String testMessage = "";
        String mockResponse = "I didn't receive a message. Could you please try again?";

        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.user(testMessage)).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(mockResponse);

        View result = controller.generate(testMessage, "thinking-123", model);

        assertNotNull(result);
        verify(model).addAttribute("response", mockResponse);
        verify(model).addAttribute("message", testMessage);
    }

    @Test
    void testGenerateEndpoint() throws Exception {
        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Mock response");

        mockMvc.perform(post("/api/chat")
                        .param("message", "Test message")
                        .param("thinkingId", "thinking-test")
                        .header("HX-Request", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void testConstructor() {
        assertNotNull(controller);
        verify(chatClientBuilder).defaultAdvisors(any(Advisor.class));
        verify(chatClientBuilder).build();
    }

    @Test
    void testConstructorWithNullBuilder() {
        assertThrows(NullPointerException.class, () -> {
            new ChatGptController(null, chatMemory);
        });
    }

    @Test
    void testConstructorWithNullChatMemory() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ChatGptController(chatClientBuilder, null);
        });
    }
    
    @Test
    void testGenerateReturnsOnlyResponseFragment() {
        String testMessage = "Hello, world!";
        String mockResponse = "Hello! How can I help you today?";

        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.user(testMessage)).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(mockResponse);

        View result = controller.generate(testMessage, "thinking-123", model);

        assertNotNull(result);
        assertTrue(result instanceof FragmentsRendering, "Result should be FragmentsRendering");
        
        FragmentsRendering fragmentsRendering = (FragmentsRendering) result;
        // Verify both response and message fragments are included
        String viewString = fragmentsRendering.toString();
        assertTrue(viewString.contains("response :: responseFragment"), 
                  "Should contain response fragment");
        assertTrue(viewString.contains("recent-message-list :: messageFragment"), 
                   "Should contain message fragment for sidebar");
    }
}