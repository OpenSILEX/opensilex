/*
 * *****************************************************************************
 *                         GermplasmAPI.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 28/05/2024 16:21
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

package org.opensilex.olga.api;

import io.swagger.annotations.*;
import org.brapi.v2.model.BrAPIPagination;
import org.brapi.v2.model.BrAPIStatus;
import org.brapi.v2.model.germ.BrAPIGermplasm;
import org.opensilex.core.germplasm.api.GermplasmGetSingleDTO;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.olga.OlgaModule;
import org.opensilex.olga.bll.GermplasmLogic;
import org.opensilex.olga.model.GermplasmDTO;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.response.*;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Gabriel Besombes
 */
@Api(GermplasmAPI.CREDENTIAL_OLGA_GERMPLASM_GROUP_ID)
@Path(GermplasmAPI.PATH)
@ApiCredentialGroup(
        groupId = GermplasmAPI.CREDENTIAL_OLGA_GERMPLASM_GROUP_ID,
        groupLabelKey = GermplasmAPI.CREDENTIAL_OLGA_GERMPLASM_GROUP_LABEL_KEY
)
public class GermplasmAPI {

    public static final String PATH = "/olga/germplasm";
    public static final String CREDENTIAL_OLGA_GERMPLASM_GROUP_ID = "Olga Germplasm";
    public static final String CREDENTIAL_OLGA_GERMPLASM_GROUP_LABEL_KEY = "credential-groups.olga-germplasm";

    public static final String GERMPLASM_EXAMPLE_DBID = "germplasm1";
    public static final String GERMPLASM_DBID = GERMPLASM_EXAMPLE_DBID;

    public static final String GERMPLASM_EXAMPLE_NAME = "germplasm1Name";
    public static final String GERMPLASM_NAME = GERMPLASM_EXAMPLE_DBID;

    @Inject
    private OlgaModule olgaModule;

    @Inject
    private MongoDBService mongodb;


    // Service to get germplasm detail from Olga
    @GET
    @Path("{germplasmDbId}")
    @ApiOperation("Get olga germplasm by germplasmDbId")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return details of an olga germplasm", response = BrAPIGermplasm.class)
    })
    public Response getGermplasmByDbId(
            @ApiParam(value = GERMPLASM_DBID, example = GERMPLASM_EXAMPLE_DBID, required = true) @PathParam("germplasmDbId") @NotNull String germplasmDbId
    ) throws Exception {
        GermplasmLogic germplasmLogic = new GermplasmLogic(mongodb.getServiceV2(), olgaModule);

        var germplasmDetailResult = germplasmLogic.getOpensilexGermplasmDetail(germplasmDbId);

        return brapiResponseToSingleObjectResponse(
                GermplasmGetSingleDTO.fromModel(germplasmDetailResult.getGermplasmModel()),
                germplasmDetailResult.getResponsePagination(),
                germplasmDetailResult.getResponseStatus()
        ).getResponse();
    }

    // Service to search germplasms in local database
    @GET
    @Path("")
    @ApiOperation("Search germplasms")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return matching germplasms", response = GermplasmDTO.class, responseContainer = "List")
    })
    public Response searchGermplasms(
            @ApiParam(value = GERMPLASM_NAME, example = GERMPLASM_EXAMPLE_NAME) String germplasmName
    ) {
        GermplasmLogic germplasmLogic = new GermplasmLogic(mongodb.getServiceV2(), olgaModule);
        return new PaginatedListResponse<>(germplasmLogic.searchGermplasm(germplasmName)).getResponse();
    }

    // Service to force germplasm harvest for admins
    @GET
    @Path("forceHarvest")
    @ApiOperation("Force harvest of olga germplasms")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Harvest olga germplasms")
    })
    public void forceHarvestGermplasms() throws Exception {
        GermplasmLogic germplasmLogic = new GermplasmLogic(mongodb.getServiceV2(), olgaModule);
        germplasmLogic.harvestOlgaGermplasms();
    }

    private <T> SingleObjectResponse<T> brapiResponseToSingleObjectResponse(
            T responseObject, BrAPIPagination responsePagination, List<BrAPIStatus> responseStatus
    ) {

        PaginationDTO pagination = new PaginationDTO(
                responsePagination.getPageSize(), responsePagination.getCurrentPage(), responsePagination.getTotalCount(), responsePagination.getTotalPages()
        );
        MetadataDTO metadata = new MetadataDTO(pagination);

        for (BrAPIStatus status: responseStatus) {
            StatusLevel statusLevel = null;
            BrAPIStatus.MessageTypeEnum messageType = status.getMessageType();
            switch (messageType) {
                case DEBUG:
                    statusLevel = StatusLevel.DEBUG;
                    break;
                case ERROR:
                    statusLevel = StatusLevel.ERROR;
                    break;
                case WARNING:
                    statusLevel = StatusLevel.WARNING;
                    break;
                case INFO:
                    statusLevel = StatusLevel.INFO;
                    break;
            }
            metadata.addStatus(new StatusDTO(status.getMessage(), statusLevel));
        }

        SingleObjectResponse<T> response = new SingleObjectResponse<>(responseObject);
        response.setMetadata(metadata);
        return response;
    }

    // Mechanism to automate germplasm harvest every night. Should it be in BLL rather than API?
}
