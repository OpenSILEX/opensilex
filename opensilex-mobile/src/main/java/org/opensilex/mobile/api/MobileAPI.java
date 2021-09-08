//******************************************************************************
//                          MoblieModule.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.api;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.mobile.dal.FormDAO;
import org.opensilex.mobile.dal.FormModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;

/**
 *  
 * @author Alice Boizet
 */
@Api(MobileAPI.CREDENTIAL_MOBILE_GROUP_ID)
@Path("/mobile")
public class MobileAPI { 
    public static final String CREDENTIAL_MOBILE_GROUP_ID = "Mobile";
    
    @Inject
    private MongoDBService nosql;

    /**
     * Create a Area
     *
     * @param dto the Area to create
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the created Area {@link URI}
     * @throws java.lang.Exception if creation failed
     */
    @POST
    @Path("forms")
    @ApiOperation("Add a form")
    @ApiProtected
//    @ApiCredential(
//            credentialId = CREDENTIAL_AREA_MODIFICATION_ID,
//            credentialLabelKey = CREDENTIAL_AREA_MODIFICATION_LABEL_KEY
//    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Add a form", response = ObjectUriResponse.class),
            @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
//            @ApiResponse(code = 409, message = "A form with the same URI already exists", response = ErrorResponse.class),
     })

    public Response createForm(
            @ApiParam("Form to save") @NotNull @Valid FormDTO dto
    ) throws Exception {
        FormDAO dao = new FormDAO(nosql);
        FormModel createdForm = dao.create(dto.newModel());
        return new ObjectUriResponse(Response.Status.CREATED, createdForm.getUri()).getResponse();

    }
 
}
