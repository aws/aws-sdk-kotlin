# Branch protection configuration

This repo runs CI from two origins that produce status checks under **different
names**, so branch protection is configured against a single aggregate context
that both origins produce under the same name.

## The one required status context

Configure exactly one required status check on protected branches:

- **`required`**

Both the same-repo aggregate (`.github/workflows/ci-required.yml`) and the
privileged fork-PR entrypoint (`.github/workflows/external-pr-ci.yml`) produce a
job named `required`, so this single rule covers both origins.

## Why the individual checks are not listed as required

Fork PRs and same-repo PRs produce status contexts under different names:

- **Fork PRs** run through the privileged entrypoint
  (`external-pr-ci.yml`), which calls the reusable workflows via `uses:`. Reusable
  workflow job contexts are **prefixed by the caller job name**, e.g.
  `ci / jvm (17)`, `reduced-ci / jvm (17)`, `service-ci / build-sdk`.
- **Same-repo PRs** run each workflow directly via its own `pull_request` /
  `merge_group` triggers, producing **unprefixed** contexts, e.g. `jvm (17)`,
  `build-sdk`, `verify-transform`.

A context that can only ever be produced by **one** origin must **not** be marked
required: it would block the other origin's PRs forever (the check would never
report). For example, marking `ci / jvm (17)` required would block every same-repo
PR (which only ever produces `jvm (17)`), and marking `jvm (17)` required would
block every fork PR (which only ever produces `ci / jvm (17)`).

The `required` aggregate is the only context emitted under the same name on both
paths, which is why it is the sole required check.

## Caveat on the same-repo aggregate

`ci-required.yml`'s `required` job is a naming shim: it produces the aggregate
context on the same-repo path, but it does **not** itself depend on or gate the
individual same-repo checks (`jvm`, `all-platforms`, `verify-transform`,
`build-sdk`, `e2e-tests`, `changelog-verification`). Those same-repo checks run
independently via their own triggers and should remain individually required
**for the same-repo path** if you want them enforced there — but per the rule
above, only enforce contexts that the same-repo path actually produces.

On the fork-PR path the aggregate is real gating: `external-pr-ci.yml`'s
`required` job `needs:` all six caller jobs and fails if any failed or was
cancelled.
