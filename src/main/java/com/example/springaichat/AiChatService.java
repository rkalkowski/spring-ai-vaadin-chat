package com.example.springaichat;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

import java.util.function.Consumer;

/**
 * Wraps the {@link ChatClient} with the two things a UI actually needs: a cancellable
 * token stream, and token-usage accounting.
 */
@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    /** Shown to the user when the model call fails. Markdown — the message list renders it. */
    static final String ERROR_MESSAGE = "\n\n*Something went wrong — please try again.*";

    private static final String TOKENS_METRIC = "ai.tokens";

    private final ChatClient chatClient;
    private final MeterRegistry meterRegistry;
    private final AssistantProperties properties;

    AiChatService(ChatClient chatClient, MeterRegistry meterRegistry, AssistantProperties properties) {
        this.chatClient = chatClient;
        this.meterRegistry = meterRegistry;
        this.properties = properties;
    }

    /**
     * Streams an answer chunk by chunk.
     *
     * <p>Returns the {@link Disposable} so the caller can cancel the stream — the UI
     * disposes it on detach. Without that, a user who closes the tab mid-answer leaves
     * the request running and you keep paying for tokens nobody will read.
     *
     * <p>Subscribes to {@code chatResponse()} rather than {@code content()} because the
     * final chunk carries the token-usage metadata needed for cost metrics.
     *
     * @param onToken    receives each non-empty text chunk, in order
     * @param onComplete runs once when the stream finishes, successfully or not
     * @return handle for cancelling an in-flight completion
     */
    public Disposable stream(String question, Consumer<String> onToken, Runnable onComplete) {
        return chatClient.prompt()
                .user(question)
                .stream()
                .chatResponse()
                .timeout(properties.requestTimeout())
                .doOnNext(this::recordUsage)
                .map(AiChatService::textOf)
                .filter(text -> !text.isEmpty())
                .subscribe(
                        onToken,
                        error -> {
                            // Deliberately not surfacing the exception to the user: it can
                            // carry request IDs and provider internals. Log it, show a
                            // neutral message, and let the UI re-enable its input.
                            log.warn("LLM stream failed", error);
                            onToken.accept(ERROR_MESSAGE);
                            onComplete.run();
                        },
                        onComplete);
    }

    /** Null-safe text extraction: intermediate chunks can carry no result at all. */
    static String textOf(ChatResponse response) {
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
        meterRegistry.counter(TOKENS_METRIC, "type", type, "feature", properties.featureName())
                .increment(tokens.doubleValue());
    }
}
