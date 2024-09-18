package org.opensilex.core.uriSearch.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public class UriSearchSparqlDao {
    private final SPARQLService sparql;

    public UriSearchSparqlDao(SPARQLService sparql) {
        this.sparql = sparql;
    }

    //#region: PUBLIC METHODS

    public List<SPARQLNamedResourceModel> searchByUri(URI uri) {
        sparql.executeSelectQuery()
        return new ArrayList<SPARQLNamedResourceModel>() {};
    }
    /*
    sparql.executeSelectQuery(select, ThrowingConsumer.wrap((SPARQLResult result) -> {
            String expandedFactorURI = SPARQLDeserializers.getExpandedURI(result.getStringValue(FactorLevelModel.URI_FIELD));
            if (!loadedFactors.containsKey(expandedFactorURI)) {
                loadedFactors.put(expandedFactorURI, mapper.createInstance(graph, result, language, sparql));
            }
            String expandedSoURI = SPARQLDeserializers.getExpandedURI(result.getStringValue(soVar.getVarName()));
            if (!resultMap.containsKey(expandedSoURI)) {
                resultMap.put(expandedSoURI, new ArrayList<>());
            }
            resultMap.get(expandedSoURI).add(loadedFactors.get(expandedFactorURI));
        }, Exception.class));
     */

    //#endregion
    //#region: PRIVATE METHODS

    private SPARQLNamedResourceModel buildModelFromSparqlResult(SPARQLResult result) {
        SPARQLNamedResourceModel model = new SPARQLNamedResourceModel();
        String expandedURI = SPARQLDeserializers.getExpandedURI(result.getStringValue(SPARQLNamedResourceModel.URI_FIELD));
        model.setUri(URI.create(expandedURI));
        model.setName(result.getStringValue(SPARQLNamedResourceModel.NAME_FIELD));
        model.setTypeLabel();
    }

    /**
     *
     * @return A sparql request that will fetche, name, type, typename and metadata for the passed uri.
     */
    private SelectBuilder generateSparqlRequest(URI uri, AccountModel currentUser) throws Exception {
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
        result.addVar(uriVar);
        result.addVar(nameVar);
        result.addVar(typeVar);
        result.addVar(typeNameVar);
        result.addVar(publisherVar);
        result.addVar(publishedVar);
        result.addVar(updated);

        //Type
        result.addWhere(uriVar, RDF.type, typeVar);

        //Rdf type label
        WhereHandler optionalTypeLabelHandler = new WhereHandler();
        optionalTypeLabelHandler.addWhere(result.makeTriplePath(typeVar, RDFS.label, typeNameVar));
        Locale locale = Locale.forLanguageTag(lang);
        optionalTypeLabelHandler.addFilter(SPARQLQueryHelper.langFilterWithDefault(SPARQLResourceModel.TYPE_NAME_FIELD, locale.getLanguage()));
        result.getWhereHandler().addOptional(optionalTypeLabelHandler);

        //label
        WhereHandler optionalLabelHandler = new WhereHandler();
        optionalLabelHandler.addWhere(result.makeTriplePath(uriVar, RDFS.label, nameVar));
        optionalLabelHandler.addFilter(SPARQLQueryHelper.langFilterWithDefault(SPARQLNamedResourceModel.NAME_FIELD, locale.getLanguage()));
        result.getWhereHandler().addOptional(optionalLabelHandler);

        //publisher, published, updated
        result.addOptional(new WhereBuilder().addWhere(uriVar, DCTerms.publisher, publisherVar));
        result.addOptional(new WhereBuilder().addWhere(uriVar, DCTerms.issued, publishedVar));
        result.addOptional(new WhereBuilder().addWhere(uriVar, DCTerms.modified, updated));

        //uri value
        Object[] uriNodes = SPARQLDeserializers.nodeListURIAsArray(Collections.singletonList(uri));
        result.addValueVar(SPARQLResourceModel.URI_FIELD, uriNodes);

        return result;
    }
    //#endregion
}
