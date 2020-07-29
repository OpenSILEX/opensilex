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
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.OK;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.core.ontology.dal.PropertyModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return sub-classes tree", response = ResourceTreeDTO.class, responseContainer = "List")
    })
    public Response getSubClassesOf(
            @ApiParam(value = "Parent RDF class URI") @QueryParam("parentClass") @ValidURI URI parentClass,
            @ApiParam(value = "Flag to determine if only sub-classes must be include in result") @DefaultValue("false") @QueryParam("ignoreRootClasses") boolean ignoreRootClasses
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);

        SPARQLTreeListModel tree = dao.searchSubClasses(
                parentClass,
                ClassModel.class,
                currentUser,
                ignoreRootClasses,
                null
        );

        return new ResourceTreeResponse(ResourceTreeDTO.fromResourceTree(tree)).getResponse();
    }

    @GET
    @Path("/class")
    @ApiOperation("Return class model definition with properties")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return class model definition ", response = RDFClassDTO.class)
    })
    public Response getClass(
            @ApiParam(value = "RDF class URI") @QueryParam("rdfType") @ValidURI URI rdfType
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);

        ClassModel classDescription = dao.getClassModel(rdfType, ClassModel.class, currentUser.getLanguage());

        return new SingleObjectResponse<>(RDFClassDTO.fromModel(new RDFClassDTO(), classDescription)).getResponse();
    }

    @GET
    @Path("/classes")
    @ApiOperation("Return classes models definitions with properties for a list of rdt types")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return classes models definitions", response = RDFClassDTO.class, responseContainer = "List")
    })
    public Response getClasses(
            @ApiParam(value = "RDF classes URI") @QueryParam("rdfType") @ValidURI List<URI> rdfTypes
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);

        List<RDFClassDTO> classes = new ArrayList<>(rdfTypes.size());
        for (URI rdfType : rdfTypes) {
            ClassModel classDescription = dao.getClassModel(rdfType, ClassModel.class, currentUser.getLanguage());
            classes.add(RDFClassDTO.fromModel(new RDFClassDTO(), classDescription));
        }

        return new PaginatedListResponse<>(classes).getResponse();
    }

    @GET
    @Path("/class-properties")
    @ApiOperation("Return class properties model definition available for the given rdf type domain.")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return class properties model definition", response = RDFClassPropertyDTO.class, responseContainer = "List")
    })
    public Response getClassProperties(
            @ApiParam(value = "RDF class URI") @QueryParam("rdfType") @ValidURI URI rdfType
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);

        List<PropertyModel> properties = dao.searchDomainProperties(new URI("vocabulary:Device"), rdfType, currentUser.getLanguage());

        return Response.status(OK)
                .entity(properties)
                .type(MediaType.APPLICATION_JSON)
                .build();
//        return new PaginatedListResponse<Object>(properties).getResponse();
    }
}
