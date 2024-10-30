package org.opensilex.sparql.service.query;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opensilex.sparql.service.SPARQLService.URI_VAR;

/**
 * @author rcolin
 */
public abstract class AbstractSparqlUrisQuery<T extends SPARQLResourceModel> implements SparqlUrisQuery<T> {

    protected final SPARQLService sparql;

    /**
     * @return the WHERE clause used for {@link #getUnknownStream()} {@link #getExistingStream()}, {@link #resultsAsStream(Function)}
     * SPARQL queries
     */
    abstract WhereBuilder getWhere();

    /**
     * @return The {@link SelectBuilder} used for {@link #resultsAsStream(Function)} SPARQL query
     * @apiNote The default implementation add the WHERE from {@link #getWhere()} inside the {@link SelectBuilder}
     */
    abstract SelectBuilder getSelect();

    /**
     * @return The {@link Function} used to extract the URI from the incoming results from database to a {@link URI}
     * @apiNote The generated SPARQL query must use the {@link SPARQLService#URI_VAR} when retrieving existing/unknown URI
     */
    abstract Function<SPARQLResult, URI> uriHandler();


    protected AbstractSparqlUrisQuery(SPARQLService sparql) {
        this.sparql = sparql;
    }

    @Override
    public Stream<T> resultsAsStream(Function<SPARQLResult, T> resultHandler) throws SPARQLException {
        SelectBuilder select = getSelect().addWhere(
                getWhere()
        );

        // add VALUES clause
        SPARQLQueryHelper.addWhereUriStringValues(select, URI_VAR.getVarName(), getUniqueUriStringStream(), true, getQuerySize());

        return sparql.executeSelectQueryAsStream(select).map(resultHandler);
    }

    @Override
    public Stream<URI> getExistingStream() throws SPARQLException {
        // The query return distinct EXISTING URIs
        SelectBuilder select = new SelectBuilder()
                .setDistinct(true)
                .addVar(URI_VAR)
                .addWhere(getWhere());

        // add VALUES clause
        SPARQLQueryHelper.addWhereUriStringValues(select, URI_VAR.getVarName(), getUniqueUriStringStream(), true, getQuerySize());

        Function<SPARQLResult, URI> uriHandler = uriHandler();
        return sparql.executeSelectQueryAsStream(select).map(uriHandler);
    }

    @Override
    public List<T> getResults(Function<SPARQLResult, T> resultHandler,
                              String errorMsg) throws SPARQLException {

        // Get result stream and convert it to list
        Stream<T> stream = resultsAsStream(resultHandler);
        List<T> results = new ArrayList<>(getQuerySize());
        stream.forEach(results::add);

        if (results.size() == getQuerySize()) {
            return results;
        }

        // Compute unknown URIs
        Set<String> expectedURIs = getUniqueUriStringStream().collect(Collectors.toSet());
        results.forEach(result -> expectedURIs.remove(result.getUri().toString()));
        results.clear();

        throw new SPARQLInvalidUriListException(errorMsg, expectedURIs);
    }

    @Override
    public Stream<URI> getUnknownStream() throws SPARQLException {

        // The query return distinct UNKNOWN URIs
        SelectBuilder select = new SelectBuilder()
                .setDistinct(true)
                .addVar(URI_VAR)
                .addFilter(SPARQLQueryHelper.getExprFactory().notexists(
                        getWhere()
                ));

        // add VALUES clause
        SPARQLQueryHelper.addWhereUriStringValues(select, URI_VAR.getVarName(), getUniqueUriStringStream(), true, getQuerySize());

        Function<SPARQLResult, URI> uriHandler = uriHandler();
        return sparql.executeSelectQueryAsStream(select).map(uriHandler);
    }

    /**
     * Check if
     *
     * @apiNote This method rely on {{@link #getUnknownStream()}} {@link Stream} to check if some results was found
     */
    @Override
    public void checkUnknowns(String errorMsg) throws SPARQLException {
        List<URI> unknowns = getUnknownStream().collect(Collectors.toList());
        if (!unknowns.isEmpty()) {
            throw new SPARQLInvalidUriListException(errorMsg, unknowns);
        }
    }

}