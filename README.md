# Java application modernization — a worked example

One service taken from **Spring Boot 2.7 / Java 17 / Gradle 8** to
**Spring Boot 4.1 / Java 25 / Gradle 9**, one axis per commit, with every step built,
tested and run before the next one started.

The point is not the version numbers. It is the order of operations, the failures that a
green build does not show, and what of this can be automated across a large fleet.

## The problem

[`legacy-orders-service`](legacy-orders-service/) at tag
[`v1-boot27-java17`](https://github.com/smalistean/java-migration-lab/tree/v1-boot27-java17)
is a deliberately typical 2016–2021 enterprise service: REST, Spring Security, JPA, a
scheduled job, file exchange with other teams, custom Gradle build logic. It builds green
and its tests pass. It also cannot move forward:

| Try this at `v1` | What happens |
|---|---|
| Build on JDK 25 | `compileJava` dies with a bare `ExceptionInInitializerError` — a four-year-old Lombok, which the message never mentions |
| Turn on Gradle's configuration cache (default in Gradle 9) | 5 problems: `Task.project` at execution time, a `buildFinished` listener |
| Bump Spring Boot to 3.x | does not compile: `javax.*`, `WebSecurityConfigurerAdapter`, springfox; then HQL that Hibernate 6 rejects |
| `scripts/scan.sh` | `sun.misc.Unsafe`, `SecurityManager`, `finalize()`, 4 charset-dependent I/O calls, 4 locale-dependent calls, 36 `javax` imports |

The full inventory — about 60 items with symptom, cause, fix and whether each can be
automated — is in [`docs/HAZARDS.md`](docs/HAZARDS.md). The v1 code is intentionally
outdated and insecure; see [SECURITY.md](SECURITY.md).

## The migration, commit by commit

| # | Commit | Tag | What changed |
|---|---|---|---|
| 1 | [`9baab53`](https://github.com/smalistean/java-migration-lab/commit/9baab53) | `v1-boot27-java17` | **The problem state.** Legacy service, hazard catalogue, scanner |
| 2 | [`43242de`](https://github.com/smalistean/java-migration-lab/commit/43242de) | `v2-dependency-floors` | Lombok, Mockito and library versions raised; duplicated test dependency fixed. No source changes |
| 3 | [`10dd7e9`](https://github.com/smalistean/java-migration-lab/commit/10dd7e9) | `v3-boot35` | **Boot 2.7 → 3.5.** `jakarta.*`, Security 6 lambda DSL, Hibernate 6 queries and ID strategy, springfox → springdoc, Sleuth → Micrometer Tracing, trailing-slash handling, JUnit 5 |
| 4 | [`5dec498`](https://github.com/smalistean/java-migration-lab/commit/5dec498) | `v4-jdk-hazards` | Explicit charsets and locales, `java.time`, `Unsafe` → `AtomicLong`, `SecurityManager` and `finalize()` removed. Still on Java 17 |
| 5 | [`f99be53`](https://github.com/smalistean/java-migration-lab/commit/f99be53) | `v5-gradle9` | **Gradle 8.14 → 9.7**, configuration cache and build cache on, toolchains, build logic rewritten with typed task inputs |
| 6 | [`ea23792`](https://github.com/smalistean/java-migration-lab/commit/ea23792) | `v6-java25` | **Java 17 → 25.** A one-line toolchain change, because of commits 2 and 4 |
| 7 | [`bf68ded`](https://github.com/smalistean/java-migration-lab/commit/bf68ded) | `v7-api-integration-test` | Regression fix (see below) and a full-stack HTTP test |
| 8 | [`f25f3c6`](https://github.com/smalistean/java-migration-lab/commit/f25f3c6) | `v8-boot4` | **Boot 3.5 → 4.1.** Modular starters, Jackson 3, `RestClient`, tracing starter |

Read any step in isolation, for example
[`v2…v3`](https://github.com/smalistean/java-migration-lab/compare/v2-dependency-floors...v3-boot35)
for the Boot 3 change on its own.

**Why this order.** Floors first, because nothing else can be verified until the toolchain
runs. Framework before build tool, because the Boot 2.7 Gradle plugin does not run on
Gradle 9. JDK source hazards fixed while still on 17, so each fix is testable before the
JDK moves — which is why the actual Java 25 switch is one line. Boot 4 last: it removes
what 3.x deprecated, so a clean 3.5 makes it a small step.

## What the build did not catch

Three defects got past compilation and the unit tests. Each was found by starting the jar
and calling it, and each now has a test.

- **Infinite JSON recursion** (order → lines → order). Latent in v1; fixed in commit 3.
- **500 on every read endpoint once data existed.** Introduced in commit 3 by turning off
  `open-in-view`; the sliced tests mock the service and stayed green. Found three commits
  later, fixed in commit 7, which adds a `@SpringBootTest` that drives
  create → list → search over HTTP. Tags `v3`–`v6` carry this bug; it is left in the
  history rather than rewritten away.
- **Trace IDs vanished from the logs on Boot 4.** Nothing failed; the log prefix simply went
  from `[traceId,spanId]` to `[,]`. Fixed in commit 8.

This is the argument for a per-app runtime pre-flight in a migration programme:
[`docs/PLAYBOOK.md`](docs/PLAYBOOK.md) has the checklist and the failure log.

## Before and after

| | `v1` | `v8` |
|---|---|---|
| Spring Boot / Framework | 2.7.18 / 5.3 | 4.1.1 / 7 |
| Java | 17 (fails on 25) | 25 |
| Gradle | 8.14.3, 5 configuration-cache problems | 9.7.1, configuration cache reused, no deprecation warnings |
| `scan.sh` findings | Unsafe, SecurityManager, finalize, 8 charset/locale calls, 36 `javax` imports | none |
| Tests | 7 (one JUnit 4) | 12, including full-stack HTTP, charset and locale regression tests |
| Third-party libraries | springfox, Sleuth, Lombok, Guava, commons-io, commons-lang3, joda-time | springdoc, OpenTelemetry starter |

## Run it

Needs a JDK 25 installation (the compile/test toolchain) and any JDK 17+ to launch Gradle; the wrapper fetches Gradle itself.

```bash
cd legacy-orders-service
./gradlew build                      # 12 tests
./gradlew bootRun                    # http://localhost:8080/swagger-ui.html  (viewer/viewer, operator/operator)
../scripts/scan.sh                   # set JDK17_HOME and JDK25_HOME
```

To see the starting point: `git checkout v1-boot27-java17`.

## Not done

- The same path replayed with OpenRewrite recipes, to measure what automation covers versus
  the manual diff above. The *Auto* column in `docs/HAZARDS.md` is an assessment, not yet a measurement.
- Tests still use H2; a real engine via Testcontainers is what would catch Hibernate
  dialect and ID-generation differences.
- `EnumType.ORDINAL` is unchanged — switching it needs a data migration, not a code change.
- Users are in-memory demo accounts, and no OTLP endpoint is configured for trace export.

## How this was built

The code and documentation in this repository were produced with
[Claude Code](https://claude.com/claude-code), Anthropic's AI coding assistant, working
under my direction; commits carry a `Co-Authored-By` trailer saying so. Each step was
verified by building, running the tests and calling the started application — the three
defects above were found that way, not by reading the diff.
