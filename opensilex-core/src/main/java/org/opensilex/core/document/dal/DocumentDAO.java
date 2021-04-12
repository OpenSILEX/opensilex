//******************************************************************************
//                          DocumentDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.document.dal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.NoSuchFileException;
import java.util.List;

import org.apache.jena.atlas.io.IO;
import org.opensilex.core.ontology.Oeso;
import org.apache.jena.vocabulary.OA;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.rdf.model.Property;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.opensilex.fs.service.FileStorageService;
import org.apache.jena.graph.NodeFactory;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.opensilex.fs.local.LocalFileSystemConnection;
import org.opensilex.security.authentication.NotFoundURIException;
import org.apache.commons.lang3.StringUtils;


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

        if(file.length() == 0){
            throw new IOException(instance.getUri()+ " : empty document "+file.getPath());
        }

        sparql.startTransaction();
        sparql.create(instance);
        try{
            fs.writeFile(FS_DOCUMENT_PREFIX, instance.getUri(), file);
            sparql.commitTransaction();
        }catch (IOException e){
            sparql.rollbackTransaction(e);
        }

        return instance;
    }

    public byte[] getFile(URI uri) throws Exception {
        try {
            return fs.readFileAsByteArray(FS_DOCUMENT_PREFIX, uri);
        }catch (NoSuchFileException | FileNotFoundException ex){
            throw new NotFoundURIException(ex.getMessage(),uri);
        }
    }

    public DocumentModel getMetadata(URI uri, UserModel user) throws Exception {
        return sparql.getByURI(DocumentModel.class, uri, user.getLanguage());   
    }

    public DocumentModel update(DocumentModel instance, UserModel user) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public void delete(URI uri, UserModel user) throws Exception {

        if(! fs.exist(FS_DOCUMENT_PREFIX,uri)){
            throw new NotFoundURIException(uri);
        }

        sparql.startTransaction();
        sparql.delete(DocumentModel.class, uri);
        try {
            fs.delete(FS_DOCUMENT_PREFIX, uri);
            sparql.commitTransaction();
        }catch (IOException e){
            sparql.rollbackTransaction(e);
        }
    }

    public ListWithPagination<DocumentModel> search(UserModel user, URI type, String title, String date, URI targets, String authors, String subject, String multiple, String deprecated, List<OrderBy> orderByList, int page, int pageSize) throws Exception {
        
        return sparql.searchWithPagination(
            DocumentModel.class,
            user.getLanguage(),
            (SelectBuilder select) -> {
                Node docGraph = sparql.getDefaultGraph(DocumentModel.class);
                ElementGroup rootElementGroup = select.getWhereHandler().getClause();
                ElementGroup multipleGraphGroupElem =  SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, docGraph);
               
                appendTypeFilter(select, type);
                appendTitleFilter(select, title);
                appendDateFilter(select, date);
                appendTargetsFilter(multipleGraphGroupElem, targets);
                appendAuthorsFilter(multipleGraphGroupElem, authors);
                appendSubjectsListFilter(multipleGraphGroupElem, subject);
                appendMultipleFilter(select, multiple);
                appendDeprecatedFilter(select, deprecated);
            },
            orderByList,
            page,
            pageSize
        );
    }

    private void appendMultipleFilter(SelectBuilder select, String multiple){
        if (!StringUtils.isEmpty(multiple)) {
            select.addFilter(SPARQLQueryHelper.or(
                    SPARQLQueryHelper.regexFilter(DocumentModel.SUBJECT_FIELD, multiple),
                    SPARQLQueryHelper.regexFilter(DocumentModel.TITLE_FIELD, multiple)
            ));
        }
    }

    private void appendTitleFilter(SelectBuilder select, String title) throws Exception {
        if (!StringUtils.isEmpty(title)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(DocumentModel.TITLE_FIELD, title));
        }
    }
   
    private void appendDeprecatedFilter(SelectBuilder select, String deprecated) throws Exception {        
        if (!StringUtils.isEmpty(deprecated)) {
            select.addFilter(SPARQLQueryHelper.eq(DocumentModel.DEPRECATED_FIELD, deprecated));
        }
    }

    private void appendDateFilter(SelectBuilder select, String date) throws Exception {
        if (!StringUtils.isEmpty(date)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(DocumentModel.DATE_FIELD, date));
        }
    }

    private void appendTypeFilter(SelectBuilder select, URI type) throws Exception {
        if (type != null) {
            select.addFilter(SPARQLQueryHelper.eq(DocumentModel.TYPE_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(type.toString()))));
        }
    }

    private void appendAuthorsFilter(ElementGroup authorsGraphGroupElem, String authors) throws Exception {
        if (!StringUtils.isEmpty(authors)) {
            Var uriVar = SPARQLQueryHelper.makeVar(DocumentModel.URI_FIELD);
            Var authorsVar = SPARQLQueryHelper.makeVar(DocumentModel.AUTHORS_FIELD);

            Triple docAuthorsTriple = new Triple(uriVar, Oeso.hasAuthor.asNode(), authorsVar);

            authorsGraphGroupElem.addTriplePattern(docAuthorsTriple);

            Expr authorsEqExpr = SPARQLQueryHelper.regexFilter(DocumentModel.AUTHORS_FIELD, authors);
            authorsGraphGroupElem.addElementFilter(new ElementFilter(authorsEqExpr));
    
        }
    }

    private void appendSubjectsListFilter(ElementGroup subjectGraphGroupElem, String subject) throws Exception {
        if (!StringUtils.isEmpty(subject)) {

            Var uriVar = SPARQLQueryHelper.makeVar(DocumentModel.URI_FIELD);
            Var subjectVar = SPARQLQueryHelper.makeVar(DocumentModel.SUBJECT_FIELD);

            Triple docSubjectTriple = new Triple(uriVar, DCTerms.subject.asNode(), subjectVar);
  
            subjectGraphGroupElem.addTriplePattern(docSubjectTriple);

            Expr subjectEqExpr = SPARQLQueryHelper.regexFilter(DocumentModel.SUBJECT_FIELD, subject);
            subjectGraphGroupElem.addElementFilter(new ElementFilter(subjectEqExpr));
        }
    }


    private void appendTargetsFilter(ElementGroup targetsGraphGroupElem, URI targets) throws Exception {

        if (targets != null) {

            Var uriVar = SPARQLQueryHelper.makeVar(DocumentModel.URI_FIELD);
            Var targetsVar = SPARQLQueryHelper.makeVar(DocumentModel.TARGET_FIELD);

            // append where between doc uri and the multi-valued "targets" property because
            // the sparql service don't fetch multi-valued property during the search call
            Triple targetsTriple = new Triple(uriVar, OA.hasTarget.asNode(), targetsVar);

            targetsGraphGroupElem.addTriplePattern(targetsTriple);

            Expr targetsEqExpr = SPARQLQueryHelper.eq(DocumentModel.TARGET_FIELD, targets);
            targetsGraphGroupElem.addElementFilter(new ElementFilter(targetsEqExpr));
        }
    }

    private static void addWhere(SelectBuilder select, String subjectVar, Property property, String objectVar) {
        select.getWhereHandler().getClause().addTriplePattern(new Triple(makeVar(subjectVar), property.asNode(), makeVar(objectVar)));
    }

    public void validateDocumentAccess(URI documentURI, UserModel user) throws Exception {
        if (!sparql.uriExists(DocumentModel.class, documentURI)) {
            throw new NotFoundURIException("Document URI not found: ", documentURI);
        }

        if (user.isAdmin()) {
            return;
        }

        Node uriVar = SPARQLDeserializers.nodeURI(documentURI);
        Node userNodeURI = SPARQLDeserializers.nodeURI(user.getUri());


        AskBuilder ask = sparql.getUriExistsQuery(DocumentModel.class, documentURI);

        if (!sparql.executeAskQuery(ask)) {
            // check related document experiment
            List<URI> xpUris = sparql.searchURIs(ExperimentModel.class, user.getLanguage(), (select) -> {
                select.addWhere(makeVar(ExperimentModel.URI_FIELD), OA.hasTarget.asNode(), SPARQLDeserializers.nodeURI(documentURI));
            });

            ExperimentDAO xpDAO = new ExperimentDAO(sparql);
            for (URI xpUri : xpUris) {
                try {
                    xpDAO.validateExperimentAccess(xpUri, user);
                    return;
                } catch (Exception ex) {
                    // Ignore exception
                }
            }

            // check related document scientific object
            List<URI> soUris = sparql.searchURIs(ScientificObjectModel.class, user.getLanguage(), (select) -> {
                select.addWhere(makeVar(ScientificObjectModel.URI_FIELD), OA.hasTarget.asNode(), SPARQLDeserializers.nodeURI(documentURI));
            });

            ExperimentDAO xpsoDAO = new ExperimentDAO(sparql);
            for (URI soUri : soUris) {
                try {
                    xpsoDAO.validateExperimentAccess(soUri, user);
                    return;
                } catch (Exception ex) {
                    // Ignore exception
                }
            }
        }
    }
}
