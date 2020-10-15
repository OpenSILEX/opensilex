//******************************************************************************
//                          ProvenanceAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import javax.inject.Inject;
import javax.naming.NamingException;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.core.data.api.DataAPI;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.nosql.datanucleus.DataNucleusService;
import org.opensilex.nosql.exceptions.NoSQLBadPersistenceManagerException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

/**
 * Provenance API
 *
 * @author Alice Boizet
 */
@Api(DataAPI.CREDENTIAL_DATA_GROUP_ID)
@Path("/core/provenance")
@ApiCredentialGroup(
        groupId = DataAPI.CREDENTIAL_DATA_GROUP_ID,
        groupLabelKey = DataAPI.CREDENTIAL_DATA_GROUP_LABEL_KEY
)
public class ProvenanceAPI {

    public static final String PROVENANCE_EXAMPLE_URI = "http://opensilex.dev/id/provenance/provenancelabel";

    @CurrentUser
    UserModel currentUser;

    @Inject
    private DataNucleusService nosql;

    @Inject
    private SPARQLService sparql;

    @POST
    @Path("create")
    @ApiOperation("Create a provenance")
    @ApiProtected
    @ApiCredential(
            credentialId = DataAPI.CREDENTIAL_DATA_MODIFICATION_ID,
            credentialLabelKey = DataAPI.CREDENTIAL_DATA_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Create a provenance", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response createProvenance(
            @ApiParam("provenance description") @Valid ProvenanceCreationDTO provDTO
    ) throws Exception {

        ProvenanceDAO provDAO = new ProvenanceDAO(nosql);
        ProvenanceModel model = provDTO.newModel();
        ProvenanceModel provenance = provDAO.create(model);

        return new ObjectUriResponse(Response.Status.CREATED, provenance.getUri()).getResponse();
    }

    /**
     * @param uri
     * @return
     * @throws java.lang.Exception
     */
    @GET
    @Path("get/{uri}")
    @ApiOperation("Get a provenance")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Provenance retrieved", response = ProvenanceGetDTO.class),
        @ApiResponse(code = 404, message = "Provenance not found", response = ErrorDTO.class)
    })
    public Response getProvenance(
            @ApiParam(value = "Provenance URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {

        ProvenanceDAO dao = new ProvenanceDAO(nosql);
        ProvenanceModel provenance = dao.get(uri);

        if (provenance != null) {
            return new SingleObjectResponse<>(ProvenanceGetDTO.fromModel(provenance)).getResponse();
        } else {
            return new ErrorResponse(Response.Status.NOT_FOUND, "Provenance not found",
                    "Unknown provenance URI: " + uri.toString()).getResponse();
        }

    }

    /**
     * @param label
     * @param experiment
     * @param activityType
     * @param agentURI
     * @param agentType
     * @return
     * @throws java.lang.Exception
     */
    @GET
    @Path("search")
    @ApiOperation("Get lists of provenances")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return provenance", response = ProvenanceGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Germplasm not found", response = ErrorDTO.class)
    })
    public Response getProvenance(
            @ApiParam(value = "label") @QueryParam("label") String label,
            @ApiParam(value = "experiment URI") @QueryParam("experiment URI") URI experiment,
            @ApiParam(value = "activity type") @QueryParam("activity type") URI activityType,
            @ApiParam(value = "agent URI") @QueryParam("agent URI") URI agentURI,
            @ApiParam(value = "agent type") @QueryParam("agent type") URI agentType,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        ProvenanceDAO dao = new ProvenanceDAO(nosql);
        ListWithPagination<ProvenanceModel> resultList = dao.search(label, experiment, activityType, agentType, agentURI, page, pageSize);
        
// Convert paginated list to DTO
        ListWithPagination<ProvenanceGetDTO> provenances = resultList.convert(
                ProvenanceGetDTO.class,
                ProvenanceGetDTO::fromModel
        );
        return new PaginatedListResponse<>(provenances).getResponse();
    }

    @DELETE
    @Path("delete/{uri}")
    @ApiOperation("Delete a provenance")
    @ApiProtected
    @ApiCredential(
            credentialId = DataAPI.CREDENTIAL_DATA_DELETE_ID,
            credentialLabelKey = DataAPI.CREDENTIAL_DATA_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Provenance deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Invalid or unknown provenance URI", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response deleteProvenance(
            @ApiParam(value = "Provenance URI", example = PROVENANCE_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI uri) throws URISyntaxException, NamingException, IOException, ParseException, Exception {
        ProvenanceDAO dao = new ProvenanceDAO(nosql);

        //check if the provenance can be deleted (not linked to data)
        DataDAO dataDAO = new DataDAO(nosql, sparql, null);
        ListWithPagination<DataModel> resultList = dataDAO.search(
                currentUser,
                null,
                null,
                uri,
                null,
                null,
                true,
                0,
                1
        );

        if (resultList.getTotal() > 0) {
            return new ErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "The provenance is linked to some data",
                    "You can't delete a provenance linked to data"
            ).getResponse();
        } else {
            try {
                dao.delete(uri);
                return new ObjectUriResponse(uri).getResponse();

            } catch (NoSQLInvalidURIException e) {
                return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Provenance URI", e.getMessage())
                        .getResponse();
            } catch (NamingException | NoSQLBadPersistenceManagerException e) {
                return new ErrorResponse(e).getResponse();
            }
        }
    }

    @PUT
    @Path("update")
    @ApiProtected
    @ApiOperation("Update a provenance")
    @ApiCredential(
            credentialId = DataAPI.CREDENTIAL_DATA_MODIFICATION_ID,
            credentialLabelKey = DataAPI.CREDENTIAL_DATA_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Provenance updated", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response update(
            @ApiParam("Provenance description") @Valid ProvenanceUpdateDTO dto
    ) throws Exception {
        try {
            ProvenanceDAO dao = new ProvenanceDAO(nosql);
            ProvenanceModel newProvenance = dto.newModel();

            ProvenanceModel storedProvenance = dao.get(dto.getUri());

            if (storedProvenance == null) {
                throw new NoSQLInvalidURIException(dto.getUri());
            } else {
                if (!newProvenance.getLabel().equals(storedProvenance.getLabel())) {
                    throw new BadRequestException("The label can't be updated");
                } else {
                    newProvenance = dao.update(newProvenance);
                }
            }
            return new ObjectUriResponse(Response.Status.OK, newProvenance.getUri()).getResponse();
        } catch (NoSQLInvalidURIException | BadRequestException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "wrong provenance json", e.getMessage()).getResponse();
        } catch (IOException | ParseException | NamingException | NoSQLBadPersistenceManagerException e) {
            return new ErrorResponse(e).getResponse();
        }
    }
}
