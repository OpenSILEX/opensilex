//**********************************************************************************************
//                                       UriResourceService.java 
//
// Author(s): Eloan LAGIER, Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 26 Feb 2018
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  20 June, 2018
// Subject: Represents the Uri Resource Service
//***********************************************************************************************
package phis2ws.service.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.sesame.UriDaoSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Ask;
import phis2ws.service.view.model.phis.Uri;

@Api("/uri")
@Path("uri")
/**
 * Represents the Uri Resource Service
 *
 * @author Eloan LAGIER
 */
public class UriResourceService extends ResourceService {
    /**
     * search if an uri is in the triplestore or not
     *
     * @param uri
     * @return a response which contains 
     *         true if the uri exist, 
     *         false if it is an unknown uri
     */
    @GET
    @Path("{uri}/exist")
    @ApiOperation(value = "check if an uri exists or not (in the triplestore)",
            notes = "Return a boolean")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "exist result", response = Ask.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response isUriExisting(
            @ApiParam(value = DocumentationAnnotation.CONCEPT_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_CONCEPT_URI) @PathParam("uri") @URL @Required String uri) {

        UriDaoSesame uriDaoSesame = new UriDaoSesame();
        if (uri != null) {
            uriDaoSesame.uri = uri;
        }

        uriDaoSesame.user = userSession.getUser();

        return existData(uriDaoSesame);
    }

    /**
     * this GET return all the concept metadata
     *
     * @param limit
     * @param page
     * @param uri
     * @return concept list. The result form depends on the query results 
     * e.g. of result : 
     * { 
     *      "metadata": 
     *          { "pagination": null, 
     *            "status": [],
     *            "datafiles": [] 
     *          }, 
     *          "result": { 
     *              "data": [ 
     *                  { "uri": "http://www.opensilex.org/vocabulary/oeso#Document",
     *                    "infos": { 
     *                          "type":"http://www.w3.org/2002/07/owl#Class",
     *                          "label_en": "document",
     *                          "label_fr": "document" 
     *                      } 
     *                  } 
     *              ] 
     *          } 
     * }
     */
    @GET
    @Path("{uri}")
    @ApiOperation(value = "Get all uri informations",
            notes = "Retrieve all infos of the uri")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all concept informations", response = Uri.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })

    @Produces(MediaType.APPLICATION_JSON)
    public Response getUriMetadata(
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
            @ApiParam(value = "Search by uri", required = true, example = DocumentationAnnotation.EXAMPLE_CONCEPT_URI) @PathParam("uri") @URL @Required String uri) {

        UriDaoSesame uriDaoSesame = new UriDaoSesame();
        if (uri != null) {
            uriDaoSesame.uri = uri;
        }

        uriDaoSesame.user = userSession.getUser();
        uriDaoSesame.setPage(page);
        uriDaoSesame.setPageSize(limit);

        return getUriMetadata(uriDaoSesame);
    }

