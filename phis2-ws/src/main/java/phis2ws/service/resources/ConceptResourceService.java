//**********************************************************************************************
//                                       ConceptsResourceService.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: Decembre 8, 2017
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Decembre 8, 2017
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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import static phis2ws.service.authentication.TokenManager.Instance;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.phis.ExperimentDao;
import phis2ws.service.dao.sesame.ConceptDaoSesame;
import phis2ws.service.dao.sesame.UnitDaoSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormGET;
import phis2ws.service.view.brapi.form.ResponseFormInstance;
import phis2ws.service.view.brapi.form.ResponseFormUnit;
import phis2ws.service.view.model.phis.Concept;
import phis2ws.service.view.model.phis.Experiment;
import phis2ws.service.view.model.phis.Instance;
import phis2ws.service.view.model.phis.Unit;

@Api("/concepts")
@Path("concepts")
public class ConceptResourceService {
    
    final static Logger LOGGER = LoggerFactory.getLogger(ConceptResourceService.class);
    
    //Session utilisateur
    @SessionInject
    Session userSession;
    
    /**
     * @param <error>
     * @param conceptURI
     * @param r
     * @param limit
     * @param page
     * @param deep
     * @return liste des expérimentations correspondant aux différents critères de recherche 
     *                                                              (ou toutes les expérimentations si pas de critères)
     *         Le retour (dans "data") est de la forme : 
     *          [
     *              { description de l'expérimentation1 },
     *              { description de l'expérimentation2 },
     *          ]
     */
    
    private Response noResultFound(ResponseFormInstance getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No results", StatusCodeMsg.INFO, "No results for the units"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
       
    private Response getInstancesData(ConceptDaoSesame conceptDaoSesame) {
        LOGGER.trace("getInstancesData - début");
        ArrayList<Instance> instances;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormInstance getResponse;
        
        instances = conceptDaoSesame.allPaginate();
        
        if (instances == null) {
            getResponse = new ResponseFormInstance(0, 0, instances, true);
            return noResultFound(getResponse, statusList);
        } else if (!instances.isEmpty()) {
            getResponse = new ResponseFormInstance(conceptDaoSesame.getPageSize(), conceptDaoSesame.getPage(), instances, false);
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
 /*   @GET
    @ApiOperation(value = "Get the concept skos info",
                       notes = "Retrieve all Concept informations authorized for the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all informations", response = Concept.class, responseContainer = "List"),
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
    public Response ConceptBySearch(
        Response r= r;
        return r;
    }

;
        return Object o;
    }
*/
   @GET
    @Path("{uri}/instances")
    @ApiOperation(value = "Get all the instances of a concept", 
                  notes = "Retrieve all instances of subClass too, if deep=true")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve instances", response = Concept.class, responseContainer = "List"),
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
            @ApiParam(value = DocumentationAnnotation.CONCEPT_URI_DEFINITION, required = true, example=DocumentationAnnotation.EXAMPLE_CONCEPT_URI) @QueryParam("conceptUri") String conceptURI, 
            @ApiParam(value = DocumentationAnnotation.DEEP) @QueryParam("deep") @DefaultValue(DocumentationAnnotation.EXAMPLE_DEEP) String deep,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page) {
     /*   if (conceptURI == null) {
            final Status status = new Status("Access error", StatusCodeMsg.ERR, "Empty concept URI");
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(status)).build();
        }
*/        
        ConceptDaoSesame conceptDaoSesame = new ConceptDaoSesame();
        if (conceptURI != null) {
            conceptDaoSesame.uri = conceptURI;
         }
        if (deep != null) {
            conceptDaoSesame.deep = Boolean.valueOf(deep);
         }
        conceptDaoSesame.setPageSize(limit);
        conceptDaoSesame.setPage(page);
        conceptDaoSesame.user = userSession.getUser();
        
        return getInstancesData(conceptDaoSesame);
    }    

}