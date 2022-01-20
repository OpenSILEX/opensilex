/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import io.swagger.annotations.*;
import org.apache.jena.graph.Node;
import org.opensilex.core.CoreModule;
import org.opensilex.core.URIsListPostDTO;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.exceptions.ConflictException;
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.ontology.dal.*;
import org.opensilex.sparql.ontology.store.OntologyStore;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.response.ResourceTreeDTO;
import org.opensilex.sparql.response.ResourceTreeResponse;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
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

    public static final String PROPERTY_ALREADY_EXISTS_MSG = "A property with the same URI already exists";
    public static final String PROPERTY_NOT_FOUND_MSG = "Property not found";
    public static final String PROPERTY_CREATE_MSG = "Create a RDF property";
    public static final String PROPERTY_UPDATE_MSG = "Update a RDF property";
    public static final String PARENT_URI_NOT_FOUND_MSG = "Parent URI not found";

    public static final String PROPERTIES_GRAPH = SPARQLClassObjectMapper.DEFAULT_GRAPH_KEYWORD + "/properties";

    private Node getPropertyGraph() {
        return SPARQLDeserializers.nodeURI(sparqlModule.getSuffixedURI(PROPERTIES_GRAPH));
    }


    @GET
    @Path("/subclasses_of")
    @ApiOperation("Search sub-classes tree of an RDF class")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return sub-classes tree", response = ResourceTreeDTO.class, responseContainer = "List")
    })
    public Response getSubClassesOf(
            @ApiParam(value = "Parent RDF class URI") @QueryParam("parent_type") @ValidURI URI parentClass,
            @ApiParam(value = "Flag to determine if only sub-classes must be include in result") @DefaultValue("false") @QueryParam("ignoreRootClasses") boolean ignoreRootClasses
    ) throws Exception {
        return this.searchSubClassesOf(parentClass, null, ignoreRootClasses);
    }

    @GET
    @Path("/subclasses_of/search")
    @ApiOperation("Search sub-classes tree of an RDF class")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return sub-classes tree", response = ResourceTreeDTO.class, responseContainer = "List")
    })
    public Response searchSubClassesOf(
            @ApiParam(value = "Parent RDF class URI") @QueryParam("parent_type") @ValidURI URI parentClass,
            @ApiParam(value = "Name regex pattern", example = "plant_height") @QueryParam("name") String stringPattern,
            @ApiParam(value = "Flag to determine if only sub-classes must be include in result") @DefaultValue("false") @QueryParam("ignoreRootClasses") boolean ignoreRootClasses
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        SPARQLTreeListModel<ClassModel> treeList = ontologyStore.searchSubClasses(parentClass, stringPattern, currentUser.getLanguage(), ignoreRootClasses);

        List<ResourceTreeDTO> treeDto = ResourceTreeDTO.fromResourceTree(treeList);
        return new ResourceTreeResponse(treeDto).getResponse();
    }

    @GET
    @Path("/rdf_type")
    @ApiOperation("Return class model definition with properties")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return class model definition ", response = RDFTypeDTO.class)
    })
    public Response getRDFType(
            @ApiParam(value = "RDF type URI") @QueryParam("rdf_type") @NotNull @ValidURI URI rdfType,
            @ApiParam(value = "Parent RDF class URI") @QueryParam("parent_type") @ValidURI URI parentType
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        ClassModel model = ontologyStore.getClassModel(rdfType, parentType, currentUser.getLanguage());
        return new SingleObjectResponse<>(new RDFTypeDTO(model)).getResponse();
    }

    @GET
    @Path("/rdf_types")
    @ApiOperation("Return classes models definitions with properties for a list of rdf types")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return classes models definitions", response = RDFTypeDTO.class, responseContainer = "List")
    })
    public Response getClasses(
            @ApiParam(value = "RDF classes URI") @QueryParam("rdf_type") @NotNull @ValidURI List<URI> rdfTypes,
            @ApiParam(value = "Parent RDF class URI") @QueryParam("parent_type") @ValidURI URI parentType
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();

        List<RDFTypeDTO> classes = new ArrayList<>(rdfTypes.size());
        for (URI rdfType : rdfTypes) {
            ClassModel model = ontologyStore.getClassModel(rdfType, parentType, currentUser.getLanguage());
            classes.add(new RDFTypeDTO(model));
        }

        return new PaginatedListResponse<>(classes).getResponse();
    }

    private DatatypePropertyModel getDataTypePropertyModel(OntologyDAO ontologyDAO, RDFPropertyDTO dto) throws Exception {

        DatatypePropertyModel model = new DatatypePropertyModel();
        DatatypePropertyModel parentModel;

        if (dto.getParent() != null) {
            parentModel = ontologyDAO.getDataProperty(dto.getParent(), dto.getDomainType(), currentUser.getLanguage());
            if (parentModel == null) {
                throw new SPARQLInvalidURIException(PARENT_URI_NOT_FOUND_MSG, dto.getParent());
            }
            model.setRange(parentModel.getRange());
        } else {
            parentModel = CoreModule.getOntologyCacheInstance().getTopDatatypePropertyModel();
            model.setRange(dto.getRange());
        }
        dto.toModel(model);
        model.setParent(parentModel);

        return model;
    }

    private ObjectPropertyModel getObjectPropertyModel(OntologyDAO ontologyDAO, RDFPropertyDTO dto) throws Exception {
        ObjectPropertyModel model = new ObjectPropertyModel();
        ObjectPropertyModel parentModel;
        ClassModel rangeModel;
        if (dto.getParent() != null) {
            parentModel = ontologyDAO.getObjectProperty(dto.getParent(), dto.getDomainType(), currentUser.getLanguage());
            if (parentModel == null) {
                throw new SPARQLInvalidURIException(PARENT_URI_NOT_FOUND_MSG, dto.getParent());
            }
            rangeModel = parentModel.getRange();
        } else {
            parentModel = CoreModule.getOntologyCacheInstance().getTopObjectPropertyModel();
            rangeModel = new ClassModel();
            rangeModel.setUri(dto.getRange());
        }
        dto.toModel(model);
        model.setParent(parentModel);
        model.setRange(rangeModel);
        return model;
    }

    @POST
    @Path("property")
    @ApiOperation(PROPERTY_CREATE_MSG)
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = PROPERTY_CREATE_MSG, response = ObjectUriResponse.class),
            @ApiResponse(code = 409, message = PROPERTY_ALREADY_EXISTS_MSG, response = ErrorResponse.class)
    })
    public Response createProperty(
            @ApiParam("Property description") @Valid RDFPropertyDTO dto
    ) throws Exception {
        try {
            OntologyDAO dao = new OntologyDAO(sparql);
            Node propertyGraph = getPropertyGraph();

            boolean isDataProperty = dto.isDataProperty();
            if (isDataProperty) {
                DatatypePropertyModel model = getDataTypePropertyModel(dao, dto);
                dao.createDataProperty(propertyGraph, model);
                SPARQLModule.getOntologyStoreInstance().reload();
                return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
            } else {
                ObjectPropertyModel model = getObjectPropertyModel(dao, dto);
                dao.createObjectProperty(propertyGraph, model);
                SPARQLModule.getOntologyStoreInstance().reload();
                return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
            }

        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(Response.Status.CONFLICT, PROPERTY_ALREADY_EXISTS_MSG, e.getMessage()).getResponse();
        }
    }


    @PUT
    @Path("property")
    @ApiOperation("Update a RDF property")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = PROPERTY_UPDATE_MSG, response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = PROPERTY_NOT_FOUND_MSG, response = ObjectUriResponse.class),
    })

    public Response updateProperty(
            @ApiParam("Property description") @Valid RDFPropertyDTO dto
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);

        Node propertyGraph = getPropertyGraph();

        boolean isDataProperty = dto.isDataProperty();
        if (isDataProperty) {
            DatatypePropertyModel model = getDataTypePropertyModel(dao, dto);
            dao.updateDataProperty(propertyGraph, model);
            SPARQLModule.getOntologyStoreInstance().reload();
            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
        } else {
            ObjectPropertyModel objModel = getObjectPropertyModel(dao, dto);
            dao.updateObjectProperty(propertyGraph, objModel);
            SPARQLModule.getOntologyStoreInstance().reload();
            return new ObjectUriResponse(Response.Status.OK, objModel.getUri()).getResponse();
        }

    }

    @GET
    @Path("/property")
    @ApiOperation("Return property model definition detail")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return property model definition ", response = RDFPropertyDTO.class)
    })
    public Response getProperty(
            @ApiParam(value = "Property URI") @QueryParam("uri") @ValidURI URI propertyURI,
            @ApiParam(value = "Property type") @QueryParam("rdf_type") @ValidURI URI propertyType,
            @ApiParam(value = "Property type") @QueryParam("domain_rdf_type") @ValidURI URI domainType
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        AbstractPropertyModel<?> model = ontologyStore.getProperty(propertyURI, propertyType, domainType, currentUser.getLanguage());
        return new SingleObjectResponse<>(new RDFPropertyDTO(model)).getResponse();
    }

    @DELETE
    @Path("/property")
    @ApiOperation("Delete a property")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Property deleted ", response = ObjectUriResponse.class)
    })
    public Response deleteProperty(
            @ApiParam(value = "Property URI") @QueryParam("propertyURI") @ValidURI URI propertyURI,
            @ApiParam(value = "Property type") @QueryParam("propertyType") @ValidURI URI propertyType
    ) throws Exception {

        OntologyDAO dao = new OntologyDAO(sparql);
        Node propertyGraph = getPropertyGraph();

        if (RDFPropertyDTO.isDataProperty(propertyType)) {
            dao.deleteDataProperty(propertyGraph, propertyURI);
            SPARQLModule.getOntologyStoreInstance().reload();
        } else {
            dao.deleteObjectProperty(propertyGraph, propertyURI);
            SPARQLModule.getOntologyStoreInstance().reload();
        }

        return new ObjectUriResponse(Response.Status.OK, propertyURI).getResponse();
    }

    @GET
    @Path("/properties/{domain}")
    @ApiOperation("Search properties tree")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return property tree", response = ResourceTreeDTO.class, responseContainer = "List")
    })
    public Response getProperties(
            @ApiParam(value = "Domain URI") @PathParam("domain") @NotNull @ValidURI URI domainURI
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();

        List<ResourceTreeDTO> properties = ResourceTreeDTO.fromResourceTree(Arrays.asList(
                ontologyStore.searchDataProperties(domainURI, currentUser.getLanguage(),true),
                ontologyStore.searchObjectProperties(domainURI, currentUser.getLanguage(),true))
        );
        return new ResourceTreeResponse(properties).getResponse();
    }


    @GET
    @Path("/data_properties")
    @ApiOperation("Search data properties tree")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return data property tree", response = ResourceTreeDTO.class, responseContainer = "List")
    })
    public Response getDataProperties(
            @ApiParam(value = "Domain URI") @QueryParam("domain") @ValidURI URI domainURI
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        List<ResourceTreeDTO> properties = ResourceTreeDTO.fromResourceTree(
                ontologyStore.searchDataProperties(domainURI, currentUser.getLanguage(),true)
        );
        return new ResourceTreeResponse(properties).getResponse();
    }

    @GET
    @Path("/object_properties")
    @ApiOperation("Search object properties tree")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return object property tree", response = ResourceTreeDTO.class, responseContainer = "List")
    })
    public Response getObjectProperties(
            @ApiParam(value = "Domain URI") @QueryParam("domain") @ValidURI URI domainURI
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();

        List<ResourceTreeDTO> properties = ResourceTreeDTO.fromResourceTree(
                ontologyStore.searchObjectProperties(domainURI, currentUser.getLanguage(),true)
        );
        return new ResourceTreeResponse(properties).getResponse();
    }

    @POST
    @Path("rdf_type_property_restriction")
    @ApiOperation("Add a rdf type property restriction")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Class property restriction added", response = ObjectUriResponse.class)
    })
    public Response addClassPropertyRestriction(
            @ApiParam("Property description") @Valid OWLClassPropertyRestrictionDTO dto
    ) throws Exception {

        OntologyDAO dao = new OntologyDAO(sparql);

        OwlRestrictionModel restriction = this.restrictionDtoToModel(dao, dto);
        Node propertyGraph = getPropertyGraph();

        if (!dao.addClassPropertyRestriction(propertyGraph, dto.getClassURI(), restriction, currentUser.getLanguage())) {
            return new ErrorResponse(Response.Status.CONFLICT, "Property restriction already exists for class", "Class URI: " + dto.getClassURI().toString() + " - Property URI: " + dto.getProperty().toString()).getResponse();
        }
        SPARQLModule.getOntologyStoreInstance().reload();

        return new ObjectUriResponse(new URI("about:blank")).getResponse();
    }

    @DELETE
    @Path("/rdf_type_property_restriction")
    @ApiOperation("Delete a rdf type property restriction")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Class property restriction deleted ", response = ObjectUriResponse.class)
    })
    public Response deleteClassPropertyRestriction(
            @ApiParam(value = "RDF type") @QueryParam("rdf_type") @ValidURI @NotNull URI classURI,
            @ApiParam(value = "Property URI") @QueryParam("propertyURI") @ValidURI @NotNull URI propertyURI
    ) throws Exception {
        Node propertyGraph = getPropertyGraph();

        OntologyDAO dao = new OntologyDAO(sparql);
        dao.deleteClassPropertyRestriction(propertyGraph, classURI, propertyURI, currentUser.getLanguage());
        SPARQLModule.getOntologyStoreInstance().reload();

        return new ObjectUriResponse(Response.Status.OK, propertyURI).getResponse();
    }

    @PUT
    @Path("rdf_type_property_restriction")
    @ApiOperation("Update a rdf type property restriction")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Class property restriction updated", response = ObjectUriResponse.class)
    })
    public Response updateClassPropertyRestriction(
            @ApiParam("Property description") @Valid OWLClassPropertyRestrictionDTO dto
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);

        OwlRestrictionModel restriction = this.restrictionDtoToModel(dao, dto);
        dao.updateClassPropertyRestriction(getPropertyGraph(), dto.getClassURI(), restriction, currentUser.getLanguage());
        SPARQLModule.getOntologyStoreInstance().reload();

        return new ObjectUriResponse(new URI("about:blank")).getResponse();
    }

    @GET
    @Path("/uri_label")
    @ApiOperation("Return associated rdfs:label of an uri if exists")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return URI label", response = String.class)
    })
    public Response getURILabel(
            @ApiParam(value = "URI to get label from", required = true) @QueryParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);

        String uriLabel = dao.getURILabel(uri, currentUser.getLanguage());

        return new SingleObjectResponse<>(uriLabel).getResponse();
    }

    @GET
    @Path("/uris_labels")
    @ApiOperation("Return associated rdfs:label of uris if they exist")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return URI label", response = String.class)
    })
    public Response getURILabelsList(
            @ApiParam(value = "URIs to get label from", required = true) @QueryParam("uri") @NotNull @ValidURI @NotEmpty List<URI> uris,
            @ApiParam(value = "Context URI") @QueryParam("context") @ValidURI URI context
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);

        List<SPARQLNamedResourceModel> results = dao.getURILabels(uris, currentUser.getLanguage(), context);
        List<NamedResourceDTO> dtoList = results.stream().map(NamedResourceDTO::getDTOFromModel).collect(Collectors.toList());

        return new SingleObjectResponse<>(dtoList).getResponse();
    }

    @POST
    @Path("/check_rdf_types")
    @ApiOperation("Check the given rdf-types on the given uris")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return the URIs with the checked rdf:types", response = URITypesDTO.class, responseContainer = "List")
    })
    public Response checkURIsTypes(
            @ApiParam(value = "URIs list") URIsListPostDTO dto,
            @ApiParam(value = "rdf_types list you want to check on the given uris list") @NotEmpty @NotNull @ValidURI @QueryParam("rdf_types") List<URI> rdfTypes
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);
        List<URITypesModel> checkedURIsTypes = dao.checkURIsTypes(dto.getUris(), rdfTypes);
        List<URITypesDTO> dtoList = checkedURIsTypes.stream().map(URITypesDTO::fromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @PUT
    @Path("{uri}/rename")
    @ApiOperation(value = "Rename all occurrences of the given URI", notes = "**This method should not be used unless you " +
            "are fully understanding what you are doing, as it may have side-effects for external ontologies. Please " +
            "note that occurrences of the URI will NOT be changed in the NoSQL database (MongoDB).**")
    @ApiProtected(adminOnly = true)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The URI was successfully renamed")
    })
    public Response renameURI(
            @ApiParam(value = "The URI to rename") @PathParam("uri") @NotNull URI uri,
            @ApiParam(value = "The new URI") @QueryParam("newUri") @NotNull URI newUri
    ) throws Exception {
        if (!sparql.checkTripleURIExists(uri)) {
            throw new NotFoundException("URI not found : " + uri);
        }
        if (sparql.checkTripleURIExists(newUri)) {
            throw new ConflictException("Cannot rename the URI, target URI already exists : " + newUri);
        }
        try {
            sparql.startTransaction();
            sparql.renameTripleURI(uri, newUri);
            sparql.renameGraph(uri, newUri);
            sparql.commitTransaction();
        } catch (Exception e) {
            sparql.rollbackTransaction(e);
            throw e;
        }
        return new ObjectUriResponse(Response.Status.OK, newUri).getResponse();
    }


    private OwlRestrictionModel restrictionDtoToModel(OntologyDAO dao, OWLClassPropertyRestrictionDTO dto) throws Exception {
        OwlRestrictionModel restriction = new OwlRestrictionModel();

        URI propertyURI = dto.getProperty();
        restriction.setOnProperty(propertyURI);

        if (dto.isRequired()) {
            restriction.setMinCardinality(1);
        } else {
            restriction.setMinCardinality(0);
        }

        if (!dto.isList()) {
            restriction.setMaxCardinality(1);
        }

        DatatypePropertyModel dataProp = dao.getDataProperty(propertyURI, dto.getDomain(), currentUser.getLanguage());
        if (dataProp == null) {
            URI objectRangeURI = dao.getObjectProperty(propertyURI, dto.getDomain(), currentUser.getLanguage()).getRange().getUri();
//            restriction.setOnClass(objectRangeURI);
        } else {
            URI dataRangeURI = dataProp.getRange();
            restriction.setOnDataRange(dataRangeURI);
        }
        restriction.setDomain(dto.getDomain());

        return restriction;
    }
}
