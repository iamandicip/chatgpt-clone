# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a ChatGPT clone built with Spring Boot 3.5.0, Spring AI, and HTMX. The application uses Mistral AI as the backend LLM provider and implements a chat interface with memory persistence.

### Architecture

- **Backend**: Spring Boot web application with a single controller (`ChatGptController`)
- **AI Integration**: Spring AI with Mistral AI client and chat memory advisors
- **Frontend**: HTMX + Alpine.js + Tailwind CSS for reactive UI without heavy JavaScript
- **Templates**: Thymeleaf templates with fragment-based rendering for dynamic content updates
- **Chat Memory**: Conversation history is maintained across requests using Spring AI's chat memory system

The application follows a fragment-based architecture where HTMX requests return Thymeleaf fragments that update specific parts of the UI:
- `response.html` contains the chat response fragment
- `recent-message-list.html` contains the sidebar message list fragment
- Both fragments are returned simultaneously via `FragmentsRendering`

## Development Commands

### Build and Run
```bash
./mvnw spring-boot:run
```

### Test
```bash
./mvnw test
```

### Build JAR
```bash
./mvnw clean package
```

### Run specific test
```bash
./mvnw test -Dtest=ChatGptControllerTest
```

## Environment Setup

Required environment variable:
- `MISTRAL_API_KEY`: API key for Mistral AI service

## Key Implementation Details

- Chat responses use `FragmentsRendering` to return multiple Thymeleaf fragments in a single response
- Alpine.js handles Enter key submission with shift-key detection for multi-line input
- HTMX handles form submission and DOM updates with `hx-swap="beforeend"` for chat history
- Chat memory is managed automatically through Spring AI's `MessageChatMemoryAdvisor`
- Uses Java 21 features and Lombok for cleaner code