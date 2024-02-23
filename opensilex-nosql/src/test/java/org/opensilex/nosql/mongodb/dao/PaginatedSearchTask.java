package org.opensilex.nosql.mongodb.dao;

import com.mongodb.client.ClientSession;
import org.opensilex.nosql.mongodb.MongoModel;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

final class PaginatedSearchTask<T extends MongoModel,F extends MongoSearchFilter> implements Callable<Integer> {

    private final MongoReadWriteDao<T,F> dao;
    private final T[] collectedModels;

    private final F searchFilter;
    private final ClientSession session;

    public PaginatedSearchTask(MongoReadWriteDao<T, F> dao, T[] collectedModels, F searchFilter, ClientSession session) {
        this.dao = dao;
        this.collectedModels = collectedModels;
        this.searchFilter = searchFilter;
        this.session = session;
    }

    public Integer call(){

        // write results from database inside collectedModels. Use page and pageSize to determine offset
        int startIndex = searchFilter.getPage()* searchFilter.getPageSize();
        AtomicInteger arrayIndex = new AtomicInteger(startIndex);
        dao.searchAsStream(session, searchFilter, null).getSource().forEach(model ->
                collectedModels[arrayIndex.getAndIncrement()] = model
        );

        return arrayIndex.get() - startIndex;
    }
}