//******************************************************************************
//                                Accession.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 9 sept. 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
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
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.AccessionDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.Accession;
import opensilex.service.resource.brapi.GermplasmResourceService;
import opensilex.service.resource.dto.accession.AccessionPostDTO;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Accession resource service
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
@Api("accession")
@Path("accession")
public class AccessionResourceService extends ResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(GermplasmResourceService.class);
    
    /**
     * Inserts accession in the storage.
     * @example
     * {
     *      
     * }
     * @param context
     * @return the post result with the errors or the uri of the inserted germplasm
     */
    @POST
    @ApiOperation(value = "Post Accession,",
                  notes = "Register new Accession in the database")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Accessions saved", response = ResponseFormPOST.class),
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
    public Response post(
        @ApiParam (value = DocumentationAnnotation.GERMPLASM_POST_DEFINITION) @Valid ArrayList<AccessionPostDTO> accession,
        @Context HttpServletRequest context) throws Exception {
        AbstractResultForm postResponse = null;
        
        if (accession != null && !accession.isEmpty()) {
            AccessionDAO accessionDAO = new AccessionDAO();
            
            accessionDAO.user = userSession.getUser();
            
            POSTResultsReturn result = accessionDAO.checkAndInsert(accessionPostDTOsToAccession(accession));
            
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
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty accession(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Generates a germplasm list from a given list of AccessionPostDTO
     * @param accessionDTOs
     * @return the list of sensors
     */
    private List<Accession> accessionPostDTOsToAccession(List<AccessionPostDTO> accessionDTOs) throws Exception {
        ArrayList<Accession> accessions = new ArrayList<>();
        
        for (AccessionPostDTO accessionDTO : accessionDTOs) {
            accessions.add(accessionDTO.createObjectFromDTO());
        }
        
        return accessions;
    }
    
}
