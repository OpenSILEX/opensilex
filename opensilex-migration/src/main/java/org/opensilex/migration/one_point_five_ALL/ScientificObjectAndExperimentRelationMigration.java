package org.opensilex.migration.one_point_five_ALL;

import org.apache.jena.arq.querybuilder.*;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.SOSA;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.migration.DatabaseMigrationModuleUpdate;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;

/**
 * @author dnooka / Max
 */
public class ScientificObjectAndExperimentRelationMigration {

    private static final Node SO_VAR = NodeFactory.createVariable("scientificObj");
    private static final Node GRAPH_VAR = NodeFactory.createVariable("graph");
    private static final Node TYPE_VAR = NodeFactory.createVariable("type");
    protected static String DESCRIPTION = "Scientific Objects created from XP will have a relation: participatesIn an XP in the XP context";

    private final SPARQLService sparql;

    public ScientificObjectAndExperimentRelationMigration(SPARQLService sparql){
        this.sparql = sparql;
    }

    /**
     * Checks if this migration was most likely already run. Does this by checking if any participatesIn relations already exist on ScientificObjects
     * @return true if participatesIn was found on any ScientificObject
     */
    protected boolean wasMigrationPreviouslyRun() throws SPARQLException {
        //TODO MAX untested, also can i simply replace these things with an AskBuilder? probably shpuld do that
        SelectBuilder participatesInSelect = new SelectBuilder();

        participatesInSelect.addVar(SO_VAR);
        participatesInSelect.addWhere(TYPE_VAR, Ontology.subClassAny, Oeso.ScientificObject);
        participatesInSelect.addWhere(SO_VAR, RDF.type, TYPE_VAR);
        participatesInSelect.addWhere(SO_VAR, Oeso.participatesIn, GRAPH_VAR);

        return !sparql.executeSelectQueryAsStream(participatesInSelect).toList().isEmpty();
    }

    private WhereBuilder buildWhere() throws SPARQLException {

        Node scientificObjectClass = NodeFactory.createURI(Oeso.ScientificObject.getURI());
        Node defaultOsGraph = sparql.getDefaultGraph(ScientificObjectModel.class);
        ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();

        // build where clause to get all SOs which are present in XP context to migrate the existing SOs with a link to XP
        return new WhereBuilder()
                .addWhere(TYPE_VAR, Ontology.subClassAny, scientificObjectClass)
                .addGraph(GRAPH_VAR, new WhereBuilder()
                        .addWhere(SO_VAR, RDF.type, TYPE_VAR)
                .addFilter(exprFactory.not(exprFactory.eq(GRAPH_VAR, defaultOsGraph))));

    }

    protected void execute() throws SPARQLException {

        UpdateBuilder updateQuery = new UpdateBuilder()
                .addInsert(GRAPH_VAR, SO_VAR, Oeso.participatesIn, GRAPH_VAR)
                .addWhere(buildWhere());
        sparql.executeUpdateQuery(updateQuery);
    }
}
