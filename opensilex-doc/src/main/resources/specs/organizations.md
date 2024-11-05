******
* This document follows the Specification Guidelines draft (as of 2023-05-12)
* Author : valentin.rigolle@inrae.fr
* Date : 2023-05-12
******

# Specifications : organizations

This document briefly describes the functional and technical specifications of the organizations in OpenSILEX.

## Needs

Organizations represent the institutions responsible for the experiments and facilities. They include for example
national organizations, installations or experimental units. Organizations are hierarchically structured : an org
can be part of zero, one or more "parents", and can have multiple "children".

Organizations regroup users with different roles, and different permissions.

Organizations are located within Sites, and are hosted by Facilities.

## Solution

### Business logic

A user has access to an organization if and only if at least one of these conditions is verified :

- The user is admin
- The user is a member of a group associated with the organization
- The user is a member of a group associated with an ancestor organization
- The user is the creator of the organization
- Neither the organization nor any of its ancestor have associated groups (an organization without
  group is considered public)

## Technical specifications

### Definitions

- **DAG** (_Directed Acyclic Graph_) is a type of graph where all arcs are directed and no cycle is formed. It
  corresponds to multi-parent hierarchies. A set of organizations with their "is part of" relationships can be
  represented by a DAG.

### Detailed explanations

The logic for checking the organization access rights is performed by a SPARQL queries. The class `OrganizationSPARQLHelper`
regroups some methods to build this query, mainly `addOrganizationAccessClause` (see its Javadoc for more details
on how the query works).

To increase the performance, the search results are cached in `OrganizationDAO`. The first time a user tries to search
organizations, a first query is performed to retrieve all organizations the user has access to. The filter is then
applied on the cached values. The next time this user performs a search, the results will directly be taken from the cache
and no SPARQL query will be performed, allowing for a much shorter response time.

The organization search uses the `SPARQLListFetcher` class to retrieve all children organizations in a single query,
delegating the hierarchy reconstruction to the caller.

The list of organizations is turned into a list of `ResourceDagDTO` using the `ResourceDagDTOBuilder`. This class
updates the `parents` fields based on the `children` of the given models.

### Tests

Generic organization tests are located in `OrganizationAPITest`.

Access right tests are located in `OrganizationAccessAPITest`.