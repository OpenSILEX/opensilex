/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

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
import org.apache.commons.lang3.BooleanUtils;
import org.opensilex.core.CoreModule;
import org.opensilex.core.utils.URIsListPostDTO;
import org.opensilex.core.sharedResource.SharedResourceInstanceDTO;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.exceptions.ConflictException;
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.ontology.dal.*;
import org.opensilex.sparql.ontology.store.OntologyStore;
import org.opensilex.sparql.response.*;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * @author vince
 */
@Tag(name = "Ontology")
@Path(OntologyAPI.PATH)
public class OntologyAPI {

    public static final String PATH = "/ontology";
    public static final String GET_NAMESPACE_PATH = "/name_space";

    public static final String GET_BASEURI_PATH = "/base_uri";

    public static final String RDF_TYPE_PROPERTY_RESTRICTION = "rdf_type_property_restriction";

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private CoreModule coreModule;

    public static final String PROPERTY_ALREADY_EXISTS_MSG = "A property with the same URI already exists";
    public static final String PROPERTY_NOT_FOUND_MSG = "Property not found";
    public static final String PROPERTY_CREATE_MSG = "Create a RDF property";
    public static final String PROPERTY_UPDATE_MSG = "Update a RDF property";
    public static final String PARENT_URI_NOT_FOUND_MSG = "Parent URI not found";

    public static final String SUBCLASSES_OF_PATH = "subclasses_of";

