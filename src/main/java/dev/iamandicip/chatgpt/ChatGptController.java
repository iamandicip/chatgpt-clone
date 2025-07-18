package dev.iamandicip.chatgpt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.FragmentsRendering;

@Controller
@CrossOrigin
@Slf4j
public class ChatGptController {

    private final ChatClient chatClient;

    public ChatGptController(ChatClient.Builder builder, ChatMemory chatMemory) {
        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .build())
                .build();
    }

    @GetMapping("")
    public String home() {
        return "index";
    }

    @PostMapping("/api/chat")
    public View generate(@RequestParam String message, @RequestParam String thinkingId, Model model) {
        log.debug("User message: {}", message);

        String response = chatClient.prompt()
                .user(message)
                .call()
                .content();

        model.addAttribute("response", response);
        model.addAttribute("message", message);
        model.addAttribute("thinkingId", thinkingId);

        return FragmentsRendering
                .with("response :: responseFragment")
                .fragment("recent-message-list :: messageFragment")
                .build();
    }
}
