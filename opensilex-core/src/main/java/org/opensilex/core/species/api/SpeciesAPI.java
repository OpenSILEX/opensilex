//******************************************************************************
//                          SpeciesAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.species.api;

import io.swagger.annotations.*;
import org.opensilex.core.species.dal.SpeciesDAO;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import org.opensilex.security.authentication.ApiTranslatable;
import org.opensilex.security.authentication.injection.CurrentUser;

/**
 * @author Renaud COLIN
 */
@Api(SpeciesAPI.CREDENTIAL_SPECIES_GROUP_ID)
@Path("/core/species")
public class SpeciesAPI {

    public final static String SPECIES_CACHE_CATEGORY = "species";

    public static final String CREDENTIAL_SPECIES_GROUP_ID = "Species";

    @Inject
    private SPARQLService sparql;

    @CurrentUser
    AccountModel user;

    @GET
    @ApiOperation("get species (no pagination)")
    @ApiTranslatable
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return Species list", response = SpeciesDTO.class, responseContainer = "List"),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    public Response getAllSpecies() throws Exception {
        SpeciesDAO dao = new SpeciesDAO(sparql);
        List<SpeciesModel> species = dao.getAll(user.getLanguage());

        List<SpeciesDTO> dtoList = species.stream().map(SpeciesDTO::fromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }
    
}
