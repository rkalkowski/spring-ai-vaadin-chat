package com.example.springaichat;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Verifies the RAG ingestion pipeline — read, split, store — with no API key, no
 * embedding model, and no network. Fast enough to run on every commit.
 */
class DocsIngestionTest {

    @Test
    void ingestsBundledDocsIntoTheVectorStore() throws Exception {
        var vectorStore = mock(VectorStore.class);
        var docs = new PathMatchingResourcePatternResolver()
                .getResources("classpath:docs/*.md");

        assertThat(docs).as("sample docs should be on the classpath").isNotEmpty();

        new DocsIngestion()
                .ingestDocs(vectorStore, docs)
                .run(new DefaultApplicationArguments());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());

        List<Document> chunks = captor.getValue();
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(docs.length);
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.getText()).isNotBlank());
        assertThat(chunks)
                .as("SSO content should survive ingestion so RAG can retrieve it")
                .anySatisfy(chunk ->
                        assertThat(chunk.getText()).containsIgnoringCase("OpenID Connect"));
    }
}
