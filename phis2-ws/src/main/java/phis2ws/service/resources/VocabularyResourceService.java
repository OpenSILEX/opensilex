//******************************************************************************
//                                       VocabularyResourceService.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 18 juin 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  18 juin 2018
// Subject: represents the vocabulary service
//******************************************************************************
package phis2ws.service.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.sesame.VocabularyDAOSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.resources.dto.PropertyVocabularyDTO;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormVocabularyProperty;
import phis2ws.service.view.model.phis.Property;

/**
 * vocabulary service. Used to generate client side menues and forms, dynamically
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/vocabularies")
@Path("/vocabularies")
public class VocabularyResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(VocabularyResourceService.class);
    
    //user session
    @SessionInject
    Session userSession;
    
    /**
     * 
     * @param getResponse
     * @param insertStatusList
     * @return the response "no result found" for the service
     */
    private Response noResultFound(ResponseFormVocabularyProperty getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status(StatusCodeMsg.NO_RESULTS, StatusCodeMsg.INFO, "No results for the vocabulary"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
    
    /**
     * get the list of rdfs properties that can be used, for the clients interfaces
     * @param vocabularyDAO
     * @return the list of the rdfs properties, with their labels
     */
    private Response getRdfsData(VocabularyDAOSesame vocabularyDAO) {
        ArrayList<PropertyVocabularyDTO> properties;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormVocabularyProperty getResponse;
        
        properties = vocabularyDAO.allPaginateRdfsProperties();
        
        if (properties == null) {
            getResponse = new ResponseFormVocabularyProperty(0, 0, properties, true);
            return noResultFound(getResponse, statusList);
        } else if (properties.isEmpty()) {
            getResponse = new ResponseFormVocabularyProperty(0, 0, properties, true);
            return noResultFound(getResponse, statusList);
        } else {
            getResponse = new ResponseFormVocabularyProperty(vocabularyDAO.getPageSize(), vocabularyDAO.getPage(), properties, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        }
    }
    
    /**
     * get the list of rdfs properties that can be used, for the clients interfaces
     * @param pageSize
     * @param page
     * @return list of the rdfs properties, with their labels
     * e.g.
     * {
     *      "metadata": {
     *          "pagination": {
     *              "pageSize": 20,
     *              "currentPage": 0,
     *              "totalCount": 2,
     *              "totalPages": 1
     *          },
     *          "status": [],
     *          "datafiles": []
     *      },
     *      "result": {
     *          "data": [
     *              {
     *                  "relation": "rdfs:label",
     *                  "labels": {
     *                      "en": "label"
     *                   }
     *              },
     *              {
     *                  "relation": null,
     *                  "labels": {
     *                      "en": "comment"
     *                   }
     *              }
     *          ]
     *      }
     * }
     */
    @GET
    @Path("rdfs/properties")
    @ApiOperation(value = "Get all rdfs properties that can be added to an instance",
                  notes = "Retrieve all rdfs properties authorized")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all rdfs properties", response = Property.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
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
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page ) {
        
        VocabularyDAOSesame vocabularyDAO = new VocabularyDAOSesame();
        
        vocabularyDAO.user = userSession.getUser();
        vocabularyDAO.setPage(page);
        vocabularyDAO.setPageSize(pageSize);
        
        return getRdfsData(vocabularyDAO);
    }
    
    /**
     * get the list of contact properties that can be added to a given concept
     * @param vocabularyDAO
     * @return the list of the contact properties, with their labels
     */
    private Response getContactProperties(VocabularyDAOSesame vocabularyDAO) {
        ArrayList<PropertyVocabularyDTO> properties;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormVocabularyProperty getResponse;
        
        properties = vocabularyDAO.allPaginateContactProperties();
        
        if (properties == null) {
            getResponse = new ResponseFormVocabularyProperty(0, 0, properties, true);
            return noResultFound(getResponse, statusList);
        } else if (properties.isEmpty()) {
            getResponse = new ResponseFormVocabularyProperty(0, 0, properties, true);
            return noResultFound(getResponse, statusList);
        } else {
            getResponse = new ResponseFormVocabularyProperty(vocabularyDAO.getPageSize(), vocabularyDAO.getPage(), properties, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        }
    }
    
    /**
     * get the list of contact properties that can be associated to a given rdfType
     * @param rdfType
     * @param pageSize
     * @param page
     * @return the list of the properties, with their labels
     * e.g.
     * {
     *      "metadata": {
     *          "pagination": null,
     *          "status": [],
     *          "datafiles": []
     *      },
     *      "result": {
     *          "data": [
     *              {
     *                  "relation": "http://www.phenome-fppn.fr/vocabulary/2017#hasTechnicalContact",
     *                  "labels": {
     *                      "en": "technical contact",
     *                      "fr": "contact technique"
     *                  }
     *              }
     *          ]
     *      }
     * }
     */
    @GET
    @Path("contacts/properties")
    @ApiOperation(value = "Get all contact properties that can be added to a given rdfType",
                  notes = "Retrieve all contact properties authorized")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all contact properties", response = Property.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
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
            @ApiParam(value = "Search by rdfType", example = DocumentationAnnotation.EXAMPLE_SENSOR_RDF_TYPE) @QueryParam("rdfType") String rdfType,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page) {
        
        VocabularyDAOSesame vocabularyDAO = new VocabularyDAOSesame();
        
        if (rdfType != null) {
            vocabularyDAO.domainRdfType = rdfType;
        }
        
        vocabularyDAO.user = userSession.getUser();
        vocabularyDAO.setPage(page);
        vocabularyDAO.setPageSize(pageSize);
        
        return getContactProperties(vocabularyDAO);
    }
}
