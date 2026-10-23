# REST API, DTOs, credentials, errors

Applies to every `concept/api` package. Canonical example: `opensilex-core/src/main/java/org/opensilex/core/project/api/` (`ProjectAPI`, `ProjectDTO`, `ProjectCreationDTO`, `ProjectGetDTO`, `ProjectGetDetailDTO`).

## Contents
- API class skeleton
- Endpoint annotations
- Query parameter conventions
- DTO roles
- Response classes
- Errors and HTTP status
- Credentials
- Generated TypeScript client

## API class skeleton

```java
@Api(ProjectAPI.CREDENTIAL_PROJECT_GROUP_ID)
@Path("/core/projects")
@ApiCredentialGroup(
        groupId = ProjectAPI.CREDENTIAL_PROJECT_GROUP_ID,
        groupLabelKey = ProjectAPI.CREDENTIAL_PROJECT_GROUP_LABEL_KEY
)
public class ProjectAPI {
    public static final String CREDENTIAL_PROJECT_GROUP_ID = "Projects";
    public static final String CREDENTIAL_PROJECT_GROUP_LABEL_KEY = "credential-groups.projects";
    public static final String CREDENTIAL_PROJECT_MODIFICATION_ID = "project-modification";
    public static final String CREDENTIAL_PROJECT_MODIFICATION_LABEL_KEY = "credential.default.modification";
    public static final String CREDENTIAL_PROJECT_DELETE_ID = "project-delete";
    public static final String CREDENTIAL_PROJECT_DELETE_LABEL_KEY = "credential.default.delete";

    @CurrentUser AccountModel currentUser;      // authenticated caller, injected per request
    @Inject private SPARQLService sparql;       // injected service; DAOs are created with new XxxDAO(sparql)
    ...
}
```

Path convention: `/<module>/<plural-concept>` (`/core/projects`); single resource `@Path("{uri}")`; sub-actions are explicit segments (`by_uris`).

## Endpoint annotations

A write endpoint (POST/PUT/DELETE):

```java
@POST
@ApiOperation("Add a project")
@ApiProtected
@ApiCredential(credentialId = CREDENTIAL_PROJECT_MODIFICATION_ID,
               credentialLabelKey = CREDENTIAL_PROJECT_MODIFICATION_LABEL_KEY)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApiResponses(value = {
    @ApiResponse(code = 201, message = "A project is created", response = URI.class),
    @ApiResponse(code = 409, message = "A project with the same URI already exists", response = ErrorResponse.class)
})
public Response createProject(@ApiParam("Project description") @Valid ProjectCreationDTO dto) throws Exception { ... }
```

- `@ApiProtected` on every endpoint that is not deliberately public; it enforces authentication. Reads use `@ApiProtected` alone; writes add `@ApiCredential` so access can be granted per profile.
- Measured on 363 endpoints (core 296, security 49, brapi 10, faidare 6, graphql 2): `@ApiOperation` 100 %, `@ApiResponses` 98 %, `@Produces` 99 %, `@Consumes` 92 %, `@ApiProtected` 95 % (the 5 % are public endpoints such as login), `@ApiCredential` 30 % (write operations), `@Deprecated` 12 % (mostly GET `by_uris`).
- Dominant annotation order (139 + 56 endpoints): HTTP verb, `@Path`, `@ApiOperation`, `@ApiProtected`, `@ApiCredential`, `@Consumes`, `@Produces`, `@ApiResponses`; put `@Deprecated(since = "x.y.z", forRemoval = true)` first.
- `@ApiOperation("...")` text: an imperative sentence starting with a verb, no final period (353 of 362): `Get` 124, `Search` 50, `Delete` 36, `Update` 30, `Add` 25, `Create` 16, `Count` 10, `Import`/`Export`; use `notes = "..."` (22 uses) for the long explanation.
- `@Api(...)` takes a constant: `SecurityModule.REST_SECURITY_API_ID` in `opensilex-security`, the API class's own `CREDENTIAL_XXX_GROUP_ID` in `opensilex-core`, `"BRAPI"` in BrAPI.
- Namespaces are `javax.ws.rs`, `javax.validation`, `javax.inject` (never `jakarta.*`).
- Declare `@ApiResponses` for each documented status; Swagger output feeds the generated TypeScript client and the API docs.
- Validate input with `@Valid` on the body DTO and `@NotNull` / `@Min` on path and query parameters.
- API methods are declared `throws Exception`: let unexpected errors propagate to the global mapper (below), catch only the cases you can translate into a specific response.

## Query parameter conventions

snake_case names (572 vs 30 camelCase outside BrAPI/FAIDARE, which follow their external specs and are camelCase 31 vs 18) and these defaults, exactly as in `ProjectAPI.searchProjects`:

```java
@DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
@QueryParam("page") @DefaultValue("0") @Min(0) int page,
@QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
```

