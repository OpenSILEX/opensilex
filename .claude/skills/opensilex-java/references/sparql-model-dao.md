# SPARQL models and DAOs

Applies to `opensilex-sparql` (the mapping machinery) and every `concept/dal` package. Canonical small example: `opensilex-core/src/main/java/org/opensilex/core/project/dal/` (`ProjectModel`, `ProjectDAO`).

## Contents
- Model anatomy
- Annotation attributes
- URI generation
- Update semantics (the main source of silent data loss)
- Lazy proxies and relation fetching
- DAO pattern and `SPARQLService` entry points
- Search filters, pagination, schema fetching
- Transactions and multi-store logic
- Adding a property or a class to the ontology

## Model anatomy

```java
@SPARQLResource(
        ontology = Oeso.class,        // vocabulary class holding the RDF type
        resource = "Project",         // local name of the rdf:type
        graph = ProjectModel.GRAPH,   // named graph, a "project" constant per concept
        prefix = "prj"                // prefix used when generating instance URIs
)
public class ProjectModel extends SPARQLNamedResourceModel<ProjectModel>
        implements ClassURIGenerator<ProjectModel> {

    public static final String GRAPH = "project";

    @SPARQLProperty(ontology = Oeso.class, property = "startDate", required = true)
    private LocalDate startDate;
    public static final String START_DATE_FIELD = "startDate";   // field-name constant, used by DAO queries
    ...
}
```

- Extend `SPARQLNamedResourceModel<T>` for anything with a name; it brings `uri`, `rdfType`, `name` (mapped to `rdfs:label`), `publisher`, `publicationDate`, `lastUpdateDate`. Use `SPARQLResourceModel` for nameless resources.
- Every mapped field needs a getter and setter (the mapper and the proxies go through accessors).
- Relations to other models are typed fields (`List<PersonModel> coordinators`) - they are stored as URIs and hydrated by the mapper.
- Define one `public static final String X_FIELD = "x"` per field and use it everywhere a field name is needed (24 of the 42 `@SPARQLResource` classes do; the constant sits right after its field; `lint.py` reports the missing ones as info). Models extend `SPARQLResourceModel` (12), `SPARQLNamedResourceModel` (12), `SPARQLTreeModel` (3, parent/children hierarchies) or a domain base class (`BaseVariableModel`). Fields may be private or package-private (`DeviceModel`), but getter and setter are always required.

## Annotation attributes (from `opensilex-sparql/.../annotations`)

`@SPARQLResource`: `ontology`, `resource`, `uriGenerator` (default `DefaultURIGenerator`), `graph`, `prefix`, `ignoreValidation`, `allowBlankNode`, `handleCustomProperties`.

