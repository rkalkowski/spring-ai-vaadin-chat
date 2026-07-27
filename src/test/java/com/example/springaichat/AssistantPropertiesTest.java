package com.example.springaichat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the {@code assistant.*} binding and, more importantly, that a bad value stops
 * the application at startup. A misconfigured chunk size or timeout that only surfaces on
 * a user's first question is far more expensive to diagnose than a failed boot.
 *
 * <p>Uses {@link ApplicationContextRunner} rather than {@code @SpringBootTest}: this loads
 * only the properties binding, so the test stays offline and takes milliseconds instead of
 * downloading an embedding model.
 */
class AssistantPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void bindsTheDocumentedDefaults() {
        runner.withPropertyValues(
                        "assistant.request-timeout=60s",
                        "assistant.chunk-size=400",
                        "assistant.top-k=4",
                        "assistant.feature-name=docs-assistant")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var properties = context.getBean(AssistantProperties.class);
                    assertThat(properties.requestTimeout()).isEqualTo(Duration.ofSeconds(60));
                    assertThat(properties.chunkSize()).isEqualTo(400);
                    assertThat(properties.topK()).isEqualTo(4);
                    assertThat(properties.featureName()).isEqualTo("docs-assistant");
                });
    }

    @Test
    void rejectsANonPositiveTimeout() {
        runner.withPropertyValues(
                        "assistant.request-timeout=0s",
                        "assistant.chunk-size=400",
                        "assistant.top-k=4",
                        "assistant.feature-name=docs-assistant")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsATopKBelowOne() {
        runner.withPropertyValues(
                        "assistant.request-timeout=60s",
                        "assistant.chunk-size=400",
                        "assistant.top-k=0",
                        "assistant.feature-name=docs-assistant")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsABlankFeatureName() {
        runner.withPropertyValues(
                        "assistant.request-timeout=60s",
                        "assistant.chunk-size=400",
                        "assistant.top-k=4",
                        "assistant.feature-name=")
                .run(context -> assertThat(context).hasFailed());
    }

    @EnableConfigurationProperties(AssistantProperties.class)
    static class TestConfig {
    }
}
