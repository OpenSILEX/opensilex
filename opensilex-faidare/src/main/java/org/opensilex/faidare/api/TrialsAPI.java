//******************************************************************************
//                          VariablesAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
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
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.faidare.builder.Faidarev1TrialDTOBuilder;
import org.opensilex.faidare.model.Faidarev1TrialDTO;
import org.opensilex.faidare.responses.Faidarev1TrialListResponse;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Gabriel Besombes
 */
@Tag(name = CallsAPI.CREDENTIAL_CALLS_GROUP_ID)
@Path("/faidare/")
@ApiCredentialGroup(
        groupId = CallsAPI.CREDENTIAL_CALLS_GROUP_ID,
        groupLabelKey = CallsAPI.CREDENTIAL_CALLS_GROUP_LABEL_KEY
)
public class TrialsAPI extends FaidareCall {
    
    @Inject
    private SPARQLService sparql;
    @Inject
    private MongoDBService mongodb;
    @Inject
    private FileStorageService fs;
    
    @CurrentUser
    AccountModel currentUser;
    
    
    @GET
    @Path("v1/trials")
    @FaidareVersion("1.3")
    @Operation(summary = "Faidarev1CallDTO to retrieve a list of trials available in the system",
            description = "retrieve trials information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "retrieve trials information", content = @Content(schema = @Schema(implementation = Faidarev1TrialListResponse.class)))})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTrialsList(
            @Parameter(description = "pageSize") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize,
            @Parameter(description = "page") @QueryParam("page") @DefaultValue("0") @Min(0) int page
    ) throws Exception {
        ProjectDAO projectDAO = new ProjectDAO(sparql);

        ListWithPagination<ProjectModel> projects;
        projects = projectDAO.search(
                null, null, null, null, currentUser, null, page, pageSize
        );

        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, mongodb);
        Faidarev1TrialDTOBuilder builder = new Faidarev1TrialDTOBuilder(experimentDAO);
        ListWithPagination<Faidarev1TrialDTO> resultDTOList = projects.convert(
                Faidarev1TrialDTO.class,
                projectModel -> {
                    try {
                        return builder.fromModel(projectModel, currentUser);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        return new Faidarev1TrialListResponse(resultDTOList).getResponse();
    }
    
}
