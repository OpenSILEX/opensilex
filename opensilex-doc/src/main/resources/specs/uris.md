******
* This document follows the Specification Guidelines draft (as of 2023-05-22)
* Author : valentin.rigolle@inrae.fr
* Date : 2023-05-22
******

> WARNING : This document is incomplete ! You can help by expanding it.
> 
> Currently covered topics :
> 
> - Long & short forms of URIs
> 
> Missing topics :
> 
> - URI generation
> - URI label management
> - Other ?

# Specifications : URI Management

- Date : 2023-05-22
- Referent persons :
  - Valentin Rigolle (valentin.rigolle@inrae.fr)
  - Anne Tireau (anne.tireau@inrae.fr)
- OpenSILEX version : 1.0.0-rc+7.2 (develop)

## Needs

URIs are used in OpenSILEX to identify resources. When creating a resource, a user may want to use a specific URI for
it, or let OpenSILEX generate one.

- Use case #1 : As a user, when referring to a URI, I want to be able to use a shortened version : instead of writing 
  `http://www.opensilex.org/vocabulary/oeso#Experiment`, I want to write `vocabulary:Experiment` and expect the same
  behaviour from the application
- Use case #2 : As a user, if I try to refer to a URI in its shortened form, but the prefix is not recognized, I want
  to be alerted as this can be a mistake. For example, if I write `phenome:id/variable/entity.canopy` but the `phenome`
  prefix is not recognized by OpenSILEX, I want the application to warn me that I forgot to put the namespace in the
  triple store.

## Solution

OpenSILEX defines a **prefix mapping**, matching strings called "prefixes" to URIs called "namespaces". Using this
mapping, a URI can have a "short" or "long" form.

Example mapping :

| Prefix     | Namespace                                 |
|------------|-------------------------------------------|
| vocabulary | http://www.opensilex.org/vocabulary/oeso# |
| foaf       | http://xmlns.com/foaf/0.1/                |

Using this mapping, the following URIs are considered the same :

- `vocabulary:Experiment` and `http://www.opensilex.org/vocabulary/oeso#Experiment`
- `foaf:Person` and `http://xmlns.com/foaf/0.1/Person`

However, `phenome:id/variable/entity.canopy` is not recognized by the mapping. It is possible to create a resource using
this URI, however a warning will be sent by OpenSILEX to alter that it may be a mistake.

The prefix mapping is constructed by OpenSILEX from the following sources :

- The `baseUriAlias` and `baseUri` from the config file are used as a prefix and namespace respectively
- Each resource type defined as a `@SPARQLResource` in the API contains a graph and prefix: for each graph name, a
  mapping is created using the prefix `{baseUriAlias}-{prefix}` and the namespace `{baseUri}set/{graph}` 
- Each module can define its own prefixes and namespaces
- Additional mappings can be defined in the `customPrefixes` field of the configuration file
- Mappings defined in the triple store are fetched by OpenSILEX to be added to the application mapping

## Technical specifications

### Definitions

See the URI syntax specification for URI-specific vocabulary :
https://en.wikipedia.org/wiki/Uniform_Resource_Identifier#Syntax.

- **Prefix mapping** : a mapping which associates strings called "prefixes" to URIs called "namespaces" in a one-to-one
  relationship. A prefix mapping is used to define equivalences between URIs. In the context of a prefix mapping, a
  URI can have a "short" or "long" form. Both forms of these URIs are equivalent and interchangeable.
- **Prefix** : a string containing only alphanumerical characters registered as a key in a prefix mapping. A prefix can
  be used as a URI _scheme_.
- **Namespace** : a URI defined as a value in a prefix mapping. Usually, a namespace has an empty _fragment_ (e.g.
  `http://www.opensilex.org/vocabulary/oeso#`)
- **Short form** : a shortened version of a URI in the context of a prefix mapping, i.e. **the _scheme_ of the URI matches
  one of the prefixes in the mapping**. If we take as an example the prefix mapping defined in the [Solution](#solution)
  section, `vocabulary:Experiment` is a URI in a short form. Its extended form is
  `http://www.opensilex.org/vocabulary/oeso#Experiment`.
- **Long form** (or **extended form**) : an extended version of a URI in the context of a prefix mapping, i.e. **the URI 
  starts with one of the namespaces in the mapping**. From the previous example, 
  `http://www.opensilex.org/vocabulary/oeso#Experiment` is a URI in a long form. Its short form is 
  `vocabulary:Experiment`.
- **Recognized URI** : a URI is "recognized" or "known" by a prefix mapping if it can be matched as either a short or
  a long form in the mapping, i.e. either the _scheme_ is a prefix or the URI starts with a namespace from the mapping.

> From the definitions, it is clear that the notions of "short" and "long" forms for URIs only make sense in the context
> of a prefix mapping. If a URI is not recognized by a prefix mapping, it cannot be characterized as either "short" or
> "long". That's why we cannot detect if a user has entered a "wrong" URI (a URI in a "short" form where the user forgot
> to add the prefix in the mapping), we can only guess if that is the case.

### Detailed explanations

#### URI prefix mapping

The transformation of URIs is solely handled by the API. The triple store does store a prefix mapping, but uses it only
for user convenience (query results are displayed in short forms, entering a URI in a short form in the query editor of
the workbench will automatically import the mapping, etc.).

The prefix mapping is managed by a `PrefixMapping` object generated by `SPARQLService` and stored in `URIDeserializer`.
It is constructed using static methods, like `SPARQLService#addPrefix`.

Upon startup of the instance, the mapping will be filled by multiple sources :

- A module class can define its own mappings. For example, `CoreModule` adds prefixes for the Oeso, Oeev and Time
  ontologies.
- The `SPARQLServiceFactory#startup` method generates prefixes from the `baseUri` and `baseUriAlias` parameters in the
  configuration.
  - `{baseUriAlias}` is mapped to `{baseUri}`
  - For each `@SPARQLResource` annotated class (which defines a `graph` and `prefix`), a prefix 
    `{baseUriAlias}-{prefix}` is mapped to `{baseUri}set/{graph}`.
  - The prefixes and namespaces defined in the `customPrefixes` are added to the mapping
- The `RDF4JServiceFactory#startup` method generates additional prefixes for an RDF4J implementation of the
  SPARQL service. In that case, the prefixes and namespaces of the triple store are fetched and added to the mapping.

#### Wrong URI detection

As explained in the [Definitions](#definitions) section, there is no way to detect if a URI is wrong, i.e. if the user
tried to enter a short form but forgot to add the prefix to the mapping. However, we can try to guess and provide a
warning if we believe that there may be an error.

We consider that a URI is potentially wrong **if it is not recognized by the prefix mapping AND it has no _authority_
part**. In that case, we accept the URI but send a warning to the user.

The reasoning behind this is that short forms often have no _authority_ part (most of them are in the form 
`scheme:path`), and that long forms almost always have one (most long forms start with `http://` or `https://`).

The method `SPARQLService#hasKnownPrefix` is used to check whether the given URI is known by the mapping. It simply
checks if the short and long forms of the URIs using the mapping are different.

### Tests

- In `SPARQLServiceTest` :
  - `testPrefixInRepositoryExists` checks that a prefix only defined in the triple store is correctly added to the 
    mapping.
  - `testCreateWithPrefixInRepository` checks that when creating a resource using a URI only defined in the triple store
    the prefix is correctly expanded.