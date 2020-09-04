//******************************************************************************
//                          ProvenanceAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.nosql.service.NoSQLService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;

/**
 *
 * @author boizetal
 */
@Api(ProvenanceAPI.CREDENTIAL_PROVENANCE_GROUP_ID)
@Path("/core/provenance")
@ApiCredentialGroup(
        groupId = ProvenanceAPI.CREDENTIAL_PROVENANCE_GROUP_ID,
        groupLabelKey = ProvenanceAPI.CREDENTIAL_PROVENANCE_GROUP_LABEL_KEY
)
public class ProvenanceAPI {
    
    public static final String CREDENTIAL_PROVENANCE_GROUP_ID = "Provenances";
    public static final String CREDENTIAL_PROVENANCE_GROUP_LABEL_KEY = "credential-groups.provenance";

    public static final String CREDENTIAL_PROVENANCE_MODIFICATION_ID = "provenance-modification";
    public static final String CREDENTIAL_PROVENANCE_MODIFICATION_LABEL_KEY = "credential.provenance.modification";

    public static final String CREDENTIAL_PROVENANCE_READ_ID = "provenance-read";
    public static final String CREDENTIAL_PROVENANCE_READ_LABEL_KEY = "credential.provenance.read";

    public static final String CREDENTIAL_PROVENANCE_DELETE_ID = "provenance-delete";
    public static final String CREDENTIAL_PROVENANCE_DELETE_LABEL_KEY = "credential.provenance.delete";

    @CurrentUser
    UserModel currentUser;
    
    @Inject
    NoSQLService nosql;    
   
    @POST
    @Path("create")
    @ApiOperation("Create a provenance")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_PROVENANCE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_PROVENANCE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Create a provenance", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response createProvenance(
            @ApiParam("provenance description") @Valid ProvenanceCreationDTO provDTO
    ) throws Exception {

        ProvenanceDAO provDAO = new ProvenanceDAO(nosql);        
               
        URI  provenanceURI = provDAO.create(provDTO); 
        
        return new ObjectUriResponse(Response.Status.CREATED, provenanceURI).getResponse();

    }
    
    /**
     *
     * @param label
     * @param key
     * @param value
     * @param uri
     * @return
     * @throws java.lang.Exception
     */
    @GET
    @Path("get")
    @ApiOperation("Get lists of experiments where the germplasm has been used")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_PROVENANCE_READ_ID,
            credentialLabelKey = CREDENTIAL_PROVENANCE_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return provenance", response = ProvenanceGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Germplasm not found", response = ErrorDTO.class)
    })
    public Response getProvenance(
            @ApiParam(value = "label") @QueryParam("label") String label,
            @ApiParam(value = "metadataKey") @QueryParam("sensor URI") URI sensor,
            @ApiParam(value = "metadataValue") @QueryParam("operator URI") URI operator
    ) throws Exception {

        ProvenanceDAO dao = new ProvenanceDAO(nosql);
        List<ProvenanceModel> provenances = dao.search(label, sensor, operator);
        
        return new PaginatedListResponse<>(provenances).getResponse();
    }  

}
