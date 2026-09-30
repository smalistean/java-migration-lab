# Migration hazard catalogue

Everything in `legacy-orders-service/` that breaks, or silently changes behaviour, on the
path Boot 2.7 → 3.x → 4, Java 17 → 25, Gradle 8 → 9.

For each hazard: where it is, what you see, the fix, and **Auto** — whether a recipe
(OpenRewrite or similar) can handle it without a human decision. That last column is what
determines effort across a large fleet.

Items marked ✔ were reproduced against this repo; their output is quoted.

---

## A. Spring Boot 2.7 → 3.x

### A1. `javax.*` → `jakarta.*`

36 imports across 6 files (`scan.sh` step 6): persistence, validation, servlet, annotation, transaction.

| Fix | Auto |
|---|---|
| Package rename | yes |
| `javax.transaction.Transactional` in `OrderService` → Spring's `@Transactional` (propagation/isolation support), not the Jakarta one | partial — a choice |
| Third-party jars still compiled against `javax` | no — upgrade the library, or bytecode-transform (Eclipse Transformer) as a last resort |

### A2. Spring Security 6 — `config/SecurityConfig.java`

Does not compile on Boot 3.

| Hazard | Fix | Auto |
|---|---|---|
| `WebSecurityConfigurerAdapter` removed | `SecurityFilterChain` bean | yes |
| `antMatchers()` | `requestMatchers()` | yes |
| `authorizeRequests()` + `.and()` chaining | `authorizeHttpRequests()` with lambda DSL (mandatory by Security 7) | yes |
| `@EnableGlobalMethodSecurity` | `@EnableMethodSecurity` | yes |
| `NoOpPasswordEncoder`, CSRF disabled | real defects independent of the migration — raise with the owning team | no |

### A3. Trailing-slash matching — `config/WebMvcConfig.java`

`setUseTrailingSlashMatch(true)` is deprecated in Spring 6 and the default flipped:
`/api/orders/` no longer matches a mapping on `/api/orders`.

Compiles, starts, passes health checks — and returns 404 to any client sending the slash.
`OrderControllerWebTest.listAlsoWorksWithTrailingSlash` exists to catch it.

Fix is a per-app decision (explicit dual mapping, gateway redirect, or client change). **Auto: detect only.**

### A4. Hibernate 5 → 6 — `repo/OrderRepository.java`, entities

| Hazard | Where | Effect | Auto |
|---|---|---|---|
| `like :prefix%` | `findBySkuPrefix` | rejected by the stricter HQL parser | no |
| implicit cross join `from PurchaseOrder o, OrderLine l where l.order = o` | `findBySkuPrefix` | rewrite as explicit `join o.lines l` | no |
| positional `order by 2` | `totalsByCustomer` | rejected | no |
| `rownum` in a native query | `findTopN` | vendor lock-in; use `Pageable` | no |
| `GenerationType.AUTO` | both entities | **strategy changes to sequence-based** — can collide with existing rows. Pin `IDENTITY` or a named sequence before upgrading | detect only |
| `hibernate.id.new_generator_mappings: false` | `application.yml` | property removed | yes |
| camelCase `@Column` names | entities | verify against physical naming strategy and real schema | no |
| `EnumType.ORDINAL` | `PurchaseOrder.status` | not a migration break; corrupts data if the enum is ever reordered | detect only |

The ID-generation change is the highest-risk item here: it is a data problem that no compile or unit test surfaces. Testing against the real database engine (Testcontainers) instead of H2 is what catches this class.

### A5. End-of-life dependencies — `build.gradle`

| Dependency | Replacement | Note |
|---|---|---|
| springfox 3.0.0 | springdoc-openapi | ✔ already needs a reflection hack on Boot 2.7 (`SpringfoxCompatibilityConfig`); without it: `NullPointerException at WebMvcPatternsRequestConditionWrapper.java:56`. Delete the hack and the `ANT_PATH_MATCHER` property with it |
| spring-cloud-starter-sleuth | Micrometer Tracing + OTel bridge | `logback-spring.xml` reads `traceId`/`spanId` from MDC — verify log correlation after the swap |
| joda-time | `java.time` | |

### A6. Configuration and registration

| Hazard | Where | Fix | Auto |
|---|---|---|---|
| auto-config in `META-INF/spring.factories` | resources | `META-INF/spring/…AutoConfiguration.imports` | yes |
| `@ConstructorBinding` on the type | `AppProperties` | remove (single constructor is inferred) | yes |
| `spring.redis.*` | `application.yml` | `spring.data.redis.*` | yes |
| `management.metrics.export.prometheus.*` | `application.yml` | `management.prometheus.metrics.export.*` | yes |
| `allow-circular-references: true` | `application.yml` | find and break the cycle | no |
| `@Primary ObjectMapper` replacing Boot's | `JacksonConfig` | customizer bean instead; also the Jackson 3 touchpoint for Boot 4 | partial |
| `RestTemplateBuilder.setConnectTimeout/setReadTimeout` | `PricingClient` | deprecated in 3.4; consider `RestClient` | yes |

---

## B. Java 17 → 25

### B1. Toolchain floors come first ✔

Pointing JDK 25 at the unchanged project:

```
> Task :compileJava FAILED
Execution failed for task ':compileJava'.
> java.lang.ExceptionInInitializerError
```

Nothing in the message names the cause. `--stacktrace` does:

