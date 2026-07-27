# Spring AI + Vaadin: a streaming LLM chat feature in pure Java

A complete, runnable example of adding an AI assistant to a Java web application:
a real chat UI, token-by-token streaming, RAG over your own documents, and the
production concerns (token cost, cancellation, resilience) that demos usually skip.

**No JavaScript. No separate frontend project. One Spring Boot jar.**

Companion code for the article *Spring AI 2.0 in Practice: Adding LLM Features to a
Java Web App* — TODO: link post on publish.

<!-- TODO: add screenshot of the running chat view here -->

## Verified version matrix

| | Version |
|---|---|
| Spring AI | 2.0.0 |
| Spring Boot | 4.1.0 |
| Vaadin | 25.2.3 |
| Java | 21+ (21 release target, built on JDK 25) |

What was actually run against those versions, on 2026-07-27:

- `./mvnw test` — compiles clean (no warnings) and the ingestion test passes
- `./mvnw spring-boot:run` — starts in ~23 s, embeds the bundled docs, serves on :8080
- Browser check — the chat view renders, Markdown is applied, and submitting a question
  streams a server-pushed update back into the message list
- `./mvnw -Pproduction package` — builds the frontend bundle and an executable jar

Not verified: a **successful model response**, which needs a real `ANTHROPIC_API_KEY`.
Without a key the app still starts and the UI works; the request fails and the view shows
its error message (which is how the error path above was exercised).

## Quick start

```bash
export ANTHROPIC_API_KEY=sk-ant-...
./mvnw spring-boot:run
```

Then open <http://localhost:8080> and ask *"how do I configure SSO?"* — the answer comes
from the Markdown files in `src/main/resources/docs`, not from the model's training data.

First run is slow: Vaadin installs frontend tooling and Spring AI downloads a local
embedding model (~90 MB). Subsequent starts take seconds.

To run the tests (no API key needed, no network):

```bash
./mvnw test
```

## How it works

Five classes, each mapping to one step of the article:

| File | Role |
|---|---|
| `Application.java` | Boots Spring; `@Push` enables server→browser streaming |
| `AiConfig.java` | `ChatClient` bean, system prompt, in-memory vector store, RAG advisor |
| `AiChatService.java` | Streams tokens as a `Flux`, records token usage in Micrometer |
| `DocsIngestion.java` | Reads → splits → embeds the bundled docs at startup |
| `AssistantView.java` | The whole UI: `MessageList` + `MessageInput`, ~80 lines of Java |

Request flow:

```
Browser ⇄ Vaadin (server-side Java UI) → ChatClient (Spring AI) → Anthropic
                                              ↓
                                 VectorStore (retrieval for RAG)
```

## Two things that trip people up

**1. Anthropic has no embedding API.** RAG needs embeddings, so this project also
includes `spring-ai-starter-model-transformers` — a local ONNX model that runs
in-process with no extra API key. If you'd rather call a hosted embedding model, add
the OpenAI starter and configure it for embeddings only.

**2. `QuestionAnswerAdvisor` no longer exists.** Spring AI 2.0 removed it. RAG is now
`RetrievalAugmentationAdvisor` (in `spring-ai-rag`) composed with a
`VectorStoreDocumentRetriever` — see `AiConfig`. Most tutorials you'll find online
still show the 1.x API and will not compile against 2.0.

## Streaming: the parts that matter

Three details separate a demo from something you can ship:

1. **`@Push`** on the application shell. Without it the browser only updates on the
   next client round trip, so tokens arrive all at once at the end.
2. **`ui.access(...)`** around every UI mutation. Tokens arrive on a Reactor thread;
   `ui.access` re-enters the session lock safely and pushes the change.
3. **Dispose the subscription on detach.** `AiChatService.stream(...)` returns the
   `Disposable`, and `AssistantView.onDetach` disposes it. Skip this and a user who
   closes the tab mid-answer leaves you paying for tokens nobody reads.

## Token cost

The app subscribes to `chatResponse()` rather than `content()` so it can read usage
metadata off the final chunk, then counts it in Micrometer — including Anthropic's
cache-read/cache-write tokens, which are billed differently and matter a lot for RAG:

```bash
curl localhost:8080/actuator/metrics/ai.tokens
```

## Going to production

This project is deliberately minimal. Before shipping something like it:

- [ ] Swap `SimpleVectorStore` for **pgvector** — add
      `spring-ai-starter-vector-store-pgvector` and configure a datasource. Embeddings
      then live in Postgres next to your business data.
- [ ] Move ingestion out of startup into a scheduled job or webhook, and re-embed only
      what changed.
- [ ] Add a **circuit breaker and retry** (Resilience4j) around the model call.
- [ ] Enforce **per-user token budgets** and fail soft when they're hit.
- [ ] Put the view behind **Spring Security** — it's a business screen like any other.
- [ ] Treat ingested documents as untrusted input: **prompt injection** is real, and
      anything in the vector store ends up in the prompt.
- [ ] Don't log raw prompts if they can contain personal data; log counts and latencies.

## License

Apache License 2.0 — see [LICENSE](LICENSE). Sample documents describe a fictional
"Acme ERP" and are provided as RAG test fixtures only.
