/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.rest.ontology.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.rest.ontology.dal.ClassModel;
import org.opensilex.rest.ontology.dal.OntologyDAO;
import org.opensilex.rest.ontology.dal.PropertyModel;
import org.opensilex.rest.sparql.dto.ResourceTreeDTO;
import org.opensilex.rest.sparql.response.ResourceTreeResponse;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.rest.validation.ValidURI;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.tree.ResourceTree;

/**
 *
 * @author vince
 */
@Api("Ontology")
@Path("/ontology")
public class OntologyAPI {

    private final SPARQLService sparql;

    /**
     * Inject SPARQL service
     */
    @Inject
    public OntologyAPI(SPARQLService sparql) {
        this.sparql = sparql;
    }

    @GET
    @Path("/subclass-of")
    @ApiOperation("Search sub-classes tree of an RDF class")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return group", response = ResourceTreeDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response getSubClassesOf(
            @ApiParam(value = "Parent RDF class URI", example = "owl:Class") @QueryParam("parentClass") @ValidURI URI parentClass,
            @Context SecurityContext securityContext
    ) throws Exception {
        UserModel user = (UserModel) securityContext.getUserPrincipal();
        OntologyDAO dao = new OntologyDAO(sparql);

        ResourceTree<ClassModel> tree = dao.searchSubClasses(
                parentClass,
                user
        );

        return new ResourceTreeResponse(ResourceTreeDTO.fromResourceTree(tree)).getResponse();
    }

    @GET
    @Path("/subproperties-of")
    @ApiOperation("Search sub-properties tree of an RDF property")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return group", response = ResourceTreeDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response getSubPropertiesOf(
            @ApiParam(value = "Parent RDF Property URI", example = "owl:DatatypeProperty") @QueryParam("parentProperty") @ValidURI URI parentProperty,
            @Context SecurityContext securityContext
    ) throws Exception {
        UserModel user = (UserModel) securityContext.getUserPrincipal();
        OntologyDAO dao = new OntologyDAO(sparql);

        ResourceTree<PropertyModel> tree = dao.searchProperties(
                parentProperty,
                user
        );

        return new ResourceTreeResponse(ResourceTreeDTO.fromResourceTree(tree)).getResponse();
    }
}
