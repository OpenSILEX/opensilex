/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import org.opensilex.sparql.response.ResourceTreeDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.core.ontology.dal.PropertyModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.rest.cache.ApiCache;
import org.opensilex.server.rest.cache.ApiCacheService;
import org.opensilex.sparql.response.ResourceTreeResponse;

/**
 *
 * @author vince
 */
@Api("Ontology")
@Path("/ontology")
public class OntologyAPI {

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

    @GET
    @Path("/subclass-of")
    @ApiOperation("Search sub-classes tree of an RDF class")
    @ApiProtected
    @ApiCache(
            category = ApiCacheService.STATIC_CATEGORY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return group", response = ResourceTreeDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response getSubClassesOf(
            @ApiParam(value = "Parent RDF class URI") @QueryParam("parentClass") @ValidURI URI parentClass,
            @ApiParam(value = "Flag to determine if only sub-classes must be include in result") @DefaultValue("false") @QueryParam("ignoreRootClasses") boolean ignoreRootClasses
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);

        SPARQLTreeListModel<ClassModel> tree = dao.searchSubClasses(
                parentClass,
                currentUser,
                ignoreRootClasses
        );

        return new ResourceTreeResponse(ResourceTreeDTO.fromResourceTree(tree)).getResponse();
    }

    @GET
    @Path("/subproperties-of")
    @ApiOperation("Search sub-properties tree of an RDF property")
    @ApiProtected
    @ApiCache(
            category = ApiCacheService.STATIC_CATEGORY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return group", response = ResourceTreeDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response getSubPropertiesOf(
            @ApiParam(value = "Parent RDF Property URI", example = "owl:DatatypeProperty") @QueryParam("parentProperty") @ValidURI URI parentProperty,
            @ApiParam(value = "Flag to determine if only sub-properties must be include in result") @DefaultValue("false") @QueryParam("ignoreRootProperties") boolean ignoreRootProperties
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);

        SPARQLTreeListModel<PropertyModel> tree = dao.searchProperties(
                parentProperty,
                currentUser,
                ignoreRootProperties
        );

        return new ResourceTreeResponse(ResourceTreeDTO.fromResourceTree(tree)).getResponse();
    }
}
