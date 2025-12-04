# Specifications : GeneticResources access rights


**Document history (please add a line when you edit the document)**


| Date       | Editor(s)    | OpenSILEX version | Comment           |
|------------|--------------|-------------------|-------------------|
| 22/09/2025 | Lydia ALIANE |                   | Document creation |




## Table of contents


<!-- TOC -->
* [Specifications : [{GeneticResources}] {access rights}]
* [Table of contents](#table-of-contents)
* [Definitions](#definitions)
* [Needs](#needs)
    * [Non-functional requirements](#non-functional-requirements)
* [Solution](#solution)
    * [Business logic](#business-logic)
* [Technical specifications](#technical-specifications)
    * [Tests](#tests)
    * [Environment](#environment)
<!-- TOC -->


## Definitions


- **geneticResource** : genetic material of plant cells.


## Needs


The main objective is to restrict access to geneticResources by introducing the concept of public and private geneticResources.


- Users need to see only the geneticResources they are authorized to access, with the visibility status (public or private) clearly indicated in the main list.


- Users need to filter geneticResources by visibility (public/private) in the advanced filter menu, showing only the geneticResources they are authorized to access.


- Users need restricted access to private geneticResources, only if they belong to the associated group or are the publisher.


- Admin users need full access to all geneticResources without restriction.




### Non-functional requirements


- **Ergonomy** :
    - Each geneticResource in the main list shows whether it is public or private, allowing users to immediately understand the access restrictions.
    - In the search menu, Users can filter geneticResources by visibility (public/private) using advanced filters.




## Solution


GeneticResource access is now managed using a public/private classification combined with group-based permissions. Admin users have full access, while non-admins can view only public geneticResources or private ones they are authorized to access.


### Business logic


- **Public geneticResources**
  These are accessible to all users, regardless of their group membership.

- **Private geneticResources**
  These are only accessible to users who belong to one of the groups associated with the geneticResource.

- **Admin users**
    - Have unrestricted access to all geneticResources (no filters applied).

- **Non-admin users**
    1. If the user has **no group membership**:
        - They can only see **public geneticResources**.
    2. If the user has **one or more group memberships**:
        - They can see:
            - **Public geneticResources**, and
            - **Private geneticResources** associated with their groups.


- **Search and filtering**


1. Access restrictions are applied **transparently** during search operations:
    - Filters are automatically added to SPARQL queries to enforce the rules above.
    - The `is_public` parameter and `groupsUsers` are used to define the visibility scope.


2. **Sorting, pagination, and metadata filters** operate only within the subset of geneticResources the user is authorized to access.


- *Security note:* Users who belong to multiple groups have cumulative access rights. Care must be taken to ensure that access control is correctly enforced across all groups.


## Technical specifications


## API Layer

- **Endpoint:** `GeneticResourceResource.searchGeneticResource()`
- **Accepted Query Parameters:**  
  `uri`, `rdf_type`, `name`, `accession`, `species`, `variety`, `institute`, `experiment`, `parent_geneticResources`, `metadata`, `is_public`, `order_by`, `page`, `page_size`
- **Functionality:**
    - Converts search results into DTOs (`GeneticResourceGetAllDTO`)
    - Returns a paginated response (`PaginatedListResponse`)

---

## Business Logic Layer

- **Class:** `GeneticResourceLogic`
- **Method:** `search(GeneticResourceSearchFilter searchFilter, boolean fetchMetadata, boolean fetchNestedObjects)`
- **Responsibilities:**
    - Builds the search query using `GeneticResourceSearchFilter`
    - Handles optional metadata loading via `metaDataDao.getMetaDataAssociatedTo()`
    - Loads nested geneticResource relations conditionally via `fetchGeneticResourcesOfRelation()`

---

## Data Access Layer

- **Classes:** `GeneticResourceDAO`, `SPARQLDAO`
- **Responsibilities:**
    - Performs SPARQL queries with pagination using `searchWithPagination()`
    - Applies access filters at the query level via `appendUserGeneticResourceFilter()` and `appendgroupsListFilters()` to enforce user permissions



