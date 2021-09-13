//******************************************************************************
//                        FormDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opensilex.mobile.dal;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.bson.Document;
import static org.opensilex.core.data.dal.DataDAO.DATA_COLLECTION_NAME;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.scientificObject.api.ScientificObjectNodeDTO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.mobile.api.FormCreationDTO;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

/**
 *
 * @author Arnaud Charleroy
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
    
    /*public ArrayList<FormModel> getForms(List<URI> formsURI) {
        FindIterable<Document> documents  = nosql.getDatabase().getCollection(FORM_COLLECTION_NAME).find();
    }*/
    
    public ListWithPagination<FormModel> search(
            List<URI> uris,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) throws Exception {

        Document filter = searchFilter(uris);

        ListWithPagination<FormModel> forms = nosql.searchWithPagination(FormModel.class, FORM_COLLECTION_NAME, filter, orderByList, page, pageSize);

        return forms;

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

