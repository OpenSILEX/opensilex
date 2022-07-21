//******************************************************************************
//                          SpeciesAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.species.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import org.opensilex.core.CoreModule;
import org.opensilex.core.sharedResource.SharedResourcesFunctions;
import org.opensilex.core.species.dal.SpeciesDAO;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.core.variable.api.entity.EntityDetailsDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.opensilex.security.authentication.ApiTranslatable;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;

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

    @Inject
    private CoreModule coreModule;


    @CurrentUser
    UserModel user;

    @GET
    @ApiOperation("get species (no pagination)")
    @ApiTranslatable
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return Species list", response = SpeciesDTO.class, responseContainer = "List"),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    public Response getAllSpecies(
            @ApiParam(value = "Resource") @QueryParam("resource") URI resource
    ) throws Exception {

        if (resource == null) {
            SpeciesDAO dao = new SpeciesDAO(sparql);
            List<SpeciesModel> species = dao.getAll(user.getLanguage());

            List<SpeciesDTO> dtoList = species.stream().map(SpeciesDTO::fromModel).collect(Collectors.toList());
            return new PaginatedListResponse<>(dtoList).getResponse();
        }else{
            SharedResourcesFunctions sharedResourcesFunctions = SharedResourcesFunctions.getInstance(coreModule);

            // construction de l'adresse du service avec l'uri encodé de chaque variable
            String token = sharedResourcesFunctions.getToken(resource.toString());
            String urlService = resource.toString() + "/core/species";

            List<SpeciesDTO> resultDTOList = new ArrayList<>();
            // utilisation du service de recherche des espèces sur la ressource partagée
            String stringResponse = sharedResourcesFunctions.connectionToService(urlService, token);
            JsonNode jsonResult;
            if (stringResponse != null) {
                ObjectMapper mapper = new ObjectMapper();
                jsonResult = mapper.readTree(stringResponse);
                SingleObjectResponse<List<SpeciesDTO>> getResponse = mapper.convertValue(jsonResult, new TypeReference<SingleObjectResponse<List<SpeciesDTO>>>() {});
                resultDTOList = getResponse.getResult();
            }
            return new PaginatedListResponse<>(resultDTOList).getResponse();
        }
    }
    
}
