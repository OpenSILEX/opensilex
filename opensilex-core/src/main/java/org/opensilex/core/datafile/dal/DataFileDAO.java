package org.opensilex.core.datafile.dal;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URISyntaxException;
import java.util.List;

public class DataFileDAO extends MongoReadWriteDao<DataFileModel, DataFileSearchFilter> {

    public static final String FILE_COLLECTION_NAME = "file";
    public static final String FS_FILE_PREFIX = "datafile";

    DataDAO dataDAO;

    public DataFileDAO(MongoDBService mongodb, SPARQLService sparql, FileStorageService fs) throws URISyntaxException {
        super(mongodb, DataFileModel.class, FILE_COLLECTION_NAME, FS_FILE_PREFIX);
        dataDAO = new DataDAO(mongodb, sparql, fs);
    }

    @Override
    protected void createIndexes() {

        IndexOptions unicityOptions = new IndexOptions().unique(true);

        collection.createIndex(Indexes.ascending(MongoModel.URI_FIELD), unicityOptions);
        collection.createIndex(Indexes.ascending(DataFileModel.PATH_FIELD), unicityOptions);
        collection.createIndex(Indexes.ascending(DataModel.PROVENANCE_FIELD, DataModel.TARGET_FIELD, DataModel.DATE_FIELD));
        collection.createIndex(Indexes.ascending(DataModel.TARGET_FIELD, DataModel.DATE_FIELD));
        collection.createIndex(Indexes.descending(DataModel.DATE_FIELD));
    }

    @Override
    public List<Bson> getBsonFilters(DataFileSearchFilter filter) {

        // Delegate data filters on the DataDao
        // Then add DataFile specific filters
        List<Bson> filters = dataDAO.getBsonFilters(filter);

        if (!StringUtils.isEmpty(filter.getPath())) {
            filters.add(Filters.eq(DataFileModel.PATH_FIELD, filter.getPath()));
        }
        if (!StringUtils.isEmpty(filter.getFilename())) {
            filters.add(Filters.eq(DataFileModel.FILENAME_FIELD, filter.getFilename()));
        }
        if (filter.getArchive() != null) {
            filters.add(Filters.eq(DataFileModel.ARCHIVE_FIELD, filter.getArchive()));
        }
        return filters;
    }
}
