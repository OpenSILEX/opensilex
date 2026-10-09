# Java practices for the release defined in the project

**Source of truth**: `java.compiler.version` in `opensilex-parent/pom.xml` (first occurrence; read it with
`scripts/jdk.sh level`). It feeds `maven-compiler-plugin <release>`, so the compiler rejects every language feature and
API newer than that release. Value when this file was written: **17**. README: "our project is tested with JDK 17". Annotation processing is
disabled (`-proc:none`); there is no `-Werror`.

> This file is written for release 17. If `jdk.sh level` prints another number, re-validate each row of the tables below
> against that release before using it, and update the file.

The opt-in profile `-Pfor-java-11` sets `java.compiler.version=11`, but it is **stale**: 317 `.toList()` calls
(Java 16) in 127 files and 3 records (Java 16) cannot compile with release 11. Do not target 11, do not use that
profile as an argument against Java 12-17 features.

## 1. What the code base actually uses (census over 1,155 files)

| Feature (since) | Uses (files) | In files added since 2025 | Verdict for new code |
|---|---|---|---|
| `var` for locals (10) | 403 (59) | 169 (16) | **Use** when the right-hand side shows the type (`var dao = new XxxDAO(sparql);`). Not for fields, parameters or when the type is not obvious from the call. |
| `Stream.toList()` (16) | 317 (127) | 42 (14) | **Use** for read-only results. The list is *unmodifiable*: if a caller later does `add`/`remove`, use `Collectors.toList()` or `new ArrayList<>(...)`. |
| `Collectors.toList()` | 230 (100) | 10 (4) | Keep when a mutable list is needed (SPARQL models and DTO setters receive lists that are sometimes appended to). |
| `List.of` / `Set.of` / `Map.of` (9) | 237 (57) | 120 (14) | **Use** for constants and literals. They reject `null` elements and are immutable. |
| `Arrays.asList` | 201 (56) | 1 (1) | Legacy: prefer `List.of`, unless you need `set()` or `null` elements. |
| `record` (16) | 3 (3): `MinimalData`, `SPARQLLiteral`, a test `User` | 2 (2) | **Only** small internal immutable carriers. **Never** for SPARQL models, DTOs or `*Config` (reflection + ByteBuddy proxies need non-final classes with accessors; Jackson/Swagger DTOs are mutable POJOs here). |
| Lambdas, method references, streams (8) | 1,065 / 407 / 586 | many | **Use**. Avoid side effects in `forEach` that fill an outer list; use `map(...).toList()`. |
| try-with-resources (7) | 83 (42) | 12 (7) | **Use** for every `AutoCloseable` (Mongo cursors/sessions, streams, files). |
| `java.time` | 286 imports (183 files) | 23 (17) | **Use** `OffsetDateTime`, `Instant`, `LocalDate`. |
| `Date`, `Calendar`, `SimpleDateFormat` | 37 (7) | 31 (5): spectra/CSV parsers | **Avoid**; tolerated only where an external parser forces it. |
| `Optional` | 13 (9); as field 6; as parameter 0 | 0 | Return type only. Never as field/parameter/collection element, never `.get()` without a check; prefer `orElseThrow()`. |
| `Objects.requireNonNull` / `equals` | 103 / 61 | 4 / 0 | **Use** `requireNonNull(x, "message")` for constructor arguments. |
| `StringUtils` (commons-lang3) | 212 (90) | 13 (3) | Accepted. Plain `String.isBlank()` (11) is also fine (2 uses). |
| Text blocks `"""` (15) | **0** | 0 | Allowed by the compiler and recommended for multi-line SPARQL/JSON literals in tests; nobody uses them yet. |
| Switch expressions `case X ->` (14) | **0** | 0 | Allowed; use only if it removes fall-through or temp variables. Keep one style per file. |
| `instanceof` pattern matching (16) | **0** (legacy `if (x instanceof T) { T t = (T) x; ... }` is the norm) | 0 | Allowed and preferred in new code over instanceof + cast. |
| `sealed` / `permits` (17) | **0** | 0 | Do not introduce without a design reason. |
| `@Override` | 1,094 | 36 | Always. |
| `@Deprecated` | 101 (35): 18 with `since`, 20 with `forRemoval` | 2 | Use `@Deprecated(since = "x.y.z", forRemoval = true)` **and** a Javadoc `@deprecated` naming the replacement (see the `by_uris` GET endpoints). |

