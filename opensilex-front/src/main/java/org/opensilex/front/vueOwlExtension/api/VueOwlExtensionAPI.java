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
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.jena.graph.Node;
import org.opensilex.core.ontology.api.RDFClassDTO;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.front.vueOwlExtension.dal.VueClassExtensionModel;
import org.opensilex.front.vueOwlExtension.dal.VueClassPropertyExtensionModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.service.SPARQLQueryHelper;

/**
 *
 * @author vince
 */
@Api("Ontology")
@Path("/ontology/vue-owl-extension")
public class VueOwlExtensionAPI {

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

    @GET
    @Path("/class")
    @ApiOperation("Return class model definition with properties")
    @ApiProtected()
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return class model definition ", response = VueClassDTO.class)
    })
    public Response getClass(
            @ApiParam(value = "RDF class URI") @QueryParam("rdfType") @ValidURI URI rdfType
    ) throws Exception {
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);

        ClassModel classDescription = ontologyDAO.getClassModel(rdfType, ClassModel.class, currentUser.getLanguage());

        VueClassExtensionModel classExtension = sparql.getByURI(VueClassExtensionModel.class, classDescription.getUri(), currentUser.getLanguage());

        List<VueClassPropertyExtensionModel> classPropertiesExtension = sparql.search(VueClassPropertyExtensionModel.class, currentUser.getLanguage(), (select) -> {
            select.addFilter(SPARQLQueryHelper.eq(VueClassPropertyExtensionModel.OWL_CLASS_FIELD, classDescription.getUri()));
        });

        return new SingleObjectResponse<>(VueClassDTO.fromModel(new VueClassDTO(), classDescription, classExtension, classPropertiesExtension)).getResponse();
    }

//    @GET
//    @Path("/classes")
//    @ApiOperation("Return classes models definitions with properties for a list of rdt types")
//    @ApiProtected
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    @ApiResponses(value = {
//        @ApiResponse(code = 200, message = "Return classes models definitions", response = RDFClassDTO.class, responseContainer = "List")
//    })
//    public Response getClasses(
//            @ApiParam(value = "RDF classes URI") @QueryParam("rdfType") @ValidURI List<URI> rdfTypes
//    ) throws Exception {
//        OntologyDAO dao = new OntologyDAO(sparql);
//
//        List<RDFClassDTO> classes = new ArrayList<>(rdfTypes.size());
//        for (URI rdfType : rdfTypes) {
//            ClassModel classDescription = dao.getClassModel(rdfType, ClassModel.class, currentUser.getLanguage());
//            classes.add(RDFClassDTO.fromModel(new RDFClassDTO(), classDescription));
//        }
//
//        return new PaginatedListResponse<>(classes).getResponse();
//    }
//
//    @GET
//    @Path("/class-properties")
//    @ApiOperation("Return class properties model definition available for the given rdf type domain.")
//    @ApiProtected
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    @ApiResponses(value = {
//        @ApiResponse(code = 200, message = "Return class properties model definition", response = RDFPropertyDTO.class, responseContainer = "List")
//    })
//    public Response getClassProperties(
//            @ApiParam(value = "RDF root class URI") @QueryParam("rootClassDomain") @NotNull @ValidURI URI rootType,
//            @ApiParam(value = "RDF child class URI") @QueryParam("childClassDomain") @ValidURI URI childType
//    ) throws Exception {
//        OntologyDAO dao = new OntologyDAO(sparql);
//
//        List<PropertyModel> properties = dao.searchDomainProperties(rootType, childType, currentUser.getLanguage());
//
//        List<RDFPropertyDTO> dtoList = new ArrayList<>(properties.size());
//        properties.forEach(property -> {
//            RDFPropertyDTO dto = new RDFPropertyDTO();
//            dto.setUri(property.getUri());
//            dto.setLabel(property.getName());
//            if (property.getComment() != null) {
//                dto.setComment(property.getComment().getDefaultValue());
//            }
//            dto.setDomain(property.getDomain());
//            dtoList.add(dto);
//        });
//
//        return new PaginatedListResponse<RDFPropertyDTO>(dtoList).getResponse();
//    }
}
