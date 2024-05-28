/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api.facility;

import io.swagger.annotations.*;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.facility.FacilitySearchFilter;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.response.CreatedUriResponse;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.opensilex.core.organisation.api.OrganizationAPI.CREDENTIAL_GROUP_ORGANIZATION_ID;

/**
 *
 * @author vidalmor
 */
@Api(CREDENTIAL_GROUP_ORGANIZATION_ID)
@Path(FacilityAPI.PATH)
@ApiCredentialGroup(
        groupId = FacilityAPI.CREDENTIAL_GROUP_FACILITY_ID,
        groupLabelKey = FacilityAPI.CREDENTIAL_GROUP_FACILITY_LABEL_KEY
)
public class FacilityAPI {

    public static final String PATH = "/core/facilities";
    public static final String CREDENTIAL_GROUP_FACILITY_ID = "Facilities";
    public static final String CREDENTIAL_GROUP_FACILITY_LABEL_KEY = "credential-groups.facilities";

    public static final String CREDENTIAL_FACILITY_MODIFICATION_ID = "facility-modification";
    public static final String CREDENTIAL_FACILITY_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_FACILITY_DELETE_ID = "facility-delete";
    public static final String CREDENTIAL_FACILITY_DELETE_LABEL_KEY = "credential.default.delete";

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @CurrentUser
    AccountModel currentUser;

