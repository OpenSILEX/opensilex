**History**

| Date | Author | Developer(s) | OpenSILEX version | Comment |
|----|----|----|----|----|
| 16/10/2023 | Gabriel Besombes | Gabriel Besombes | 1.1.0 | Document creation & mappings |
| 04/12/2023 | Gabriel Besombes | Gabriel Besombes | 1.1.0 | Updates and fixes for release 1.2.0 |


# Table of contents

* [General description](#general-description)
* [Special service : GET BrAPI Calls](#special-service--get-brapi-calls)
* [Mapping between BrAPI V1.3 concepts and OpenSILEX concepts](#mapping-between-brapi-v13-concepts-and-opensilex-concepts)
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


The BrAPI module is an implementation of the [BrAPI API specification](https://brapi.org/specification) for OpenSILEX.

As such the specs are relatively straightforward. They are simple mappings of BrAPI notions and OpenSILEX notions.


# Special service : GET BrAPI Calls

This service should return a list of the available services as well as their datatypes, methods and version. The expected result is :

```json
{
  "metadata": {
    "pagination": {
      "pageSize": 20,
      "currentPage": 0,
      "totalCount": 10,
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
        "call": "variables/{observationVariableDbId}",
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
        "call": "studies/{studyDbId}/observationunits",
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
        "call": "studies/{studyDbId}",
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
        "call": "studies/{studyDbId}/observationvariables",
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
        "call": "studies/{studyDbId}/observations",
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
        "call": "studies-search",
        "dataTypes": [
          "application/json"
        ],
        "methods": [
          "GET"
        ],
        "versions": [
          "1.2"
        ]
      }
    ]
  }
}
```

# Mapping between BrAPI V1.3 concepts and OpenSILEX concepts

<br>

## Variables

| BrAPIv1ObservationVariableDTO | OpenSILEX VariableModel | Notes |
|----|----|----|
| contextOfUse |    |    |
| crop | species when only one |    |
| defaultValue |    |    |
| documentationURL |    |    |
| growthStage |    |    |
| institution |    |    |
| language | nothing yet. Waiting for multilabels |    |
| method | method |    |
| ontologyReference |    |    |
| scale | unit/scale |    |
| scientist |    |    |
| status |    |    |
| submissionTimestamp |    |    |
| synonyms | alternativeName | can be extended once multilabels are available |
| trait | trait |    |
| xref | first exactMatch if available |    |
| observationVariableDbId | URI |    |
| observationVariableName | name |    |

### Methods

| BrAPIv1MethodDTO | OpenSILEX MethodModel | Notes |
|----|----|----|
| class	string	Method class (examples: "Measurement", "Counting", "Estimation", "Computation", etc. |    | We have something somewhat equivalent but on the provenance so it is more linked to the observation than directly to the method (that is part of the variable) |
| description	string	Method description. | description |    |
| formula	string	For computational methods i.e., when the method consists in assessing the trait by computing measurements, write the generic formula used for the calculation |    | No equivalent in OpenSILEX but would be useful |
| methodDbId	string	Method unique identifier | uri |    |
| methodName	string	Human readable name for the method | name |    |
| ontologyReference	object |    | Not mapped because opensilex has a more complex ontology reference (skos matches) and list of references |
| reference	string	Bibliographical reference describing the method. |    |    |

### Traits

__Note__ : this is used for *GET {studies}/observationVariables*, *GET variables* and *GET variables/{observationVariableDbId}*

| BrAPIv1TraitDTO | OpenSILEX VariableModel | Notes |
|----|----|----|
| alternativeAbbreviations	array\[string\]	Other frequent abbreviations of the trait, if any. These abbreviations do not have to follow a convention |    |    |
| attribute	string	A trait can be decomposed as "Trait" = "Entity" + "Attribute", the attribute is the observed feature (or characteristic) of the entity e.g., for "grain colour", attribute = "colour" | Characteristic name |    |
| class	string	Trait class. (examples: "morphological trait", "phenological trait", "agronomical trait", "physiological trait", "abiotic stress trait", "biotic stress trait", "biochemical trait", "quality traits trait", "fertility trait", etc.) |    |    |
| description	string	The description of a trait |    |    |
| entity	string	A trait can be decomposed as "Trait" = "Entity" + "Attribute", the entity is the part of the plant that the trait refers to e.g., for "grain colour", entity = "grain" | Entity name |    |
| mainAbbreviation	string	Main abbreviation for trait name. (examples: "Carotenoid content" => "CC") |    |    |
| ontologyReference	object |    |    |
| documentationLinks	array\[object\]	links to various ontology documentation |    |    |
| URL	string (uri) |    |    |
| type	string |    |    |
| ontologyDbId	string	Ontology database unique identifier |    |    |
| ontologyName	string	Ontology name |    |    |
| version	string	Ontology version (no specific format) |    |    |
| status	string	Trait status (examples: "recommended", "obsolete", "legacy", etc.) |    |    |
| synonyms	array\[string\]	Other trait names |    |    |
| traitDbId	string	The ID which uniquely identifies a trait | trait URI if exists |    |
| traitName	string	The human readable name of a trait | trait name if exist else Entity name + “_” + Characteristic name |    |
| xref	string	Cross reference of the trait to an external ontology or database term e.g., Xref to a trait ontology (TO) term |    |    |

### Scales

| BrAPIv1ScaleDTO | OpenSILEX UnitModel | Notes |
|----|----|----|
| dataType	string	Class of the scale, entries can be "Code" - This scale class is exceptionally used to express complex traits. Code is a nominal scale that combines the expressions of the different traits composing the complex trait. For exemple a severity trait might be expressed by a 2 digit and 2 character code. The first 2 digits are the percentage of the plant covered by a fungus and the 2 characters refer to the delay in development, e.g. "75VD" means "75%" of the plant is Crop Ontology & Integrated Breeding Platform Curation Guidelines 5/6/2016 9 infected and the plant is very delayed. "Date" - The date class is for events expressed in a time format, e.g. yyyymmddThh:mm:ssZ or dd/mm/yy "Duration" - The Duration class is for time elapsed between two events expressed in a time format, e.g. days, hours, months "Nominal" - Categorical scale that can take one of a limited and fixed number of categories. There is no intrinsic ordering to the categories "Numerical" - Numerical scales express the trait with real numbers. The numerical scale defines the unit e.g. centimeter, ton per hectar, branches "Ordinal" - Ordinal scales are scales composed of ordered categories "Text" - A free text is used to express the trait. | Extracted from the variable's dataType. With the following mapping : <br>xsd:decimal + xsd:integer -> Numerical <br> xsd:date + xsd:dateTime -> Date <br>xsd:string -> Text <br>xsd:boolean -> Nominal | This mapping is only possible when a variable is demanded as an isolated unit doesn't have a dataType in OpenSILEX. Only a variable has one. |
| decimalPlaces	integer	For numerical, number of decimal places to be reported |    |    |
| ontologyReference	object |    |    |
| scaleDbId	string	Unique identifier of the scale. If left blank, the upload system will automatically generate a scale ID. | uri |    |
| scaleName	string	Name of the scale | name |    |
| validValues	object |    |    |
| xref	string	Cross reference to the scale, for example to a unit ontology such as UO or to a unit of an external major database |    | There can be multiple in OpenSILEX (equivalent to skos:exactMatch) |

<br>

## Germplasm


__Note : only accessions are considered germplasms for BrAPI__

This is why an error is returned when this notion doesn't exist :

```java
return new BadRequestException(
        "The 'Accession' notion doesn't exist in your ontology so this service is unavailable"
);
```

| BrAPIv1GermplasmDTO | OpenSILEX GermplasmModel | Notes |
|----|----|----|
| accessionNumber | accessionNumber |    |
| acquisitionDate |    |    |
| biologicalStatusOfAccessionCode |    |    |
| breedingMethodDbId |    |    |
| commonCropName | species name |    |
| countryOfOriginCode |    |    |
| defaultDisplayName | name |    |
| documentationURL | website | Incomplete mapping |
| donors |    |    |
| germplasmDbId | uri |    |
| germplasmGenus |    |    |
| germplasmName | name |    |
| germplasmPUI | germplasm URI |    |
| germplasmSpecies | species name |    |
| instituteCode | institute Code |    |
| instituteName |    |    |
| pedigree |    |    |
| seedSource |    |    |
| species | species name |    |
| speciesAuthority |    |    |
| subtaxa | var. *variety* |    |
| subtaxaAuthority |    |    |
| synonyms | synonyms |    |
| taxonIds |    |    |
| typeOfGermplasmStorageCode |    |    |

<br>


## Studies

| BrAPIv1StudyDTO | OpenSILEX ExperimentModel | Notes |
|----|----|----|
| active	string	Is this study currently active | calculated from start/end dates and current date | Could make some cleanup because code is repeated in the frontend for active/inactive experiment |
| additionalInfo	object	Additional arbitrary info |    | Could add some unmapped info in there (Its a simple map string : string) |
| commonCropName	string	Common name for the crop associated with this study | Name of species if only one species used in the experiment | Could pick the first species associated with the study but incomplete because it's not the common crop name |
| documentationURL	string (uri)	A URL to the human readable documentation of this object |    | Could use URL of the detail page on the instance but hard to do |
| endDate	string (date)	The date the study ends | endDate |    |
| locationDbId	string	The ID which uniquely identifies a location | First  facility in returned facilities. A bit unstable (might return something different depending on facility order) | Treated differently in Study and Study details |
| locationName	string	The human readable name for a location |    | Same as above |
| name	string	DEPRECATED in v1.3 - Use "studyName" |    | Removed as it is deprecated |
| programDbId	string	The ID which uniquely identifies a program within the given database server | First Project if exists. Same as facilities, not perfect | incomplete as projects is a list in OpenSILEX |
| programName	string	The humane readable name of a program | same as above |    |
| seasons	array\[object\]	List of seasons over which this study was performed. | created a season for each year in the experiment if end date is defined | incomplete as OpenSILEX doesn't have this notion |
| startDate	string (date)	The date this study started | startDate |    |
| studyDbId	string	The ID which uniquely identifies a study within the given database server | uri |    |
| studyName	string	The humane readable name of a study | name |    |
| studyType	string	DEPRECATED in v1.3 - See "studyTypeName" |    | removed as it is deprecated |
| studyTypeDbId	string	The unique identifier of the type of study being performed. |    | Notion doesn't exist in OpenSILEX |
| studyTypeName	string	The name of the type of study being performed. ex. "Yield Trial", etc |    | see above |
| trialDbId	string	The ID which uniquely identifies a trial |    | Notion doesn't exist in OpenSILEX |
| trialName	string	The human readable name of a trial |    | see above |

Changes in the detailed results :

| BrAPIv1StudyDetailsDTO | OpenSILEX ExperimentModel | Notes |
|----|----|----|
| contacts	array\[object\]	List of contact entities associated with this study | Supervisors |    |
| dataLinks	array\[object\]	List of links to extra data files associated with this study. Extra data could include notes, images, and reference data. |    | Couldn't realy match this with anything.. Could be matched to datasets? |
| lastUpdate	object	The date and time when this study was last modified |    | Could be matched in the future with metadatas once dev is finished |
| license	string	The usage license associated with the study data |    | Could we associate a license to all public experiments? |
| location	object | first facility of experiment if exists | (same problem with n -> 1) |
| studyDescription	string	The description of this study | experiment description |    |

### Contacts

| BrAPIv1ContactDTO | OpenSILEX PersonModel | Notes |
|----|----|----|
| contactDbId	string	The ID which uniquely identifies this contact | Person URI |    |
| email	string	The contacts email address | Person email |    |
| instituteName	string	The name of the institution which this contact is part of |    | No equivalence (maybe organizations but same issue of n -> 1) |
| name	string	The full name of this contact person | Person last name to uppercase + person firstname with uppercase first letter |    |
| orcid	string	The Open Researcher and Contributor ID for this contact person (orcid.org) | Person ORCID |    |
| type	string	The type of person this contact represents (ex: Coordinator, Scientist, PI, etc.) | Person role in experiment |    |

### Locations

| BrAPIv1LocationDTO | OpenSILEX FacilityModel | Notes |
|----|----|----|
| abbreviation	string	An abbreviation which represents this location |    |    |
| additionalInfo	object	Additional arbitrary info |    |    |
| altitude	number	The altitude of this location |    | we usually don't have this info |
| countryCode	string	ISO_3166-1_alpha-3 spec | Code of the country of the facility's address or of the site of its first parent organization with one |    |
| countryName	string	The full name of the country where this location is | Same as above but name |    |
| documentationURL	string (uri)	A URL to the human readable documentation of this object |    |    |
| instituteAddress	string	The street address of the institute representing this location | Address of the site of the first parent organisation with only one |    |
| instituteName	string	each institute/laboratory can have several experimental field | Same as above but name |    |
| latitude	number	The latitude of this location | Latitude of centroid of facility geometry |    |
| locationDbId	string	string identifier | URI of facility |    |
| locationName	string	A human readable name for this location | Name of facility |    |
| locationType	string	The type of location this represents (ex. Breeding Location, Storage Location, etc) | URI of type of facility |    |
| longitude	number	the longitude of this location | Longitude of centroid of facility geometry |    |

<br>

## Observations

| BrAPIv1ObservationDTO | OpenSILEX DataModel | Notes |
|----|----|----|
| germplasmDbId	string	The ID which uniquely identifies a germplasm | URI of first germplasm of target if exists | same issue of n->1 |
| germplasmName	string	Name of the germplasm. It can be the prefered name and does not have to be unique. | same as above but name | same as above |
| observationDbId	string	The ID which uniquely identifies an observation | URI of the data |    |
| observationLevel	string	The level of an observation unit. ex. "plot", "plant" | type of the target |    |
| observationTimeStamp	string (date-time)	The date and time when this observation was made | date of the data |    |
| observationUnitDbId	string	The ID which uniquely identifies an observation unit | URI of target |    |
| observationUnitName	string	A human readable name for an observation unit | label of target |    |
| observationVariableDbId	string	The ID which uniquely identifies an observation variable | variable URI |    |
| observationVariableName	string	A human readable name for an observation variable | variable name |    |
| operator	string	The name or identifier of the entity which collected the observation |    | can't map properly as there can be multiple operators in OpenSILEX |
| season	object | mapped season year |    |
| studyDbId	string	The ID which uniquely identifies a study within the given database server | URI of the experiment |    |
| uploadedBy	string	The name or id of the user who uploaded the observation to the database system |    | Could be completed once dev on metadata is complete |
| value	string	The value of the data collected as an observation | data value |    |

<br>

## ObservationUnit

| BrAPIv1ObservationUnitDTO | OpenSILEX ScientificObjectDTO | Notes |
|----|----|----|
| X	string	DEPRECATED - use "positionCoordinateX" |    |    |
| Y	string	DEPRECATED - use "positionCoordinateY" |    |    |
| blockNumber	string	The block number for an observation unit. Different systems may use different block designs. |    |    |
| entryNumber	string	The entry number for an observation unit. Different systems may use different entry systems. |    |    |
| entryType	string	The type of entry for this observation unit. ex. "check", "test", "filler" |    |    |
| germplasmDbId	string	The ID which uniquely identifies a germplasm | SO germplasm if only one |    |
| observationLevel	string	The level of an observation unit. ex. "plot", "plant" | SO type |    |
| observationUnitDbId	string	The ID which uniquely identifies an observation unit | SO URI |    |
| observationUnitName	string	A human readable name for an observation unit | SO name |    |
| observationUnitXref	array\[object\]	A list of external references to this observation unit |    |    |
| observations	array\[object\]	List of observations associated with this observation unit | Data of the object |    |
| plantNumber	string	The plant number in a field. Applicable for observationLevel: "plant" |    |    |
| plotNumber	string	The plot number in a field. Applicable for observationLevel: "plot" |    |    |
| positionCoordinateX	string	The X position coordinate for an observation unit. Different systems may use different coordinate systems. | Long/lat of center of th Object if it has a geometry. If only one move event use long/lat of center facility, of center of geometry, X,Y | Textual position is ignore as no mapping is possible |
| positionCoordinateXType	string	The type of positional coordinate used. Must be one of the following values LONGITUDE - ISO 6709 standard, WGS84 geodetic datum. See "Location Coordinate Encoding" for details LATITUDE - ISO 6709 standard, WGS84 geodetic datum. See "Location Coordinate Encoding" for details PLANTED_ROW - The physical planted row number PLANTED_INDIVIDUAl - The physical counted number, could be independant or within a planted row GRID_ROW - The row index number of a square grid overlay GRID_COL - The column index number of a square grid overlay MEASURED_ROW - The distance in meters from a defined 0th row MEASURED_COL - The distance in meters from a defined 0th column | LONGITUDE or GRID_ROW depending on case above. By default set to LONGITUDE as null isn't allowed |    |
| positionCoordinateY	string	The Y position coordinate for an observation unit. Different systems may use different coordinate systems. | same as X coordinates |    |
| positionCoordinateYType	string	The type of positional coordinate used. Must be one of the following values LONGITUDE - ISO 6709 standard, WGS84 geodetic datum. See "Location Coordinate Encoding" for details LATITUDE - ISO 6709 standard, WGS84 geodetic datum. See "Location Coordinate Encoding" for details PLANTED_ROW - The physical planted row number PLANTED_INDIVIDUAl - The physical counted number, could be independant or within a planted row GRID_ROW - The row index number of a square grid overlay GRID_COL - The column index number of a square grid overlay MEASURED_ROW - The distance in meters from a defined 0th row MEASURED_COL - The distance in meters from a defined 0th column | LATITUDE or GRID_COL (see X coordinates type). By default set to LATITUDE as null isn't allowed |    |
| replicate	string	The replicate number of an observation unit. May be the same as blockNumber. |    |    |
| studyDbId	string	The ID which uniquely identifies a study within the given database server | experimentation URI |    |
| treatments	array\[object\]	List of treatments applied to an observation unit. | Object factor levels |    |
| trialDbId	string	The ID which uniquely identifies a trial |    |    |
| trialName	string	The human readable name of a trial |    |    |


# Known issues and limitations

For Observation Units positions the move events aren’t taken into account.

No tests are implemented. Testing was done using the [BrAPI-Validator](https://github.com/plantbreeding/IPK-BrAPI-Validator) in a modifyied version to allow for the use of special characters in DbIds. This is needed because we use URIs, that have special characters, as DbIds. These changes couldn’t be added to the main repo yet. So for now this version is available [here](https://forge.inrae.fr/OpenSILEX/opensilex-brapi-validator).

An additional step would be to use this app in our gitlab-ci for automated testing of these services.