---
name: aiagentos-lesson-dev
description: "The AIagentos lesson development workflow is packaged as the workspace skill at .github/skills/aiagentos-lesson-dev/SKILL.md."
---

# AIagentos Lesson Development

The executable workflow is maintained in the workspace skill:

[AIagentos lesson development skill](../.github/skills/aiagentos-lesson-dev/SKILL.md)

Invoke it with a lesson number, for example `16`. It reads lesson material from
`doc/class/`, follows the repository's Maven modules and persistence conventions, and reports
build evidence and any manual provider smoke-test steps. Keep this file as the legacy entry
point; update the skill rather than duplicating its workflow here.
