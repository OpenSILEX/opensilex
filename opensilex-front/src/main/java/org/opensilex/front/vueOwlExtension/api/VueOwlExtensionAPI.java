/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.core.ontology.api.RDFClassDTO;
import org.opensilex.core.ontology.api.RDFPropertyDTO;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.DatatypePropertyModel;
import org.opensilex.core.ontology.dal.ObjectPropertyModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.core.ontology.dal.OwlRestrictionModel;
import org.opensilex.front.vueOwlExtension.dal.VueClassExtensionModel;
import org.opensilex.front.vueOwlExtension.dal.VueOwlExtensionDAO;
import org.opensilex.front.vueOwlExtension.types.VueOntologyDataType;
import org.opensilex.front.vueOwlExtension.types.VueOntologyObjectType;
import org.opensilex.front.vueOwlExtension.types.VueOntologyType;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;

/**
 *
 * @author vince
 */
@Api("Vue.js - Ontology extension")
@Path("/vuejs/owl-extension")
public class VueOwlExtensionAPI {

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

    @GET
    @Path("get-class")
    @ApiOperation("Return class model definition with properties")
    @ApiProtected()
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return class model definition ", response = VueClassDTO.class)
    })
    public Response getClass(
            @ApiParam(value = "RDF class URI") @QueryParam("rdfType") @NotNull @ValidURI URI rdfType,
            @ApiParam(value = "Parent RDF class URI") @QueryParam("parentType") @ValidURI URI parentType
    ) throws Exception {
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);

        ClassModel classDescription = ontologyDAO.getClassModel(rdfType, parentType, currentUser.getLanguage());

        VueClassExtensionModel classExtension = sparql.getByURI(VueClassExtensionModel.class, classDescription.getUri(), currentUser.getLanguage());

        return new SingleObjectResponse<>(VueClassDTO.fromModel(new VueClassDTO(), classDescription, classExtension)).getResponse();
    }

    @POST
    @Path("create-class")
    @ApiOperation("Create a custom class")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Create a custom class", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "A class with the same URI already exists", response = ErrorResponse.class)
    })

    public Response createClass(
            @ApiParam("Class description") @Valid VueClassDTO dto
    ) throws Exception {
        try {
            VueOwlExtensionDAO dao = new VueOwlExtensionDAO(sparql);

            ClassModel classModel = dto.getClassModel(currentUser.getLanguage());

            VueClassExtensionModel classExtModel = dto.getExtClassModel();

            dao.createExtendedClass(classModel, classExtModel);
            return new ObjectUriResponse(Response.Status.CREATED, classModel.getUri()).getResponse();

        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(Response.Status.CONFLICT, "Infrastructure already exists", e.getMessage()).getResponse();
        }
    }

    @PUT
    @Path("update-class")
    @ApiOperation("Update a custom class")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Update a RDF property", response = ObjectUriResponse.class)
    })
    public Response updateClass(
            @ApiParam("Class description") @Valid VueClassDTO dto
    ) throws Exception {
        VueOwlExtensionDAO dao = new VueOwlExtensionDAO(sparql);

        ClassModel classModel = dto.getClassModel(currentUser.getLanguage());

        VueClassExtensionModel classExtModel = dto.getExtClassModel();

        dao.updateExtendedClass(classModel, classExtModel);
        return new ObjectUriResponse(Response.Status.CREATED, classModel.getUri()).getResponse();
    }

    @PUT
    @Path("properties-order")
    @ApiOperation("Define properties order")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Define properties order", response = ObjectUriResponse.class)
    })
    public Response setClassPropertiesOrder(
            @ApiParam(value = "Class uri", required = true) @QueryParam("classURI") @ValidURI @NotNull URI classURI,
            @ApiParam("Array of properties") @ValidURI List<URI> properties
    ) throws Exception {
        VueOwlExtensionDAO dao = new VueOwlExtensionDAO(sparql);

        dao.setPropertiesOrder(classURI, properties, currentUser.getLanguage());
        return new ObjectUriResponse(Response.Status.OK, classURI).getResponse();
    }

    @DELETE
    @Path("/delete-class")
    @ApiOperation("Delete a class")
    @ApiProtected(adminOnly = true)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Class deleted ", response = RDFPropertyDTO.class)
    })
    public Response deleteClass(
            @ApiParam(value = "Class URI") @QueryParam("classURI") @ValidURI URI classURI
    ) throws Exception {
        VueOwlExtensionDAO dao = new VueOwlExtensionDAO(sparql);
        dao.deleteExtendedClass(classURI);
        return new ObjectUriResponse(Response.Status.OK, classURI).getResponse();
    }

    @GET
    @Path("/get-class-properties")
    @ApiOperation("Return class model properties definitions")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return class model properties definitions ", response = VueClassDTO.class)
    })
    public Response getClassProperties(
            @ApiParam(value = "RDF class URI") @QueryParam("rdfType") @NotNull @ValidURI URI rdfType,
            @ApiParam(value = "Parent RDF class URI") @QueryParam("parentType") @ValidURI URI parentType
    ) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);

        ClassModel classDescription = dao.getClassModel(rdfType, parentType, currentUser.getLanguage());

        VueClassDTO classProperties = new VueClassDTO();
        VueClassExtensionModel classExtension = sparql.getByURI(VueClassExtensionModel.class, classDescription.getUri(), currentUser.getLanguage());
        VueClassDTO.fromModel(classProperties, classDescription, classExtension);

        ClassModel parentModel = classDescription.getParent();
        dao.buildProperties(parentModel, currentUser.getLanguage());

        List<VueClassPropertyDTO> dataProperties = new ArrayList<>(classDescription.getDatatypeProperties().size());
        for (DatatypePropertyModel dt : classDescription.getDatatypeProperties().values()) {
            VueClassPropertyDTO pDTO = new VueClassPropertyDTO();
            pDTO.setIsCustom(false);
            pDTO.setProperty(dt.getUri());
            pDTO.setName(dt.getName());
            if (dt.getComment() != null) {
                pDTO.setComment(dt.getComment().getDefaultValue());
            }
            OwlRestrictionModel restriction = classDescription.getRestrictions().get(dt.getUri());
            pDTO.setIsList(restriction.isList());
            pDTO.setIsRequired(restriction.isRequired());
            VueOntologyType vueType = VueOwlExtensionDAO.getVueType(dt.getTypeRestriction());
            if (vueType != null) {
                pDTO.setTargetProperty(dt.getTypeRestriction());
                pDTO.setInputComponent(vueType.getInputComponent());
                pDTO.setViewComponent(vueType.getViewComponent());
                pDTO.setIsCustom(true);
            }
            if (parentModel.isDatatypePropertyRestriction(dt.getUri())) {
                pDTO.setInherited(true);
            } else {
                pDTO.setInherited(false);
            }
            dataProperties.add(pDTO);
        }
        classProperties.setDataProperties(dataProperties);

        List<VueClassPropertyDTO> objectProperties = new ArrayList<>(classDescription.getObjectProperties().size());
        for (ObjectPropertyModel obj : classDescription.getObjectProperties().values()) {
            VueClassPropertyDTO pDTO = new VueClassPropertyDTO();
            pDTO.setIsCustom(false);
            pDTO.setProperty(obj.getUri());
            pDTO.setName(obj.getName());
            if (obj.getComment() != null) {
                pDTO.setComment(obj.getComment().getDefaultValue());
            }
            OwlRestrictionModel restriction = classDescription.getRestrictions().get(obj.getUri());
            pDTO.setIsList(restriction.isList());
            pDTO.setIsRequired(restriction.isRequired());
            VueOntologyObjectType vueType = (VueOntologyObjectType) VueOwlExtensionDAO.getVueType(obj.getTypeRestriction());
            if (vueType != null) {
                pDTO.setTargetProperty(obj.getTypeRestriction());
                pDTO.setInputComponent(vueType.getInputComponent());
                pDTO.setInputComponentsByProperty(vueType.getInputComponentsMap());
                pDTO.setViewComponent(vueType.getViewComponent());
                pDTO.setIsCustom(true);
            }
            if (parentModel.isObjectPropertyRestriction(obj.getUri())) {
                pDTO.setInherited(true);
            } else {
                pDTO.setInherited(false);
            }
            objectProperties.add(pDTO);
        }
        classProperties.setObjectProperties(objectProperties);

        VueOwlExtensionDAO daoExt = new VueOwlExtensionDAO(sparql);

        classProperties.setPropertiesOrder(daoExt.getPropertiesOrder(rdfType, currentUser.getLanguage()));

        return new SingleObjectResponse<>(classProperties).getResponse();
    }

    @GET
    @Path("get-data-types")
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
        return new PaginatedListResponse<VueDataTypeDTO>(datatypeDTOs).getResponse();
    }

    @GET
    @Path("get-object-types")
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
            dto.setRdfClass(RDFClassDTO.fromModel(new RDFClassDTO(), objectClass));
            datatypeDTOs.add(dto);
        }
        return new PaginatedListResponse<VueObjectTypeDTO>(datatypeDTOs).getResponse();
    }

    @GET
    @Path("get-classes-parameters")
    @ApiOperation("Return class parameters for Vue.js application")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return class parameters", response = VueClassParameterDTO.class, responseContainer = "List")
    })
    public Response getClassesParameters() throws Exception {
        VueOwlExtensionDAO dao = new VueOwlExtensionDAO(sparql);

        List<VueClassExtensionModel> extendedClasses = dao.getExtendedClasses(currentUser.getLanguage());

        List<VueClassParameterDTO> dtoList = extendedClasses.stream().map(VueClassParameterDTO::getDTOFromModel).collect(Collectors.toList());

        return new PaginatedListResponse<VueClassParameterDTO>(dtoList).getResponse();
    }
}
