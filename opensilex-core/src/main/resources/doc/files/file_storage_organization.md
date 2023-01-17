# File storage organization

- **Description**: Rules concerning path computing for all model which requires a file storage connection
- **Author** : Renaud COLIN (INRAE MISTEA)
- **Date** : 06/01/2023
- **Tags**: `[FileSystem, URI, Document, DataFile]`
- 
# Document

Document is composed of two component :
- The description, which is stored in RDF, inside the `document` graph. The description contains all metadata
  about the document like, uri, type, title, author, etc
- The document file it-self, which can be stored in any available file storage connection (local, gridfs, S3, etc)

## Data file path compute

Note: we consider `http://www.opensilex.com` as base URI

The following procedure is applied :

> URI computing

- Compute the URI of a document file with OpenSILEX base URI and file title
    - ex : For the book _RDF Database Systems_, the URI will be `http://www.opensilex.com/id/document/rdf_database_systems`
  
> Path computing

- Compute a hex code of the file URI, ex : `687474703A2F2F7777772E6F70656E73696C65782E636` (the example is truncated for readability reason)
- Extract path from URI
  - **If the URI is absolute** (ex : `http://www.opensilex.com/id/document/rdf_database_systems`)
    - extract path (here: `id/document/rdf_database_systems`)
  - **else** (ex: `test:id/rdf_database_systems`)
    - extract scheme specific part (here: `id/document/rdf_database_systems`)
- append `documents` prefix
- **The final Path in the file storage connection will be** : `documents/id/document/rdf_database_systems/687474703A2F2F7777772E6F70656E73696C65782E636`



# DataFile

Data file is composed of two component : 
- The description, which is stored in MongoDB, inside the `file` collection. The description contains all metadata
about the file like, uri, filename, date, provenance, etc
- The data file it-self, which can be stored in any available file storage connection (local, gridfs, S3, etc)

## Data file path compute

The following procedure is applied : 

> URI computing
- Compute the URI of a Data file with a UUID type 4 if the URI is not set (default case)
  - ex : `http://www.opensilex.com/id/datafile/1be49e1a-62b7-4047-83b3-74500caeb0ae`
  - The usage of UUID type 4, ensure a nearly unique URI. If the URI already exists (which has very low chance of occurs), then the URI unicity constraint on MongoDB collection will
  throw an error, and the insertion will fail.

> Path computing
- Compute a Base64 of the URI
  - ex : `ZGF0YWZpbGUKaHR0cDovL3d3dy5waGVub21lLWZwc` (the example is truncated for readability reason)
- append `datafile` prefix
- **The final Path in the file storage connection will be** `datafile/ZGF0YWZpbGUKaHR0cDovL3d3dy5waGVub21lLWZwc`