Free-text filters are documented as "regex pattern for ..." and applied with `SPARQLQueryHelper.regexFilter`. Multi-URI lookups use `POST .../by_uris` with a JSON body; the `GET .../by_uris?uris=` variants are deprecated (long URL lists) - do not add new ones.

## DTO roles

- `XxxDTO` (abstract): fields shared by creation and read.
- `XxxCreationDTO extends XxxDTO`: used for POST/PUT, owns `newModel()` that builds the model. Related objects are accepted as URIs and turned into stub models carrying only the URI (`ProjectCreationDTO`).
- `XxxGetDTO`: has a static `fromModel(model)`; relations are exposed as URI lists via `SPARQLResourceModel.getUriList(...)`.
- `XxxGetDetailDTO`: richer read view (e.g. embeds the publisher as `UserGetDTO`).
- Newer code extends `ResourceDTO<T>` / `NamedResourceDTO<T>` from `opensilex-sparql/.../response/`, which is what the test helper `testBasicCRUDAsAdmin` expects. Prefer them for new DTOs.

Keep DTO <-> model conversion inside the DTO classes, never in the DAO (`dal` must not know DTOs). Every field you add to a model must be added to the DTO conversion in both directions, otherwise updates silently erase it (see `sparql-model-dao.md`).

Measured on the 255 DTO files: plain `XxxDTO` 124 (shared base), `XxxGetDTO` 44, `XxxCreationDTO` 36, `XxxUpdateDTO` 26, `XxxDetailsDTO` 13 (the usual name of the detailed read view; `GetDetailDTO` only 2), `XxxListDTO` 4, `XxxSearchDTO` 3. Conversion vocabulary: instance `fromModel(model)` / `toModel(model)`, `newModel()` (calls the abstract `newModelInstance()` then `toModel`), static `getDTOFromModel(model)`. `ResourceDTO<T>` carries `uri`, `rdf_type`, `rdf_type_name`, `publication_date`, `last_updated_date`; `NamedResourceDTO<T>` adds `name`. Mandatory input fields are marked on the **getter** of the creation DTO with `@Required` (`org.opensilex.server.rest.validation.Required`) plus `@ApiModelProperty(required = true)`; the update DTO makes `getUri()` `@NotNull`. Field JSON names are snake_case through `@JsonProperty`; the `@ApiModelProperty` documentation (value + example) sits on getters. Compile-checked example: `assets/templates/api/Widget*DTO.java`.

## Response classes

| Use | Class |
|---|---|
| 201 with the created URI | `new CreatedUriResponse(uri).getResponse()` (`org.opensilex.sparql.response`) |
| update/delete returning the URI | `new ObjectUriResponse(Response.Status.OK, uri).getResponse()` |
| one object | `new SingleObjectResponse<>(dto).getResponse()` |
| paginated list | `new PaginatedListResponse<>(listWithPaginationOfDtos).getResponse()` |
| expected error | `new ErrorResponse(Status, "title", "message").getResponse()` |

Classes live in `org.opensilex.server.response` unless noted.

## Errors and HTTP status

A global `ExceptionJsonMapper` (`opensilex-security/.../authentication/ExceptionJsonMapper.java`) converts exceptions to JSON `ErrorResponse`:
- `WebApplicationException` subclasses keep their status. The ready-made ones are in `org.opensilex.server.exceptions`: `NotFoundException`, `NotFoundURIException`, `ConflictException`, `BadRequestException`, `ForbiddenException`, `UnauthorizedException`, `InvalidValueException`, `ServiceUnavailableException`, ... Throw these from BLL/DAO code for expected business failures; the API layer does not need a try/catch.
- `IllegalArgumentException` -> 400, `SPARQLInvalidUriListException` -> 404.
- Anything else -> 500 and is logged with the request path. If a failure should be a 4xx, throw the right exception type rather than letting it become a 500.
- Catch specific storage exceptions only to translate them, like `createProject` turning `SPARQLAlreadyExistingUriException` into a 409 `ErrorResponse`.

## Credentials

- Group the endpoints of a concept with `@ApiCredentialGroup`; give each protected write action an `@ApiCredential(credentialId, credentialLabelKey)`.
- Label keys are i18n keys resolved by the front. Reuse `credential.default.modification` / `credential.default.delete` for standard write/delete, as `ProjectAPI` does; add new keys to the front language files only for genuinely new actions.
- New credentials must be granted to profiles for non-admin users to reach them; an endpoint that works as admin in tests can still be forbidden for real users.

## Generated TypeScript client

The Vue front calls the API through a client generated from the Swagger spec at build time (`**/front/src/lib`, git-ignored; plugin in `opensilex-swagger-codegen-maven-plugin`). Client method names derive from the Java API method names and `@ApiOperation`/parameter definitions, so renaming a method, a path or a parameter is a breaking change for any `.vue`/`.ts` caller. When you change a signature, search the front (`opensilex-front/front`, module `front/` folders) for usages and tell the user what must change on the front side.
