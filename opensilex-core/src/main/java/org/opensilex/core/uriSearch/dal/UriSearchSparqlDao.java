/*
 * ******************************************************************************
 *                                     UriSearchSparqlDao.java
 *  OpenSILEX
 *  Copyright © INRAE 2024
 *  Creation date:  26 august, 2024
 *  Contact: maximilian.hart@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */

package org.opensilex.core.uriSearch.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public class UriSearchSparqlDao {

    //#region: attributes/constructor
    private final SPARQLService sparql;
    private final AccountModel currentUser;

    public UriSearchSparqlDao(SPARQLService sparql, AccountModel currentUser) {
        this.sparql = sparql;
        this.currentUser = currentUser;
    }
    //#endregion

    //#region: static stuff
    private final static String CONTEXT_STRING_VAR = "g";
    private final static String COMMENT_STRING_VAR = "comment";
    private final static String IS_STUDY_EFFECT_IN = "studyIn";
    private final static String HAS_FACTOR = "factor";
    //#endregion

    //#region: PUBLIC METHODS

    /**
     *
     * Precondition: if multiple elements have same uri then we suppose they are SCIENTIFIC OBJECTS
     */
    public SparqlNamedResourceModelPlus searchByUri(URI uri) throws Exception {
        List<SparqlNamedResourceModelPlus> resultsAsModels = new ArrayList<>();
        sparql.executeSelectQueryAsStream(
                generateSparqlRequest(uri)).forEach((SPARQLResult result) -> resultsAsModels.add(buildModelFromSparqlResult(result)));
        //If multiple found then keep only default graph one
        if(resultsAsModels.isEmpty()) {
            return null;
        }else if(resultsAsModels.size() == 1) {
            return resultsAsModels.get(0);
        }
        //TODO if elements other than Scientific objects can be in multiple graphs then we would need to replace the below line with a fetching of the correct graph URI by looking at which Super type has a graph associated to it
        URI defaultGraph = sparql.getDefaultGraphURI(ScientificObjectModel.class);
        for(SparqlNamedResourceModelPlus next : resultsAsModels){
            if(SPARQLDeserializers.compareURIs(defaultGraph, next.getContext())) {
                return next;
            }
        }
        //If no default graph found throw an exception because this will need to be debugged
        throw new NotFoundException("No element from a default graph found!");
    }

    //#endregion
    //#region: PRIVATE METHODS

    private SparqlNamedResourceModelPlus buildModelFromSparqlResult(SPARQLResult result) {
        PersonModel publisher = new PersonModel();
        URI factor = null;
        SPARQLNamedResourceModel model = new SPARQLNamedResourceModel();

        //uri
        String expandedURI = SPARQLDeserializers.getExpandedURI(result.getStringValue(SPARQLNamedResourceModel.URI_FIELD));
        model.setUri(URI.create(expandedURI));

        //name
        model.setName(result.getStringValue(SPARQLNamedResourceModel.NAME_FIELD));

        //Comment
        String rdfsComment = result.getStringValue(COMMENT_STRING_VAR);

        //type
        String typeUri = SPARQLDeserializers.getExpandedURI(result.getStringValue(SPARQLResourceModel.TYPE_FIELD));
        model.setType(URI.create(typeUri));

        //typename
        SPARQLLabel typeLabel = new SPARQLLabel();
        typeLabel.setDefaultLang(currentUser.getLanguage());
        typeLabel.setDefaultValue(result.getStringValue(SPARQLResourceModel.TYPE_NAME_FIELD));
        model.setTypeLabel(typeLabel);

        //Set factor if not null
        String factorStringUri = result.getStringValue(HAS_FACTOR);
        if (Objects.nonNull(factorStringUri)) {
            factor = URI.create(factorStringUri);
        }

        // context, crush with studied effect in if it was a factor or factor level
        String contextStringUri = result.getStringValue(CONTEXT_STRING_VAR);
        String studiedInXp = result.getStringValue(IS_STUDY_EFFECT_IN);
        URI contextUri = null;
        if (Objects.nonNull(studiedInXp)) {
            contextUri = URI.create(studiedInXp);
        }
        if (contextUri == null && Objects.nonNull(contextStringUri)) {
            contextUri = URI.create(contextStringUri);
        }

        //publisher, published, updated
        String publisherUri = SPARQLDeserializers.getShortURI(result.getStringValue(SPARQLResourceModel.PUBLISHER_FIELD));
        String publicationDate = result.getStringValue(SPARQLResourceModel.PUBLICATION_DATE_FIELD);
        String lastUpdateDate = result.getStringValue(SPARQLResourceModel.LAST_UPDATE_DATE_FIELD);
        if (Objects.nonNull(publisherUri)) {
            model.setPublisher(URI.create(publisherUri));
            publisher.setFirstName(result.getStringValue(PersonModel.FIRST_NAME_FIELD));
            publisher.setLastName(result.getStringValue(PersonModel.LAST_NAME_FIELD));
        }
        if (Objects.nonNull(publicationDate)) {
            try {
                model.setPublicationDate(OffsetDateTime.parse(publicationDate));
            } catch (DateTimeParseException e) {
                model.setPublicationDate(null);
            }
        }
        if (Objects.nonNull(lastUpdateDate)) {
            try {
                model.setLastUpdateDate(OffsetDateTime.parse(lastUpdateDate));
            } catch (DateTimeParseException e) {
                model.setLastUpdateDate(null);
            }
        }

        return new SparqlNamedResourceModelPlus(model, publisher, contextUri, rdfsComment, factor);
    }

    /**
     *
     * @return A sparql request that will fetch, name, type, typename and metadata for the passed uri.
     * Includes publisher first and last name to be able to show immediately without making another request
     */
    private SelectBuilder generateSparqlRequest(URI uri) throws Exception {
        //Some stuff that gets used later in this function
        String lang = currentUser.getLanguage();
        SelectBuilder result = new SelectBuilder();
        Locale locale = Locale.forLanguageTag(lang);

        //Vars returned
        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        Var nameVar = makeVar(SPARQLNamedResourceModel.NAME_FIELD);
        Var typeVar = makeVar(SPARQLResourceModel.TYPE_FIELD);
        Var typeNameVar = makeVar(SPARQLResourceModel.TYPE_NAME_FIELD);
        Var rdfsCommentVar = makeVar(COMMENT_STRING_VAR);
        Var publisherVar = makeVar(SPARQLResourceModel.PUBLISHER_FIELD);
        Var publisherFirstName = makeVar(PersonModel.FIRST_NAME_FIELD);
        Var publisherLastName = makeVar(PersonModel.LAST_NAME_FIELD);
        Var publishedVar = makeVar(SPARQLResourceModel.PUBLICATION_DATE_FIELD);
        Var updated = makeVar(SPARQLResourceModel.LAST_UPDATE_DATE_FIELD);
        Var graphVar = makeVar(CONTEXT_STRING_VAR);
        Var isStudyEffectInVar = makeVar(IS_STUDY_EFFECT_IN);
        Var factorVar = makeVar(HAS_FACTOR);
        //Other vars used
        Var publisherPersonVar = makeVar("person");

        result.addVar(uriVar);
        result.addVar(nameVar);
        result.addVar(typeVar);
        result.addVar(typeNameVar);
        result.addVar(rdfsCommentVar);
        result.addVar(publisherVar);
        result.addVar(publisherFirstName);
        result.addVar(publisherLastName);
        result.addVar(publishedVar);
        result.addVar(updated);
        result.addVar(graphVar);
        result.addVar(isStudyEffectInVar);
        result.addVar(factorVar);

        //Everything that concerns our uri needs to be by distinct graph to avoid duplicates when same uri is present in multiple graphs
        //To do this make a subwhere to put in an addGraph operation
        WhereBuilder inGraphWhere = new WhereBuilder();

        //Type
        inGraphWhere.addWhere(uriVar, RDF.type, typeVar);

        //Comment
        WhereHandler optionalCommentHandler = new WhereHandler();
        optionalCommentHandler.addWhere(result.makeTriplePath(uriVar, RDFS.comment, rdfsCommentVar));
        optionalCommentHandler.addFilter(SPARQLQueryHelper.langFilterWithDefault(COMMENT_STRING_VAR, locale.getLanguage()));
        inGraphWhere.getWhereHandler().addOptional(optionalCommentHandler);

        //If the Uri was a factor or factor level then we need to fetch the Experiment for redirection
        inGraphWhere.addOptional(new WhereBuilder().addWhere(uriVar, Oeso.studiedEffectIn, isStudyEffectInVar));
        inGraphWhere.addOptional(new WhereBuilder().addWhere(uriVar, Oeso.hasFactor, factorVar).addWhere(factorVar, Oeso.studiedEffectIn, isStudyEffectInVar));

        //label
        WhereHandler optionalLabelHandler = new WhereHandler();
        optionalLabelHandler.addWhere(result.makeTriplePath(uriVar, RDFS.label, nameVar));
        optionalLabelHandler.addFilter(SPARQLQueryHelper.langFilterWithDefault(SPARQLNamedResourceModel.NAME_FIELD, locale.getLanguage()));
        inGraphWhere.getWhereHandler().addOptional(optionalLabelHandler);

        //publisher, published, updated
        inGraphWhere.addOptional(new WhereBuilder().addWhere(uriVar, DCTerms.publisher, publisherVar));
        inGraphWhere.addOptional(new WhereBuilder().addWhere(uriVar, DCTerms.issued, publishedVar));
        inGraphWhere.addOptional(new WhereBuilder().addWhere(uriVar, DCTerms.modified, updated));

        //Extra details for publisher info, search in user graph and in an optional for the admin use-case (no first or last name)
        result.addGraph(graphVar, inGraphWhere);
        WhereBuilder foafDetails = new WhereBuilder().addGraph(
                sparql.getDefaultGraph(AccountModel.class),
                new WhereBuilder()
                        .addWhere(publisherPersonVar, FOAF.account.asNode(), publisherVar)
                        .addWhere(publisherPersonVar, FOAF.firstName.asNode(), publisherFirstName)
                        .addWhere(publisherPersonVar, FOAF.lastName.asNode(), publisherLastName)
        );
        result.addOptional(foafDetails);

        //Rdf type label outside of graph as this information is stored in global graph
        WhereHandler optionalTypeLabelHandler = new WhereHandler();
        optionalTypeLabelHandler.addWhere(result.makeTriplePath(typeVar, RDFS.label, typeNameVar));
        optionalTypeLabelHandler.addFilter(SPARQLQueryHelper.langFilterWithDefault(SPARQLResourceModel.TYPE_NAME_FIELD, locale.getLanguage()));
        result.getWhereHandler().addOptional(optionalTypeLabelHandler);

        //uri value
        result.addValueVar(SPARQLResourceModel.URI_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(uri)));

        return result;
    }
    //#endregion
    //#region: Classes used as return types

    /**
     * Class used as return type for search instead of making a Model class just for this
     */
    public static class SparqlNamedResourceModelPlus {
        private final SPARQLNamedResourceModel model;
        private final PersonModel publisher;
        private final URI context;
        private final String rdfsComment;
        private final URI factor;

        public SparqlNamedResourceModelPlus(SPARQLNamedResourceModel model, PersonModel publisher, URI context, String rdfsComment, URI factor) {
            this.model = model;
            this.publisher = publisher;
            this.context = context;
            this.rdfsComment = rdfsComment;
            this.factor = factor;
        }

        public SPARQLNamedResourceModel getModel() {
            return model;
        }

        public PersonModel getPublisher() {
            return publisher;
        }

        public URI getContext() {
            return context;
        }

        public String getRdfsComment() {
            return rdfsComment;
        }

        public URI getFactor() {
            return factor;
        }
    }
    //#endregion
}
