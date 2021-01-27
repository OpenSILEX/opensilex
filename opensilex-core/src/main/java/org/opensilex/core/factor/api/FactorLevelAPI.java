/*
 * ******************************************************************************
 *                                     FactorAPI.java
 *  OpenSILEX
 *  Copyright © INRA 2019
 *  Creation date:  17 December, 2019
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.factor.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.core.factor.dal.FactorLevelDAO;
import org.opensilex.core.factor.dal.FactorLevelModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Arnaud Charleroy
 */
@Api(FactorAPI.CREDENTIAL_FACTOR_GROUP_ID)
@Path("/core/factors/levels")
@ApiCredentialGroup(
        groupId = FactorAPI.CREDENTIAL_FACTOR_GROUP_ID,
        groupLabelKey = FactorAPI.CREDENTIAL_FACTOR_GROUP_LABEL_KEY
)
public class FactorLevelAPI {

    public static final String FACTOR_LEVEL_EXAMPLE_URI = "http://opensilex/set/factorLevel/irrigation.ww";
    public static final String DEFAULT_TRANSLATION_LANGUAGE = "en";

    public static final Logger LOGGER = LoggerFactory.getLogger(FactorLevelAPI.class);

    @Inject
    private SPARQLService sparql;

    @CurrentUser
    UserModel user;

    
    /**
     * Retreive factor level by uri
     *
     * @param factorLevelUri factor uri level
     * @return Return factor level
     * @throws Exception in case of server error
     */
    @GET
    @Path("{uri}")
    @ApiOperation("Get a factor level")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Factor level retrieved", response = FactorLevelGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 404, message = "Factor level not found", response = ErrorResponse.class)
    })
    public Response getFactorLevel(
            @ApiParam(value = "Factor Level URI", example = FACTOR_LEVEL_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI factorLevelUri
    ) throws Exception {
        FactorLevelDAO dao = new FactorLevelDAO(sparql);
        FactorLevelModel model = dao.get(factorLevelUri);

        if (model != null) {
            return new SingleObjectResponse<>(
                    FactorLevelGetDTO.fromModel(model)
            ).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Factor level not found",
                    "Unknown factor level URI: " + factorLevelUri.toString()
            ).getResponse();
        }
    }

    /**
     * Retreive factor level by uri
     *
     * @param factorLevelUri factor uri level
     * @return Return factor level
     * @throws Exception in case of server error
     */
    @GET
    @Path("{uri}/details")
    @ApiOperation("Get a factor level")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Factor level retrieved", response = FactorLevelGetDetailDTO.class, responseContainer = "List"),
        @ApiResponse(code = 404, message = "Factor level not found", response = ErrorResponse.class)
    })
    public Response getFactorLevelDetail(
            @ApiParam(value = "Factor Level URI", example = FACTOR_LEVEL_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI factorLevelUri
    ) throws Exception {
        FactorLevelDAO dao = new FactorLevelDAO(sparql);
        FactorLevelModel model = dao.get(factorLevelUri);

        if (model != null) {
            return new SingleObjectResponse<>(
                    FactorLevelGetDetailDTO.fromModel(model)
            ).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Factor level not found",
                    "Unknown factor level URI: " + factorLevelUri.toString()
            ).getResponse();
        }
    }
}
