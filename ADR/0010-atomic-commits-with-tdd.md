# 10. Atomic commits with TDD

Date: 2026-03-22

## Status

Accepted

## Context

Earlier iterations were developed in long sessions and committed in bulk — sometimes an entire iteration (multiple classes, tests, and docs) as a single commit. This makes it hard to review individual changes, bisect regressions, or understand why a specific line was written.

The project already follows a strict TDD red/green loop. Each cycle — write a failing test, make it pass, optionally refactor — is a natural unit of work with a clear before/after state. Aligning commits with these cycles makes the test suite and the commit history tell the same story.

## Decision

Each commit must be **atomic**: it represents one complete, self-consistent change that leaves the codebase in a working state. In practice this means:

- **Red → Green** — a failing test plus the minimum implementation to make it pass is one commit.
- **Refactor** — a behaviour-preserving cleanup after a green test is its own commit if it touches more than a trivial rename.
- **Docs / config** — changelog, ADR, README, or config updates that accompany a code change may be bundled into the same commit as that change, or follow immediately as a separate commit. They must not precede the code change.

A commit must not mix unrelated concerns (e.g. a bug fix and a new feature, or two independent test cases). If `git diff` covers more than one logical idea, split it.

The TDD rule is unchanged: no implementation code exists in the repository without a failing test having been committed first (or in the same commit).

## Consequences

- The commit log becomes a readable history of decisions, not a dump of session output.
- Regressions can be bisected to a single red→green cycle, which immediately identifies both the breaking change and the test that should have caught it.
- Pull request reviews are easier: each commit is reviewable in isolation.
- Committing more frequently creates a lightweight checkpoint habit — partial work is visible earlier.
- Amending or squashing commits before they are shared is acceptable; rewriting shared history is not.
