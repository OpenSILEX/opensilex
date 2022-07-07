package org.opensilex.sparql.rdf4j.graphdb;

import org.opensilex.config.ConfigDescription;
import org.opensilex.sparql.rdf4j.RDF4JConfig;

/**
 * {@link RDF4JConfig} specific to the use of a graphdb triple store.
 * Pay attention to your graphdb license, according to this, the repository configuration can be different
 *
 * @author rcolin
 * @see <a href="https://graphdb.ontotext.com/documentation/9.11/free/configuring-a-repository.html#configuration-parameters">GraphDB free repository parameters </a>
 * @see <a href="https://graphdb.ontotext.com/documentation/9.11/standard/configuring-a-repository.html#configuration-parameters">GraphDB standard repository parameters </a>
 * @see <a href="https://graphdb.ontotext.com/documentation/9.11/enterprise/configuring-a-repository.html#configuration-parameters">GraphDB enterprise repository parameters </a>
 */
public interface OntotextGraphDBConfig extends RDF4JConfig {

    String REPOSITORY_TYPE_TEMPLATE_PARAMETER = "repository-type";
    String SAIL_TYPE_TEMPLATE_PARAMETER = "sail-type";
    String QUERY_TIMEOUT_TEMPLATE_PARAMETER = "query-timeout";
    String QUERY_LIMIT_RESULTS_TEMPLATE_PARAMETER = "query-limit-results";

    @ConfigDescription(
            value = "GraphDB repository type",
            defaultString = "graphdb:FreeSailRepository"
    )
    String repositoryType();

    @ConfigDescription(
            value = "Type of Sail being configured (https://rdf4j.org/documentation/reference/configuration/)",
            defaultString = "graphdb:FreeSail"
    )
    String sailType();

    @ConfigDescription(
            value = "Number of seconds after which the evaluation of a query will be terminated",
            defaultInt = 60
    )
    int queryTimeout();

    @ConfigDescription(
            value = "Sets the maximum number of results returned from a query",
            defaultInt = 100000
    )
    int queryLimitResult();

}
