# Agent skills

Skills for AI coding agents working in projects that **use** paging-kit. Each skill is a directory with
a `SKILL.md` — YAML frontmatter (`name`, `description`) plus instructions — and the reference files it
points to, in the [Agent Skills](https://agentskills.io) layout.

| Skill | Use it when |
|---|---|
| [`paging-kit`](paging-kit/SKILL.md) | installing paging-kit into an app; writing a loader over a backend; a ViewModel holding a paged collection; lists that load more on scroll; several paged sections or grouped/sticky lists; wrapping chips that page; shimmer, empty, error and retry UI; pull to refresh, search and filters; adding or deleting items without a reload; tests; debugging blank lists, runaway loading or duplicate keys; reviewing paging code |

`paging-kit` keeps its entry file short and loads detail on demand:

```
paging-kit/
├── SKILL.md                         the model, orientation greps, task router, verification checklist
└── references/
    ├── setup.md                     dependencies, graph (Metro or by hand), composition root, previews and PagingPreviewParameterProvider
    ├── paginator.md                 PageLoader shapes (pages, offsets, cursors), the ViewModel, fetch, edits, reset
    ├── lists.md                     PaginationList, PagedItemsParams, windows, grouped windows, scroll state
    ├── flow-row.md                  PaginationFlowRow and how it differs from the list
    ├── states-and-errors.md         what each state renders, slots, retry, pull to refresh, search and filters
    ├── testing.md                   commonTest recipes for paginators and ViewModels
    └── troubleshooting.md           error messages, symptoms, review checklist
```

## Using a skill

**Without installing.** An agent can read `skills/paging-kit/SKILL.md` straight from this repository
and follow its links; nothing in it depends on being installed.

**Installed**, so the agent loads it on its own when a task matches the description — copy the skill
directory into the directory your agent loads skills from:

```sh
git clone --depth 1 https://github.com/Thernal/paging-kit.git /tmp/paging-kit

# Claude Code, for one project
mkdir -p .claude/skills && cp -R /tmp/paging-kit/skills/paging-kit .claude/skills/

# Claude Code, for every project on this machine
mkdir -p ~/.claude/skills && cp -R /tmp/paging-kit/skills/paging-kit ~/.claude/skills/
```

Runtimes that read Agent Skills from another directory (for example `.agents/skills/`) take the same
directory unchanged.

A skill describes the revision of the kit it was copied from. When the project moves to a newer
paging-kit, copy the skill again from the same revision.

**With skill-manager** (the author's own projects), the skill is not copied on its own: `skillctl.sh kit
install paging-kit` takes the code and this skill together, renamed to the project's package, and records
the revision in `kits.lock` so both are offered every later change — see [`kit.yml`](../kit.yml).

## Maintaining

The skill restates the public contracts documented in [`paging/api/README.md`](../paging/api/README.md)
for an agent audience. A change to a public contract updates that README and the matching reference
file in the same change; an exception message changed in `paging/impl` updates
[`troubleshooting.md`](paging-kit/references/troubleshooting.md).
