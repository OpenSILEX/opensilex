//******************************************************************************
//                                       ProvenanceResourceService.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 4 mars 2019
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
import phis2ws.service.dao.mongo.ProvenanceDAOMongo;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.ProvenanceDTO;
import phis2ws.service.resources.dto.provenance.ProvenancePostDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.model.phis.provenance.Provenance;

/**
 * Provenance resource service
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/provenances")
@Path("/provenances")
public class ProvenanceResourceService extends ResourceService {
    
    /**
     * Generates a RadiometricTarget list from a given list of RadiometricTargetPostDTO
     * @param provenanceDTOs
     * @return the list of radiometric targets
     */
    private List<Provenance> provenancePostDTOsToprovenances(List<ProvenancePostDTO> provenanceDTOs) {
        ArrayList<Provenance> provenances = new ArrayList<>();
        
        for (ProvenancePostDTO provenancePostDTO : provenanceDTOs) {
            provenances.add(provenancePostDTO.createObjectFromDTO());
        }
        
        return provenances;
    }
    
    /**
     * Insert provenances.
     * @example
     * 
     * @param provenances
     * @param context
     * @return the insertion result
     */
    @POST
    @ApiOperation(value = "Post provenance(s)",
                  notes = "Register provenance(s) in the database")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "provenance(s) saved", response = ResponseFormPOST.class),
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
        @ApiParam (value = DocumentationAnnotation.PROVENACE_POST_DEFINITION) @Valid ArrayList<ProvenancePostDTO> provenances,
        @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        if (provenances != null && !provenances.isEmpty()) {
            ProvenanceDAOMongo provenanceDAO = new ProvenanceDAOMongo();
            
            provenanceDAO.user = userSession.getUser();
            
            POSTResultsReturn result = provenanceDAO.checkAndInsert(provenancePostDTOsToprovenances(provenances));
            
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
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty provenances(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
}
