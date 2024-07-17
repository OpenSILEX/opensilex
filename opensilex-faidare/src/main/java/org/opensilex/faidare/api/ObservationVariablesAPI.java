//******************************************************************************
//                          VariablesAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.api;

import io.swagger.annotations.*;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.faidare.builder.Faidarev1ObservationVariableDTOBuilder;
import org.opensilex.faidare.model.Faidarev1ObservationVariableDTO;
import org.opensilex.faidare.responses.Faidarev1ObservationVariableListResponse;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;

/**
 * @author Gabriel Besombes
 */
@Api(CallsAPI.CREDENTIAL_CALLS_GROUP_ID)
@Path("/faidare/")
@ApiCredentialGroup(
        groupId = CallsAPI.CREDENTIAL_CALLS_GROUP_ID,
        groupLabelKey = CallsAPI.CREDENTIAL_CALLS_GROUP_LABEL_KEY
)
public class ObservationVariablesAPI extends FaidareCall {
    
    @Inject
    private SPARQLService sparql;
    @Inject
    private MongoDBService mongodb;
    @Inject
    private FileStorageService fs;
    
    @CurrentUser
    AccountModel currentUser;
    
    
    @GET
    @Path("v1/variables")
    @FaidareVersion("1.3")
    @ApiOperation(value = "Faidarev1CallDTO to retrieve a list of observationVariables available in the system",
            notes = "retrieve variables information")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "retrieve variables information", response = Faidarev1ObservationVariableListResponse.class)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVariablesList(
            @ApiParam(value = "observationVariableDbId") @QueryParam("observationVariableDbId") URI observationVariableDbId,
            @ApiParam(value = "pageSize") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize,
            @ApiParam(value = "page") @QueryParam("page") @DefaultValue("0") @Min(0) int page
    ) throws Exception {
        VariableDAO varDAO = new VariableDAO(sparql,mongodb,fs, currentUser);

        ListWithPagination<VariableModel> variables;
        if (observationVariableDbId != null) {
            VariableModel variable = varDAO.get(observationVariableDbId);
            if (variable != null) {
                variables = new ListWithPagination<>(Collections.singletonList(variable));
            } else {
                throw new NotFoundURIException(observationVariableDbId);
            }            
        } else {
            variables = varDAO.search(null, null, page, pageSize, currentUser.getLanguage());
        }

        Faidarev1ObservationVariableDTOBuilder observationVariableDTOBuilder = new Faidarev1ObservationVariableDTOBuilder();
        ListWithPagination<Faidarev1ObservationVariableDTO> resultDTOList = variables.convert(
                Faidarev1ObservationVariableDTO.class,
                observationVariableDTOBuilder::fromModel
        );
        return new Faidarev1ObservationVariableListResponse(resultDTOList).getResponse();
    }
    
}
