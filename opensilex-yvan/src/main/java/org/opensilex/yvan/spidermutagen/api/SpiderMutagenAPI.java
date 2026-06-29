/*
 * *****************************************************************************
 *                         SpiderMutageneAPI.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 26/06/2026 17:17
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.yvan.spidermutagen.api;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.sparql.response.CreatedUriResponse;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class SpiderMutagenAPI {
    @Inject
    private SPARQLService sparqlService;

    @POST
    @ApiOperation("Add a spider mutagene")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_PERSON_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_PERSON_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses({
            @ApiResponse(code = 201, message = "A SM is created"),
            @ApiResponse(code = 409, message = "The SM already exists (duplicate URI)")
    })
    public Response createPerson(
            @ApiParam("Person description") @Valid SpiderMutagenDTO dto
    ) throws Exception {

        return new CreatedUriResponse().getResponse();
    }

}
