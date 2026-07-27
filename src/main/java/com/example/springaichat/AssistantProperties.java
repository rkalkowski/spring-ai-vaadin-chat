package com.example.springaichat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Tunables for the docs assistant, bound from the {@code assistant.*} prefix.
 *
 * <p>These four values are the ones you actually end up changing — a timeout when the
 * model gets slower, chunk size and retrieval depth when answer quality drifts, the
 * metric tag when a second AI feature ships and the dashboards need to tell them apart.
 * Keeping them here rather than inline means they can be tuned per environment without
 * a recompile, and the validation annotations make a bad value fail at startup rather
 * than halfway through a user's first question.
 *
 * @param requestTimeout how long to wait for a completion before giving up
 * @param chunkSize      token target when splitting ingested documents
 * @param topK           how many chunks to retrieve and put in the prompt
 * @param featureName    value of the {@code feature} tag on the {@code ai.tokens} metric
 */
@Validated
@ConfigurationProperties(prefix = "assistant")
record AssistantProperties(
        Duration requestTimeout,
        @Min(50) int chunkSize,
        @Min(1) int topK,
        @NotBlank String featureName) {

    AssistantProperties {
        if (requestTimeout == null || requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException(
                    "assistant.request-timeout must be a positive duration, was: " + requestTimeout);
        }
    }
}
