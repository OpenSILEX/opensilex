//******************************************************************************
//                       AcquisitionSessionResourceService.java
// SILEX-PHIS
// Copyright © INRA
// Creation date: 30 August 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************

package opensilex.service.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.AcquisitionSessionDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.dto.acquisitionSession.MetadataFileDTO;
import opensilex.service.resource.dto.acquisitionSession.MetadataFileUAVDTO;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.view.brapi.Status;
import opensilex.service.result.ResultForm;

/**
 * Acquisition session resource service.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/acquisitionSessions")
@Path("/acquisitionSessions")
public class AcquisitionSessionResourceService extends ResourceService {
    
    /**
     * Searches acquisition session metadata file metadata corresponding to the given type of file wanted.
     * @param acquisitionSessionDAO
     * @return the acquisition session file metadata content for the hiddenPhis 
     *         part of the acquisition session file used for 4P.
     */
    private Response getAcquisitionSessionMetadataFile(AcquisitionSessionDAO acquisitionSessionDAO) {       
        ArrayList<MetadataFileDTO> fileMetadata;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<MetadataFileDTO> getResponse;
        
        //Retrieve file format.
        fileMetadata = acquisitionSessionDAO.allPaginateFileMetadata();
        
        Integer count = acquisitionSessionDAO.countFileMetadataRows();
        
        if (fileMetadata == null) {
            getResponse = new ResultForm<>(0, 0, new ArrayList<>(), true);
            return noResultFound(getResponse, statusList);
        } else if (fileMetadata.isEmpty()) {
            getResponse = new ResultForm<>(0, 0, fileMetadata, true);
            return noResultFound(getResponse, statusList);
        } else {
            //SILEX:info
            //In this service, the total count is equals to the number of rows
            //\SILEX:info
            getResponse = new ResultForm<>(acquisitionSessionDAO.getPageSize(), acquisitionSessionDAO.getPage(), fileMetadata, true, count);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }
    
    /**
     * Service to get the hiddenPhis content of the excel file used to define 
     * acquisition sessions for the 4P platform.
     * @param vectorRdfType
     * @param pageSize
     * @param page
     * @example {
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
     *                  "GroupPlot_type": "http://www.opensilex.org/vocabulary/oeso#Experiment",
     *                  "GroupPlot_alias": "",
     *                  "GroupPlot_uri": "http://www.phenome-fppn.fr/mauguio/diaphen/DIA2008-1",
     *                  "GroupPlot_species": "",
     *                  "Pilot": "trudqfc@truc.fr"
     *              },
     *              {
     *                  "Installation": null,
     *                  "GroupPlot_type": "http://www.opensilex.org/vocabulary/oeso#Experiment",
     *                  "GroupPlot_alias": "testURI2",
     *                  "GroupPlot_uri": "http://www.phenome-fppn.fr/mauguio/diaphen/DIA2015-1",
     *                  "GroupPlot_species": "",
     *                  "Pilot": "userTest@group.fr"
     *              },
     *          ]
     *      }
     * }
     * @return the query result, which will be json formatted
     */
    @GET
    @Path("metadataFile")
    @ApiOperation(value = "Get the metadata for the acquisition session file",
            notes = "Retrieve the metadata for the acquisition session file. Need URL encoded of the vector type of the acquisition session (uav or drone)")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve a annotation", response = MetadataFileUAVDTO.class, responseContainer = "List"),
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
            @ApiParam(value = DocumentationAnnotation.VECTOR_RDF_TYPE_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_VECTOR_RDF_TYPE) @QueryParam("vectorRdfType") @Required @URL String vectorRdfType,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {
        AcquisitionSessionDAO acquisitionSessionDAO = new AcquisitionSessionDAO();
        acquisitionSessionDAO.vectorRdfType = vectorRdfType;
        acquisitionSessionDAO.setPage(page);
        acquisitionSessionDAO.setPageSize(pageSize);
        acquisitionSessionDAO.user = userSession.getUser();
        
        return getAcquisitionSessionMetadataFile(acquisitionSessionDAO);
    }
}
