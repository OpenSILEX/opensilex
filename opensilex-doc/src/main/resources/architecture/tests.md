

# Tests in OpenSILEX

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 09/02/2024 | Gabriel Besombes | 1.2               | Document creation |

> ⚠️ _WARNING_ : This document is incomplete ! You can help by expanding it. ⚠️
>
> Currently covered topics :
>
> - {Test classes}
>
> Missing topics :
>
> - Framework and libraries
> - Maven

## Table of contents

<!-- TOC -->
* [Tests in OpenSILEX](#tests-in-opensilex)
  * [Table of contents](#table-of-contents)
  * [Framework and libraries](#framework-and-libraries)
  * [Maven](#maven)
  * [Test classes](#test-classes)
    * [Base abstract classes](#base-abstract-classes)
    * [Inner classes for calls](#inner-classes-for-calls)
    * [Helper methods](#helper-methods)
    * [Usage examples](#usage-examples)
<!-- TOC -->

## Framework and libraries

## Maven

## Test classes

### Base abstract classes
The main classes for tests are these three abstract classes (in inheritance order) :
* `AbstractIntegrationTest`
* `AbstractSecurityIntegrationTest`
* `AbstractMongoIntegrationTest`

Your test classes should inherit from one of these. In most cases `AbstractSecurityIntegrationTest` 
(if you only need triple-store access) or `AbstractMongoIntegrationTest` if you need triplestore and mongo access.
Most of the logic is located in `AbstractIntegrationTest` and `AbstractSecurityIntegrationTest` inside inner classes.

### Inner classes for calls

* `PublicCall` is used to make calls without authentication (rarely called directly)
  * `executeCall()` will execute the call and return the raw `Response` object. You will need to handle the fact that it is an `AutoClosable`.
  * `executeCallAndAssertStatus(Status)` will execute the call and check the response's actual status against the one given as parameter.
  * `executeCallAndDeserialize(TypeReference<T> typeReference)` will execute the call, check that the response's status is 
  in the 200 - 299 range, deserialize the response to the type given by the user and return a `Result` object.
  * `executeCallAndReturnURI()` will do the same as the previous one but deserialize to an `ObjectUriResponse` for 
  PUT, POST, DELETE or use custom classes otherwise : `OpensilexResponseDeserialize<UriResourceDTO>`. It will only return 
  the URI contained in the response. __WARNING__ : This obviously won't work if the result of the response contains a list 
  instead of a single object -> this could be added later on?
* `PublicCallBuilder` is the class used to build `PublicCall` objects.
  * `Map<String, Object> params` contains the path parameters (OPTIONAL : for searches mainly)
  * `Object body` contains the body parameter (OPTIONAL : for POST and PUT mainly)
  * `Map<String, Object> pathTemplateParams` contains the parameters to replace in the path template and the value to
  * replace them with. (OPTIONAL : for GET by URI, DELETE mainly to replace "uri" in the path template by the actual URI of the resource)
  * `Method serviceMethod` java method that corresponds to the service
  * `String pathTemplate` path template for the service
  * `String httpMethod` http method of the service (Calculated automatically by the class)
  * `MediaType callMediaType` media type of the call (Defaults to APPLICATION_JSON)
  * `List<MediaType> responseMediaTypes` media type of the response (OPTIONAL : mainly used for file streaming in exports services)

* `UserCall` inherits from `PublicCall`. Used to make calls with authentication (authenticates automatically if necessary and registers the token in the token map)
  * `String userEmail` email of the user
  * `String userPassword` password of the user
* `UserCallBulder` inherits from `PublicCallBuilder` and has the same use.
  * `buildAdmin()` builds a `UserCall` with the admin credentials.

* `Result` a class that represents the result of a call with both the `Response` object (closed) and the deserialized response.
* `ServiceDescription` a class that describes a webservice with both its path and the corresponding java method.

### Helper methods

Most helper methods are in the `AbstractSecurityIntegrationTest` class :
* `convertToNotNullNestedMap(Object object)` converts an object's attributes to a map and list representation in a recursive 
  manner while removing all null and empty attributes. (Don't use directly unless you have a specific case that differs from the use of `testBasicCRUDAsAdmin`)
* `isDeepMapIncluded(LinkedHashMap<String, Object> superMap, LinkedHashMap<String, Object> subMap)` check if a map is included in another map.
  Meant to be used in conjunction with the previous method. (Don't use directly unless you have a specific case that differs from the use of `testBasicCRUDAsAdmin`)
* `testBasicCreateAsAdmin(ServiceDescription createServiceDescription, ResourceDTO<?> entityToPost)` Tests basic creation
  of a resource, asserts success, parses and returns the uri of the created resource.
* `testBasicReadAsAdmin(ServiceDescription readServiceDescription, TypeReference<T> entityTypeReference, URI resourceUri)`
  Tests basic read of a resource, asserts success and return the response deserialized to the type given in the entityTypeReference parameter.
* `testBasicUpdateAsAdmin(ServiceDescription readServiceDescription, ServiceDescription updateServiceDescription, ResourceDTO<?> entityToPut, TypeReference<T> entityTypeReference)`
  Tests basic update of a resource, asserts success, compares the expected and actual arguments of the given `entityToPut` and
  response using `convertToNotNullNestedMap` and `isDeepMapIncluded` to ensure that all the arguments that were set in
  `entityToPut` are the same in the response (null and empty arguments are ignored).
* `testBasicDeleteAsAdmin(ServiceDescription readServiceDescription, ServiceDescription deleteServiceDescription, URI resourceUri)`
  Tests basic delete of a resource, asserts success, reads the resource again and asserts not found.
* `testBasicCRUDAsAdmin` Tests basic CRUD by calling the four previous methods. __\[Use this method instead of the previous four
  unless you have a different use case\]__

__WARNING__ : Don't just test the basic CRUD. Add other pertinent tests for your feature or bugfix.

### Usage examples

You can find usage examples in the following test classes :
* `AreaAPITest` examples of calls using `UserCall`
* `AuthenticationAPITest` examples of public calls
* `GroupAPITest` examples of tests using `testBasicCRUDAsAdmin`
