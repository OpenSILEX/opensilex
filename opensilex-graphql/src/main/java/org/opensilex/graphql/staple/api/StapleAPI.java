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
import org.opensilex.OpenSilex;
import org.opensilex.graphql.staple.StapleApiUtils;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapperIndex;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

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
    private OpenSilex openSilex;

    @Inject
    private SPARQLService sparql;

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
    @GET
    @Path("resource_graph")
    @ApiOperation("Get all graphs associated with resources")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResourceGraphs() throws Exception {
        Map<URI, URI> result = new HashMap<>();
        SPARQLClassObjectMapperIndex index = sparql.getMapperIndex();
        for (Class<?> resourceClass : openSilex.getAnnotatedClasses(SPARQLResource.class)) {
            if (!(SPARQLResourceModel.class.isAssignableFrom(resourceClass))) {
                break;
            }
            SPARQLClassObjectMapper<?> mapper = index.getForClass(resourceClass);
            if (mapper.getDefaultGraphURI() != null) {
                result.put(URI.create(mapper.getRDFType().getURI()), mapper.getDefaultGraphURI());
            }
        }
        return new SingleObjectResponse<>(result).getResponse();
    }
}
