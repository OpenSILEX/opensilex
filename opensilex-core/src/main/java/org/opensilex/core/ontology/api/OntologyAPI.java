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
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.OWL2;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.DatatypePropertyModel;
import org.opensilex.core.ontology.dal.ObjectPropertyModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
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

    @Inject
    private SPARQLModule sparqlModule;

    private Node getPropertyGraph() {
        Node propertyGraph = SPARQLDeserializers.nodeURI(sparqlModule.getSuffixedURI("properties"));
        return propertyGraph;
    }

    @GET
    @Path("/get-subclass-of")
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
    @Path("/get-class")
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

        ClassModel classDescription = dao.getClassModel(rdfType, currentUser.getLanguage());

        return new SingleObjectResponse<>(RDFClassDTO.fromModel(new RDFClassDTO(), classDescription)).getResponse();
    }

    @GET
    @Path("/get-classes")
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
            ClassModel classDescription = dao.getClassModel(rdfType, currentUser.getLanguage());
            classes.add(RDFClassDTO.fromModel(new RDFClassDTO(), classDescription));
        }

        return new PaginatedListResponse<>(classes).getResponse();
    }

    @POST
    @Path("create-property")
    @ApiOperation("Create a RDF property")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Create a RDF property", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "A property with the same URI already exists", response = ErrorResponse.class)
    })

    public Response createProperty(
            @ApiParam("Property description") @Valid RDFPropertyDTO dto
    ) throws Exception {
        try {
            OntologyDAO dao = new OntologyDAO(sparql);

            Node propertyGraph = getPropertyGraph();

            boolean isDataProperty = dto.isDataProperty();

            if (isDataProperty) {
                DatatypePropertyModel dtModel = new DatatypePropertyModel();
                if (dto.getParent() != null) {
                    DatatypePropertyModel parentModel = dao.getDataProperty(dto.getParent(), currentUser);
                    if (parentModel == null) {
                        throw new SPARQLInvalidURIException("Parent URI not found", dto.getParent());
                    }
                    dtModel.setParent(parentModel);
                    dtModel.setRange(parentModel.getRange());
                } else {
                    DatatypePropertyModel parentModel = new DatatypePropertyModel();
                    parentModel.setUri(new URI(OWL2.topDataProperty.getURI()));
                    dtModel.setParent(parentModel);
                }
                dto.toModel(dtModel);
                dao.createDataProperty(propertyGraph, dtModel);
                return new ObjectUriResponse(Response.Status.CREATED, dtModel.getUri()).getResponse();
            } else {
                ObjectPropertyModel objModel = new ObjectPropertyModel();

                if (dto.getParent() != null) {
                    ObjectPropertyModel parentModel = dao.getObjectProperty(dto.getParent(), currentUser);
                    if (parentModel == null) {
                        throw new SPARQLInvalidURIException("Parent URI not found", dto.getParent());
                    }
                    objModel.setParent(parentModel);
                    objModel.setRange(parentModel.getRange());
                } else {
                    ObjectPropertyModel parentModel = new ObjectPropertyModel();
                    parentModel.setUri(new URI(OWL2.topObjectProperty.getURI()));
                    objModel.setParent(parentModel);
                }

                dto.toModel(objModel);
                ClassModel range = new ClassModel();
                range.setUri(dto.getRange());
                objModel.setRange(range);
                dao.createObjectProperty(propertyGraph, objModel);
                return new ObjectUriResponse(Response.Status.CREATED, objModel.getUri()).getResponse();
            }

        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(Response.Status.CONFLICT, "Property already exists", e.getMessage()).getResponse();
        }
    }

    @PUT
    @Path("update-property")
    @ApiOperation("Update a RDF property")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Update a RDF property", response = ObjectUriResponse.class)
    })

    public Response updateProperty(
            @ApiParam("Property description") @Valid RDFPropertyDTO dto
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);

        Node propertyGraph = getPropertyGraph();

        boolean isDataProperty = dto.isDataProperty();

        if (isDataProperty) {
            DatatypePropertyModel dtModel = new DatatypePropertyModel();
            if (dto.getParent() != null) {
                DatatypePropertyModel parentModel = dao.getDataProperty(dto.getParent(), currentUser);
                if (parentModel == null) {
                    throw new SPARQLInvalidURIException("Parent URI not found", dto.getParent());
                }
                dtModel.setParent(parentModel);
                dtModel.setRange(parentModel.getRange());
            } else {
                DatatypePropertyModel parentModel = new DatatypePropertyModel();
                parentModel.setUri(new URI(OWL2.topDataProperty.getURI()));
                dtModel.setParent(parentModel);
            }
            dto.toModel(dtModel);
            dao.updateDataProperty(propertyGraph, dtModel);
            return new ObjectUriResponse(Response.Status.CREATED, dtModel.getUri()).getResponse();
        } else {
            ObjectPropertyModel objModel = new ObjectPropertyModel();

            if (dto.getParent() != null) {
                ObjectPropertyModel parentModel = dao.getObjectProperty(dto.getParent(), currentUser);
                if (parentModel == null) {
                    throw new SPARQLInvalidURIException("Parent URI not found", dto.getParent());
                }
                objModel.setParent(parentModel);
                objModel.setRange(parentModel.getRange());
            } else {
                ObjectPropertyModel parentModel = new ObjectPropertyModel();
                parentModel.setUri(new URI(OWL2.topObjectProperty.getURI()));
                objModel.setParent(parentModel);
            }

            dto.toModel(objModel);
            ClassModel range = new ClassModel();
            range.setUri(dto.getRange());
            objModel.setRange(range);
            dao.updateObjectProperty(propertyGraph, objModel);
            return new ObjectUriResponse(Response.Status.CREATED, objModel.getUri()).getResponse();
        }

    }

    @GET
    @Path("/get-property")
    @ApiOperation("Return property model definition detail")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return property model definition ", response = RDFPropertyDTO.class)
    })
    public Response getProperty(
            @ApiParam(value = "Property URI") @QueryParam("propertyURI") @ValidURI URI propertyURI,
            @ApiParam(value = "Property type") @QueryParam("propertyType") URI propertyType
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);

        RDFPropertyDTO dto;
        if (RDFPropertyDTO.isDataProperty(propertyType)) {
            DatatypePropertyModel property = dao.getDataProperty(propertyURI, currentUser);
            dto = RDFPropertyDTO.fromModel(property, property.getParent());
            dto.setRange(property.getRange());
        } else {
            ObjectPropertyModel property = dao.getObjectProperty(propertyURI, currentUser);
            dto = RDFPropertyDTO.fromModel(property, property.getParent());
            if (property.getRange() != null) {
                dto.setRange(property.getRange().getUri());
                dto.setRangeLabel(property.getRange().getName());
            }
        }

        return new SingleObjectResponse<>(dto).getResponse();
    }

    @DELETE
    @Path("/delete-property")
    @ApiOperation("Delete a property")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Property deleted ", response = RDFPropertyDTO.class)
    })
    public Response deleteProperty(
            @ApiParam(value = "Property URI") @QueryParam("propertyURI") @ValidURI URI propertyURI,
            @ApiParam(value = "Property type") @QueryParam("propertyType") URI propertyType
    ) throws Exception {
        Node propertyGraph = getPropertyGraph();
        
        if (RDFPropertyDTO.isDataProperty(propertyType)) {
            sparql.delete(propertyGraph, DatatypePropertyModel.class, propertyURI);
        } else {
            sparql.delete(propertyGraph, ObjectPropertyModel.class, propertyURI);
        }

        return new ObjectUriResponse(Response.Status.OK, propertyURI).getResponse();
    }

    @GET
    @Path("/get-properties")
    @ApiOperation("Search properties tree")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return property tree", response = ResourceTreeDTO.class, responseContainer = "List")
    })
    public Response getProperties(
            @ApiParam(value = "Domain URI") @QueryParam("domain") @ValidURI URI domainURI
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);

        SPARQLTreeListModel dataPropertyTree = dao.searchDataProperties(domainURI, currentUser);

        SPARQLTreeListModel objectPropertyTree = dao.searchObjectProperties(domainURI, currentUser);

        return new ResourceTreeResponse(ResourceTreeDTO.fromResourceTree(dataPropertyTree, objectPropertyTree)).getResponse();
    }
}
