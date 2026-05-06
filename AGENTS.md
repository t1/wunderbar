# AGENTS.md

## Project overview

WunderBar is a multi-module Maven project for code-first, low-ceremony consumer-driven contract testing in Java.
The core idea is:

- consumers define expectations in tests,
- WunderBar records those interactions into `bar` archives,
- providers replay those archives as dynamic tests.

The repo mixes library code, JUnit integration, a deployable mock server, and two demo applications that exercise the full workflow.

## Canonical documentation

Before changing public behavior or public API, read these first:

- `README.adoc` — top-level product documentation and usage model
- `junit/src/main/java/com/github/t1/wunderbar/junit/consumer/WunderBarApiConsumer.java`
- `junit/src/main/java/com/github/t1/wunderbar/junit/consumer/Level.java`
- `junit/src/main/java/com/github/t1/wunderbar/junit/consumer/WunderbarExpectationBuilder.java`
- `junit/src/main/java/com/github/t1/wunderbar/junit/provider/WunderBarApiProvider.java`
- `junit/src/main/java/com/github/t1/wunderbar/junit/provider/WunderBarTestFinder.java`

Also read any nearby `package-info.java` files before editing classes in that package.

## Module map

### `lib/`
Low-level shared code:

- HTTP request/response abstractions in `lib/src/main/java/com/github/t1/wunderbar/junit/http/`
- common utilities in `lib/src/main/java/com/github/t1/wunderbar/common/`
- mock expectation DTOs under `lib/src/main/java/com/github/t1/wunderbar/common/mock/`

This module is the shared foundation for the other modules.

### `junit/`
Main end-user API.

Contains:

- consumer-side annotations and extensions
- provider-side annotations and dynamic test support
- optional convenience custom AssertJ assertions
- expectation builders, BAR readers/writers, and test discovery

If a change affects how users write tests, this is usually the main module.

### `mock/`
WAR-packaged mock server.

- `mock/src/main/java/com/github/t1/wunderbar/mock/MockServlet.java`

This module turns recorded expectations into a deployable servlet-based mock service.

### `demo/order/`
Consumer demo application.

- Produces `target/wunder.bar`
- Contains REST and GraphQL consumer tests across unit, integration, and system levels
- Attaches the generated BAR as a Maven artifact during `package`

### `demo/product/`
Provider demo application.

- Quarkus-based provider
- Acceptance tests replay BAR files created by `demo/order`
- `demo/product/src/test/java/test/acceptance/ConsumerDrivenAT.java` is the best end-to-end provider example

## Build and test commands

### Preferred commands

- Unit tests only: `mvn test`
- Full test suite including integration/system/acceptance tests: `mvn verify`
- Full CI-style build: `mvn --batch-mode --show-version --no-transfer-progress -DCI=GitHub install`

CI runs on JDK 21 and 25 via `.github/workflows/maven.yml`.

### Module-scoped commands

Use `-pl <module> -am` to build a module plus required dependencies, for example:

- `mvn -pl lib -am test`
- `mvn -pl junit -am test`
- `mvn -pl demo/order -am verify`
- `mvn -pl demo/product -am verify`

### Important Maven caveat

Avoid `mvn clean install -DskipTests` for this repo.

`demo/order` attaches `target/wunder.bar` during `package`, so skipping the tests that generate it breaks installation.
You'll see:
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-install-plugin:3.1.4:install (default-install) on project wunderbar.demo.order: Failed to install artifact com.github.t1:wunderbar.demo.order:bar:products:4.0-SNAPSHOT: /Users/rdohna/workspace/t1/wunderbar/demo/order/target/wunder.bar -> [Help 1]
```

Preferred alternatives:

- `mvn package -DskipTests`
- or, if install is really required, add `-Dbuildhelper.skipAttach`

Also note that the parent POM sets the default goal to `install`.

## Test layout and naming

The parent POM splits test execution like this:

- Surefire: regular unit tests
- Failsafe: `*IT`, `*ST`, `*AT`

Observed naming conventions:

- `*UnitTest` / `*Test` for unit tests
- `*IT` for integration tests
- `*ST` for system tests
- `*AT` for acceptance tests

WunderBar itself also infers consumer test level from class names when `@WunderBarApiConsumer(level = AUTO)` is used.

Generated artifacts used by tests include:

- `target/wunder.bar`
- `target/system-wunder.jar`
- `target/weld-wunder.jar`

## Coding conventions observed in the codebase

### Java and dependencies

- Java 21 source/target in the parent POM
- Lombok is used in production code
- JUnit Jupiter, AssertJ, Mockito, MicroProfile, RESTEasy, and SmallRye GraphQL are central dependencies

### Test style

Tests commonly use:

- AssertJ BDD assertions: `then(...)`
- Mockito BDD stubbing: `given(...).willReturn(...)`
- WunderBar stubbing: `given(...).returns(...)`

Keep tests readable with clear given/when/then block separation.

### Documentation style

- Top-level docs are in AsciiDoc (`README.adoc`)
- Public API behavior is heavily documented in Javadoc on annotations and core entry points
- If behavior changes, update code, Javadoc, and `README.adoc` together

## Repo-specific implementation notes

- The consumer/provider architecture is annotation- and extension-driven. Small changes in annotations, extension wiring, or naming conventions can have broad impact.
- Demo modules are not throwaway samples; they are executable documentation and a regression net for the intended workflow.
- `demo/order` and `demo/product` depend on each other indirectly through generated artifacts and test setup. Treat demo changes as integration changes, not isolated examples.

## Change checklist for agents

When making changes:

1. Identify the affected module(s): `lib`, `junit`, `mock`, `demo/order`, `demo/product`.
2. Read the nearest `package-info.java` and the relevant public Javadocs.
3. Prefer the smallest possible change that preserves the consumer/provider model.
4. If public behavior changes, update `README.adoc` and the corresponding Javadocs.
5. Run the narrowest meaningful Maven command first, then broaden if needed.
6. Use `target/` for temporary outputs; do not rely on global temp locations.

## High-value files for orientation

- `README.adoc`
- `pom.xml`
- `.github/workflows/maven.yml`
- `junit/src/main/java/com/github/t1/wunderbar/junit/consumer/WunderBarApiConsumer.java`
- `junit/src/main/java/com/github/t1/wunderbar/junit/provider/WunderBarApiProvider.java`
- `junit/src/main/java/com/github/t1/wunderbar/junit/provider/WunderBarTestFinder.java`
- `lib/src/main/java/com/github/t1/wunderbar/junit/http/HttpRequest.java`
- `lib/src/main/java/com/github/t1/wunderbar/junit/http/HttpResponse.java`
- `mock/src/main/java/com/github/t1/wunderbar/mock/MockServlet.java`
- `demo/order/src/test/java/test/graphql/ProductsResolverIT.java`
- `demo/product/src/test/java/test/acceptance/ConsumerDrivenAT.java`
