# Contributing

This repository is the companion code for a blog article, so its primary job is
to stay **small, readable, and runnable**. Contributions are welcome within that
constraint.

## Good contributions

- Fixes for anything that doesn't build or run on a clean checkout.
- Corrections when a library API changes (Spring AI and Vaadin both move quickly).
- Clarifications to comments or the README where something was hard to follow.

## Please avoid

- New features or extra abstraction layers. Every class here maps to one step of
  the article; adding indirection makes the article wrong.
- Reformatting unrelated code, or dependency bumps unrelated to a fix.

## Before opening a pull request

```bash
./mvnw verify
```

The test suite is intentionally offline — it needs no API key and makes no network
calls, so it runs anywhere. If you change behaviour that only shows up against the
live API, say so in the PR description and describe how you verified it, because
CI cannot.

Keep the existing style: 4-space indent, no wildcard imports, comments that explain
*why* rather than restating the code. `.editorconfig` covers the mechanical parts.
