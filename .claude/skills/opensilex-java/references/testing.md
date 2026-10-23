# Testing OpenSILEX Java code

Official notes (partly incomplete): `opensilex-doc/src/main/resources/technical-documentation/architecture/tests.md`. This file adds what the code shows.

## Contents
- Choosing a base class
- What the test environment gives you
- Writing an API test with the call builders
- Basic CRUD helper
- Isolation between tests
- Unit tests for DAOs
- Running tests

## Choosing a base class

| Need | Extend | Module |
|---|---|---|
| Triplestore only, REST calls, authentication | `AbstractSecurityIntegrationTest` (default choice) | `opensilex-security` tests |
| Triplestore + MongoDB | `AbstractMongoIntegrationTest` | `opensilex-core` tests |
| Plain unit test with no server | `AbstractUnitTest` | `opensilex-main` tests |
| Unauthenticated calls only | `AbstractIntegrationTest` | `opensilex-main` tests |

Inheritance: `AbstractIntegrationTest` (Jersey test server, `PublicCall` builders) -> `AbstractSecurityIntegrationTest` (admin creation, tokens, `UserCall`, CRUD helpers) -> `AbstractMongoIntegrationTest`.

Measured on the 87 test classes that contain `@Test`: `AbstractMongoIntegrationTest` 36, `AbstractSecurityIntegrationTest` 20, `AbstractUnitTest` 9, no base class 13 (plain unit tests), BrAPI/FAIDARE helpers 7.
- **JUnit 4.13.2 only** (`org.junit.Test`, `@Before/@After`, `@BeforeClass/@AfterClass`, `Assert`); no Jupiter, no AssertJ, no Hamcrest; Mockito in 6 files. Do not introduce them (`lint.py` rule `TEST-JUNIT5`).
- Surefire has no custom includes: a class is run only if its name is `Test*`, `*Test`, `*Tests` or `*TestCase`. `XxxAPITest`, `XxxApiTest` and `XxxDAOTest` are the local spellings; `lint.py` rule `NAME-TEST-SUFFIX` catches a class that would be skipped silently.
- Method names: `testXxx` in 76 % of methods, but about half of the methods added since 2025 are descriptive sentences (`groupWithoutProfileHasEmptyListOfUserProfiles`). Either is fine; do not mix both in one class.
- `assert*` calls come from `org.junit.Assert` (static imports, `import static org.junit.Assert.*` is tolerated in tests); keep the expected value first.
- A compile-checked API test to copy: `assets/templates/test/WidgetAPITest.java` (`ServiceDescription` + `testBasicCRUDAsAdmin` + conflict + search + `getModelsToClean`).

## What the test environment gives you

Tests start OpenSILEX with the `test` profile (`opensilex-main/src/main/resources/config/test/opensilex.yml`):
- embedded RDF4J LMDB triplestore (`RDF4JLMDBServiceFactory`), SHACL validation on, base URI `http://opensilex.test/`;
- embedded MongoDB (flapdoodle) on `127.0.0.1:28018`, database `test`;
- a temporary file system.

No Docker is needed for tests. On recent Ubuntu the embedded Mongo may need an older libssl - see `opensilex-doc/.../faq/common-issues.md` ("surefire error ... nosql test module").

The server is started once per test class, not per method, so state leaks between methods unless you clean it (below).

## Writing an API test with the call builders

Describe each service once with its Java method and path, then build calls from it:

```java
public static final ServiceDescription create;
static {
    try {
        create = new ServiceDescription(
                ProjectAPI.class.getMethod("createProject", ProjectCreationDTO.class), "/core/projects");
    } catch (NoSuchMethodException e) { throw new RuntimeException(e); }
}
```

`ServiceDescription` ties the test to the real API method, so a renamed or re-signatured endpoint breaks the test at compile/class-load time instead of silently testing a stale URL.

Call builders (inner classes of the base tests):
- `new UserCallBuilder(serviceDescription).buildAdmin()` - authenticated as admin (registers the token automatically); `.setUser(...)`/`.setUserEmail(...)` for other users; `new PublicCallBuilder<>(serviceDescription)...build()` for anonymous calls (see `AuthenticationAPITest`).
- On the builder: `setBody(dto)`, `addParam(name, value)` (query params), `setUriInPath(uri)` / `addPathTemplateParam(...)` (path), `setMultipartBody(...)`.
- On the call: `executeCallAndAssertStatus(Status)`, `executeCallAndDeserialize(new TypeReference<SingleObjectResponse<XxxGetDTO>>() {})` (asserts 2xx and returns a `Result`), `executeCallAndReturnURI()`, `executeCallAndReturnUriList()`.
- `executeCall()` returns a raw `Response` that must be closed.

Test the failure paths of the endpoint you touch (403 for a non-privileged user, 404 unknown URI, 409 duplicate, 400 invalid body), not just the happy path. The docs say it directly: don't just test basic CRUD.

## Basic CRUD helper

`testBasicCRUDAsAdmin(create, read, update, delete, entityToPost, entityToPut, typeReference)` covers create/read/update/delete and compares the sent fields with the response (nulls and empty values ignored). It requires DTOs extending `ResourceDTO<?>`. Use it for the baseline, then add focused tests. Examples to imitate: `GroupAPITest` (helper-based), `AreaAPITest` (`UserCall`), `AuthenticationAPITest` (public calls).

## Isolation between tests

`AbstractSecurityIntegrationTest` runs `clearGraphs()` after each test. Override `getModelsToClean()` to return every model class your test writes, otherwise data from one method leaks into the next and order-dependent failures appear. MongoDB-backed tests must clean their collections explicitly.

## Unit tests for DAOs

The architecture docs place DAO tests in `concept/dal` extending `AbstractUnitTest`. Where a DAO test needs a real triplestore, use the integration base class instead; do not mock `SPARQLService` for query behaviour - the value of the test is that the generated SPARQL actually runs.

## Running tests

Use JDK 17 (see `SKILL.md`). Examples:

```bash
mvn -pl opensilex-core test -Dtest=ProjectAPITest -DskipFrontBuild
mvn -pl opensilex-core test -Dtest=ProjectAPITest#testCreate -DskipFrontBuild
```

When the change spans modules, add `-am -Dsurefire.failIfNoSpecifiedTests=false`, or re-install the upstream modules first. Integration tests start a server and an embedded database; a class takes noticeably longer than a unit test, so run the targeted class, not the whole module, while iterating.

Do not copy the legacy style visible in older tests (`getJsonPostResponseAsAdmin`, `extractUriFromResponse`, `appendSearchParams`): those helpers are `@Deprecated`. `ProjectAPITest` still uses some of them; its `ServiceDescription` setup is the part worth imitating.