    @GET
    @Path(SUBCLASSES_OF_PATH)
    @Operation(summary = "Search sub-classes tree of an RDF class")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return sub-classes tree", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResourceTreeDTO.class))))
    })
    public Response getSubClassesOf(
            @Parameter(description = "Parent RDF class URI") @QueryParam("parent_type") @ValidURI URI parentClass,
            @Parameter(description = "Flag to determine if only sub-classes must be include in result") @DefaultValue("false") @QueryParam("ignoreRootClasses") boolean ignoreRootClasses
    ) throws Exception {
        return this.searchSubClassesOf(parentClass, null, ignoreRootClasses);
    }

    public static final String SEARCH_SUB_CLASS_OF_PATH = SUBCLASSES_OF_PATH + "/search";

    @GET
    @Path(SEARCH_SUB_CLASS_OF_PATH)
    @Operation(summary = "Search sub-classes tree of an RDF class")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return sub-classes tree", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResourceTreeDTO.class))))
    })
    public Response searchSubClassesOf(
            @Parameter(description = "Parent RDF class URI") @QueryParam("parent_type") @ValidURI @NotNull URI parentClass,
            @Parameter(description = "Name regex pattern", example = "plant_height") @QueryParam("name") String stringPattern,
            @Parameter(description = "Flag to determine if only sub-classes must be include in result") @DefaultValue("false") @QueryParam("ignoreRootClasses") boolean ignoreRootClasses
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        SPARQLTreeListModel<ClassModel> treeList = ontologyStore.searchSubClasses(parentClass, stringPattern, currentUser.getLanguage(), ignoreRootClasses);

        List<ResourceTreeDTO> treeDto = ResourceTreeDTO.fromResourceTree(treeList);
        return new ResourceTreeResponse(treeDto).getResponse();
    }

    public static final String RDF_TYPE = "rdf_type";

    @GET
    @Path(RDF_TYPE)
    @Operation(summary = "Return class model definition with properties")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return class model definition ", content = @Content(schema = @Schema(implementation = RDFTypeDTO.class)))
    })
    public Response getRDFType(
            @Parameter(description = "RDF type URI") @QueryParam("rdf_type") @NotNull @ValidURI URI rdfType,
            @Parameter(description = "Parent RDF class URI") @QueryParam("parent_type") @ValidURI URI parentType
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        ClassModel model = ontologyStore.getClassModel(rdfType, parentType, currentUser.getLanguage());
        return new SingleObjectResponse<>(new RDFTypeDTO(model)).getResponse();
    }

    @GET
    @Path("/rdf_types")
    @Operation(summary = "Return classes models definitions with properties for a list of rdf types")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return classes models definitions", content = @Content(array = @ArraySchema(schema = @Schema(implementation = RDFTypeDTO.class))))
    })
    public Response getClasses(
            @Parameter(description = "RDF classes URI") @QueryParam("rdf_type") @NotNull @ValidURI List<URI> rdfTypes,
            @Parameter(description = "Parent RDF class URI") @QueryParam("parent_type") @ValidURI URI parentType
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
    public static final String SUB_PROPERTY_OF_PATH = "subproperties_of";

    @POST
    @Path(PROPERTY_PATH)
    @Operation(summary = PROPERTY_CREATE_MSG)
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = PROPERTY_CREATE_MSG, content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "409", description = PROPERTY_ALREADY_EXISTS_MSG, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createProperty(
            @Parameter(description = "Property description") @Valid RDFPropertyDTO dto
    ) throws Exception {
        try {
            OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
            OntologyDAO dao = new OntologyDAO(sparql);

            boolean isDataProperty = dto.isDataProperty();
            if (isDataProperty) {
                DatatypePropertyModel model = getDataTypePropertyModel(ontologyStore, dto);
                model.setPublisher(currentUser.getUri());
                dao.createDataProperty(model);
                SPARQLModule.getOntologyStoreInstance().reload();
                return new CreatedUriResponse(model.getUri()).getResponse();
            } else {
                ObjectPropertyModel model = getObjectPropertyModel(ontologyStore, dto);
                model.setPublisher(currentUser.getUri());
                dao.createObjectProperty(model);
                SPARQLModule.getOntologyStoreInstance().reload();
                return new CreatedUriResponse(model.getUri()).getResponse();
            }

        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(Response.Status.CONFLICT, PROPERTY_ALREADY_EXISTS_MSG, e.getMessage()).getResponse();
        }
    }


    @PUT
    @Path(PROPERTY_PATH)
    @Operation(summary = "Update a RDF property")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = PROPERTY_UPDATE_MSG, content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "404", description = PROPERTY_NOT_FOUND_MSG, content = @Content(schema = @Schema(implementation = URI.class))),
    })

    public Response updateProperty(
            @Parameter(description = "Property description") @Valid RDFPropertyDTO dto
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
    @Operation(summary = "Return property model definition detail")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return property model definition ", content = @Content(schema = @Schema(implementation = RDFPropertyGetDTO.class)))
    })
    public Response getProperty(
            @Parameter(description = "Property URI") @QueryParam("uri") @ValidURI URI propertyURI,
            @Parameter(description = "Property type") @QueryParam("rdf_type") @ValidURI URI propertyType,
            @Parameter(description = "Property type") @QueryParam("domain_rdf_type") @ValidURI URI domainType
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        AbstractPropertyModel<?> model = ontologyStore.getProperty(propertyURI, propertyType, domainType, currentUser.getLanguage());
        RDFPropertyGetDTO dto = new RDFPropertyGetDTO(model, currentUser.getLanguage());
        if (Objects.nonNull(model.getPublisher())) {
            dto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(model.getPublisher())));
        }
        return new SingleObjectResponse<>(dto).getResponse();
    }

    @GET
    @Path(SUB_PROPERTY_OF_PATH)
    @Operation(summary = "Return property list from a parent property")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return property model definition ", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ObjectNamedResourceDTO.class))))
    })
    public Response getSubPropertiesOf(
            @Parameter(description = "Domain URI") @QueryParam("domain") @ValidURI URI domainURI,
            @Parameter(description = "Property URI") @QueryParam("uri") @ValidURI URI propertyURI,
            @Parameter(description = "Flag to determine if only sub-properties must be included in result") @DefaultValue("false") @QueryParam("ignoreRootProperty") boolean ignoreRootProperty
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);
        List<ObjectNamedResourceDTO> result = dao.getSubPropertiesOf(domainURI, propertyURI, ignoreRootProperty, currentUser.getLanguage());
        return new PaginatedListResponse<>(result).getResponse();
    }

    @DELETE
    @Path(PROPERTY_PATH)
    @Operation(summary = "Delete a property")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Property deleted ", content = @Content(schema = @Schema(implementation = URI.class)))
    })
    public Response deleteProperty(
            @Parameter(description = "Property URI") @QueryParam("uri") @NotNull @ValidURI URI propertyURI,
            @Parameter(description = "Property type") @QueryParam("rdf_type") @NotNull @ValidURI URI propertyType
    ) throws Exception {

        OntologyDAO dao = new OntologyDAO(sparql);

        if (RDFPropertyDTO.isDataProperty(propertyType)) {
            dao.deleteProperty(propertyURI,true);
            SPARQLModule.getOntologyStoreInstance().reload();
        } else if(RDFPropertyDTO.isObjectProperty(propertyType)) {
            dao.deleteProperty(propertyURI, false);
            SPARQLModule.getOntologyStoreInstance().reload();
        }else{
            throw new IllegalArgumentException("Unknown OWL property type " + propertyType + ". Only owl:DatatypeProperty or owl:ObjectProperty URI are accepted.");
        }

        return new ObjectUriResponse(Response.Status.OK, propertyURI).getResponse();
    }

    @GET
    @Path("/properties/{domain}")
    @Operation(summary = "Search properties tree")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return property tree", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResourceTreeDTO.class))))
    })
    public Response getProperties(
            @Parameter(description = "Domain URI") @PathParam("domain") @NotNull @ValidURI URI domainURI,
            @Parameter(description = "Name regex pattern", example = "plant_height") @QueryParam("name") String namePattern,
            @Parameter(description = "Return all properties from sub-classes") @QueryParam("include_sub_classes") @DefaultValue("true") boolean includeSubClasses
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
    @Operation(summary = "Search properties linkable to a domain")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return property tree", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResourceTreeDTO.class))))
    })
    public Response getLinkableProperties(
            @Parameter(description = "Domain URI") @QueryParam("domain") @NotNull @ValidURI URI domainURI,
            @Parameter(description = "Domain parent URI") @QueryParam("parent") @ValidURI URI ancestorURI

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
    @Path("/domain_hierarchy_restrictions")
    @Operation(summary = "Get restrictions from some super-class domain to one lower down in the hierarchy, ordered by what domain they first appear in.")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return list of objects containing domain source and list of property trees", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PropertiesByDomainDTO.class))))
    })
    public Response getPropertiesByDomainHierarchyUsingRestrictions(
            @Parameter(description = "Domain ancestor URI") @QueryParam("ancestor") @NotNull @ValidURI URI ancestorURI,
            @Parameter(description = "Domain uris from types that have ancestor as an ancestor") @NotEmpty @NotNull @ValidURI @QueryParam("children") List<URI> childrenDomains
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        List<PropertiesByDomainDTO> propertiesByDomainDTOList = new ArrayList<>();
        List<URI> encounteredRdfTypeUris = new ArrayList<>();

        if(childrenDomains.size()==1 && SPARQLDeserializers.compareURIs(childrenDomains.get(0), ancestorURI)){
            encounteredRdfTypeUris = Collections.singletonList(ancestorURI);
        }else{
            //Loop over children then fusion common rdf types after
            for(URI currentChild : childrenDomains){
                LinkedHashSet<String> encounteredRdfTypesFromChildAsStrings = ontologyStore.getAncestorHierarchy(currentChild, ancestorURI);
                if(encounteredRdfTypesFromChildAsStrings.isEmpty()){
                    throw new BadRequestException("The ancestor uri was never encountered from one of the domainUris and up.");
                }
                List<URI> encounteredRdfTypesFromChild = new ArrayList<>();
                for(String typeUriString : encounteredRdfTypesFromChildAsStrings){
                    encounteredRdfTypesFromChild.add(new URI(typeUriString));
                }
                encounteredRdfTypesFromChild.removeAll(encounteredRdfTypeUris);
                encounteredRdfTypeUris.addAll(encounteredRdfTypesFromChild);
            }
            encounteredRdfTypeUris.addAll(childrenDomains);
        }

        //Part 2 : now that we have the hierarchy of classes, get the properties from the ancestor, and at each level until domainUri
        //Get by restrictions ad return trees only that match the restrictions
        Set<String> restrictionPropertiesFromSuperClassAndUnder = ontologyStore.getOwlRestrictionsUris(ancestorURI, true);

        Set<URI> visitedProperties = new HashSet<>();
        BiPredicate<DatatypePropertyModel, ClassModel> dataPropFilter = ((property, classModel) ->
                property.getRangeURI() != null &&
                restrictionPropertiesFromSuperClassAndUnder.contains(SPARQLDeserializers.getShortURI(property.getUri())));
        BiPredicate<ObjectPropertyModel, ClassModel> objectPropFilter = ((property, classModel) ->
                property.getRangeURI() != null &&
                restrictionPropertiesFromSuperClassAndUnder.contains(SPARQLDeserializers.getShortURI(property.getUri())));
        for(int i = 0 ; i<encounteredRdfTypeUris.size() ; i++){
            URI currentRdfType = encounteredRdfTypeUris.get(i);
            List<ResourceTreeDTO> propertiesForCurrentRdfType = ResourceTreeDTO.fromResourceTree(Arrays.asList(
                    ontologyStore.searchDataProperties(currentRdfType, null, currentUser.getLanguage(), false, dataPropFilter),
                    ontologyStore.searchObjectProperties(currentRdfType, null, currentUser.getLanguage(), false, objectPropFilter)));
            Set<ResourceTreeDTO> nonVisitedPropertiesForCurrentRdfType = new HashSet<>();
            for(ResourceTreeDTO nextResourceTreeDTO : propertiesForCurrentRdfType){
                if(nextResourceTreeDTO.allMatch(tree -> visitedProperties.contains(tree.getUri()))){
                    continue;
                }
                nextResourceTreeDTO.visit((e -> visitedProperties.add(e.getUri())), true);
                nonVisitedPropertiesForCurrentRdfType.add(nextResourceTreeDTO);
            }
            propertiesByDomainDTOList.add(new PropertiesByDomainDTO(currentRdfType, new ArrayList<>(nonVisitedPropertiesForCurrentRdfType)));
        }

        return new PaginatedListResponse<>(propertiesByDomainDTOList).getResponse();
    }

    @GET
    @Path("/data_properties")
    @Operation(summary = "Search data properties tree")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return data property tree", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResourceTreeDTO.class))))
    })
    public Response getDataProperties(
            @Parameter(description = "Domain URI") @QueryParam("domain") @ValidURI URI domainURI,
            @Parameter(description = "Name regex pattern", example = "plant_height") @QueryParam("name") String namePattern
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        List<ResourceTreeDTO> properties = ResourceTreeDTO.fromResourceTree(
                ontologyStore.searchDataProperties(domainURI, namePattern, currentUser.getLanguage(), true, null)
        );
        return new ResourceTreeResponse(properties).getResponse();
    }

    @GET
    @Path("/object_properties")
    @Operation(summary = "Search object properties tree")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return object property tree", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResourceTreeDTO.class))))
    })
    public Response getObjectProperties(
            @Parameter(description = "Domain URI") @QueryParam("domain") @ValidURI URI domainURI,
            @Parameter(description = "Name regex pattern", example = "plant_height") @QueryParam("name") String namePattern
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();

        List<ResourceTreeDTO> properties = ResourceTreeDTO.fromResourceTree(
                ontologyStore.searchObjectProperties(domainURI, namePattern, currentUser.getLanguage(), true, null)
        );
        return new ResourceTreeResponse(properties).getResponse();
    }

    @POST
    @Path(RDF_TYPE_PROPERTY_RESTRICTION)
    @Operation(summary = "Add a rdf type property restriction")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Class property restriction added", content = @Content(schema = @Schema(implementation = URI.class)))
    })
    public Response addClassPropertyRestriction(
            @Parameter(description = "Property description") @Valid OWLClassPropertyRestrictionDTO dto
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
    @Operation(summary = "Delete a rdf type property restriction")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Class property restriction deleted ", content = @Content(schema = @Schema(implementation = URI.class)))
    })
    public Response deleteClassPropertyRestriction(
            @Parameter(description = "RDF type") @QueryParam("rdf_type") @ValidURI @NotNull URI classURI,
            @Parameter(description = "Property URI") @QueryParam("propertyURI") @ValidURI @NotNull URI propertyURI
    ) throws Exception {

        OntologyDAO dao = new OntologyDAO(sparql);
        dao.deleteClassPropertyRestriction(classURI, propertyURI, currentUser.getLanguage());
        SPARQLModule.getOntologyStoreInstance().reload();

        return new ObjectUriResponse(Response.Status.OK, propertyURI).getResponse();
    }

    @PUT
    @Path("rdf_type_property_restriction")
    @Operation(summary = "Update a rdf type property restriction")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Class property restriction updated", content = @Content(schema = @Schema(implementation = URI.class)))
    })
    public Response updateClassPropertyRestriction(
            @Parameter(description = "Property description") @Valid OWLClassPropertyRestrictionDTO dto
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
    @Operation(summary = "Return associated rdfs:label of an uri if exists")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return URI label", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Response getURILabel(
            @Parameter(description = "URI to get label from", required = true) @QueryParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);

        String uriLabel = dao.getURILabel(uri, currentUser.getLanguage());

        return new SingleObjectResponse<>(uriLabel).getResponse();
    }

    @POST
    @Path("/uris_labels")
    @Operation(summary = "Return associated rdfs:label of uris if they exist")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return URI label", content = @Content(array = @ArraySchema(schema = @Schema(implementation = NamedResourceDTO.class))))
    })
    public Response getURILabelsList(
            @Parameter(description = "URIs to get label from", required = true) @NotNull @ValidURI @NotEmpty List<URI> uris,
            @Parameter(description = "Context URI") @QueryParam("context") @ValidURI URI context,
            @Parameter(description = "Look for all contexts if not present in specified context") @QueryParam("searchDefault") Boolean searchDefault
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);

        List<SPARQLNamedResourceModel> results = dao.getURILabels(uris, currentUser.getLanguage(), context);
        List<NamedResourceDTO> dtoList = results.stream().map(NamedResourceDTO::getDTOFromModel).collect(Collectors.toList());

        Set<URI> foundUriSet = dtoList.stream()
                .map(NamedResourceDTO::getUri)
                .map(SPARQLDeserializers::formatURI)
                .collect(Collectors.toSet());
        Set<URI> missingUriSet = new HashSet<>(uris).stream()
                .map(SPARQLDeserializers::formatURI)
                .filter(uri -> !foundUriSet.contains(uri))
                .collect(Collectors.toSet());

        if (context != null && BooleanUtils.isTrue(searchDefault) && !missingUriSet.isEmpty()) {
            dao.getURILabels(missingUriSet, currentUser.getLanguage(), null).stream()
                    .map(NamedResourceDTO::getDTOFromModel)
                    .forEach(dtoList::add);
            Set<URI> newFoundUriSet = dtoList.stream()
                    .map(NamedResourceDTO::getUri)
                    .map(SPARQLDeserializers::formatURI)
                    .collect(Collectors.toSet());
            missingUriSet = new HashSet<>(uris).stream()
                    .map(SPARQLDeserializers::formatURI)
                    .filter(uri -> !newFoundUriSet.contains(uri))
                    .collect(Collectors.toSet());
        }

        SingleObjectResponse<List<NamedResourceDTO>> response = new SingleObjectResponse<>(dtoList);

        for (URI uri : missingUriSet) {
            response.addMetadataStatus(new StatusDTO(
                    String.format(OntologyDAO.NO_LABEL_FOR_URI_MESSAGE, uri),
                    StatusLevel.WARNING
            ));
        }

        return response.getResponse();
    }

    @GET
    @Path("/shared_resource_instances")
    @Operation(summary = "Return the list of shared resource instances")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return shared resource instances", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SharedResourceInstanceDTO.class))))
    })
    public Response getSharedResourceInstances(

    ) {
        return new PaginatedListResponse<>(coreModule.getSharedResourceInstancesFromConfiguration(currentUser.getLanguage()))
                .getResponse();
    }

    @POST
    @Path("/uri_types")
    @Operation(summary = "Return all rdf types of some URIS")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return URI rdf types", content = @Content(array = @ArraySchema(schema = @Schema(implementation = URITypesDTO.class))))
    })
    public Response getURITypes(
            @Parameter(description = "URIs to get types from", required = true) @NotNull @ValidURI @NotEmpty List<URI> uris
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);

        List<URITypesDTO> types = dao.getSuperClassesByURI(uris)
                .stream().map(URITypesDTO::fromModel)
                .collect(Collectors.toList());

        return new SingleObjectResponse<>(types).getResponse();
    }

    @POST
    @Path("/check_rdf_types")
    @Operation(summary = "Check the given rdf-types on the given uris")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return the URIs with the checked rdf:types", content = @Content(array = @ArraySchema(schema = @Schema(implementation = URITypesDTO.class))))
    })
    public Response checkURIsTypes(
            @Parameter(description = "URIs list") URIsListPostDTO dto,
            @Parameter(description = "rdf_types list you want to check on the given uris list") @NotEmpty @NotNull @ValidURI @QueryParam("rdf_types") List<URI> rdfTypes
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);
        List<URITypesModel> checkedURIsTypes = dao.checkURIsTypes(dto.getUris(), rdfTypes);
        List<URITypesDTO> dtoList = checkedURIsTypes.stream().map(URITypesDTO::fromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @Path(GET_NAMESPACE_PATH)
    @Operation(summary = "Return namespaces")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return namespaces", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Response getNameSpace() {
        Map<String, String> nameSpaces = SPARQLService.getPrefixes();
        return new SingleObjectResponse<>(nameSpaces).getResponse();
    }

    @GET
    @Path(GET_BASEURI_PATH)
    @Operation(summary = "Return base uri")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return base uri", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Response getBaseURI() {
        String base_uri = sparql.getBaseURI().toString();
        return new SingleObjectResponse<>(base_uri).getResponse();
    }

    @PUT
    @Path("{uri}/rename")
    @Operation(summary = "Rename all occurrences of the given URI", description = "**This method should not be used unless you " +
            "are fully understanding what you are doing, as it may have side-effects for external ontologies. Please " +
            "note that occurrences of the URI will NOT be changed in the NoSQL database (MongoDB).**")
    @ApiProtected(adminOnly = true)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The URI was successfully renamed")
    })
    public Response renameURI(
            @Parameter(description = "The URI to rename") @PathParam("uri") @NotNull URI uri,
            @Parameter(description = "The new URI") @QueryParam("newUri") @NotNull URI newUri
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
