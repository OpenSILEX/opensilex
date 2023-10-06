/*******************************************************************************
 *                         StapleAPI.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2023.
 * Last Modification: 02/10/2023
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.graphql.staple.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFWriter;
import org.opensilex.graphql.staple.StapleApiUtils;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;

/**
 * Services useful only for the Staple GraphQL API
 *
 * @author Valentin Rigolle
 */
@Api("Staple API")
@Path(StapleAPI.PATH)
public class StapleAPI {
    public static final String PATH = "/staple";

    @Inject
    private StapleApiUtils stapleApiUtils;

    @GET
    @Path("ontology_file")
    @ApiOperation("Export ontology file for Staple API as turtle syntax")
    @Produces("application/x-turtle")
    public Response exportOntologyFile() throws Exception {
        Model stapleModel = stapleApiUtils.getStapleModel();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        RDFWriter.create()
                .source(stapleModel)
                .lang(Lang.TURTLE)
                .build()
                .output(baos);

        return Response.ok(baos.toByteArray(), new MediaType("application", "x-turtle")).build();
    }
}
