**History**

| Date | Author | Developer(s) | OpenSILEX version | Comment |
|----|----|----|----|----|
| 13/02/2023 | Gabriel Besombes | Gabriel Besombes | 1.2.1 | Document creation & mappings |


# Table of contents

* [General description](#general-description)
* [Special service : GET Faidare Calls](#special-service--get-brapi-calls)
* [Mapping between Faidare V1.3 concepts and OpenSILEX concepts](#mapping-between-brapi-v13-concepts-and-opensilex-concepts)
  * [Variables](#variables)
    * [Methods](#methods)
    * [Traits](#traits)
    * [Scales](#scales)
  * [Germplasm](#germplasm)
  * [Studies](#studies)
    * [Contacts](#contacts)
    * [Locations](#locations)
  * [Observations](#observations)
  * [ObservationUnit](#observationunit)
* [Known issues and limitations](#known-issues-and-limitations)


# General description


The Faidare module is an implementation of the [BrAPI API specification](https://brapi.org/specification) for OpenSILEX with specific changes to be compatible with the Faidare implementation of the BrAPI specification.

As such the specs are relatively similar to [BrAPIV1.md](./BrAPIV1.md) .


Differences will be higlited in **Bold**.


# Special service : GET Faidare Calls

This service should return a list of the available services as well as their datatypes, methods and version. The expected result is :

```json
{
  "metadata": {
    "pagination": {
      "pageSize": 20,
      "currentPage": 0,
      "totalCount": 6,
      "totalPages": 1
    },
    "status": [],
    "datafiles": []
  },
  "result": {
    "data": [
      {
        "call": "calls",
        "dataTypes": [
          "application/json"
        ],
        "methods": [
          "GET"
        ],
        "versions": [
          "1.3"
        ]
      },
      {
        "call": "studies",
        "dataTypes": [
          "application/json"
        ],
        "methods": [
          "GET"
        ],
        "versions": [
          "1.3"
        ]
      },
      {
        "call": "germplasm",
        "dataTypes": [
          "application/json"
        ],
        "methods": [
          "GET"
        ],
        "versions": [
          "1.3"
        ]
      },
      {
        "call": "locations",
        "dataTypes": [
          "application/json"
        ],
        "methods": [
          "GET"
        ],
        "versions": [
          "1.3"
        ]
      },
      {
        "call": "variables",
        "dataTypes": [
          "application/json"
        ],
        "methods": [
          "GET"
        ],
        "versions": [
          "1.3"
        ]
      },
      {
        "call": "trials",
        "dataTypes": [
          "application/json"
        ],
        "methods": [
          "GET"
        ],
        "versions": [
          "1.3"
        ]
      }
    ]
  }
}
```

# Mapping between Faidare V1.3 concepts and OpenSILEX concepts

<br>

## Variables

| Faidarev1ObservationVariableDTO                                                                                                  | OpenSILEX VariableModel              | Notes                                          |
|----------------------------------------------------------------------------------------------------------------------------------|--------------------------------------|------------------------------------------------|
| contextOfUse array\[string\]	Indication of how trait is routinely used. (examples: \["Trial evaluation", "Nursery evaluation"\]) |                                      |                                                |
| crop string	Crop name (examples: "Maize", "Wheat")                                                                               | species when only one                |                                                |
| defaultValue string	Variable default value. (examples: "red", "2.3", etc.)                                                       |                                      |                                                |
| documentationURL string (uri)	A URL to the human readable documentation of this object                                           |                                      |                                                |
| growthStage string	Growth stage at which measurement is made (examples: "flowering")                                             |                                      |                                                |
| institution string	Name of institution submitting the variable                                                                   |                                      |                                                |
| language string	2 letter ISO code for the language of submission of the variable.                                                | nothing yet. Waiting for multilabels |                                                |
| method object	Method metadata                                                                                                    | see method below                     |                                                |
| scale object	Scale metadata                                                                                                      | see scale below                      |                                                |
| scientist string	Name of scientist submitting the variable.                                                                      |                                      |                                                |
| status string	Variable status. (examples: "recommended", "obsolete", "legacy", etc.)                                             |                                      |                                                |
| submissionTimestamp Timestamp when the Variable was added (ISO 8601)                                                             |                                      | __not in Faidare__                             |
| synonyms array\[string\]	Other variable names                                                                                    | alternativeName                      | can be extended once multilabels are available |
| trait object                                                                                                                     | see trait below                      |                                                |
| xref string	Cross reference of the variable term to a term from an external ontology or to a database of a major system.         | first exactMatch if available        | imperfect as n -> 1                            |
| observationVariableDbId string	Variable unique identifier                                                                        | URI                                  |                                                |
| observationVariableName string	Variable name (usually a short name)                                                              |                                      | __not in Faidare__                             |
| name string	string	DEPRECATED in v1.3 - Use "observationVariableName"                                                            | name                                 | __deprecated but used in faidare__             |
| date string	DEPRECATED in v1.3 - see "submissionTimestamp"                                                                       | publicationDate                      | __deprecated but used in faidare__             |
| ontologyDbId string	Ontology database unique identifier                                                                          |                                      |                                                |
| ontologyName string	Ontology name                                                                                                |                                      |                                                |
| ontologyReference object                                                                                                         |                                      | __not in Faidare__                             |

### Methods

| Faidarev1MethodDTO                                                                                                                                                           | OpenSILEX MethodModel | Notes                                                                                                                                                          |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| class	string	Method class (examples: "Measurement", "Counting", "Estimation", "Computation", etc.                                                                            |                       | We have something somewhat equivalent but on the provenance so it is more linked to the observation than directly to the method (that is part of the variable) |
| description	string	Method description.                                                                                                                                       | description           |                                                                                                                                                                |
| formula	string	For computational methods i.e., when the method consists in assessing the trait by computing measurements, write the generic formula used for the calculation |                       | No equivalent in OpenSILEX but would be useful                                                                                                                 |
| methodDbId	string	Method unique identifier                                                                                                                                   | uri                   |                                                                                                                                                                |
| methodName	string	Human readable name for the method                                                                                                                         |                       | __not in Faidare__                                                                                                                                             |
| name	string	DEPRECATED in v1.3 - Use "methodName"                                                                                                                            | name                  | __deprecated but used in faidare__                                                                                                                             |
| ontologyReference	object                                                                                                                                                     |                       | __not in Faidare__                                                                                                                                             |
| reference	string	Bibliographical reference describing the method.                                                                                                            |                       |                                                                                                                                                                |

### Traits

| Faidarev1TraitDTO                                                                                                                                                                                                                                  | OpenSILEX VariableModel                                          | Notes                              |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|------------------------------------|
| alternativeAbbreviations	array\[string\]	Other frequent abbreviations of the trait, if any. These abbreviations do not have to follow a convention                                                                                                 |                                                                  |                                    |
| attribute	string	A trait can be decomposed as "Trait" = "Entity" + "Attribute", the attribute is the observed feature (or characteristic) of the entity e.g., for "grain colour", attribute = "colour"                                             | Characteristic name                                              |                                    |
| class	string	Trait class. (examples: "morphological trait", "phenological trait", "agronomical trait", "physiological trait", "abiotic stress trait", "biotic stress trait", "biochemical trait", "quality traits trait", "fertility trait", etc.) |                                                                  |                                    |
| description	string	The description of a trait                                                                                                                                                                                                      |                                                                  |                                    |
| entity	string	A trait can be decomposed as "Trait" = "Entity" + "Attribute", the entity is the part of the plant that the trait refers to e.g., for "grain colour", entity = "grain"                                                               | Entity name                                                      |                                    |
| mainAbbreviation	string	Main abbreviation for trait name. (examples: "Carotenoid content" => "CC")                                                                                                                                                 |                                                                  |                                    |
| ontologyReference	object                                                                                                                                                                                                                           |                                                                  |                                    |
| status	string	Trait status (examples: "recommended", "obsolete", "legacy", etc.)                                                                                                                                                                   |                                                                  |                                    |
| synonyms	array\[string\]	Other trait names                                                                                                                                                                                                         |                                                                  |                                    |
| traitDbId	string	The ID which uniquely identifies a trait                                                                                                                                                                                          | trait URI if exists                                              |                                    |
| traitName	string	The human readable name of a trait                                                                                                                                                                                                |                                                                  | __not in Faidare__                 |
| name	string	DEPRECATED in v1.3 - Use "traitName"                                                                                                                                                                                                   | trait name if exist else Entity name + “_” + Characteristic name | __deprecated but used in faidare__ |
| xref	string	Cross reference of the trait to an external ontology or database term e.g., Xref to a trait ontology (TO) term                                                                                                                         |                                                                  |                                    |

### Scales

| Faidarev1ScaleDTO                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 | OpenSILEX UnitModel                                                                                                                                                                                    | Notes                                                              |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------|
| dataType	string	Class of the scale, entries can be "Code" - This scale class is exceptionally used to express complex traits. Code is a nominal scale that combines the expressions of the different traits composing the complex trait. For exemple a severity trait might be expressed by a 2 digit and 2 character code. The first 2 digits are the percentage of the plant covered by a fungus and the 2 characters refer to the delay in development, e.g. "75VD" means "75%" of the plant is Crop Ontology & Integrated Breeding Platform Curation Guidelines 5/6/2016 9 infected and the plant is very delayed. "Date" - The date class is for events expressed in a time format, e.g. yyyymmddThh:mm:ssZ or dd/mm/yy "Duration" - The Duration class is for time elapsed between two events expressed in a time format, e.g. days, hours, months "Nominal" - Categorical scale that can take one of a limited and fixed number of categories. There is no intrinsic ordering to the categories "Numerical" - Numerical scales express the trait with real numbers. The numerical scale defines the unit e.g. centimeter, ton per hectar, branches "Ordinal" - Ordinal scales are scales composed of ordered categories "Text" - A free text is used to express the trait. | Extracted from the variable's dataType. With the following mapping : <br>xsd:decimal + xsd:integer -> Numerical <br> xsd:date + xsd:dateTime -> Date <br>xsd:string -> Text <br>xsd:boolean -> Nominal |                                                                    |
| decimalPlaces	integer	For numerical, number of decimal places to be reported                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |                                                                                                                                                                                                        |                                                                    |
| ontologyReference	object                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |                                                                                                                                                                                                        |                                                                    |
| scaleDbId	string	Unique identifier of the scale. If left blank, the upload system will automatically generate a scale ID.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | uri                                                                                                                                                                                                    |                                                                    |
| scaleName	string	Name of the scale                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |                                                                                                                                                                                                        | __not in Faidare__                                                 |
| name	string	DEPRECATED in v1.3 - Use "scaleName"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | name                                                                                                                                                                                                   | __deprecated but used in faidare__                                 |
| validValues	object                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |                                                                                                                                                                                                        |                                                                    |
| xref	string	Cross reference to the scale, for example to a unit ontology such as UO or to a unit of an external major database                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |                                                                                                                                                                                                        | There can be multiple in OpenSILEX (equivalent to skos:exactMatch) |

<br>

## Germplasm


__Note : only accessions are considered germplasms for BrAPI__

This is why an error is returned when this notion doesn't exist :

```java
return new BadRequestException(
        "The 'Accession' notion doesn't exist in your ontology so this service is unavailable"
);
```

| Faidarev1GermplasmDTO                                                                                                                                                                                          | OpenSILEX GermplasmModel | Notes              |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------|--------------------|
| accessionNumber string	This is the unique identifier for accessions within a genebank, and is assigned when a sample is entered into the genebank collection                                                   | accessionNumber          |                    |
| acquisitionDate string (date)	The date this germplasm was aquired by the genebank (MCPD)                                                                                                                       |                          |                    |
| biologicalStatusOfAccessionCode integer	The 3 digit code representing the biological status of the accession (MCPD)                                                                                            |                          |                    |
| breedingMethodDbId string	The unique identifier for the breeding method used to create this germplasm                                                                                                          |                          | __not in faidare__ |
| commonCropName string	Common name for the crop (MCPD)                                                                                                                                                          |              |                    |
| countryOfOriginCode string	3-letter ISO 3166-1 code of the country in which the sample was originally collected (MCPD)                                                                                         |                          |                    |
| defaultDisplayName string	Human readable name used for display purposes                                                                                                                                        | name                     |                    |
| documentationURL string (uri)	A URL to the human readable documentation of this object                                                                                                                         | website                  | Incomplete mapping |
| donors array\[object\]	List of donor institutes (MCPD)                                                                                                                                                         |                          |                    |
| germplasmDbId string	The ID which uniquely identifies a germplasm within the given database server                                                                                                             | uri                      |                    |
| genus string	DEPRECATED in v1.3 - see "germplasmGenus"                                                                                                                                                         |                          |                    |
| germplasmGenus string	Genus name for taxon. Initial uppercase letter required. (MCPD)                                                                                                                          |                          | __not in faidare__ |
| germplasmName string	Name of the germplasm. It can be the prefered name and does not have to be unique.                                                                                                        | name                     |                    |
| germplasmPUI string	The Permanent Unique Identifier which represents a germplasm                                                                                                                               | germplasm URI            |                    |
| germplasmSpecies string	Specific epithet portion of the scientific name in lowercase letters. (MCPD)                                                                                                           |                          | __not in faidare__ |
| instituteCode string	The code for the Institute that has bred the material. (MCPD)                                                                                                                             | institute Code           |                    |
| instituteName string	The name of the institution which bred the material (MCPD)                                                                                                                                |                          |                    |
| pedigree string	The cross name and optional selection history.                                                                                                                                                 |                          |                    |
| seedSource string	The source of the seed                                                                                                                                                                       |                          |                    |
| species string	DEPRECATED in v1.3 - see "germplasmSpecies"                                                                                                                                                     | species name             |                    |
| speciesAuthority string	The authority organization responsible for tracking and maintaining the species name (MCPD)                                                                                            |                          |                    |
| subtaxa string	Subtaxon can be used to store any additional taxonomic identifier. (MCPD)                                                                                                                       | var. *variety*           |                    |
| subtaxaAuthority string	The authority organization responsible for tracking and maintaining the subtaxon information (MCPD)                                                                                    |                          |                    |
| synonyms array\[string\]	List of alternative names or IDs used to reference this germplasm                                                                                                                     |                  |                    |
| taxonIds array\[object\]	The list of IDs for this SPECIES from different sources. If present, NCBI Taxon should be always listed as "ncbiTaxon" preferably with a purl. The rank of this ID should be species. |                          |                    |
| typeOfGermplasmStorageCode array\[string\]	The 2 digit code representing the type of storage this germplasm is kept in at a genebank. (MCPD)                                                                   |                          |                    |

<br>


## Studies

__Note :__ The FAIDARE studies call seems to be a mix between the GET studies and GET studies by studyDbId BrAPI calls.

| Faidarev1StudyDTO                                                                                                                                   | OpenSILEX ExperimentModel                                                                                             | Notes                                                                                                         |
|-----------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| active	string	Is this study currently active                                                                                                        | calculated from start/end dates and current date                                                                      | Could make some cleanup because code is repeated in the frontend for active/inactive experiment               |
| additionalInfo	object	Additional arbitrary info                                                                                                     |                                                                                                                       | Could add some unmapped info in there (Its a simple map string : string)                                      |
| ~~commonCropName	string	Common name for the crop associated with this study~~                                                                       | Name of species if only one species used in the experiment                                                            | Could pick the first species associated with the study but incomplete because it's not the common crop name   |
| documentationURL	string (uri)	A URL to the human readable documentation of this object                                                              | URL of detail page for the experiment -> should probably be done with dereferencing instead                           |                                                                                                               |
| endDate	string (date)	The date the study ends                                                                                                       | endDate                                                                                                               |                                                                                                               |
| locationDbId	string	The ID which uniquely identifies a location                                                                                     | First  facility in returned facilities. A bit unstable (might return something different depending on facility order) |                                                                                                               |
| locationName	string	The human readable name for a location                                                                                          | Same as above but name                                                                                                |                                                                                                               |
| name	string	DEPRECATED in v1.3 - Use "studyName"                                                                                                    | name                                                                                                                  |                                                                                                               |
| programDbId	string	The ID which uniquely identifies a program within the given database server                                                      |                                                                                                                       |                                                                                                               |
| programName	string	The humane readable name of a program                                                                                            |                                                                                                                       |                                                                                                               |
| seasons	array\[string\]	List of seasons over which this study was performed.                                                                        | return a string for each year in the experiment                                                                       | incomplete as OpenSILEX doesn't have this notion. __array\[object\] in BrAPI but array\[string\] in Faidare__ |
| startDate	string (date)	The date this study started                                                                                                 | startDate                                                                                                             |                                                                                                               |
| studyDbId	string	The ID which uniquely identifies a study within the given database server                                                          | uri                                                                                                                   |                                                                                                               |
| studyName	string	The humane readable name of a study                                                                                                | name                                                                                                                  |                                                                                                               |
| studyType	string	DEPRECATED in v1.3 - See "studyTypeName"                                                                                           |                                                                                                                       | Notion doesn't exist in OpenSILEX                                                                             |
| ~~studyTypeDbId	string	The unique identifier of the type of study being performed.~~                                                                |                                                                                                                       | Notion doesn't exist in OpenSILEX __not used in Faidare__                                                     |
| ~~studyTypeName	string	The name of the type of study being performed. ex. "Yield Trial", etc~~                                                      |                                                                                                                       | see above __not used in Faidare__                                                                             |
| trialDbId	string	The ID which uniquely identifies a trial                                                                                           |                                                                                                                       | Can't be mapped properly in OpenSILEX as there can be multiple projects. See "trialDbIds" instead             |
| trialName	string	The human readable name of a trial                                                                                                 |                                                                                                                       | see above                                                                                                     |
| trialDbIds	array\[string\]	The IDs which uniquely identifies the trials                                                                             | mapped to project uris                                                                                                | Notion doesn't really exist in OpenSILEX. __attribute doesn't exist in BrAPI__                                |
| contacts	array\[object\]	List of contact entities associated with this study                                                                        | Supervisors (scientific and technical)                                                                                | __from study by studyDbId__                                                                                   |
| dataLinks	array\[object\]	List of links to extra data files associated with this study. Extra data could include notes, images, and reference data. |                                                                                                                       | Couldn't realy match this with anything.. Could be matched to datasets? __from study by studyDbId__           |
| lastUpdate	object	The date and time when this study was last modified                                                                               | last update to timestamp (we don't have versioning)                                                                   | __from study by studyDbId ?__                                                                                 |
| studyDescription	string	The description of this study                                                                                               | experiment description                                                                                                |                                                                                                               |
| observationVariableDbIds	array[\string\]	Observation variables used in the study                                                                    | uris of variables of data in the experiment                                                                           | __not form BrAPI. Added for FAIDARE__                                                                         |
| germplasmDbIds	array[\string\]	Germplasms used in the study                                                                                         | accessions of the scientific objects in the experiment                                                                | __not form BrAPI. Added for FAIDARE__                                                                         |

StudySummary :

| Faidarev1StudySummaryDTO | OpenSILEX ExperimentModel | Notes                                                                                 |
|--------------------------|---------------------------|---------------------------------------------------------------------------------------|
| locationDbId	string      | uri of first facility     | issues with n -> 1 because multiple facilities in OpenSILEX for one location in BrAPI |
| locationName	string      | same as above but name    |                                                                                       |
| studyDbId	string         | experiment uri            |                                                                                       |
| studyName	string         | experiment name           |                                                                                       |


### Contacts

| Faidarev1ContactDTO                                                                           | OpenSILEX PersonModel                                                        | Notes                          |
|-----------------------------------------------------------------------------------------------|------------------------------------------------------------------------------|--------------------------------|
| contactDbId	string	The ID which uniquely identifies this contact                              | Person URI                                                                   |                                |
| email	string	The contacts email address                                                       | Person email                                                                 |                                |
| instituteName	string	The name of the institution which this contact is part of                | Person affiliation                                                           | __institutionName in Faidare__ |
| name	string	The full name of this contact person                                              | Person last name to uppercase + person firstname with uppercase first letter |                                |
| orcid	string	The Open Researcher and Contributor ID for this contact person (orcid.org)       | Person ORCID                                                                 |                                |
| type	string	The type of person this contact represents (ex: Coordinator, Scientist, PI, etc.) | Person role in experiment                                                    |                                |

### Locations

| Faidarev1LocationDTO                                                                                    | OpenSILEX FacilityModel                                                                                | Notes                                    |
|---------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|------------------------------------------|
| abbreviation	string	An abbreviation which represents this location                                      |                                                                                                        |                                          |
| abreviation	string	Deprecated Use abbreviation                                                          |                                                                                                        | __deprecated but still used in faidare__ |
| additionalInfo	object	Additional arbitrary info                                                         |                                                                                                        |                                          |
| altitude	number	The altitude of this location                                                           |                                                                                                        | we usually don't have this info          |
| countryCode	string	ISO_3166-1_alpha-3 spec                                                              | Code of the country of the facility's address or of the site of its first parent organization with one |                                          |
| countryName	string	The full name of the country where this location is                                  | Same as above but name or country from address if exists                                               |                                          |
| documentationURL	string (uri)	A URL to the human readable documentation of this object                  |                                                                                                        |                                          |
| instituteAddress	string	The street address of the institute representing this location                  | Address of the facility or, if none, of the site of the first parent organisation with only one        |                                          |
| instituteAdress	string	Deprecated Use instituteAddress                                                  | Address of the facility or, if none, of the site of the first parent organisation with only one        | __deprecated but still used in faidare__ |
| instituteName	string	each institute/laboratory can have several experimental field                      | Same as above but name                                                                                 |                                          |
| latitude	number	The latitude of this location                                                           | Latitude of centroid of facility geometry                                                              |                                          |
| locationDbId	string	string identifier                                                                   | URI of facility                                                                                        |                                          |
| locationName	string	A human readable name for this location                                             | Name of facility                                                                                       |                                          |
| name	string	DEPRECATED in v1.3 - Use "locationName"                                                     | Name of facility                                                                                       |                                          |
| locationType	string	The type of location this represents (ex. Breeding Location, Storage Location, etc) | URI of type of facility                                                                                |                                          |
| longitude	number	the longitude of this location                                                         | Longitude of centroid of facility geometry                                                             |                                          |

## Trials

__Note :__ The FAIDARE Trials call seems to be a mix between the GET Trials and GET Trials by trialDbId BrAPI calls. 

| Faidarev1ObservationUnitDTO                                                            | OpenSILEX ProjectDTO                                                                                        | Notes                                                                                  |
|----------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| trialDbId string The ID which uniquely identifies a trial                              | Project uri                                                                                                 |                                                                                        |
| trialName string The human readable name of a trial                                    | Project name                                                                                                |                                                                                        |
| trialType string                                                                       | Project objective                                                                                           | __Added in FAIDARE, not in BrAPI__                                                     |
| endDate string (date) The date this trial ends                                         | Project endDate                                                                                             |                                                                                        |
| startDate string (date) The date this trial started                                    | Project stratDate                                                                                           |                                                                                        |
| active boolean Is this trail currently active                                          |                                                                                                             | no mapping available                                                                   |
| programDbId string The ID which uniquely identifies a program                          |                                                                                                             | not sure what to map this to. It seems like brapi:Program is also an opensilex:Project |
| programName string The human readable name of a program                                |                                                                                                             | see above                                                                              |
| datasetAuthorship object                                                               |                                                                                                             | __Now deprecated__ no mapping availabel                                                |
| contacts array\[object\] List of contact entities associated with this trial           | Project combined set of Administrative and Scientific contacts (no repeats if people are both). see contact | __Present in details, not in list__                                                    |
| studies array\[object\] List of studies inside this trial                              | see study summary, experiments of the project                                                               |                                                                                        |
| additionalInfo object Additional arbitrary info                                        | see detail below                                                                                            |                                                                                        |
| documentationURL string (uri) A URL to the human readable documentation of this object | Project homePage                                                                                            |                                                                                        |
| ~~commonCropName string Common name for the crop associated with this trial~~          |                                                                                                             | no mapping available __Removed in Faidare__                                            |
| ~~publications	array\[object\]~~	                                                      |                                                                                                             | __Present in details, not in list. removed in Faidare__                                |

Faidarev1TrialAdditionalInfoDTO :

| Key              | Value                                       |
|------------------|---------------------------------------------|
| shortName        | Project shortName                           |
| description      | Project description                         |
| financialFunding | Project financialFunding                    |
| relatedProjects  | Project relatedProjects                     |
| coordinators     | Project coordinators as Faidarev1ContactDTO |


# Known issues and limitations

For Observation Units positions the move events aren’t taken into account.

No tests are implemented. Testing was done using the [BrAPI-Validator](https://github.com/plantbreeding/IPK-BrAPI-Validator) in a modified version to allow for the use of special characters in DbIds. This is needed because we use URIs, that have special characters, as DbIds. These changes couldn’t be added to the main repo yet. So for now this version is available [here](https://forgemia.inra.fr/OpenSILEX/opensilex-brapi-validator).

An additional step would be to use this app in our gitlab-ci for automated testing of these services.