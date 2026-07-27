/**
 * A documentation assistant: Spring AI for the model calls, Vaadin for the UI, both in
 * the same Spring Boot application.
 *
 * <p>Five classes, each mapping to one step of the companion article:
 *
 * <ul>
 *   <li>{@link com.example.springaichat.Application} — boots Spring; {@code @Push} is what
 *       makes server-to-browser streaming possible
 *   <li>{@link com.example.springaichat.AiConfig} — the {@code ChatClient}, the system
 *       prompt, the vector store, and the RAG advisor
 *   <li>{@link com.example.springaichat.DocsIngestion} — reads, splits and embeds the
 *       bundled Markdown at startup
 *   <li>{@link com.example.springaichat.AiChatService} — streams chunks as a cancellable
 *       {@code Flux} and records token usage
 *   <li>{@link com.example.springaichat.AssistantView} — the whole UI, in Java
 * </ul>
 *
 * <p>Everything is package-private except {@code Application}, {@code AiChatService} and
 * {@code AssistantView}: the configuration classes are wiring, not API.
 */
package com.example.springaichat;
