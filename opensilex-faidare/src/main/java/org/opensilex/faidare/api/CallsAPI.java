//******************************************************************************
//                          FaidareModule.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.api;

import io.swagger.annotations.*;
import org.opensilex.faidare.responses.Faidarev1CallListResponse;
import org.opensilex.faidare.model.Faidarev1CallDTO;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.utils.ListWithPagination;

import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Calls resource service.
 * @author Gabriel Besombes
 */
@Api(CallsAPI.CREDENTIAL_CALLS_GROUP_ID)
@Path("/faidare/")
@ApiCredentialGroup(
        groupId = CallsAPI.CREDENTIAL_CALLS_GROUP_ID,
        groupLabelKey = CallsAPI.CREDENTIAL_CALLS_GROUP_LABEL_KEY
)
public class CallsAPI extends FaidareCall {
    public static final String CREDENTIAL_CALLS_GROUP_ID = "faidare";
    public static final String CREDENTIAL_CALLS_GROUP_LABEL_KEY = "credential-group-faidare-calls";

    @GET
    @Path("v1/calls")
    @ApiOperation(
            value = "Check the available faidare calls",
            notes = "Check the available faidare calls"
    )
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_CALLS_GROUP_ID,
            credentialLabelKey = CREDENTIAL_CALLS_GROUP_LABEL_KEY
    )
    @FaidareVersion("1.3")
    @ApiResponses(value = {
        @ApiResponse(
                code = 200,
                message = "Retrieve faidare calls",
                response = Faidarev1CallListResponse.class
        )
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCalls(
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize,
            @ApiParam(value = "datatype", example = "json") @QueryParam("dataType") String dataType) {
        List<Faidarev1CallDTO> faidareCallsInfo = FaidareCall.getfaidareCallsInfo();
        ListWithPagination<Faidarev1CallDTO> callsList = new ListWithPagination<>(faidareCallsInfo, page, pageSize, faidareCallsInfo.size());
        return new Faidarev1CallListResponse(callsList).getResponse();
    }
}
