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

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.opensilex.nosql.mongodb.MongoDBService;

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
}
