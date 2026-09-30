# Migration playbook

How the path in this repo scales from one service to a large estate, and a log of failures
reproduced here with their exact output.

---

## Approach at fleet scale

1. **Inventory and classify.** Per app: Boot version, JDK, Gradle version, plugin set, and
   fingerprints for the hazards in [HAZARDS.md](HAZARDS.md) (`scripts/scan.sh` is the
   single-repo form of that). The output is a distribution, not a list — which failure
   classes are common enough to automate.
2. **Raise the floors.** Annotation processors, bytecode libraries, build plugins, the
   wrapper. Mechanical, low-risk, and a precondition for everything else (see entry 1 below).
3. **Pilot on a representative sample** (~20 apps spanning the classes). Record every
   failure in the format below. This becomes the failure taxonomy.
4. **Automate the top classes** as recipes; leave decisions to humans. The *Auto* column in
   HAZARDS.md is the split.
5. **Roll out in waves** behind the shared BOM / version catalog, delivering each team a
   green PR plus the relevant playbook entries. The team's cost should be a review.
6. **Pre-flight the silent breaks** (below) per app before it ships — these are the ones
   that cost trust in a migration programme.

### Sequencing

```
floors → Boot 3.5 (jakarta, Security 6, Hibernate 6) → JDK source hazards → Gradle 9 → JDK 25 → Boot 4
```

This is the order used in this repo (tags `v2` … `v8`). For apps further behind, step through
Boot 3.0 and JDK 21 as intermediate stops.

- Framework before build tool: the Boot 2.7 Gradle plugin does not run on Gradle 9.
- Toolchains, so the JDK that compiles is independent of the JDK running Gradle.
- One axis per step. Boot, JDK, and Gradle are never bumped in the same change, so a
  failure has one possible cause and a rollback is one revert.
- Boot 4 last: it removes what 3.x deprecated, so a warning-free 3.5 makes it a small step.

### Pre-flight checklist — breaks a green build does not show

- [ ] charset-dependent file and stream I/O (JEP 400)
- [ ] trailing-slash routes used by real clients
- [ ] ID generation strategy against existing data (Hibernate 6)
- [ ] date/number formatting consumed by parsers, file names, downstream systems
- [ ] trace/span MDC keys after Sleuth → Micrometer Tracing
- [ ] agents (APM, JaCoCo, Mockito) under restricted dynamic attach
- [ ] `--add-opens` flags that are hiding reflective access
- [ ] every read endpoint called against real data after any persistence or session-scope change
- [ ] JDK warnings raised by dependencies (`sun.misc.Unsafe` from shaded libraries)

---

## Failure log

Format: symptom (verbatim), root cause, fix, automatable, blast radius.

### 1. JDK 25: `ExceptionInInitializerError` in `compileJava`

- **Symptom**
  ```
  > Task :compileJava FAILED
  > java.lang.ExceptionInInitializerError
  ```
  with `--stacktrace`: `at lombok.javac.apt.LombokProcessor.…(LombokProcessor.java:175)`
- **Cause** Lombok 1.18.20 uses `javac` internals that changed.
- **Fix** Upgrade Lombok to a JDK 25-capable release.
- **Automatable** Yes — version bump.
- **Blast radius** Every app on an old Lombok; compile-time; blocks all further JDK work.

### 2. Gradle configuration cache: 5 problems

- **Symptom**
  ```
  - build.gradle: line 107: registration of listener on 'Gradle.buildFinished' is unsupported
  - build.gradle: line 81: invocation of 'Task.project' at execution time is unsupported.
  5 problems were found storing the configuration cache, 4 of which seem unique.
  ```
- **Cause** Build logic reads the `Project` model during task execution and registers a
  build listener; neither can be serialised into the cache.
- **Fix** Move values to configuration time / injected services; replace the listener.
- **Automatable** Partly — common patterns yes, bespoke build logic no.
- **Blast radius** Any app with custom tasks; build-time; mandatory on Gradle 9.

