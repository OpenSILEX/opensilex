//******************************************************************************
//                          SectionDAO.java
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
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

/**
 *
 * @author Arnaud Charleroy
 */
public class SectionDAO {
    public static final String SECTION_COLLECTION_NAME = "sections";
    public static final String SECTION_PREFIX = "id/sections";
    protected final MongoDBService nosql; 
     
    public SectionDAO(MongoDBService nosql ) {
        this.nosql = nosql; 
    }  

    public SectionModel create(SectionModel section) throws Exception {
        nosql.getDatabase().getCollection(SECTION_COLLECTION_NAME).createIndex(Indexes.ascending("uri"), new IndexOptions().unique(true));
        nosql.create(section, SectionModel.class, SECTION_COLLECTION_NAME, SECTION_PREFIX);

        return section;
    }
    
    
    public ListWithPagination<SectionModel> search(
            List<URI> uris,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) throws Exception {

        Document filter = searchFilter(uris);

        ListWithPagination<SectionModel> sections = nosql.searchWithPagination(SectionModel.class, SECTION_COLLECTION_NAME, filter, orderByList, page, pageSize);

        return sections;

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
        nosql.delete(SectionModel.class, SECTION_COLLECTION_NAME, uri);
    }

    public void delete(List<URI> uris) throws NoSQLInvalidURIException, Exception {
        nosql.delete(SectionModel.class, SECTION_COLLECTION_NAME, uris);
    }
    
    public SectionModel update(SectionModel instance) throws NoSQLInvalidURIException {
        nosql.update(instance, SectionModel.class, SECTION_COLLECTION_NAME);
        return instance;
    }

}

