//******************************************************************************
//                          DocumentDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.document.dal;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OA;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.NoSuchFileException;
import java.util.List;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;


/**
 * @author Emilie Fernandez
 */
public class DocumentDAO {

    protected final SPARQLService sparql;
    protected final MongoDBService nosql;
    protected final FileStorageService fs;

    public final static String FS_DOCUMENT_PREFIX = "documents";

    public DocumentDAO(SPARQLService sparql, MongoDBService nosql, FileStorageService fs) {
        this.sparql = sparql;
        this.nosql = nosql;
        this.fs = fs;
    }

    /**
     * Creates a document, either with a file or from a source URL if the `file` parameter is null.
     *
     * @param instance The document instance.
     * @param file The file to associate with the document. If null, a document with a source will be created.
     * @return The new document
     * @throws Exception
     */
    public DocumentModel create(DocumentModel instance, File file) throws Exception {
        if (file == null) {
            return createWithSource(instance);
        }
        return createWithFile(instance, file);
    }

    /**
     * Creates a document with a file.
     *
     * @param instance
     * @param file
     * @return
     * @throws Exception If the file is empty.
     */
    public DocumentModel createWithFile(DocumentModel instance, File file) throws Exception {
        if (file.length() == 0) {
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

    /**
     * Creates a document with a source URL. The property `source` should not be null.
     *
     * @param instance
     * @return
     * @throws Exception If the property `source` of the document is null.
     */
    public DocumentModel createWithSource(DocumentModel instance) throws Exception {
        if (instance.getSource() == null) {
            throw new BadRequestException("Source cannot be null");
        }

        sparql.create(instance);

        return instance;
    }

    public byte[] getFile(URI uri) throws Exception {
        try {
            return fs.readFileAsByteArray(FS_DOCUMENT_PREFIX, uri);
        }catch (NoSuchFileException | FileNotFoundException ex){
            throw new NotFoundURIException(ex.getMessage(),uri);
        }
    }

    public DocumentModel getMetadata(URI uri, AccountModel user) throws Exception {
        return sparql.getByURI(DocumentModel.class, uri, user.getLanguage());   
    }

    public DocumentModel update(DocumentModel instance, AccountModel user) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public void delete(URI uri, AccountModel user) throws Exception {

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

    public boolean isDocumentType(URI type) throws SPARQLException {
        return sparql.executeAskQuery(new AskBuilder()
                .addWhere(SPARQLDeserializers.nodeURI(type), Ontology.subClassAny, Oeso.Document)
        );
    }

    /**
     * Search a document with several optional parameters
     *
     * @param user
     * @param type
     * @param title
     * @param date
     * @param targets
     * @param authors
     * @param subject Filters by keyword.
     * @param multiple Filters by title OR keyword. Useful for a quick search.
     * @param deprecated
     * @param orderByList
     * @param page
     * @param pageSize
     * @return
     * @throws Exception
     */
    public ListWithPagination<DocumentModel> search(AccountModel user, URI type, String title, String date, URI targets, String authors, String subject, String multiple, String deprecated, List<OrderBy> orderByList, int page, int pageSize) throws Exception {
        
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
                // If either the subject or the "multiple" (ie. title or subject) fields is present, then the "subject"
                // clause must be added in the query (because it is not present by default)
                if (StringUtils.isNotEmpty(subject) || StringUtils.isNotEmpty(multiple)) {
                    // Appends the subject clause with its triple
                    appendSubjectsListClause(multipleGraphGroupElem);
                    // Appends the subject and/or multiple filters
                    appendSubjectsListFilter(multipleGraphGroupElem, subject);
                    appendMultipleFilter(select, multiple);
                }
                appendDeprecatedFilter(select, deprecated);
            },
            orderByList,
            page,
            pageSize
        );
    }

    /**
     * Appends the following filter :
     *
     * <pre>
     * FILTER ( regex(?subject, "test", "i") || regex(?title, "test", "i") )
     * </pre>
     *
     * @param select
     * @param multiple
     */
    private void appendMultipleFilter(SelectBuilder select, String multiple) {
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

    /**
     * Appends the following clause to the elementGroup :
     *
     * <pre>
     * OPTIONAL {
     *    ?uri dc:subject ?subject
     * }
     * </pre>
     *
     * @param elementGroup
     */
    private void appendSubjectsListClause(ElementGroup elementGroup) {
        Var uriVar = SPARQLQueryHelper.makeVar(DocumentModel.URI_FIELD);
        Var subjectVar = SPARQLQueryHelper.makeVar(DocumentModel.SUBJECT_FIELD);

        ElementGroup subjectTripleGroup = new ElementGroup();
        subjectTripleGroup.addTriplePattern(new Triple(uriVar, DCTerms.subject.asNode(), subjectVar));
        elementGroup.addElement(new ElementOptional(subjectTripleGroup));
    }

    /**
     * Appends the following filter to the element group :
     *
     * <pre>
     * FILTER regex(?subject, "test", "i")
     * </pre>
     *
     * @param subjectGraphGroupElem
     * @param subject
     */
    private void appendSubjectsListFilter(ElementGroup subjectGraphGroupElem, String subject) {
        if (!StringUtils.isEmpty(subject)) {
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

    public void validateDocumentAccess(URI documentURI, AccountModel user) throws Exception {
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

            ExperimentDAO xpDAO = new ExperimentDAO(sparql, nosql);
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

            ExperimentDAO xpsoDAO = new ExperimentDAO(sparql, nosql);
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
