//******************************************************************************
//                          StudiesResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 22 Aug. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.brapi;

import opensilex.service.resource.dto.experiment.StudyDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import opensilex.service.configuration.DateFormat;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.dao.DataDAO;
import opensilex.service.dao.ScientificObjectRdf4jDAO;
import opensilex.service.dao.VariableDAO;
import opensilex.service.model.Call;
import opensilex.service.resource.dto.data.BrapiObservationDTO;
import opensilex.service.resource.dto.scientificObject.BrapiObservationSummaryDTO;
import opensilex.service.resource.dto.scientificObject.BrapiObservationUnitDTO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.BrapiVariable;
import opensilex.service.model.Data;
import opensilex.service.model.ScientificObject;
import opensilex.service.model.StudyDetails;
import opensilex.service.model.Variable;
import opensilex.service.ontology.Oeso;
import opensilex.service.resource.ResourceService;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.BrapiMultiResponseForm;
import opensilex.service.view.brapi.form.BrapiSingleResponseForm;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.dal.ExperimentSearch;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Api("/brapi/v1/studies")
@Path("/brapi/v1/studies")
/**
 * Study services :
 * GET Studies/{studyDbId}
 * GET Studies/{studyDbId}/observations
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class StudiesResourceService extends ResourceService implements BrapiCall {    
    
    final static Logger LOGGER = LoggerFactory.getLogger(StudiesResourceService.class);
       
    @Inject
    public StudiesResourceService(SPARQLService sparql) {
        this.sparql = sparql;
    }

    private final SPARQLService sparql;
    
    /**
     * Overriding BrapiCall method
     * @date 27 Aug 2018
     * @return Calls call information
     */
    @Override
    public ArrayList<Call> callInfo() {
        ArrayList<Call> calls = new ArrayList();
        ArrayList<String> calldatatypes = new ArrayList<>();
        calldatatypes.add("json");
        ArrayList<String> callMethods = new ArrayList<>();
        callMethods.add("GET");
        ArrayList<String> callVersions = new ArrayList<>();
        callVersions.add("1.3");
        Call call1 = new Call("studies/{studyDbId}", calldatatypes, callMethods, callVersions);
        Call call2 = new Call("studies/{studyDbId}/observations", calldatatypes, callMethods, callVersions);
        Call call3 = new Call("studies/{studyDbId}/observationvariables", calldatatypes, callMethods, callVersions);
        Call call4 = new Call("studies/{studyDbId}/observationunits", calldatatypes, callMethods, callVersions);      
        Call call5 = new Call("studies", calldatatypes, callMethods, callVersions);
        Call call6 = new Call("studies?studyDbId={studyDbId}", calldatatypes, callMethods, callVersions);
        
        calls.add(call1);
        calls.add(call2);
        calls.add(call3);
        calls.add(call4);      
        calls.add(call5);
        calls.add(call6);
        return calls;
    }
       
    /**
     * Retrieve studies information
     * @param studyDbId
     * @param commonCropName
     * @param studyTypeDbId
     * @param programDbId - not managed
     * @param locationDbId - not managed
     * @param seasonDbId
     * @param trialDbId - not managed
     * @param active
     * @param sortBy
     * @param sortOrder
     * @param pageSize
     * @param page
     * @return the study information
     * @example
     *  {
        "metadata": {
          "pagination": {
            "pageSize": 1,
            "currentPage": 0,
            "totalCount": 1,
            "totalPages": 1
          },
          "status": null,
          "datafiles": []
        },
        "result": {
          "data": [
            {
              "active": "false",
              "additionalInfo": null,
              "commonCropName": "",
              "documentationURL": null,
              "endDate": "2019-02-01",
              "locationDbId": null,
              "locationName": null,
              "name": null,
              "programDbId": null,
              "programName": null,
              "seasons": [
                "2018"
              ],
              "startDate": "2018-07-01",
              "studyDbId": "http://www.opensilex.org/demo/DMO2018-3",
              "studyName": "EXP01",
              "studyType": null,
              "studyTypeDbId": null,
              "studyTypeName": null,
              "trialDbId": null,
              "trialName": null
            }
          ]
        }
      }
     */    
    @GET
    @ApiOperation(value = "Retrieve studies information", notes = "Retrieve studies information")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve studies information", response = StudyDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)   

    public Response getStudies (
        @ApiParam(value = "Search by studyDbId", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI ) @QueryParam("studyDbId") @URL String studyDbId,
        //@ApiParam(value = "Search by commonCropName", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_CROP_SPECIES ) @QueryParam("commonCropName") String commonCropName,
        //@ApiParam(value = "Search by studyTypeDbId - NOT COVERED YET") @QueryParam("studyTypeDbId") String studyTypeDbId,
        //@ApiParam(value = "Search by programDbId - NOT COVERED YET ") @QueryParam("programDbId ") String programDbId,
        //@ApiParam(value = "Search by locationDbId - NOT COVERED YET") @QueryParam("locationDbId") String locationDbId,
        @ApiParam(value = "Search by seasonDbId", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_CAMPAIGN ) @QueryParam("seasonDbId") String seasonDbId,
        //@ApiParam(value = "Search by trialDbId - NOT COVERED YET") @QueryParam("trialDbId") String trialDbId,
        @ApiParam(value = "Filter active status true/false") @QueryParam("active") String active,
        @ApiParam(value = "Name of the field to sort by: studyDbId, commonCropName or seasonDbId") @QueryParam("sortBy") String sortBy,
        @ApiParam(value = "Sort order direction - ASC or DESC") @QueryParam("sortOrder") String sortOrder,
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page
        ) throws SQLException {               

        try {
            ExperimentSearch searchDTO = new ExperimentSearch();
            ArrayList<OrderBy> orderByList = new ArrayList();
            
            if (!StringUtils.isEmpty(studyDbId)) {
                searchDTO.setUri(URI.create(studyDbId));                
            }            

            if (!StringUtils.isEmpty(seasonDbId)) {
                searchDTO.setCampaign(Integer.parseInt(seasonDbId));
            }
            if (!StringUtils.isEmpty(active)) {
                searchDTO.setEnded(!Boolean.parseBoolean(active));
            }
            if (!StringUtils.isEmpty(sortBy)) {
                if (null == sortBy) {
                    sortBy = "";
                } else switch (sortBy) {
                    case "studyDbId":
                        sortBy = "uri";
                        break;
                    case "seasonDbId":
                        sortBy = "campaign";
                        break;
                    default:
                        sortBy = "";
                        break;
                }
                String orderByStr = new String();
                if (!StringUtils.isEmpty(sortOrder)) {
                    orderByStr = sortBy + "=" + sortOrder;
                } else {
                    orderByStr = sortBy + "=" + "desc";
                }
                OrderBy order = new OrderBy(orderByStr);
                orderByList.add(order);
            }

            ExperimentDAO xpDao = new ExperimentDAO(sparql);
            ListWithPagination<ExperimentModel> resultList = xpDao.search(searchDTO, orderByList, page, pageSize);

            ArrayList<StudyDTO> studies = new ArrayList();
            for (ExperimentModel exp:resultList.getList()) {
                StudyDTO study = convert(exp);
                studies.add(study);                
            }
            ArrayList<Status> statusList = new ArrayList<>();
            if (studies.isEmpty()) {
                BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(0, 0, studies, true);
                return noResultFound(getResponse, statusList);
            } else {
                BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(resultList.getPageSize(), resultList.getPage(), studies, true, resultList.getTotal());
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }  
        } catch (Exception e) {
            AbstractResultForm postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, e.getMessage()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(postResponse).build();
        }  

    }
    /**
     * Retrieve one study information
     * @param studyDbId
     * @return the study information
     * @example
     *  {
         "metadata": {
           "pagination": {
             "pageSize": 0,
             "currentPage": 0,
             "totalCount": 0,
             "totalPages": 0
           },
           "status": null,
           "datafiles": []
         },
         "result": {
           "studyDbId": "http://www.opensilex.org/demo/DMO2018-3",
           "studyName": "EXP01",
           "studyTypeDbId": null,
           "studyTypeName": null,
           "studyDescription": "",
           "seasons": [
             "2018"
           ],
           "commonCropName": "",
           "trialDbId": null,
           "trialName": null,
           "startDate": "2018-07-01",
           "endDate": "2019-02-01",
           "active": false,
           "license": null,
           "location": null,
           "contacts": [],
           "dataLinks": [],
           "lastUpdate": null,
           "additionalInfo": null,
           "documentationURL": null
         }
        }
     */    
    @GET
    @Path("{studyDbId}")
    @ApiOperation(value = "Retrieve study details", notes = "Retrieve study details")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve study details", response = StudyDetails.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)   

    public Response getStudyDetails (
        @ApiParam(value = "Search by studyDbId", required = true, example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI ) @PathParam("studyDbId") @URL @Required String studyDbId
        ) throws Exception {   
        try {
            
            ExperimentSearch searchDTO = new ExperimentSearch();
            ArrayList<OrderBy> orderByList = new ArrayList();
            searchDTO.setUri(URI.create(studyDbId));

            
            ExperimentDAO xpDao = new ExperimentDAO(sparql);
            ListWithPagination<ExperimentModel> resultList = xpDao.search(searchDTO, orderByList, 0, 20);

            ArrayList<StudyDTO> studies = new ArrayList();
            for (ExperimentModel exp:resultList.getList()) {
                StudyDTO study = convert(exp);
                studies.add(study);                
            }
            ArrayList<Status> statusList = new ArrayList<>();
            if (studies.isEmpty()) {
                BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(0, 0, studies, true);
                return noResultFound(getResponse, statusList);
            } else {
                BrapiSingleResponseForm getResponse = new BrapiSingleResponseForm(studies.get(0));
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }  
        } catch (Exception e) {
            AbstractResultForm postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, e.getMessage()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(postResponse).build();
        }    
       
    }

    /**
     * Retrieve one study observations
     * @param studyDbId
     * @param observationVariableDbIds
     * @param limit
     * @param page
     * @return the study observations
     * @example
     *  {
            "metadata": {
              "pagination": {
                "pageSize": 20,
                "currentPage": 0,
                "totalCount": 1,
                "totalPages": 1
              },
              "status": null,
              "datafiles": []
            },
            "result": {
              "data": [
                {
                  "germplasmDbId": null,
                  "germplasmName": null,
                  "observationDbId": null,
                  "observationLevel": "http://www.opensilex.org/vocabulary/oeso#Plant",
                  "observationTimeStamp": "2019-02-27",
                  "observationUnitDbId": "http://www.phenome-fppn.fr/platform/2019/o19000001",
                  "observationUnitName": "Plant01",
                  "observationVariableDbId": "http://www.phenome-fppn.fr/platform/id/variables/v004",
                  "observationVariableName": "ttt_mmm_uuu",
                  "operator": null,
                  "season": null,
                  "studyDbId": "http://www.opensilex.org/demo/DMO2018-3",
                  "uploadedBy": null,
                  "value": "0.484969"
                }
              ]
            }
        }
     */  
    @GET
    @Path("{studyDbId}/observations")
    @ApiOperation(value = "Get the observations associated to a specific study", notes = "Get the observations associated to a specific study")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = BrapiObservationDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)   
    public Response getObservations (
        @ApiParam(value = "studyDbId", required = true, example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI ) @PathParam("studyDbId") @URL @Required String studyDbId,
        @ApiParam(value = "observationVariableDbIds") @QueryParam(value = "observationVariableDbIds") @URL List<String> observationVariableDbIds,  
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page
    ) throws SQLException {               

        List<String> variableURIs = new ArrayList();

        if (observationVariableDbIds != null) {
            variableURIs = observationVariableDbIds;
        }

        return getStudyObservations(studyDbId, variableURIs, limit, page);
    }
    /**
     * Brapi Call GET studies/{studyDbId}/observationvariables V1.3
     * Retrieve all observation variables measured in the study
     * @param studyDbId
     * @param limit
     * @param page
     * @return the study observation variables
     * @example
        {
          "metadata": {
            "pagination": {
              "pageSize": 1,
              "currentPage": 1,
              "totalCount": 2,
              "totalPages": 2
            },
            "status": null,
            "datafiles": []
          },
          "result": {
            "data": [
              {
                "ObservationVariableDbId": "http://www.phenome-fppn.fr/platform/id/variables/v004",
                "ObservationVariableName": "ttt_mmm_uuu",
                "ontologyReference": null,
                "synonyms": [],
                "contextOfUse": [],
                "growthStage": null,
                "status": null,
                "xref": null,
                "institution": null,
                "scientist": null,
                "submissionTimesTamp": null,
                "language": null,
                "crop": null,
                "trait": {
                  "traitDbId": "http://www.phenome-fppn.fr/platform/id/traits/t003",
                  "traitName": "ttt",
                  "class": null,
                  "description": null,
                  "synonyms": [],
                  "mainAbbreviation": null,
                  "alternativeAbbreviations": [],
                  "entity": null,
                  "attribute": null,
                  "status": null,
                  "xref": null,
                  "ontologyReference": null
                },
                "method": {
                  "methodDbId": "http://www.phenome-fppn.fr/platform/id/methods/m003",
                  "methodName": "mmm",
                  "class": null,
                  "description": null,
                  "formula": null,
                  "ontologyReference": null,
                  "reference": null
                },
                "scale": {
                  "scaleDbid": "http://www.phenome-fppn.fr/platform/id/units/u004",
                  "scaleName": "uuu",
                  "dataType": "Numerical",
                  "decimalPlaces": null,
                  "ontologyReference": null,
                  "xref": null,
                  "validValues": null
                },
                "defaultValue": null,
                "documentationURL": null
              }
            ]
          }
        }
     */  
    @GET
    @Path("{studyDbId}/observationvariables")
    @ApiOperation(value = "List all the observation variables measured in the study.", notes = "List all the observation variables measured in the study.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = BrapiVariable.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)   
    public Response getObservationVariables (
        @ApiParam(value = "studyDbId", required = true, example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI ) @PathParam("studyDbId") @URL @Required String studyDbId,
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page
    ) throws Exception {               

        try {
            
            ExperimentSearch searchDTO = new ExperimentSearch();
            ExperimentDAO xpDao = new ExperimentDAO(sparql);
            ExperimentModel xpModel = xpDao.get(new URI(studyDbId));
            
            ArrayList<Status> statusList = new ArrayList<>();  
            
            if (xpModel != null) {
                List<URI> variables = xpModel.getVariables();
                //SPARQLDeserializers.getExpandedURI(xp.getUri().toString())
                ArrayList<BrapiVariable> brapiVariables= new ArrayList();
            
                for (URI var:variables) {
                    VariableDAO varDAO = new VariableDAO(sparql);
                    BrapiVariable brapiVar = varDAO.findBrapiVariableById(SPARQLDeserializers.getExpandedURI(var.toString()));
                    brapiVariables.add(brapiVar);
                }
                if (brapiVariables.isEmpty()) {
                    BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(0, 0, brapiVariables, true);
                    return noResultFound(getResponse, statusList);
                } else {
                    BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(limit, page, brapiVariables, false);
                    return Response.status(Response.Status.OK).entity(getResponse).build();
                }
            
            } else {
                ArrayList<BrapiVariable> var = new ArrayList<>();
                BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(0,0,var,true);
                return noResultFound(getResponse, statusList);            
            }
            
        } catch (Exception e) {
            AbstractResultForm postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, e.getMessage()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(postResponse).build();        
        }
      
    }
    
    /**
     * Retrieve all observationUnits linked to the study, ie ScientificObject that participates in an experiment
     * @param studyDbId
     * @param observationLevel
     * @param limit
     * @param page
     * @return the observationUnits linked to the study and their observations
     * @example
        {
          "metadata": {
            "pagination": {
              "pageSize": 20,
              "currentPage": 0,
              "totalCount": 2,
              "totalPages": 1
            },
            "status": null,
            "datafiles": []
          },
          "result": {
            "data": [
              {
                "blockNumber": null,
                "entryNumber": null,
                "entryType": null,
                "germplasmDbId": null,
                "germplasmName": null,
                "locationDbId": null,
                "locationName": null,
                "observationLevel": "http://www.opensilex.org/vocabulary/oeso#Plot",
                "observationLevels": null,
                "observationUnitDbId": "http://www.phenome-fppn.fr/platform/2019/o19000002",
                "observationUnitName": "Plot01",
                "observationUnitXref": null,
                "observations": [
                  {
                    "collector": null,
                    "observationDbId": "http://www.phenome-fppn.fr/platform/id/data/ucmseqox2dmscr2k2tzx53nyeompiygginingjy546p4sb3aaara086a6cf083af46d9b25dd1812a6542e9",
                    "observationTimeStamp": "Mar 16, 2019 1:51:00 AM",
                    "observationVariableDbId": "http://www.phenome-fppn.fr/platform/id/variables/v003",
                    "observationVariableName": "Plant-Height_MethodMPH_cm",
                    "season": null,
                    "value": null
                  },
                  {
                    "collector": null,
                    "observationDbId": "http://www.phenome-fppn.fr/platform/id/data/cvljyfvbdd77ceylvgpb4kzljexii3fjdohzpw3jt4dtqc6xvygq1b9b713f75714f5290038f0f11e43d16",
                    "observationTimeStamp": "Mar 16, 2019 12:51:00 AM",
                    "observationVariableDbId": "http://www.phenome-fppn.fr/platform/id/variables/v003",
                    "observationVariableName": "Plant-Height_MethodMPH_cm",
                    "season": null,
                    "value": null
                  }
                ],
                "pedigree": null,
                "plantNumber": null,
                "plotNumber": null,
                "positionCoordinateX": null,
                "positionCoordinateXType": null,
                "positionCoordinateY": null,
                "positionCoordinateYType": null,
                "programDbId": null,
                "programName": null,
                "replicate": null,
                "studyDbId": "http://www.opensilex.org/demo/DMO2018-3",
                "studyName": "EXP01",
                "treatments": null,
                "trialDbId": null,
                "trialName": null
              },
              {
                "blockNumber": null,
                "entryNumber": null,
                "entryType": null,
                "germplasmDbId": null,
                "germplasmName": null,
                "locationDbId": null,
                "locationName": null,
                "observationLevel": "http://www.opensilex.org/vocabulary/oeso#Plot",
                "observationLevels": null,
                "observationUnitDbId": "http://www.phenome-fppn.fr/platform/2019/o19000011",
                "observationUnitName": "plottest",
                "observationUnitXref": null,
                "observations": [],
                "pedigree": null,
                "plantNumber": null,
                "plotNumber": null,
                "positionCoordinateX": null,
                "positionCoordinateXType": null,
                "positionCoordinateY": null,
                "positionCoordinateYType": null,
                "programDbId": null,
                "programName": null,
                "replicate": null,
                "studyDbId": "http://www.opensilex.org/demo/DMO2018-3",
                "studyName": "EXP01",
                "treatments": null,
                "trialDbId": null,
                "trialName": null
              }
            ]
          }
        }
     */  
    @GET
    @Path("{studyDbId}/observationunits")
    @ApiOperation(value = "List all the observation units measured in the study.", notes = "List all the observation units measured in the study.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = BrapiObservationUnitDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)   
    public Response getObservationUnits (
        @ApiParam(value = "studyDbId", required = true, example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI ) @PathParam("studyDbId") @URL @Required String studyDbId,
        @ApiParam(value = "observationLevel", example = "Plot" ) @QueryParam("observationLevel") String  observationLevel,
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page
    ) throws Exception {           

        try {
            ArrayList<Status> statusList = new ArrayList<>();    
            
            String rdfType = null;
            if (observationLevel != null) {
                rdfType =  Oeso.NAMESPACE + observationLevel;
            }

            ScientificObjectRdf4jDAO scientificObjectsDAO = new ScientificObjectRdf4jDAO(sparql);
            ArrayList<ScientificObject> scientificObjects = scientificObjectsDAO.find(null, null, null, rdfType, studyDbId, null, false);

            ExperimentDAO experimentDAO = new ExperimentDAO(sparql);
            ExperimentModel xp = experimentDAO.get(new URI(studyDbId));
            if (xp != null) {
                ArrayList<BrapiObservationUnitDTO> observationUnits= getObservationUnitsResult(scientificObjects,xp);

                if (observationUnits.isEmpty()) {
                    BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(0, 0, observationUnits, true);
                    return noResultFound(getResponse, statusList);
                } else {
                    BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(limit, page, observationUnits, false);
                    return Response.status(Response.Status.OK).entity(getResponse).build();
                }  

            } else {
                BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(0, 0, new ArrayList<>(), true);
                return noResultFound(getResponse, statusList);
            }  
        } catch (Exception e) {
                AbstractResultForm postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, e.getMessage()));
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(postResponse).build();        
        }
              

    }

    private Response noResultFound(BrapiMultiResponseForm getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No result", StatusCodeMsg.INFO, "no result for this query"));
        getResponse.getMetadata().setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }

    private Response sqlError(BrapiMultiResponseForm getResponse, ArrayList<Status> insertStatusList) {
         insertStatusList.add(new Status("SQL error" ,StatusCodeMsg.ERR, "can't fetch result"));
         getResponse.getMetadata().setStatus(insertStatusList);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(getResponse).build();
    }

    /**
     * Retrieve the response with observations corresponding to the user query (parameters: one specific study and eventually some variables)
     * @param studyDAO the study for which we want to retrieve the linked observations
     * @param variableURIs to filter the observations on a list of variableURIs defined by the user
     * @param limit pagesize
     * @param page the page number
     * @return observations list 
     */
    private Response getStudyObservations(String studyDbId, List<String> variableURIs, int limit, int page) {
        ArrayList<Status> statusList = new ArrayList<>();  
        try {
            ArrayList<BrapiObservationDTO> observations = getObservationsList(studyDbId,variableURIs);

            if (observations.isEmpty()) {
                BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(0, 0, observations, true);
                return noResultFound(getResponse, statusList);
            } else {
                BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(limit, page, observations, false);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }   
            
        } catch (Exception e) {
            AbstractResultForm postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, e.getMessage()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(postResponse).build();
        }
         
    }

    /**
     * Retrieve the observations list corresponding to the user query
     * @param studyDAO the study for which we want to retrieve the linked observations
     * @param variableURIs to filter the observations on a list of variableURIs defined by the user
     * @param limit pagesize
     * @param page the page number
     * @return observations list 
     */
    private ArrayList<BrapiObservationDTO> getObservationsList(String studyDbId, List<String> variableURIs) throws Exception {

        ArrayList<BrapiObservationDTO> observations = new ArrayList();  
        ScientificObjectRdf4jDAO objectDAO = new ScientificObjectRdf4jDAO(sparql);

        ArrayList<ScientificObject> objectsList = objectDAO.find(null, null, null, null, studyDbId, null, false);

        ArrayList<Variable> variablesList = new ArrayList();

        if (variableURIs.isEmpty()) {  
            VariableDAO variableDaoSesame = new VariableDAO(sparql);
            //if variableURIs is empty, we look for all variables observations
            variablesList = variableDaoSesame.getAll(false, false); 

        } else {            
            //in case a variable uri is duplicated, we keep distinct uris
            List<String> uniqueVariableURIs= variableURIs.stream().distinct().collect(Collectors.toList());
            for (String variableURI:uniqueVariableURIs) {
                VariableDAO variableDAO = new VariableDAO(sparql);
                try {
                    Variable variable = variableDAO.findById(variableURI);
                    variablesList.add(variable);
                } catch (Exception ex) {
                    // ignore unknown variables
                }

            }                
        }

        for (Variable variable:variablesList) {
            DataDAO dataDAOMongo = new DataDAO(sparql);
            dataDAOMongo.variableUri = variable.getUri();
            ArrayList<BrapiObservationDTO> observationsPerVariable = new ArrayList();
            for (ScientificObject object:objectsList) {            
                dataDAOMongo.objectUri = object.getUri();
                ArrayList<Data> dataList = dataDAOMongo.allPaginate();
                ArrayList<BrapiObservationDTO> observationsPerVariableAndObject = getObservationsFromData(dataList,variable,object);
                observationsPerVariable.addAll(observationsPerVariableAndObject);            
            }
            observations.addAll(observationsPerVariable);
        }

        return observations;
    }

    /**
     * Fill the observations attributes with Data, Variable and ScientificObject attributes
     * @param dataList list of data corresponding to the variable and the scientificObject
     * @param variable variable linked to the dataList
     * @param object scientific object linked to the dataList
     * @return observations list 
     */
    private ArrayList<BrapiObservationDTO> getObservationsFromData(ArrayList<Data> dataList, Variable variable, ScientificObject object) {
        SimpleDateFormat df = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());
        ArrayList<BrapiObservationDTO> observations = new ArrayList();

        for (Data data:dataList){            
            BrapiObservationDTO observation= new BrapiObservationDTO();
            observation.setObservationUnitDbId(object.getUri());
            observation.setObservationUnitName(object.getLabel());
            observation.setObservationLevel(object.getRdfType());            
            observation.setStudyDbId(object.getExperiment());
            observation.setObservationVariableDbId(variable.getUri());
            observation.setObservationVariableName(variable.getLabel());    
            observation.setObservationDbId(data.getUri());
            observation.setObservationTimeStamp(df.format(data.getDate()));
            observation.setValue(data.getValue().toString());
            observations.add(observation);
        }

        return observations;
    }

    /**
     * Retrieve the observationUnits information from scientificObjects list and experiment
     * @param scientificObjects list of ScientificObjects corresponding to the user query
     * @param experiment Experiment linked to those scientific objects (user query filter)
     * @return observationUnits list 
     */
    private ArrayList<BrapiObservationUnitDTO> getObservationUnitsResult(ArrayList<ScientificObject> scientificObjects, ExperimentModel experiment) throws Exception {
        SimpleDateFormat df = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());
        VariableDAO variableDaoSesame = new VariableDAO(sparql);
        ArrayList<Variable> variablesList = variableDaoSesame.allPaginate(); 
        ArrayList<BrapiObservationUnitDTO> observationUnitsList = new ArrayList();

        for (ScientificObject object:scientificObjects) {
            BrapiObservationUnitDTO unit = new BrapiObservationUnitDTO(object.getUri());
            String rdfUnitType = object.getRdfType();
            String unitType[] = rdfUnitType.split("#");
            unit.setObservationLevel(unitType[1]);
            unit.setObservationUnitName(object.getLabel());
            unit.setStudyDbId(experiment.getUri().toString());
            unit.setStudyName(experiment.getLabel());
            ArrayList<BrapiObservationSummaryDTO> observationsPerObsUnit = new ArrayList(); 
            for (Variable variable:variablesList) {
                //retrieve observations
                DataDAO dataDAOMongo = new DataDAO(sparql);
                ArrayList<BrapiObservationSummaryDTO> observationsPerObjectAndVariable = new ArrayList();          
                dataDAOMongo.objectUri = object.getUri();
                dataDAOMongo.variableUri = variable.getUri();
                ArrayList<Data> dataList = dataDAOMongo.allPaginate();
                for (Data data:dataList) {
                    BrapiObservationSummaryDTO obs = new BrapiObservationSummaryDTO();
                    obs.setObservationDbId(data.getUri());
                    obs.setObservationTimeStamp(df.format(data.getDate()));
                    obs.setObservationVariableDbId(variable.getUri());
                    obs.setObservationVariableName(variable.getLabel());
                    obs.setValue(data.getValue().toString());
                    observationsPerObjectAndVariable.add(obs);
                }
                observationsPerObsUnit.addAll(observationsPerObjectAndVariable);
            }
            unit.setObservations(observationsPerObsUnit);
            observationUnitsList.add(unit);
        } 
        return observationUnitsList;
    }
    
    public static StudyDTO convert(ExperimentModel xp) {

        StudyDTO study = new StudyDTO();
        study.setStudyDbId(SPARQLDeserializers.getExpandedURI(xp.getUri().toString()));
        study.setName(xp.getLabel());
        study.setStudyName(xp.getLabel());
        ArrayList<String> seasons = new ArrayList();
        if (xp.getCampaign() != null) {
            seasons.add(xp.getCampaign().toString());
        }        
        study.setSeasons(seasons);
        study.setStartDate(xp.getStartDate().toString());
        if (xp.getEndDate() != null) {
            study.setEndDate(xp.getEndDate().toString());
        }
        LocalDate startDate = xp.getStartDate();
        LocalDate endDate = xp.getEndDate();        
        if (startDate.compareTo(LocalDate.now()) <= 0  && (endDate == null | endDate.compareTo(LocalDate.now()) >= 0 )) {
            study.setActive("true");                        
        } else {
            study.setActive("false");
        }

//        URI species = xp.getSpecies();
//        SpeciesDAO speciesDAO = new SpeciesDAO();
//        List<String> speciesLabels = speciesDAO.findLabelsForUri(species.toString());
//        if (speciesLabels != null) {
//            study.setCommonCropName(speciesLabels.get(0));
//        }
        
        return study;
    }
    
}