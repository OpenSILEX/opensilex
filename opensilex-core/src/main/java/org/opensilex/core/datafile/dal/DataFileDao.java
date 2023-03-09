//******************************************************************************
//                          DataFileDao.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.datafile.dal;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.nosql.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * @author rcolin
 */
public class DataFileDao extends MongoReadWriteDao<DataFileModel, DataFileSearchFilter> {

    public static final String FILE_COLLECTION_NAME = "file";
    public static final String FILE_PREFIX = "file";

    /**
     * Delegated dao use for sharing search filter building
     */
    private final DataDAO dataDAO;

    public DataFileDao(MongoDBService mongodb, SPARQLService sparql) {
        super(mongodb, DataFileModel.class, FILE_COLLECTION_NAME, FILE_PREFIX);
        dataDAO = new DataDAO(mongodb,sparql);
    }

    @Override
    protected void createIndexes() {
        IndexOptions unicityOptions = new IndexOptions().unique(true);

        collection.createIndex(Indexes.ascending(MongoModel.URI_FIELD), unicityOptions);
        collection.createIndex(Indexes.ascending(DataModel.DATE_FIELD), unicityOptions);
        collection.createIndex(Indexes.ascending(DataFileModel.PATH_FIELD), unicityOptions);

        collection.createIndex(Indexes.ascending(DataModel.PROVENANCE_FIELD, DataModel.TARGET_FIELD, DataModel.DATE_FIELD), unicityOptions);
        collection.createIndex(Indexes.ascending(DataModel.TARGET_FIELD, DataModel.DATE_FIELD), unicityOptions);
    }

    @Override
    public List<Bson> getBsonFilters(DataFileSearchFilter searchFilter) throws Exception {

        // rely on DataDao for building the document filter according DataSearchFilter (extended by DataFileSearchFilter)
        List<Bson> bsonFilters = dataDAO.getBsonFilters(searchFilter);

        // apply custom filter which are specific to a DataFileModel
        if(!CollectionUtils.isEmpty(searchFilter.getRdfTypes())){
            bsonFilters.add(Filters.in(DataFileModel.RDF_TYPE_FIELD, searchFilter.getRdfTypes()));
        }

        if(!StringUtils.isEmpty(searchFilter.getFileName())){

            // regex pattern on filename, Case insensitivity is used with "i" option
            bsonFilters.add(Filters.regex(DataFileModel.FILENAME_FIELD,searchFilter.getFileName(),"i"));
        }

        return bsonFilters;
    }

    public Set<URI> getUsedProvenancesURIs(DataFileSearchFilter searchFilter) throws Exception {
        Document filter = filterToDocument(searchFilter);
        return mongodb.distinct(DataModel.PROVENANCE_URI_FIELD, URI.class, FILE_COLLECTION_NAME, filter);
    }

}
