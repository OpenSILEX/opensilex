package org.opensilex.core.data.bll.dataImport;

import com.mongodb.client.result.DeleteResult;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.data.api.BatchHistoryGetDTO;
import org.opensilex.core.data.dal.batchHistory.BatchHistoryDao;
import org.opensilex.core.data.dal.batchHistory.BatchHistorySearchFilter;
import org.opensilex.core.data.dal.batchHistory.BatchHistoryModel;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.dao.MongoSearchQuery;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public class BatchHistoryLogic {

    private final MongoDBService nosql;

    public BatchHistoryLogic(MongoDBService nosql) {
        this.nosql = nosql;
    }

    /**
     *
     * @param batchHistoryUri
     * @return The batch history model associated with this uri
     * @throws NoSQLInvalidURIException if no batch history model found or invalid uri
     */
    public BatchHistoryModel get(URI batchHistoryUri) throws NoSQLInvalidURIException {
        BatchHistoryDao dao = new BatchHistoryDao(nosql.getServiceV2());
        return dao.get(batchHistoryUri);
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
