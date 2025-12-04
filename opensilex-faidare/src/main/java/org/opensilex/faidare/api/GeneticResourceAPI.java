//******************************************************************************
//                          GeneticResourceAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.api;

import io.swagger.annotations.*;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.faidare.builder.Faidarev1GeneticResourceDTOBuilder;
import org.opensilex.faidare.dal.Faidarev1GeneticResourceDAO;
import org.opensilex.faidare.model.Faidarev1GeneticResourceDTO;
import org.opensilex.faidare.model.Faidarev1GeneticResourceModel;
import org.opensilex.faidare.responses.Faidarev1GeneticResourceListResponse;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.StatusDTO;
import org.opensilex.server.response.StatusLevel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Objects;

/**
 * The faidare geneticResource corresponds to an accession in OpenSILEX
 * @author Gabriel Besombes
 */
@Api(CallsAPI.CREDENTIAL_CALLS_GROUP_ID)
@Path("/faidare/")
@ApiCredentialGroup(
        groupId = CallsAPI.CREDENTIAL_CALLS_GROUP_ID,
        groupLabelKey = CallsAPI.CREDENTIAL_CALLS_GROUP_LABEL_KEY
)
public class GeneticResourceAPI extends FaidareCall {
    
    @Inject
    private SPARQLService sparql;
    
    @Inject
    private MongoDBService nosql;

    @Inject
    MongoDBServiceV2 nosqlV2;
    
    @CurrentUser
    AccountModel currentUser;
    
    @GET
    @Path("v1/geneticResource")
    @FaidareVersion("1.3")
    @ApiOperation("Submit a search request for geneticResource")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = Faidarev1GeneticResourceListResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response getGeneticResourcesBySearch(
            @ApiParam(value = "Search by geneticResourceDbId") @QueryParam("geneticResourceDbId") URI uri,
            @ApiParam(value = "Search by geneticResourcePUI") @QueryParam("geneticResourcePUI") URI geneticResourcePUI,
            @ApiParam(value = "Search by geneticResourceName") @QueryParam("geneticResourceName") String geneticResourceName,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        if (Objects.isNull(ontologyDAO.getRdfType(new URI(Oeso.Accession.getURI()), "en"))){
            Faidarev1GeneticResourceListResponse response = new Faidarev1GeneticResourceListResponse(new ListWithPagination<>(new ArrayList<>()));
            response.setStatus(Response.Status.SERVICE_UNAVAILABLE);
            response.addMetadataStatus(
                    new StatusDTO(
                            "The accession notion doesn't exist in your instance so this service is unavailable",
                            StatusLevel.ERROR
                    )
            );
            return response.getResponse();
        }

        if (geneticResourcePUI != null && uri == null) {
            uri = geneticResourcePUI;
        }

        Faidarev1GeneticResourceDAO geneticResourceDAO = new Faidarev1GeneticResourceDAO(sparql, nosqlV2);

        ListWithPagination<Faidarev1GeneticResourceModel> resultList = geneticResourceDAO.faidareSearch(
                currentUser,
                uri,
                geneticResourceName,
                page,
                pageSize,
                nosql
        );

        Faidarev1GeneticResourceDTOBuilder geneticResourceDTOBuilder = new Faidarev1GeneticResourceDTOBuilder();

        // Convert paginated list to DTO
        ListWithPagination<Faidarev1GeneticResourceDTO> resultDTOList = resultList.convert(
                Faidarev1GeneticResourceDTO.class,
                geneticResourceModel -> {
                    try {
                        return geneticResourceDTOBuilder.fromModel(geneticResourceModel);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        return new Faidarev1GeneticResourceListResponse(resultDTOList).getResponse();
    }
    
}
