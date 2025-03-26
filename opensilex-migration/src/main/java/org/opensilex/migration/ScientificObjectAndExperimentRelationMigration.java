package org.opensilex.migration;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;

/**
 * @author dnooka
 */
public class ScientificObjectAndExperimentRelationMigration extends DatabaseMigrationModuleUpdate {

    private static final Node TYPE_VAR = NodeFactory.createVariable("type");
    private static final Node SO_VAR = NodeFactory.createVariable("scientificObj");
    private static final Node GRAPH_VAR = NodeFactory.createVariable("graph");

    Node scientificObjectClass = NodeFactory.createURI(Oeso.ScientificObject.getURI());

    @Override
    public String getDescription() {
        return "Scientific Objects created from XP will have a relation: participatesIn an XP in the XP context";
    }

    private WhereBuilder buildWhere(SPARQLService sparql) throws SPARQLException {

        Node defaultOsGraph = sparql.getDefaultGraph(ScientificObjectModel.class);
        ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();

        // build where clause to get all SOs which are present in XP context to migrate the existing SOs with a link to XP
        return new WhereBuilder()
                .addWhere(TYPE_VAR, Ontology.subClassAny, scientificObjectClass)
                .addGraph(GRAPH_VAR, new WhereBuilder()
                        .addWhere(SO_VAR, RDF.type, TYPE_VAR)
                .addFilter(exprFactory.not(exprFactory.eq(GRAPH_VAR, defaultOsGraph))));

    }

    @Override
    protected boolean applyOnSparql(SPARQLService sparql, SPARQLConfig sparqlConfig) throws SPARQLException {
        return sparql.executeAskQuery(
                new AskBuilder().addWhere(buildWhere(sparql))
        );
    }

    /**
     *
     * <pre>
     * {@code
     * INSERT {
     *   GRAPH ?graph {
     *    ?scientificObj oeso:participatesIn ?graph .
     *   }
     * }
     * WHERE {
     *    GRAPH ?graph {
     * 	    ?scientificObj a ?type .
     *   }
     *   ?type rdfs:subClassOf* <http://www.opensilex.org/vocabulary/oeso#ScientificObject> .
     *    FILTER (?graph != <http://opensilex.dev/set/scientific-object>)
     * }
     * }
     * </pre>
     */

    @Override
    protected void sparqlOperation(SPARQLService sparql, SPARQLConfig sparqlConfig) throws SPARQLException {

        UpdateBuilder updateQuery = new UpdateBuilder()
                .addInsert(GRAPH_VAR, SO_VAR, Oeso.participatesIn, GRAPH_VAR)
                .addWhere(buildWhere(sparql));
        sparql.executeUpdateQuery(updateQuery);
    }
}
