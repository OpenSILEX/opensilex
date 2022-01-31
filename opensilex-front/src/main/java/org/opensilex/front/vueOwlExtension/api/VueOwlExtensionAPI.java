/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.api;

import io.swagger.annotations.*;
import org.opensilex.core.ontology.api.RDFTypeDTO;
import org.opensilex.front.vueOwlExtension.dal.VueClassExtensionModel;
import org.opensilex.front.vueOwlExtension.dal.VueOwlExtensionDAO;
import org.opensilex.front.vueOwlExtension.types.VueOntologyDataType;
import org.opensilex.front.vueOwlExtension.types.VueOntologyObjectType;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.ontology.store.OntologyStore;
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
import java.util.stream.Collectors;

/**
 * @author vince
 */
@Api("Vue.js - Ontology extension")
@Path("/vuejs/owl_extension")
public class VueOwlExtensionAPI {

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

    @GET
    @Path("rdf_type")
    @ApiOperation("Return rdf type model definition with properties")
    @ApiProtected()
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return class model definition ", response = VueRDFTypeDTO.class)
    })
    public Response getRDFType(
            @ApiParam(value = "RDF type URI") @QueryParam("rdf_type") @NotNull @ValidURI URI rdfType,
            @ApiParam(value = "Parent RDF class URI") @QueryParam("parentType") @ValidURI URI parentType
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        ClassModel model = ontologyStore.getClassModel(rdfType, parentType, currentUser.getLanguage());

        VueClassExtensionModel modelExt = sparql.getByURI(VueClassExtensionModel.class, model.getUri(), currentUser.getLanguage());
        return new SingleObjectResponse<>(new VueRDFTypeDTO(model, modelExt)).getResponse();
    }

    @POST
    @Path("rdf_type")
    @ApiOperation("Create a custom class")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Create a custom class", response = ObjectUriResponse.class),
            @ApiResponse(code = 409, message = "A class with the same URI already exists", response = ErrorResponse.class)
    })

    public Response createRDFType(
            @ApiParam("Class description") @Valid VueRDFTypeDTO dto
    ) throws Exception {
        try {
            VueOwlExtensionDAO dao = new VueOwlExtensionDAO(sparql);

            ClassModel classModel = dto.getClassModel(currentUser.getLanguage());
            VueClassExtensionModel classExtModel = dto.getExtClassModel();
            dao.createExtendedClass(classModel, classExtModel);
            SPARQLModule.getOntologyStoreInstance().reload();

            return new ObjectUriResponse(Response.Status.CREATED, classModel.getUri()).getResponse();

        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(Response.Status.CONFLICT, "Infrastructure already exists", e.getMessage()).getResponse();
        }
    }

    @PUT
    @Path("rdf_type")
    @ApiOperation("Update a custom class")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Update a RDF property", response = ObjectUriResponse.class)
    })
    public Response updateRDFType(
            @ApiParam("RDF type definition") @Valid VueRDFTypeDTO dto
    ) throws Exception {

        VueOwlExtensionDAO dao = new VueOwlExtensionDAO(sparql);

        ClassModel classModel = dto.getClassModel(currentUser.getLanguage());
        VueClassExtensionModel classExtModel = dto.getExtClassModel();
        dao.updateExtendedClass(classModel, classExtModel);
        SPARQLModule.getOntologyStoreInstance().reload();

        return new ObjectUriResponse(Response.Status.CREATED, classModel.getUri()).getResponse();
    }

    @PUT
    @Path("properties_order")
    @ApiOperation("Define properties order")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Define properties order", response = ObjectUriResponse.class)
    })
    public Response setRDFTypePropertiesOrder(
            @ApiParam(value = "RDF type", required = true) @QueryParam("rdf_type") @ValidURI @NotNull URI classURI,
            @ApiParam("Array of properties") @ValidURI List<URI> properties
    ) throws Exception {
        VueOwlExtensionDAO dao = new VueOwlExtensionDAO(sparql);

        dao.setPropertiesOrder(classURI, properties, currentUser.getLanguage());
        return new ObjectUriResponse(Response.Status.OK, classURI).getResponse();
    }

    @DELETE
    @Path("/rdf_type")
    @ApiOperation("Delete a RDF type")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Class deleted ", response = ObjectUriResponse.class)
    })
    public Response deleteRDFType(
            @ApiParam(value = "RDF type") @QueryParam("rdf_type") @ValidURI URI classURI
    ) throws Exception {
        VueOwlExtensionDAO dao = new VueOwlExtensionDAO(sparql);
        dao.deleteExtendedClass(classURI);
        SPARQLModule.getOntologyStoreInstance().reload();
        return new ObjectUriResponse(Response.Status.OK, classURI).getResponse();
    }

    @GET
    @Path("/rdf_type_properties")
    @ApiOperation("Return class model properties definitions")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return class model properties definitions ", response = VueRDFTypeDTO.class)
    })
    public Response getRDFTypeProperties(
            @ApiParam(value = "RDF class URI") @QueryParam("rdf_type") @NotNull @ValidURI URI rdfType,
            @ApiParam(value = "Parent RDF class URI") @QueryParam("parent_type") @NotNull @ValidURI URI parentType
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        VueOwlExtensionDAO daoExt = new VueOwlExtensionDAO(sparql);

        ClassModel model = ontologyStore.getClassModel(rdfType, parentType, currentUser.getLanguage());
        VueClassExtensionModel modelExt = sparql.getByURI(VueClassExtensionModel.class, model.getUri(), currentUser.getLanguage());

        VueRDFTypeDTO vueRDFTypeDTO = new VueRDFTypeDTO(model, modelExt);
        vueRDFTypeDTO.setPropertiesOrder(daoExt.getPropertiesOrder(rdfType, currentUser.getLanguage()));

        return new SingleObjectResponse<>(vueRDFTypeDTO).getResponse();
    }

    @GET
    @Path("data_types")
    @ApiOperation("Return literal datatypes definition")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return literal datatypes definition ", response = VueDataTypeDTO.class, responseContainer = "List")
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
    @ApiOperation("Return object types definition")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return object types definition ", response = VueObjectTypeDTO.class, responseContainer = "List")
    })
    public Response getObjectTypes() throws Exception {
        List<VueObjectTypeDTO> datatypeDTOs = new ArrayList<>();

        OntologyDAO dao = new OntologyDAO(sparql);

        for (VueOntologyObjectType objectType : VueOwlExtensionDAO.getObjectTypes()) {
            VueObjectTypeDTO dto = new VueObjectTypeDTO();
            dto.setUri(new URI(objectType.getTypeUri()));
            dto.setInputComponent(objectType.getInputComponent());
            dto.setInputComponentsByProperty(objectType.getInputComponentsMap());
            dto.setViewComponent(objectType.getViewComponent());

            ClassModel objectClass = dao.getClassModel(dto.getUri(), null, currentUser.getLanguage());
            dto.setRdfClass(new RDFTypeDTO(objectClass));
            dto.setName(objectClass.getName());
            datatypeDTOs.add(dto);
        }
        return new PaginatedListResponse<>(datatypeDTOs).getResponse();
    }

    @GET
    @Path("rdf_types_parameters")
    @ApiOperation("Return RDF types parameters for Vue.js application")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return rdf types parameters", response = VueRDFTypeParameterDTO.class, responseContainer = "List")
    })
    public Response getRDFTypesParameters() throws Exception {

        VueOwlExtensionDAO dao = new VueOwlExtensionDAO(sparql);

        List<VueClassExtensionModel> extendedClasses = dao.getExtendedClasses(currentUser.getLanguage());
        List<VueRDFTypeParameterDTO> dtoList = extendedClasses.stream().map(VueRDFTypeParameterDTO::getDTOFromModel).collect(Collectors.toList());

        return new PaginatedListResponse<>(dtoList).getResponse();
    }
}
