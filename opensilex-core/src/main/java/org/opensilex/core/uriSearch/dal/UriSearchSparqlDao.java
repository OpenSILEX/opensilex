package org.opensilex.core.uriSearch.dal;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.OpenSilex;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ThrowingConsumer;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public class UriSearchSparqlDao {
    private final SPARQLService sparql;
    private final AccountModel currentUser;

    public UriSearchSparqlDao(SPARQLService sparql, AccountModel currentUser) {
        this.sparql = sparql;
        this.currentUser = currentUser;
    }

    //#region: PUBLIC METHODS

    public List<SPARQLNamedResourceModel> searchByUri(URI uri) throws Exception {
        List<SPARQLNamedResourceModel> resultsAsModels = new ArrayList<>();
        sparql.executeSelectQueryAsStream(
                generateSparqlRequest(uri)).forEach((SPARQLResult result) -> resultsAsModels.add(buildModelFromSparqlResult(result)));
        return resultsAsModels;
    }

    //#endregion
    //#region: PRIVATE METHODS

    private SPARQLNamedResourceModel buildModelFromSparqlResult(SPARQLResult result) {
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
        //publisher, published, updated
        //TODO This is duplicated, in ontology store loader, not sure where to put this
        String publisherUri = SPARQLDeserializers.getShortURI(result.getStringValue(SPARQLResourceModel.PUBLISHER_FIELD));
        String publicationDate = result.getStringValue(SPARQLResourceModel.PUBLICATION_DATE_FIELD);
        String lastUpdateDate = result.getStringValue(SPARQLResourceModel.LAST_UPDATE_DATE_FIELD);
        if (Objects.nonNull(publisherUri)) {
            model.setPublisher(URI.create(publisherUri));
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

        return model;
    }

    /**
     *
     * @return A sparql request that will fetch, name, type, typename and metadata for the passed uri.
     */
    private SelectBuilder generateSparqlRequest(URI uri) throws Exception {
        String lang = currentUser.getLanguage();
        SelectBuilder result = new SelectBuilder();
        //Vars
        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        Var nameVar = makeVar(SPARQLNamedResourceModel.NAME_FIELD);
        Var typeVar = makeVar(SPARQLResourceModel.TYPE_FIELD);
        Var typeNameVar = makeVar(SPARQLResourceModel.TYPE_NAME_FIELD);
        Var publisherVar = makeVar(SPARQLResourceModel.PUBLISHER_FIELD);
        Var publishedVar = makeVar(SPARQLResourceModel.PUBLICATION_DATE_FIELD);
        Var updated = makeVar(SPARQLResourceModel.LAST_UPDATE_DATE_FIELD);
        Var graphVar = makeVar("g");
        result.addVar(uriVar);
        result.addVar(nameVar);
        result.addVar(typeVar);
        result.addVar(typeNameVar);
        result.addVar(publisherVar);
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

        result.addGraph(graphVar, inGraphWhere);

        //uri value
        Object[] uriNodes = SPARQLDeserializers.nodeListURIAsArray(Collections.singletonList(uri));
        result.addValueVar(SPARQLResourceModel.URI_FIELD, uriNodes);

        return result;
    }
    //#endregion
}
