//******************************************************************************
//                          DocumentDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.document.dal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.security.user.dal.UserModel;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;
import java.io.File;
import java.nio.file.Path;
import org.opensilex.fs.service.FileStorageService;
import java.time.LocalDate;
import org.apache.jena.graph.NodeFactory;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.vocabulary.DCTerms;

/**
 * @author Emilie Fernandez
 */
public class DocumentDAO {

    protected final SPARQLService sparql;

    protected final FileStorageService fs;

    public final static String FS_DOCUMENT_PREFIX = "documents";

    public DocumentDAO(SPARQLService sparql, FileStorageService fs) {
        this.sparql = sparql;
        this.fs = fs;
    }

    public DocumentModel create(DocumentModel instance, File file) throws Exception {
        sparql.create(instance);
        fs.writeFile(FS_DOCUMENT_PREFIX, instance.getUri(), file);
        return instance;
    }

    // public String getFile(URI uri) throws Exception {
    //     return fs.readFile(FS_DOCUMENT_PREFIX, uri);    
    // }

    public byte[] getFile(URI uri) throws Exception {
        return fs.readFileAsByteArray(FS_DOCUMENT_PREFIX, uri);
    }

    public DocumentModel getMetadata(URI uri, UserModel user) throws Exception {
        return sparql.getByURI(DocumentModel.class, uri, user.getLanguage());   
    }

    public DocumentModel update(DocumentModel instance, UserModel user) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public void delete(URI uri, UserModel user) throws Exception {
        sparql.delete(DocumentModel.class, uri);
        fs.delete(FS_DOCUMENT_PREFIX, uri);
    }

    public ListWithPagination<DocumentModel> search(String name, String deprecated, String subject, String date, URI rdfType, URI user, List<OrderBy> orderByList, int page, int pageSize) throws Exception {
        ListWithPagination<DocumentModel> listDocumentModel = sparql.searchWithPagination(
            DocumentModel.class,
            null,
            //TODO: implements filters
            (SelectBuilder select) -> {
                appendNameFilters(select, name);
                appendDeprecatedFilters(select, deprecated);
                appendSubjectFilters(select, subject);
                appendDateFilter(select, date);
                appendRdfTypeFilter(select, rdfType);
                appendUserDocFilter(select, user);
            },
            orderByList,
            page,
            pageSize
        );
        return listDocumentModel;
    }

    private void appendNameFilters(SelectBuilder select, String name) throws Exception {
        if (name != null) {
            select.addFilter(SPARQLQueryHelper.regexFilter(DocumentModel.NAME_FIELD, name));
        }
    }
   
    private void appendDeprecatedFilters(SelectBuilder select, String deprecated) throws Exception {        
        if (deprecated != null) {
            select.addFilter(SPARQLQueryHelper.eq(DocumentModel.DEPRECATED_FIELD, deprecated));
        }
    }

    private void appendSubjectFilters(SelectBuilder select, String subject) throws Exception {
        if (subject != null) {
            select.addFilter(SPARQLQueryHelper.regexFilter(DocumentModel.SUBJECT_FIELD, subject));
        }
    }

    private void appendDateFilter(SelectBuilder select, String date) throws Exception {
        if (date != null) {
            select.addFilter(SPARQLQueryHelper.regexFilter(DocumentModel.DATE_FIELD, date));
        }
    }

    private void appendRdfTypeFilter(SelectBuilder select, URI rdfType) throws Exception {
        if (rdfType != null) {
            select.addFilter(SPARQLQueryHelper.eq(DocumentModel.TYPE_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(rdfType.toString()))));
        }
    }

    private void appendUserDocFilter(SelectBuilder select, URI user) throws Exception {
        if (user != null) {
            select.addFilter(SPARQLQueryHelper.eq(DocumentModel.CREATOR_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(user.toString()))));
        }
    }
}
