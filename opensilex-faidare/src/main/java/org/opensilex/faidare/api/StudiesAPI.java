/*
 * *****************************************************************************
 *                         StudiesAPI.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 25/05/2024 00:00
 * Contact: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
 * *****************************************************************************
 */
package org.opensilex.faidare.api;

import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.brapi.responses.BrAPIv1AccessionWarning;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.dal.ExperimentSearchFilter;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.faidare.builder.Faidarev1StudyDTOBuilder;
import org.opensilex.faidare.model.Faidarev1StudyDTO;
import org.opensilex.faidare.responses.Faidarev1StudyListResponse;
import org.opensilex.front.FrontModule;
import org.opensilex.front.api.RouteDTO;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.ServerModule;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Gabriel Besombes
 */
@Api(CallsAPI.CREDENTIAL_CALLS_GROUP_ID)
@Path("/faidare/")
@ApiCredentialGroup(
        groupId = CallsAPI.CREDENTIAL_CALLS_GROUP_ID,
        groupLabelKey = CallsAPI.CREDENTIAL_CALLS_GROUP_LABEL_KEY
)
public class StudiesAPI extends FaidareCall {

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;

    @Inject
    private FrontModule frontModule;

    @Inject
    private ServerModule serverModule;

    @CurrentUser
    AccountModel currentUser;


    @GET
    @Path("v1/studies")
    @FaidareVersion("1.3")
    @ApiOperation(value = "Retrieve studies information", notes = "Retrieve studies information")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve studies information", response = Faidarev1StudyListResponse.class)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)

    public Response getStudiesList(
            @ApiParam(value = "Search by studyDbId") @QueryParam("studyDbId") URI studyDbId,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        ExperimentDAO xpDao = new ExperimentDAO(sparql, nosql);

        String experimentPathExtention = "";
        List<RouteDTO> routeDTOS = frontModule.getConfigDTO(currentUser, sparql).getRoutes();
        for (RouteDTO routeDTO : routeDTOS) {
            if (SPARQLDeserializers.compareURIs(routeDTO.getRdfType(), Oeso.Experiment.toString())) {
                experimentPathExtention = routeDTO.getPath().substring(1);
                break;
            }
        }
        String appUrl = serverModule.getAppUrl();
        DataDAO dataDAO = new DataDAO(nosql, sparql, fs);
        Faidarev1StudyDTOBuilder studyDTOBuilder = new Faidarev1StudyDTOBuilder(
                dataDAO,
                currentUser,
                sparql,
                appUrl + experimentPathExtention
        );

        ListWithPagination<ExperimentModel> resultList;
        if (studyDbId != null) {
            ExperimentModel model = xpDao.get(studyDbId, currentUser);
            if (Objects.isNull(model)) {
                throw new NotFoundURIException(studyDbId);
            } else {
                resultList = new ListWithPagination<>(Collections.singletonList(model), 0, 0, 1);
            }
        } else {
            ExperimentSearchFilter filter = new ExperimentSearchFilter()
                    .setUser(currentUser);
            filter.setPage(page)
                    .setPageSize(pageSize);

            resultList = xpDao.search(filter, true, true, true);
        }

        ListWithPagination<Faidarev1StudyDTO> resultDTOList = resultList.convert(
                Faidarev1StudyDTO.class,
                experimentModel -> {
                    try {
                        return studyDTOBuilder.fromModel(experimentModel);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        Faidarev1StudyListResponse response = new Faidarev1StudyListResponse(resultDTOList);
        BrAPIv1AccessionWarning.setAccessionWarningIfNeeded(response);
        return response.getResponse();
    }
}
