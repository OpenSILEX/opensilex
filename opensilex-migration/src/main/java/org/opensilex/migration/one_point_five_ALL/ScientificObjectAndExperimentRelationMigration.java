package org.opensilex.migration.one_point_five_ALL;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.OpenSilex;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

/**
 * @author dnooka / Max
 */
public class ScientificObjectAndExperimentRelationMigration implements OpenSilexModuleUpdate {

    private static final Node SO_VAR = NodeFactory.createVariable("scientificObj");
    private static final Node GRAPH_VAR = NodeFactory.createVariable("graph");
    private static final Node TYPE_VAR = NodeFactory.createVariable("type");
    protected static String DESCRIPTION = "Scientific Objects created from XP will have a relation: participatesIn an XP in the XP context";

    private OpenSilex opensilex;
    private final Logger logger;

    public ScientificObjectAndExperimentRelationMigration() {
        this(LoggerFactory.getLogger(ScientificObjectAndExperimentRelationMigration.class));
    }

    public ScientificObjectAndExperimentRelationMigration(Logger logger) {
        this.logger = logger;
    }

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.now();
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public void execute() throws OpensilexModuleUpdateException {
        var factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        var sparql = factory.provide();
        var mongo = opensilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);

        try {
            new SparqlMongoTransaction(sparql, mongo.getServiceV2()).execute(session -> {
                executeWithinTransaction(sparql);
                return null;
            });
        } catch (Exception e) {
            logger.error("Error during scientific objects experiment relations migration", e);
            throw new OpensilexModuleUpdateException(this, e);
        }
    }

    @Override
    public void setOpensilex(OpenSilex opensilex) {

    }

    /**
     * Checks if this migration was most likely already run. Does this by checking if any participatesIn relations already exist on ScientificObjects
     *
     * @return true if participatesIn was found on any ScientificObject
     */
    protected boolean wasMigrationPreviouslyRun(SPARQLService sparql) throws SPARQLException {
        AskBuilder participatesInSelect = new AskBuilder();

        participatesInSelect.addWhere(TYPE_VAR, Ontology.subClassAny, Oeso.ScientificObject);
        participatesInSelect.addWhere(SO_VAR, RDF.type, TYPE_VAR);
        participatesInSelect.addWhere(SO_VAR, Oeso.participatesIn, GRAPH_VAR);

        return sparql.executeAskQuery(participatesInSelect);
    }

    private WhereBuilder buildWhere(SPARQLService sparql) throws SPARQLException {

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

    protected void executeWithinTransaction(SPARQLService sparql) throws SPARQLException {
        if (wasMigrationPreviouslyRun(sparql)) {
            logger.info("The migration seems to have already been performed. Nothing will be done.");
            return;
        }

        try {
            UpdateBuilder updateQuery = new UpdateBuilder()
                    .addInsert(GRAPH_VAR, SO_VAR, Oeso.participatesIn, GRAPH_VAR)
                    .addWhere(buildWhere(sparql));
            sparql.executeUpdateQuery(updateQuery);
        } catch (SPARQLException e) {
            logger.warn("Something went wrong in the ScientificObjectAndExperimentRelationMigration part of the migration!");
            throw e;
        }
    }
}
