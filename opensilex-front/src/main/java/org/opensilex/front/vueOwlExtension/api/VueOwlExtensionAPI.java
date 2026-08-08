/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.api;

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
import org.opensilex.core.ontology.api.RDFTypeTranslatedDTO;
import org.opensilex.front.vueOwlExtension.dal.VueClassExtensionModel;
import org.opensilex.front.vueOwlExtension.dal.VueOwlExtensionDAO;
import org.opensilex.front.vueOwlExtension.types.VueOntologyDataType;
import org.opensilex.front.vueOwlExtension.types.VueOntologyObjectType;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.ontology.dal.AbstractPropertyModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.DatatypePropertyModel;
import org.opensilex.sparql.ontology.dal.ObjectPropertyModel;
import org.opensilex.sparql.ontology.store.OntologyStore;
import org.opensilex.sparql.response.CreatedUriResponse;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author vince
 */
@Tag(name = "Vue.js - Ontology extension")
@Path(VueOwlExtensionAPI.PATH)
public class VueOwlExtensionAPI {

    public static final String PATH = "/vuejs/owl_extension";
    public static final String RDF_TYPE_PATH = "rdf_type";


    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    @GET
    @Path("rdf_type")
    @Operation(summary = "Return rdf type model definition with properties")
    @ApiProtected()
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return class model definition ", content = @Content(schema = @Schema(implementation = VueRDFTypeDTO.class)))
    })
    public Response getRDFType(
            @Parameter(description = "RDF type URI") @QueryParam("rdf_type") @NotNull @ValidURI URI rdfType,
            @Parameter(description = "Parent RDF class URI") @QueryParam("parentType") @ValidURI URI parentType
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        try {
            ClassModel model = ontologyStore.getClassModel(rdfType, parentType, currentUser.getLanguage());

            VueClassExtensionModel modelExt = sparql.getByURI(VueClassExtensionModel.class, model.getUri(), currentUser.getLanguage());
            return new SingleObjectResponse<>(new VueRDFTypeDTO(model, modelExt)).getResponse();
        } catch (SPARQLInvalidURIException e) {
            throw new NotFoundURIException(rdfType);
        }
    }

    @POST
    @Path(RDF_TYPE_PATH)
    @Operation(summary = "Create a custom class")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Create a custom class", content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "409", description = "A class with the same URI already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })

    public Response createRDFType(
            @Parameter(description = "Class description") @Valid VueRDFTypeDTO dto
    ) throws Exception {
        try {
            VueOwlExtensionDAO dao = new VueOwlExtensionDAO(sparql);

            ClassModel classModel = dto.toModel(currentUser.getLanguage());
            classModel.setPublisher(currentUser.getUri());
            VueClassExtensionModel classExtModel = dto.getExtClassModel();
            dao.createExtendedClass(classModel, classExtModel);
            SPARQLModule.getOntologyStoreInstance().reload();

            return new CreatedUriResponse(classModel.getUri()).getResponse();

        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(Response.Status.CONFLICT, "Infrastructure already exists", e.getMessage()).getResponse();
        }
    }

    @PUT
    @Path("rdf_type")
    @Operation(summary = "Update a custom class")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Update a RDF property", content = @Content(schema = @Schema(implementation = URI.class)))
    })
    public Response updateRDFType(
            @Parameter(description = "RDF type definition") @Valid VueRDFTypeDTO dto
    ) throws Exception {

        VueOwlExtensionDAO dao = new VueOwlExtensionDAO(sparql);

        ClassModel classModel = dto.toModel(currentUser.getLanguage());
        VueClassExtensionModel classExtModel = dto.getExtClassModel();
        dao.updateExtendedClass(classModel, classExtModel);
        SPARQLModule.getOntologyStoreInstance().reload();

        return new CreatedUriResponse(classModel.getUri()).getResponse();
    }

    @PUT
    @Path("properties_order")
    @Operation(summary = "Define properties order")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Define properties order", content = @Content(schema = @Schema(implementation = URI.class)))
    })
    public Response setRDFTypePropertiesOrder(
            @Parameter(description = "RDF type", required = true) @QueryParam("rdf_type") @ValidURI @NotNull URI classURI,
            @Parameter(description = "Array of properties") @ValidURI List<URI> properties
    ) throws Exception {
        VueOwlExtensionDAO dao = new VueOwlExtensionDAO(sparql);

        dao.setPropertiesOrder(classURI, properties, currentUser.getLanguage());
        return new ObjectUriResponse(Response.Status.OK, classURI).getResponse();
    }

    @DELETE
    @Path(RDF_TYPE_PATH+"/{uri}")
    @Operation(summary = "Delete a RDF type")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Class deleted ", content = @Content(schema = @Schema(implementation = URI.class)))
    })
    public Response deleteRDFType(
            @Parameter(description = "RDF type") @PathParam("uri") @NotNull @ValidURI URI classURI
    ) throws Exception {
        VueOwlExtensionDAO dao = new VueOwlExtensionDAO(sparql);
        dao.deleteExtendedClass(classURI);
        SPARQLModule.getOntologyStoreInstance().reload();
        return new ObjectUriResponse(Response.Status.OK, classURI).getResponse();
    }

    @GET
    @Path("/rdf_type_properties")
    @Operation(summary = "Return class model properties definitions")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return class model properties definitions ", content = @Content(schema = @Schema(implementation = VueRDFTypeDTO.class)))
    })
    public Response getRDFTypeProperties(
            @Parameter(description = "RDF class URI") @QueryParam("rdf_type") @NotNull @ValidURI URI rdfType,
            @Parameter(description = "Parent RDF class URI") @QueryParam("parent_type") @NotNull @ValidURI URI parentType
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        VueOwlExtensionDAO daoExt = new VueOwlExtensionDAO(sparql);

        ClassModel classModel = ontologyStore.getClassModel(rdfType, parentType, currentUser.getLanguage());
        VueClassExtensionModel modelExt = sparql.getByURI(VueClassExtensionModel.class, classModel.getUri(), currentUser.getLanguage());

        VueRDFTypeDTO vueRDFTypeDTO = new VueRDFTypeDTO(classModel, modelExt);
        if (Objects.nonNull(classModel.getPublisher())) {
            vueRDFTypeDTO.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(classModel.getPublisher())));
        }
        vueRDFTypeDTO.setPropertiesOrder(daoExt.getPropertiesOrder(rdfType, currentUser.getLanguage()));

        for(URI propertyURI : classModel.getRestrictionsByProperties().keySet()) {

            // #TODO link restriction to property model to avoid extra properties retrieval
            AbstractPropertyModel<?> propertyModel = ontologyStore.getProperty(propertyURI,null,null, currentUser.getLanguage());

            VueRDFTypePropertyDTO vuePropertyDto = new VueRDFTypePropertyDTO(classModel, propertyModel);
            if(propertyModel instanceof DatatypePropertyModel){
                vueRDFTypeDTO.getDataProperties().add(vuePropertyDto);
            }
            else if(propertyModel instanceof ObjectPropertyModel){
                vueRDFTypeDTO.getObjectProperties().add(vuePropertyDto);
            }
        }
        return new SingleObjectResponse<>(vueRDFTypeDTO).getResponse();
    }

    @GET
    @Path("data_types")
    @Operation(summary = "Return literal datatypes definition")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return literal datatypes definition ", content = @Content(array = @ArraySchema(schema = @Schema(implementation = VueDataTypeDTO.class))))
    })
    public Response getDataTypes() throws Exception {
        List<VueDataTypeDTO> datatypeDTOs = new ArrayList<>();

        for (VueOntologyDataType datatype : VueOwlExtensionDAO.getDataTypes()) {
            VueDataTypeDTO dto = new VueDataTypeDTO();
            dto.setUri(new URI(datatype.getTypeUri()));
            dto.setInputComponent(datatype.getInputComponent());
            dto.setViewComponent(datatype.getViewComponent());
            dto.setLabelKey(datatype.getLabelKey());
            datatypeDTOs.add(dto);
        }
        return new PaginatedListResponse<>(datatypeDTOs).getResponse();
    }

    @GET
    @Path("object_types")
    @Operation(summary = "Return object types definition")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return object types definition ", content = @Content(array = @ArraySchema(schema = @Schema(implementation = VueObjectTypeDTO.class))))
    })
    public Response getObjectTypes() throws Exception {
        List<VueObjectTypeDTO> datatypeDTOs = new ArrayList<>();

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();

        for (VueOntologyObjectType objectType : VueOwlExtensionDAO.getObjectTypes()) {
            VueObjectTypeDTO dto = new VueObjectTypeDTO();
            dto.setUri(new URI(objectType.getTypeUri()));
            dto.setInputComponent(objectType.getInputComponent());
            dto.setInputComponentsByProperty(objectType.getInputComponentsMap());
            dto.setViewComponent(objectType.getViewComponent());

            ClassModel objectClass = ontologyStore.getClassModel(dto.getUri(), null, currentUser.getLanguage());
            dto.setRdfClass(new RDFTypeTranslatedDTO(objectClass));
            dto.setName(objectClass.getName());
            datatypeDTOs.add(dto);
        }
        return new PaginatedListResponse<>(datatypeDTOs).getResponse();
    }

    @GET
    @Path("rdf_types_parameters")
    @Operation(summary = "Return RDF types parameters for Vue.js application")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return rdf types parameters", content = @Content(array = @ArraySchema(schema = @Schema(implementation = VueRDFTypeParameterDTO.class))))
    })
    public Response getRDFTypesParameters() throws Exception {

        VueOwlExtensionDAO dao = new VueOwlExtensionDAO(sparql);

        List<VueClassExtensionModel> extendedClasses = dao.getExtendedClasses(currentUser.getLanguage());
        List<VueRDFTypeParameterDTO> dtoList = extendedClasses.stream().map(VueRDFTypeParameterDTO::getDTOFromModel).collect(Collectors.toList());

        return new PaginatedListResponse<>(dtoList).getResponse();
    }
}
