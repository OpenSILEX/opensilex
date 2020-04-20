/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.document.dal;

import java.io.InputStream;
import org.opensilex.nosql.mongodb.MongoDBConnection;
import org.opensilex.sparql.service.SPARQLService;

/**
 *
 * @author charlero
 */
/**
 * Documents DAO for MongoDB.
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class DocumentDAO {

    protected final MongoDBConnection nosql;

    protected final SPARQLService sparql;

    public DocumentDAO(SPARQLService sparql, MongoDBConnection nosql) {
        this.nosql = nosql;
        this.sparql = sparql;
    }

    public DocumentModel create(DocumentModel instance,InputStream file) throws Exception {
        sparql.create(instance);
        nosql.createFileFromStream(instance.getLabel(), file);
        return instance;
    }

    public DocumentModel update(DocumentModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

 
}
