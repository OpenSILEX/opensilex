OpenSILEX REST API - FAQ
================================================================================

# What is a REST API ?
--------------------------------------------------------------------------------

// TODO LIEN

OpenSILEX REST API is extensible with Jersey - JAX-RS annotations mechanism // TODO LIEN

OpenSILEX REST API is self-documented with Swagger V1.5 annotations // TODO LIEN


# How to add a Web Service to a module REST API ?
--------------------------------------------------------------------------------

1. Enable API Extension mechanism

Edit your module class which extends "OpenSilexModule" and make it implements "APIExtension" like in the following example:
```
package {modulePackagePrefix};

import org.opensilex.module.OpenSilexModule;
import org.opensilex.module.extensions.APIExtension;

/**
 * My module implementation
 */
public class MyModule extends OpenSilexModule implements APIExtension {

}
```

2. Create an API class

Create a package like {modulePackagePrefix}.api, if you plan to have multiple API classes for various concepts, you should create a subpackage for each one of them like {modulePackagePrefix}.api.myConcept

You must also define a {ApiPath} which will be the root URL access to your API entry point.

Then in this package create a class suffixed by "API" and implementing "RestApplicationAPI" like in the example bellow:
```
package {modulePackagePrefix}.api.myConcept;

import org.opensilex.server.rest.RestApplicationAPI;

@Api("{ApiPath}")
@Path("{ApiPath}")
public class MyConceptAPI implements RestApplicationAPI {

}
```

3. Create DTO classes 

DTO design pattern is used to separate view representation from model internal structure.

View (https://en.wikipedia.org/wiki/Data_transfer_object)[https://en.wikipedia.org/wiki/Data_transfer_object] for more informations on this pattern.

A DTO class is a simple POJA which contains only fields, getters, setters and eventually mechanism to convert them from/to models classes.

This kind of class should be suffixed with "DTO" to identify them easily.

So you will have to create your own DTO for your service inputs and outputs (if necessary).

In this example we will assume two DTO classes MyWebServiceInputDTO and MyWebServiceOutputDTO, located in the corresponding API folder.

4. Create the web service method

In the API class, add a method called by example "myWebService" with all parameters you need and returning a "Response" object.

Add Jersey JAX-RS annotations to define your REST service, by example:

- @GET/@POST/@PUT/@DELETE: To define which HTTP verb an incoming request must have to be redirected to your web service
- @Path: To define on which sub URL your web service can be accessed (cumulate with preivous @Path annotation defined at class level)
- @Consumes: To define which format will be accepted on input, by example MediaType.APPLICATION_JSON
- @Produces: To define which format will returned, by example MediaType.APPLICATION_JSON

For more information about Jersey JAX-RS annotations and mechanism please see (https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/jaxrs-resources.html)[https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/jaxrs-resources.html]

You also need to add Swagger V1.5.X Annotations to have your web service beeing well described in Swagger IHM.

For details on Swagger annotations see (https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X)[https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X]

You should end with something similar to the following example:
```
package {modulePackagePrefix};

import org.opensilex.module.OpenSilexModule;
import org.opensilex.module.extensions.APIExtension;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * My module implementation
 */
public class MyModule extends OpenSilexModule implements APIExtension {

    @Inject

    @POST
    @Path("{my-web-service}")
    @ApiOperation(value = "Do something",
                  notes = "Do something but with more details")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = MyWebServiceOutputDTO.class),
        @ApiResponse(code = 500, message = "Internal error")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response myWebService(
        @ApiParam(value = "My input parameter description") @Valid MyWebServiceInputDTO input) {
           
	MyWebServiceOutputDTO output;

        ...
        
        return Response.ok(output).build();
    }

}
```
