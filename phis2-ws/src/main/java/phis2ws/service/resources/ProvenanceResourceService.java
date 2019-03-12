//******************************************************************************
//                                       ProvenanceResourceService.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 4 mars 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
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
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.mongo.ProvenanceDAOMongo;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.provenance.ProvenanceDTO;
import phis2ws.service.resources.dto.provenance.ProvenancePostDTO;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.brapi.form.ResponseFormProvenance;
import phis2ws.service.view.model.phis.provenance.Provenance;

/**
 * Provenance resource service
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/provenances")
@Path("/provenances")
public class ProvenanceResourceService extends ResourceService {
    
    /**
     * Generates a Provenance list from a given list of ProvenancePostDTO
     * @param provenanceDTOs
     * @return the list of provenances
     */
    private List<Provenance> provenancePostDTOsToprovenances(List<ProvenancePostDTO> provenanceDTOs) {
        ArrayList<Provenance> provenances = new ArrayList<>();
        
        for (ProvenancePostDTO provenancePostDTO : provenanceDTOs) {
            provenances.add(provenancePostDTO.createObjectFromDTO());
        }
        
        return provenances;
    }
    /**
     * Generates a Provenance list from a given list of ProvenanceDTO
     * @param provenanceDTOs
     * @return the list of provenances
     */
    private List<Provenance> provenanceDTOsToprovenances(List<ProvenanceDTO> provenanceDTOs) {
        ArrayList<Provenance> provenances = new ArrayList<>();
        
        for (ProvenanceDTO provenanceDTO : provenanceDTOs) {
            provenances.add(provenanceDTO.createObjectFromDTO());
        }
        
        return provenances;
    }
    
    /**
     * Insert provenances.
     * @param provenances
     * @param context
     * @example 
     * [
     *  {
     *      "label": "PROV2019-LEAF",
     *      "comment": "In this provenance we have count the number of leaf per plant",
     *      "metadata": { 
     *          "SensingDevice" : "http://www.opensilex.org/demo/s001",
     *          "Vector" : "http://www.opensilex.org/demo/v001"
     *      }
     *  }
     * ]
     * @return the insertion result
     */
    @POST
    @ApiOperation(value = "Post provenance(s)",
                  notes = "Register provenance(s) in the database")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "provenance(s) saved", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(
        @ApiParam (value = DocumentationAnnotation.PROVENACE_POST_DEFINITION) @Valid ArrayList<ProvenancePostDTO> provenances,
        @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        if (provenances != null && !provenances.isEmpty()) {
            ProvenanceDAOMongo provenanceDAO = new ProvenanceDAOMongo();
            
            provenanceDAO.user = userSession.getUser();
            
            POSTResultsReturn result = provenanceDAO.checkAndInsert(provenancePostDTOsToprovenances(provenances));
            
            if (result.getHttpStatus().equals(Response.Status.CREATED)) {
                postResponse = new ResponseFormPOST(result.statusList);
                postResponse.getMetadata().setDatafiles(result.getCreatedResources());
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty provenances(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Update the given provenances.
     * @param provenances
     * @param context
     * @example
     * [
     *  {
     *      "uri": "http://www.opensilex.org/opensilex/id/provenance/1551805521606",
     *      "label": "PROV2019-EAR",
     *      "comment": "In this provenance we have count the number of leaf per plant",
     *      "metadata": { 
     *                      "SensingDevice" : "http://www.opensilex.org/demo/s001",
     *                      "Vector" : "http://www.opensilex.org/demo/v001"
     *                  }
     *  }
     * ]
     * @return the query result
     */
    @PUT
    @ApiOperation(value = "Update provenance")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Update provenance(s) updated", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 404, message = "Update provenance(s) not found"),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(
        @ApiParam(value = DocumentationAnnotation.PROVENACE_POST_DEFINITION) @Valid ArrayList<ProvenanceDTO> provenances,
        @Context HttpServletRequest context) {
        AbstractResultForm putResponse = null;

        ProvenanceDAOMongo provenanceDAO = new ProvenanceDAOMongo();

        provenanceDAO.user = userSession.getUser();

        POSTResultsReturn result = provenanceDAO.checkAndUpdate(provenanceDTOsToprovenances(provenances));

        if (result.getHttpStatus().equals(Response.Status.OK)
                || result.getHttpStatus().equals(Response.Status.CREATED)) {
            putResponse = new ResponseFormPOST(result.statusList);
            putResponse.getMetadata().setDatafiles(result.createdResources);
        } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                || result.getHttpStatus().equals(Response.Status.OK)
                || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
            putResponse = new ResponseFormPOST(result.statusList);
        }
        return Response.status(result.getHttpStatus()).entity(putResponse).build();
    }
    
    /**
     * Service to get provenances
     * @param pageSize
     * @param page
     * @param uri
     * @param label
     * @param comment
     * @param jsonValueFilter
     * @return list of the provenances corresponding to the search params given
     * @example
     * {
     *      "metadata": {
     *          "pagination": null,
     *          "status": [],
     *          "datafiles": []
     *      },
     *      "result": {
     *          "data": [
     *              {
     *                  "uri": "http://www.opensilex.org/opensilex/id/provenance/1551805521606",
     *                  "label": "PROV2019-LEAF",
     *                  "comment": "In this provenance we have count the number of leaf per plant",
     *                  "metadata": {
     *                      "SensingDevice": "http://www.opensilex.org/demo/s001",
     *                      "Vector": "http://www.opensilex.org/demo/v001"
     *                  }
     *              }
     *          ]
     *      }
     * }
     */
    @GET
    @ApiOperation(value = "Get all provenances corresponding to the search params given",
                  notes = "Retrieve all provenances authorized for the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all provenances", response = ProvenanceDTO.class, responseContainer = "List"),
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
    public Response getProvenances(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
        @ApiParam(value = "Search by provenance uri", example = DocumentationAnnotation.EXAMPLE_PROVENANCE_URI) @QueryParam("uri") @URL String uri,
        @ApiParam(value = "Search by provenance label", example = DocumentationAnnotation.EXAMPLE_PROVENANCE_LABEL) @QueryParam("label") String label,
        @ApiParam(value = "Search by comment", example = DocumentationAnnotation.EXAMPLE_PROVENANCE_COMMENT) @QueryParam("comment") String comment,
        @ApiParam(value = "Search by json filter", example = DocumentationAnnotation.EXAMPLE_PROVENANCE_METADATA) @QueryParam("jsonValueFilter") String jsonValueFilter) {

        ProvenanceDAOMongo provenanceDAO = new ProvenanceDAOMongo();
        
        Provenance searchProvenance = new Provenance();
        searchProvenance.setUri(uri);
        searchProvenance.setLabel(label);
        searchProvenance.setComment(comment);
        
        provenanceDAO.user = userSession.getUser();
        provenanceDAO.setPage(page);
        provenanceDAO.setPageSize(pageSize);
        
        // 2. Get provenances count
        int totalCount = provenanceDAO.count(searchProvenance, jsonValueFilter);
        
        // 3. Get environment measures page list
        ArrayList<Provenance> measures = provenanceDAO.getProvenances(searchProvenance, jsonValueFilter);
        
        // 4. Initialize returned provenances
        ArrayList<ProvenanceDTO> list = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormProvenance getResponse;
        
        if (measures == null) {
            // Request failure
            getResponse = new ResponseFormProvenance(0, 0, list, true, 0);
            return noResultFound(getResponse, statusList);
        } else if (measures.isEmpty()) {
            // No results
            getResponse = new ResponseFormProvenance(0, 0, list, true, 0);
            return noResultFound(getResponse, statusList);
        } else {
            // Convert all provenances object to DTO's
            measures.forEach((provenance) -> {
                list.add(new ProvenanceDTO(provenance));
            });
            
            // Return list of DTO
            getResponse = new ResponseFormProvenance(provenanceDAO.getPageSize(), provenanceDAO.getPage(), list, true, totalCount);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }
}
