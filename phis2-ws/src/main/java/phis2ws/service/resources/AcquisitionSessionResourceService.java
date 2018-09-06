//******************************************************************************
//                                       AcquisitionSessionResourceService.java
// SILEX-PHIS
// Copyright © INRA
// Creation date: 30 August, 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************

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
import phis2ws.service.dao.sesame.AcquisitionSessionDAOSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.resources.dto.MetadataFileDTO;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormMetadataFile;
import phis2ws.service.view.manager.ResultForm;

/**
 * Acquisition session service.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/acquisitionSessions")
@Path("/acquisitionSessions")
public class AcquisitionSessionResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(AcquisitionSessionResourceService.class);
    
    //user session
    @SessionInject
    Session userSession;
    
    /**
     * Generic reponse for not found acquisition session.
     * SILEX:todo
     * Need to create an abstract resource class to make this method more generic
     * \SILEX:todo
     * @param getResponse
     * @param insertStatusList
     * @return 
     */
    private Response noResultFound(ResultForm getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status(StatusCodeMsg.NO_RESULTS, StatusCodeMsg.INFO, "No results for the acquisition session"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }

    /**
     * Search acquisition session metadata file metadata corresponding to the given type of file wanted.
     * @param acquisitionSessionDAOSesame
     * @return the acquisition session file metadata content for the hiddenPhis 
     *         part of the acquisition session file used for 4P.
     */
    private Response getAcquisitionSessionMetadataFile(AcquisitionSessionDAOSesame acquisitionSessionDAOSesame) {       
        ArrayList<MetadataFileDTO> fileMetadata;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormMetadataFile getResponse;
        
        //Retrieve file format.
        fileMetadata = acquisitionSessionDAOSesame.allPaginateFileMetadata();
        
        if (fileMetadata == null) {
            getResponse = new ResponseFormMetadataFile(0, 0, fileMetadata, true);
            return noResultFound(getResponse, statusList);
        } else if (fileMetadata.isEmpty()) {
            getResponse = new ResponseFormMetadataFile(0, 0, fileMetadata, true);
            return noResultFound(getResponse, statusList);
        } else {
            getResponse = new ResponseFormMetadataFile(1, 0, fileMetadata, true);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }
    
    //SILEX:todo
    //add the rules
    //\SILEX:todo
    /**
     * Service to get the hidenPhis content of the excel file used to define 
     * acquisition session for the 4P platform.
     * @param vectorRdfType
     * @param pageSize
     * @param page
     * @return e.g.
     * {
     *      "metadata": {
     *          "pagination": {
     *              "pageSize": 1,
     *              "currentPage": 0,
     *              "totalCount": 23,
     *              "totalPages": 23
     *          },
     *          "status": [],
     *          "datafiles": []
     *      },
     *      "result": {
     *          "data": [
     *              {
     *                  "Installation": null,
     *                  "GroupPlot_type": "http://www.phenome-fppn.fr/vocabulary/2017#Experiment",
     *                  "GroupPlot_alias": "",
     *                  "GroupPlot_uri": "http://www.phenome-fppn.fr/mauguio/diaphen/DIA2008-1",
     *                  "GroupPlot_species": "",
     *                  "Pilot": "trudqfc@truc.fr"
     *              },
     *              {
     *                  "Installation": null,
     *                  "GroupPlot_type": "http://www.phenome-fppn.fr/vocabulary/2017#Experiment",
     *                  "GroupPlot_alias": "testURI2",
     *                  "GroupPlot_uri": "http://www.phenome-fppn.fr/mauguio/diaphen/DIA2015-1",
     *                  "GroupPlot_species": "",
     *                  "Pilot": "userTest@group.fr"
     *              },
     *          ]
     *      }
     * }
     */
    @GET
    @Path("metadataFile")
    @ApiOperation(value = "Get the metadata for the acquisition session file",
            notes = "Retrieve the metadata for the acquisition session file. Need URL encoded of the vector type of the acquisition session (uav or drone)")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve a annotation", response = MetadataFileDTO.class, responseContainer = "List"),
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
    public Response get4PMetadataFile(
            @ApiParam(value = DocumentationAnnotation.VECTOR_RDF_TYPE_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_VECTOR_RDF_TYPE) @QueryParam("vectorRdfType") String vectorRdfType,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page) {
        AcquisitionSessionDAOSesame acquisitionSessionDAO = new AcquisitionSessionDAOSesame();
        acquisitionSessionDAO.vectorRdfType = vectorRdfType;
        acquisitionSessionDAO.setPage(page);
        acquisitionSessionDAO.setPageSize(pageSize);
        acquisitionSessionDAO.user = userSession.getUser();
        
        return getAcquisitionSessionMetadataFile(acquisitionSessionDAO);
    }
}
