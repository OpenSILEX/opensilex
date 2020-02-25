//******************************************************************************
//                          EnvironmentResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 29 Oct. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource;

import com.mongodb.BasicDBObjectBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import opensilex.service.resource.validation.interfaces.Date;
import java.util.List;
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
import opensilex.service.configuration.DateFormat;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.DataDAO;
import opensilex.service.dao.ProvenanceDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.Data;
import opensilex.service.resource.dto.environment.EnvironmentMeasurePostDTO;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.model.EnvironmentMeasure;
import opensilex.service.resource.dto.data.DataDTO;
import opensilex.service.resource.dto.environment.EnvironmentMeasureDTO;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.result.ResultForm;
import opensilex.service.view.model.provenance.Provenance;

/**
 * Environmental measure resource service.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/environments")
@Path("/environments")
@Deprecated
public class EnvironmentResourceService extends ResourceService {
    /**
     * Generates environmental measures from a given list of environmental measures DTOs.
     * @param environmentMeasureDTOs
     * @return the list of environments
     */
    private List<EnvironmentMeasure> environmentMeasurePostDTOsToEnvironmentMeasure(List<EnvironmentMeasurePostDTO> environmentMeasureDTOs) {
        ArrayList<EnvironmentMeasure> environments = new ArrayList<>();
        
        environmentMeasureDTOs.forEach((environmentDTO) -> {
            environments.add(environmentDTO.createObjectFromDTO());
        });
        
        return environments;
    }
    
    /**
     * Service to insert environmental measures. 
     * @example
     * [
     *  {
     *      "sensorUri": "http://www.phenome-fppn.fr/diaphen/2018/s18521",
     *      "variableUri": "http://www.phenome-fppn.fr/id/variables/v001",
     *      "date": "2017-06-15T10:51:00+0200",
     *      "value": "0.5"
     *  }
     * ]
     * @param environmentMeasures
     * @param context
     * @return the insertion result. 
     */
    @POST
    @ApiOperation(value = "Post environment(s) measures",
                  notes = "Register environment(s) measures in the database"
                            + "<br/> The 'value' parameter could be a string representing any java BigDecimal"
                            + "<br/> By example it could be: -2, 3.14, 1.23E+3, -1.23e-12, etc..."
                            + "<br/> @see https://docs.oracle.com/javase/7/docs/api/java/math/BigDecimal.html#BigDecimal(java.lang.String)"
                          )
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "environment(s) saved", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public Response postEnvironmentMeasures(
        @ApiParam(value = DocumentationAnnotation.ENVIRONMENT_POST_DEFINITION) @Valid ArrayList<EnvironmentMeasurePostDTO> data,
        @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
         try {
            if (data != null && !data.isEmpty()) {
                DataDAO dataDAO = new DataDAO();

                dataDAO.user = userSession.getUser();

                POSTResultsReturn result = dataDAO.checkAndInsert(environmentMeasurePostDTOTOsToData(data));

                if (result.getHttpStatus().equals(Response.Status.CREATED)) {
                    postResponse = new ResponseFormPOST(result.statusList);
                    postResponse.getMetadata().setDatafiles(result.getCreatedResources());
                } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                        || result.getHttpStatus().equals(Response.Status.OK)
                        || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                    postResponse = new ResponseFormPOST(result.statusList);
                }
                return Response.status(result.getHttpStatus()).entity(postResponse).build();
            } else {
                postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "No data to add"));
                return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
            }       
        } catch (ParseException e) {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, e.getMessage()));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Generates an data list from a given list of EnvironmentMeasurePostDTO.
     * @param dataDTOs
     * @return the list of data
     */
    private List<Data> environmentMeasurePostDTOTOsToData(List<EnvironmentMeasurePostDTO> dataDTOs) throws ParseException {
        ArrayList<Data> dataList = new ArrayList<>();
        
        for (EnvironmentMeasurePostDTO dataDTO : dataDTOs) {
            Data data = new Data();
            SimpleDateFormat parser = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());
            java.util.Date date = parser.parse(dataDTO.getDate());
            data.setDate(date);
            data.setObjectUri(null);
            data.setProvenanceUri(dataDTO.getDate());
            data.setValue(dataDTO.getValue());
            data.setVariableUri(dataDTO.getVariableUri());
            dataList.add(data);            
        }
        
        return dataList;
    }    
    
    
      /**
     * Generates an data list from a given list of EnvironmentMeasurePostDTO.
     * @param dataDTOs
     * @return the list of data
     */
    private List<EnvironmentMeasurePostDTO> DataenvironmentToMeasurePostDTOTOs(List<Data> dataDTOs) throws ParseException {
        ArrayList<EnvironmentMeasurePostDTO> dataList = new ArrayList<>();
        
        for (Data dataDTO : dataDTOs) {
            EnvironmentMeasurePostDTO environmentMeasurePostDTO = new EnvironmentMeasurePostDTO();
            SimpleDateFormat formatter = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());
             environmentMeasurePostDTO.setDate(formatter.format(dataDTO.getDate()));
             environmentMeasurePostDTO.setProvenanceUri(dataDTO.getDate());
            environmentMeasurePostDTO.setValue((BigDecimal) dataDTO.getValue());
            environmentMeasurePostDTO.setVariableUri(dataDTO.getVariableUri());
            dataList.add(environmentMeasurePostDTO);            
        }
        
        return dataList;
    }  

    /**
     * Service to get environment measures.
     * @param pageSize
     * @param page
     * @param variableUri
     * @param startDate
     * @param endDate
     * @param sensorUri
     * @param dateSortAsc
     * @return list of the environment measures corresponding to the search parameters given
     * @example
     * {
     *      "metadata": {
     *          "pagination": {
     *              "pageSize": 20,
     *              "currentPage": 0,
     *              "totalCount": 3,
     *              "totalPages": 1
     *          },
     *          "status": [],
     *          "datafiles": []
     *      },
     *      "result": {
     *          "data": [
     *              {
     *                "sensorUri": "http://www.phenome-fppn.fr/mauguio/diaphen/2013/sb140227",
     *                "date": "2017-06-07 13:14:32+0200",
     *                "value": 36.78
     *              },
     *              {
     *                "sensorUri": "http://www.phenome-fppn.fr/mauguio/diaphen/2013/sb140227",
     *                "date": "2017-06-07 13:14:40+0200",
     *                "value": 36.78
     *              },
     *              {
     *                "sensorUri": "http://www.phenome-fppn.fr/mauguio/diaphen/2013/sb140227",
     *                "date": "2017-06-07 13:14:55+0200",
     *                "value": 36.78
     *              }
     *          ]
     *      }
     * }
     */
    @GET
    @ApiOperation(value = "Get all environment measures corresponding to the search params given",
                  notes = "Retrieve all environment measures authorized for the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all radiometric targets", response = EnvironmentMeasureDTO.class, responseContainer = "List"),
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
    @Deprecated
    public Response getEnvironmentMeasures(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
        @ApiParam(value = "Search by variable uri", example = DocumentationAnnotation.EXAMPLE_VARIABLE_URI) @QueryParam("variable") @URL @Required String variableUri,
        @ApiParam(value = "Search by minimal date", example = DocumentationAnnotation.EXAMPLE_XSDDATETIME) @QueryParam("startDate") @Date(DateFormat.YMDTHMSZ) String startDate,
        @ApiParam(value = "Search by maximal date", example = DocumentationAnnotation.EXAMPLE_XSDDATETIME) @QueryParam("endDate") @Date(DateFormat.YMDTHMSZ) String endDate,
        @ApiParam(value = "Search by sensor uri", example = DocumentationAnnotation.EXAMPLE_SENSOR_URI) @QueryParam("sensor")  @URL String sensorUri,
        @ApiParam(value = "Date search result order ('true' for ascending and 'false' for descending)", example = "true") @QueryParam("dateSortAsc") boolean dateSortAsc
    ) {
        // 1. Initialize dataDAO with parameters
        DataDAO dataDAO = new DataDAO();
         
        dataDAO.user = userSession.getUser();
        dataDAO.setPage(page);
        dataDAO.setPageSize(pageSize); 

        ProvenanceDAO provenanceDAO  = new ProvenanceDAO();
        provenanceDAO.setPage(1);
        provenanceDAO.setPageSize(1000);
        
        ArrayList<String> provenanceUrisAssociatedToSensor = new ArrayList<>();
        if (sensorUri != null){
            Provenance searchProvenance = new Provenance();
            String jsonFilter = BasicDBObjectBuilder.start("metadata.prov:Agent.prov:id", sensorUri).get().toString();
            ArrayList<Provenance> provenances = provenanceDAO.getProvenances(searchProvenance, jsonFilter);

            for (Provenance provenance : provenances) {
                provenanceUrisAssociatedToSensor.add(provenance.getUri());
            }
        }
        
        
        //1. Get sensor data count
        Integer totalCount = dataDAO.count(variableUri, startDate, endDate, null, provenanceUrisAssociatedToSensor);
        List<Data> dataFounded = new ArrayList<>();
        //2. Get sensor data
        if(totalCount > 0){
            dataFounded = dataDAO.find(page, pageSize, variableUri,  startDate, endDate, null, provenanceUrisAssociatedToSensor);
        }
        
        //3. Return result
        ArrayList<Status> statusList = new ArrayList<>();
        ArrayList<DataDTO> sensorsToReturn = new ArrayList<>();
        ResultForm<DataDTO> getResponse;
        if (dataFounded == null) { //Request failure
            getResponse = new ResultForm<>(0, 0, sensorsToReturn, true);
            return noResultFound(getResponse, statusList);
        } else if (dataFounded.isEmpty()) { //No result found
            getResponse = new ResultForm<>(0, 0, sensorsToReturn, true);
            return noResultFound(getResponse, statusList);
        } else { //Results
            //Convert all objects to DTOs
            dataFounded.forEach((data) -> {
                sensorsToReturn.add(new DataDTO(data));
            });
            
            getResponse = new ResultForm<>(pageSize, page, sensorsToReturn, true, totalCount);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }
}