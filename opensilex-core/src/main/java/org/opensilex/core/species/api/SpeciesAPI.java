//******************************************************************************
//                          SpeciesAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.species.api;

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
import org.opensilex.core.CoreModule;
import org.opensilex.core.external.opensilex.SharedResourceInstanceService;
import org.opensilex.core.species.dal.SpeciesDAO;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.opensilex.security.authentication.ApiTranslatable;
import org.opensilex.security.authentication.injection.CurrentUser;

/**
 * @author Renaud COLIN
 */
@Tag(name = SpeciesAPI.CREDENTIAL_SPECIES_GROUP_ID)
@Path(SpeciesAPI.PATH)
public class SpeciesAPI {

    public static final String PATH = "/core/species";

    public final static String SPECIES_CACHE_CATEGORY = "species";

    public static final String CREDENTIAL_SPECIES_GROUP_ID = "Species";

    @Inject
    private SPARQLService sparql;

    @Inject
    private CoreModule coreModule;


    @CurrentUser
    AccountModel user;

    @GET
    @Operation(summary = "get species (no pagination)")
    @ApiTranslatable
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Return Species list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SpeciesDTO.class)))),
        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getAllSpecies(
            @Parameter(description = "Shared resource instance URI") @QueryParam("sharedResourceInstance") URI sharedResourceInstance
    ) throws Exception {
        if (sharedResourceInstance == null) {
            SpeciesDAO dao = new SpeciesDAO(sparql);
            List<SpeciesModel> species = dao.getAll(user.getLanguage());

            List<SpeciesDTO> dtoList = species.stream().map(SpeciesDTO::fromModel).collect(Collectors.toList());
            return new PaginatedListResponse<>(dtoList).getResponse();
        }

        SharedResourceInstanceService service = new SharedResourceInstanceService(coreModule.getSharedResourceInstanceConfiguration(sharedResourceInstance), user.getLanguage());
        ListWithPagination<SpeciesDTO> species = service.search(SpeciesAPI.PATH, null, SpeciesDTO.class);
        return new PaginatedListResponse<>(species).getResponse();
    }
}
