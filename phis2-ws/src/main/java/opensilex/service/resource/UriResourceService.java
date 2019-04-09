//******************************************************************************
//                             UriResourceService.java 
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 26 Feb 2018
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource;

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
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.UriDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.view.brapi.Status;
import opensilex.service.result.ResultForm;
import opensilex.service.model.Ask;
import opensilex.service.model.Uri;

/**
 * URI resource service.
 * @author Eloan Lagier
 */
@Api("/uri")
@Path("uri")
public class UriResourceService extends ResourceService {
    
    /**
     * Searches if a URI exists.
     * @param uri
     * @return a response which contains 
     *         true if the URI exist, 
     *         false if it is an unknown URI
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
            @ApiParam(value = DocumentationAnnotation.CONCEPT_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_CONCEPT_URI) 
                @PathParam("uri") 
                @URL 
                @Required String uri) {

        UriDAO uriDao = new UriDAO();
        if (uri != null) {
            uriDao.uri = uri;
        }

        uriDao.user = userSession.getUser();

        return existData(uriDao);
    }

    /**
     * Returns the concept's metadata.
     * @param limit
     * @param page
     * @param uri
     * @return concept list. The result form depends on the query results 
     * @example
     * result : 
     * { 
     *    "metadata": 
     *    { "pagination": null, 
     *      "status": [],
     *      "datafiles": [] 
     *    }, 
     *    "result": { 
     *        "data": [ 
     *            { "uri": "http://www.opensilex.org/vocabulary/oeso#Document",
     *              "infos": { 
     *                    "type":"http://www.w3.org/2002/07/owl#Class",
     *                    "label_en": "document",
     *                    "label_fr": "document" 
     *                } 
     *            } 
     *        ] 
     *    } 
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

    /**
     * Gets a URI's metadata.
     */
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUriMetadata(
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
            @ApiParam(value = "Search by uri", required = true, example = DocumentationAnnotation.EXAMPLE_CONCEPT_URI) @PathParam("uri") @URL @Required String uri) {

        UriDAO uriDao = new UriDAO();
        if (uri != null) {
            uriDao.uri = uri;
        }

        uriDao.user = userSession.getUser();
        uriDao.setPage(page);
        uriDao.setPageSize(limit);

        return getUriMetadata(uriDao);
    }

