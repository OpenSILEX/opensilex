//******************************************************************************
//                          FormDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: maximilian.hart@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************

package org.opensilex.mobile.dal;


import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

/**
 * This class interacts with the mongo database, it provides all the basic CRUD operations for forms.
 *
 * @author Maximilian Hart
 */
public class FormDAO {

    public static final String FORM_COLLECTION_NAME = "forms";
    public static final String FORM_PREFIX = "id/forms";
    protected final MongoDBService nosql;

    public FormDAO(MongoDBService nosql ) {
        this.nosql = nosql;
    }

    public FormModel create(FormModel form) throws Exception {
        nosql.getDatabase().getCollection(FORM_COLLECTION_NAME).createIndex(Indexes.ascending("uri"), new IndexOptions().unique(true));
        nosql.create(form, FormModel.class, FORM_COLLECTION_NAME, FORM_PREFIX);

        return form;
    }


    public ListWithPagination<FormModel> search(
            List<URI> uris,
            List<URI> rdfTypes,
            boolean byRoot,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) throws Exception {

        Document filter = searchFilter(uris, rdfTypes, byRoot);

        ListWithPagination<FormModel> forms = nosql.searchWithPagination(FormModel.class, FORM_COLLECTION_NAME, filter, orderByList, page, pageSize);

        return forms;

    }

    public Document searchFilter(List<URI> uris, List<URI> rdfTypes, boolean byRoot) throws Exception {

        Document filter = new Document();

        if (uris != null && !uris.isEmpty()) {
            Document inFilter = new Document();
            inFilter.put("$in", uris);
            filter.put("uri", inFilter);
        }
        if (byRoot==true) {
            filter.put("root", true);
        }

        if(rdfTypes!=null && !rdfTypes.isEmpty()){
            List<URI> expandedRdfTypes = new ArrayList<>();
            for(URI shortUri : rdfTypes){
                expandedRdfTypes.add(new URI(SPARQLDeserializers.getExpandedURI(shortUri)));
            }
            //URI expandedRdfType = new URI(SPARQLDeserializers.getExpandedURI(rdfType));
            Document inFilter = new Document();
            inFilter.put("$in", expandedRdfTypes);
            filter.put("type", inFilter);
        }
        return filter;
    }

    public void delete(URI uri) throws NoSQLInvalidURIException, Exception {
        nosql.delete(FormModel.class, FORM_COLLECTION_NAME, uri);
    }

    public void delete(List<URI> uris) throws NoSQLInvalidURIException, Exception {
        nosql.delete(FormModel.class, FORM_COLLECTION_NAME, uris);
    }

    public FormModel update(FormModel instance) throws NoSQLInvalidURIException {
        nosql.update(instance, FormModel.class, FORM_COLLECTION_NAME);
        return instance;
    }

}

