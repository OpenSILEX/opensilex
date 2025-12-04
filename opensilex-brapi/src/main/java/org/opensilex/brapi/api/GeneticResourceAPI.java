//******************************************************************************
//                          GeneticResourceAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.api;

import io.swagger.annotations.*;
import org.opensilex.brapi.model.BrAPIv1GeneticResourceDTO;
import org.opensilex.brapi.responses.BrAPIv1AccessionWarning;
import org.opensilex.brapi.responses.BrAPIv1GeneticResourceListResponse;
import org.opensilex.core.geneticResource.dal.GeneticResourceDAO;
import org.opensilex.core.geneticResource.dal.GeneticResourceModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.ontology.store.OntologyStore;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * The BrAPI geneticResource corresponds to an accession in OpenSILEX
 * @author Alice BOIZET
 */
@Api("BRAPI")
@Path("/brapi/")
public class GeneticResourceAPI extends BrapiCall {
    
    @Inject
    private SPARQLService sparql;
    
    @Inject
    private MongoDBService nosql;
    
    @CurrentUser
    AccountModel currentUser;
    
    @GET
    @Path("v1/geneticResource")
    @BrapiVersion("1.3")
    @ApiOperation("Submit a search request for geneticResource (type accession in OpenSILEX")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = BrAPIv1GeneticResourceListResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response getGeneticResourceBySearch(
            @ApiParam(value = "Search by geneticResourceDbId (URI of an OpenSilex accession)") @QueryParam("geneticResourceDbId") URI uri,
            @ApiParam(value = "Search by geneticResourcePUI (URI of an OpenSilex accession)") @QueryParam("geneticResourcePUI") URI geneticResourcePUI,
            @ApiParam(value = "Search by geneticResourceName (name of an OpenSilex accession)") @QueryParam("geneticResourceName") String geneticResourceName,
            @ApiParam(value = "Search by commonCropName (name of the species of an OpenSilex accession)") @QueryParam("commonCropName") String commonCropName,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        if (!ontologyStore.classExist(
                BrAPIv1AccessionWarning.ACCESSION_URI,
                BrAPIv1AccessionWarning.GENETIC_RESOURCE_URI
        )) {
            throw new BadRequestException(
                    "The 'Accession' notion doesn't exist in your ontology so this service is unavailable"
            );
        }
        GeneticResourceDAO geneticResourceDAO = new GeneticResourceDAO(sparql, nosql);

        if (geneticResourcePUI != null && uri == null) {
            uri = geneticResourcePUI;
        }

        ListWithPagination<GeneticResourceModel> resultList = geneticResourceDAO.brapiSearch(
                currentUser,
                uri,
                geneticResourceName,
                commonCropName,
                page,
                pageSize
        );
        if (resultList.getList().isEmpty()){
            throw new NotFoundURIException("No accession could be found with this URI ", uri);
        }

        // Convert paginated list to DTO
        ListWithPagination<BrAPIv1GeneticResourceDTO> resultDTOList = resultList.convert(
                BrAPIv1GeneticResourceDTO.class,
                BrAPIv1GeneticResourceDTO::fromModel
        );
        return new BrAPIv1GeneticResourceListResponse(resultDTOList).getResponse();
    }
    
}
