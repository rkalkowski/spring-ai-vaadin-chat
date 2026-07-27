package com.example.springaichat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the null-handling in text extraction. Intermediate streaming chunks routinely
 * arrive with no result or no output, and an unguarded {@code getText()} there would
 * throw inside the Reactor pipeline — surfacing to the user as the generic error message
 * while the request had actually succeeded.
 */
class AiChatServiceTest {

    @Test
    void extractsTextFromAChunk() {
        var response = new ChatResponse(List.of(new Generation(new AssistantMessage("Hello"))));

        assertThat(AiChatService.textOf(response)).isEqualTo("Hello");
    }

    @Test
    void returnsEmptyStringWhenTheChunkCarriesNoResult() {
        var response = new ChatResponse(List.of());

        assertThat(AiChatService.textOf(response)).isEmpty();
    }

    @Test
    void returnsEmptyStringWhenTheChunkCarriesNoText() {
        var response = mock(ChatResponse.class);
        var generation = mock(Generation.class);
        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(new AssistantMessage(null));

        assertThat(AiChatService.textOf(response)).isEmpty();
    }

    @Test
    void errorMessageIsMarkdownSoTheMessageListRendersIt() {
        assertThat(AiChatService.ERROR_MESSAGE).startsWith("\n\n*").endsWith("*");
    }
}
