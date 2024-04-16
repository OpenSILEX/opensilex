package org.opensilex.olga.bll;

import org.brapi.v2.model.BrAPIPagination;
import org.brapi.v2.model.BrAPIStatus;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.olga.dal.GermplasmDAO;
import org.opensilex.olga.dal.GermplasmModel;
import org.opensilex.olga.model.GermplasmDTO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.*;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class GermplasmLogic {

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;


    public static void updateGermplasms(List<GermplasmDTO> germplasmDTOs) {
        List<GermplasmModel> germplasmModels = germplasmDTOs.stream().map(GermplasmModel::fromDTO).collect(Collectors.toList());

        GermplasmDAO germplasmDAO = new GermplasmDAO();

        germplasmDAO.updateGermplasms(germplasmModels);
    }

    public static <T> SingleObjectResponse<T> brapiResponseToSingleObjectResponse(
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
}
