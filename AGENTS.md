# Agent Guidelines

## Routes

 - For new endpoints, make sure to use tilgang-plugin so that only one consumer can use it. We have decided to add new endpoints for new consumers rather than sharing REST endpoints. This is to avoid breaking changes.
 - Always put PII in payloads, never in URL.
 - We have strong backward compatability requirements. Always thing about compatability when changing routes.

## Database

 - We use a wrapper around JDBC, source code here https://github.com/navikt/aap-kelvin-komponenter/tree/main/dbconnect

## Git

- Leave changes uncommitted by default so the user can review first.
- You may commit and push when the user explicitly asks for it after review.
- **Never push directly to `main`.** All changes must go through a pull request.

### Small, Incremental Changes

- **Always make the smallest possible change** that moves toward the goal
- Keep existing functionality working while adding new features
- One logical change at a time - don't combine multiple improvements
- Verify each change compiles and tests pass before moving to next change

### Documentation

- Don't add docstrings to methods and classes unless they are very complicated
- Prefer clear method names over documentation
- Add comments only when the code needs clarification
- **Always run `npx prettier --write <file>` on markdown files after creating or editing them**
- If you make something complicated, suggest to the user to update the sysdoc as well (at https://github.com/navikt/aap-intern-sysdok/).

### Immutable State

We prefer immutable state throughout the codebase. Avoid mutating objects and collections; instead, create new instances with the desired changes.

### Test-Driven Development

Always write tests before making functional changes. Tests should be written first to:

- Define expected behavior
- Drive the design of the implementation
- Ensure the changes work as intended

### Testing Strategy

For testing, prefer in-memory fakes over mocks. Fakes provide:

- More realistic behavior
- Better refactoring support
- Clearer test intentions
- Reduced coupling to implementation details

### File Search

- Never run `find` (or similar) against `/` or other paths outside the repo working directory. Scope searches to the repo (or known dependency source/cache dirs) instead.

### Gradle Commands

- **Never use --no-daemon** when running Gradle tests or builds - it's slower
- The Gradle daemon improves build performance through caching and hot JVM
- **Always run `./gradlew test` before committing** to ensure all tests pass
- **Run `./gradlew detektMain` after significant code changes** to check code quality
- Fix any Detekt violations before committing
- Compiling alone is not sufficient — always run the full test suite

### Domain Classes vs. Kontrakt

Never use kontrakt (e.g. `behandlingsflyt.kontrakt`) types directly in domain logic, repositories, or database storage. Always map to local domain classes first. This keeps the domain decoupled from external contracts.

Also for responses and requests in API endpoints. Always make new data classes and map to domain objects.

### Database Migrations

- Do not add new foreign keys with `ON DELETE CASCADE`.
- Prefer TEXT over VARCHAR(n).
