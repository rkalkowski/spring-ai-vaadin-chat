package com.example.springaichat;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

import java.time.Duration;
import java.util.function.Consumer;

@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final ChatClient chatClient;
    private final MeterRegistry meterRegistry;

    AiChatService(ChatClient chatClient, MeterRegistry meterRegistry) {
        this.chatClient = chatClient;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Streams an answer token by token.
     *
     * <p>Returns the {@link Disposable} so the caller can cancel the stream — the UI
     * disposes it on detach. Without that, a user who closes the tab mid-answer leaves
     * the request running and you keep paying for tokens nobody will read.
     *
     * <p>We subscribe to {@code chatResponse()} rather than {@code content()} because the
     * final chunk carries the token-usage metadata we want for cost metrics.
     */
    public Disposable stream(String question, Consumer<String> onToken, Runnable onComplete) {
        return chatClient.prompt()
                .user(question)
                .stream()
                .chatResponse()
                .timeout(TIMEOUT)
                .doOnNext(this::recordUsage)
                .map(AiChatService::textOf)
                .filter(text -> !text.isEmpty())
                .subscribe(
                        onToken,
                        error -> {
                            log.warn("LLM stream failed", error);
                            onToken.accept("\n\n*Something went wrong — please try again.*");
                            onComplete.run();
                        },
                        onComplete);
    }

    private static String textOf(ChatResponse response) {
        var result = response.getResult();
        if (result == null || result.getOutput() == null) {
            return "";
        }
        var text = result.getOutput().getText();
        return text == null ? "" : text;
    }

    /**
     * Token usage is the unit AI features are billed in, so treat it like any other
     * metric: count it from day one, tagged per feature, and put it on a dashboard
     * before the first invoice arrives.
     */
    private void recordUsage(ChatResponse response) {
        Usage usage = response.getMetadata().getUsage();
        if (usage == null) {
            return;
        }
        count("input", usage.getPromptTokens());
        count("output", usage.getCompletionTokens());
        // Anthropic prompt caching: cache reads are billed at a fraction of input tokens,
        // so tracking them separately is what makes a RAG cost model believable.
        count("cache_read", usage.getCacheReadInputTokens());
        count("cache_write", usage.getCacheWriteInputTokens());
    }

    private void count(String type, Number tokens) {
        if (tokens == null || tokens.longValue() <= 0) {
            return;
        }
        meterRegistry.counter("ai.tokens", "type", type, "feature", "docs-assistant")
                .increment(tokens.doubleValue());
    }
}
