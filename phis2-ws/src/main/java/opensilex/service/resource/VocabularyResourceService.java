//******************************************************************************
//                            VocabularyResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 18 June 2018
// Contact: morgane.vidal@inra.fr, arnaud.charleroy@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.VocabularyDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.dto.PropertyVocabularyDTO;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.view.brapi.Status;
import opensilex.service.result.ResultForm;
import opensilex.service.model.Namespace;
import opensilex.service.model.Property;

/**
 * Vocabulary resource service.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/vocabularies")
@Path("/vocabularies")
public class VocabularyResourceService extends ResourceService {
    
    /**
     * Gets the list of RDFs properties that can be used.
     * @param vocabularyDAO
     * @return the list of the RDFs properties, with their labels
     */
    private Response getRdfsData(VocabularyDAO vocabularyDAO) {
        ArrayList<PropertyVocabularyDTO> properties;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<PropertyVocabularyDTO> getResponse;

        properties = vocabularyDAO.allPaginateRdfsProperties();

        if (properties == null) {
            getResponse = new ResultForm<>(0, 0, properties, true);
            return noResultFound(getResponse, statusList);
        } else if (properties.isEmpty()) {
            getResponse = new ResultForm<>(0, 0, properties, true);
            return noResultFound(getResponse, statusList);
        } else {
            getResponse = new ResultForm<>(vocabularyDAO.getPageSize(), vocabularyDAO.getPage(), properties, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        }
    }

    /**
     * Gets the list of RDFs properties that can be used.
     * @param pageSize
     * @param page
     * @return list of the RDFs properties, with their labels 
     * @example
     * { 
     *  "metadata":
     * { 
     *  "pagination": 
     *      { 
     *          "pageSize": 20, 
     *          "currentPage": 0, 
     *          "totalCount": 2,
     *          "totalPages": 1 
     *      }, 
     *      "status": [], 
     *      "datafiles": [] }, 
     *      "result": { 
     *          "data": [
     *              { 
     *                  "relation": "rdfs:label", 
     *                  "labels": { 
     *                              "en": "label" 
     *                           } 
     *               }, 
     *               { 
     *                  "relation": null, 
     *                  "labels": { 
     *                      "en": "comment" 
     *                  } 
     *              ] 
     *          } 
     *      }
     *   }
     */
    @GET
    @Path("rdfs/properties")
    @ApiOperation(value = "Get all rdfs properties that can be added to an instance",
            notes = "Retrieve all rdfs properties authorized")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all rdfs properties", response = Property.class, responseContainer = "List")
        ,
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION)
        ,
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED)
        ,
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRdfs(
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {

        VocabularyDAO vocabularyDAO = new VocabularyDAO();

        vocabularyDAO.user = userSession.getUser();
        vocabularyDAO.setPage(page);
        vocabularyDAO.setPageSize(pageSize);

        return getRdfsData(vocabularyDAO);
    }

    /**
     * Gets the list of contact properties that can be added to a given concept.
     * @param vocabularyDAO
     * @return the list of the contact properties, with their labels
     */
    private Response getContactProperties(VocabularyDAO vocabularyDAO) {
        ArrayList<PropertyVocabularyDTO> properties;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<PropertyVocabularyDTO> getResponse;

        properties = vocabularyDAO.allPaginateContactProperties();

        if (properties == null) {
            getResponse = new ResultForm<>(0, 0, properties, true);
            return noResultFound(getResponse, statusList);
        } else if (properties.isEmpty()) {
            getResponse = new ResultForm<>(0, 0, properties, true);
            return noResultFound(getResponse, statusList);
        } else {
            getResponse = new ResultForm<>(vocabularyDAO.getPageSize(), vocabularyDAO.getPage(), properties, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        }
    }

    /**
     * Gets the list of contact properties that can be associated to a given rdfType
     * @param rdfType
     * @param pageSize
     * @param page
     * @return the list of the properties, with their labels
     * @example
     * { 
     *   "metadata":
     *   { 
     *     "pagination": null, 
     *     "status": [], 
     *     "datafiles": [] 
     *   }, 
     *   "result": {
     *     "data": [ 
     *       { 
     *          "relation": "http://www.opensilex.org/vocabulary/oeso#hasTechnicalContact",
     *          "labels": { 
     *            "en": "technical contact", 
     *            "fr": "contact technique" 
     *          } 
     *       } 
     *     ] 
     *   }
     * }
     */
    @GET
    @Path("contacts/properties")
    @ApiOperation(value = "Get all contact properties that can be added to a given rdfType",
            notes = "Retrieve all contact properties authorized")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all contact properties", response = Property.class, responseContainer = "List")
        ,
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION)
        ,
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED)
        ,
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getContacts(
            @ApiParam(value = "Search by rdfType", example = DocumentationAnnotation.EXAMPLE_SENSOR_RDF_TYPE) @QueryParam("rdfType") @URL @Required String rdfType,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {

        VocabularyDAO vocabularyDAO = new VocabularyDAO();

        if (rdfType != null) {
            vocabularyDAO.domainRdfType = rdfType;
        }

        vocabularyDAO.user = userSession.getUser();
        vocabularyDAO.setPage(page);
        vocabularyDAO.setPageSize(pageSize);

        return getContactProperties(vocabularyDAO);
    }

    /**
     * Gets the list of device properties that can be added to a given concept.
     * @param vocabularyDAO
     * @return the list of the device properties, with their labels
     */
    private Response getDeviceProperties(VocabularyDAO vocabularyDAO) {
        ArrayList<PropertyVocabularyDTO> properties;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<PropertyVocabularyDTO> getResponse;

        properties = vocabularyDAO.allPaginateDeviceProperties();

        if (properties == null) {
            getResponse = new ResultForm<>(0, 0, properties, true);
            return noResultFound(getResponse, statusList);
        } else if (properties.isEmpty()) {
            getResponse = new ResultForm<>(0, 0, properties, true);
            return noResultFound(getResponse, statusList);
        } else {
            getResponse = new ResultForm<>(vocabularyDAO.getPageSize(), vocabularyDAO.getPage(), properties, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        }
    }

    /**
     * Gets the list of device properties that can be associated to a given rdfType.
     * @param rdfType
     * @param pageSize
     * @param page
     * @return the list of the properties, with their labels
     * @example
     * { "metadata":
     *  { 
     *  "pagination": 
     *      { 
     *          "pageSize": 20, 
     *          "currentPage": 0, 
     *          "totalCount": 5,
     *          "totalPages": 1 
     *      }, 
     *      "status": [], 
     *      "datafiles": [] 
     *  }, 
     *  "result": { 
     *    "data": [
     *       { 
     *           "relation": "http://www.opensilex.org/vocabulary/oeso#dateOfLastCalibration",
     *           "labels": { 
     *               "en": "date of last calibration", 
     *               "fr": "date de la dernière calibration" } 
     *       }, 
     *       { 
     *           "relation": "http://www.opensilex.org/vocabulary/oeso#dateOfPurchase",
     *           "labels": {
     *               "en": "date of purchase",
     *               "fr": "date d'achat" } 
     *       }, ... 
     *     ] 
     *   } 
     * }
     */
    @GET
    @Path("devices/properties")
    @ApiOperation(value = "Get all device properties that can be added to a given rdfType",
            notes = "Retrieve all device properties authorized")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all device properties", response = Property.class, responseContainer = "List")
        ,
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION)
        ,
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED)
        ,
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeviceProperties(
            @ApiParam(value = "Search by rdfType", example = DocumentationAnnotation.EXAMPLE_SENSOR_RDF_TYPE) @QueryParam("rdfType") @URL @Required String rdfType,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {
        VocabularyDAO vocabularyDAO = new VocabularyDAO();

        if (rdfType != null) {
            vocabularyDAO.domainRdfType = rdfType;
        }

        vocabularyDAO.user = userSession.getUser();
        vocabularyDAO.setPage(page);
        vocabularyDAO.setPageSize(pageSize);

        return getDeviceProperties(vocabularyDAO);
    }

    /**
     * Gets the list of triplestore namespaces.
     * @param pageSize
     * @param page
     * @return the list of the namespaces, with their prefix
     * @example
     * { 
     * "metadata":
     *  { 
     *   "pagination": { 
     *    "pageSize": 20, 
     *       "currentPage": 0, 
     *       "totalCount": 19,
     *       "totalPages": 1 
     *   }, 
     *   "status": [], 
     *   "datafiles": []
     *  }, 
     *  "result": { 
     *   "data": [
     *   { 
     *    "prefix": "oa", 
     *    "namespace": "http://www.w3.org/ns/oa#" 
     *   }, 
     *   { 
     *    "prefix": "owl",
     *    "namespace": "http://www.w3.org/2002/07/owl#" 
     *   }, 
     *   { 
     *    "prefix": "rdf",
     *    "namespace": "http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
     *   }
     *   ]
     *  }
     * }
     */
    @GET
    @Path("namespaces")
    @ApiOperation(value = "Get all triplestore namespaces",
            notes = "Retrieve all triplestore namespaces")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all contact properties", response = Property.class, responseContainer = "List")
        ,
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION)
        ,
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED)
        ,
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNamespaces(
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {

        VocabularyDAO vocabularyDAO = new VocabularyDAO();

        vocabularyDAO.user = userSession.getUser();
        vocabularyDAO.setPage(page);
        vocabularyDAO.setPageSize(pageSize);

        return getNamespaces(vocabularyDAO);
    }

    /**
     * Gets the list of namespaces.
     * @param vocabularyDAO
     * @return the list of namespaces
     */
    private Response getNamespaces(VocabularyDAO vocabularyDAO) {
        ArrayList<Namespace> namespaces;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Namespace> getResponse;

        namespaces = vocabularyDAO.allNamespacesProperties();

        if (namespaces == null) {
            getResponse = new ResultForm<> (0, 0, namespaces, true);
            return noResultFound(getResponse, statusList);
        } else if (namespaces.isEmpty()) {
            getResponse = new ResultForm<> (0, 0, namespaces, true);
            return noResultFound(getResponse, statusList);
        } else {
            getResponse = new ResultForm<> (vocabularyDAO.getPageSize(), vocabularyDAO.getPage(), namespaces, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        }
    }
}