### 3. Gradle 9: `JavaPluginConvention` removed

- **Symptom** `The org.gradle.api.plugins.JavaPluginConvention type has been deprecated. This is scheduled to be removed in Gradle 9.0.`
- **Cause** Top-level `sourceCompatibility` / `targetCompatibility`.
- **Fix** `java { toolchain { languageVersion = … } }`.
- **Automatable** Yes in build scripts; no when it originates in a third-party plugin.
- **Blast radius** Most older builds; build-time.

### 4. springfox on Boot 2.6+: NPE at context start

- **Symptom**
  ```
  OrdersApplicationContextTest > contextLoads() FAILED
      Caused by: java.lang.NullPointerException at WebMvcPatternsRequestConditionWrapper.java:56
  ```
- **Cause** springfox 3.0.0 assumes Ant path matching; Boot moved to `PathPatternParser`.
- **Fix** Replace with springdoc-openapi. The common workaround (a `BeanPostProcessor`
  reflecting into springfox, present here as `SpringfoxCompatibilityConfig`) should be
  deleted in the same change.
- **Automatable** Dependency and annotations yes; custom `Docket` config needs review.
- **Blast radius** Every springfox app; startup; hard blocker for Boot 3.

### 5. Trailing slash → 404 on Spring 6

- **Symptom** `Status expected:<200> but was:<404>` for `GET /api/orders/`.
- **Cause** Trailing-slash matching is off by default and its switch is deprecated.
- **Fix** Per-app: explicit mapping, gateway redirect, or client change.
- **Automatable** Detection only.
- **Blast radius** Any app whose clients send trailing slashes; runtime, after deploy, with a green build.

### 6. JSON serialisation recursion (latent, pre-existing)

- **Symptom** `Could not write JSON: Document nesting depth (1001) exceeds the maximum allowed`
- **Cause** Bidirectional `PurchaseOrder` ↔ `OrderLine` serialised from both sides.
- **Fix** `@JsonIgnore` on the back-reference; test asserts the shape.
- **Automatable** Detection only.
- **Blast radius** Not caused by the migration, but surfaced by it — expect migrations to uncover defects the owning team did not know about, and agree up front who fixes them.

### 7. Lazy loading after disabling open-in-view

- **Symptom** `GET /api/orders` → 500, `failed to lazily initialize a collection`, only once data exists.
- **Cause** `spring.jpa.open-in-view: false` set during the Boot 3 step; entities serialised outside a session.
- **Fix** Entity graphs on the read queries; full-stack `@SpringBootTest` over HTTP.
- **Automatable** No.
- **Blast radius** Runtime only; sliced tests with a mocked service cannot see it. Carried by tags `v3`–`v6` here.

### 8. Boot 4: trace ids disappear from logs

- **Symptom** Log prefix `[6f1e…,a3c1…]` becomes `[,]`. No error, no warning, green build.
- **Cause** A bare `micrometer-tracing-bridge-otel` dependency is no longer enough to auto-configure tracing.
- **Fix** `spring-boot-starter-opentelemetry`.
- **Automatable** Dependency swap yes; noticing it requires looking at a log line.
- **Blast radius** Every traced app; breaks log correlation and whatever alerts depend on it.

### 9. JDK 25: `sun.misc.Unsafe` warning from a dependency

- **Symptom** `WARNING: sun.misc.Unsafe::objectFieldOffset has been called by io.opentelemetry.internal.shaded.jctools.util.UnsafeAccess`
- **Cause** Shaded JCTools inside an older OpenTelemetry SDK; the application's own code was already clean.
- **Fix** Library upgrade (arrived with the Boot 4 BOM).
- **Automatable** Yes — but `jdeps` on application classes does not see it; it only shows at runtime.
- **Blast radius** A warning today, a failure when the method is removed.
