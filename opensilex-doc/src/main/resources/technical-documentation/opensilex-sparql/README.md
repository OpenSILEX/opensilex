# Technical documentation : [`sparql`] Documentation index

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2026-09-11 | Arnaud Charleroy | BUILD-SNAPSHOT    | Document creation |
| 2026-09-13 | Arnaud Charleroy | BUILD-SNAPSHOT    | Added the related-documents index; corrected the sparql-property-annotation row |

## Table of contents

<!-- TOC -->
- [What lives here](#what-lives-here)
- [Start here](#start-here)
- [Document index](#document-index)
  - [Overview](#overview)
  - [The ORM, subsystem by subsystem](#the-orm-subsystem-by-subsystem)
  - [Cross-cutting and pre-existing notes](#cross-cutting-and-pre-existing-notes)
  - [Related documents outside this directory](#related-documents-outside-this-directory)
- [Conventions used in these documents](#conventions-used-in-these-documents)
- [Rendering the diagrams](#rendering-the-diagrams)
<!-- TOC -->

## What lives here

This directory documents the `opensilex-sparql` Maven module: the hand-written object/RDF mapper
that turns an annotated Java class into a readable, writable and searchable RDF resource, plus
everything built directly on it — the runtime ontology store, OWL restriction validation and the
generic CSV import/export pipeline.

It does **not** document the concepts mapped with that ORM (experiments, scientific objects,
devices, germplasm, …). Those live under their own module's documentation.

The `orm/` sub-directory holds one document per subsystem; everything else sits at this level.
Three older design notes that these documents treat as authoritative live one directory up,
under `architecture/`; they are listed in
[Related documents outside this directory](#related-documents-outside-this-directory).

## Start here

Read [The OpenSILEX SPARQL ORM](./orm-architecture.md) first. It is the only document written to
be read on its own: what the module is and why it exists, a layered view, the ORM's own class
model, the three lifecycles (JVM startup, per request, per operation), three end-to-end
walkthroughs with the real generated SPARQL, a vocabulary table and a reading order for three
common tasks. Every other document below assumes you have read it.

## Document index

### Overview

| Document | What it answers | Read it when |
|---|---|---|
| [orm-architecture.md](./orm-architecture.md) | How the whole ORM fits together, from annotation to triple store | Always first |
| [orm-optimizations.md](./orm-optimizations.md) | Which verified optimization opportunities exist, and what each would cost | A request is slow, or you are planning performance work |
| [orm-bugs-and-memory-leaks.md](./orm-bugs-and-memory-leaks.md) | Verified bugs, resource leaks and memory-retention risks | Behaviour surprises you, or before you assume something is intentional |

### The ORM, subsystem by subsystem

| Document | What it answers | Read it when |
|---|---|---|
| [orm/01-annotations-and-class-analysis.md](./orm/01-annotations-and-class-analysis.md) | What each annotation attribute does, and what `SPARQLClassAnalyzer` derives from it at startup | You are writing or changing a model class |
| [orm/02-object-mapper-and-index.md](./orm/02-object-mapper-and-index.md) | How a class resolves to a mapper, how the named graph and generation prefix are computed | You need to know where a concept's triples are stored |
| [orm/03-query-generation.md](./orm/03-query-generation.md) | The exact SPARQL generated for SELECT, COUNT, ASK, INSERT and DELETE, and why | You are reading a query in the logs, or a search returns the wrong rows |
| [orm/04-proxies-and-lazy-loading.md](./orm/04-proxies-and-lazy-loading.md) | How a result row becomes an object, what a proxy costs, and the three fetching strategies | You see N+1 queries, or a model breaks outside its request |
| [orm/05-sparql-service-crud.md](./orm/05-sparql-service-crud.md) | Every read and write entry point of `SPARQLService`, and how they differ | You are calling the ORM from a DAO |
| [orm/06-transactions-uri-and-validation.md](./orm/06-transactions-uri-and-validation.md) | Transaction nesting, URI generation and collision retry, relation validation, the prefix registry | You are writing data, or a URI or transaction behaves unexpectedly |
| [orm/07-filters-and-query-helpers.md](./orm/07-filters-and-query-helpers.md) | What a `filterHandler` can add: `SPARQLQueryHelper`, `SearchFilter`, URI-list queries, fetch plans | You are building a search |
| [orm/08-type-system-deserializers.md](./orm/08-type-system-deserializers.md) | Which Java types are literals, how URIs are shortened and compared, `SPARQLResult` | You are adding a field type, or two URIs will not compare equal |
| [orm/09-ontology-store-and-owl.md](./orm/09-ontology-store-and-owl.md) | The in-RAM vocabulary index, `OntologyDAO`, OWL restriction validation, SHACL | You are working on user-defined types and properties |
| [orm/10-connection-and-lifecycle.md](./orm/10-connection-and-lifecycle.md) | Connections, the RDF4J backend, configuration, module startup and shutdown | You are configuring a repository or debugging startup |
| [orm/11-csv-pipeline.md](./orm/11-csv-pipeline.md) | The generic CSV import and export pipeline driven by the ontology | You are adding CSV support to a concept |
| [orm/12-models-and-responses.md](./orm/12-models-and-responses.md) | The base model classes, multilingual labels, trees and DAGs, response DTOs | You are choosing a base class or building an API response |

### Cross-cutting and pre-existing notes

| Document | What it answers | Read it when |
|---|---|---|
| [graph-organization.md](./graph-organization.md) | Which named graph each OpenSILEX concept is stored in, and why the platform splits them | You need to know where instances live before choosing a `graph` attribute |
| [metadata.md](./metadata.md) | The generic metadata mechanism behind `SPARQLModelRelation` — untyped triples attached to a model | You are handling custom or user-defined properties on an instance |
| [ontology-ram-storage-optimization.md](./ontology-ram-storage-optimization.md) | Why the ontology store exists and what it replaced | You are wondering whether to enable or disable the RAM store |
| [sparql-property-annotation.md](./sparql-property-annotation.md) | The behaviour of `@IgnoreUpdateIfNull` and `@AutoUpdate`, from the caller's point of view. Its `@CascadeDelete` section is a stub — cascade delete is covered in [orm/05-sparql-service-crud.md](./orm/05-sparql-service-crud.md) | You are annotating a field whose update semantics matter |
| [sparql-update.md](./sparql-update.md) | The delete-then-recreate update cycle in short form | You are changing how a model is updated |

`OntologyStoreUMLClassDiagramm.png` accompanies
[ontology-ram-storage-optimization.md](./ontology-ram-storage-optimization.md).

### Related documents outside this directory

Three pre-existing design notes live under `technical-documentation/architecture/` rather than
here. The documents above treat them as authoritative and link to them; this set does not replace
them.

| Document | What it answers | Read it when |
|---|---|---|
| [architecture/sparql/DataFetching.md](../architecture/sparql/DataFetching.md) | The design note behind `SPARQLListFetcher`: how many-to-many values are fetched with `VALUES` instead of one query per model | You are reading [orm/03](./orm/03-query-generation.md), [orm/04](./orm/04-proxies-and-lazy-loading.md), [orm/05](./orm/05-sparql-service-crud.md) or [orm/07](./orm/07-filters-and-query-helpers.md) and want the original rationale |
| [architecture/sparql/graph-storage.md](../architecture/sparql/graph-storage.md) | How a model's storage graph is configured, generated or inherited | You are reading [orm/02](./orm/02-object-mapper-and-index.md) or [graph-organization.md](./graph-organization.md); it is the `graph`/`prefix` reference the ORM documents defer to |
| [architecture/csv/CSV%20import.md](../architecture/csv/CSV%20import.md) | The use cases and original conception of CSV validation and import, with a UML class diagram | You want the intent behind the pipeline that [orm/11-csv-pipeline.md](./orm/11-csv-pipeline.md) documents as built |

## Conventions used in these documents

- Every document opens with a history table; **add a line when you edit one**.
- Java files are linked with repo-root-relative `../` chains: six levels from a document at this
  level, seven from a document inside `orm/`.
- A precise location is written as `SPARQLService.java:412`.
- Each subsystem document ends with a "Gotchas and invariants" section. That is where behaviour
  that surprised the author is recorded rather than smoothed over — read it before changing that
  subsystem.
- Generated SPARQL shown in these documents is reconstructed from the builder code, with predicate
  IRIs expanded, because that is what is actually sent to the store.

## Rendering the diagrams

The diagrams are fenced code blocks tagged `mermaid`. **GitLab renders them natively**, so reading
these documents in the GitLab web UI gives the intended result.

The VitePress documentation site does **not** render them as shipped: it would need the
`vitepress-plugin-mermaid` plugin (and its `mermaid` peer dependency) registered in the site
configuration. Until that is added, every diagram appears on the site as a plain code block. The
documents are written so that the surrounding prose carries the same information — a diagram
shown as source is a readability loss, not an information loss.
