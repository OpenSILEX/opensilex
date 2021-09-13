//******************************************************************************
//                          VariablesAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.brapi.model.Call;
import org.opensilex.brapi.model.ObservationVariableDTO;
import org.opensilex.brapi.model.StudyDetailsDTO;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.URL;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

/**
 * @see Brapi documentation V1.3 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3
 * @author Alice Boizet
 */
@Api("BRAPI")
@Path("/brapi/v1")
public class VariablesAPI implements BrapiCall {
    
    @Inject
    private SPARQLService sparql;
    @Inject
    private MongoDBService mongodb;
    @Inject
    private FileStorageService fs;
    
    @CurrentUser
    UserModel currentUser;
    
    @Override
    public ArrayList<Call> callInfo() {
        ArrayList<Call> calls = new ArrayList();

        //SILEX:info 
        //Calls description
        ArrayList<String> calldatatypes = new ArrayList<>();
        calldatatypes.add("json");
        ArrayList<String> callMethods = new ArrayList<>();
        callMethods.add("GET");
        ArrayList<String> callVersions = new ArrayList<>();
        callVersions.add("1.3");
        Call call1 = new Call("variables", calldatatypes, callMethods, callVersions);
        Call call2 = new Call("variables/{variables}", calldatatypes, callMethods, callVersions);
        //Call call3 = new Call("observationvariables?studyDbId={studyDbId}", calldatatypes, callMethods, callVersions);
        //\SILEX:info       

        calls.add(call1);
        calls.add(call2);
        //calls.add(call3);

        return calls;
    }
    
    
    @GET
    @Path("variables")
    @ApiOperation(value = "Call to retrieve a list of observationVariables available in the system",
            notes = "retrieve variables information")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "retrieve variables information", response = ObservationVariableDTO.class, responseContainer = "List")})
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
                List<VariableModel> variablesList = new ArrayList<>();
                variablesList.add(variable);
                return new SingleObjectResponse<>(ObservationVariableDTO.fromModel(variable)).getResponse();
            } else {
                throw new NotFoundURIException(observationVariableDbId);
            }            
        } else {
            variables = varDAO.search(null, null, page, pageSize); 
            ListWithPagination<ObservationVariableDTO> resultDTOList = variables.convert(ObservationVariableDTO.class, ObservationVariableDTO::fromModel);
            return new BrapiPaginatedListResponse<>(resultDTOList).getResponse();
        }
        
    }
    
    @GET
    @Path("variables/{observationVariableDbId}")
    @ApiOperation(value = "Retrieve variable details by id",
            notes = "Retrieve variable details by id")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve variable details by id", response = ObservationVariableDTO.class, responseContainer = "List")})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVariableDetails(
            @ApiParam(value = "A variable URI (Unique Resource Identifier)", required = true) @PathParam("observationVariableDbId") @NotNull URI observationVariableDbId
    ) throws Exception {

        VariableDAO variableDAO = new VariableDAO(sparql,mongodb,fs);

        VariableModel variable = variableDAO.get(observationVariableDbId);
        if (variable != null) {
            return new SingleObjectResponse<>(ObservationVariableDTO.fromModel(variable)).getResponse();
        } else {
            throw new NotFoundURIException(observationVariableDbId);
        }        
    }
    
}
