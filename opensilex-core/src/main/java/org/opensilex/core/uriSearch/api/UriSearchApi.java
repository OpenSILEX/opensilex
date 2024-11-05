/*
 * ******************************************************************************
 *                                     UriSearchApi.java
 *  OpenSILEX
 *  Copyright © INRAE 2024
 *  Creation date:  26 august, 2024
 *  Contact: maximilian.hart@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.uriSearch.api;
import io.swagger.annotations.*;
import org.opensilex.core.germplasm.api.GermplasmAPI;
import org.opensilex.core.uriSearch.bll.UriSearchLogic;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

@Api("UriSearch")
@Path("/core/uri_search")
public class UriSearchApi {

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;

    @CurrentUser
    AccountModel currentUser;

    /**
     *
     * @param uri
     * @return a list of dtos containing label, type, typeLabel and metadata (publisher, published, creator) that correspond to the given uri
     * @throws Exception
     */
    @GET
    @Path("{uri}")
    @ApiOperation("Get a list of objects that match the passed URI")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return dto list", response = URIGlobalSearchDTO.class),
            @ApiResponse(code = 400, message = "Bad user request", response = ErrorDTO.class)
    })
    public Response searchByUri(
            @ApiParam(value = "URI", example = GermplasmAPI.GERMPLASM_EXAMPLE_SPECIES, required = true) @PathParam("uri") @ValidURI @NotNull URI uri
    ) throws Exception {
        UriSearchLogic logic = new UriSearchLogic(sparql, nosql, currentUser, fs);

        //Here we create dtos in the logic layer as it has to handle different types of models (SPARQLNamedResourceModels and MongoModels)
        URIGlobalSearchDTO result = logic.searchByUri(uri);

        if (result != null) {
            return new SingleObjectResponse<>(result).getResponse();
        } else {
            throw new NotFoundURIException("No elements matching this URI were found : ", uri);
        }
    }
}
