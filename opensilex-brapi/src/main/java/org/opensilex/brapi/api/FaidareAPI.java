//******************************************************************************
//                          StudiesAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.api;

import org.opensilex.brapi.model.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
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


import org.apache.commons.lang3.StringUtils;
import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.germplasm.api.GermplasmSearchFilter;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.api.ScientificObjectSearchDTO;
import org.opensilex.core.scientificObject.api.ScientificObjectNodeDTO;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectSearchFilter;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

/**
 * @see Brapi documentation V1.3 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3
 * @author Alice Boizet
 */
@Api("FAIDARE")
@Path("/FAIDARE/v1")
public class FaidareAPI implements BrapiCall {

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;

    @CurrentUser
    UserModel currentUser;

    /**
     * Overriding BrapiCall method
     *
     * @return Calls call information
     * @date 27 Aug 2018
     */
    @Override
    public ArrayList<Call> callInfo() {
        ArrayList<Call> calls = new ArrayList();
        ArrayList<String> calldatatypes = new ArrayList<>();
        calldatatypes.add("json");
        ArrayList<String> callMethods = new ArrayList<>();
        callMethods.add("GET");
        ArrayList<String> callVersions = new ArrayList<>();
        callVersions.add("1.3");
        Call call1 = new Call("studies/{studyDbId}", calldatatypes, callMethods, callVersions);
        Call call2 = new Call("studies/{studyDbId}/observations", calldatatypes, callMethods, callVersions);
        Call call3 = new Call("studies/{studyDbId}/observationvariables", calldatatypes, callMethods, callVersions);
        Call call4 = new Call("studies/{studyDbId}/observationunits", calldatatypes, callMethods, callVersions);
        Call call5 = new Call("studies", calldatatypes, callMethods, callVersions);

        calls.add(call1);
        calls.add(call2);
        calls.add(call3);
        calls.add(call4);
        calls.add(call5);

        return calls;
    }

    @GET
    @Path("studies/{studyDbId}")
    @ApiOperation(value = "Retrieve study details", notes = "Retrieve study details")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieve study details", response = StudyDetailsDTO.class)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)

    public Response getStudyDetails(
            @ApiParam(value = "Search by studyDbId", required = true) @PathParam("studyDbId") @NotNull URI studyDbId
    ) throws Exception {

        ExperimentDAO dao = new ExperimentDAO(sparql, nosql);
        ExperimentModel model = dao.get(studyDbId, currentUser);

        StudyDetailsDTO dto = StudyDetailsDTO.fromModel(model);
        DataDAO dataDAO = new DataDAO(nosql, sparql, fs);

        Set<URI> varListURI = dataDAO.getUsedVariablesByExpeSoDevice(currentUser, new ArrayList<>(Collections.singleton(studyDbId)), null, null);

        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);
        GermplasmSearchFilter filter = new GermplasmSearchFilter();
        filter.setExperiment(studyDbId);

        List<GermplasmModel> listGermplasm = germplasmDAO.search(filter, false).getList();
        List<URI> listUriGermplasm = new ArrayList<>();
        for (GermplasmModel germplasm : listGermplasm) {
            listUriGermplasm.add(germplasm.getUri());
        }

        List<Location> locationDbId = Location.fromFacilities(model.getFacilities());
        List<URI> locationURI = new ArrayList<>();
        for (Location location : locationDbId) {
            locationURI.add(location.getLocationDbId());
        }


        dto.setVariables(varListURI);
        dto.setGermplasm(listUriGermplasm);
        dto.setLocationDbId(locationURI);

        if (model != null) {
            return new SingleObjectResponse<>(dto).getResponse();
        } else {
            throw new NotFoundURIException(studyDbId);
        }

    }

    @GET
    @Path("studies/{studyDbId}/observationvariables")
    @ApiOperation(value = "List all the observation variables measured in the study.", notes = "List all the observation variables measured in the study.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = ObservationVariableDTO.class)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObservationVariables(
            @ApiParam(value = "studyDbId", required = true) @PathParam("studyDbId") @NotNull URI studyDbId,
            @ApiParam(value = "pageSize") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize,
            @ApiParam(value = "page") @QueryParam("page") @DefaultValue("0") @Min(0) int page
    ) throws Exception {

        DataDAO dataDAO = new DataDAO(nosql, sparql, fs);
        ListWithPagination<VariableModel> variables = dataDAO.getVariablesByExperiment(currentUser, studyDbId, page, pageSize);

        ListWithPagination<ObservationVariableDTO> resultDTOList = variables.convert(ObservationVariableDTO.class, ObservationVariableDTO::fromModel);
        return new BrapiPaginatedListResponse<>(resultDTOList).getResponse();
    }

    @GET
    @Path("studies/{studyDbId}/germplasm")
    @ApiOperation(value = "List all the germplasm in the study.", notes = "List all the germplasm in the study.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = GermplasmDTO.class)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGermplasm(
            @ApiParam(value = "studyDbId", required = true) @PathParam("studyDbId") @NotNull URI studyDbId,
            @ApiParam(value = "pageSize") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize,
            @ApiParam(value = "page") @QueryParam("page") @DefaultValue("0") @Min(0) int page
    ) throws Exception {

        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);
        GermplasmSearchFilter filter = new GermplasmSearchFilter();
        filter.setExperiment(studyDbId);
        filter.setPage(page);
        filter.setPageSize(pageSize);
        ListWithPagination<GermplasmModel> listGermplasm = germplasmDAO.search(filter, false);

        // Convert paginated list to DTO
        ListWithPagination<GermplasmDTO> resultDTOList = listGermplasm.convert(
                GermplasmDTO.class,
                GermplasmDTO::fromModel
        );
        return new BrapiPaginatedListResponse<>(resultDTOList).getResponse();
    }

    @GET
    @Path("studies/{studyDbId}/Location")
    @ApiOperation(value = "List all the details of locations in the study.", notes = "List all Locations in the study.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = Location.class)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLocation(
            @ApiParam(value = "studyDbId", required = true) @PathParam("studyDbId") @NotNull URI studyDbId,
            @ApiParam(value = "pageSize") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize,
            @ApiParam(value = "page") @QueryParam("page") @DefaultValue("0") @Min(0) int page
    ) throws Exception {

        ExperimentDAO experimentDao = new ExperimentDAO(sparql,nosql);
        ExperimentModel exp = experimentDao.get(studyDbId, currentUser);

        List<Location> resultDTOList = Location.fromFacilities(exp.getFacilities());

        ListWithPagination<Location> list = new ListWithPagination<>(resultDTOList,page,pageSize,resultDTOList.size());

        return new BrapiPaginatedListResponse<>(list).getResponse();
    }
}