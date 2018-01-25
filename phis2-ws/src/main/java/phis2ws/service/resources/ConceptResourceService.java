//**********************************************************************************************
//                                       ConceptsResourceService.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: Decembre 8, 2017
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Janvier 25, 2017
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
import phis2ws.service.view.brapi.form.ResponseFormConcept;
import phis2ws.service.view.brapi.form.ResponseFormInstance;
import phis2ws.service.view.model.phis.Concept;
import phis2ws.service.view.model.phis.Instance;

@Api("/concepts")
@Path("concepts")
/**
 * Represents the Concept Resource Service
 * @author Eloan Lagier
 */
public class ConceptResourceService {
    
    final static Logger LOGGER = LoggerFactory.getLogger(ConceptResourceService.class);
    
    //Session utilisateur
    @SessionInject
    Session userSession;
    
     /* @return concept list deppending of the GET calling
     *                                                             
     *         The return (in "data") is in the form : 
     *          [
     *              { concept description },
     *              { concept description },
     *          ]
     */
    private Response noResultFound(ResponseFormConcept getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No results", StatusCodeMsg.INFO, "No results for the concepts"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
    private Response noResultFound(ResponseFormInstance getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No results", StatusCodeMsg.INFO, "No results for the concepts"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
   
 
    /*this GET return all the concept info it could 
     there will be in an Hashmap in  because of the variation of its result
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
            @ApiParam( value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int limit,
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
            
            
            
          
    /* GET {uri}/instances return the list of instances of a concept
    
    if the deep param is equal to TRUE it will also give the instances of the 
    subClass of the concept
    
    
    
    */   
    /**
     *
     * @param uri
     * @param limit
     * @param deep
     * @param page
     * @return
     */
   @GET
    @Path("{uri}/instances")
    @ApiOperation(value = "Get all the instances of a concept", 
                  notes = "Retrieve all instances of subClass too, if deep=true")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve instances", response = Instance.class, responseContainer = "List"),
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
    public Response getInstancesList(
            @ApiParam(value = DocumentationAnnotation.CONCEPT_URI_DEFINITION, required = true, example=DocumentationAnnotation.EXAMPLE_CONCEPT_URI) @QueryParam("conceptUri") String uri, 
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

    
          
    /*GET {uri}/descendants give all the subClass of a concept
    
    */
    
     /*
     * @param uri
     * @param limit
     * @param page
     * @return
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
            @ApiParam(value = DocumentationAnnotation.CONCEPT_URI_DEFINITION, required = true, example=DocumentationAnnotation.EXAMPLE_CONCEPT_URI) @QueryParam("conceptUri") String uri,
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

    /*GET {uri}/ancestors give all the class parent of the concept
    */
    /*
     * @param uri
     * @param limit
     * @param page
     * @return
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
            @ApiParam(value = DocumentationAnnotation.CONCEPT_URI_DEFINITION, required = true, example=DocumentationAnnotation.EXAMPLE_CONCEPT_URI) @QueryParam("conceptUri") String uri,
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
    
    
    /*GET {uri}/siblings 
     give all the Concepts parent of the Concept given
    */ 
    
     /*
     * @param uri
     * @param limit
     * @param page
     * @return
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
            @ApiParam(value = DocumentationAnnotation.CONCEPT_URI_DEFINITION, required = true, example=DocumentationAnnotation.EXAMPLE_SIBLING_URI) @QueryParam("conceptUri") String uri,
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
    
    
 
    
    
    /**getConceptMetadata:
    * @param ConceptDaoSesame conceptDaoSesame
    * collect all the data of the user request
    */
    private Response getConceptMetadata(ConceptDaoSesame conceptDaoSesame) {
        ArrayList<Concept> concepts ;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormConcept getResponse;
        
        concepts = conceptDaoSesame.ConceptAllPaginate();
        LOGGER.debug("CONCEPTS="+concepts.toString());
        if (concepts == null) {
            getResponse = new ResponseFormConcept(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        } else if (!concepts.isEmpty()) {
            getResponse = new ResponseFormConcept(conceptDaoSesame.getPageSize(), conceptDaoSesame.getPage(), concepts, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResponseFormConcept(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        }
    }
    
    /**getDescendantsMetaData:
    * @param ConceptDaoSesame conceptDaoSesame
    * collect all the data of the user request
    */
    private Response getDescendantsMetaData(ConceptDaoSesame conceptDaoSesame) {
        ArrayList<Concept> concepts ;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormConcept getResponse;
        
        concepts = conceptDaoSesame.DescendantsAllPaginate();
        LOGGER.debug("CONCEPTS="+concepts.toString());
        if (concepts == null) {
            getResponse = new ResponseFormConcept(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        } else if (!concepts.isEmpty()) {
            getResponse = new ResponseFormConcept(conceptDaoSesame.getPageSize(), conceptDaoSesame.getPage(), concepts, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResponseFormConcept(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        }
    }
    
    /**getAncestorsMetaData:
    * @param ConceptDaoSesame conceptDaoSesame
    * collect all the data of the user request
    */
    private Response getAncestorsMetaData(ConceptDaoSesame conceptDaoSesame) {
        ArrayList<Concept> concepts ;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormConcept getResponse;
        
        concepts = conceptDaoSesame.AncestorsAllPaginate();
        LOGGER.debug("CONCEPTS="+concepts.toString());
        if (concepts == null) {
            getResponse = new ResponseFormConcept(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        } else if (!concepts.isEmpty()) {
            getResponse = new ResponseFormConcept(conceptDaoSesame.getPageSize(), conceptDaoSesame.getPage(), concepts, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResponseFormConcept(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        }
    }
     
    /**getSiblingsMetaData:
    * @param ConceptDaoSesame conceptDaoSesame
    * collect all the data of the user request
    */
    
    private Response getSiblingsMetaData(ConceptDaoSesame conceptDaoSesame) {
        ArrayList<Concept> concepts ;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormConcept getResponse;
        
        concepts = conceptDaoSesame.SiblingsAllPaginate();
        LOGGER.debug("CONCEPTS="+concepts.toString());
        if (concepts == null) {
            getResponse = new ResponseFormConcept(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        } else if (!concepts.isEmpty()) {
            getResponse = new ResponseFormConcept(conceptDaoSesame.getPageSize(), conceptDaoSesame.getPage(), concepts, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResponseFormConcept(0, 0, concepts, true);
            return noResultFound(getResponse, statusList);
        }
        
    }
        
    /**getInstancesData:
    * @param InstanceDaoSesame conceptDaoSesame
    *this one return all the instanceDATA of a concept so it call an InstanceDaoSesame 
     */
    private Response getInstancesData(InstanceDaoSesame instanceDaoSesame) {
        ArrayList<Instance> instances;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormInstance getResponse;
        
        instances = instanceDaoSesame.allPaginate();
        if (instances == null) {
            getResponse = new ResponseFormInstance(0, 0, instances, true);
            return noResultFound(getResponse, statusList);
        } else if (!instances.isEmpty()) {
            getResponse = new ResponseFormInstance(instanceDaoSesame.getPageSize(), instanceDaoSesame.getPage(), instances, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResponseFormInstance(0, 0, instances, true);
            return noResultFound(getResponse, statusList);
        }
    }
    
    
    
    

}