    @POST
    @ApiOperation("Create a facility")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACILITY_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_FACILITY_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Create a facility", response = URI.class),
        @ApiResponse(code = 409, message = "A facility with the same URI already exists", response = ErrorResponse.class)
    })
    public Response createFacility(
            @ApiParam("Facility description") @Valid FacilityCreationDTO dto
    ) throws Exception {
        try {
            OrganizationDAO organizationDAO = new OrganizationDAO(sparql, nosql);
            FacilityDAO facilityDAO = new FacilityDAO(sparql, nosql, organizationDAO);
            FacilityModel facility = dto.newModel();
            facility.setPublisher(currentUser.getUri());

            if (dto.getRelations() != null) {
                OntologyDAO ontoDAO = new OntologyDAO(sparql);
                ClassModel model = SPARQLModule.getOntologyStoreInstance().getClassModel(facility.getType(),
                        new URI(Oeso.Facility.getURI()), currentUser.getLanguage());
                URI graph = sparql.getDefaultGraphURI(FacilityModel.class);
                for (RDFObjectRelationDTO relation : dto.getRelations()) {
                    if (!ontoDAO.validateObjectValue(graph, model, relation.getProperty(), relation.getValue(), facility)) {
                        throw new InvalidValueException("Invalid relation value for " + relation.getProperty().toString() + " => " + relation.getValue());
                    }
                }
            }

            facility = facilityDAO.create(
                    facility,
                    Objects.isNull(dto.getGeometry()) ? null : GeospatialDAO.geoJsonToGeometry(dto.getGeometry()),
                    currentUser
            );

            return new CreatedUriResponse(facility.getUri()).getResponse();

        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(Response.Status.CONFLICT, "Facility already exists", e.getMessage()).getResponse();
        }
    }

    @GET
    @Path("all_facilities")
    @ApiOperation("Get all facilities")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Facility retrieved", response = NamedResourceDTO.class, responseContainer = "List"),
        @ApiResponse(code = 404, message = "Facility URI not found", response = ErrorResponse.class)
    })
    public Response getAllFacilities() throws Exception {
        OrganizationDAO organizationDAO = new OrganizationDAO(sparql, nosql);
        FacilityDAO facilityDAO = new FacilityDAO(sparql, nosql, organizationDAO);
        FacilitySearchFilter searchFilter = new FacilitySearchFilter();
        searchFilter.setPageSize(0);
        List<FacilityModel> facilities = facilityDAO.search(searchFilter
                        .setUser(currentUser))
                .getList();

        List<NamedResourceDTO> dtoList = facilities.stream().map(NamedResourceDTO::getDTOFromModel).collect(Collectors.toList());

        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get a facility")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Facility retrieved", response = FacilityGetDTO.class),
        @ApiResponse(code = 404, message = "Facility URI not found", response = ErrorResponse.class)
    })
    public Response getFacility(
            @ApiParam(value = "facility URI", example = "http://opensilex.dev/organisations/facility/phenoarch", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        OrganizationDAO organizationDAO = new OrganizationDAO(sparql, nosql);
        FacilityDAO facilityDAO = new FacilityDAO(sparql, nosql, organizationDAO);
        FacilityModel model = facilityDAO.get(uri, currentUser);
        FacilityGetDTO facilityGetDTO = FacilityGetDTO.getDTOFromModel(
                model,
                true
        );
        if (Objects.nonNull(model.getPublisher())){
            facilityGetDTO.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(model.getPublisher())));
        }
        facilityGetDTO.fromGeospatialModel(facilityDAO.getFacilityGeospatialModel(uri));
        return new SingleObjectResponse<>(facilityGetDTO).getResponse();
    }

    @GET
    @Path("by_uris")
    @ApiOperation("Get facilities by their URIs")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return facilities", response = FacilityNamedDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Facility not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    public Response getFacilitiesByURI(
            @ApiParam(value = "Facilities URIs", required = true) @QueryParam("uris") @NotNull @NotEmpty @ValidURI List<URI> uris) throws Exception {

        OrganizationDAO organizationDAO = new OrganizationDAO(sparql, nosql);
        FacilityDAO facilityDAO = new FacilityDAO(sparql, nosql, organizationDAO);

        List<FacilityModel> facilities = facilityDAO.getList(uris, currentUser);

        if (facilities.isEmpty()) {
            return new ErrorResponse(Response.Status.NOT_FOUND, "Facilities not found", "Unknown facilities URIs").getResponse();
        }

        List<FacilityNamedDTO> dtoList = facilities.stream()
                .map(FacilityNamedDTO::new)
                .collect(Collectors.toList());

        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @ApiOperation("Search facilities")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return facilities", response = FacilityNamedDTO.class, responseContainer = "List")
    })
    public Response searchFacilities(
            @ApiParam(value = "Regex pattern for filtering facilities by names", example = ".*") @DefaultValue(".*") @QueryParam("pattern") String pattern,
            @ApiParam(value = "List of organizations hosted by the facilities to filter") @QueryParam("organizations") List<URI> organizations,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number") @QueryParam("page") int page,
            @ApiParam(value = "Page size") @QueryParam("page_size") int pageSize
    ) throws Exception {
        OrganizationDAO organizationDAO = new OrganizationDAO(sparql, nosql);
        FacilityDAO facilityDAO = new FacilityDAO(sparql, nosql, organizationDAO);

        FacilitySearchFilter filter = (FacilitySearchFilter) new FacilitySearchFilter()
                .setUser(currentUser)
                .setPattern(pattern)
                .setOrderByList(orderByList)
                .setPage(page)
                .setPageSize(pageSize);
        if (!organizations.isEmpty()) {
            filter.setOrganizations(organizations);
        }
        ListWithPagination<FacilityModel> facilities = facilityDAO.search(filter);

        List<FacilityGetDTO> dtoList = facilities.getList().stream()
                .map((facilityModel) -> FacilityGetDTO.getDTOFromModel(facilityModel, true))
                .collect(Collectors.toList());

        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a facility")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACILITY_DELETE_ID,
            credentialLabelKey = CREDENTIAL_FACILITY_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Facility deleted", response = URI.class),
        @ApiResponse(code = 404, message = "Facility URI not found", response = ErrorResponse.class)
    })
    public Response deleteFacility(
            @ApiParam(value = "Facility URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        OrganizationDAO organizationDAO = new OrganizationDAO(sparql, nosql);
        FacilityDAO facilityDAO = new FacilityDAO(sparql, nosql, organizationDAO);

        facilityDAO.delete(uri, currentUser);

        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @PUT
    @ApiOperation("Update a facility")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACILITY_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_FACILITY_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return updated facility", response = URI.class),
        @ApiResponse(code = 404, message = "Facility URI not found", response = ErrorResponse.class)
    })
    public Response updateFacility(
            @ApiParam("Facility description")
            @Valid FacilityUpdateDTO dto
    ) throws Exception {
        OrganizationDAO organizationDAO = new OrganizationDAO(sparql, nosql);
        FacilityDAO facilityDAO = new FacilityDAO(sparql, nosql, organizationDAO);

        FacilityModel facility = dto.newModel();

        if (dto.getRelations() != null) {
            OntologyDAO ontoDAO = new OntologyDAO(sparql);
            ClassModel model = ontoDAO.getClassModel(facility.getType(), new URI(Oeso.Facility.getURI()), currentUser.getLanguage());
            URI graph = sparql.getDefaultGraphURI(FacilityModel.class);
            for (RDFObjectRelationDTO relation : dto.getRelations()) {
                if (!ontoDAO.validateObjectValue(graph, model, relation.getProperty(), relation.getValue(), facility)) {
                    throw new InvalidValueException("Invalid relation value for " + relation.getProperty().toString() + " => " + relation.getValue());
                }
            }
        }

        facility = facilityDAO.update(
                facility,
                Objects.isNull(dto.getGeometry()) ? null : GeospatialDAO.geoJsonToGeometry(dto.getGeometry()),
                currentUser
        );

        Response response = new ObjectUriResponse(Response.Status.OK, facility.getUri()).getResponse();

        return response;
    }

}