`@SPARQLProperty`: `ontology`, `property`, `required`, `inverse`, `ignoreUpdateIfNull`, `cascadeDelete`, `autoUpdate`, `useDefaultGraph` (true: the object lives in its own default graph; false: it is stored in the subject's graph).

The package also holds `@SPARQLIgnore`, `@SPARQLManualLoading`, `@SPARQLResourceURI`, `@SPARQLTypeRDF`, `@SPARQLTypeRDFLabel` - read their source before using one; do not guess semantics.

## URI generation

`ClassURIGenerator<T>.getInstancePathSegments(instance)` returns the path segments used to build the instance URI under the class prefix. `ProjectModel` uses the shortname, falling back to the name. Default (`SPARQLNamedResourceModel`) is the name. URIs get normalised and de-duplicated by the generator, so do not build URIs by string concatenation in business code.

## Update semantics (the main source of silent data loss)

`sparql.update(model)` rewrites the resource from the model you pass. A field that is `null` is treated as "no value" and the stored triples are removed, unless the property is declared `ignoreUpdateIfNull = true` (as `name` is).

Consequences:
- An update endpoint that builds a model from a DTO (`dto.newModel()`) wipes every persisted field the DTO does not carry. When you add a model field, add it to the DTO and to `newModel()`/`fromModel()` in the same change.
- To protect data the CRUD flow does not own, use `ignoreUpdateIfNull = true`. `ProjectModel.inverseRelatedProjects` is the worked example: `inverse = true` + `ignoreUpdateIfNull = true` keeps other projects from losing their link to this one on update, and the field is deliberately always null.
- Be explicit when you rely on cascade: `cascadeDelete` removes the related objects with the parent.

## Lazy proxies and relation fetching

Related models come back as lazy proxies generated with ByteBuddy (`opensilex-sparql/.../mapping/SPARQLProxy`). Touching a proxy's data can trigger another SPARQL query, which turns a list endpoint into N+1 queries. Rules of thumb:
- In list/search paths, tell the service which relations to fetch together with the main query using a `SparqlSchema` (see below) instead of walking proxies afterwards.
- Do not pass proxies across threads, caches or transaction boundaries and expect them to still load; resolve what you need first.
- If you only need the URIs of relations, use `SPARQLResourceModel.getUriList(...)` (what `ProjectGetDTO.fromModel` does) and avoid dereferencing.

## DAO pattern and `SPARQLService` entry points

A DAO is a plain class holding a `SPARQLService`:

```java
public class ProjectDAO {
    protected final SPARQLService sparql;
    public ProjectDAO(SPARQLService sparql) { this.sparql = sparql; }

    public ProjectModel create(ProjectModel instance) throws Exception { sparql.create(instance); return instance; }
    public ProjectModel update(ProjectModel instance, AccountModel user) throws Exception { sparql.update(instance); return instance; }
    public void delete(URI uri, AccountModel user) throws Exception { sparql.delete(ProjectModel.class, uri); }
    public ProjectModel get(URI uri, AccountModel user) throws Exception {
        return sparql.getByURI(ProjectModel.class, uri, user.getLanguage());
    }
}
```

Main `SPARQLService` operations: `create` (throws `SPARQLAlreadyExistingUriException` if the URI exists; has batch and `checkUriExist` variants), `update`, `delete`, `getByURI`, `getListByURIs`, `search`, `searchURIs`, `searchWithPagination`, `searchWithPaginationUsingSchema`, `searchUsingSchema`, `searchAsStream`, `count`, `uriExists`. Many have overloads taking a `Node graph`; use the model-class overloads unless you really need a specific graph.

Pass the `AccountModel user` into DAO methods even when the plain implementation ignores it: the language (`user.getLanguage()`) and, in richer DAOs such as `FacilityDAO`/`FacilityLogic`, the access rights depend on it.

Measured on the 37 `*DAO` classes: 32 extend nothing (plain objects), 5 are `final class`, 19 take only `SPARQLService` (Mongo-backed ones add `MongoDBService` and `FileStorageService`), 315 of 414 public methods declare `throws Exception`. Method-name families: `get*` 143, `search*` 55, `create` 51, `update` 41, `delete` 38, `count*` 12, `is*`/`exists*` 11, `check*` 9, `validate*` 6. A compile-checked DAO, model and search filter to copy: `assets/templates/dal/`.

## Search filters, pagination, schema fetching

- Filtering goes through a `ThrowingConsumer<SelectBuilder, Exception>` callback that adds filters to the query; build expressions with `SPARQLQueryHelper` (`regexFilter`, `or`, `dateRange`, `intervalDateRange`, ...) using the `*_FIELD` constants. Null filter arguments return null expressions, so guard with `if (expr != null)` as `ProjectDAO.search` does.
- Pagination is 0-based `page` + `pageSize`, ordering is `List<OrderBy>`; the result type is `ListWithPagination<T>`, converted to DTOs with `resultList.convert(Dto.class, Dto::fromModel)` in the API layer.
- For 3+ criteria, create `XxxSearchFilter extends org.opensilex.sparql.service.SearchFilter` (base has `includedUris`, `rdfTypes`, `orderByList`, `page`, `pageSize` (default 20), `lang`; subclasses use **fluent setters** that `return this`, see `OrganizationSearchFilter`) and pass it as one argument. MongoDB-backed concepts extend `MongoSearchFilter` (plain setters, e.g. `LocationObservationSearchFilter`). Search filters live in `dal`; `GermplasmSearchFilter` sits in `api` and is one of the 11 historical `dal` -> `api` violations: do not copy it.
- Eager relation fetching: build a `SparqlSchema` with a `SparqlSchemaRootNode` and `SparqlSchemaSimpleNode(RelatedModel.class, Model.RELATION_FIELD)` children, then call `searchWithPaginationUsingSchema` (see `ProjectDAO.getSparqlSchema`). Package: `org.opensilex.sparql.service.schemaQuery`.

## Transactions and multi-store logic

`SPARQLService` exposes `startTransaction()`, `commitTransaction()`, `rollbackTransaction()` / `rollbackTransaction(Exception)` and `hasActiveTransaction()`. When a business operation touches both the triplestore and MongoDB (facility locations, for example), keep it in a `bll` class that owns the sequencing and rollback, passing a Mongo `ClientSession` where needed - `FacilityLogic.create/update/delete` is the model to follow. Do not scatter half-transactions across API methods.

## Adding a property or a class to the ontology

A mapped property must exist in the ontology: add the term to the OWL file under `opensilex-core/src/main/resources/ontologies/` (e.g. `oeso-core.owl`) and expose a constant in `org.opensilex.core.ontology.Oeso` if code references it. The test profile enables SHACL validation (`enableSHACL: true` in `opensilex-main/src/main/resources/config/test/opensilex.yml`), so a model that does not match its ontology definition can fail on create in tests even when the Java compiles. Existing databases need a migration when the change affects stored data - see `modules-migrations.md`.
