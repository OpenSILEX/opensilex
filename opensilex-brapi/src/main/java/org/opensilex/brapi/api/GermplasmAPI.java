//******************************************************************************
//                          GermplasmAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.api;

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
import org.opensilex.brapi.model.BrAPIv1GermplasmDTO;
import org.opensilex.brapi.responses.BrAPIv1AccessionWarning;
import org.opensilex.brapi.responses.BrAPIv1GermplasmListResponse;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.ontology.store.OntologyStore;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * The BrAPI germplasm corresponds to an accession in OpenSILEX
 * @author Alice BOIZET
 */
@Tag(name = "BRAPI")
@Path("/brapi/")
public class GermplasmAPI extends BrapiCall {
    
    @Inject
    private SPARQLService sparql;
    
    @Inject
    private MongoDBService nosql;
    
    @CurrentUser
    AccountModel currentUser;
    
    @GET
    @Path("v1/germplasm")
    @BrapiVersion("1.3")
    @Operation(summary = "Submit a search request for germplasm (type accession in OpenSILEX")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = BrAPIv1GermplasmListResponse.class))),
        @ApiResponse(responseCode = "400", description = "Bad user request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})

    public Response getGermplasmBySearch(
            @Parameter(description = "Search by germplasmDbId (URI of an OpenSilex accession)") @QueryParam("germplasmDbId") URI uri,
            @Parameter(description = "Search by germplasmPUI (URI of an OpenSilex accession)") @QueryParam("germplasmPUI") URI germplasmPUI,
            @Parameter(description = "Search by germplasmName (name of an OpenSilex accession)") @QueryParam("germplasmName") String germplasmName,
            @Parameter(description = "Search by commonCropName (name of the species of an OpenSilex accession)") @QueryParam("commonCropName") String commonCropName,
            @Parameter(description = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        if (!ontologyStore.classExist(
                BrAPIv1AccessionWarning.ACCESSION_URI,
                BrAPIv1AccessionWarning.GERMPLASM_URI
        )) {
            throw new BadRequestException(
                    "The 'Accession' notion doesn't exist in your ontology so this service is unavailable"
            );
        }
        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);

        if (germplasmPUI != null && uri == null) {
            uri = germplasmPUI;
        }

        ListWithPagination<GermplasmModel> resultList = germplasmDAO.brapiSearch(
                currentUser,
                uri,
                germplasmName,
                commonCropName,
                page,
                pageSize
        );
        if (resultList.getList().isEmpty()){
            throw new NotFoundURIException("No accession could be found with this URI ", uri);
        }

        // Convert paginated list to DTO
        ListWithPagination<BrAPIv1GermplasmDTO> resultDTOList = resultList.convert(
                BrAPIv1GermplasmDTO.class,
                BrAPIv1GermplasmDTO::fromModel
        );
        return new BrAPIv1GermplasmListResponse(resultDTOList).getResponse();
    }
    
}
