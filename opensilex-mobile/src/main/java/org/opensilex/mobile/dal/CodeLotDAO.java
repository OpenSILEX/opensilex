//******************************************************************************
//                          CodeLotDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: maximilian.hart@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.dal;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import java.net.URI;
import java.util.List;
import org.bson.Document;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

/**
 *
 * @author Maximilian Hart
 */
public class CodeLotDAO {
    public static final String CODELOT_COLLECTION_NAME = "code_lots";
    public static final String CODELOT_PREFIX = "id/code_lots";
    protected final MongoDBService nosql;

    public CodeLotDAO(MongoDBService nosql ) {
        this.nosql = nosql; 
    }

    public CodeLotModel create(CodeLotModel code) throws Exception {
        nosql.getDatabase().getCollection(CODELOT_COLLECTION_NAME).createIndex(Indexes.ascending("uri"), new IndexOptions().unique(true));
        //nosql.create(code, CodeLotModel.class, CODELOT_COLLECTION_NAME, CODELOT_PREFIX);

        return code;
    }

    public List<CodeLotModel> createAll(List<CodeLotModel> instances) throws Exception {
        //nosql.createAll(instances, CodeLotModel.class, CODELOT_COLLECTION_NAME, CODELOT_PREFIX);
        return instances;
    }

    public ListWithPagination<CodeLotModel> search(
            List<URI> uris,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) throws Exception {

        Document filter = searchFilter(uris);

        ListWithPagination<CodeLotModel> codes = nosql.searchWithPagination(CodeLotModel.class, CODELOT_COLLECTION_NAME, filter, orderByList, page, pageSize);

        return codes;

    }
    
    public Document searchFilter(List<URI> uris) throws Exception {   
                
        Document filter = new Document();

        if (uris != null && !uris.isEmpty()) {
            Document inFilter = new Document(); 
            inFilter.put("$in", uris);
            filter.put("uri", inFilter);
        }                
        return filter;
    }
    
    public void delete(URI uri) throws NoSQLInvalidURIException, Exception {
        //nosql.delete(CodeLotModel.class, CODELOT_COLLECTION_NAME, uri);
    }
}
