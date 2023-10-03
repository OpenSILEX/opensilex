******
* This document follows the Specification Guidelines draft (as of 2023-06-08)
* Author: yvan.roux@inrae.fr
* Date: 2023-04-09
******

# Specifications: OrcidClient

This document briefly describes the functional and technical specifications of `OrcidClient.java`.

## Context

ORCID is an international initiative with the purpose of giving a unique ID to researchers in order to identify them in official publications. For further information, see the [official website](https://orcid.org).

## Needs

The purpose of this class is to enhance interoperability of the OpenSilex API, specifically with the ORCID API. The first functionality we decided to implement is to pre-fill the person form with the information found on the ORCID API. This functionality allows users to create person objects easier and faster if the person has an ORCID record.

## Technical specifications

### Resources

Documentation about the ORCID API is available on the official [GitHub Orcid API page](https://github.com/ORCID/ORCID-Source/tree/main/orcid-api-web). Because we are using the public API, I conducted manual tests to ensure that there are no major restrictions on the number of requests we are allowed to make. These tests were able to make more than 15,000 successful requests in one hour. The test was manually stopped because the results were satisfying.

### Detailed explanations

The `PersonAPI` class contains 1 new webservice: `getOrcidRecord(orcid)`, which returns the information found on the ORCID API about the person who holds the "orcid" ID. It includes only information that can be used to create a person.

The `ORCIDClient` class is responsible for making requests to the ORCID API, with the correct URL and parameters. Currently, it only uses the public ORCID API, so authentication is not required to make requests. This class returns a boolean or a JsonObject extracted from the response.

The `OrcidRecordDTO` class is the DTO used by the `getOrcidRecord` webservice. It has a constructor that accepts a JsonObject parameter to instantiate the DTO with the data of an ORCID record.

### Detailed behaviour

If the ORCID servers are not working correctly  :
- Creating a person with an ORCID is impossible because we are unable to verify if the ORCID actually exists.
- The service will return a BadGateway error response with detailed error information.