```
at lombok.javac.apt.LombokProcessor.placePostCompileAndDontMakeForceRoundDummiesHook(LombokProcessor.java:175)
```

Lombok 1.18.20 depends on `javac` internals that moved. Same class of problem: `mockito-inline` 3.12 (agent attach, superseded by Mockito 5), old Byte Buddy/ASM pulled in transitively, the build tool itself.

**Consequence for a fleet:** the first pass is not a code migration — it is an inventory of apps pinned below the minimum versions of annotation processors, bytecode libraries, and build plugins. **Auto: yes** (version bumps).

### B2. JEP 400 — UTF-8 by default — `service/ReportService.java` ✔ (scan step 4)

```
ReportService.java:38   new PrintWriter(new FileWriter(out))
ReportService.java:55   new BufferedReader(new FileReader(file))
ReportService.java:68   FileUtils.readFileToString(manifest)
ReportService.java:76   content.getBytes()
```

Since JDK 18 these use UTF-8 rather than the platform charset. On hosts that were not UTF-8 the bytes written and read change, with no error. Fix: explicit charset everywhere — and confirm with the consumer of each file which encoding it actually expects. `-Dfile.encoding=COMPAT` is a temporary bridge. **Auto: yes for the code; no for the decision.**

### B3. Locale and date formatting ✔ (scan step 5)

| Where | Hazard |
|---|---|
| `ReportService:59` `toUpperCase()` | locale-dependent (Turkish dotted İ breaks lookups) → `Locale.ROOT` |
| `ReportService:72` `Locale.getDefault()` | output varies by host |
| `OrderService:32` static `SimpleDateFormat("dd/MMM/yyyy …")` | not thread-safe; and `MMM` output differs across JDK CLDR versions (`Sep`/`Sept`), which breaks log parsers |

### B4. JDK internals and removed APIs ✔ (scan steps 2–3)

| Where | Hazard | Fix | Auto |
|---|---|---|---|
| `OffHeapCounter` | `sun.misc.Unsafe` memory access — deprecated for removal (JEP 471/498); reflective access to `ArrayList.elementData` | `AtomicLong` / `VarHandle`; delete the reflection | partial |
| `SandboxRunner` | `System.setSecurityManager` throws on JDK 24+ (JEP 486); `AccessController` for removal | **no drop-in** — needs a design decision (process/container isolation) with the owning team | no |
| `TempSpoolFile` | `finalize()` for removal | already `Closeable`; drop the finalizer, or `Cleaner` | yes |
| `build.gradle` test `jvmArgs` | `-XX:+EnableDynamicAgentLoading` masks the JEP 451 warning | configure agents explicitly | partial |

---

## C. Gradle 8 → 9

Gradle 9 enables the configuration cache by default and removes long-deprecated APIs.

### C1. Configuration-cache violations ✔ (scan step 8)

```
- build.gradle: line 107: registration of listener on 'Gradle.buildFinished' is unsupported
- build.gradle: line 81: invocation of 'Task.project' at execution time is unsupported.
- build.gradle: line 83: invocation of 'Task.project' at execution time is unsupported.
- build.gradle: line 84: invocation of 'Task.project' at execution time is unsupported.
5 problems were found storing the configuration cache, 4 of which seem unique.
```

| Hazard | Fix |
|---|---|
| `project.*` inside `@TaskAction` / `doLast` (`StampBuildTask`, `printReport`) | capture values or inject `ProjectLayout` at configuration time |
| `gradle.buildFinished {}` | build service / `FlowAction` |
| `System.getenv('BUILD_ID')`, `InetAddress.getLocalHost()` at configuration time | `providers.environmentVariable(...)`; drop host lookups |
| `File` held as task state with `@Internal` | `RegularFileProperty` with `@InputFile` — also restores up-to-date checks and cacheability |

### C2. Removed APIs ✔ (scan step 7)

```
The org.gradle.api.plugins.JavaPluginConvention type has been deprecated.
This is scheduled to be removed in Gradle 9.0.
```

Triggered by top-level `sourceCompatibility`/`targetCompatibility`; fix with a `java { toolchain { … } }` block, which also decouples the JDK that runs Gradle from the JDK that compiles — the mechanism for staging a JDK rollout. `project.buildDir` → `layout.buildDirectory`. When the convention usage is inside a third-party plugin, the only fix is a plugin upgrade.

### C3. Dependency hygiene

`spring-boot-starter-test` is declared twice, the second declaration cancelling the first one's `junit-vintage` exclusion. No version catalog, no dependency locking.

---

## D. Tests

| Hazard | Where | Fix | Auto |
|---|---|---|---|
| `@MockBean` | `OrderControllerWebTest` | `@MockitoBean` (Boot 3.4+) | yes |
| JUnit 4 runner and `TemporaryFolder` rule | `LegacyReportServiceTest` | JUnit 5, `@TempDir` | yes |
| H2 standing in for the production database | all | Testcontainers — H2 hides Hibernate 6 dialect and ID-generation issues | no |

---

## E. Boot 3.5 → 4

Boot 4 removes what 3.x deprecated, so the effective strategy is: reach 3.5 with zero
deprecation warnings first. Beyond that, the main work items are the modularised starters
(dependency coordinates change) and Jackson 2 → 3 (`com.fasterxml.jackson` → `tools.jackson`),
for which `JacksonConfig` is this project's touchpoint. Details to be recorded against the
official migration guide as that step lands.
