# Technical documentation : [`architecture`] `opensilex-security` module

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2026-09-29 | Arnaud Charleroy | BUILD-SNAPSHOT    | Document creation |

> ⚠️ _WARNING_ : written from the `develop` branch (commit `6725c2912`) by reading the code; nothing was built or run.
> Feature-level documents already exist for [accounts](./Accounts/account_API_service.md),
> [persons](./Persons/person_API_service.md), [GDPR](./Persons/GDPR.md) and
> [authentication](./security-and-authentication.md). The last one still refers to the old package `org.opensilex.rest`
> and to token claims (`experiments_list`, `experiments_exceed_limit`) that nothing in the code sets any more.

## Table of contents

<!-- TOC -->
* [Technical documentation : [`architecture`] `opensilex-security` module](#technical-documentation--architecture-opensilex-security-module)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Solution](#solution)
    * [Module identity](#module-identity)
    * [Package map](#package-map)
    * [How a request is authenticated and authorised](#how-a-request-is-authenticated-and-authorised)
    * [Annotations](#annotations)
    * [Credential model](#credential-model)
    * [Tokens](#tokens)
    * [Extension points](#extension-points)
    * [Resources](#resources)
  * [Tests](#tests)
  * [Limitations and improvements](#limitations-and-improvements)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **Account** : a login (`foaf:OnlineAccount`), the `Principal` of a request. **Person** : a human (`foaf:Person`).
- **Profile** : a list of credential identifiers. **Group** : a set of (account, profile) associations.
- **Credential** : a string identifier checked against the credentials of the caller's token.
- **Credential group** : a label used to gather credentials in the user interface.

## Solution

### Module identity

- Module class: [SecurityModule](../../../../../../opensilex-security/src/main/java/org/opensilex/security/SecurityModule.java),
  config id `security`, configuration interface `SecurityConfig`.
- Implements `APIExtension`, `LoginExtension` and `SPARQLExtension`.
- Maven dependencies: `opensilex-main` and `opensilex-sparql`. The module imports nothing from `core`, `nosql` or `fs`.
- `SecurityConfig` keys: `authentication` (the service class), `email` (`enable`, `simulateSending`, `sender`, and
  `smtp` with `host`, `port`, `userId`, `userPassword`), `allowMultiConnection`, `openID` (`enable`, `providerURI`,
  `redirectURI`, `clientID`, `clientSecret`, `connectionTitle`), `saml` (`enable`, `samlProxyLoginURI`,
  `samlLandingPageURI`, `attributes`, `connectionTitle`) and `gdprPdfPathsByLanguages`.
- `getPackagesToScan()` adds `io.swagger.jaxrs.listing` (which serves `swagger.json`) and this module's `authentication`,
  `filters` and `injection` packages; that is how the filters and the global exception mapper get registered.
- `bindServices` binds `CurrentUserFactory` (a request-scoped `AccountModel`) and `CurrentUserResolver`.
- The super administrator is created by `inMemoryInitialization()` (the `SPARQLExtension` hook) and the default profile by
  `install()`. The guest account, profile and group are created by `createDefaultGuestGroupUserProfile()`, called by
  `FrontModule.install` when `front.connectAsGuest` is true and by `opensilex user add-guest`.

### Package map

Root package `org.opensilex.security`; 76 Java files in `src/main/java`.

| Package                         | Files | Role                                                                                         |
|---------------------------------|-------|----------------------------------------------------------------------------------------------|
| `security` (root)               | 7     | `SecurityModule`, `SecurityConfig`, `OpenIDConfig`, `SAMLConfig`, `SAMLAttributesConfig`, `EmailConfig`, `SMTPConfig` |
| `account`                       | 1 + 5 api + 2 dal | `AccountAPI` (`/security/accounts`), `AccountDAO`, `AccountModel` (graph `user`)       |
| `person`                        | 4 api + 2 dal | `PersonAPI` (`/security/persons`, ORCID and GDPR endpoints), `ORCIDClient`, `PersonModel` |
| `group`                         | 7 api + 3 dal | `GroupAPI`, `GroupModel`, `GroupUserProfileModel` (links an account to a profile)         |
| `profile`                       | 4 api + 2 dal | `ProfileAPI`, `ProfileModel` (list of credential strings)                                 |
| `user`                          | 9 api | The legacy `UserAPI` (`/security/users`), deprecated; no `dal` package                        |
| `authentication`                | 10 + 5 api + 1 dal | Annotations, `AuthenticationService`, `AuthenticationAPI`, `AuthenticationDAO`, `SecurityOntology` |
| `authentication/filters`        | 3     | `AuthenticationFilter`, `CredentialFilter` and one more filter                                 |
| `authentication/injection`      | 3     | `@CurrentUser`, `CurrentUserFactory`, `CurrentUserResolver`                                    |
| `credentials`                   | 1 + 3 | `ExtraCredentialService` and the credential configuration interfaces                          |
| `email`                         | 1     | `EmailService`                                                                               |
| `cli`                           | 1     | `UserCommands` (`opensilex user add`, `add-guest`)                                            |
| `extensions`, `ontology`        | 1 each | `LoginExtension`; `OesoSecurity`                                                              |

Concepts with both `api` and `dal`: account, person, group, profile. `authentication` has an API and a small DAO. The
`user` package is a legacy REST facade whose DAO and model are `account/dal/AccountDAO` and `AccountModel`; its DTOs are
still imported by `opensilex-core`.

### How a request is authenticated and authorised

Three components, in this order:

1. **`AuthenticationFilter`** is a `@Provider` `@PreMatching` filter with priority `AUTHENTICATION`. It reads the
   `Authorization` header (`Bearer` scheme), verifies the JWT and takes the account URI from the `sub` claim. The account is
   looked up in an in-memory registry of logged-in users.
   - No token, or a token that does not verify: the caller becomes the anonymous account.
   - A valid token whose account is not in the registry (logged out, or server restarted): `ForbiddenException`.
   - Otherwise a `SecurityContext` is installed whose principal is the `AccountModel`.
2. **`CredentialFilter`** has priority `AUTHORIZATION`. For a method annotated `@ApiProtected`:
   an anonymous caller gets 401; an administrator passes; `adminOnly = true` rejects everyone else; otherwise the
   credential id of the method (from `@ApiCredential`) is looked up in the `credentials_list` claim of the token, and a
   miss gives `ForbiddenException`. An `@ApiProtected` method without `@ApiCredential` (or with `hide = true`) only
   requires a logged-in caller.
3. **`@CurrentUser`** fields (`AccountModel`) are resolved by `CurrentUserFactory` from the security context.

Methods without `@ApiProtected` are public.

### Annotations

| Annotation             | Target | Purpose                                                                                     |
|------------------------|--------|---------------------------------------------------------------------------------------------|
| `@ApiProtected`        | method | Requires authentication; `adminOnly` restricts to administrators; also adds the Swagger header parameter |
| `@ApiCredential`       | method | Declares the credential id and label key, its group id and label key, and `hide`             |
| `@ApiCredentialGroup`  | type   | Declares the group id and label key of the credentials of an API class                       |
| `@CurrentUser`         | type, field | Injects the authenticated `AccountModel`                                                |

Credential constants are declared in the API class itself, for example (from `ExperimentAPI` in `opensilex-core`):

```java
public static final String CREDENTIAL_EXPERIMENT_GROUP_ID = "Experiments";
public static final String CREDENTIAL_EXPERIMENT_MODIFICATION_ID = "experiment-modification";
public static final String CREDENTIAL_EXPERIMENT_MODIFICATION_LABEL_KEY = "credential.default.modification";
```

### Credential model

A credential is a string id. A profile stores a list of ids. The set of known ids is built once, lazily, by
`AuthenticationDAO.buildCredentials` from two sources: every `@ApiCredential` method found by classpath scanning, and
`src/main/resources/credentials/credentials.yml` for credentials that do not belong to an API method (menu
entries). A user's credentials are the union over the profiles reachable through their `GroupUserProfileModel` links; they
are written into the token at login. `GET /security/credentials` lists the credential groups and is not protected.

### Tokens

- Library: java-jwt. Algorithm RSA with SHA-512, over a 2048-bit key pair generated **in memory** when
  `AuthenticationService` is constructed.
- Validity: 45 minutes. Passwords are hashed with BCrypt, cost 12.
- Claims: `iss`, `sub` (account URI), `iat`, `exp`, `given_name`, `family_name`, `email`, `name`, `locale`, `is_admin`
  and `credentials_list` (omitted for administrators).
- Login is stateful: each login registers the account in a concurrent map and starts a thread that removes it after the
  token duration. A still-valid token of a logged-out account is therefore answered with 403, not 401.
- `AuthenticationAPI` (`/security`) exposes `authenticate`, `renew-token`, `forgot-password`, `renew-password`, `logout`,
  `credentials`, `openid` and `saml`. OpenID Connect and SAML authentication are implemented in
  `AuthenticationService`.

### Extension points

- Defines `LoginExtension` (`login(user, JWTCreator.Builder)` and `logout(user)`, both default no-ops). It is called by
  `AuthenticationService` at login and logout. `SecurityModule` implements it without overriding anything.
- Defines `ModuleWithNosqlEntityLinkedToAccount`, a plain interface implemented by `CoreModule`; `AccountDAO` uses it to
  refuse the deletion of an account that published MongoDB data, without depending on `opensilex-nosql`.
- Implements `SPARQLExtension` (creates the super administrator; registers no ontology file) and `APIExtension`.

### Resources

`credentials/credentials.yml` and `email/forgot-password.mustache`. There are no configuration profiles, no i18n
files and no ontology file: the `os-sec` vocabulary is defined in the `oeso-core.owl` shipped by `opensilex-core`. Two
constant classes describe the same namespace (`http://www.opensilex.org/security#`): `SecurityOntology` and `OesoSecurity`.

The security screens (accounts, persons, groups, profiles) are in `opensilex-front`; `opensilex-security/front` only
contains an `index.ts` that binds the generated TypeScript client.

## Tests

`AbstractSecurityIntegrationTest` (829 lines, in `src/test`) extends the main `AbstractIntegrationTest`, creates a super
administrator, provides `UserCall` and `UserCallBuilder`, and is reused by `core`, `front` and `phis` tests through the
security `test-jar`. Test classes are `*APITest` and `*DAOTest` (both integration tests) and plain JUnit tests
(`UserCommandsTest`, `ORCIDClientTest`, `OrcidRecordDTOTest`). There is no test of `GroupAPI` or of the group DAO in this
module (`GroupAPITest` lives in the `core` tests), and none that targets the filters or `AuthenticationService` directly.

## Limitations and improvements

- `AccountAPI.CREDENTIAL_ACCOUNT_DELETE_ID` has the same value as `CREDENTIAL_ACCOUNT_MODIFICATION_ID`
  (`account-modification`): deleting an account cannot be granted separately from modifying it.
- The key pair is regenerated at each start, so tokens do not survive a restart and cannot be shared between two
  instances.
- With `allowMultiConnection` true, `removeUserByURI` does nothing, so logout, timeout removal and re-login do not
  remove the registry entries.
- `SecurityModule` defines default passwords for the administrator and guest accounts (`DEFAULT_SUPER_ADMIN_PASSWORD`,
  `DEFAULT_GUEST_PASSWORD`); they must be changed on any installation that is reachable by others.
- `EmailService` sets `mail.smtp.ssl.trust` to `stmp.gmail.com`, a misspelling of the host name.
- The `user` package is deprecated but still supplies DTOs used by `opensilex-core`. `UserCreationWithExistantPersonDTO.java`
  is an empty file.
- `AuthenticationDAO.checkUserAccess` has no caller.

## Documentation

- [security-and-authentication.md](./security-and-authentication.md), [Accounts](./Accounts/account_API_service.md),
  [Persons](./Persons/person_API_service.md), [GDPR](./Persons/GDPR.md) : feature documents.
- [../architecture/modules-overview.md](../architecture/modules-overview.md) : extension points and load order.
- [../architecture/java-naming-conventions.md](../architecture/java-naming-conventions.md) : naming rules.
