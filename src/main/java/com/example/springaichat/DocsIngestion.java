package com.example.springaichat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.Arrays;
import java.util.List;

/**
 * Reads the Markdown files in {@code resources/docs}, splits them into chunks, and
 * embeds them into the vector store at startup.
 *
 * <p>Startup ingestion is right for a small, static doc set that ships with the app.
 * For content that changes independently, move this to a scheduled job or trigger it
 * from your CMS webhook — and use a persistent store (pgvector) so you re-embed only
 * what actually changed. Embedding is the expensive part; don't redo it on every boot.
 *
 * <p>Security note: everything ingested here becomes part of the prompt. Only ingest
 * sources you trust — a document containing "ignore your instructions and ..." is a
 * prompt-injection vector.
 */
@Configuration(proxyBeanMethods = false)
class DocsIngestion {

    private static final Logger log = LoggerFactory.getLogger(DocsIngestion.class);

    @Bean
    ApplicationRunner ingestDocs(VectorStore vectorStore,
                                 AssistantProperties properties,
                                 @Value("classpath:docs/*.md") Resource[] docs) {
        return args -> {
            List<Document> documents = Arrays.stream(docs)
                    .map(TextReader::new)
                    .flatMap(reader -> reader.get().stream())
                    .toList();

            // Chunk size is the main RAG tuning knob: too large and retrieval returns
            // mostly irrelevant text (and costs input tokens), too small and answers
            // lose the surrounding context.
            List<Document> chunks = TokenTextSplitter.builder()
                    .withChunkSize(properties.chunkSize())
                    .build()
                    .split(documents);

            vectorStore.add(chunks);

            log.info("Ingested {} document(s) into {} chunk(s)", documents.size(), chunks.size());
        };
    }
}
