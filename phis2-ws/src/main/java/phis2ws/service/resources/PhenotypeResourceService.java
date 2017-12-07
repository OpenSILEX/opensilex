package phis2ws.service.resources;

//**********************************************************************************************
//                                       RawDataResourceService.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: September 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  September, 13 2017
// Subject: Represents the raw data service
//***********************************************************************************************

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.mongo.PhenotypeDaoMongo;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.resources.dto.PhenotypeDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.brapi.form.ResponseFormPhenotype;
import phis2ws.service.view.model.phis.Phenotype;

@Api("/phenotypes")
@Path("/phenotypes") 
public class PhenotypeResourceService {
    
    final static Logger LOGGER = LoggerFactory.getLogger(ExperimentResourceService.class);
   
    //Session de l'utilisateur
    @SessionInject
    Session userSession;
    
    /**
     * 
     * @param phenotypes
     * @param context
     * @return le résultat de la création du phénotype
     */
    @POST
    @ApiOperation(value = "Post phenotype data")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Phenotypes saved", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "Authorization", required = true,
                          dataType = "string", paramType = "header",
                          value = DocumentationAnnotation.ACCES_TOKEN,
                          example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postPhenotypeData(@ApiParam(value = DocumentationAnnotation.RAW_DATA_POST_DATA_DEFINITION) ArrayList<PhenotypeDTO> phenotypes,
            @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        //Si dans les données envoyées, il y au moins une provenance (phenotypes)
        if (phenotypes != null && !phenotypes.isEmpty()) {
            PhenotypeDaoMongo phenotypesDaoMongo = new PhenotypeDaoMongo();
            phenotypesDaoMongo.user = userSession.getUser();
            
            //Vérification des données et insertion dans MongoDb
            POSTResultsReturn result = phenotypesDaoMongo.checkAndInsert(phenotypes);
            
            postResponse = new ResponseFormPOST(result.statusList);
            
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty phenotypes to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Collecte les données issues d'une requête de l'utilisateur (recherche de phénotypes)
     * @param phenotypeDaoMongo
     * @return la réponse pour l'utilisateur,
     *          Contient la liste des phénotypes correspondant à la recherche (/!\ il y a de la pagination)
     */
    private Response getPhenotypesData(PhenotypeDaoMongo phenotypeDaoMongo) {
        //TODO reprendre Phenotype pour l'adapter au résultat attendu pour la recherche..
        ArrayList<Phenotype> phenotypes = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormPhenotype getResponse;
        
        phenotypes = phenotypeDaoMongo.allPaginate();
        
        if (phenotypes == null) {
            getResponse = new ResponseFormPhenotype(0, 0, phenotypes, true);
            return noResultFound(getResponse, statusList);
        } else if (!phenotypes.isEmpty()) {
            getResponse = new ResponseFormPhenotype(phenotypeDaoMongo.getPageSize(), phenotypeDaoMongo.getPage(), phenotypes, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResponseFormPhenotype(0, 0, phenotypes, true);
            return noResultFound(getResponse, statusList);
        }
    }
    
    private Response noResultFound(ResponseFormPhenotype getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No results", StatusCodeMsg.INFO, "No results for the agronomical objects"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
    
    /**
     * 
     * @param pageSize
     * @param page
     * @param experiment
     * @param variable
     * @param agronomicalObjects
     * @param startDate
     * @param endDate
     * @return liste des phenotypes correspondant aux différents critères de recherche 
     *                                                              (ou tous les phénotypes si pas de critères)
     *         Le retour (dans "data") est de la forme : 
     *          [
     *              { description du phenotype1 },
     *              { description du phenotype2 },
     *          ]
     */
    @GET
    @ApiOperation(value = "Get all phenotypes corresponding to the search params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all phenotypes", response = Phenotype.class, responseContainer = "List"),
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
    public Response getPhenotypesBySearch(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page,
        @ApiParam(value = "Search by experiment", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI) @QueryParam("experiment") String experiment,
        @ApiParam(value = "Search by variable", example = DocumentationAnnotation.EXAMPLE_VARIABLE_URI) @QueryParam("variable") String variable,
        @ApiParam(value = "Search by agronomical(s) object(s), séparated by coma", example = DocumentationAnnotation.EXAMPLE_AGRONOMICAL_OBJECT_URI + "," + DocumentationAnnotation.EXAMPLE_AGRONOMICAL_OBJECT_URI) @QueryParam("agronomicalObjects") String agronomicalObjects,
        @ApiParam(value = "Search by interval - Start date", example = DocumentationAnnotation.EXAMPLE_PROJECT_DATE_START) @QueryParam("startDate") String startDate,
        @ApiParam(value = "Search by interval - End date", example = DocumentationAnnotation.EXAMPLE_PROJECT_DATE_END) @QueryParam("endDate") String endDate) {
        
        PhenotypeDaoMongo phenotypeDaoMongo = new PhenotypeDaoMongo();
        
        if (experiment != null) {
            phenotypeDaoMongo.experiment = experiment;
        }
        if (variable != null) {
            phenotypeDaoMongo.variable = variable;
        }
        if (startDate != null && endDate != null) {
            //SILEX:todo
            //Faire une vérification du format des dates etc.
            phenotypeDaoMongo.startDate = startDate;
            phenotypeDaoMongo.endDate = endDate;
            //\SILEX:todo
        }
        if (agronomicalObjects != null) {
            //les uris d'objets agronomiques doivent être séparées par des "," 
            String[] agronomicalObjectsURIs = agronomicalObjects.split(",");
            phenotypeDaoMongo.agronomicalObjects.addAll(Arrays.asList(agronomicalObjectsURIs));
        }
        
        phenotypeDaoMongo.user = userSession.getUser();
        phenotypeDaoMongo.setPage(page);
        phenotypeDaoMongo.setPageSize(pageSize);
        
        return getPhenotypesData(phenotypeDaoMongo);
    }
}
