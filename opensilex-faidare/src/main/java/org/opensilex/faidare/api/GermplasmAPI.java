//******************************************************************************
//                          GermplasmAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.api;

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
import org.opensilex.core.ontology.Oeso;
import org.opensilex.faidare.builder.Faidarev1GermplasmDTOBuilder;
import org.opensilex.faidare.dal.Faidarev1GermplasmDAO;
import org.opensilex.faidare.model.Faidarev1GermplasmDTO;
import org.opensilex.faidare.model.Faidarev1GermplasmModel;
import org.opensilex.faidare.responses.Faidarev1GermplasmListResponse;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.StatusDTO;
import org.opensilex.server.response.StatusLevel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Objects;

/**
 * The faidare germplasm corresponds to an accession in OpenSILEX
 * @author Gabriel Besombes
 */
@Tag(name = CallsAPI.CREDENTIAL_CALLS_GROUP_ID)
@Path("/faidare/")
@ApiCredentialGroup(
        groupId = CallsAPI.CREDENTIAL_CALLS_GROUP_ID,
        groupLabelKey = CallsAPI.CREDENTIAL_CALLS_GROUP_LABEL_KEY
)
public class GermplasmAPI extends FaidareCall {
    
    @Inject
    private SPARQLService sparql;
    
    @Inject
    private MongoDBService nosql;

    @Inject
    MongoDBServiceV2 nosqlV2;
    
    @CurrentUser
    AccountModel currentUser;
    
    @GET
    @Path("v1/germplasm")
    @FaidareVersion("1.3")
    @Operation(summary = "Submit a search request for germplasm")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = Faidarev1GermplasmListResponse.class))),
        @ApiResponse(responseCode = "400", description = "Bad user request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})

    public Response getGermplasmsBySearch(
            @Parameter(description = "Search by germplasmDbId") @QueryParam("germplasmDbId") URI uri,
            @Parameter(description = "Search by germplasmPUI") @QueryParam("germplasmPUI") URI germplasmPUI,
            @Parameter(description = "Search by germplasmName") @QueryParam("germplasmName") String germplasmName,
            @Parameter(description = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        if (Objects.isNull(ontologyDAO.getRdfType(new URI(Oeso.Accession.getURI()), "en"))){
            Faidarev1GermplasmListResponse response = new Faidarev1GermplasmListResponse(new ListWithPagination<>(new ArrayList<>()));
            response.setStatus(Response.Status.SERVICE_UNAVAILABLE);
            response.addMetadataStatus(
                    new StatusDTO(
                            "The accession notion doesn't exist in your instance so this service is unavailable",
                            StatusLevel.ERROR
                    )
            );
            return response.getResponse();
        }

        if (germplasmPUI != null && uri == null) {
            uri = germplasmPUI;
        }

        Faidarev1GermplasmDAO germplasmDAO = new Faidarev1GermplasmDAO(sparql, nosqlV2);

        ListWithPagination<Faidarev1GermplasmModel> resultList = germplasmDAO.faidareSearch(
                currentUser,
                uri,
                germplasmName,
                page,
                pageSize,
                nosql
        );

        Faidarev1GermplasmDTOBuilder germplasmDTOBuilder = new Faidarev1GermplasmDTOBuilder();

        // Convert paginated list to DTO
        ListWithPagination<Faidarev1GermplasmDTO> resultDTOList = resultList.convert(
                Faidarev1GermplasmDTO.class,
                germplasmModel -> {
                    try {
                        return germplasmDTOBuilder.fromModel(germplasmModel);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        return new Faidarev1GermplasmListResponse(resultDTOList).getResponse();
    }
    
}
