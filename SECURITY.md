# Security

`legacy-orders-service/` is a **migration demo fixture**, deliberately kept in an outdated
state. It intentionally contains, among other things:

- in-memory demo credentials and `NoOpPasswordEncoder`
- CSRF protection disabled
- `sun.misc.Unsafe`, a `SecurityManager` installation, and a `finalize()` override
- charset- and locale-dependent I/O
- end-of-life dependencies (Spring Boot 2.7.x, springfox 3.0.0, Spring Cloud Sleuth)

These are the subject of the case study and are catalogued in [docs/HAZARDS.md](docs/HAZARDS.md).
Scanner findings against that directory are expected — please don't report them.
Nothing here is deployed, and none of it should be copied into a real application.