**Not available at release 17** (compile error): sequenced collections (`getFirst()`, `getLast()`, `reversed()`),
pattern matching in `switch`, record patterns, virtual threads, `Math.clamp`, unnamed variables `_`, string templates.

## 2. Codebase-specific constraints that override "modern Java"
- **SPARQL models** (`@SPARQLResource`): public class, public no-arg constructor, private fields with getter *and* setter,
  mutable `List` fields. The mapper and the lazy proxies (`SPARQLProxy`, ByteBuddy subclassing) rely on that. No `final`
  class, no record, no constructor injection.
- **DTOs**: mutable POJOs with Jackson (`@JsonProperty` snake_case) and Swagger annotations, converted with
  `fromModel`/`toModel`. Bean-validation annotations come from **`javax.validation`**.
- **Namespaces**: `javax.ws.rs`, `javax.inject`, `javax.validation`. Never `jakarta.*` (except `jakarta.json`).
- **Tests**: JUnit **4.13.2** (`org.junit.Test`, `@BeforeClass`, `@Rule`...), Mockito in 6 files, no JUnit 5, no AssertJ,
  no Hamcrest. Do not introduce them.
- **Exceptions**: API/DAO/logic methods declare `throws Exception` (norm). Typed business exceptions live in
  `org.opensilex.server.exceptions`.
- **Dependencies**: no Lombok (0 imports). Do not add libraries for something the JDK or commons-lang3 already covers.

## 3. Good practices that apply to every change (release 17)
1. **Null-safety**: validate input at the boundary (`@Valid`, `@NotNull`, `@ValidURI`, `@Min`), return empty collections
   instead of `null`, `Objects.requireNonNull` in constructors, compare `"literal".equals(x)` or `Objects.equals`.
2. **Immutability by default** for everything that is not a mapped model/DTO: `final` fields, `List.of`, records for pure
   value carriers, unmodifiable views when exposing internal collections.
3. **Resources**: try-with-resources; never rely on `finalize` or manual `close()` in `finally`.
4. **Exceptions**: catch the narrowest type that you can handle, keep the cause (`new XxxException(msg, e)`), log once at
   the boundary, never swallow. Multi-catch (`catch (A | B e)`) over duplicated blocks.
5. **Collections/streams**: do not mutate the result of `Stream.toList()`/`List.of`; do not use parallel streams (4 legacy
   uses) in request handling; pre-size `new ArrayList<>(n)` only when `n` is known and cheap.
6. **Strings**: no concatenation in loops (use `StringBuilder`/`String.join`); SPARQL is built with the Jena
   `SelectBuilder` and `SPARQLQueryHelper`, never by concatenating user input into query text.
7. **Time**: store and exchange `OffsetDateTime`/`Instant`/`LocalDate`; parse with `DateTimeFormatter`, not
   `SimpleDateFormat` (not thread-safe).
8. **Generics**: no raw types; `@SuppressWarnings("unchecked")` only on the smallest statement with a comment
   (`@SuppressWarnings("all")` exists only in the unbuilt `opensilex-dataverse` module: never use it).
9. **Concurrency**: no new mutable static state; `synchronized` only with a comment saying what it protects.
10. **Logging**: SLF4J placeholders, correct level, no secrets/tokens/passwords in messages.

## 4. Modernising: boundaries
- Apply modern forms **to the lines you write or change**. Do not sweep untouched code (CONTRIBUTING.md: reformatting and
  functional changes must not be mixed; reviewers read the diff).
- Behaviour-preserving checks before replacing an idiom: `Collectors.toList()` -> `.toList()` changes mutability;
  `Arrays.asList` -> `List.of` changes null-tolerance and `set()` support; `instanceof` + cast -> pattern is safe;
  anonymous class -> lambda is safe only for functional interfaces and changes `this` semantics.
- Match the surrounding file when a mixed style would be jarring, but never copy a LEGACY pattern listed in
  `naming-and-style.md` or the tables above.
- When SonarLint is run (see `lint.md`), rules such as S6201 (instanceof pattern), S6126 (text block) and S6204 (modifying
  a `Stream.toList()` result) are the Java 16/17-aware checks; the last one is a real bug, not a style hint.
