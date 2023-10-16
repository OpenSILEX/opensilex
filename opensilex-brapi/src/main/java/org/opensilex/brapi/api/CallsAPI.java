//******************************************************************************
//                          BrapiModule.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.api;

import io.swagger.annotations.*;
import org.opensilex.brapi.responses.BrAPIv1CallListResponse;
import org.opensilex.brapi.model.BrAPIv1CallDTO;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.utils.ListWithPagination;

import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Calls resource service.
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
@Api("BRAPI")
@Path("/brapi/")
@ApiCredentialGroup(
        groupId = CallsAPI.CREDENTIAL_CALLS_GROUP_ID,
        groupLabelKey = CallsAPI.CREDENTIAL_CALLS_GROUP_LABEL_KEY
)
public class CallsAPI extends BrapiCall {
    public static final String CREDENTIAL_CALLS_GROUP_ID = "brapi-calls";
    public static final String CREDENTIAL_CALLS_GROUP_LABEL_KEY = "credential-group-brapi-calls";

    @GET
    @Path("v1/calls")
    @ApiOperation(
            value = "Check the available BrAPI calls",
            notes = "Check the available BrAPI calls"
    )
    @BrapiVersion("1.3")
    @ApiResponses(value = {
        @ApiResponse(
                code = 200,
                message = "Retrieve BrAPI calls",
                response = BrAPIv1CallListResponse.class
        )
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCalls(
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize,
            @ApiParam(value = "datatype", example = "json") @QueryParam("dataType") String dataType) {
        List<BrAPIv1CallDTO> brapiCallsInfo = BrapiCall.getBrapiCallsInfo();
        ListWithPagination<BrAPIv1CallDTO> callsList = new ListWithPagination<>(brapiCallsInfo, page, pageSize, brapiCallsInfo.size());
        return new BrAPIv1CallListResponse(callsList).getResponse();
    }
}
