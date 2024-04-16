package org.opensilex.olga;

import org.brapi.client.v2.ApiResponse;
import org.brapi.v2.model.BrAPIPagination;
import org.brapi.v2.model.BrAPIResponse;
import org.brapi.v2.model.BrAPIStatus;
import org.brapi.v2.model.germ.BrAPIGermplasm;
import org.opensilex.server.response.*;

import java.util.List;

public class BrAPIResponseSerialiser {

    public static <T> SingleObjectResponse<T> singleObjectResponseSerialiser(
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
