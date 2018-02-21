//**********************************************************************************************
//                                       ConceptsResourceService.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: Decembre 8, 2017
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Janvier 31, 2018
// Subject: Represente the Concept data service
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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.sesame.ConceptDaoSesame;
import phis2ws.service.dao.sesame.InstanceDaoSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormAsk;
import phis2ws.service.view.brapi.form.ResponseFormConcept;
import phis2ws.service.view.brapi.form.ResponseFormInstance;
import phis2ws.service.view.model.phis.Ask;
import phis2ws.service.view.model.phis.Concept;
import phis2ws.service.view.model.phis.Instance;

@Api("/concepts")
@Path("concepts")
/**
 * Represents the Concept Resource Service
 *
 * @author Eloan Lagier
 */
public class ConceptResourceService {

    final static Logger LOGGER = LoggerFactory.getLogger(ConceptResourceService.class);

    //user Session 
    @SessionInject
    Session userSession;

    /**
     * the no resultFound methode for ResponseFormConcept
     *
     * @param getResponse ResponseFormConcept
     * @param insertStatusList
     * @return the response "no result found" for the service
     */
    private Response noResultFound(ResponseFormConcept getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No results", StatusCodeMsg.INFO, "No results for the concepts"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }

    /**
     * the no resultFound methode for ResponseFormInstance
     *
     * @param getResponse ResponseFormInstance
     * @param insertStatusList
     * @return the response "no result found" for the service
     */
    private Response noResultFound(ResponseFormInstance getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No results", StatusCodeMsg.INFO, "No results for the instances"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }

    /**
     * the no resultFound methode for ResponseFormAsk
     *
     * @param getResponse ResponseFormAsk
     * @param insertStatusList
     * @return the response "no result found" for the service
     */
    private Response noResultFound(ResponseFormAsk getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No results", StatusCodeMsg.INFO, "No results for the ask"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }

    /**
     * this GET return all the concept informations
     *
     * @param limit
     * @param page
     * @param uri
     * @return concept list. The result form depends on the query results e.g.
     * of result : { "metadata": { "pagination": null, "status": [],
     * "datafiles": [] }, "result": { "data": [ { "uri":
     * "http://www.phenome-fppn.fr/vocabulary/2017#Document", "infos": { "type":
     * "http://www.w3.org/2002/07/owl#Class", "label_en": "document",
     * "label_fr": "document" } } ] } }
     */
    @GET
    @ApiOperation(value = "Get all concept informations",
            notes = "Retrieve all infos in the limit of what we knows")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all experiments", response = Concept.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})
    @ApiImplicitParams({
        @ApiImplicitParam(name = "Authorization", required = true,
                dataType = "string", paramType = "header",
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })

    @Produces(MediaType.APPLICATION_JSON)
    public Response getConceptBySearch(
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page,
            @ApiParam(value = "Search by uri", example = DocumentationAnnotation.EXAMPLE_CONCEPT_URI) @QueryParam("uri") String uri) {

        ConceptDaoSesame conceptDao = new ConceptDaoSesame();
        if (uri != null) {
            conceptDao.uri = uri;
        }

        conceptDao.user = userSession.getUser();
        conceptDao.setPage(page);
        conceptDao.setPageSize(limit);

        return getConceptMetadata(conceptDao);
    }

    /**
     * search if an uri is in the tripplestore or not
     *
     * @param uri
     * @return boolean
     */
    @GET
    @Path("{uri}")
    @ApiOperation(value = "ask if an URI is in the triplestore or not",
            notes = "Return a String of a boolean")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "ask concept", response = Ask.class, responseContainer = "List"),
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
    public Response isUriExisting(
            @ApiParam(value = DocumentationAnnotation.CONCEPT_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_CONCEPT_URI) @QueryParam("conceptUri") String uri) {

        ConceptDaoSesame conceptDaoSesame = new ConceptDaoSesame();
        if (uri != null) {
            conceptDaoSesame.uri = uri;
        }

        conceptDaoSesame.user = userSession.getUser();

        return getAskdata(conceptDaoSesame);
    }

    /**
     * return the type of an uri if it exist
     *
     * @param uri
     * @return false or type of the uri
     */
    @GET
    @Path("{uri}/type")
    @ApiOperation(value = "get the type of an uri if exist",
            notes = "else it will say false")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve type", response = Ask.class, responseContainer = "List"),
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
    public Response getTypeIfUriExist(
            @ApiParam(value = DocumentationAnnotation.CONCEPT_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_CONCEPT_URI) @QueryParam("conceptUri") String uri) {

        ConceptDaoSesame conceptDaoSesame = new ConceptDaoSesame();
        if (uri != null) {
            conceptDaoSesame.uri = uri;
        }

        conceptDaoSesame.user = userSession.getUser();

        return getAskTypedata(conceptDaoSesame);
    }

    /**
     * return the list of instances of a concept
     * if the deep param is equal to TRUE it will also give the instances of the 
     * subClass of the concept
     * @param uri
     * @param limit
     * @param deep
     * @param page
     * @return { "metadata": { "pagination": null, "status": [], "datafiles": []
     * }, "result": { "data": [ { "uri":
     * "http://www.phenome-fppn.fr/phenovia/documents/document90fb96ace2894cdb9f4575173d8ed4c9",
     * "type": "http://www.phenome-fppn.fr/vocabulary/2017#ScientificDocument" }
     * ] } }
     */
    @GET
    @Path("{uri}/instances")
    @ApiOperation(value = "Get all the instances of a concept",
            notes = "Retrieve all instances of subClass too, if deep=true")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve instances", response = Instance.class, responseContainer = "List")
        ,
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION)
        ,
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED)
        ,
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "Authorization", required = true,
                dataType = "string", paramType = "header",
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInstancesList(
            @ApiParam(value = DocumentationAnnotation.CONCEPT_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_CONCEPT_URI) @QueryParam("conceptUri") String uri,
            @ApiParam(value = DocumentationAnnotation.DEEP) @QueryParam("deep") @DefaultValue(DocumentationAnnotation.EXAMPLE_DEEP) String deep,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page) {

        InstanceDaoSesame instanceDaoSesame = new InstanceDaoSesame();
        if (uri != null) {
            instanceDaoSesame.uri = uri;
        }
        if (deep != null) {
            instanceDaoSesame.deep = Boolean.valueOf(deep);
        }
        instanceDaoSesame.setPageSize(limit);
        instanceDaoSesame.setPage(page);
        instanceDaoSesame.user = userSession.getUser();

        return getInstancesData(instanceDaoSesame);
    }

    /**
     * search all descendants of a given class
     *
     * @param uri
     * @param limit
     * @param page
     * @return { "metadata": { "pagination": { "pageSize": 20, "currentPage": 0,
     * "totalCount": 12, "totalPages": 1 }, "status": [], "datafiles": [] },
     * "result": { "data": [ { "uri":
     * "http://www.phenome-fppn.fr/vocabulary/2017#Document", "properties": {}
     * }, { "uri": "http://www.phenome-fppn.fr/vocabulary/2017#DataFile",
     * "properties": {} }, { "uri":
     * "http://www.phenome-fppn.fr/vocabulary/2017#TechnicalDocument",
     * "properties": {} } ] } }
     */
    @GET
    @Path("{uri}/descendants")
    @ApiOperation(value = "Get all the descendants of a concept",
            notes = "Retrieve all subclass and the subClass of subClass too")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve Concept", response = Concept.class, responseContainer = "List"),
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
    public Response getDescendantsList(
            @ApiParam(value = DocumentationAnnotation.CONCEPT_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_CONCEPT_URI) @QueryParam("conceptUri") String uri,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page) {

        ConceptDaoSesame conceptDaoSesame = new ConceptDaoSesame();
        if (uri != null) {
            conceptDaoSesame.uri = uri;
        }
        conceptDaoSesame.setPageSize(limit);
        conceptDaoSesame.setPage(page);
        conceptDaoSesame.user = userSession.getUser();

        return getDescendantsMetaData(conceptDaoSesame);
    }

    /**
     * give all the class parent of the concept
     *
     * @param uri
     * @param limit
     * @param page
     * @return { "metadata": { "pagination": null, "status": [], "datafiles": []
     * }, "result": { "data": [ { "uri":
     * "http://www.phenome-fppn.fr/vocabulary/2017#Document", "properties": {} }
     * ] } }
     */
    @GET
    @Path("{uri}/ancestors")
    @ApiOperation(value = "Get all the ancestor of a concept",
            notes = "Retrieve all Class parents of the Class")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve Concept", response = Concept.class, responseContainer = "List"),
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
    public Response getAncestorsList(
            @ApiParam(value = DocumentationAnnotation.CONCEPT_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_CONCEPT_URI) @QueryParam("conceptUri") String uri,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page) {

        ConceptDaoSesame conceptDaoSesame = new ConceptDaoSesame();
        if (uri != null) {
            conceptDaoSesame.uri = uri;
        }
        conceptDaoSesame.setPageSize(limit);
        conceptDaoSesame.setPage(page);
        conceptDaoSesame.user = userSession.getUser();

        return getAncestorsMetaData(conceptDaoSesame);
    }

    /**
     * give all the concepts with the same parent
     *
     * @param uri
     * @param limit
     * @param page
     * @return { "metadata": { "pagination": { "pageSize": 20, "currentPage": 0,
     * "totalCount": 11, "totalPages": 1 }, "status": [], "datafiles": [] },
     * "result": { "data": [ { "uri":
     * "http://www.phenome-fppn.fr/vocabulary/2017#DataFile", "properties": {}
     * }, { "uri":
     * "http://www.phenome-fppn.fr/vocabulary/2017#TechnicalDocument",
     * "properties": {} } ] } }
     */
    @GET
    @Path("{uri}/siblings")
    @ApiOperation(value = "Get all the siblings of a concept",
            notes = "Retrieve all Class with same parent")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve Concept", response = Concept.class, responseContainer = "List"),
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
    public Response getSibblingsList(
            @ApiParam(value = DocumentationAnnotation.CONCEPT_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_SIBLING_URI) @QueryParam("conceptUri") String uri,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page) {

        ConceptDaoSesame conceptDaoSesame = new ConceptDaoSesame();
        if (uri != null) {
            conceptDaoSesame.uri = uri;
        }
        conceptDaoSesame.setPageSize(limit);
        conceptDaoSesame.setPage(page);
        conceptDaoSesame.user = userSession.getUser();

        return getSiblingsMetaData(conceptDaoSesame);
    }

    /**
     * collect all the data of the user request
     *
     * @param conceptDaoSesame
     * @return Response
     */
    private Response getConceptMetadata(ConceptDaoSesame conceptDaoSesame) {
        ArrayList<Concept> concepts;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormConcept getResponse;

        concepts = conceptDaoSesame.allPaginate();

        if (concepts == null) { //no result found
            getResponse = new ResponseFormConcept(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        } else if (!concepts.isEmpty()) { //concepts founded
            getResponse = new ResponseFormConcept(conceptDaoSesame.getPageSize(), conceptDaoSesame.getPage(), concepts, false);
            if (getResponse.getResult().dataSize() == 0) { // no result found
                return noResultFound(getResponse, statusList);
            } else { //return concepts metadata
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else { //no result found
            getResponse = new ResponseFormConcept(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        }
    }


    /**
     * collect all the data for the descendant request
     * @param conceptDaoSesame
     * @return Response
     */
    private Response getDescendantsMetaData(ConceptDaoSesame conceptDaoSesame) {
        ArrayList<Concept> concepts;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormConcept getResponse;

        concepts = conceptDaoSesame.descendantsAllPaginate();
        if (concepts == null) { //no result found
            getResponse = new ResponseFormConcept(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        } else if (!concepts.isEmpty()) { //concept founded 
            getResponse = new ResponseFormConcept(conceptDaoSesame.getPageSize(), conceptDaoSesame.getPage(), concepts, false);
            if (getResponse.getResult().dataSize() == 0) { // no result found
                return noResultFound(getResponse, statusList);
            } else {//return concepts metadata
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else { // no result found
            getResponse = new ResponseFormConcept(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        }
    }

    /**
     * collect all the data for the ancestors request
     * @param conceptDaoSesame
     * @return Response
     */
    private Response getAncestorsMetaData(ConceptDaoSesame conceptDaoSesame) {
        ArrayList<Concept> concepts;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormConcept getResponse;

        concepts = conceptDaoSesame.AncestorsAllPaginate();
        if (concepts == null) { //no result found
            getResponse = new ResponseFormConcept(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        } else if (!concepts.isEmpty()) { //result founded
            getResponse = new ResponseFormConcept(conceptDaoSesame.getPageSize(), conceptDaoSesame.getPage(), concepts, false);
            if (getResponse.getResult().dataSize() == 0) { //no result found
                return noResultFound(getResponse, statusList);
            } else { //return concepts metadata
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else { // no result found
            getResponse = new ResponseFormConcept(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        }
    }

    /**
     * collect all the data for the siblings request
     * @param conceptDaoSesame
     * @return Response
     */
    private Response getSiblingsMetaData(ConceptDaoSesame conceptDaoSesame) {
        ArrayList<Concept> concepts;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormConcept getResponse;

        concepts = conceptDaoSesame.SiblingsAllPaginate();
        if (concepts == null) { //no result found
            getResponse = new ResponseFormConcept(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        } else if (!concepts.isEmpty()) { //result founded
            getResponse = new ResponseFormConcept(conceptDaoSesame.getPageSize(), conceptDaoSesame.getPage(), concepts, false);
            if (getResponse.getResult().dataSize() == 0) { //no result founded
                return noResultFound(getResponse, statusList);
            } else { //return concepts metadata

                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else { //no result found
            getResponse = new ResponseFormConcept(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        }
    }

    /**
     * collect all the data for the instances request
     * @param instanceDaoSesame
     * @return Response
     */
    private Response getInstancesData(InstanceDaoSesame instanceDaoSesame) {
        ArrayList<Instance> instances;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormInstance getResponse;

        instances = instanceDaoSesame.allPaginate();
        if (instances == null) { //no result found
            getResponse = new ResponseFormInstance(0, 0, instances, true);
            return noResultFound(getResponse, statusList);
        } else if (!instances.isEmpty()) { //result founded
            getResponse = new ResponseFormInstance(instanceDaoSesame.getPageSize(), instanceDaoSesame.getPage(), instances, false);
            if (getResponse.getResult().dataSize() == 0) { //no result found
                return noResultFound(getResponse, statusList);
            } else { //return instances metadata
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else { //no result found
            getResponse = new ResponseFormInstance(0, 0, instances, true);
            return noResultFound(getResponse, statusList);
        }
    }

    /**
     * collect all the data for the ask request
     * @param conceptDaoSesame
     * @return Response
     */
    private Response getAskdata(ConceptDaoSesame conceptDaoSesame) {
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormAsk getResponse;
        ArrayList<Ask> ask = conceptDaoSesame.askUriExistance();
        if (ask == null) {//no result found
            getResponse = new ResponseFormAsk(0, 0, ask, true);
            return noResultFound(getResponse, statusList);
        } else if (!ask.isEmpty()) {// result founded
            getResponse = new ResponseFormAsk(conceptDaoSesame.getPageSize(), conceptDaoSesame.getPage(), ask, false);
            if (getResponse.getResult().dataSize() == 0) { //no result 
                return noResultFound(getResponse, statusList);
            } else { // return ask metadata
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else { //no result found
            getResponse = new ResponseFormAsk(0, 0, ask, true);
            return noResultFound(getResponse, statusList);
        }
    }

    /**
     * collect all the data of the ask/type request
     * @param conceptDaoSesame
     * @return Response
     */
    private Response getAskTypedata(ConceptDaoSesame conceptDaoSesame) {
        ArrayList<Ask> ask;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormAsk getResponse;
        ask = conceptDaoSesame.getAskTypeAnswer();
        if (ask == null) {//no result found
            getResponse = new ResponseFormAsk(0, 0, ask, true);
            return noResultFound(getResponse, statusList);
        } else if (!ask.isEmpty()) { //result founded
            getResponse = new ResponseFormAsk(conceptDaoSesame.getPageSize(), conceptDaoSesame.getPage(), ask, false);
            if (getResponse.getResult().dataSize() == 0) {//no result found
                return noResultFound(getResponse, statusList);
            } else { //return meta data
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {//no result found
            getResponse = new ResponseFormAsk(0, 0, ask, true);
            return noResultFound(getResponse, statusList);
        }
    }
}
