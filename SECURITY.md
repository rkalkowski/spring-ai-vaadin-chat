# Security

This is an example project intended for local experimentation. It is **not**
hardened for production or internet exposure — see the "Going to production"
checklist in [README.md](README.md) before adapting it.

## Reporting a vulnerability

Open a [private security advisory](https://github.com/rkalkowski/spring-ai-vaadin-chat/security/advisories/new)
rather than a public issue.

## Handling your API key

The application reads `ANTHROPIC_API_KEY` from the environment and never writes
it to disk or logs. When working on this project:

- **Do not** put the key in `application.properties`, a `.env` file, or a commit.
  `.gitignore` covers `.env`, but the reliable protection is not typing it there.
- Prefer a secret manager or your OS keychain over a plaintext shell profile.
- If a key is ever exposed — pasted into a chat, a screenshot, a CI log — revoke
  it in the Anthropic Console and issue a new one. Rotation is cheap; assuming
  an exposed key is still private is not.

## Things this example deliberately does not do

- **No authentication.** The assistant view is open to anyone who can reach the
  port. Put it behind Spring Security before exposing it.
- **No per-user rate limiting or token budget.** A single user can spend as much
  as the model will produce.
- **No egress restrictions on ingested content.** Everything in
  `src/main/resources/docs` is placed into the prompt verbatim; treat ingested
  documents as untrusted input (see prompt injection in the README).