    /**
     * search all uri with the label given
     *
     * @param limit
     * @param page
     * @param label
     * @return Response
     */
    @GET
    @ApiOperation(value = "get all uri with a given label",
            notes = "Retrieve all uri from the label given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all labels", response = Uri.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })

    @Produces(MediaType.APPLICATION_JSON)
    public Response getUrisByLabel(
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
            @ApiParam(value = "Search by label", required = true, example = DocumentationAnnotation.EXAMPLE_CONCEPT_LABEL) @QueryParam("label") @Required String label) {

        UriDaoSesame uriDaoSesame = new UriDaoSesame();
        if (label != null) {
            uriDaoSesame.label = label;
        }

        uriDaoSesame.user = userSession.getUser();
        uriDaoSesame.setPage(page);
        uriDaoSesame.setPageSize(limit);

        return getLabelMetaData(uriDaoSesame);
    }

    /**
     * Get all the instances of an uri
     * @param uri
     * @param deep verify subclass or not
     * @param limit
     * @param page
     * @update Arnaud Charleroy (18/07/2018): change deep string type to real boolean type
     * @return the query result, with the list of the instances or the errors
     */
    @GET
    @Path("{uri}/instances")
    @ApiOperation(value = "Get all the instances of a concept",
            notes = "Retrieve all instances of subClass too, if deep = true")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve instances from a concept", response = Uri.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInstancesByConcept(
            @ApiParam(value = DocumentationAnnotation.CONCEPT_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_CONCEPT_URI) @Required @URL @PathParam("uri") String uri,
            @ApiParam(value = DocumentationAnnotation.DEEP) @QueryParam("deep") @DefaultValue(DocumentationAnnotation.EXAMPLE_DEEP) Boolean deep, 
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {

        UriDaoSesame uriDaoSesame = new UriDaoSesame();
        if (uri != null) {
            uriDaoSesame.uri = uri;
        }

        if (deep != null) {
            uriDaoSesame.deep = deep;
        } else {
            uriDaoSesame.deep = true;
        }

        uriDaoSesame.setPageSize(limit);
        uriDaoSesame.setPage(page);
        uriDaoSesame.user = userSession.getUser();

        return getInstancesData(uriDaoSesame);
    }

    /**
     * give all the class parent of a given uri
     *
     * @param uri
     * @param limit
     * @param page
     * @return the result. 
     * Example :
     * { 
     *      "metadata": 
     *          { 
     *              "pagination": null, 
     *              "status": [], 
     *              "datafiles": []
     *          },
     *      "result": 
     *          { 
     *              "data": [ 
     *                  { "uri": "http://www.opensilex.org/vocabulary/oeso#Document",
     *                    "properties": {} 
     *                  }
     *              ] 
     *          } 
     * }
     */
    @GET
    @Path("{uri}/ancestors")
    @ApiOperation(value = "Get all the ancestor of an uri",
            notes = "Retrieve all Class parents of the uri")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve Concept", response = Uri.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAncestors(
            @ApiParam(value = DocumentationAnnotation.CONCEPT_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_CONCEPT_URI) @PathParam("uri") @URL @Required String uri,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {

        UriDaoSesame uriDaoSesame = new UriDaoSesame();
        if (uri != null) {
            uriDaoSesame.uri = uri;
        }
        uriDaoSesame.setPageSize(limit);
        uriDaoSesame.setPage(page);
        uriDaoSesame.user = userSession.getUser();

        return getAncestorsMetaData(uriDaoSesame);
    }

    /**
     * give all the concepts with the same parent
     *
     * @param uri
     * @param limit
     * @param page
     * @return 
     * Example : 
     * { 
     *      "metadata": { 
     *          "pagination": { 
     *              "pageSize": 20,
     *              "currentPage": 0,
     *              "totalCount": 11,
     *              "totalPages": 1 
     *          }, 
     *          "status": [], 
     *          "datafiles": [] 
     *      },
     *      "result": { 
     *          "data": [ 
     *              { "uri": "http://www.opensilex.org/vocabulary/oeso#DataFile",
     *                "properties": {}
     *              },
     *              { "uri": "http://www.opensilex.org/vocabulary/oeso#TechnicalDocument",
     *                "properties": {} 
     *              } 
     *          ] 
     *      } 
     * }
     */
    @GET
    @Path("{uri}/siblings")
    @ApiOperation(value = "Get all the siblings of an Uri",
            notes = "Retrieve all Class with same parent")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve Concept", response = Uri.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "Authorization", required = true,
                dataType = "string", paramType = "header",
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSibblings(
            @ApiParam(value = DocumentationAnnotation.CONCEPT_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_SIBLING_URI) @PathParam("uri") @URL @Required String uri,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {

        UriDaoSesame uriDaoSesame = new UriDaoSesame();
        if (uri != null) {
            uriDaoSesame.uri = uri;
        }
        uriDaoSesame.setPageSize(limit);
        uriDaoSesame.setPage(page);
        uriDaoSesame.user = userSession.getUser();

        return getSiblingsMetaData(uriDaoSesame);
    }

    /**
     * search all descendants of a given uri
     *
     * @param uri
     * @param limit
     * @param page
     * @return 
     * { 
     *      "metadata": { 
     *          "pagination": { 
     *              "pageSize": 20, 
     *              "currentPage": 0,
     *              "totalCount": 12, 
     *              "totalPages": 1 
     *          },
     *          "status": [], 
     *          "datafiles": [] 
     *      },
     *      "result": { 
     *          "data": [ 
     *              { "uri": "http://www.opensilex.org/vocabulary/oeso#Document",
     *                "properties": {}
     *              }, 
     *              { "uri": "http://www.opensilex.org/vocabulary/oeso#DataFile",
     *                "properties": {} 
     *              } 
     *          ] 
     *      } 
     * }
     */
    @GET
    @Path("{uri}/descendants")
    @ApiOperation(value = "Get all the descendants of an uri",
            notes = "Retrieve all subclass and the subClass of subClass too")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve uri's descendants", response = Uri.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDescendants(
            @ApiParam(value = DocumentationAnnotation.CONCEPT_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_CONCEPT_URI) @PathParam("uri") @URL @Required String uri,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {

        UriDaoSesame uriDaoSesame = new UriDaoSesame();
        if (uri != null) {
            uriDaoSesame.uri = uri;
        }
        uriDaoSesame.setPageSize(limit);
        uriDaoSesame.setPage(page);
        uriDaoSesame.user = userSession.getUser();

        return getDescendantsMetaData(uriDaoSesame);
    }

    /**
     * return the type of an uri if exist else return empty list
     *
     * @param uri
     * @return false or type of the uri
     */
    @GET
    @Path("{uri}/type")
    @ApiOperation(value = "get the type of an uri if it exist")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve the uri existance", response = Uri.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })

    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTypeIfUriExist(
            @ApiParam(value = DocumentationAnnotation.CONCEPT_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_CONCEPT_URI) @PathParam("uri") @URL @Required String uri) {

        UriDaoSesame uriDaoSesame = new UriDaoSesame();
        if (uri != null) {
            uriDaoSesame.uri = uri;
        }

        uriDaoSesame.user = userSession.getUser();

        return getUriType(uriDaoSesame);
    }

    /**
     * collect all the data for the instances request
     *
     * @param uriDaoSesame
     * @return Response
     */
    private Response getInstancesData(UriDaoSesame uriDaoSesame) {
        ArrayList<Uri> uris;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Uri> getResponse;

        uris = uriDaoSesame.instancesPaginate();
        if (uris == null) { //no result found
            getResponse = new ResultForm<Uri>(0, 0, uris, true);
            return noResultFound(getResponse, statusList);
        } else if (!uris.isEmpty()) { //result founded
            getResponse = new ResultForm<Uri>(uriDaoSesame.getPageSize(), uriDaoSesame.getPage(), uris, false);
            if (getResponse.getResult().dataSize() == 0) { //no result found
                return noResultFound(getResponse, statusList);
            } else { //return instances metadata
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else { //no result found
            getResponse = new ResultForm<Uri>(0, 0, uris, true);
            return noResultFound(getResponse, statusList);
        }
    }

    /**
     * use the uriDaoSesame to check if the uris exist or not and return the 
     * formatted result
     *
     * @param uriDaoSesame
     * @return Response the result, containing the existing of each uri
     */
    private Response existData(UriDaoSesame uriDaoSesame) {
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Ask> getResponse;
        ArrayList<Ask> ask = uriDaoSesame.askUriExistance();
        
        if (ask == null) {//no result found
            getResponse = new ResultForm<Ask>(0, 0, ask, true);
            return noResultFound(getResponse, statusList);
        } else if (!ask.isEmpty()) {// result founded
            getResponse = new ResultForm<Ask>(uriDaoSesame.getPageSize(), uriDaoSesame.getPage(), ask, false);
            if (getResponse.getResult().dataSize() == 0) { //no result 
                return noResultFound(getResponse, statusList);
            } else { // return ask metadata
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else { //no result found
            getResponse = new ResultForm<Ask>(0, 0, ask, true);
            return noResultFound(getResponse, statusList);
        }
    }

    /**
     * get the metadata of a given uri. 
     * The uri can be a concept uri or an instance
     * @param uriDaoSesame
     * @return Response
     */
    private Response getUriMetadata(UriDaoSesame uriDaoSesame) {
        ArrayList<Uri> uris;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Uri> getResponse;

        uris = uriDaoSesame.allPaginate();

        if (uris == null) { //no result found
            getResponse = new ResultForm<Uri>(0, 0, uris, true);
            return noResultFound(getResponse, statusList);
        } else if (!uris.isEmpty()) { //concepts founded
            getResponse = new ResultForm<Uri>(uriDaoSesame.getPageSize(), uriDaoSesame.getPage(), uris, false);
            if (getResponse.getResult().dataSize() == 0) { // no result found
                return noResultFound(getResponse, statusList);
            } else { //return concepts metadata
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else { //no result found
            getResponse = new ResultForm<Uri>(0, 0, uris, true);
            return noResultFound(getResponse, statusList);
        }
    }

    /**
     * return the list of uris which has the given label
     *
     * @param uriDaoSesame collect all the Label data
     */
    private Response getLabelMetaData(UriDaoSesame uriDaoSesame) {
        ArrayList<Uri> uris;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Uri> getResponse;

        uris = uriDaoSesame.labelsPaginate();
        if (uris == null) {
            getResponse = new ResultForm<Uri>(0, 0, uris, true);
            return noResultFound(getResponse, statusList);
        } else if (!uris.isEmpty()) {
            getResponse = new ResultForm<Uri>(uriDaoSesame.getPageSize(), uriDaoSesame.getPage(), uris, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResultForm<Uri>(0, 0, uris, true);
            return noResultFound(getResponse, statusList);

        }
    }

    /**
     * collect all the ancestors of a given uri
     *
     * @param uriDaoSesame
     * @return Response
     */
    private Response getAncestorsMetaData(UriDaoSesame uriDaoSesame) {
        ArrayList<Uri> concepts;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Uri> getResponse;

        concepts = uriDaoSesame.ancestorsAllPaginate();
        if (concepts == null) { //no result found
            getResponse = new ResultForm<Uri>(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        } else if (!concepts.isEmpty()) { //result founded
            getResponse = new ResultForm<Uri>(uriDaoSesame.getPageSize(), uriDaoSesame.getPage(), concepts, false);
            if (getResponse.getResult().dataSize() == 0) { //no result found
                return noResultFound(getResponse, statusList);
            } else { //return concepts metadata
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else { // no result found
            getResponse = new ResultForm<Uri>(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        }
    }

    /**
     * collect all the siblings of a given uri
     *
     * @param uriDaoSesame
     * @return Response
     */
    private Response getSiblingsMetaData(UriDaoSesame uriDaoSesame) {
        ArrayList<Uri> concepts;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Uri> getResponse;

        concepts = uriDaoSesame.siblingsAllPaginate();
        if (concepts == null) { //no result found
            getResponse = new ResultForm<Uri>(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        } else if (!concepts.isEmpty()) { //result founded
            getResponse = new ResultForm<Uri>(uriDaoSesame.getPageSize(), uriDaoSesame.getPage(), concepts, false);
            if (getResponse.getResult().dataSize() == 0) { //no result founded
                return noResultFound(getResponse, statusList);
            } else { //return concepts metadata

                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else { //no result found
            getResponse = new ResultForm<Uri>(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        }
    }

    /**
     * collect all the descendants of a given uri
     *
     * @param uriDaoSesame
     * @return Response
     */
    private Response getDescendantsMetaData(UriDaoSesame uriDaoSesame) {
        ArrayList<Uri> concepts;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Uri> getResponse;

        concepts = uriDaoSesame.descendantsAllPaginate();
        if (concepts == null) { //no result found
            getResponse = new ResultForm<Uri>(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        } else if (!concepts.isEmpty()) { //concept founded 
            getResponse = new ResultForm<Uri>(uriDaoSesame.getPageSize(), uriDaoSesame.getPage(), concepts, false);
            if (getResponse.getResult().dataSize() == 0) { // no result found
                return noResultFound(getResponse, statusList);
            } else {//return concepts metadata
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else { // no result found
            getResponse = new ResultForm<Uri>(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        }
    }

    /**
     * get the type of a given uri
     *
     * @param uriDaoSesame
     * @return Response
     */
    private Response getUriType(UriDaoSesame uriDaoSesame) {
        ArrayList<Uri> uris;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Uri> getResponse;
        uris = uriDaoSesame.getAskTypeAnswer();
        
        if (uris == null) {//no result found
            getResponse = new ResultForm<Uri>(0, 0, uris, true);
            return noResultFound(getResponse, statusList);
        } else if (!uris.isEmpty()) { //result founded
            getResponse = new ResultForm<Uri>(uriDaoSesame.getPageSize(), uriDaoSesame.getPage(), uris, false);
            if (getResponse.getResult().dataSize() == 0) {//no result found
                return noResultFound(getResponse, statusList);
            } else { //return meta data
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {//no result found
            getResponse = new ResultForm<Uri>(0, 0, uris, true);
            return noResultFound(getResponse, statusList);
        }
    }
}
