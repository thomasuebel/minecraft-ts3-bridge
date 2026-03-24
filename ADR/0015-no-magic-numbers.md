# 15. No magic numbers

Date: 2026-03-24

## Status

Accepted

## Context

During SSH support work, numeric literals appeared directly in source code and tests:

- `513` — TS3 error code for "nickname already in use"
- `770` — TS3 error code for "already member of channel"
- `768` — TS3 error code for "invalid channel ID" (used in a test negative assertion)

A reader encountering `e.getError().getId() == 513` cannot understand the condition without
looking it up. The same number in two places with no shared name means a future change to
one site can silently leave the other stale.

## Decision

Every numeric literal that carries domain meaning must be replaced by a named constant.
This applies equally to production code and test code — a magic number is just as opaque
in a test assertion as in a branch condition.

**Placement:**
- Declare the constant as close to its usage as possible.
- If the constant is used only within one class, declare it `private static final` on that class.
- If tests in the same package need to reference it, promote it to package-private (no modifier).
- Do not create a shared constants class just to collect unrelated values.

**Scope:**
- TS3 server error codes, port numbers, timeout values, sentinel values, and other
  protocol-level literals all qualify.
- Loop indices, array sizes for local collections, and other purely mechanical numbers
  do not require naming unless their value is non-obvious.

**New code:** magic numbers are not merged. Fix them in the same commit they are introduced.

**Pre-existing code:** fix on a dedicated branch and PR, separate from unrelated changes.

## Consequences

- Code reads as intent, not as raw protocol knowledge.
- A single constant definition is the canonical source of truth; changing a protocol value
  requires editing one place.
- Pre-existing magic numbers are addressed incrementally on their own branches, keeping
  history clean and reviews focused.
