package org.opensilex.core.uriSearch.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public class UriSearchSparqlDao {
    private final SPARQLService sparql;

    public UriSearchSparqlDao(SPARQLService sparql) {
        this.sparql = sparql;
    }

    //#region: PUBLIC METHODS

    public List<SPARQLNamedResourceModel> searchByUri(URI uri) {
        //sparql.executeSelectQuery()
        return new ArrayList<SPARQLNamedResourceModel>() {};
    }

    //#endregion
    //#region: PRIVATE METHODS

    /**
     *
     * @return A sparql request that fetches, name, type, typename and metadata for the passed uri.
     */
    private SelectBuilder generateSparqlRequest(URI uri){
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
        //where builder
        WhereBuilder whereBuilder = new WhereBuilder();
        whereBuilder.addWhere(uriVar, RDF.type, typeVar);
        whereBuilder.addOptional
        return result;
    }
    //#endregion
}
