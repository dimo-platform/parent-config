# parent-config

Shared Spring Boot library and parent POM for the Dimo services. Java 21, Maven (`./mvnw verify`).

## Coding standards
Follow the user's global standards in `~/.claude/CLAUDE.md` (explicit types, MapStruct, naming,
tests with Arrange–Act–Assert and `<method>_<behavior>` names). Keep controllers, DTOs and
business rules out of this library: only shared infrastructure belongs here.

## Rules for this repo
- `develop` is protected: change it through PRs (`feature/x`, `bug/x`) with CI green.
- Public classes, auto-configuration and the parent POM are contracts for every service. Mark
  breaking changes with a MAJOR version bump.
- Never commit tokens, `settings.xml` or env files.
- Releases are tags `vX.Y.Z[-rc.N]`; see README.

## graphify
If `graphify-out/graph.json` exists, run `graphify query "<question>"` before grepping or reading
source for a codebase question, and read raw files only for lines the graph located.
After modifying code run `graphify update .`.
