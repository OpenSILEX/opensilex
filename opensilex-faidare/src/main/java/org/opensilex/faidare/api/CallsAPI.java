/*
 * *****************************************************************************
 *                         CallsAPI.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 25/05/2024 00:00
 * Contact: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
 * *****************************************************************************
 */
package org.opensilex.faidare.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import org.opensilex.faidare.model.Faidarev1CallDTO;
import org.opensilex.faidare.responses.Faidarev1CallListResponse;
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
@Tag(name = CallsAPI.CREDENTIAL_CALLS_GROUP_ID)
@Path("/faidare/")
@ApiCredentialGroup(
        groupId = CallsAPI.CREDENTIAL_CALLS_GROUP_ID,
        groupLabelKey = CallsAPI.CREDENTIAL_CALLS_GROUP_LABEL_KEY
)
public class CallsAPI extends FaidareCall {
    public static final String CREDENTIAL_CALLS_GROUP_ID = "Faidare";
    public static final String CREDENTIAL_CALLS_GROUP_LABEL_KEY = "credential-group-faidare-calls";

    @GET
    @Path("v1/calls")
    @Operation(summary = "Check the available faidare calls",
            description = "Check the available faidare calls")
    @ApiProtected
    @FaidareVersion("1.3")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                description = "Retrieve faidare calls",
                content = @Content(schema = @Schema(implementation = Faidarev1CallListResponse.class)))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCalls(
            @Parameter(description = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize) {
        List<Faidarev1CallDTO> faidareCallsInfo = FaidareCall.getfaidareCallsInfo();
        ListWithPagination<Faidarev1CallDTO> callsList = new ListWithPagination<>(faidareCallsInfo, page, pageSize, faidareCallsInfo.size());
        return new Faidarev1CallListResponse(callsList).getResponse();
    }
}
