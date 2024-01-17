//******************************************************************************
//                          GermplasmAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.api;

import io.swagger.annotations.*;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.faidare.builder.Faidarev1GermplasmDTOBuilder;
import org.opensilex.faidare.model.Faidarev1GermplasmDTO;
import org.opensilex.faidare.responses.Faidarev1GermplasmListResponse;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * The faidare germplasm corresponds to an accession in OpenSILEX
 * @author Gabriel Besombes
 */
@Api("faidare")
@Path("/faidare/")
public class GermplasmAPI extends FaidareCall {
    
    @Inject
    private SPARQLService sparql;
    
    @Inject
    private MongoDBService nosql;
    
    @CurrentUser
    AccountModel currentUser;
    
    @GET
    @Path("v1/germplasm")
    @FaidareVersion("1.3")
    @ApiOperation("Submit a search request for germplasm")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = Faidarev1GermplasmListResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response getGermplasmBySearch(
            @ApiParam(value = "Search by germplasmDbId") @QueryParam("germplasmDbId") URI uri,
            @ApiParam(value = "Search by germplasmPUI") @QueryParam("germplasmPUI") URI germplasmPUI,
            @ApiParam(value = "Search by germplasmName") @QueryParam("germplasmName") String germplasmName,
            @ApiParam(value = "Search by commonCropName") @QueryParam("commonCropName") String commonCropName,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);

        if (germplasmPUI != null && uri == null) {
            uri = germplasmPUI;
        }

        Faidarev1GermplasmDTOBuilder germplasmDTOBuilder = new Faidarev1GermplasmDTOBuilder(germplasmDAO);
        if(uri != null) {
            return new SingleObjectResponse<>(
                    germplasmDTOBuilder.fromModel(
                            germplasmDAO.get(uri, currentUser, false),
                            currentUser
                    )
            ).getResponse();
        }

        ListWithPagination<GermplasmModel> resultList = germplasmDAO.brapiSearch(
                currentUser,
                null,
                germplasmName,
                commonCropName,
                page,
                pageSize
        );
        
        // Convert paginated list to DTO
        ListWithPagination<Faidarev1GermplasmDTO> resultDTOList = resultList.convert(
                Faidarev1GermplasmDTO.class,
                germplasmModel -> {
                    try {
                        return germplasmDTOBuilder.fromModel(germplasmModel, currentUser);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        return new Faidarev1GermplasmListResponse(resultDTOList).getResponse();
    }
    
}
