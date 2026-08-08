/*
 * ******************************************************************************
 *                                     FactorAPI.java
 *  OpenSILEX
 *  Copyright © INRA 2019
 *  Creation date:  17 December, 2019
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.experiment.factor.api;

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
import org.opensilex.core.experiment.factor.dal.FactorLevelDAO;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static org.opensilex.core.experiment.factor.api.FactorAPI.*;

/**
 *
 * @author Arnaud Charleroy
 */
@Tag(name = FactorAPI.CREDENTIAL_FACTOR_GROUP_ID)
@Path("/core/experiments/factors/levels")
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
    AccountModel user;

    /**
     * Retrieve factor level by uri
     *
     * @param factorLevelUri factor uri level
     * @return Return factor level
     * @throws Exception in case of server error
     */
    @GET
    @Path("{uri}")
    @Operation(summary = "Get a factor level")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Factor level retrieved", content = @Content(schema = @Schema(implementation = FactorLevelGetDTO.class))),
        @ApiResponse(responseCode = "404", description = "Factor level not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getFactorLevel(
            @Parameter(description = "Factor Level URI", example = FACTOR_LEVEL_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI factorLevelUri
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
     * Retrieve factor level by uri
     *
     * @param factorLevelUri factor uri level
     * @return Return factor level
     * @throws Exception in case of server error
     */
    @GET
    @Path("{uri}/details")
    @Operation(summary = "Get a factor level")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Factor level retrieved", content = @Content(schema = @Schema(implementation = FactorLevelGetDetailDTO.class))),
        @ApiResponse(responseCode = "404", description = "Factor level not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getFactorLevelDetail(
            @Parameter(description = "Factor Level URI", example = FACTOR_LEVEL_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI factorLevelUri
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

    /**
     * Remove an factor level
     *
     * @param uri the factor level URI
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the deleted Factor {@link URI}
     */
    @DELETE
    @Path("{uri}")
    @Operation(summary = "Delete a factor level")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACTOR_DELETE_ID,
            credentialLabelKey = CREDENTIAL_FACTOR_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Factor level deleted", content = @Content(schema = @Schema(implementation = URI.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or unknown Factor URI", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or unknown Factor URI", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),

        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public Response deleteFactorLevel(
            @Parameter(description = "Factor level URI", example = FACTOR_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI uri) {

        // TODO : check super admin
        try {
            FactorLevelDAO dao = new FactorLevelDAO(sparql);
            FactorLevelModel model = dao.get(uri);
            if (model != null) {
                // check if scientific objects are linked to this factor level
                if (dao.isLinkedToSth(model)) {
                    return new ErrorResponse(
                            Response.Status.BAD_REQUEST,
                            "The factor level is linked to scientific object(s)",
                            "You can't delete a factor linked to another object"
                    ).getResponse();
                }
                dao.delete(uri);
                return new ObjectUriResponse(uri).getResponse();
            } else {
                return new ErrorResponse(Response.Status.NOT_FOUND, "Factor level not found",
                        "Unknown factor level URI: " + uri.toString()).getResponse();
            }

        } catch (SPARQLInvalidURIException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Factor level URI", e.getMessage())
                    .getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }
}
