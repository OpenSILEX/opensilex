//**********************************************************************************************
//                                       LabelResourceService.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date:Feb 1 2018
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Fevrier 2, 2018
// Subject: Represente the Label data service
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
import phis2ws.service.dao.sesame.LabelDaoSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormLabel;
import phis2ws.service.view.model.phis.Label;



@Api("/labels")
@Path("labels")
/**
 *
 * @author Eloan LAGIER
 */
public class LabelResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(LabelResourceService.class);

    //Session utilisateur
    @SessionInject
    Session userSession;
    
    private Response noResultFound(ResponseFormLabel getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No results", StatusCodeMsg.INFO, "No results for the label"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
        
        
    /**
     * searche all concept or instance with the label given
     * @param limit
     * @param page
     * @param name
     * @return Response
     */
    @GET
    @ApiOperation(value = "get all concepts with this label",
                    notes = "Retrieve all concepts from the label given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all labels", response = Label.class, responseContainer = "List"),
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
    public Response getLabelBySearch(
            @ApiParam( value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page,
            @ApiParam(value = "Search by label", example = DocumentationAnnotation.EXAMPLE_CONCEPT_LABEL) @QueryParam("label") String name) {
            
            LabelDaoSesame labelDao = new LabelDaoSesame();
            if (name != null ) {
                labelDao.name = name;
            }
            
            labelDao.user =userSession.getUser();
            labelDao.setPage(page);
            labelDao.setPageSize(limit);
            
            return getLabelMetaData(labelDao);
    }

    /** getLabelMetaData:
     * 
     * @param labelDao
     * collect all the Label data
     */
    private Response getLabelMetaData(LabelDaoSesame labelDao) {
        ArrayList<Label> labels ; 
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormLabel getResponse;
        
        labels = labelDao.AllPaginate();
        LOGGER.debug("LABELS="+labels.toString());
        if (labels == null) {
            getResponse = new ResponseFormLabel(0, 0, labels, true);
            return noResultFound(getResponse, statusList);
        } else if (!labels.isEmpty()) {
            getResponse = new ResponseFormLabel(labelDao.getPageSize(), labelDao.getPage(), labels, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
        } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();   
            }
        } else {
            getResponse = new ResponseFormLabel(0, 0, labels ,true);
            return noResultFound(getResponse, statusList);
            
        }
    }
    
}
