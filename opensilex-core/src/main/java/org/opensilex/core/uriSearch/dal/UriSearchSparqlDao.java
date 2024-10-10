package org.opensilex.core.uriSearch.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.dal.PersonModel;
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
    private static String contextStringVar = "g";
    //#endregion

    //#region: PUBLIC METHODS

    /**
     *
     * Precondition: if multiple elements have same uri then we suppose they are SCIENTIFIC OBJECTS
     */
    public SparqlNamedResourceModelWithExtraStuff searchByUri(URI uri) throws Exception {
        List<SparqlNamedResourceModelWithExtraStuff> resultsAsModels = new ArrayList<>();
        sparql.executeSelectQueryAsStream(
                generateSparqlRequest(uri)).forEach((SPARQLResult result) -> resultsAsModels.add(buildModelFromSparqlResult(result)));
        //If multiple found then keep only default graph one
        if(resultsAsModels.isEmpty()) {
            return null;
        }else if(resultsAsModels.size() == 1) {
            return resultsAsModels.get(0).setTotalMatches(1);
        }else{
            //TODO if elements other than Scientific objects can be in multiple graphs then we would need to replace the below line with a fetching of the correct graph URI by looking at which Super type has a graph associated to it
            URI defaultGraph = sparql.getDefaultGraphURI(ScientificObjectModel.class);
            for(SparqlNamedResourceModelWithExtraStuff next : resultsAsModels){
                if(SPARQLDeserializers.compareURIs(defaultGraph, next.getContext())) {
                    return next.setTotalMatches(resultsAsModels.size());
                }
            }
            //If no default graph found throw an exception because this will need to be debugged
            throw new Exception("No element from a default graph found!");
        }
    }

    //#endregion
    //#region: PRIVATE METHODS

    private SparqlNamedResourceModelWithExtraStuff buildModelFromSparqlResult(SPARQLResult result) {
        PersonModel publisher = new PersonModel();
        SPARQLNamedResourceModel model = new SPARQLNamedResourceModel();
        //uri
        String expandedURI = SPARQLDeserializers.getExpandedURI(result.getStringValue(SPARQLNamedResourceModel.URI_FIELD));
        model.setUri(URI.create(expandedURI));
        //name
        model.setName(result.getStringValue(SPARQLNamedResourceModel.NAME_FIELD));
        //type
        String typeUri = SPARQLDeserializers.getExpandedURI(result.getStringValue(SPARQLResourceModel.TYPE_FIELD));
        model.setType(URI.create(typeUri));
        //typename
        SPARQLLabel typeLabel = new SPARQLLabel();
        typeLabel.setDefaultLang(currentUser.getLanguage());
        typeLabel.setDefaultValue(result.getStringValue(SPARQLResourceModel.TYPE_NAME_FIELD));
        model.setTypeLabel(typeLabel);
        // context
        String contextStringUri = result.getStringValue(contextStringVar);
        URI contextUri = null;
        if (Objects.nonNull(contextStringUri)) {
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

        return new SparqlNamedResourceModelWithExtraStuff(model, publisher, contextUri);
    }

    /**
     *
     * @return A sparql request that will fetch, name, type, typename and metadata for the passed uri.
     * Includes publisher first and last name to be able to show immediately without making another request
     */
    private SelectBuilder generateSparqlRequest(URI uri) throws Exception {
        String lang = currentUser.getLanguage();
        SelectBuilder result = new SelectBuilder();
        //Vars returned
        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        Var nameVar = makeVar(SPARQLNamedResourceModel.NAME_FIELD);
        Var typeVar = makeVar(SPARQLResourceModel.TYPE_FIELD);
        Var typeNameVar = makeVar(SPARQLResourceModel.TYPE_NAME_FIELD);
        Var publisherVar = makeVar(SPARQLResourceModel.PUBLISHER_FIELD);
        Var publisherFirstName = makeVar(PersonModel.FIRST_NAME_FIELD);
        Var publisherLastName = makeVar(PersonModel.LAST_NAME_FIELD);
        Var publishedVar = makeVar(SPARQLResourceModel.PUBLICATION_DATE_FIELD);
        Var updated = makeVar(SPARQLResourceModel.LAST_UPDATE_DATE_FIELD);
        Var graphVar = makeVar(contextStringVar);
        //Other vars used
        Var publisherPersonVar = makeVar("person");
        result.addVar(uriVar);
        result.addVar(nameVar);
        result.addVar(typeVar);
        result.addVar(typeNameVar);
        result.addVar(publisherVar);
        result.addVar(publisherFirstName);
        result.addVar(publisherLastName);
        result.addVar(publishedVar);
        result.addVar(updated);
        result.addVar(graphVar);

        //Rdf type label outside of graph as this information is stored in global graph
        WhereHandler optionalTypeLabelHandler = new WhereHandler();
        optionalTypeLabelHandler.addWhere(result.makeTriplePath(typeVar, RDFS.label, typeNameVar));
        Locale locale = Locale.forLanguageTag(lang);
        optionalTypeLabelHandler.addFilter(SPARQLQueryHelper.langFilterWithDefault(SPARQLResourceModel.TYPE_NAME_FIELD, locale.getLanguage()));
        result.getWhereHandler().addOptional(optionalTypeLabelHandler);

        //Everything that concerns our uri needs to be by distinct graph to avoid duplicates when same uri is present in multiple graphs
        //To do this make a subwhere to put in an addGraph operation
        WhereBuilder inGraphWhere = new WhereBuilder();

        //Type
        inGraphWhere.addWhere(uriVar, RDF.type, typeVar);

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

        //uri value
        Object[] uriNodes = SPARQLDeserializers.nodeListURIAsArray(Collections.singletonList(uri));
        result.addValueVar(SPARQLResourceModel.URI_FIELD, uriNodes);

        return result;
    }
    //#endregion
    //#region: Classes used as return types

    /**
     * Class used as return type for search instead of making a Model class just for this
     */
    public static class SparqlNamedResourceModelWithExtraStuff {
        private SPARQLNamedResourceModel model;
        private PersonModel publisher;
        private URI context;
        private int totalMatches;

        public SparqlNamedResourceModelWithExtraStuff(SPARQLNamedResourceModel model, PersonModel publisher, URI context) {
            this.model = model;
            this.publisher = publisher;
            this.context = context;
            this.totalMatches = totalMatches;
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

        public int getTotalMatches() {
            return totalMatches;
        }

        public SparqlNamedResourceModelWithExtraStuff setTotalMatches(int totalMatches) {
            this.totalMatches = totalMatches;
            return this;
        }
    }
    //#endregion
}