    /**
     * Searches URIs with the label given.
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

        UriDAO uriDao = new UriDAO();
        if (label != null) {
            uriDao.label = label;
        }

        uriDao.user = userSession.getUser();
        uriDao.setPage(page);
        uriDao.setPageSize(limit);

        return getLabelMetaData(uriDao);
    }

    /**
     * Gets all the instances of an URI.
     * @param uri
     * @param deep verify subclass or not
     * @param limit
     * @param page
     * @update [Arnaud Charleroy] 18 Jul. 2018: change deep string type to real 
     * boolean type
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

        UriDAO uriDao = new UriDAO();
        if (uri != null) {
            uriDao.uri = uri;
        }

        if (deep != null) {
            uriDao.deep = deep;
        } else {
            uriDao.deep = true;
        }

        uriDao.setPageSize(limit);
        uriDao.setPage(page);
        uriDao.user = userSession.getUser();

        return getInstancesData(uriDao);
    }

    /**
     * Gives all the parent class of a given URI.
     * @param uri
     * @param limit
     * @param page
     * @return the result
     * @example
     * { 
     *    "metadata": 
     *    { 
     *       "pagination": null, 
     *       "status": [], 
     *       "datafiles": []
     *    },
     *    "result": 
     *    { 
     *          "data": [ 
     *            { 
     *              "uri": "http://www.opensilex.org/vocabulary/oeso#Document",
     *              "properties": {} 
     *            }
     *          ] 
     *    } 
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

        UriDAO uriDao = new UriDAO();
        if (uri != null) {
            uriDao.uri = uri;
        }
        uriDao.setPageSize(limit);
        uriDao.setPage(page);
        uriDao.user = userSession.getUser();

        return getAncestorsMetaData(uriDao);
    }

    /**
     * Gives all the concepts with the same parent.
     * @param uri
     * @param limit
     * @param page
     * @return 
     * @example
     * { 
     *   "metadata": { 
     *     "pagination": { 
     *       "pageSize": 20,
     *       "currentPage": 0,
     *       "totalCount": 11,
     *       "totalPages": 1 
     *     }, 
     *     "status": [], 
     *     "datafiles": [] 
     *   },
     *   "result": { 
     *   "data": [ 
     *       { "uri": "http://www.opensilex.org/vocabulary/oeso#DataFile",
     *        "properties": {}
     *      },
     *      { 
     *        "uri": "http://www.opensilex.org/vocabulary/oeso#TechnicalDocument",
     *        "properties": {} 
     *      } 
     *    ] 
     *   } 
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

        UriDAO uriDao = new UriDAO();
        if (uri != null) {
            uriDao.uri = uri;
        }
        uriDao.setPageSize(limit);
        uriDao.setPage(page);
        uriDao.user = userSession.getUser();

        return getSiblingsMetaData(uriDao);
    }

    /**
     * Searches all descendants of a given URI.
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
     *        "data": [ 
     *            { 
     *              "uri": "http://www.opensilex.org/vocabulary/oeso#Document",
     *              "properties": {}
     *            }, 
     *            { 
     *              "uri": "http://www.opensilex.org/vocabulary/oeso#DataFile",
     *              "properties": {} 
     *            } 
     *        ] 
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

        UriDAO uriDao = new UriDAO();
        if (uri != null) {
            uriDao.uri = uri;
        }
        uriDao.setPageSize(limit);
        uriDao.setPage(page);
        uriDao.user = userSession.getUser();

        return getDescendantsMetaData(uriDao);
    }

    /**
     * Returns the type of an URI if exist else return empty list.
     * @param uri
     * @return false or type of the URI
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

        UriDAO uriDao = new UriDAO();
        if (uri != null) {
            uriDao.uri = uri;
        }

        uriDao.user = userSession.getUser();

        return getUriType(uriDao);
    }

    /**
     * Collects all the data for the instances request.
     * @param uriDao
     * @return Response
     */
    private Response getInstancesData(UriDAO uriDao) {
        ArrayList<Uri> uris;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Uri> getResponse;

        uris = uriDao.instancesPaginate();
        if (uris == null) { //no result found
            getResponse = new ResultForm<>(0, 0, uris, true);
            return noResultFound(getResponse, statusList);
        } else if (!uris.isEmpty()) { //result founded
            getResponse = new ResultForm<>(uriDao.getPageSize(), uriDao.getPage(), uris, false);
            if (getResponse.getResult().dataSize() == 0) { //no result found
                return noResultFound(getResponse, statusList);
            } else { //return instances metadata
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else { //no result found
            getResponse = new ResultForm<>(0, 0, uris, true);
            return noResultFound(getResponse, statusList);
        }
    }

    /**
     * Checks if the URIs exist or not and returns the formatted result.
     * @param uriDao
     * @return Response the result, containing the existing of each URI
     */
    private Response existData(UriDAO uriDao) {
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Ask> getResponse;
        ArrayList<Ask> ask = uriDao.askUriExistance();
        
        if (ask == null) {//no result found
            getResponse = new ResultForm<>(0, 0, ask, true);
            return noResultFound(getResponse, statusList);
        } else if (!ask.isEmpty()) {// result founded
            getResponse = new ResultForm<>(uriDao.getPageSize(), uriDao.getPage(), ask, false);
            if (getResponse.getResult().dataSize() == 0) { //no result 
                return noResultFound(getResponse, statusList);
            } else { // return ask metadata
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else { //no result found
            getResponse = new ResultForm<>(0, 0, ask, true);
            return noResultFound(getResponse, statusList);
        }
    }

    /**
     * Gets the metadata of a given URI. 
     * The URI can be a concept URI or an instance
     * @param uriDao
     * @return Response
     */
    private Response getUriMetadata(UriDAO uriDao) {
        ArrayList<Uri> uris;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Uri> getResponse;

        uris = uriDao.allPaginate();

        if (uris == null) { //no result found
            getResponse = new ResultForm<>(0, 0, uris, true);
            return noResultFound(getResponse, statusList);
        } else if (!uris.isEmpty()) { //concepts founded
            getResponse = new ResultForm<>(uriDao.getPageSize(), uriDao.getPage(), uris, false);
            if (getResponse.getResult().dataSize() == 0) { // no result found
                return noResultFound(getResponse, statusList);
            } else { //return concepts metadata
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else { //no result found
            getResponse = new ResultForm<>(0, 0, uris, true);
            return noResultFound(getResponse, statusList);
        }
    }

    /**
     * Returns the list of URIs which has the given label.
     * @param uriDao collect all the Label data
     */
    private Response getLabelMetaData(UriDAO uriDao) {
        ArrayList<Uri> uris;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Uri> getResponse;

        uris = uriDao.labelsPaginate();
        if (uris == null) {
            getResponse = new ResultForm<>(0, 0, uris, true);
            return noResultFound(getResponse, statusList);
        } else if (!uris.isEmpty()) {
            getResponse = new ResultForm<>(uriDao.getPageSize(), uriDao.getPage(), uris, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResultForm<>(0, 0, uris, true);
            return noResultFound(getResponse, statusList);

        }
    }

    /**
     * Collects all the ancestors of a given URI.
     * @param uriDao
     * @return Response
     */
    private Response getAncestorsMetaData(UriDAO uriDao) {
        ArrayList<Uri> concepts;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Uri> getResponse;

        concepts = uriDao.ancestorsAllPaginate();
        if (concepts == null) { //no result found
            getResponse = new ResultForm<>(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        } else if (!concepts.isEmpty()) { //result founded
            getResponse = new ResultForm<>(uriDao.getPageSize(), uriDao.getPage(), concepts, false);
            if (getResponse.getResult().dataSize() == 0) { //no result found
                return noResultFound(getResponse, statusList);
            } else { //return concepts metadata
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else { // no result found
            getResponse = new ResultForm<>(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        }
    }

    /**
     * Collects all the siblings of a given URI.
     * @param uriDao
     * @return Response
     */
    private Response getSiblingsMetaData(UriDAO uriDao) {
        ArrayList<Uri> concepts;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Uri> getResponse;

        concepts = uriDao.siblingsAllPaginate();
        if (concepts == null) { //no result found
            getResponse = new ResultForm<>(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        } else if (!concepts.isEmpty()) { //result founded
            getResponse = new ResultForm<>(uriDao.getPageSize(), uriDao.getPage(), concepts, false);
            if (getResponse.getResult().dataSize() == 0) { //no result founded
                return noResultFound(getResponse, statusList);
            } else { //return concepts metadata

                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else { //no result found
            getResponse = new ResultForm<>(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        }
    }

    /**
     * Collects the descendants of a given URI.
     * @param uriDao
     * @return Response
     */
    private Response getDescendantsMetaData(UriDAO uriDao) {
        ArrayList<Uri> concepts;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Uri> getResponse;

        concepts = uriDao.descendantsAllPaginate();
        if (concepts == null) { //no result found
            getResponse = new ResultForm<>(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        } else if (!concepts.isEmpty()) { //concept founded 
            getResponse = new ResultForm<>(uriDao.getPageSize(), uriDao.getPage(), concepts, false);
            if (getResponse.getResult().dataSize() == 0) { // no result found
                return noResultFound(getResponse, statusList);
            } else {//return concepts metadata
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else { // no result found
            getResponse = new ResultForm<>(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        }
    }

    /**
     * Gets the type of a given URI.
     * @param uriDao
     * @return Response
     */
    private Response getUriType(UriDAO uriDao) {
        ArrayList<Uri> uris;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Uri> getResponse;
        uris = uriDao.getAskTypeAnswer();
        
        if (uris == null) {//no result found
            getResponse = new ResultForm<>(0, 0, uris, true);
            return noResultFound(getResponse, statusList);
        } else if (!uris.isEmpty()) { //result founded
            getResponse = new ResultForm<>(uriDao.getPageSize(), uriDao.getPage(), uris, false);
            if (getResponse.getResult().dataSize() == 0) {//no result found
                return noResultFound(getResponse, statusList);
            } else { //return meta data
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {//no result found
            getResponse = new ResultForm<>(0, 0, uris, true);
            return noResultFound(getResponse, statusList);
        }
    }
}
