# Java Application Modernization — reference case

A compact, runnable case study of the migration path **Spring Boot 2.7 → 3.x → 4**,
**Java 17 → 25**, and **Gradle 8 → 9**: a representative legacy service, a catalogue of
what breaks on that path and why, and the scanning and rollout approach for doing it
across a large application fleet rather than one repo at a time.

> **`legacy-orders-service/` is intentionally outdated.** It is a fixture that reproduces a
> typical 2016–2021 enterprise service — `WebSecurityConfigurerAdapter`, `javax.*`,
> `sun.misc.Unsafe`, plaintext demo credentials, abandoned dependencies. Those are the
> migration targets, not oversights. Nothing in it is production code. See [SECURITY.md](SECURITY.md).

## What's here

| Path | What it is |
|---|---|
| [`legacy-orders-service/`](legacy-orders-service/) | The "before": Spring Boot 2.7.18 / Java 17 / Gradle 8.14 order service. Builds green, tests pass |
| [`docs/HAZARDS.md`](docs/HAZARDS.md) | Catalogue of ~60 migration hazards in the service: symptom, root cause, fix, and whether it can be automated |
| [`docs/PLAYBOOK.md`](docs/PLAYBOOK.md) | Rollout approach for a multi-thousand-app estate, plus recorded failures with their exact error output |
| [`scripts/scan.sh`](scripts/scan.sh) | Pre-migration scanner: `jdeps`, `jdeprscan`, charset/locale/`javax` checks, Gradle 9 and configuration-cache readiness |

## Why the hazards matter more than the happy path

A version bump is easy. What costs time in a real programme is the set of failures that a
green build does not reveal, or that reveal themselves with no useful message:

- **JDK 25, first contact** — the build stops with a bare `ExceptionInInitializerError` in
  `compileJava`. The cause is a four-year-old Lombok reaching into `javac` internals; the
  message never mentions it. Dependency floors have to be raised before any code changes.
- **JEP 400** — `new FileWriter(file)` silently changes encoding on any host that was not
  already UTF-8. No exception; the finance CSV is just wrong.
- **Spring 6 path matching** — `/api/orders/` stops matching `/api/orders`. The app starts,
  the health check is green, and a mobile client gets 404s.
- **Hibernate 6** — `GenerationType.AUTO` changes strategy. A data problem, not a compile error.
- **Gradle 9** — configuration cache is on by default; any task touching `Project` at
  execution time fails.

Each is reproduced in the fixture and documented in [`docs/HAZARDS.md`](docs/HAZARDS.md).

## Run it

Requires JDK 17 and JDK 25 (the Gradle wrapper is included).

```bash
cd legacy-orders-service
JAVA_HOME=/path/to/jdk-17 ./gradlew clean build       # green baseline, 7 tests
JAVA_HOME=/path/to/jdk-25 ./gradlew clean build       # fails: Lombok vs JDK 25
JAVA_HOME=/path/to/jdk-17 ./gradlew build --configuration-cache   # 5 problems
```

Scan before migrating:

```bash
JDK17_HOME=/path/to/jdk-17 JDK25_HOME=/path/to/jdk-25 ./scripts/scan.sh
```

Sample output (abridged):

```
2. JDK-internal API usage (jdeps)
   com.acme.orders.service.OffHeapCounter -> sun.misc.Unsafe   JDK internal API (jdk.unsupported)
3. deprecated-for-removal API usage (jdeprscan)
   class com/acme/orders/util/TempSpoolFile overrides deprecated method java/lang/Object::finalize()V (forRemoval=true)
   class com/acme/orders/service/SandboxRunner uses deprecated method java/lang/System::setSecurityManager(...) (forRemoval=true)
4. charset-unsafe I/O (JEP 400)
   ReportService.java:38:  new PrintWriter(new FileWriter(out))
6. javax.* -> jakarta.* candidates
   20 javax.persistence   7 javax.servlet   6 javax.validation   2 javax.annotation   1 javax.transaction
8. configuration-cache violations
   - build.gradle: line 81: invocation of 'Task.project' at execution time is unsupported.
   - build.gradle: line 107: registration of listener on 'Gradle.buildFinished' is unsupported
   5 problems were found storing the configuration cache, 4 of which seem unique.
```

## Status

| Step | State |
|---|---|
| Legacy baseline (Boot 2.7 / Java 17 / Gradle 8), tag `v1-boot27-java17` | done |
| Hazard catalogue and pre-migration scanner | done |
| Boot 3.5, `jakarta.*`, Security 6, Hibernate 6 | next |
| Java 21 → 25, Gradle 9 with configuration cache | next |
| Boot 4, then the same path replayed with OpenRewrite recipes | next |

Each step lands as its own tagged commit so the diff for one class of change can be read in isolation.
