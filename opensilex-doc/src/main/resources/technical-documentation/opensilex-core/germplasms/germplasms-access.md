# Specifications : Germplasms access rights


**Document history (please add a line when you edit the document)**


| Date       | Editor(s)    | OpenSILEX version | Comment           |
|------------|--------------|-------------------|-------------------|
| 22/09/2025 | Lydia ALIANE | 1.4.10            | Document creation |




## Table of contents


<!-- TOC -->
* [Specifications : Germplasms access](#specifications--germplasms-access-rights)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Needs](#needs)
    * [Non-functional requirements](#non-functional-requirements)
  * [Technical specifications](#technical-specifications)
      * [API Layer](#api-layer)
      * [Business Logic Layer](#business-logic-layer)
      * [Data Access Layer](#data-access-layer)
<!-- TOC -->


## Definitions


- **germplasm** : genetic material of plant cells.


## Needs


The main objective is to restrict access to germplasms by introducing the concept of public and private germplasms.


- Users need to see only the germplasms they are authorized to access, with the visibility status (public or private) clearly indicated in the main list.


- Users need to filter germplasms by visibility (public/private) in the advanced filter menu, showing only the germplasms they are authorized to access.


- Users need restricted access to private germplasms, only if they belong to the associated group or are the publisher.


- Admin users need full access to all germplasms without restriction.

### Non-functional requirements


- **Ergonomy** :
    - Each germplasm in the main list shows whether it is public or private, allowing users to immediately understand the access restrictions.
    - In the search menu, Users can filter germplasms by visibility (public/private) using advanced filters.
    - 

## Technical specifications


### API Layer

- **Endpoint:** `GermplasmResource.searchGermplasm()`
- **Accepted Query Parameters:**  
  `uri`, `rdf_type`, `name`, `accession`, `species`, `variety`, `institute`, `experiment`, `parent_germplasms`, `metadata`, `is_public`, `order_by`, `page`, `page_size`
- **Functionality:**
    - Converts search results into DTOs (`GermplasmGetAllDTO`)
    - Returns a paginated response (`PaginatedListResponse`)

---

### Business Logic Layer

- **Class:** `GermplasmLogic`
- **Method:** `search(GermplasmSearchFilter searchFilter, boolean fetchMetadata, boolean fetchNestedObjects)`
- **Responsibilities:**
    - Builds the search query using `GermplasmSearchFilter`
    - Handles optional metadata loading via `metaDataDao.getMetaDataAssociatedTo()`
    - Loads nested germplasm relations conditionally via `fetchGermplasmsOfRelation()`

---

### Data Access Layer

- **Classes:** `GermplasmDAO`, `SPARQLDAO`
- **Responsibilities:**
    - Performs SPARQL queries with pagination using `searchWithPagination()`
    - Applies access filters at the query level via `appendUserGermplasmFilter()` and `appendgroupsListFilters()` to enforce user permissions


