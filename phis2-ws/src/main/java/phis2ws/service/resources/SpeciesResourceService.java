//******************************************************************************
//                                       SpeciesResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 7 déc. 2018
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
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.sesame.SpeciesDAOSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.species.SpeciesDTO;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Species;

/**
 * Species resource service.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/species")
@Path("/species")
public class SpeciesResourceService extends ResourceService {
    
    /**
     * Generates a speciesDTO list from a given list of species.
     * @param species
     * @return the list of species dto
     */
    private ArrayList<SpeciesDTO> speciesToSpeciesDTO(ArrayList<Species> species) {
        ArrayList<SpeciesDTO> speciesDTO = new ArrayList<>();
        
        species.forEach((specie) -> {
            speciesDTO.add(new SpeciesDTO(specie));
        });
        
        return speciesDTO;
    }
    
    /**
     * Get the species corresponding to the search params given.
     * @param pageSize
     * @param page
     * @param uri
     * @param label
     * @param language
     * @return the result
     * @example 
     * {
     *      "metadata": {
     *          "pagination": {
     *              "pageSize": 2,
     *              "currentPage": 0,
     *              "totalCount": 18,
     *              "totalPages": 9
     *          },
     *          "status": [],
     *          "datafiles": []
     *      },
     *      "result": {
     *          "data": [
     *              {
     *                  "uri": "http://www.phenome-fppn.fr/id/species/zeamays",
     *                  "label": "Maize"
     *              },
     *              {
     *                  "uri": "http://www.phenome-fppn.fr/id/species/gossypiumhirsutum",
     *                  "label": "Upland cotton"
     *              }
     *          ]
     *      }
     * }
     */
    @GET
    @ApiOperation(value = "Get all species corresponding to the search params given",
                  notes = "Retrieve all species authorized for the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all species", response = SpeciesDTO.class, responseContainer = "List"),
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
    public Response get(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
        @ApiParam(value = "Search by species uri", example = DocumentationAnnotation.EXAMPLE_SPECIES_URI) @QueryParam("uri") @URL String uri,
        @ApiParam(value = "Search by species label", example = DocumentationAnnotation.EXAMPLE_SPECIES_LABEL) @QueryParam("label") String label,
        @ApiParam(value = "Select language", example = DocumentationAnnotation.EXAMPLE_LANGUAGE) @QueryParam("language") String language) {
        //1. Initialize species filter
        Species filter = new Species();
        filter.setUri(uri);
        filter.setLabel(label);
        
        if (language == null) {
            language = DEFAULT_LANGUAGE;
        }
        
        SpeciesDAOSesame speciesDAO = new SpeciesDAOSesame();
        speciesDAO.setPage(page);
        speciesDAO.setPageSize(pageSize);
        
        //2. Get number of species result
        int totalCount = speciesDAO.countWithFilter(filter, language);
        
        //3. Get species result
        ArrayList<Species> searchResult = speciesDAO.searchWithFilter(filter, language);
        
        //4. Send result
        ResultForm<SpeciesDTO> getResponse;
        ArrayList<Status> statusList = new ArrayList<>();
        ArrayList<SpeciesDTO> speciesToReturn = speciesToSpeciesDTO(searchResult);
        
        if (searchResult == null || speciesToReturn.isEmpty()) {
            //No result found
            getResponse = new ResultForm<SpeciesDTO>(0, 0, speciesToReturn, true, 0);
            return noResultFound(getResponse, statusList);
        } else {
            //Return the result list
            getResponse = new ResultForm<SpeciesDTO>(pageSize, page, speciesToReturn, true, totalCount);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }
}
