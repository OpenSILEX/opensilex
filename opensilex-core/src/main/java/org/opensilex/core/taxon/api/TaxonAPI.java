//******************************************************************************
//                          OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026.
//******************************************************************************
package org.opensilex.core.taxon.api;

import io.swagger.annotations.*;
import org.opensilex.core.taxon.bll.TaxonLogic;
import org.opensilex.core.taxon.dal.TaxonModel;
import org.opensilex.core.taxon.dal.TaxonSearchFilter;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.response.CreatedUriResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Taxa (species, genera): the taxonomic reference of OpenSILEX.
 */
@Api(TaxonAPI.CREDENTIAL_TAXON_GROUP_ID)
@Path(TaxonAPI.PATH)
@ApiCredentialGroup(
        groupId = TaxonAPI.CREDENTIAL_TAXON_GROUP_ID,
        groupLabelKey = TaxonAPI.CREDENTIAL_TAXON_GROUP_LABEL_KEY
)
public class TaxonAPI {

    public static final String PATH = "/core/taxa";

    public static final String CREDENTIAL_TAXON_GROUP_ID = "Taxon";
    public static final String CREDENTIAL_TAXON_GROUP_LABEL_KEY = "credential-groups.taxon";
    public static final String CREDENTIAL_TAXON_MODIFICATION_ID = "taxon-modification";
    public static final String CREDENTIAL_TAXON_MODIFICATION_LABEL_KEY = "credential.default.modification";
    public static final String CREDENTIAL_TAXON_DELETE_ID = "taxon-delete";
    public static final String CREDENTIAL_TAXON_DELETE_LABEL_KEY = "credential.default.delete";

    private static final String EXAMPLE_SPECIES = "http://www.opensilex.org/vocabulary/oeso#Species";
    private static final String EXAMPLE_KINGDOM = "http://www.opensilex.org/vocabulary/oeso#Plantae";

    @Inject
    private SPARQLService sparql;

    @CurrentUser
    AccountModel currentUser;

    private TaxonLogic logic() {
        return new TaxonLogic(sparql, currentUser);
    }

    @GET
    @ApiOperation("Search taxa")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return taxa", response = TaxonGetDTO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Bad user request", response = ErrorDTO.class)
    })
    public Response searchTaxa(
            @ApiParam(value = "Regex pattern on the scientific name", example = "vitis") @QueryParam("name") String name,
            @ApiParam(value = "Rank (rdf type)", example = EXAMPLE_SPECIES) @QueryParam("rdf_type") List<URI> types,
            @ApiParam(value = "Kingdom URI", example = EXAMPLE_KINGDOM) @QueryParam("kingdom") URI kingdom,
            @ApiParam(value = "Identifier of the taxon, with its source prefix", example = "NCBITaxon:29760") @QueryParam("taxon_id") String taxonId,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "label=asc") @DefaultValue("label=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        TaxonSearchFilter filter = new TaxonSearchFilter()
                .setName(name)
                .setKingdom(kingdom)
                .setTaxonId(taxonId);
        filter.setRdfTypes(types == null ? Collections.emptyList() : types);
        filter.setOrderByList(orderByList).setPage(page).setPageSize(pageSize);

        ListWithPagination<TaxonModel> result = logic().search(filter);
        return new PaginatedListResponse<>(result.convert(TaxonGetDTO.class, TaxonGetDTO::fromModel)).getResponse();
    }

    @GET
    @Path("kingdoms")
    @ApiOperation("Get the kingdoms (closed list of the ontology)")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return kingdoms", response = KingdomDTO.class, responseContainer = "List")
    })
    public Response getKingdoms() throws Exception {
        List<KingdomDTO> kingdoms = logic().getKingdoms().stream().map(KingdomDTO::fromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(kingdoms).getResponse();
    }



    @GET
    @Path("{uri}")
    @ApiOperation("Get a taxon")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return the taxon", response = TaxonGetDTO.class),
            @ApiResponse(code = 404, message = "Taxon not found", response = ErrorDTO.class)
    })
    public Response getTaxon(
            @ApiParam(value = "Taxon URI", required = true) @PathParam("uri") @ValidURI @NotNull URI uri
    ) throws Exception {
        return new SingleObjectResponse<>(TaxonGetDTO.fromModel(logic().get(uri))).getResponse();
    }

    @POST
    @ApiOperation("Create a taxon")
    @ApiProtected
    @ApiCredential(credentialId = CREDENTIAL_TAXON_MODIFICATION_ID, credentialLabelKey = CREDENTIAL_TAXON_MODIFICATION_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Taxon created", response = ObjectUriResponse.class),
            @ApiResponse(code = 400, message = "Bad user request", response = ErrorDTO.class),
            @ApiResponse(code = 409, message = "Identifier already used", response = ErrorDTO.class)
    })
    public Response createTaxon(@ApiParam("Taxon description") @Valid TaxonCreationDTO dto) throws Exception {
        TaxonModel taxon = logic().create(dto.newModel());
        return new CreatedUriResponse(taxon.getUri()).getResponse();
    }

    @PUT
    @ApiOperation("Update a taxon")
    @ApiProtected
    @ApiCredential(credentialId = CREDENTIAL_TAXON_MODIFICATION_ID, credentialLabelKey = CREDENTIAL_TAXON_MODIFICATION_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Taxon updated", response = ObjectUriResponse.class),
            @ApiResponse(code = 400, message = "Bad user request", response = ErrorDTO.class),
            @ApiResponse(code = 404, message = "Taxon not found", response = ErrorDTO.class),
            @ApiResponse(code = 409, message = "Conflict", response = ErrorDTO.class)
    })
    public Response updateTaxon(@ApiParam("Taxon description") @Valid TaxonCreationDTO dto) throws Exception {
        TaxonModel taxon = logic().update(dto.newModel());
        return new ObjectUriResponse(Response.Status.OK, taxon.getUri()).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a taxon (refused if it is used)")
    @ApiProtected
    @ApiCredential(credentialId = CREDENTIAL_TAXON_DELETE_ID, credentialLabelKey = CREDENTIAL_TAXON_DELETE_LABEL_KEY)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Taxon deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Taxon not found", response = ErrorDTO.class),
            @ApiResponse(code = 409, message = "Taxon used", response = ErrorDTO.class)
    })
    public Response deleteTaxon(
            @ApiParam(value = "Taxon URI", required = true) @PathParam("uri") @ValidURI @NotNull URI uri
    ) throws Exception {
        logic().delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }
}
