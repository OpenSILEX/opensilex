//******************************************************************************
//                          StudiesAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.api;

import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.opensilex.brapi.responses.*;
import org.opensilex.brapi.model.*;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.dal.ExperimentSearchFilter;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectSearchFilter;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation 1.3</a>
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.2">BrAPI documentation 1.2</a>
 * @author Alice Boizet
 */
@Api("BRAPI")
@Path("/brapi/")
public class StudiesAPI extends BrapiCall {

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;

    @CurrentUser
    AccountModel currentUser;

    protected Response standardGetStudies(URI studyDbId, String active, String sortBy, String sortOrder, int page, int pageSize) throws Exception {

        ExperimentDAO xpDao = new ExperimentDAO(sparql, nosql);

        ArrayList<OrderBy> orderByList = new ArrayList<>();

        if (!StringUtils.isEmpty(sortBy)) {
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

        if (studyDbId != null && xpDao.get(studyDbId, currentUser) == null) {
            throw new NotFoundURIException(studyDbId);
        } else {
            ExperimentSearchFilter filter = new ExperimentSearchFilter()
                    .setEnded(isEnded)
                    .setUser(currentUser);
            filter.setOrderByList(orderByList)
                    .setPage(page)
                    .setPageSize(pageSize);

            ListWithPagination<ExperimentModel> resultList = xpDao.search(filter);
            ListWithPagination<BrAPIv1StudyDTO> resultDTOList = resultList.convert(BrAPIv1StudyDTO.class, BrAPIv1StudyDTO::fromModel);
            return new BrAPIv1StudyListResponse(resultDTOList).getResponse();
        }
    }


    @GET
    @Path("v1/studies")
    @BrapiVersion("1.3")
    @ApiOperation(value = "Retrieve studies information", notes = "Retrieve studies information")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve studies information", response = BrAPIv1StudyListResponse.class)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)

    public Response getStudies(
            @ApiParam(value = "Search by studyDbId") @QueryParam("studyDbId") URI studyDbId,
            @ApiParam(value = "Filter active status true/false") @QueryParam("active") String active,
            @ApiParam(value = "Name of the field to sort by: studyDbId, active") @QueryParam("sortBy") String sortBy,
            @ApiParam(value = "Sort order direction - ASC or DESC") @QueryParam("sortOrder") String sortOrder,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        return this.standardGetStudies(studyDbId, active, sortBy, sortOrder, page, pageSize);
    }

    @GET
    @Path("v1/studies-search")
    @BrapiVersion("1.2")
    @ApiOperation(value = "Retrieve studies information", notes = "Retrieve studies information")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieve studies information", response = BrAPIv1StudyListResponse.class)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)

    public Response getStudiesSearch(
            @ApiParam(value = "Search by studyDbId") @QueryParam("studyDbId") URI studyDbId,
            @ApiParam(value = "Filter active status true/false") @QueryParam("active") String active,
            @ApiParam(value = "Name of the field to sort by: studyDbId or seasonDbId") @QueryParam("sortBy") String sortBy,
            @ApiParam(value = "Sort order direction - ASC or DESC") @QueryParam("sortOrder") String sortOrder,
            @ApiParam(value = "pageSize") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize,
            @ApiParam(value = "page") @QueryParam("page") @DefaultValue("0") @Min(0) int page
    ) throws Exception {
        return this.standardGetStudies(studyDbId, active, sortBy, sortOrder, page, pageSize);
    }

    private void validateExperimentRightsAndURI(URI expeURI, ExperimentDAO xpDao) throws ForbiddenURIAccessException {
        try {
            xpDao.validateExperimentAccess(expeURI, currentUser);
        } catch (MalformedQueryException e) {
            throw new BadRequestException(expeURI.toString() + " is not a valid experiment URI");
        } catch (ForbiddenURIAccessException e) {
            throw new ForbiddenURIAccessException(expeURI, "You don't have the rights to access this experiment : " + expeURI.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("v1/studies/{studyDbId}")
    @BrapiVersion("1.3")
    @ApiOperation(value = "Retrieve study details", notes = "Retrieve study details")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve study details", response = BrAPIv1SingleStudyResponse.class)})  // TODO : wrong return type
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStudyDetails(
            @ApiParam(value = "Search by studyDbId", required = true) @PathParam("studyDbId") @NotNull URI studyDbId
    ) throws Exception {
        ExperimentDAO xpDao = new ExperimentDAO(sparql, nosql);
        validateExperimentRightsAndURI(studyDbId, xpDao);

        OrganizationDAO organisationDAO = new OrganizationDAO(sparql, nosql);
        FacilityDAO facilityDAO = new FacilityDAO(sparql, nosql, organisationDAO);
        ExperimentModel model = xpDao.get(studyDbId, currentUser);

        if (model != null) {
            return new BrAPIv1SingleStudyResponse(BrAPIv1StudyDetailsDTO.fromModel(model, facilityDAO, organisationDAO, currentUser)).getResponse();
        } else {
            throw new NotFoundURIException(studyDbId);
        }

    }

    @GET
    @Path("v1/studies/{studyDbId}/observations")
    @BrapiVersion("1.3")
    @ApiOperation(value = "Get the observations associated to a specific study", notes = "Get the observations associated to a specific study")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = BrAPIv1ObservationListResponse.class)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObservations(
            @ApiParam(value = "studyDbId", required = true) @PathParam("studyDbId") @NotNull URI studyDbId,
            @ApiParam(value = "observationVariableDbIds") @QueryParam(value = "observationVariableDbIds") List<URI> observationVariableDbIds,
            @ApiParam(value = "pageSize") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize,
            @ApiParam(value = "page") @QueryParam("page") @DefaultValue("0") @Min(0) int page
    ) throws Exception {
        ExperimentDAO xpDao = new ExperimentDAO(sparql, nosql);
        validateExperimentRightsAndURI(studyDbId, xpDao);

        ExperimentModel experimentModel = xpDao.get(studyDbId, currentUser);
        List<URI> experiments = new ArrayList<>();
        experiments.add(studyDbId);

        DataDAO dataDAO = new DataDAO(nosql, sparql, fs);
        ScientificObjectDAO scientificObjectDAO = new ScientificObjectDAO(sparql, nosql);
        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        ListWithPagination<DataModel> datas = dataDAO.search(currentUser, experiments, null, observationVariableDbIds, null, null, null, null, null, null, null, null, null, page, pageSize);
        ListWithPagination<BrAPIv1ObservationDTO> observations = datas.convert(BrAPIv1ObservationDTO.class, data -> {
            try {
                return BrAPIv1ObservationDTO.fromModel(data, experimentModel, ontologyDAO, sparql, currentUser, scientificObjectDAO, germplasmDAO);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return new BrAPIv1ObservationListResponse(observations).getResponse();

    }

    @GET
    @Path("v1/studies/{studyDbId}/observationvariables")
    @BrapiVersion("1.3")
    @ApiOperation(value = "List all the observation variables measured in the study.", notes = "List all the observation variables measured in the study.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = BrAPIv1ObservationVariableListResponse.class)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObservationVariables(
            @ApiParam(value = "studyDbId", required = true) @PathParam("studyDbId") @NotNull URI studyDbId,
            @ApiParam(value = "pageSize") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize,
            @ApiParam(value = "page") @QueryParam("page") @DefaultValue("0") @Min(0) int page
    ) throws Exception {
        ExperimentDAO xpDao = new ExperimentDAO(sparql, nosql);
        validateExperimentRightsAndURI(studyDbId, xpDao);

        DataDAO dataDAO = new DataDAO(nosql, sparql, fs);
        List<VariableModel> variables = dataDAO.getUsedVariables(currentUser, Collections.singletonList(studyDbId), null, null, null);

        ListWithPagination<VariableModel> variablesPaginated = new ListWithPagination<>(variables, page, pageSize, variables.size());
        ListWithPagination<BrAPIv1ObservationVariableDTO> resultDTOList = variablesPaginated.convert(BrAPIv1ObservationVariableDTO.class, BrAPIv1ObservationVariableDTO::fromModel);
        return new BrAPIv1ObservationVariableListResponse(resultDTOList).getResponse();
    }

    @GET
    @Path("v1/studies/{studyDbId}/observationunits")
    @BrapiVersion("1.3")
    @ApiOperation(value = "List all the observation units measured in the study.", notes = "List all the observation units measured in the study.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = BrAPIv1ObservationUnitListResponse.class)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObservationUnits(
            @ApiParam(value = "studyDbId", required = true) @PathParam("studyDbId") @NotNull URI studyDbId,
            @ApiParam(value = "observationLevel", example = "Plot") @QueryParam("observationLevel") String observationLevel,
            @ApiParam(value = "pageSize") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int limit,
            @ApiParam(value = "page") @QueryParam("page") @DefaultValue("0") @Min(0) int page
    ) throws Exception {
        ExperimentDAO xpDao = new ExperimentDAO(sparql, nosql);
        validateExperimentRightsAndURI(studyDbId, xpDao);

        List<URI> rdfTypes = new ArrayList<>();
        if (observationLevel != null) {
            URI rdfType = new URI(Oeso.DOMAIN + "#" + observationLevel.substring(0, 1).toUpperCase() + observationLevel.substring(1));
            rdfTypes.add(rdfType);
        }

        ScientificObjectDAO soDAO = new ScientificObjectDAO(sparql, nosql);
        OrganizationDAO organizationDAO = new OrganizationDAO(sparql, nosql);
        FacilityDAO facilityDAO = new FacilityDAO(sparql, nosql, organizationDAO);
        DataDAO dataDAO = new DataDAO(nosql, sparql, fs);
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        MoveEventDAO moveEventDAO = new MoveEventDAO(sparql, nosql);
        GeospatialDAO geospatialDAO = new GeospatialDAO(nosql);

        ScientificObjectSearchFilter searchFilter = new ScientificObjectSearchFilter()
                .setExperiment(studyDbId)
                .setRdfTypes(rdfTypes);

        searchFilter.setPage(page)
                .setPageSize(limit)
                .setLang(currentUser.getLanguage());

        ListWithPagination<ScientificObjectModel> scientificObjects = soDAO.search(searchFilter, Collections.singletonList(ScientificObjectModel.FACTOR_LEVEL_FIELD));
        
        ListWithPagination<BrAPIv1ObservationUnitDTO> observations = scientificObjects.convert(BrAPIv1ObservationUnitDTO.class, (scientificObjectModel) -> {
            try {
                return BrAPIv1ObservationUnitDTO.fromModel(
                        scientificObjectModel,
                        facilityDAO,
                        currentUser,
                        dataDAO,
                        xpDao.get(studyDbId, currentUser),
                        ontologyDAO,
                        sparql,
                        moveEventDAO,
                        geospatialDAO);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return new BrAPIv1ObservationUnitListResponse(observations).getResponse();
    }
}
