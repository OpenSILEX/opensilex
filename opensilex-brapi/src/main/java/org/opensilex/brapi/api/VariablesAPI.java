//******************************************************************************
//                          VariablesAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.api;

import io.swagger.annotations.*;
import org.opensilex.brapi.responses.BrAPIv1ObservationVariableListResponse;
import org.opensilex.brapi.responses.BrAPIv1SingleObservationVariableResponse;
import org.opensilex.brapi.model.BrAPIv1ObservationVariableDTO;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
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
@Api("BRAPI")
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
    @ApiOperation(value = "BrAPIv1CallDTO to retrieve a list of observationVariables available in the system",
            notes = "retrieve variables information")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "retrieve variables information", response = BrAPIv1ObservationVariableListResponse.class)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVariablesList(
            @ApiParam(value = "observationVariableDbId") @QueryParam("observationVariableDbId") URI observationVariableDbId,
            @ApiParam(value = "pageSize") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize,
            @ApiParam(value = "page") @QueryParam("page") @DefaultValue("0") @Min(0) int page
    ) throws Exception {
        VariableDAO varDAO = new VariableDAO(sparql,mongodb,fs);

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

        ListWithPagination<BrAPIv1ObservationVariableDTO> resultDTOList = variables.convert(BrAPIv1ObservationVariableDTO.class, BrAPIv1ObservationVariableDTO::fromModel);
        return new BrAPIv1ObservationVariableListResponse(resultDTOList).getResponse();
    }
    
    @GET
    @Path("v1/variables/{observationVariableDbId}")
    @BrapiVersion("1.3")
    @ApiOperation(value = "Retrieve variable details by id",
            notes = "Retrieve variable details by id")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve variable details by id", response = BrAPIv1SingleObservationVariableResponse.class)}) // TODO : wrong return type
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVariableDetails(
            @ApiParam(value = "A variable URI (Unique Resource Identifier)", required = true) @PathParam("observationVariableDbId") @NotNull URI observationVariableDbId
    ) throws Exception {

        VariableDAO variableDAO = new VariableDAO(sparql,mongodb,fs);

        VariableModel variable = variableDAO.get(observationVariableDbId);
        if (variable != null) {
            return new BrAPIv1SingleObservationVariableResponse(BrAPIv1ObservationVariableDTO.fromModel(variable)).getResponse();
        } else {
            throw new NotFoundURIException(observationVariableDbId);
        }        
    }
    
}
