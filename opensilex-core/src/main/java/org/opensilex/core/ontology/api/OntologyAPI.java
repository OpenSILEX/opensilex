/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import io.swagger.annotations.*;
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
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
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
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * @author vince
 */
@Api("Ontology")
@Path(OntologyAPI.PATH)
public class OntologyAPI {

    public static final String PATH = "/ontology";

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

    public static final String PROPERTY_ALREADY_EXISTS_MSG = "A property with the same URI already exists";
    public static final String PROPERTY_NOT_FOUND_MSG = "Property not found";
    public static final String PROPERTY_CREATE_MSG = "Create a RDF property";
    public static final String PROPERTY_UPDATE_MSG = "Update a RDF property";
    public static final String PARENT_URI_NOT_FOUND_MSG = "Parent URI not found";

    public static final String SUBCLASSES_OF_PATH = "subclasses_of";

    @GET
    @Path(SUBCLASSES_OF_PATH)
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

    public static final String SEARCH_SUB_CLASS_OF_PATH = SUBCLASSES_OF_PATH + "/search";

    @GET
    @Path(SEARCH_SUB_CLASS_OF_PATH)
    @ApiOperation("Search sub-classes tree of an RDF class")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return sub-classes tree", response = ResourceTreeDTO.class, responseContainer = "List")
    })
    public Response searchSubClassesOf(
            @ApiParam(value = "Parent RDF class URI") @QueryParam("parent_type") @ValidURI @NotNull URI parentClass,
            @ApiParam(value = "Name regex pattern", example = "plant_height") @QueryParam("name") String stringPattern,
            @ApiParam(value = "Flag to determine if only sub-classes must be include in result") @DefaultValue("false") @QueryParam("ignoreRootClasses") boolean ignoreRootClasses
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        SPARQLTreeListModel<ClassModel> treeList = ontologyStore.searchSubClasses(parentClass, stringPattern, currentUser.getLanguage(), ignoreRootClasses);

        List<ResourceTreeDTO> treeDto = ResourceTreeDTO.fromResourceTree(treeList);
        return new ResourceTreeResponse(treeDto).getResponse();
    }

    public static final String RDF_TYPE = "rdf_type";

    @GET
    @Path(RDF_TYPE)
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

    private DatatypePropertyModel getDataTypePropertyModel(OntologyStore ontologyStore, RDFPropertyDTO dto) throws SPARQLException {

        DatatypePropertyModel model = new DatatypePropertyModel();
        dto.toModel(model);

        DatatypePropertyModel parentModel;

        if (dto.getParent() != null) {
            parentModel = ontologyStore.getDataProperty(dto.getParent(), dto.getDomain(), currentUser.getLanguage());
            if (parentModel == null) {
                throw new SPARQLInvalidURIException(PARENT_URI_NOT_FOUND_MSG, dto.getParent());
            }
            model.setParent(parentModel);
            if(dto.getRange() == null){
                model.setRange(parentModel.getRange());
            }
        }

        ClassModel domainModel = ontologyStore.getClassModel(dto.getDomain(), null, currentUser.getLanguage());
        model.setDomain(domainModel);

        if(dto.getRange() != null){
            model.setRange(dto.getRange());
        }

        return model;
    }

    private ObjectPropertyModel getObjectPropertyModel(OntologyStore ontologyStore, RDFPropertyDTO dto) throws SPARQLException {
        ObjectPropertyModel model = new ObjectPropertyModel();
        dto.toModel(model);

        ObjectPropertyModel parentModel;

        if (dto.getParent() != null) {
            parentModel = ontologyStore.getObjectProperty(dto.getParent(), dto.getDomain(), currentUser.getLanguage());
            if (parentModel == null) {
                throw new SPARQLInvalidURIException(PARENT_URI_NOT_FOUND_MSG, dto.getParent());
            }
            model.setParent(parentModel);
            if(dto.getRange() == null){
                model.setRange(parentModel.getRange());
            }
        }

        ClassModel domainModel = ontologyStore.getClassModel(dto.getDomain(), null, currentUser.getLanguage());
        model.setDomain(domainModel);

        if(dto.getRange() != null){
            ClassModel rangeModel = ontologyStore.getClassModel(dto.getRange(), null, currentUser.getLanguage());
            model.setRange(rangeModel);
        }

        return model;
    }

    public static final String PROPERTY_PATH = "property";

    @POST
    @Path(PROPERTY_PATH)
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
            OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
            OntologyDAO dao = new OntologyDAO(sparql);

            boolean isDataProperty = dto.isDataProperty();
            if (isDataProperty) {
                DatatypePropertyModel model = getDataTypePropertyModel(ontologyStore, dto);
                dao.createDataProperty(model);
                SPARQLModule.getOntologyStoreInstance().reload();
                return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
            } else {
                ObjectPropertyModel model = getObjectPropertyModel(ontologyStore, dto);
                dao.createObjectProperty(model);
                SPARQLModule.getOntologyStoreInstance().reload();
                return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
            }

        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(Response.Status.CONFLICT, PROPERTY_ALREADY_EXISTS_MSG, e.getMessage()).getResponse();
        }
    }


    @PUT
    @Path(PROPERTY_PATH)
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
        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();

        boolean isDataProperty = dto.isDataProperty();
        if (isDataProperty) {
            DatatypePropertyModel model = getDataTypePropertyModel(ontologyStore, dto);
            dao.updateDataProperty(model);
            SPARQLModule.getOntologyStoreInstance().reload();
            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
        } else {
            ObjectPropertyModel model = getObjectPropertyModel(ontologyStore, dto);
            dao.updateObjectProperty(model);
            SPARQLModule.getOntologyStoreInstance().reload();
            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
        }

    }

    @GET
    @Path(PROPERTY_PATH)
    @ApiOperation("Return property model definition detail")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return property model definition ", response = RDFPropertyGetDTO.class)
    })
    public Response getProperty(
            @ApiParam(value = "Property URI") @QueryParam("uri") @ValidURI URI propertyURI,
            @ApiParam(value = "Property type") @QueryParam("rdf_type") @ValidURI URI propertyType,
            @ApiParam(value = "Property type") @QueryParam("domain_rdf_type") @ValidURI URI domainType
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        AbstractPropertyModel<?> model = ontologyStore.getProperty(propertyURI, propertyType, domainType, currentUser.getLanguage());
        return new SingleObjectResponse<>(new RDFPropertyGetDTO(model, currentUser.getLanguage())).getResponse();
    }

    @DELETE
    @Path(PROPERTY_PATH)
    @ApiOperation("Delete a property")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Property deleted ", response = ObjectUriResponse.class)
    })
    public Response deleteProperty(
            @ApiParam(value = "Property URI") @QueryParam("uri") @ValidURI URI propertyURI,
            @ApiParam(value = "Property type") @QueryParam("rdf_type") @ValidURI URI propertyType
    ) throws Exception {

        OntologyDAO dao = new OntologyDAO(sparql);

        if (RDFPropertyDTO.isDataProperty(propertyType)) {
            dao.deleteDataProperty(propertyURI);
            SPARQLModule.getOntologyStoreInstance().reload();
        } else {
            dao.deleteObjectProperty(propertyURI);
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
            @ApiParam(value = "Domain URI") @QueryParam("domain") @NotNull @ValidURI URI domainURI,
            @ApiParam(value = "Name regex pattern", example = "plant_height") @QueryParam("name") String namePattern,
            @ApiParam(value = "Return all properties from sub-classes") @QueryParam("include_sub_classes") @DefaultValue("true") boolean includeSubClasses
    ) throws Exception {

        BiPredicate<DatatypePropertyModel, ClassModel> dataPropFilter = ((property, classModel) -> property.getRangeURI() != null);
        BiPredicate<ObjectPropertyModel, ClassModel> objectPropFilter = ((property, classModel) -> property.getRangeURI() != null);

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();

        List<ResourceTreeDTO> properties = ResourceTreeDTO.fromResourceTree(Arrays.asList(
                ontologyStore.searchDataProperties(domainURI, namePattern, currentUser.getLanguage(), includeSubClasses, dataPropFilter),
                ontologyStore.searchObjectProperties(domainURI, namePattern, currentUser.getLanguage(), includeSubClasses, objectPropFilter)
        ));
        return new ResourceTreeResponse(properties).getResponse();
    }


    @GET
    @Path("/linkable_properties")
    @ApiOperation("Search properties linkable to a domain")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return property tree", response = ResourceTreeDTO.class, responseContainer = "List")
    })
    public Response getLinkableProperties(
            @ApiParam(value = "Domain URI") @QueryParam("domain") @NotNull @ValidURI URI domainURI,
            @ApiParam(value = "Domain parent URI") @QueryParam("parent") @ValidURI URI ancestorURI

    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();

        List<ResourceTreeDTO> properties = new ArrayList<>();

        ontologyStore.getLinkableDataProperties(domainURI, ancestorURI, currentUser.getLanguage()).forEach(property -> {
            ResourceTreeDTO dto = new ResourceTreeDTO();
            dto.fromModel(property);
            properties.add(dto);
        });

        ontologyStore.getLinkableObjectProperties(domainURI, ancestorURI, currentUser.getLanguage()).forEach(property -> {
            ResourceTreeDTO dto = new ResourceTreeDTO();
            dto.fromModel(property);
            properties.add(dto);
        });

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
            @ApiParam(value = "Domain URI") @QueryParam("domain") @ValidURI URI domainURI,
            @ApiParam(value = "Name regex pattern", example = "plant_height") @QueryParam("name") String namePattern
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        List<ResourceTreeDTO> properties = ResourceTreeDTO.fromResourceTree(
                ontologyStore.searchDataProperties(domainURI, namePattern, currentUser.getLanguage(), true, null)
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
            @ApiParam(value = "Domain URI") @QueryParam("domain") @ValidURI URI domainURI,
            @ApiParam(value = "Name regex pattern", example = "plant_height") @QueryParam("name") String namePattern
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();

        List<ResourceTreeDTO> properties = ResourceTreeDTO.fromResourceTree(
                ontologyStore.searchObjectProperties(domainURI, namePattern, currentUser.getLanguage(), true, null)
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
        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();

        OwlRestrictionModel restriction = this.restrictionDtoToModel(ontologyStore, dto);

        if (!dao.addClassPropertyRestriction(dto.getClassURI(), restriction, currentUser.getLanguage())) {
            return new ErrorResponse(Response.Status.CONFLICT, "Property restriction already exists for class", "Class URI: " + dto.getClassURI().toString() + " - Property URI: " + dto.getProperty().toString()).getResponse();
        }
        ontologyStore.reload();

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

        OntologyDAO dao = new OntologyDAO(sparql);
        dao.deleteClassPropertyRestriction(classURI, propertyURI, currentUser.getLanguage());
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
        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();

        OwlRestrictionModel restriction = this.restrictionDtoToModel(ontologyStore, dto);
        dao.updateClassPropertyRestriction(dto.getClassURI(), restriction, currentUser.getLanguage());
        ontologyStore.reload();

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
            @ApiResponse(code = 200, message = "Return URI label", response = NamedResourceDTO.class, responseContainer = "List")
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

    @GET
    @Path("/name_space")
    @ApiOperation("Return namespaces")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return namespaces", response = String.class)
    })
    public Response getNameSpace() {
        Map<String, String> nameSpaces = SPARQLService.getPrefixes();
        return new SingleObjectResponse<>(nameSpaces).getResponse();
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


    private OwlRestrictionModel restrictionDtoToModel(OntologyStore ontologyStore, OWLClassPropertyRestrictionDTO dto) throws Exception {
        OwlRestrictionModel restriction = new OwlRestrictionModel();

        ClassModel domainClass = ontologyStore.getClassModel(dto.getClassURI(), dto.getDomain(), currentUser.getLanguage());
        restriction.setDomain(domainClass);

        PropertyModel property = ontologyStore.getProperty(dto.getProperty(), null, dto.getDomain(), currentUser.getLanguage());
        restriction.setOnProperty(property.getUri());

        if (property instanceof DatatypePropertyModel) {
            restriction.setOnDataRange(property.getRangeURI());
        } else if (property instanceof ObjectPropertyModel) {
            restriction.setOnClass(property.getRangeURI());
        }

        if (dto.isRequired()) {
            restriction.setMinQualifiedCardinality(1);
        } else {
            restriction.setMinQualifiedCardinality(0);
        }
        if (!dto.isList()) {
            restriction.setMaxQualifiedCardinality(1);
        }

        return restriction;
    }
}
