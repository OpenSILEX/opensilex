package org.opensilex.core.dataV2.service;

import com.mongodb.client.result.DeleteResult;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.dataV2.api.dto.BatchHistoryGetDTO;
import org.opensilex.core.dataV2.dao.BatchHistoryDao;
import org.opensilex.core.dataV2.dao.BatchHistorySearchFilter;
import org.opensilex.core.dataV2.model.BatchHistoryModel;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.dao.MongoSearchQuery;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public class BatchHistoryService {
    private final AccountModel user;
    private final MongoDBService nosql;

    public BatchHistoryService(AccountModel user, MongoDBService nosql) {
        this.user = user;
        this.nosql = nosql;
    }


    /**
     * Retrieves the batch history items with pagination.
     *
     * @param startDate   the start date for filtering batch history
     * @param endDate     the end date for filtering batch history
     * @param orderByList the list of fields to sort the results by
     * @param page        the page number to retrieve
     * @param pageSize    the number of results per page
     * @return a list of batch history items with pagination
     */
    public ListWithPagination<BatchHistoryGetDTO> getBatchHistoryWithPagination(String startDate, String endDate, List<OrderBy> orderByList, int page, int pageSize) {
        BatchHistoryDao dao = new BatchHistoryDao(nosql.getServiceV2());

        Instant startInstant = StringUtils.isNotBlank(startDate) ? Instant.parse(startDate) : null;
        Instant endInstant = StringUtils.isNotBlank(endDate) ? Instant.parse(endDate) : Instant.now();

        BatchHistorySearchFilter filter = (BatchHistorySearchFilter) new BatchHistorySearchFilter()
                .setUserName(user.getName())
                .setStartDate(startInstant)
                .setEndDate(endInstant)
                .setPage(page)
                .setPageSize(pageSize)
                .setOrderByList(orderByList);

        return dao.searchWithPagination(
                new MongoSearchQuery<BatchHistoryModel, BatchHistorySearchFilter, BatchHistoryGetDTO>()
                        .setFilter(filter)
                        .setConvertFunction(BatchHistoryGetDTO::fromModel)
        );
    }

    /**
     * Deletes a batch history document from the database based on its URI.
     *
     * @param batchHistoryURI The URI of the batch history to delete
     * @return DeleteResult containing information about the deletion operation
     * @throws NoSQLInvalidURIException if the provided URI is invalid or not found in the database
     * @see DeleteResult for details about the deletion operation results
     */
    public DeleteResult deleteBatchHistoryByURI(URI batchHistoryURI) throws NoSQLInvalidURIException {
        BatchHistoryDao dao = new BatchHistoryDao(nosql.getServiceV2());
        return dao.delete(batchHistoryURI);
    }
}
