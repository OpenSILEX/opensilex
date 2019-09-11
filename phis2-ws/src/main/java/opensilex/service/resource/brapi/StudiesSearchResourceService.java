//******************************************************************************
//                          StudiesResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 22 Aug. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.brapi;

import opensilex.service.resource.dto.experiment.StudySearchDTO;
import opensilex.service.dao.StudyRDFDAO;
import opensilex.service.resource.dto.experiment.StudyDTO;
import opensilex.service.dao.StudySQLDAO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
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
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.model.Call;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.ResourceService;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.BrapiMultiResponseForm;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Api("/brapi/v1/studies-search")
@Path("/brapi/v1/studies-search")
/**
 * Study services :
 * GET Studies/{studyDbId}
 * GET Studies/{studyDbId}/observations
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class StudiesSearchResourceService extends ResourceService implements BrapiCall {    
    
    final static Logger LOGGER = LoggerFactory.getLogger(StudiesSearchResourceService.class);
    
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
        callMethods.add("POST");
        ArrayList<String> callVersions = new ArrayList<>();
        callVersions.add("1.2");
        Call call1 = new Call("studies-search", calldatatypes, callMethods, callVersions);
        calls.add(call1);
        return calls;
    }
    
    /**
     * @param studySearch
     * @param context
     * @return result of the studies-search request BRAPI V1.2
     * @throws opensilex.service.dao.exception.DAOPersistenceException
     */
    @POST
    @ApiOperation(value = "search studies",
                  notes = "search studies")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "query created", response = ResponseFormPOST.class),
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
    
    public Response postStudiesSearch(
            @ApiParam(value = DocumentationAnnotation.EXPERIMENT_POST_DATA_DEFINITION) @Valid StudySearchDTO studySearch,
            @Context HttpServletRequest context) throws DAOPersistenceException {
        AbstractResultForm postResponse = null;
     
        if (studySearch != null) {                    
            // get studies from postgresql DB
            StudySQLDAO studySqlDAO = new StudySQLDAO();
            
//          //get studies from rdf4j based on germplasms --> to implement when germplasm services implemented;
//            if (studySearch.getGermplasmDbIds() != null) {
//                StudyRDFDAO studyRdfDAO = new StudyRDFDAO();            
//                studyRdfDAO.germplasmDbIds = studySearch.getGermplasmDbIds();            
//                ArrayList<String> studyURIs = studyRdfDAO.getStudiesFromGermplasms();
//                if (!studyURIs.isEmpty()) {
//                    studySqlDAO.studyDbIds = studyURIs;
//                } else {
//                    studyURIs.add("noStudyCorresponding"); //to have no study found in studySQLDAO if no study used those germplasms 
//                    studySqlDAO.studyDbIds = studyURIs;
//                }
//            }
            
            if (studySearch.getStudyDbIds() != null) {
//                if (studySqlDAO.germplasmDbIds != null) {   //if there are studyDbIds and germplasmDbIds, we check if studies and germplasms correspond
//                    ArrayList<String> studiesList = new ArrayList();
//                    for (String study:studySearch.getStudyDbIds()) {                    
//                        if (studySqlDAO.studyDbIds.contains(study))  {
//                            studiesList.add(study);
//                        }
//                    }
//                    if (studiesList.isEmpty()) {
//                        studiesList.add("noStudyCorresponding");
//                        studySqlDAO.studyDbIds = studiesList;
//                    }
//                    studySqlDAO.studyDbIds = studiesList;                
//                } else {
//                    studySqlDAO.studyDbIds = studySearch.getStudyDbIds();
//                }
                studySqlDAO.studyDbIds = studySearch.getStudyDbIds();
            } 
            
            if (studySearch.getCommonCropName() != null) {
                studySqlDAO.commonCropName = studySearch.getCommonCropName();
            }
            
            if (studySearch.getPage() != null) {
                studySqlDAO.setPage(studySearch.getPage()) ;
            } else {
                studySqlDAO.setPage(0);
            }
            
            if (studySearch.getPageSize()!= null) {
                studySqlDAO.setPageSize(studySearch.getPageSize()) ;
            } else {
                studySqlDAO.setPageSize(20);
            }
            
            if (studySearch.getSeasonDbId()!= null) {
                studySqlDAO.seasonDbIds = new ArrayList();
                studySqlDAO.seasonDbIds.add(studySearch.getSeasonDbId());
            }
            
            if (studySearch.getSortBy()!= null) {
                studySqlDAO.sortBy = studySearch.getSortBy();
                //SILEX:info
                //can't sort on germplasm
                //\SILEX:info
            }
            
            if (studySearch.getSortOrder()!= null) {
                studySqlDAO.sortOrder = studySearch.getSortOrder();
            }
            
            if (studySearch.getStudyNames()!= null) {
                studySqlDAO.studyNames = studySearch.getStudyNames();
            }
            studySqlDAO.user=userSession.getUser();
            Integer studiesCount = studySqlDAO.count();
            ArrayList<StudyDTO> studies = studySqlDAO.allPaginate();
            ArrayList<Status> statusList = new ArrayList(); 
            
            if (studies.isEmpty()) {
                BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(0, 0, studies, true);
                return noResultFound(getResponse, statusList);
            } else {
                BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(studySqlDAO.getPageSize(), studySqlDAO.getPage(), studies, true, studiesCount);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            } 

        } else {
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty search parameters"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Retrieve studies information
     * @param studyDbId
     * @param commonCropName
     * @param studyTypeDbId - not covered
     * @param programDbId - not covered
     * @param locationDbId - not covered
     * @param seasonDbId
     * @param trialDbId - not covered
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
    @ApiImplicitParams({
       @ApiImplicitParam(name = "Authorization", required = true,
                         dataType = "string", paramType = "header",
                         value = DocumentationAnnotation.ACCES_TOKEN,
                         example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)   

    public Response getStudiesSearch (
        @ApiParam(value = "Search by studyDbId", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI ) @QueryParam("studyDbId") @URL String studyDbId,
        @ApiParam(value = "Search by commonCropName", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_CROP_SPECIES ) @QueryParam("commonCropName") String commonCropName,
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

         StudySQLDAO studySqlDAO = new StudySQLDAO();

        if (studyDbId != null) {
            studySqlDAO.studyDbIds = new ArrayList();
            studySqlDAO.studyDbIds.add(studyDbId);
        }     
        if (commonCropName != null) {
            studySqlDAO.commonCropName = commonCropName;
        }    
        if (seasonDbId != null) {
            studySqlDAO.seasonDbIds = new ArrayList();
            studySqlDAO.seasonDbIds.add(seasonDbId);
        }         
        if (sortBy != null) {
            studySqlDAO.sortBy = sortBy;
        }
        if (sortOrder != null) {
            studySqlDAO.sortOrder = sortOrder;
        } 
        if (active != null) {
            studySqlDAO.active = active;
        }
        
        studySqlDAO.setPageSize(pageSize);
        studySqlDAO.setPage(page);
        studySqlDAO.user=userSession.getUser();
        
        ArrayList<Status> statusList = new ArrayList<>(); 
        Integer studiesCount = studySqlDAO.count();
        ArrayList<StudyDTO> studies = studySqlDAO.allPaginate();
        
        if (studies.isEmpty()) {
            BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(0, 0, studies, true);
            return noResultFound(getResponse, statusList);
        } else {
            BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(pageSize, page, studies, true, studiesCount);
            return Response.status(Response.Status.OK).entity(getResponse).build();
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
}