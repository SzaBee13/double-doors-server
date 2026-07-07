# testing-reviewing

Use this skill when the user asks to test code, review code, or do both.

## What to do

- Inspect the changed files first.
- Run the smallest relevant tests before broader ones.
- For reviews, focus on bugs, regressions, missing tests, and risky edge cases.
- Prefer concrete findings over summaries.
- Include file and line references when reporting issues.

## Testing workflow

- Start with targeted tests for the touched area.
- If the change is broad or unclear, run the project’s main test/build command.
- Report exact commands run and whether they passed.

## Review workflow

- Read the diff and surrounding code.
- Check behavior, error handling, data flow, and concurrency.
- Look for absent or weak tests.
- If no problems are found, say so explicitly and mention residual risk.

## Output style

- Keep it short and factual.
- Put findings first.
- Use one-level bullet lists only.
