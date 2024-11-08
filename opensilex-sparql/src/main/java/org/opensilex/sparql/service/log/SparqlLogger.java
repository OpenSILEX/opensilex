package org.opensilex.sparql.service.log;

import net.logstash.logback.argument.StructuredArgument;
import org.opensilex.log.OpenSilexStructuredLogger;
import org.slf4j.Logger;

public class SparqlLogger extends OpenSilexStructuredLogger {

    // region LOG TYPES
    public static final String SPARQL_CLEAR_GRAPH_QUERY = "sparql_clear_graph";
    public static final String SPARQL_CLEAR_QUERY = "sparql_clear";
    public static final String SPARQL_MOVE_QUERY = "sparql_move";
    public static final String SPARQL_LOAD_ONTOLOGY = "sparql_load_ontology";

    // region LOG PROPERTIES
    public static final String SPARQL_QUERY_TYPE = "sparql_query_type";
    public static final String SPARQL_QUERY = "sparql_query";
    public static final String SPARQL_GRAPH = "graph";
    public static final String SPARQL_MOVE_FROM = "src_graph";
    public static final String SPARQL_MOVE_TO = "dest_graph";

    // region SPARQL queries
    public static final String SPARQL_SELECT_QUERY = "sparql_select";
    public static final String SPARQL_DESCRIBE_QUERY = "sparql_describe";
    public static final String SPARQL_ASK_QUERY = "sparql_ask";
    public static final String SPARQL_CONSTRUCT_QUERY = "sparql_construct";



    public SparqlLogger(Logger logger, StructuredArgument structuredArgument) {
        super(logger, structuredArgument);
    }
}
