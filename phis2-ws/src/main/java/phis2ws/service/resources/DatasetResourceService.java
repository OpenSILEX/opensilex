//**********************************************************************************************
//                                       DatasetResourceService.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: September 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  January, 12 2018 Update POST datasets : add 
// possibility to post a dataset with an already existing provenance                             
// Subject: Represents the dataset service
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
import java.util.Arrays;
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
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.mongo.DatasetDAOMongo;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.DatasetDTO;
import phis2ws.service.resources.validation.interfaces.Date;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.brapi.form.ResponseFormDataset;
import phis2ws.service.view.model.phis.Dataset;

/**
 * Represents the datasets service
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/datasets")
@Path("/datasets") 
public class DatasetResourceService extends ResourceService {
    /**
     * 
     * @param datasets dataset to save. If in the provance there is only the uri
     *                 it meens that the provenance is supposed to already exist
     * example : 
     * [{
     *	"variableUri": "http://www.phenome-fppn.fr/phis_field/id/variables/v001",
     * 	"provenance": {
     *		"uri": "http://www.phenome-fppn.fr/mtp/2018/pv181515071552",
     *		"creationDate": "2017-06-15 10:51:00+0200",
     *		"wasGeneratedBy": {
     *			"wasGeneratedBy": "fileuri",
     *			"wasGeneratedByDescription": "string"
     *		}
     *	},
     *	"data": [{
     *		"agronomicalObject": "http://www.phenome-fppn.fr/phenovia/2017/o1032481",
     *		"date": "2017-06-15 10:51:00+0200",
     *		"value": "0.7777777",
     *		"variable": "http://www.phenome-fppn.fr/phis_field/id/variables/v001"
     *	}]
     * }]
     * @param context
     * @return the query result with the list of provenance uri if the datasets 
     *         has been saved in the database
     */
    @POST
    @ApiOperation(value = "Post dataset")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Dataset saved", response = ResponseFormPOST.class),
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
    public Response postDatasetData(@ApiParam(value = DocumentationAnnotation.RAW_DATA_POST_DATA_DEFINITION, required = true) @Valid ArrayList<DatasetDTO> datasets,
            @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        //If there are at least one provenance (and dataset) in the sended data
        if (datasets != null && !datasets.isEmpty()) {
            DatasetDAOMongo datasetDAOMongo = new DatasetDAOMongo();
            datasetDAOMongo.user = userSession.getUser();
            
            //check data and insert in the mongo database
            POSTResultsReturn result = datasetDAOMongo.checkAndInsert(datasets);
            
            postResponse = new ResponseFormPOST(result.statusList);
            postResponse.getMetadata().setDatafiles(result.createdResources);
            
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty datasets to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Collect data from a user query (search data)
     * @param datasetDAOMongo
     * @return the user answer with the results
     */
    private Response getDatasetsData(DatasetDAOMongo datasetDAOMongo) {
        ArrayList<Dataset> datasets = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormDataset getResponse;
        
        datasets = datasetDAOMongo.allPaginate();
        
        if (datasets == null) {
            getResponse = new ResponseFormDataset(0, 0, datasets, true);
            return noResultFound(getResponse, statusList);
        } else if (!datasets.isEmpty()) {
            getResponse = new ResponseFormDataset(datasetDAOMongo.getPageSize(), datasetDAOMongo.getPage(), datasets, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResponseFormDataset(0, 0, datasets, true);
            return noResultFound(getResponse, statusList);
        }
    }
    
    /**
     * 
     * @param pageSize
     * @param page
     * @param experiment the experiment's uri from whom the user wants data 
     *                   (e.g http://www.phenome-fppn.fr/diaphen/DIA2017-1)
     * @param variable the variable uri of the searched data
     *                 (e.g http://www.phenome-fppn.fr/diaphen/id/variables/v001)
     * @param agronomicalObjects the agronomical object's uri whom the user 
     *                           wants data
     *                           (e.g http://www.phenome-fppn.fr/diaphen/2017/o17010091)
     * @param startDate the start date of the searched data 
     *                  (e.g 2017-06-15 10:51:00+0200)
     * @param endDate the end date of the searched data
     *                (e.g 2017-06-15 10:51:00+0200)
     * @param sensor the sensor which provides the data
     *                 (e.g. http://www.phenome-fppn.fr/diaphen/2018/s18001)
     * @param incertitude the incertitude of the data
     *                  (e.g. 0.4)
     * @see phis2ws.service.json.DatasetsSerializer
     * @return data corresponding to the search params. Every data if no search 
     *         params.
     *         returned JSON : 
     *      {
     *          agronomicalObject: "http://.....",
     *          experiment: "http://....",
     *          data: [
     *              {
     *                  date: "....",
     *                  value: "...",
     *                  variable: "http://...."
     *              }
     *          ]
     *      }
     */
    @GET
    @ApiOperation(value = "Get all data corresponding to the search params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all datasets", response = Dataset.class, responseContainer = "List"),
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
    public Response getDataBySearch(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
        @ApiParam(value = "Search by experiment", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI) @QueryParam("experiment") @URL String experiment,
        @ApiParam(value = "Search by variable", example = DocumentationAnnotation.EXAMPLE_VARIABLE_URI) @QueryParam("variable") @URL String variable,
        @ApiParam(value = "Search by agronomical(s) object(s), separated by coma", example = DocumentationAnnotation.EXAMPLE_SCIENTIFIC_OBJECT_URI + "," + DocumentationAnnotation.EXAMPLE_SCIENTIFIC_OBJECT_URI) @QueryParam("agronomicalObjects") String agronomicalObjects,
        @ApiParam(value = "Search by interval - Start date", example = DocumentationAnnotation.EXAMPLE_DATETIME) @QueryParam("startDate") @Date(DateFormat.YMD) String startDate,
        @ApiParam(value = "Search by interval - End date", example = DocumentationAnnotation.EXAMPLE_DATETIME) @QueryParam("endDate") @Date(DateFormat.YMD) String endDate,
        @ApiParam(value = "Search by sensor", example = DocumentationAnnotation.EXAMPLE_SENSOR_URI) @QueryParam("sensor")  @URL String sensor,
        @ApiParam(value = "Search by incertitude", example = DocumentationAnnotation.EXAMPLE_DATA_INCERTITUDE) @QueryParam("incertitude") String incertitude) {
        
        DatasetDAOMongo datasetDAOMongo = new DatasetDAOMongo();
        
        if (experiment != null) {
            datasetDAOMongo.experiment = experiment;
        }
        if (variable != null) {
            datasetDAOMongo.variable = variable;
        }
        if (startDate != null && endDate != null) {
            datasetDAOMongo.startDate = startDate;
            datasetDAOMongo.endDate = endDate;
        }
        if (agronomicalObjects != null) {
            //the agronomical object's uri must be separated by ","
            String[] agronomicalObjectsURIs = agronomicalObjects.split(",");
            datasetDAOMongo.agronomicalObjects.addAll(Arrays.asList(agronomicalObjectsURIs));
        }
        if (sensor != null) {
            datasetDAOMongo.sensor = sensor;
        }
        if (incertitude != null) {
            datasetDAOMongo.incertitude = incertitude;
        }
        
        datasetDAOMongo.user = userSession.getUser();
        datasetDAOMongo.setPage(page);
        datasetDAOMongo.setPageSize(pageSize);
        
        return getDatasetsData(datasetDAOMongo);
    }
}
