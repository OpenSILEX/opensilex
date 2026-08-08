//******************************************************************************
//                          VariablesAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.api;

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
import org.opensilex.brapi.responses.BrAPIv1ObservationVariableListResponse;
import org.opensilex.brapi.responses.BrAPIv1SingleObservationVariableResponse;
import org.opensilex.brapi.model.BrAPIv1ObservationVariableDTO;
import org.opensilex.core.variable.dal.BaseVariableDAO;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
@Tag(name = "BRAPI")
@Path("/brapi/")
public class VariablesAPI extends BrapiCall {
    
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
    @BrapiVersion("1.3")
    @Operation(summary = "BrAPIv1CallDTO to retrieve a list of observationVariables available in the system",
            description = "retrieve variables information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "retrieve variables information", content = @Content(schema = @Schema(implementation = BrAPIv1ObservationVariableListResponse.class)))})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVariablesList(
            @Parameter(description = "observationVariableDbId") @QueryParam("observationVariableDbId") URI observationVariableDbId,
            @Parameter(description = "pageSize") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize,
            @Parameter(description = "page") @QueryParam("page") @DefaultValue("0") @Min(0) int page
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
            variables = varDAO.search(null, null, page, pageSize,currentUser.getLanguage());
        }

        BaseVariableDAO<MethodModel> baseVariableDAO = new BaseVariableDAO<>(MethodModel.class, sparql);
        ListWithPagination<BrAPIv1ObservationVariableDTO> resultDTOList = variables.convert(
                BrAPIv1ObservationVariableDTO.class,
                variableModel -> {
                    try {
                        return BrAPIv1ObservationVariableDTO.fromModel(variableModel, baseVariableDAO);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        return new BrAPIv1ObservationVariableListResponse(resultDTOList).getResponse();
    }
    
    @GET
    @Path("v1/variables/{observationVariableDbId}")
    @BrapiVersion("1.3")
    @Operation(summary = "Retrieve variable details by id",
            description = "Retrieve variable details by id")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieve variable details by id", content = @Content(schema = @Schema(implementation = BrAPIv1SingleObservationVariableResponse.class)))}) // TODO : wrong return type
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVariableDetails(
            @Parameter(description = "A variable URI (Unique Resource Identifier)", required = true) @PathParam("observationVariableDbId") @NotNull URI observationVariableDbId
    ) throws Exception {

        VariableDAO variableDAO = new VariableDAO(sparql,mongodb,fs, currentUser);

        VariableModel variable = variableDAO.get(observationVariableDbId);
        if (variable != null) {
            BaseVariableDAO<MethodModel> baseVariableDAO = new BaseVariableDAO<>(MethodModel.class, sparql);
            return new BrAPIv1SingleObservationVariableResponse(
                    BrAPIv1ObservationVariableDTO.fromModel(variable, baseVariableDAO)
            ).getResponse();
        } else {
            throw new NotFoundURIException(observationVariableDbId);
        }        
    }
    
}
