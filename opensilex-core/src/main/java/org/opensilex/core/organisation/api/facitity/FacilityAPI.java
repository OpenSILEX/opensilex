/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api.facitity;

import io.swagger.annotations.*;
import org.opensilex.core.CoreModule;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.core.organisation.api.InfrastructureAPI;
import org.opensilex.core.organisation.dal.InfrastructureDAO;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
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
import java.util.stream.Collectors;

import static org.opensilex.core.organisation.api.InfrastructureAPI.CREDENTIAL_GROUP_INFRASTRUCTURE_ID;

/**
 *
 * @author vidalmor
 */
@Api(CREDENTIAL_GROUP_INFRASTRUCTURE_ID)
@Path("core/facilities")
@ApiCredentialGroup(
        groupId = FacilityAPI.CREDENTIAL_GROUP_FACILITY_ID,
        groupLabelKey = FacilityAPI.CREDENTIAL_GROUP_FACILITY_LABEL_KEY
)
public class FacilityAPI {

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
    UserModel currentUser;

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
        @ApiResponse(code = 201, message = "Create a facility", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "A facility with the same URI already exists", response = ErrorResponse.class)
    })
    public Response createInfrastructureFacility(
            @ApiParam("Facility description") @Valid InfrastructureFacilityCreationDTO dto
    ) throws Exception {
        try {
            InfrastructureDAO dao = new InfrastructureDAO(sparql, nosql);
            InfrastructureFacilityModel facility = dto.newModel();

            if (dto.getRelations() != null) {
                OntologyDAO ontoDAO = new OntologyDAO(sparql);
                ClassModel model = SPARQLModule.getOntologyStoreInstance().getClassModel(facility.getType(),
                        new URI(Oeso.Facility.getURI()), currentUser.getLanguage());
                URI graph = sparql.getDefaultGraphURI(InfrastructureFacilityModel.class);
                for (RDFObjectRelationDTO relation : dto.getRelations()) {
                    if (!ontoDAO.validateObjectValue(graph, model, relation.getProperty(), relation.getValue(), facility)) {
                        throw new InvalidValueException("Invalid relation value for " + relation.getProperty().toString() + " => " + relation.getValue());
                    }
                }
            }

            facility = dao.createFacility(facility, currentUser);

            return new ObjectUriResponse(Response.Status.CREATED, facility.getUri()).getResponse();

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
        InfrastructureDAO dao = new InfrastructureDAO(sparql, nosql);
        List<InfrastructureFacilityModel> facilities = dao.getAllFacilities(currentUser);

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
        @ApiResponse(code = 200, message = "Facility retrieved", response = InfrastructureFacilityGetDTO.class),
        @ApiResponse(code = 404, message = "Facility URI not found", response = ErrorResponse.class)
    })
    public Response getInfrastructureFacility(
            @ApiParam(value = "facility URI", example = "http://opensilex.dev/organisations/facility/phenoarch", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql, nosql);

        InfrastructureFacilityGetDTO facilityGetDTO = InfrastructureFacilityGetDTO.getDTOFromModel(
                dao.getFacility(uri, currentUser),
                true
        );
        facilityGetDTO.fromGeospatialModel(dao.getFacilityGeospatialModel(uri));
        return new SingleObjectResponse<>(facilityGetDTO).getResponse();
    }

    @GET
    @Path("by_uris")
    @ApiOperation("Get facilities by their URIs")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return facilities", response = InfrastructureFacilityNamedDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Facility not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    public Response getFacilitiesByURI(
            @ApiParam(value = "Facilities URIs", required = true) @QueryParam("uris") @NotNull @NotEmpty @ValidURI List<URI> uris) throws Exception {

        InfrastructureDAO dao = new InfrastructureDAO(sparql, nosql);
        List<InfrastructureFacilityModel> facilities = dao.getFacilitiesByURI(currentUser, uris);

        if (facilities.isEmpty()) {
            return new ErrorResponse(Response.Status.NOT_FOUND, "Facilities not found", "Unknown facilities URIs").getResponse();
        }

        List<InfrastructureFacilityNamedDTO> dtoList = facilities.stream()
                .map(InfrastructureFacilityNamedDTO::new)
                .collect(Collectors.toList());

        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @ApiOperation("Search facilities")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return facilities", response = InfrastructureFacilityNamedDTO.class, responseContainer = "List")
    })
    public Response searchInfrastructureFacilities(
            @ApiParam(value = "Regex pattern for filtering facilities by names", example = ".*") @DefaultValue(".*") @QueryParam("pattern") String pattern,
            @ApiParam(value = "List of organizations hosted by the facilities to filter") @QueryParam("organizations") List<URI> organizations,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number") @QueryParam("page") int page,
            @ApiParam(value = "Page size") @QueryParam("page_size") int pageSize
    ) throws Exception {

        InfrastructureDAO dao = new InfrastructureDAO(sparql, nosql);
        ListWithPagination<InfrastructureFacilityModel> facilities = dao.searchFacilities(currentUser, pattern, organizations, orderByList, page, pageSize);

        List<InfrastructureFacilityGetDTO> dtoList = facilities.getList().stream()
                .map((facilityModel) -> InfrastructureFacilityGetDTO.getDTOFromModel(facilityModel, true))
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
        @ApiResponse(code = 200, message = "Facility deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Facility URI not found", response = ErrorResponse.class)
    })
    public Response deleteInfrastructureFacility(
            @ApiParam(value = "Facility URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql, nosql);
        dao.deleteFacility(uri, currentUser);
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
        @ApiResponse(code = 200, message = "Return updated facility", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Facility URI not found", response = ErrorResponse.class)
    })
    public Response updateInfrastructureFacility(
            @ApiParam("Facility description")
            @Valid InfrastructureFacilityUpdateDTO dto
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql, nosql);

        InfrastructureFacilityModel facility = dto.newModel();

        if (dto.getRelations() != null) {
            OntologyDAO ontoDAO = new OntologyDAO(sparql);
            ClassModel model = ontoDAO.getClassModel(facility.getType(), new URI(Oeso.Facility.getURI()), currentUser.getLanguage());
            URI graph = sparql.getDefaultGraphURI(InfrastructureFacilityModel.class);
            for (RDFObjectRelationDTO relation : dto.getRelations()) {
                if (!ontoDAO.validateObjectValue(graph, model, relation.getProperty(), relation.getValue(), facility)) {
                    throw new InvalidValueException("Invalid relation value for " + relation.getProperty().toString() + " => " + relation.getValue());
                }
            }
        }

        facility = dao.updateFacility(facility, currentUser);

        Response response = new ObjectUriResponse(Response.Status.OK, facility.getUri()).getResponse();

        return response;
    }

}
