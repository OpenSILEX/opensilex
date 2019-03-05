//******************************************************************************
//                      DataResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 1 March 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.mongo.DataDAOMongo;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.data.DataPostDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.model.phis.Data;

/**
 * Data resource service
 */
@Api("/data")
@Path("/data")
public class DataResourceService extends ResourceService {
    
    /**
     * Service to insert data. 
     * @example
     * [
     *  {
     *      "objectUri": "http://www.phenome-fppn.fr/diaphen/2018/s18521",
     *      "variableUri": "http://www.phenome-fppn.fr/id/variables/v001",
     *      "date": "2017-06-15T10:51:00+0200",
     *      "value": "0.5"
     *  }
     * ]
     * @param rawData
     * @param context
     * @return the insertion result. 
     */
    @POST
    @ApiOperation(value = "Post data",
                  notes = "Register data in the database")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "data saved", response = ResponseFormPOST.class),
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
    public Response postData(
        @ApiParam(value = DocumentationAnnotation.DATA_POST_DEFINITION) @Valid List<DataPostDTO> data,
        @Context HttpServletRequest context) {
        
        AbstractResultForm postResponse = null;
        
        try {
            if (data != null && !data.isEmpty()) {
                DataDAOMongo dataDAO = new DataDAOMongo();

                dataDAO.user = userSession.getUser();

                POSTResultsReturn result = dataDAO.checkAndInsert(dataDTOsToData(data));

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
     * Generayes an data list from a given list of DataPostDTO.
     * @param environmentDTOs
     * @return the list of environments
     */
    private List<Data> dataDTOsToData(List<DataPostDTO> dataDTOs) throws ParseException {
        ArrayList<Data> dataList = new ArrayList<>();
        
        for (DataPostDTO dataDTO : dataDTOs) {
            dataList.add(dataDTO.createObjectFromDTOWithException());            
        }
        
        return dataList;
    }
}
