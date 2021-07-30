//******************************************************************************
//                          StudiesAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.api;

import org.opensilex.brapi.model.ObservationUnitDTO;
import org.opensilex.brapi.model.ObservationVariableDTO;
import org.opensilex.brapi.model.StudyDetailsDTO;
import org.opensilex.brapi.model.ObservationDTO;
import org.opensilex.brapi.model.StudyDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
import org.opensilex.brapi.model.Call;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.api.ScientificObjectNodeDTO;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
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
@Api("BRAPI")
@Path("/brapi/v1")
public class StudiesAPI implements BrapiCall {

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

    /**
     * Retrieve studies information
     *
     * @param studyDbId
     * @param seasonDbId
     * @param active
     * @param sortBy
     * @param sortOrder
     * @param pageSize
     * @param page
     * @return the study information
     * @throws java.sql.SQLException
     */
    @GET
    @Path("studies")
    @ApiOperation(value = "Retrieve studies information", notes = "Retrieve studies information")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve studies information", response = StudyDTO.class, responseContainer = "List")})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)

    public Response getStudies(
            @ApiParam(value = "Search by studyDbId") @QueryParam("studyDbId") URI studyDbId,
            @ApiParam(value = "Filter active status true/false") @QueryParam("active") String active,
            @ApiParam(value = "Name of the field to sort by: studyDbId, active") @QueryParam("sortBy") String sortBy,
            @ApiParam(value = "Sort order direction - ASC or DESC") @QueryParam("sortOrder") String sortOrder,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws SQLException, Exception {

        ArrayList<OrderBy> orderByList = new ArrayList();

        if (!StringUtils.isEmpty(sortBy)) {
            if (null == sortBy) {
                sortBy = "";
            } else {
                switch (sortBy) {
                    case "studyDbId":
                        sortBy = "uri";
                        break;
                    case "seasonDbId":
                        sortBy = "campaign";
                        break;
                    default:
                        sortBy = "";
                        break;
                }
            }
            String orderByStr;
            if (!StringUtils.isEmpty(sortOrder)) {
                orderByStr = sortBy + "=" + sortOrder;
            } else {
                orderByStr = sortBy + "=" + "desc";
            }
            OrderBy order = new OrderBy(orderByStr);
            orderByList.add(order);
        }

        Boolean isEnded = !StringUtils.isEmpty(active) ? !Boolean.parseBoolean(active) : null;

        ExperimentDAO xpDao = new ExperimentDAO(sparql);
        if (studyDbId != null) {
            ExperimentModel model = xpDao.get(studyDbId, currentUser);
            if (model != null) {
                return new SingleObjectResponse<>(StudyDetailsDTO.fromModel(model)).getResponse();
            } else {
                throw new NotFoundURIException(studyDbId);
            }
        } else {
            ListWithPagination<ExperimentModel> resultList = xpDao.search(null, null, null, null, isEnded, null, null, currentUser, orderByList, page, pageSize);
            ListWithPagination<StudyDTO> resultDTOList = resultList.convert(StudyDTO.class, StudyDTO::fromModel);
            return new PaginatedListResponse<>(resultDTOList).getResponse();
        }

    }

    @GET
    @Path("studies/{studyDbId}")
    @ApiOperation(value = "Retrieve study details", notes = "Retrieve study details")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve study details", response = StudyDetailsDTO.class, responseContainer = "List")})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)

    public Response getStudyDetails(
            @ApiParam(value = "Search by studyDbId", required = true) @PathParam("studyDbId") @NotNull URI studyDbId
    ) throws Exception {

        ExperimentDAO dao = new ExperimentDAO(sparql);
        ExperimentModel model = dao.get(studyDbId, currentUser);

        if (model != null) {
            return new SingleObjectResponse<>(StudyDetailsDTO.fromModel(model)).getResponse();
        } else {
            throw new NotFoundURIException(studyDbId);
        }

    }

    @GET
    @Path("studies/{studyDbId}/observations")
    @ApiOperation(value = "Get the observations associated to a specific study", notes = "Get the observations associated to a specific study")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = ObservationDTO.class, responseContainer = "List")})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObservations(
            @ApiParam(value = "studyDbId", required = true) @PathParam("studyDbId") @NotNull URI studyDbId,
            @ApiParam(value = "observationVariableDbIds") @QueryParam(value = "observationVariableDbIds") List<URI> observationVariableDbIds,
            @ApiParam(value = "pageSize") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize,
            @ApiParam(value = "page") @QueryParam("page") @DefaultValue("0") @Min(0) int page
    ) throws SQLException, URISyntaxException, Exception {

        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(studyDbId, currentUser);
        List<URI> experiments = new ArrayList<>();
        experiments.add(studyDbId);

        DataDAO dataDAO = new DataDAO(nosql, sparql, fs);
        ListWithPagination<DataModel> datas = dataDAO.search(currentUser, experiments, null, observationVariableDbIds, null, null, null, null, null, null, null, null, page, pageSize);
        ListWithPagination<ObservationDTO> observations = datas.convert(ObservationDTO.class, ObservationDTO::fromModel);
        return new PaginatedListResponse<>(observations).getResponse();

    }

    @GET
    @Path("studies/{studyDbId}/observationvariables")
    @ApiOperation(value = "List all the observation variables measured in the study.", notes = "List all the observation variables measured in the study.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = ObservationVariableDTO.class, responseContainer = "List")})
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
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    @GET
    @Path("studies/{studyDbId}/observationunits")
    @ApiOperation(value = "List all the observation units measured in the study.", notes = "List all the observation units measured in the study.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = ObservationUnitDTO.class, responseContainer = "List")})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObservationUnits(
            @ApiParam(value = "studyDbId", required = true) @PathParam("studyDbId") @NotNull URI studyDbId,
            @ApiParam(value = "observationLevel", example = "Plot") @QueryParam("observationLevel") String observationLevel,
            @ApiParam(value = "pageSize") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int limit,
            @ApiParam(value = "page") @QueryParam("page") @DefaultValue("0") @Min(0) int page
    ) throws Exception {

        List<URI> rdfTypes = new ArrayList<>();
        if (observationLevel != null) {
            URI rdfType = new URI(Oeso.DOMAIN + "#" + observationLevel.substring(0, 1).toUpperCase() + observationLevel.substring(1));
            rdfTypes.add(rdfType);
        }

        ScientificObjectDAO soDAO = new ScientificObjectDAO(sparql, nosql);
        ListWithPagination<ScientificObjectNodeDTO> scientificObjects = soDAO.search(
                studyDbId,
                null,
                rdfTypes,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                page,
                limit,
                null,
                currentUser
        );

        Collection<URI> nodeUris = scientificObjects.getList().stream().map(ScientificObjectNodeDTO::getUri).collect(Collectors.toList());
        Map<String, List<FactorLevelModel>> soUriFactorLevelMap = soDAO.getScientificObjectsFactors(studyDbId, nodeUris, currentUser.getLanguage());
        
        ListWithPagination<ObservationUnitDTO> observations = scientificObjects.convert(ObservationUnitDTO.class, (item) -> {
            String expandedUri = SPARQLDeserializers.getExpandedURI(item.getUri());
            List<FactorLevelModel> factors = soUriFactorLevelMap.get(expandedUri);
            return ObservationUnitDTO.fromModel(item, factors);
        });
        return new PaginatedListResponse<>(observations).getResponse();
    }
}
