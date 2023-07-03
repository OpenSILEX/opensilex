# Specifications : Shared Resource Instances

<!-- TOC -->
* [Specifications : Shared Resource Instances](#specifications--shared-resource-instances)
  * [Needs](#needs)
  * [Solution](#solution)
  * [Technical specifications](#technical-specifications)
    * [Definitions](#definitions)
    * [Detailed explanations](#detailed-explanations)
      * [Configuration](#configuration)
      * [API](#api)
      * [Connexion to SRI](#connexion-to-sri)
      * [Front-end](#front-end)
  * [Limitations and prospects](#limitations-and-prospects)
<!-- TOC -->

- Developers :
  - Alexia Leroy
  - Valentin Rigolle (valentin.rigolle@inrae.fr)
- Date : 2023-06-01
- OpenSILEX version : 1.0.0-rc+7.3 (develop)

> Related documentation :
> 
> - [SRI configuration](../installation/configuration/shared-resource-instances.md)

## Needs

The goals of the feature is to allow for re-usability of declared resources, e.g. variables ; and to promote the sharing 
and re-use of resources rather than systematic declarations of said resources.

- Use case #1 : As a user, I want to browse a list of pre-declared resources that I might want to use on my instance
- Use case #2 : As a user, I want to re-use a pre-declared resource on my instance
- Use case #3 : As a user, I want to know which resources on my instance come from another source
- Use case #4 : As an administrator, I want to specify the sources my user can browse for pre-declared resources

## Solution

An OpenSILEX instance can be configured to have associated shared resource instances.

The search service of the variable API can be used to browse variables, entities, etc. from an SRI defined in the
instance configuration rather than the local instance. This is also true for the `getByUri` and `getListByUris`
services. A new service is added to copy a variable, entity etc. from an SRI to the local instance. The copied
resource has an attribute referring to the original source.

In the web application, the top field of the variable list page allows to change the source of browsed variables. By
default, the user will see the variables that are present on the local instance. They can choose to browse variables 
from any SRI defined in the instance configuration.

The local variables in the list have an icon indicating whether they come from a shared resource instance. On hovering
the icon, the name of the instance is displayed. Similarly, when browsing variables from an SRI, an icon will show
whether the variable has already been copied to the local instance.

The detail page of a local variable also displays the origin SRI, as well as the last time the variable was updated
from the SRI.

When browsing variables from an SRI, the user can choose variables to copy to the local instance. When doing so, the
components of the variables such as the entity, etc. are also copied on the local instance.

## Technical specifications

### Definitions

- **Shared Resource Instance (SRI)** : An OpenSILEX instance used to store resources that are meant to be re-use by 
  other instances. For example, they can be common variables for a specific domain. In the context of any OpenSILEX
  instance, the Shared Resource Instances are the instances defined under the `core.sharedResourceInstances` section
  of the configuration file. (See the [configuration documentation](../installation/configuration/shared-resource-instances.md)).
- **Copy** : Create a new resource on an instance from an origin resource in an SRI. The new resource will have the same
  fields and URI as the origin, and an additional field referring to the source SRI
  (`vocabulary:fromSharedResourceInstance`).

### Detailed explanations

#### Configuration

The SRI configurations are defined in the `CoreConfig` and `SharedResourceInstanceItem` interfaces. Methods for
accessing these configurations are defined in the injectable `CoreModule` class. They can be accessed either as
`SharedResourceInstanceItem` for `SharedResourceInstanceDTO`, the difference being that the DTO only has one label,
chosen according to the user language, whereas the item has a map of all labels depending on the language.

#### API

- `OntologyAPI` : the service `GET /ontology/shared_resource_instances` returns the list of SRI defined in the
  configurations as `SharedResourceInstanceDTO`.
- `VariableAPI` : the service `POST /variable/copy_from_shared_resource_instance` performs a copy of one or more
  variables from a specified SRI onto the local instance.

Several GET services take an optional `sharedResourceInstance` parameter to retrieve a resource from an SRI instead of
the local instance.

| API                 | Affected services                     |
|---------------------|---------------------------------------|
| `VariableAPI`       | `GET /`, `GET /{uri}`, `GET /by_uris` |
| `EntityAPI`         | `GET /`, `GET /by_uris`               |
| `InterestEntityAPI` | `GET /`, `GET /by_uris`               |
| `CahracteristicAPI` | `GET /`, `GET /by_uris`               |
| `MethodAPI`         | `GET /`, `GET /by_uris`               |
| `UnitAPI`           | `GET /`, `GET /by_uris`               |

#### Connexion to SRI

The connexion and queries to the SRI are managed by an object of type `SharedResourceInstanceService` which is
built using a `SharedResourceInstanceItem` and a user language. An instance of `SharedResourceInstanceService` can
query a single SRI in a specific language. It manages the user token, the namespace and expansion of the SRI's short
URIs, and allows to make arbitrary GET requests to the SRI.

The service exposes the following public methods :

- `get(WebTarget target, Map<String, String[]> queryParameters, JavaType responseType, boolean expandUris)` : performs
  a GET request on the `target` with the parameters `queryParameters`. The result is converted using Jackson into the
  specified `responseType` (the response is assumed to be a JSON). The `expandUris` arg allows to parse the JSON to
  expand all the short URIs.  This method is not intended to be used as-is, but is publicly exposed to allow for custom
  queries if necessary. If possible, you should use on of the other public methods.
- `search(String path, Map<String, String[]> parameters, Class<T> type)` : performs a request on `path` with the query
  parameters `parameters` and converts the result into a `ListWithPagination<T>`. This method allows to query the
  services that return a `PaginatedListResponse<T>`, such as the `search` services.
- `getByURI(String path, URI uri, Class<T> type)` : Performs a request on `path/{uri}` and converts the result into a
  `T`.
- `getListByURI(String path, String uriParam, Collection<URI> uriCollection, Class<T> type)` : performs a request on
  `path` with a query parameter defined with the key `uriParam` and the values `uriCollection`. The result is converted
  into a `ListWithPagination<T>`. This method allows to query the `GET /by_uris` services and is typically called with
  "uris" as `uriParam`.

#### Front-end

`SharedResourceInstanceSelector` is the component selector for SRIs.

Several selector components now take a URI of an SRI as a prop (`sharedResourceInstance`). When the SRI changes, these
selectors are refreshed with the list of resources coming from the new SRI.

- `EntitySelector`
- `InterestEntitySelector`
- `CharacteristicSelector`
- `MethodSelector`
- `UnitSelector`
- `SpeciesSelector`

`VariableList` is the page grouping all these selectors together, propagating the selected SRI from the SRI selector
to the other ones.

The variable details page takes a `sharedResourceInstance` query parameter to allow a variable only present in an SRI
to be viewed.

## Limitations and prospects

The following bullet points are notes from a meeting concerning the limitations and future developments on this feature.

- Manage the update of variables on the SRI. How can these changes impact the already copied variables ?
  - Need to store the last copy date of a variable
    - Compare the update date of a variable and the last 
  - URI of variables is expected NOT to change
  - Updates can be minor or major :
    - Minor changes (new annotation, document, etc.)
    - Major changes : update of the entity, etc.
    - Example of a complex case : a variable can be split in two more specific variables
- Allow for resource moderation on the SRI.
  - Non-administrator user should be able to submit variables to add to an SRI. These variables could be submitted to
    moderation before being accepted.
  - The moderator can make suggestions before accepting the variable (using annotations)
- When declaring a variable on the local instance, automatically search the SRI for similar variables. Show the user
  suggested variables from the URI to encourage re-use rather than re-declaration.
- Keep the "source" filter visible even if the filter window is closed on the variable list