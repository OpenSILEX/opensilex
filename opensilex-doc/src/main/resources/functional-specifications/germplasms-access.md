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
  * [Solution](#solution)
      * [Business logic](#business-logic)
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

    

## Solution


Germplasm access is now managed using a public/private classification combined with group-based permissions. Admin users have full access, while non-admins can view only public germplasms or private ones they are authorized to access.


### Business logic


- **Public germplasms**
  These are accessible to all users, regardless of their group membership.

- **Private germplasms**
  These are only accessible to users who belong to one of the groups associated with the germplasm.

- **Admin users**
    - Have unrestricted access to all germplasms (no filters applied).

- **Non-admin users**
    1. If the user has **no group membership**:
        - They can only see **public germplasms**.
    2. If the user has **one or more group memberships**:
        - They can see:
            - **Public germplasms**, and
            - **Private germplasms** associated with their groups.


- **Search and filtering**


1. Access restrictions are applied **transparently** during search operations:
    - Filters are automatically added to SPARQL queries to enforce the rules above.
    - The `is_public` parameter and `groupsUsers` are used to define the visibility scope.


2. **Sorting, pagination, and metadata filters** operate only within the subset of germplasms the user is authorized to access.


- *Security note:* Users who belong to multiple groups have cumulative access rights. Care must be taken to ensure that access control is correctly enforced across all groups.



